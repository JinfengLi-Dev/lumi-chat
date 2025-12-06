<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, UserFilled, Delete, SwitchButton, Plus } from '@element-plus/icons-vue'
import { groupApi } from '@/api/group'
import GroupMemberList from './GroupMemberList.vue'
import type { GroupDetail, GroupMember } from '@/api/group'
import type { Group } from '@/types'

const props = defineProps<{
  group: Group | null
  currentUserId: number
}>()

const emit = defineEmits<{
  (e: 'update:group', group: Group): void
  (e: 'invite-members'): void
  (e: 'leave'): void
  (e: 'dissolve'): void
}>()

const groupDetail = ref<GroupDetail | null>(null)
const members = ref<GroupMember[]>([])
const isLoadingDetail = ref(false)
const isLoadingMembers = ref(false)
const isEditing = ref(false)
const editForm = ref({
  name: '',
  announcement: '',
})

const isOwner = computed(() => groupDetail.value?.ownerId === props.currentUserId)
const isAdmin = computed(() => {
  const currentMember = members.value.find((m) => m.userId === props.currentUserId)
  return currentMember?.role === 'admin'
})
const isOwnerOrAdmin = computed(() => isOwner.value || isAdmin.value)

async function loadGroupDetail() {
  if (!props.group?.id) return

  isLoadingDetail.value = true
  try {
    groupDetail.value = await groupApi.getGroup(props.group.id)
    editForm.value = {
      name: groupDetail.value.name,
      announcement: groupDetail.value.announcement || '',
    }
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to load group details'
    ElMessage.error(message)
  } finally {
    isLoadingDetail.value = false
  }
}

async function loadMembers() {
  if (!props.group?.id) return

  isLoadingMembers.value = true
  try {
    members.value = await groupApi.getGroupMembers(props.group.id)
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to load members'
    ElMessage.error(message)
  } finally {
    isLoadingMembers.value = false
  }
}

async function saveGroupInfo() {
  if (!props.group?.id) return

  try {
    const updated = await groupApi.updateGroup(props.group.id, {
      name: editForm.value.name,
      announcement: editForm.value.announcement,
    })
    groupDetail.value = updated
    isEditing.value = false
    ElMessage.success('Group info updated')

    // Emit update to parent
    emit('update:group', {
      ...props.group,
      name: updated.name,
      announcement: updated.announcement,
    })
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to update group'
    ElMessage.error(message)
  }
}

function cancelEdit() {
  isEditing.value = false
  if (groupDetail.value) {
    editForm.value = {
      name: groupDetail.value.name,
      announcement: groupDetail.value.announcement || '',
    }
  }
}

async function handleRemoveMember(member: GroupMember) {
  if (!props.group?.id) return

  try {
    await ElMessageBox.confirm(
      `Are you sure you want to remove ${member.nickname} from this group?`,
      'Remove Member',
      {
        confirmButtonText: 'Remove',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )

    await groupApi.removeMember(props.group.id, member.userId)
    members.value = members.value.filter((m) => m.userId !== member.userId)
    ElMessage.success(`${member.nickname} has been removed from the group`)
  } catch (error) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : 'Failed to remove member'
      ElMessage.error(message)
    }
  }
}

async function handlePromoteMember(_member: GroupMember) {
  // Note: This would require a separate API endpoint for role updates
  // For now, show a message that this feature is coming soon
  ElMessage.info('Promote to admin feature coming soon')
}

async function handleDemoteMember(_member: GroupMember) {
  // Note: This would require a separate API endpoint for role updates
  // For now, show a message that this feature is coming soon
  ElMessage.info('Demote to member feature coming soon')
}

async function handleLeaveGroup() {
  if (!props.group?.id) return

  try {
    await ElMessageBox.confirm(
      'Are you sure you want to leave this group?',
      'Leave Group',
      {
        confirmButtonText: 'Leave',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )

    await groupApi.leaveGroup(props.group.id)
    ElMessage.success('You have left the group')
    emit('leave')
  } catch (error) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : 'Failed to leave group'
      ElMessage.error(message)
    }
  }
}

