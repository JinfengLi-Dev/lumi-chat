<script setup lang="ts">
import { computed } from 'vue'
import { Download, Document, FolderOpened, Refresh } from '@element-plus/icons-vue'
import { fileApi } from '@/api/file'
import type { Message } from '@/types'

const props = defineProps<{
  message: Message
}>()

const emit = defineEmits<{
  retry: [message: Message]
}>()

const fileName = computed(() => props.message.metadata?.fileName || 'File')
const fileSize = computed(() => props.message.metadata?.fileSize || 0)
const fileId = computed(() => props.message.metadata?.fileId || extractFileId(props.message.content))
const expiresAt = computed(() => props.message.metadata?.expiresAt)
const isUploading = computed(() => props.message.status === 'sending')
const isFailed = computed(() => props.message.status === 'failed')

// Extract file ID from URL if not in metadata
function extractFileId(url: string): string {
  if (!url) return ''
  const match = url.match(/\/files\/([^/]+)/)
  return match ? match[1] : ''
}

// Format file size
const formattedSize = computed(() => {
  const size = fileSize.value
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / (1024 * 1024)).toFixed(1)} MB`
  return `${(size / (1024 * 1024 * 1024)).toFixed(1)} GB`
})

// Format expiration time
const expiresIn = computed(() => {
  if (!expiresAt.value) return null
  const expires = new Date(expiresAt.value)
  const now = new Date()
  if (expires <= now) return 'Expired'

  const diffMs = expires.getTime() - now.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays === 0) return 'today'
  if (diffDays === 1) return '1 day'
  if (diffDays < 7) return `${diffDays} days`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks`
  return `${Math.floor(diffDays / 30)} months`
})

// Get file extension
const fileExtension = computed(() => {
  const name = fileName.value
  const lastDot = name.lastIndexOf('.')
  return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : ''
})

// Get icon color based on file type
const iconColor = computed(() => {
  const ext = fileExtension.value
  const colorMap: Record<string, string> = {
    pdf: '#E53935',
    doc: '#1565C0',
    docx: '#1565C0',
    xls: '#2E7D32',
    xlsx: '#2E7D32',
    ppt: '#D84315',
    pptx: '#D84315',
    zip: '#FFA000',
    rar: '#FFA000',
    '7z': '#FFA000',
    txt: '#757575',
  }
  return colorMap[ext] || 'var(--el-color-primary)'
})

// Get file type label
const fileTypeLabel = computed(() => {
  const ext = fileExtension.value
  const labelMap: Record<string, string> = {
    pdf: 'PDF',
    doc: 'Word',
    docx: 'Word',
    xls: 'Excel',
    xlsx: 'Excel',
    ppt: 'PowerPoint',
    pptx: 'PowerPoint',
    zip: 'ZIP',
    rar: 'RAR',
    '7z': '7Z',
    txt: 'Text',
  }
  return labelMap[ext] || ext.toUpperCase() || 'File'
})

function handleClick() {
  if (isUploading.value || isFailed.value || !fileId.value) return
  handleOpen()
}

function handleOpen() {
  if (!fileId.value) return
  fileApi.openFile(fileId.value)
}

function handleDownload() {
  if (!fileId.value) return
  fileApi.downloadFile(fileId.value, fileName.value)
}

function handleRetry() {
  emit('retry', props.message)
}
</script>

<template>
  <div
    class="file-message"
    :class="{ clickable: !isUploading && !isFailed && fileId }"
    @click="handleClick"
  >
    <!-- File Icon -->
    <div class="file-icon" :style="{ backgroundColor: iconColor + '20' }">
      <el-icon :size="28" :color="iconColor">
        <Document />
      </el-icon>
      <span class="file-type-label" :style="{ color: iconColor }">
        {{ fileTypeLabel }}
      </span>
    </div>

    <!-- File Info -->
    <div class="file-info">
      <div class="file-name" :title="fileName">{{ fileName }}</div>
      <div class="file-meta">
        <span class="file-size">{{ formattedSize }}</span>
        <span v-if="expiresIn" class="expires-in" :class="{ expired: expiresIn === 'Expired' }">
          {{ expiresIn === 'Expired' ? 'Expired' : `Expires ${expiresIn}` }}
        </span>
      </div>
    </div>

    <!-- Actions -->
    <div class="file-actions">
      <!-- Progress for uploading -->
      <div v-if="isUploading" class="uploading-indicator">
        <el-icon class="is-loading">
          <Refresh />
        </el-icon>
      </div>

      <!-- Retry for failed -->
      <el-button
        v-else-if="isFailed"
        type="danger"
        circle
        size="small"
        @click.stop="handleRetry"
      >
        <el-icon><Refresh /></el-icon>
      </el-button>

      <!-- Actions for completed -->
      <template v-else>
        <el-button
          type="primary"
          circle
          size="small"
          title="Open"
          @click.stop="handleOpen"
        >
          <el-icon><FolderOpened /></el-icon>
        </el-button>
        <el-button
          circle
          size="small"
          title="Download"
          @click.stop="handleDownload"
        >
          <el-icon><Download /></el-icon>
        </el-button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.file-message {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  min-width: 240px;
  max-width: 320px;
}

.file-message.clickable {
  cursor: pointer;
  transition: background-color 0.2s;
}

.file-message.clickable:hover {
  background: var(--el-fill-color);
}

.file-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  gap: 2px;
}

.file-type-label {
  font-size: 8px;
  font-weight: 600;
  text-transform: uppercase;
}

.file-info {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.file-size {
  flex-shrink: 0;
}

.expires-in {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.expires-in.expired {
  color: var(--el-color-danger);
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.uploading-indicator {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
}

.is-loading {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
