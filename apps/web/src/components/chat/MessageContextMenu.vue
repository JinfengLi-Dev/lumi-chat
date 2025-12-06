<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CopyDocument,
  RefreshLeft,
  Promotion,
  Delete,
  ChatLineSquare,
} from '@element-plus/icons-vue'
import type { Message } from '@/types'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  message: Message | null
  isSelf: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'copy'): void
  (e: 'recall'): void
  (e: 'forward'): void
  (e: 'delete'): void
  (e: 'quote'): void
}>()

const menuRef = ref<HTMLDivElement>()

const menuStyle = computed(() => ({
  left: `${props.x}px`,
  top: `${props.y}px`,
}))

const canRecall = computed(() => {
  if (!props.message || !props.isSelf || props.message.recalledAt) return false

  // Check if message is within 2 minutes
  const serverTime = new Date(props.message.serverCreatedAt).getTime()
  const now = Date.now()
  const twoMinutes = 2 * 60 * 1000

  return now - serverTime < twoMinutes
})

const isRecalled = computed(() => {
  return props.message?.recalledAt != null
})

function handleClickOutside(e: MouseEvent) {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    emit('update:visible', false)
  }
}

function handleCopy() {
  if (!props.message) return

  let textToCopy = ''

  switch (props.message.msgType) {
    case 'text':
      textToCopy = props.message.content
      break
    case 'image':
    case 'file':
      textToCopy = props.message.content // URL
      break
    default:
      textToCopy = props.message.content
  }

  navigator.clipboard.writeText(textToCopy).then(() => {
    ElMessage.success('Copied to clipboard')
  }).catch(() => {
    ElMessage.error('Failed to copy')
  })

  emit('copy')
  emit('update:visible', false)
}

async function handleRecall() {
  if (!props.message || !canRecall.value) return

  try {
    await ElMessageBox.confirm(
      'Are you sure you want to recall this message? This action cannot be undone.',
      'Recall Message',
      {
        confirmButtonText: 'Recall',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )

    emit('recall')
  } catch {
    // User cancelled
  }

  emit('update:visible', false)
}

function handleForward() {
  emit('forward')
  emit('update:visible', false)
}

async function handleDelete() {
  if (!props.message) return

  try {
    await ElMessageBox.confirm(
      'Delete this message? It will only be removed from your device.',
      'Delete Message',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )

    emit('delete')
  } catch {
    // User cancelled
  }

  emit('update:visible', false)
}

function handleQuote() {
  emit('quote')
  emit('update:visible', false)
}

watch(() => props.visible, (visible) => {
  if (visible) {
    document.addEventListener('click', handleClickOutside)
    document.addEventListener('contextmenu', handleClickOutside)
  } else {
    document.removeEventListener('click', handleClickOutside)
    document.removeEventListener('contextmenu', handleClickOutside)
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('contextmenu', handleClickOutside)
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible && message && !isRecalled"
      ref="menuRef"
      class="context-menu"
      :style="menuStyle"
    >
      <div class="context-menu-item" @click="handleCopy">
        <el-icon><CopyDocument /></el-icon>
        <span>Copy</span>
      </div>

      <div class="context-menu-item" @click="handleQuote">
        <el-icon><ChatLineSquare /></el-icon>
        <span>Quote</span>
      </div>

      <div class="context-menu-item" @click="handleForward">
        <el-icon><Promotion /></el-icon>
        <span>Forward</span>
      </div>

      <div
        v-if="isSelf && canRecall"
        class="context-menu-item warning"
        @click="handleRecall"
      >
        <el-icon><RefreshLeft /></el-icon>
        <span>Recall</span>
      </div>

      <div class="context-menu-divider"></div>

      <div class="context-menu-item danger" @click="handleDelete">
        <el-icon><Delete /></el-icon>
        <span>Delete</span>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 150px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 6px 0;
  animation: fadeIn 0.15s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  color: var(--el-text-color-primary);
  transition: background-color 0.2s;
}

.context-menu-item:hover {
  background-color: var(--el-fill-color-light);
}

.context-menu-item.warning {
  color: var(--el-color-warning);
}

.context-menu-item.warning:hover {
  background-color: var(--el-color-warning-light-9);
}

.context-menu-item.danger {
  color: var(--el-color-danger);
}

.context-menu-item.danger:hover {
  background-color: var(--el-color-danger-light-9);
}

.context-menu-divider {
  height: 1px;
  background-color: var(--el-border-color-lighter);
  margin: 6px 0;
}
</style>
