<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, Close } from '@element-plus/icons-vue'
import type { Message } from '@/types'

const props = defineProps<{
  pinnedMessages: Message[]
  conversationId: number
}>()

const emit = defineEmits<{
  (e: 'jump-to', messageId: number): void
  (e: 'unpin', messageId: number): void
  (e: 'close'): void
}>()

const displayedMessages = computed(() => {
  // Show up to 3 pinned messages
  return props.pinnedMessages.slice(0, 3)
})

const hasMore = computed(() => props.pinnedMessages.length > 3)

function getMessagePreview(msg: Message): string {
  if (msg.recalledAt) {
    return 'Recalled message'
  }

  switch (msg.msgType) {
    case 'text':
      return msg.content.length > 50 ? msg.content.substring(0, 50) + '...' : msg.content
    case 'image':
      return '[Image]'
    case 'video':
      return '[Video]'
    case 'voice':
      return '[Voice]'
    case 'file':
      return `[File: ${msg.metadata?.fileName || 'Unknown'}]`
    case 'location':
      return '[Location]'
    case 'user_card':
      return '[Contact Card]'
    case 'group_card':
      return '[Group Card]'
    default:
      return `[${msg.msgType}]`
  }
}

function handleJumpTo(messageId: number) {
  emit('jump-to', messageId)
}

function handleUnpin(e: Event, messageId: number) {
  e.stopPropagation()
  emit('unpin', messageId)
}
</script>

<template>
  <div v-if="pinnedMessages.length > 0" class="pinned-messages-panel">
    <div class="panel-header">
      <span class="title">📌 Pinned Messages ({{ pinnedMessages.length }})</span>
      <el-icon class="close-btn" @click="emit('close')">
        <Close />
      </el-icon>
    </div>

    <div class="pinned-list">
      <div
        v-for="msg in displayedMessages"
        :key="msg.id"
        class="pinned-item"
        @click="handleJumpTo(msg.id)"
      >
        <div class="message-info">
          <div class="sender-name">{{ msg.sender?.nickname || 'Unknown' }}</div>
          <div class="message-preview">{{ getMessagePreview(msg) }}</div>
        </div>
        <div class="actions">
          <el-tooltip content="Jump to message" placement="top">
            <el-icon class="action-icon jump-icon">
              <ArrowDown />
            </el-icon>
          </el-tooltip>
          <el-tooltip content="Unpin" placement="top">
            <el-icon class="action-icon unpin-icon" @click="handleUnpin($event, msg.id)">
              <Close />
            </el-icon>
          </el-tooltip>
        </div>
      </div>

      <div v-if="hasMore" class="more-indicator">
        +{{ pinnedMessages.length - 3 }} more pinned message{{ pinnedMessages.length - 3 > 1 ? 's' : '' }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.pinned-messages-panel {
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 8px 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.close-btn {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  transition: color 0.2s;
}

.close-btn:hover {
  color: var(--el-text-color-primary);
}

.pinned-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pinned-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  cursor: pointer;
  transition: background 0.2s;
}

.pinned-item:hover {
  background: var(--el-fill-color-light);
}

.message-info {
  flex: 1;
  min-width: 0;
}

.sender-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--el-color-primary);
  margin-bottom: 2px;
}

.message-preview {
  font-size: 12px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.actions {
  display: flex;
  gap: 8px;
  margin-left: 8px;
}

.action-icon {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  transition: color 0.2s;
}

.jump-icon:hover {
  color: var(--el-color-primary);
}

.unpin-icon:hover {
  color: var(--el-color-danger);
}

.more-indicator {
  text-align: center;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  padding: 4px;
}
</style>
