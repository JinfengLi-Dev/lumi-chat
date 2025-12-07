<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ArrowLeft, ArrowRight, Close, Download, ZoomIn, ZoomOut, RefreshLeft } from '@element-plus/icons-vue'

const props = defineProps<{
  visible: boolean
  images: string[]
  initialIndex?: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'change', index: number): void
}>()

const currentIndex = ref(props.initialIndex ?? 0)
const scale = ref(1)
const rotation = ref(0)
const isDragging = ref(false)
const translateX = ref(0)
const translateY = ref(0)
const startX = ref(0)
const startY = ref(0)

const currentImage = computed(() => props.images[currentIndex.value] || '')

const hasMultipleImages = computed(() => props.images.length > 1)

const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < props.images.length - 1)

const imageStyle = computed(() => ({
  transform: `translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value}) rotate(${rotation.value}deg)`,
  transition: isDragging.value ? 'none' : 'transform 0.2s ease',
}))

// Reset view state when image changes
watch(currentIndex, () => {
  resetView()
  emit('change', currentIndex.value)
})

// Reset index when visible changes
watch(() => props.visible, (visible) => {
  if (visible) {
    currentIndex.value = props.initialIndex ?? 0
    resetView()
  }
})

// Update index when initialIndex prop changes
watch(() => props.initialIndex, (index) => {
  if (index !== undefined) {
    currentIndex.value = index
  }
})

function resetView() {
  scale.value = 1
  rotation.value = 0
  translateX.value = 0
  translateY.value = 0
}

function close() {
  emit('update:visible', false)
}

function prev() {
  if (hasPrev.value) {
    currentIndex.value--
  }
}

function next() {
  if (hasNext.value) {
    currentIndex.value++
  }
}

function zoomIn() {
  scale.value = Math.min(scale.value + 0.25, 3)
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.25, 0.5)
}

function rotate() {
  rotation.value = (rotation.value + 90) % 360
}

async function download() {
  if (!currentImage.value) return

  try {
    const response = await fetch(currentImage.value)
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = url
    // Extract filename from URL or use default
    const urlParts = currentImage.value.split('/')
    link.download = urlParts[urlParts.length - 1] || 'image.jpg'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Failed to download image:', error)
  }
}

function handleMouseDown(e: MouseEvent) {
  if (e.button !== 0) return // Only left click
  isDragging.value = true
  startX.value = e.clientX - translateX.value
  startY.value = e.clientY - translateY.value
}

function handleMouseMove(e: MouseEvent) {
  if (!isDragging.value) return
  translateX.value = e.clientX - startX.value
  translateY.value = e.clientY - startY.value
}

function handleMouseUp() {
  isDragging.value = false
}

function handleWheel(e: WheelEvent) {
  e.preventDefault()
  if (e.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (!props.visible) return

  switch (e.key) {
    case 'Escape':
      close()
      break
    case 'ArrowLeft':
      prev()
      break
    case 'ArrowRight':
      next()
      break
    case '+':
    case '=':
      zoomIn()
      break
    case '-':
      zoomOut()
      break
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  document.addEventListener('mouseup', handleMouseUp)
  document.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('mouseup', handleMouseUp)
  document.removeEventListener('mousemove', handleMouseMove)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="image-lightbox-overlay" @click.self="close">
        <!-- Toolbar -->
        <div class="lightbox-toolbar">
          <div class="toolbar-left">
            <span v-if="hasMultipleImages" class="image-counter">
              {{ currentIndex + 1 }} / {{ images.length }}
            </span>
          </div>
          <div class="toolbar-actions">
            <button class="toolbar-btn" @click="zoomOut" title="Zoom Out">
              <el-icon><ZoomOut /></el-icon>
            </button>
            <button class="toolbar-btn" @click="zoomIn" title="Zoom In">
              <el-icon><ZoomIn /></el-icon>
            </button>
            <button class="toolbar-btn" @click="rotate" title="Rotate">
              <el-icon><RefreshLeft /></el-icon>
            </button>
            <button class="toolbar-btn" @click="download" title="Download">
              <el-icon><Download /></el-icon>
            </button>
            <button class="toolbar-btn close-btn" @click="close" title="Close">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>

        <!-- Navigation arrows -->
        <button
          v-if="hasMultipleImages && hasPrev"
          class="nav-btn nav-prev"
          @click="prev"
        >
          <el-icon :size="32"><ArrowLeft /></el-icon>
        </button>

        <button
          v-if="hasMultipleImages && hasNext"
          class="nav-btn nav-next"
          @click="next"
        >
          <el-icon :size="32"><ArrowRight /></el-icon>
        </button>

        <!-- Image container -->
        <div
          class="lightbox-content"
          @mousedown="handleMouseDown"
          @wheel="handleWheel"
        >
          <img
            v-if="currentImage"
            :src="currentImage"
            :style="imageStyle"
            class="lightbox-image"
            draggable="false"
            alt="Preview"
          />
        </div>

        <!-- Thumbnail strip for multiple images -->
        <div v-if="hasMultipleImages" class="thumbnail-strip">
          <div
            v-for="(img, index) in images"
            :key="index"
            class="thumbnail"
            :class="{ active: index === currentIndex }"
            @click="currentIndex = index"
          >
            <img :src="img" alt="" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.image-lightbox-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background-color: rgba(0, 0, 0, 0.9);
  display: flex;
  flex-direction: column;
}

.lightbox-toolbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.5), transparent);
  z-index: 10;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.image-counter {
  color: white;
  font-size: 14px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.toolbar-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: background-color 0.2s;
}

.toolbar-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.close-btn {
  margin-left: 8px;
}

.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  border: none;
  color: white;
  cursor: pointer;
  z-index: 10;
  transition: background-color 0.2s;
}

.nav-btn:hover {
  background: rgba(0, 0, 0, 0.5);
}

.nav-prev {
  left: 0;
  border-radius: 0 8px 8px 0;
}

.nav-next {
  right: 0;
  border-radius: 8px 0 0 8px;
}

.lightbox-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: grab;
}

.lightbox-content:active {
  cursor: grabbing;
}

.lightbox-image {
  max-width: 90%;
  max-height: 80vh;
  object-fit: contain;
  user-select: none;
}

.thumbnail-strip {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.5), transparent);
  overflow-x: auto;
}

.thumbnail {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.2s, transform 0.2s;
  border: 2px solid transparent;
}

.thumbnail:hover {
  opacity: 0.8;
}

.thumbnail.active {
  opacity: 1;
  border-color: white;
  transform: scale(1.1);
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Fade transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Scrollbar for thumbnails */
.thumbnail-strip::-webkit-scrollbar {
  height: 4px;
}

.thumbnail-strip::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.thumbnail-strip::-webkit-scrollbar-track {
  background-color: transparent;
}
</style>
