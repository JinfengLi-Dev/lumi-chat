<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { useFriendStore } from '@/stores/friend'
import { conversationApi } from '@/api/conversation'
import FriendItem from './FriendItem.vue'
import type { Friend } from '@/types'

const emit = defineEmits<{
  (e: 'open-conversation', conversationId: number): void
  (e: 'context-menu', friend: Friend, event: MouseEvent): void
}>()

const friendStore = useFriendStore()
const searchQuery = ref('')
const scrollContainerRef = ref<HTMLElement | null>(null)

const filteredFriends = computed(() => {
  const friends = friendStore.sortedFriends

  if (!searchQuery.value) return friends

  const query = searchQuery.value.toLowerCase()
  return friends.filter((friend) => {
    const displayName = friend.remark || friend.nickname || ''
    return displayName.toLowerCase().includes(query)
  })
})

// Virtual scrolling setup
const virtualizer = useVirtualizer({
  get count() {
    return filteredFriends.value.length
  },
  getScrollElement: () => scrollContainerRef.value,
  estimateSize: () => 60, // Estimated height of each friend item
  overscan: 5, // Render 5 extra items above and below viewport
})

async function loadFriends() {
  try {
    await friendStore.fetchFriends()
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to load friends'
    ElMessage.error(message)
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
  <div class="friends-list" v-loading="friendStore.loading">
    <!-- Header with online count -->
    <div class="header">
      <span class="online-count">
        {{ friendStore.onlineFriendsCount }}/{{ friendStore.friends.length }} Online
      </span>
    </div>

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

    <!-- Friend List with Virtual Scrolling -->
    <div
      ref="scrollContainerRef"
      class="friends-content"
    >
      <div
        :style="{
          height: `${virtualizer.getTotalSize()}px`,
          width: '100%',
          position: 'relative',
        }"
      >
        <div
          v-for="virtualRow in virtualizer.getVirtualItems()"
          :key="String(virtualRow.key)"
          :style="{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: `${virtualRow.size}px`,
            transform: `translateY(${virtualRow.start}px)`,
          }"
        >
          <FriendItem
            :friend="filteredFriends[virtualRow.index]"
            :is-online="friendStore.isUserOnline(filteredFriends[virtualRow.index].id)"
            @click="handleFriendClick(filteredFriends[virtualRow.index])"
            @context-menu="(e) => handleContextMenu(filteredFriends[virtualRow.index], e)"
          />
        </div>
      </div>

      <div v-if="filteredFriends.length === 0 && !friendStore.loading" class="empty-state">
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

.header {
  padding: 8px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.online-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
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
  /* Performance optimization: contain layout to prevent repaints */
  contain: layout style;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}
</style>
