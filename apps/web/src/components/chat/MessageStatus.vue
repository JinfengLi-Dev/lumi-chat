<script setup lang="ts">
import { computed } from 'vue'

export type MessageStatusType = 'sending' | 'sent' | 'delivered' | 'read' | 'failed'

const props = defineProps<{
  status: MessageStatusType
  // Show timestamp alongside status
  showTime?: boolean
  time?: string
}>()

const statusConfig = computed(() => {
  switch (props.status) {
    case 'sending':
      return {
        icon: 'clock',
        color: 'var(--el-text-color-placeholder)',
        title: 'Sending...',
      }
    case 'sent':
      return {
        icon: 'single-check',
        color: 'var(--el-text-color-secondary)',
        title: 'Sent',
      }
    case 'delivered':
      return {
        icon: 'double-check',
        color: 'var(--el-text-color-secondary)',
        title: 'Delivered',
      }
    case 'read':
      return {
        icon: 'double-check',
        color: 'var(--el-color-danger)',
        title: 'Read',
      }
    case 'failed':
      return {
        icon: 'failed',
        color: 'var(--el-color-danger)',
        title: 'Failed to send',
      }
    default:
      return {
        icon: 'none',
        color: 'transparent',
        title: '',
      }
  }
})

const formattedTime = computed(() => {
  if (!props.time) return ''
  try {
    return new Date(props.time).toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return props.time
  }
})
</script>

<template>
  <span class="message-status" :title="statusConfig.title">
    <span v-if="showTime && time" class="status-time">{{ formattedTime }}</span>

    <!-- Sending indicator (clock/spinner) -->
    <span v-if="status === 'sending'" class="status-icon sending">
      <svg viewBox="0 0 24 24" width="14" height="14">
        <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2" stroke-dasharray="30 70" />
      </svg>
    </span>

    <!-- Single check (sent) -->
    <span v-else-if="status === 'sent'" class="status-icon" :style="{ color: statusConfig.color }">
      <svg viewBox="0 0 24 24" width="16" height="16">
        <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/>
      </svg>
    </span>

    <!-- Double check (delivered/read) -->
    <span v-else-if="status === 'delivered' || status === 'read'" class="status-icon" :style="{ color: statusConfig.color }">
      <svg viewBox="0 0 24 24" width="20" height="16">
        <path fill="currentColor" d="M18 7l-1.41-1.41-6.34 6.34 1.41 1.41L18 7zm4.24-1.41L11.66 16.17 7.48 12l-1.41 1.41L11.66 19l12-12-1.42-1.41zM.41 13.41L6 19l1.41-1.41L1.83 12 .41 13.41z"/>
      </svg>
    </span>

    <!-- Failed indicator -->
    <span v-else-if="status === 'failed'" class="status-icon failed" :style="{ color: statusConfig.color }">
      <svg viewBox="0 0 24 24" width="14" height="14">
        <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
      </svg>
    </span>
  </span>
</template>

<style scoped>
.message-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
}

.status-time {
  color: var(--el-text-color-secondary);
}

.status-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.status-icon.sending {
  animation: spin 1s linear infinite;
  color: var(--el-text-color-placeholder);
}

.status-icon.failed {
  cursor: pointer;
}

.status-icon.failed:hover {
  opacity: 0.8;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
