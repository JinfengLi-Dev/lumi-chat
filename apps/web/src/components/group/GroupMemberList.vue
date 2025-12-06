<script setup lang="ts">
import { computed } from 'vue'
import { Medal, User, StarFilled } from '@element-plus/icons-vue'
import type { GroupMember } from '@/api/group'

const props = defineProps<{
  members: GroupMember[]
  currentUserId: number
  ownerId: number
  isOwnerOrAdmin: boolean
}>()

const emit = defineEmits<{
  (e: 'remove-member', member: GroupMember): void
  (e: 'promote-member', member: GroupMember): void
  (e: 'demote-member', member: GroupMember): void
}>()

const sortedMembers = computed(() => {
  return [...props.members].sort((a, b) => {
    const roleOrder: Record<string, number> = { owner: 0, admin: 1, member: 2 }
    return roleOrder[a.role] - roleOrder[b.role]
  })
})

function getRoleBadge(role: string): { icon: typeof Medal; color: string; label: string } | null {
  switch (role) {
    case 'owner':
      return { icon: Medal, color: '#e6a23c', label: 'Owner' }
    case 'admin':
      return { icon: StarFilled, color: '#409eff', label: 'Admin' }
    default:
      return null
  }
}

function canRemove(member: GroupMember): boolean {
  if (!props.isOwnerOrAdmin) return false
  if (member.userId === props.currentUserId) return false
  if (member.role === 'owner') return false
  // Only owner can remove admins
  if (member.role === 'admin' && props.currentUserId !== props.ownerId) return false
  return true
}

function canPromote(member: GroupMember): boolean {
  if (props.currentUserId !== props.ownerId) return false
  return member.role === 'member'
}

function canDemote(member: GroupMember): boolean {
  if (props.currentUserId !== props.ownerId) return false
  return member.role === 'admin'
}

function getDisplayName(member: GroupMember): string {
  return member.groupNickname || member.nickname
}
</script>

<template>
  <div class="member-list">
    <div
      v-for="member in sortedMembers"
      :key="member.id"
      class="member-item"
    >
      <div class="member-avatar">
        <el-avatar :src="member.avatar" :size="36" shape="circle">
          {{ getDisplayName(member).charAt(0) }}
        </el-avatar>
      </div>

      <div class="member-info">
        <div class="member-name">
          {{ getDisplayName(member) }}
          <span v-if="member.groupNickname && member.groupNickname !== member.nickname" class="real-name">
            ({{ member.nickname }})
          </span>
        </div>
        <div class="member-uid">ID: {{ member.uid }}</div>
      </div>

      <div v-if="getRoleBadge(member.role)" class="member-role">
        <el-icon :color="getRoleBadge(member.role)!.color" :size="14">
          <component :is="getRoleBadge(member.role)!.icon" />
        </el-icon>
        <span :style="{ color: getRoleBadge(member.role)!.color }">
          {{ getRoleBadge(member.role)!.label }}
        </span>
      </div>

      <el-dropdown
        v-if="isOwnerOrAdmin && member.userId !== currentUserId && member.role !== 'owner'"
        trigger="click"
        class="member-actions"
      >
        <el-button text circle size="small">
          <el-icon><User /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="canPromote(member)" @click="emit('promote-member', member)">
              Promote to Admin
            </el-dropdown-item>
            <el-dropdown-item v-if="canDemote(member)" @click="emit('demote-member', member)">
              Demote to Member
            </el-dropdown-item>
            <el-dropdown-item
              v-if="canRemove(member)"
              divided
              style="color: var(--el-color-danger)"
              @click="emit('remove-member', member)"
            >
              Remove from Group
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div v-if="members.length === 0" class="empty-state">
      <el-empty description="No members found" :image-size="60" />
    </div>
  </div>
</template>

<style scoped>
.member-list {
  max-height: 300px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  gap: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.member-item:last-child {
  border-bottom: none;
}

.member-avatar {
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 14px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.real-name {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.member-uid {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.member-role {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  flex-shrink: 0;
}

.member-actions {
  flex-shrink: 0;
}

.empty-state {
  padding: 20px 0;
}
</style>
