<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { messageApi } from '@/api/message'
import type { Message } from '@/types'
import { getErrorMessage } from '@/utils/errorHandler'

const props = defineProps<{
  conversationId: number
}>()

const emit = defineEmits<{
  (e: 'view-image', url: string): void
}>()

// State
type MediaType = 'image' | 'video' | 'file'
const mediaType = ref<MediaType>('image')
const mediaItems = ref<Message[]>([])
const loading = ref(false)
const page = ref(0)
const hasMore = ref(true)

// Computed
const isEmpty = computed(() => !loading.value && mediaItems.value.length === 0)

// Load media messages
async function loadMedia(reset = false) {
  if (loading.value) return
  if (!reset && !hasMore.value) return

  if (reset) {
    page.value = 0
    mediaItems.value = []
    hasMore.value = true
  }

  loading.value = true
  try {
    const items = await messageApi.getMediaMessages(
      props.conversationId,
      mediaType.value,
      page.value,
      20
    )

    if (items.length < 20) {
      hasMore.value = false
    }

    if (reset) {
      mediaItems.value = items
    } else {
      mediaItems.value.push(...items)
    }
    page.value++
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || 'Failed to load media')
  } finally {
    loading.value = false
  }
}

// Watch for media type changes
watch(mediaType, () => {
  loadMedia(true)
})

// Watch for conversation changes
watch(() => props.conversationId, () => {
  loadMedia(true)
})

function handleImageClick(item: Message) {
  const url = item.content
  emit('view-image', url)
}

function getFileName(item: Message): string {
  return item.metadata?.fileName || 'Unknown file'
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return 'Unknown size'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDuration(seconds?: number): string {
  if (!seconds) return ''
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

function downloadFile(item: Message) {
  const url = item.content
  const fileName = getFileName(item)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

onMounted(() => {
  loadMedia(true)
})
</script>

<template>
  <div class="photo-album-panel">
    <!-- Tab buttons -->
    <div class="media-tabs">
      <el-radio-group v-model="mediaType" size="small">
        <el-radio-button value="image">
          <el-icon><Picture /></el-icon>
          Photos
        </el-radio-button>
        <el-radio-button value="video">
          <el-icon><VideoCamera /></el-icon>
          Videos
        </el-radio-button>
        <el-radio-button value="file">
          <el-icon><Document /></el-icon>
          Files
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- Loading state -->
    <div v-if="loading && mediaItems.length === 0" class="loading-state">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>Loading...</span>
    </div>

    <!-- Empty state -->
    <el-empty v-else-if="isEmpty" :description="`No ${mediaType}s found`" />

    <!-- Image grid -->
    <div v-else-if="mediaType === 'image'" class="media-grid">
      <div
        v-for="item in mediaItems"
        :key="item.msgId"
        class="media-item image-item"
        @click="handleImageClick(item)"
      >
        <img :src="item.metadata?.thumbnailUrl || item.content" :alt="getFileName(item)" />
      </div>
    </div>

    <!-- Video grid -->
    <div v-else-if="mediaType === 'video'" class="media-grid">
      <div
        v-for="item in mediaItems"
        :key="item.msgId"
        class="media-item video-item"
      >
        <video
          :src="item.content"
          :poster="item.metadata?.thumbnailUrl"
          controls
          preload="metadata"
        ></video>
        <span v-if="item.metadata?.duration" class="video-duration">
          {{ formatDuration(item.metadata.duration) }}
        </span>
      </div>
    </div>

    <!-- File list -->
    <div v-else-if="mediaType === 'file'" class="file-list">
      <div
        v-for="item in mediaItems"
        :key="item.msgId"
        class="file-item"
        @click="downloadFile(item)"
      >
        <el-icon class="file-icon" :size="24"><Document /></el-icon>
        <div class="file-info">
          <span class="file-name">{{ getFileName(item) }}</span>
          <span class="file-size">{{ formatFileSize(item.metadata?.fileSize) }}</span>
        </div>
        <el-icon class="download-icon"><Download /></el-icon>
      </div>
    </div>

    <!-- Load more button -->
    <div v-if="hasMore && mediaItems.length > 0" class="load-more">
      <el-button text :loading="loading" @click="loadMedia(false)">
        Load more
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.photo-album-panel {
  padding: 12px;
}

.media-tabs {
  margin-bottom: 12px;
}

.media-tabs :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #909399;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
}

.media-item {
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 4px;
  cursor: pointer;
  background-color: #f5f7fa;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}

.image-item:hover img {
  transform: scale(1.05);
}

.video-item {
  position: relative;
}

.video-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-duration {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background-color: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 2px 6px;
  border-radius: 2px;
  font-size: 11px;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.file-item:hover {
  background-color: #ecf5ff;
}

.file-icon {
  color: #409eff;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  display: block;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.download-icon {
  color: #909399;
}

.load-more {
  text-align: center;
  margin-top: 12px;
}
</style>