async function handleDissolveGroup() {
  if (!props.group?.id) return

  try {
    await ElMessageBox.confirm(
      'Are you sure you want to dissolve this group? This action cannot be undone.',
      'Dissolve Group',
      {
        confirmButtonText: 'Dissolve',
        cancelButtonText: 'Cancel',
        type: 'error',
      }
    )

    await groupApi.deleteGroup(props.group.id)
    ElMessage.success('Group has been dissolved')
    emit('dissolve')
  } catch (error) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : 'Failed to dissolve group'
      ElMessage.error(message)
    }
  }
}

watch(() => props.group?.id, async (newId) => {
  if (newId) {
    await Promise.all([loadGroupDetail(), loadMembers()])
  } else {
    groupDetail.value = null
    members.value = []
  }
}, { immediate: true })
</script>

<template>
  <div class="group-info-panel" v-loading="isLoadingDetail">
    <template v-if="groupDetail">
      <!-- Header with Avatar and Name -->
      <div class="panel-header">
        <el-avatar
          :src="groupDetail.avatar"
          :size="80"
          shape="square"
          class="group-avatar"
        >
          {{ groupDetail.name.charAt(0) }}
        </el-avatar>

        <div v-if="!isEditing" class="group-title">
          <h3>{{ groupDetail.name }}</h3>
          <p class="group-id">ID: {{ groupDetail.gid }}</p>
          <el-button
            v-if="isOwnerOrAdmin"
            text
            size="small"
            :icon="Edit"
            @click="isEditing = true"
          >
            Edit
          </el-button>
        </div>

        <div v-else class="edit-form">
          <el-input
            v-model="editForm.name"
            placeholder="Group name"
            size="small"
          />
          <div class="edit-actions">
            <el-button size="small" @click="cancelEdit">Cancel</el-button>
            <el-button size="small" type="primary" @click="saveGroupInfo">Save</el-button>
          </div>
        </div>
      </div>

      <!-- Announcement Section -->
      <div class="panel-section">
        <div class="section-header">
          <span class="section-title">Announcement</span>
          <el-button
            v-if="isOwnerOrAdmin && !isEditing"
            text
            size="small"
            :icon="Edit"
            @click="isEditing = true"
          />
        </div>
        <div v-if="!isEditing" class="announcement-content">
          {{ groupDetail.announcement || 'No announcement' }}
        </div>
        <el-input
          v-else
          v-model="editForm.announcement"
          type="textarea"
          placeholder="Enter announcement..."
          :rows="3"
        />
      </div>

      <!-- Members Section -->
      <div class="panel-section">
        <div class="section-header">
          <span class="section-title">
            <el-icon><UserFilled /></el-icon>
            Members ({{ groupDetail.memberCount }}/{{ groupDetail.maxMembers }})
          </span>
          <el-button
            v-if="isOwnerOrAdmin"
            text
            size="small"
            :icon="Plus"
            @click="emit('invite-members')"
          >
            Invite
          </el-button>
        </div>

        <div v-loading="isLoadingMembers">
          <GroupMemberList
            :members="members"
            :current-user-id="currentUserId"
            :owner-id="groupDetail.ownerId"
            :is-owner-or-admin="isOwnerOrAdmin"
            @remove-member="handleRemoveMember"
            @promote-member="handlePromoteMember"
            @demote-member="handleDemoteMember"
          />
        </div>
      </div>

      <!-- Actions Section -->
      <div class="panel-section actions-section">
        <el-button
          v-if="!isOwner"
          type="warning"
          plain
          :icon="SwitchButton"
          @click="handleLeaveGroup"
        >
          Leave Group
        </el-button>

        <el-button
          v-if="isOwner"
          type="danger"
          plain
          :icon="Delete"
          @click="handleDissolveGroup"
        >
          Dissolve Group
        </el-button>
      </div>
    </template>

    <template v-else-if="!isLoadingDetail">
      <div class="empty-state">
        <el-empty description="No group selected" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.group-info-panel {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.panel-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.group-avatar {
  margin-bottom: 12px;
}

.group-title h3 {
  margin: 0;
  font-size: 18px;
  color: var(--el-text-color-primary);
}

.group-id {
  margin: 4px 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.edit-form {
  width: 100%;
  margin-top: 12px;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.panel-section {
  padding: 16px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.panel-section:last-child {
  border-bottom: none;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.announcement-content {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
  white-space: pre-wrap;
}

.actions-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.actions-section .el-button {
  width: 100%;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}
</style>
