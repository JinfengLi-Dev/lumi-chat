<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { conversationApi } from '@/api'
import { getErrorMessage } from '@/utils/errorHandler'
import type { Conversation, Message } from '@/types'

const props = defineProps<{
  modelValue: boolean
  message: Message | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'forward', conversationIds: number[]): void
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const conversations = ref<Conversation[]>([])
const selectedConversations = ref<number[]>([])
const searchQuery = ref('')
const isLoading = ref(false)
const isForwarding = ref(false)

const filteredConversations = computed(() => {
  if (!searchQuery.value) return conversations.value
  const query = searchQuery.value.toLowerCase()
  return conversations.value.filter((conv) => {
    const name = conv.group?.name || conv.targetUser?.nickname || ''
    return name.toLowerCase().includes(query)
  })
})

const messagePreview = computed(() => {
  if (!props.message) return ''

  switch (props.message.msgType) {
    case 'text':
      return props.message.content.length > 50
        ? props.message.content.slice(0, 50) + '...'
        : props.message.content
    case 'image':
      return '[Image]'
    case 'file':
      return `[File] ${props.message.metadata?.fileName || ''}`
    case 'voice':
      return '[Voice Message]'
    case 'video':
      return '[Video]'
    case 'location':
      return '[Location]'
    default:
      return `[${props.message.msgType}]`
  }
})

async function loadConversations() {
  isLoading.value = true
  try {
    conversations.value = await conversationApi.getConversations()
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    isLoading.value = false
  }
}

function toggleConversation(convId: number) {
  const index = selectedConversations.value.indexOf(convId)
  if (index > -1) {
    selectedConversations.value.splice(index, 1)
  } else {
    selectedConversations.value.push(convId)
  }
}

async function handleForward() {
  if (selectedConversations.value.length === 0) {
    ElMessage.warning('Please select at least one conversation')
    return
  }

  isForwarding.value = true
  try {
    emit('forward', selectedConversations.value)
    ElMessage.success(`Message forwarded to ${selectedConversations.value.length} conversation(s)`)
    handleClose()
  } finally {
    isForwarding.value = false
  }
}

function handleClose() {
  selectedConversations.value = []
  searchQuery.value = ''
  dialogVisible.value = false
}

function getConversationName(conv: Conversation): string {
  return conv.group?.name || conv.targetUser?.nickname || 'Unknown'
}

function getConversationAvatar(conv: Conversation): string | undefined {
  return conv.group?.avatar || conv.targetUser?.avatar
}

watch(dialogVisible, (visible) => {
  if (visible) {
    loadConversations()
  }
})
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="Forward Message"
    width="480px"
    :before-close="handleClose"
    destroy-on-close
  >
    <!-- Message Preview -->
    <div class="message-preview">
      <div class="preview-label">Forwarding:</div>
      <div class="preview-content">{{ messagePreview }}</div>
    </div>

    <!-- Search -->
    <el-input
      v-model="searchQuery"
      placeholder="Search conversations"
      prefix-icon="Search"
      clearable
      class="search-input"
    />

    <!-- Selected count -->
    <div class="selected-count">
      Selected: {{ selectedConversations.length }} conversation(s)
    </div>

    <!-- Conversation List -->
    <div v-loading="isLoading" class="conversation-list">
      <div v-if="filteredConversations.length === 0 && !isLoading" class="empty-state">
        <el-empty description="No conversations found" />
      </div>

      <div
        v-for="conv in filteredConversations"
        :key="conv.id"
        class="conversation-item"
        :class="{ selected: selectedConversations.includes(conv.id) }"
        @click="toggleConversation(conv.id)"
      >
        <el-checkbox
          :model-value="selectedConversations.includes(conv.id)"
          @click.stop
          @change="toggleConversation(conv.id)"
        />
        <el-avatar :src="getConversationAvatar(conv)" :size="40" shape="square">
          {{ getConversationName(conv).charAt(0) }}
        </el-avatar>
        <div class="conv-info">
          <div class="conv-name">{{ getConversationName(conv) }}</div>
          <div class="conv-type">
            {{ conv.type === 'group' ? 'Group' : 'Private' }}
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">Cancel</el-button>
      <el-button
        type="primary"
        :loading="isForwarding"
        :disabled="selectedConversations.length === 0"
        @click="handleForward"
      >
        Forward ({{ selectedConversations.length }})
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.message-preview {
  margin-bottom: 16px;
  padding: 12px;
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
}

.preview-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.preview-content {
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.search-input {
  margin-bottom: 12px;
}

.selected-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.conversation-list {
  max-height: 350px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  gap: 12px;
  transition: background-color 0.2s;
}

.conversation-item:hover {
  background-color: var(--el-fill-color-light);
}

.conversation-item.selected {
  background-color: var(--el-color-primary-light-9);
}

.conv-info {
  flex: 1;
}

.conv-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.conv-type {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.empty-state {
  padding: 32px 0;
}
</style>
