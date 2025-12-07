<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import {
  Top,
  Bell,
  BellFilled,
  Delete,
  Check,
  Reading,
} from '@element-plus/icons-vue'
import type { Conversation } from '@/types'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  conversation: Conversation | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'pin', conversation: Conversation): void
  (e: 'unpin', conversation: Conversation): void
  (e: 'mute', conversation: Conversation): void
  (e: 'unmute', conversation: Conversation): void
  (e: 'mark-read', conversation: Conversation): void
  (e: 'delete', conversation: Conversation): void
}>()

const menuRef = ref<HTMLElement>()

const menuStyle = computed(() => ({
  left: `${props.x}px`,
  top: `${props.y}px`,
}))

const isPinned = computed(() => props.conversation?.isPinned ?? false)
const isMuted = computed(() => props.conversation?.isMuted ?? false)
const hasUnread = computed(() => (props.conversation?.unreadCount ?? 0) > 0)

function handlePin() {
  if (!props.conversation) return
  if (isPinned.value) {
    emit('unpin', props.conversation)
  } else {
    emit('pin', props.conversation)
  }
  close()
}

function handleMute() {
  if (!props.conversation) return
  if (isMuted.value) {
    emit('unmute', props.conversation)
  } else {
    emit('mute', props.conversation)
  }
  close()
}

function handleMarkRead() {
  if (!props.conversation) return
  emit('mark-read', props.conversation)
  close()
}

function handleDelete() {
  if (!props.conversation) return
  emit('delete', props.conversation)
  close()
}

function close() {
  emit('update:visible', false)
}

function handleClickOutside(e: MouseEvent) {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    close()
  }
}

watch(() => props.visible, (visible) => {
  if (visible) {
    setTimeout(() => {
      document.addEventListener('click', handleClickOutside)
    }, 0)
  } else {
    document.removeEventListener('click', handleClickOutside)
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="visible && conversation"
        ref="menuRef"
        class="conversation-context-menu"
        :style="menuStyle"
      >
        <!-- Pin/Unpin -->
        <div class="menu-item" @click="handlePin">
          <el-icon><Top /></el-icon>
          <span>{{ isPinned ? 'Unpin' : 'Pin to top' }}</span>
          <el-icon v-if="isPinned" class="check-icon"><Check /></el-icon>
        </div>

        <!-- Mute/Unmute -->
        <div class="menu-item" @click="handleMute">
          <el-icon>
            <BellFilled v-if="!isMuted" />
            <Bell v-else />
          </el-icon>
          <span>{{ isMuted ? 'Unmute' : 'Mute notifications' }}</span>
          <el-icon v-if="isMuted" class="check-icon"><Check /></el-icon>
        </div>

        <!-- Mark as read -->
        <div
          v-if="hasUnread"
          class="menu-item"
          @click="handleMarkRead"
        >
          <el-icon><Reading /></el-icon>
          <span>Mark as read</span>
        </div>

        <div class="menu-divider"></div>

        <!-- Delete -->
        <div class="menu-item danger" @click="handleDelete">
          <el-icon><Delete /></el-icon>
          <span>Delete conversation</span>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.conversation-context-menu {
  position: fixed;
  z-index: 2000;
  min-width: 180px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 4px 0;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.menu-item:hover {
  background-color: var(--el-fill-color-light);
}

.menu-item .el-icon {
  font-size: 16px;
  color: var(--el-text-color-secondary);
}

.menu-item.danger {
  color: var(--el-color-danger);
}

.menu-item.danger .el-icon {
  color: var(--el-color-danger);
}

.menu-item .check-icon {
  margin-left: auto;
  color: var(--el-color-primary);
}

.menu-divider {
  height: 1px;
  background: var(--el-border-color-lighter);
  margin: 4px 0;
}

/* Fade transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
