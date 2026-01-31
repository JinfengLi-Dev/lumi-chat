<script setup lang="ts">
import { computed } from 'vue'
import { User as UserIcon, ChatDotRound, Plus } from '@element-plus/icons-vue'
import { useFriendStore } from '@/stores/friend'
import type { User } from '@/types'

const props = defineProps<{
  user: User
  // Whether the current user is already friends with this user
  isFriend?: boolean
  // Whether this is the current user's own card
  isSelf?: boolean
}>()

const emit = defineEmits<{
  (e: 'view-profile', user: User): void
  (e: 'send-message', user: User): void
  (e: 'add-friend', user: User): void
}>()

const friendStore = useFriendStore()

const displayName = computed(() => props.user.nickname || 'Unknown User')

const isOnline = computed(() => friendStore.isUserOnline(props.user.id))

const statusText = computed(() => {
  return isOnline.value ? 'Online' : 'Offline'
})

const statusClass = computed(() => {
  return isOnline.value ? 'status-online' : 'status-offline'
})

function handleCardClick() {
  emit('view-profile', props.user)
}

function handleSendMessage(e: Event) {
  e.stopPropagation()
  emit('send-message', props.user)
}

function handleAddFriend(e: Event) {
  e.stopPropagation()
  emit('add-friend', props.user)
}
</script>

<template>
  <div class="user-card-message" @click="handleCardClick">
    <!-- User info section -->
    <div class="user-info">
      <div class="avatar-wrapper">
        <el-avatar :src="user.avatar" :size="48" shape="square" class="user-avatar">
          {{ displayName.charAt(0) }}
        </el-avatar>
        <span
          class="online-indicator"
          :class="{ online: isOnline }"
        />
      </div>

      <div class="user-details">
        <div class="user-name">{{ displayName }}</div>
        <div class="user-uid">ID: {{ user.uid }}</div>
        <div v-if="user.signature" class="user-signature">{{ user.signature }}</div>
      </div>
    </div>

    <!-- Status indicator -->
    <div class="user-status" :class="statusClass">
      <span class="status-dot"></span>
      {{ statusText }}
    </div>

    <!-- Action buttons -->
    <div v-if="!isSelf" class="card-actions">
      <el-button
        v-if="isFriend"
        size="small"
        type="primary"
        @click="handleSendMessage"
      >
        <el-icon><ChatDotRound /></el-icon>
        Message
      </el-button>
      <el-button
        v-else
        size="small"
        @click="handleAddFriend"
      >
        <el-icon><Plus /></el-icon>
        Add Friend
      </el-button>
    </div>

    <!-- Card type indicator -->
    <div class="card-type-indicator">
      <el-icon :size="12"><UserIcon /></el-icon>
      Contact Card
    </div>
  </div>
</template>

<style scoped>
.user-card-message {
  width: 240px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.user-card-message:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-info {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.user-avatar {
  flex-shrink: 0;
}

.online-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: var(--el-text-color-secondary);
  border: 2px solid var(--el-bg-color);
}

.online-indicator.online {
  background-color: var(--el-color-success);
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-uid {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.user-signature {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-bottom: 8px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-online {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.status-online .status-dot {
  background: var(--el-color-success);
}

.status-offline {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}

.status-offline .status-dot {
  background: var(--el-text-color-secondary);
}

.status-other {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.status-other .status-dot {
  background: var(--el-color-warning);
}

.card-actions {
  margin-bottom: 8px;
}

.card-actions .el-button {
  width: 100%;
}

.card-type-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: var(--el-text-color-placeholder);
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
