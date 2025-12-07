<script setup lang="ts">
import { computed } from 'vue'
import { UserFilled, ChatDotRound, Plus } from '@element-plus/icons-vue'
import type { Group } from '@/types'

const props = defineProps<{
  group: Group
  // Whether the current user is already a member of this group
  isMember?: boolean
}>()

const emit = defineEmits<{
  (e: 'view-group', group: Group): void
  (e: 'open-chat', group: Group): void
  (e: 'join-group', group: Group): void
}>()

const displayName = computed(() => props.group.name || 'Unknown Group')

const memberCountText = computed(() => {
  const count = props.group.memberCount ?? 0
  return `${count} member${count !== 1 ? 's' : ''}`
})

function handleCardClick() {
  emit('view-group', props.group)
}

function handleOpenChat(e: Event) {
  e.stopPropagation()
  emit('open-chat', props.group)
}

function handleJoinGroup(e: Event) {
  e.stopPropagation()
  emit('join-group', props.group)
}
</script>

<template>
  <div class="group-card-message" @click="handleCardClick">
    <!-- Group info section -->
    <div class="group-info">
      <el-avatar :src="group.avatar" :size="48" shape="square" class="group-avatar">
        {{ displayName.charAt(0) }}
      </el-avatar>

      <div class="group-details">
        <div class="group-name">{{ displayName }}</div>
        <div class="group-gid">ID: {{ group.gid }}</div>
        <div class="member-count">
          <el-icon :size="12"><UserFilled /></el-icon>
          {{ memberCountText }}
        </div>
      </div>
    </div>

    <!-- Announcement preview -->
    <div v-if="group.announcement" class="group-announcement">
      {{ group.announcement }}
    </div>

    <!-- Action buttons -->
    <div class="card-actions">
      <el-button
        v-if="isMember"
        size="small"
        type="primary"
        @click="handleOpenChat"
      >
        <el-icon><ChatDotRound /></el-icon>
        Open Chat
      </el-button>
      <el-button
        v-else
        size="small"
        @click="handleJoinGroup"
      >
        <el-icon><Plus /></el-icon>
        Join Group
      </el-button>
    </div>

    <!-- Card type indicator -->
    <div class="card-type-indicator">
      <el-icon :size="12"><UserFilled /></el-icon>
      Group Card
    </div>
  </div>
</template>

<style scoped>
.group-card-message {
  width: 240px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.group-card-message:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.group-info {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.group-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--el-color-primary-light-3), var(--el-color-primary));
  color: white;
}

.group-details {
  flex: 1;
  min-width: 0;
}

.group-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-gid {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.member-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--el-text-color-regular);
  margin-top: 4px;
}

.group-announcement {
  font-size: 12px;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-lighter);
  padding: 8px 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
