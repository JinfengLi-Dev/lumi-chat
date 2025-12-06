<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { friendApi } from '@/api/friend'
import { groupApi } from '@/api/group'
import type { Friend } from '@/types'
import type { GroupMember } from '@/api/group'

const props = defineProps<{
  modelValue: boolean
  groupId: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'invited', memberIds: number[]): void
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const friends = ref<Friend[]>([])
const existingMemberIds = ref<Set<number>>(new Set())
const selectedFriendIds = ref<number[]>([])
const searchQuery = ref('')
const isLoadingFriends = ref(false)
const isLoadingMembers = ref(false)
const isInviting = ref(false)

const availableFriends = computed(() => {
  return friends.value.filter((friend) => !existingMemberIds.value.has(friend.id))
})

const filteredFriends = computed(() => {
  if (!searchQuery.value) return availableFriends.value

  const query = searchQuery.value.toLowerCase()
  return availableFriends.value.filter((friend) => {
    const name = friend.remark || friend.nickname || ''
    return name.toLowerCase().includes(query)
  })
})

async function loadFriends() {
  isLoadingFriends.value = true
  try {
    friends.value = await friendApi.getFriends()
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to load friends'
    ElMessage.error(message)
  } finally {
    isLoadingFriends.value = false
  }
}

async function loadExistingMembers() {
  if (!props.groupId) return

  isLoadingMembers.value = true
  try {
    const members = await groupApi.getGroupMembers(props.groupId)
    existingMemberIds.value = new Set(members.map((m: GroupMember) => m.userId))
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to load members'
    ElMessage.error(message)
  } finally {
    isLoadingMembers.value = false
  }
}

function toggleFriend(friendUserId: number) {
  const index = selectedFriendIds.value.indexOf(friendUserId)
  if (index > -1) {
    selectedFriendIds.value.splice(index, 1)
  } else {
    selectedFriendIds.value.push(friendUserId)
  }
}

function isSelected(friendUserId: number): boolean {
  return selectedFriendIds.value.includes(friendUserId)
}

async function handleInvite() {
  if (selectedFriendIds.value.length === 0) {
    ElMessage.warning('Please select at least one friend')
    return
  }

  if (!props.groupId) {
    ElMessage.error('No group selected')
    return
  }

  isInviting.value = true
  try {
    await groupApi.addMembers(props.groupId, selectedFriendIds.value)
    ElMessage.success(`${selectedFriendIds.value.length} member(s) invited successfully`)
    emit('invited', selectedFriendIds.value)
    handleClose()
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to invite members'
    ElMessage.error(message)
  } finally {
    isInviting.value = false
  }
}

function handleClose() {
  selectedFriendIds.value = []
  searchQuery.value = ''
  dialogVisible.value = false
}

function getDisplayName(friend: Friend): string {
  return friend.remark || friend.nickname || 'Unknown'
}

watch(dialogVisible, async (visible) => {
  if (visible) {
    await Promise.all([loadFriends(), loadExistingMembers()])
  }
})
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="Invite Members"
    width="480px"
    :before-close="handleClose"
    destroy-on-close
  >
    <!-- Search -->
    <el-input
      v-model="searchQuery"
      placeholder="Search friends"
      :prefix-icon="Search"
      clearable
      class="search-input"
    />

    <!-- Selected count -->
    <div class="selected-count">
      Selected: {{ selectedFriendIds.length }} friend(s)
    </div>

    <!-- Friend List -->
    <div v-loading="isLoadingFriends || isLoadingMembers" class="friend-list">
      <div v-if="filteredFriends.length === 0 && !isLoadingFriends && !isLoadingMembers" class="empty-state">
        <el-empty
          :description="availableFriends.length === 0
            ? 'All your friends are already in this group'
            : 'No friends found'"
        />
      </div>

      <div
        v-for="friend in filteredFriends"
        :key="friend.id"
        class="friend-item"
        :class="{ selected: isSelected(friend.id) }"
        @click="toggleFriend(friend.id)"
      >
        <el-checkbox
          :model-value="isSelected(friend.id)"
          @click.stop
          @change="toggleFriend(friend.id)"
        />
        <el-avatar :src="friend.avatar" :size="40" shape="circle">
          {{ getDisplayName(friend).charAt(0) }}
        </el-avatar>
        <div class="friend-info">
          <div class="friend-name">{{ getDisplayName(friend) }}</div>
          <div v-if="friend.remark && friend.remark !== friend.nickname" class="friend-real-name">
            {{ friend.nickname }}
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">Cancel</el-button>
      <el-button
        type="primary"
        :loading="isInviting"
        :disabled="selectedFriendIds.length === 0"
        @click="handleInvite"
      >
        Invite ({{ selectedFriendIds.length }})
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.search-input {
  margin-bottom: 12px;
}

.selected-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.friend-list {
  max-height: 350px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  gap: 12px;
  transition: background-color 0.2s;
}

.friend-item:hover {
  background-color: var(--el-fill-color-light);
}

.friend-item.selected {
  background-color: var(--el-color-primary-light-9);
}

.friend-info {
  flex: 1;
}

.friend-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.friend-real-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.empty-state {
  padding: 32px 0;
}
</style>
