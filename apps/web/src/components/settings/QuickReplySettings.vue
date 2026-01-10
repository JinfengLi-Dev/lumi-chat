<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Rank } from '@element-plus/icons-vue'
import { useQuickReplyStore } from '@/stores/quickReply'
import type { QuickReply } from '@/api/quickReply'

const quickReplyStore = useQuickReplyStore()

const quickReplies = computed(() => quickReplyStore.quickReplies)
const loading = computed(() => quickReplyStore.loading)

const newContent = ref('')
const editingId = ref<number | null>(null)
const editingContent = ref('')
const saving = ref(false)

onMounted(() => {
  quickReplyStore.fetchQuickReplies()
})

async function handleAdd() {
  if (!newContent.value.trim()) {
    ElMessage.warning('Please enter content')
    return
  }

  if (newContent.value.length > 200) {
    ElMessage.warning('Content must not exceed 200 characters')
    return
  }

  saving.value = true
  try {
    await quickReplyStore.createQuickReply(newContent.value.trim())
    newContent.value = ''
    ElMessage.success('Quick reply added')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to add quick reply'
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}

function startEdit(reply: QuickReply) {
  editingId.value = reply.id
  editingContent.value = reply.content
}

function cancelEdit() {
  editingId.value = null
  editingContent.value = ''
}

async function saveEdit() {
  if (!editingId.value || !editingContent.value.trim()) return

  if (editingContent.value.length > 200) {
    ElMessage.warning('Content must not exceed 200 characters')
    return
  }

  saving.value = true
  try {
    await quickReplyStore.updateQuickReply(editingId.value, editingContent.value.trim())
    cancelEdit()
    ElMessage.success('Quick reply updated')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to update quick reply'
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('Are you sure you want to delete this quick reply?', 'Confirm', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning',
    })

    await quickReplyStore.deleteQuickReply(id)
    ElMessage.success('Quick reply deleted')
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : 'Failed to delete quick reply'
      ElMessage.error(message)
    }
  }
}

// Drag and drop reordering
const draggedItem = ref<QuickReply | null>(null)

function handleDragStart(reply: QuickReply) {
  draggedItem.value = reply
}

function handleDragOver(event: DragEvent) {
  event.preventDefault()
}

async function handleDrop(targetReply: QuickReply) {
  if (!draggedItem.value || draggedItem.value.id === targetReply.id) {
    draggedItem.value = null
    return
  }

  const items = [...quickReplies.value]
  const draggedIndex = items.findIndex((r) => r.id === draggedItem.value!.id)
  const targetIndex = items.findIndex((r) => r.id === targetReply.id)

  items.splice(draggedIndex, 1)
  items.splice(targetIndex, 0, draggedItem.value)

  const newOrder = items.map((r) => r.id)

  try {
    await quickReplyStore.reorderQuickReplies(newOrder)
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to reorder'
    ElMessage.error(message)
  }

  draggedItem.value = null
}

function handleDragEnd() {
  draggedItem.value = null
}
</script>

<template>
  <div class="quick-reply-settings">
    <div class="settings-header">
      <h3>Quick Replies</h3>
      <p class="settings-description">
        Create quick reply templates for fast responses. Maximum 20 templates, each up to 200
        characters.
      </p>
    </div>

    <div class="add-form">
      <el-input
        v-model="newContent"
        placeholder="Enter quick reply content"
        maxlength="200"
        show-word-limit
        clearable
        @keyup.enter="handleAdd"
      />
      <el-button type="primary" :icon="Plus" :loading="saving" @click="handleAdd">Add</el-button>
    </div>

    <div v-loading="loading" class="replies-list">
      <div v-if="quickReplies.length === 0" class="empty-state">
        <p>No quick replies yet. Add your first one above!</p>
      </div>

      <div
        v-for="reply in quickReplies"
        :key="reply.id"
        class="reply-item"
        draggable="true"
        @dragstart="handleDragStart(reply)"
        @dragover="handleDragOver"
        @drop="handleDrop(reply)"
        @dragend="handleDragEnd"
      >
        <el-icon class="drag-handle"><Rank /></el-icon>

        <template v-if="editingId === reply.id">
          <el-input
            v-model="editingContent"
            size="small"
            maxlength="200"
            show-word-limit
            @keyup.enter="saveEdit"
            @keyup.escape="cancelEdit"
          />
          <el-button size="small" type="primary" :loading="saving" @click="saveEdit">
            Save
          </el-button>
          <el-button size="small" @click="cancelEdit">Cancel</el-button>
        </template>

        <template v-else>
          <span class="reply-content" @click="startEdit(reply)">{{ reply.content }}</span>
          <el-button
            size="small"
            type="danger"
            :icon="Delete"
            circle
            @click="handleDelete(reply.id)"
          />
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.quick-reply-settings {
  padding: 20px;
}

.settings-header {
  margin-bottom: 20px;
}

.settings-header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}

.settings-description {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.add-form {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.add-form .el-input {
  flex: 1;
}

.replies-list {
  min-height: 100px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--el-text-color-secondary);
}

.reply-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  margin-bottom: 8px;
  background: var(--el-bg-color);
  cursor: grab;
  transition: all 0.2s ease;
}

.reply-item:hover {
  border-color: var(--el-border-color);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.reply-item:active {
  cursor: grabbing;
}

.drag-handle {
  color: var(--el-text-color-placeholder);
  cursor: grab;
}

.reply-content {
  flex: 1;
  cursor: text;
  padding: 4px 8px;
  border-radius: 4px;
}

.reply-content:hover {
  background: var(--el-fill-color-light);
}

.reply-item .el-input {
  flex: 1;
}
</style>
