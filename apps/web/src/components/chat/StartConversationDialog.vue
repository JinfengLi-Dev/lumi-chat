<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useFriendStore } from '@/stores/friend'
import { conversationApi } from '@/api'
import { getErrorMessage } from '@/utils/errorHandler'
import type { Friend } from '@/types'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'conversation-started', conversationId: number): void
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const friendStore = useFriendStore()
const searchQuery = ref('')
const isLoading = ref(false)
const isStarting = ref(false)

const filteredFriends = computed(() => {
  const friends = friendStore.sortedFriends
  if (!searchQuery.value) return friends

  const query = searchQuery.value.toLowerCase()
  return friends.filter((friend) => {
    const displayName = friend.remark || friend.nickname
    return (
      displayName.toLowerCase().includes(query) ||
      friend.uid.toLowerCase().includes(query)
    )
  })
})

async function loadFriends() {
  if (friendStore.friends.length > 0) return // Already loaded

  isLoading.value = true
  try {
    await friendStore.fetchFriends()
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    isLoading.value = false
  }
}

async function selectFriend(friend: Friend) {
  isStarting.value = true
  try {
    const conversation = await conversationApi.createPrivateConversation(friend.id)
    emit('conversation-started', conversation.id)
    handleClose()
    ElMessage.success(`Starting conversation with ${friend.remark || friend.nickname}`)
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    isStarting.value = false
  }
}

function handleClose() {
  searchQuery.value = ''
  dialogVisible.value = false
}

function isOnline(friendId: number): boolean {
  return friendStore.isUserOnline(friendId)
}

watch(dialogVisible, (visible) => {
  if (visible) {
    loadFriends()
  }
})
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="Start Conversation"
    width="400px"
    :before-close="handleClose"
    destroy-on-close
  >
    <!-- Search -->
    <el-input
      v-model="searchQuery"
      placeholder="Search friends by name or UID..."
      prefix-icon="Search"
      clearable
      class="search-input"
    />

    <!-- Friends count -->
    <div class="friends-count">
      {{ filteredFriends.length }} friend{{ filteredFriends.length !== 1 ? 's' : '' }}
      <span v-if="friendStore.onlineFriendsCount > 0" class="online-count">
        ({{ friendStore.onlineFriendsCount }} online)
      </span>
    </div>

    <!-- Friends List -->
    <div v-loading="isLoading || isStarting" class="friends-list">
      <div v-if="filteredFriends.length === 0 && !isLoading" class="empty-state">
        <el-empty :description="searchQuery ? 'No friends found' : 'No friends yet'" />
      </div>

      <div
        v-for="friend in filteredFriends"
        :key="friend.id"
        class="friend-item"
        @click="selectFriend(friend)"
      >
        <div class="avatar-wrapper">
          <el-avatar :src="friend.avatar" :size="44" shape="square">
            {{ (friend.remark || friend.nickname).charAt(0) }}
          </el-avatar>
          <span v-if="isOnline(friend.id)" class="online-dot" />
        </div>
        <div class="friend-info">
          <div class="friend-name">{{ friend.remark || friend.nickname }}</div>
          <div class="friend-uid">@{{ friend.uid }}</div>
        </div>
        <el-icon class="arrow-icon"><ArrowRight /></el-icon>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">Cancel</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.search-input {
  margin-bottom: 12px;
}

.friends-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.online-count {
  color: var(--el-color-success);
}

.friends-list {
  max-height: 400px;
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

.friend-item:not(:last-child) {
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.avatar-wrapper {
  position: relative;
}

.online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background-color: var(--el-color-success);
  border: 2px solid #fff;
  border-radius: 50%;
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-uid {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.arrow-icon {
  color: var(--el-text-color-secondary);
}

.empty-state {
  padding: 32px 0;
}
</style>
