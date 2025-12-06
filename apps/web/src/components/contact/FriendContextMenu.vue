<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Lock, Unlock, Delete } from '@element-plus/icons-vue'
import { friendApi } from '@/api/friend'
import type { Friend } from '@/types'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  friend: Friend | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'remark-updated', friendId: number, remark: string): void
  (e: 'blocked', friendId: number): void
  (e: 'unblocked', friendId: number): void
  (e: 'deleted', friendId: number): void
}>()

const showRemarkDialog = ref(false)
const newRemark = ref('')
const isBlocked = ref(false)

const menuStyle = computed(() => ({
  position: 'fixed' as const,
  left: `${props.x}px`,
  top: `${props.y}px`,
  zIndex: 9999,
}))

function closeMenu() {
  emit('update:visible', false)
}

function handleSetRemark() {
  if (!props.friend) return
  newRemark.value = props.friend.remark || ''
  showRemarkDialog.value = true
  closeMenu()
}

async function saveRemark() {
  if (!props.friend) return

  try {
    await friendApi.updateRemark(props.friend.id, newRemark.value)
    ElMessage.success('Remark updated')
    emit('remark-updated', props.friend.id, newRemark.value)
    showRemarkDialog.value = false
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to update remark'
    ElMessage.error(message)
  }
}

async function handleBlock() {
  if (!props.friend) return

  try {
    await ElMessageBox.confirm(
      `Are you sure you want to block ${props.friend.remark || props.friend.nickname}? You will not receive messages from them.`,
      'Block Friend',
      {
        confirmButtonText: 'Block',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )

    await friendApi.blockFriend(props.friend.id)
    ElMessage.success('Friend blocked')
    emit('blocked', props.friend.id)
    closeMenu()
  } catch (error) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : 'Failed to block friend'
      ElMessage.error(message)
    }
    closeMenu()
  }
}

async function handleUnblock() {
  if (!props.friend) return

  try {
    await friendApi.unblockFriend(props.friend.id)
    ElMessage.success('Friend unblocked')
    emit('unblocked', props.friend.id)
    closeMenu()
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to unblock friend'
    ElMessage.error(message)
    closeMenu()
  }
}

async function handleDelete() {
  if (!props.friend) return

  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete ${props.friend.remark || props.friend.nickname} from your friends list? This action cannot be undone.`,
      'Delete Friend',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        type: 'error',
      }
    )

    await friendApi.deleteFriend(props.friend.id)
    ElMessage.success('Friend deleted')
    emit('deleted', props.friend.id)
    closeMenu()
  } catch (error) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : 'Failed to delete friend'
      ElMessage.error(message)
    }
    closeMenu()
  }
}

// Close on click outside
function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.friend-context-menu')) {
    closeMenu()
  }
}

watch(() => props.visible, (visible) => {
  if (visible) {
    document.addEventListener('click', handleClickOutside)
  } else {
    document.removeEventListener('click', handleClickOutside)
  }
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible && friend"
      class="friend-context-menu"
      :style="menuStyle"
    >
      <div class="context-menu-item" @click="handleSetRemark">
        <el-icon><Edit /></el-icon>
        <span>Set Remark</span>
      </div>

      <div v-if="isBlocked" class="context-menu-item" @click="handleUnblock">
        <el-icon><Unlock /></el-icon>
        <span>Unblock</span>
      </div>
      <div v-else class="context-menu-item" @click="handleBlock">
        <el-icon><Lock /></el-icon>
        <span>Block</span>
      </div>

      <div class="context-menu-divider" />

      <div class="context-menu-item danger" @click="handleDelete">
        <el-icon><Delete /></el-icon>
        <span>Delete Friend</span>
      </div>
    </div>

    <!-- Set Remark Dialog -->
    <el-dialog
      v-model="showRemarkDialog"
      title="Set Remark"
      width="360px"
    >
      <el-input
        v-model="newRemark"
        placeholder="Enter remark name"
        maxlength="20"
        show-word-limit
      />
      <template #footer>
        <el-button @click="showRemarkDialog = false">Cancel</el-button>
        <el-button type="primary" @click="saveRemark">Save</el-button>
      </template>
    </el-dialog>
  </Teleport>
</template>

<style scoped>
.friend-context-menu {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 4px 0;
  min-width: 160px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  color: var(--el-text-color-regular);
  transition: background-color 0.2s;
}

.context-menu-item:hover {
  background-color: var(--el-fill-color-light);
}

.context-menu-item.danger {
  color: var(--el-color-danger);
}

.context-menu-divider {
  height: 1px;
  background-color: var(--el-border-color-lighter);
  margin: 4px 0;
}
</style>
