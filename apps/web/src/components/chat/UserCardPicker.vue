<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search, User as UserIcon, Close } from '@element-plus/icons-vue'
import type { User } from '@/types'

const props = defineProps<{
  visible: boolean
  friends: User[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'select', user: User): void
}>()

const searchQuery = ref('')

const filteredFriends = computed(() => {
  if (!searchQuery.value.trim()) {
    return props.friends
  }

  const query = searchQuery.value.toLowerCase()
  return props.friends.filter(
    (friend) =>
      friend.nickname.toLowerCase().includes(query) ||
      friend.uid.toLowerCase().includes(query) ||
      friend.email?.toLowerCase().includes(query)
  )
})

function selectUser(user: User) {
  emit('select', user)
  close()
}

function close() {
  emit('update:visible', false)
  searchQuery.value = ''
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="user-card-picker-overlay" @click.self="close">
      <div class="user-card-picker">
        <div class="picker-header">
          <span class="title">Send Contact Card</span>
          <el-button text @click="close">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <!-- Search bar -->
        <div class="search-bar">
          <el-input
            v-model="searchQuery"
            placeholder="Search friends..."
            :prefix-icon="Search"
            clearable
          />
        </div>

        <!-- Friends list -->
        <div class="friends-list">
          <div v-if="filteredFriends.length === 0" class="empty-state">
            <el-icon :size="48" color="#c0c4cc"><UserIcon /></el-icon>
            <p v-if="friends.length === 0">No friends yet</p>
            <p v-else>No matching friends found</p>
          </div>

          <div
            v-for="friend in filteredFriends"
            :key="friend.id"
            class="friend-item"
            @click="selectUser(friend)"
          >
            <el-avatar :src="friend.avatar" :size="40" shape="square">
              {{ friend.nickname.charAt(0) }}
            </el-avatar>

            <div class="friend-info">
              <div class="friend-name">{{ friend.nickname }}</div>
              <div class="friend-uid">ID: {{ friend.uid }}</div>
            </div>

            <el-icon class="select-icon"><el-icon-arrow-right /></el-icon>
          </div>
        </div>

        <!-- Actions -->
        <div class="picker-actions">
          <el-button @click="close">Cancel</el-button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.user-card-picker-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-card-picker {
  width: 400px;
  max-height: 80vh;
  background: var(--el-bg-color);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.search-bar {
  padding: 16px 20px;
}

.friends-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
  min-height: 200px;
  max-height: 400px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--el-text-color-secondary);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.friend-item:hover {
  background: var(--el-fill-color-light);
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-uid {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.select-icon {
  color: var(--el-text-color-placeholder);
}

.picker-actions {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* Scrollbar */
.friends-list::-webkit-scrollbar {
  width: 6px;
}

.friends-list::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color);
  border-radius: 3px;
}

.friends-list::-webkit-scrollbar-track {
  background-color: transparent;
}
</style>
