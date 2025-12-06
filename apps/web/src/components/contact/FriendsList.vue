<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { friendApi } from '@/api/friend'
import { conversationApi } from '@/api/conversation'
import FriendItem from './FriendItem.vue'
import type { Friend } from '@/types'

const emit = defineEmits<{
  (e: 'open-conversation', conversationId: number): void
  (e: 'context-menu', friend: Friend, event: MouseEvent): void
}>()

const friends = ref<Friend[]>([])
const isLoading = ref(false)
const searchQuery = ref('')

const filteredFriends = computed(() => {
  if (!searchQuery.value) return friends.value

  const query = searchQuery.value.toLowerCase()
  return friends.value.filter((friend) => {
    const displayName = friend.remark || friend.nickname || ''
    return displayName.toLowerCase().includes(query)
  })
})

async function loadFriends() {
  isLoading.value = true
  try {
    friends.value = await friendApi.getFriends()
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to load friends'
    ElMessage.error(message)
  } finally {
    isLoading.value = false
  }
}

async function handleFriendClick(friend: Friend) {
  try {
    // Create or get existing private conversation with this friend
    const conversation = await conversationApi.createPrivateConversation(friend.id)
    emit('open-conversation', conversation.id)
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to open conversation'
    ElMessage.error(message)
  }
}

function handleContextMenu(friend: Friend, event: MouseEvent) {
  emit('context-menu', friend, event)
}

// Expose methods for parent to refresh
defineExpose({
  refresh: loadFriends,
})

onMounted(() => {
  loadFriends()
})
</script>

<template>
  <div class="friends-list" v-loading="isLoading">
    <!-- Search -->
    <div class="search-container">
      <el-input
        v-model="searchQuery"
        placeholder="Search friends"
        :prefix-icon="Search"
        clearable
        class="search-input"
      />
    </div>

    <!-- Friend List -->
    <div class="friends-content">
      <FriendItem
        v-for="friend in filteredFriends"
        :key="friend.id"
        :friend="friend"
        @click="handleFriendClick(friend)"
        @context-menu="(e) => handleContextMenu(friend, e)"
      />

      <div v-if="filteredFriends.length === 0 && !isLoading" class="empty-state">
        <el-empty
          :description="searchQuery ? 'No friends found' : 'No friends yet'"
          :image-size="80"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.friends-list {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-container {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.search-input {
  width: 100%;
}

.friends-content {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}
</style>
