<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue'
import { Download, FullScreen, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import type { Message } from '@/types'

const props = defineProps<{
  message: Message
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const containerRef = ref<HTMLDivElement | null>(null)
const isPlaying = ref(false)
const isFullscreen = ref(false)
const showControls = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const isLoading = ref(false)
const hasStarted = ref(false)

const videoUrl = computed(() => props.message.content)
const thumbnailUrl = computed(() => props.message.metadata?.thumbnailUrl || '')
const messageDuration = computed(() => props.message.metadata?.duration || 0)

const progress = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const displayTime = computed(() => {
  const current = formatTime(currentTime.value)
  const total = formatTime(duration.value || messageDuration.value)
  return `${current} / ${total}`
})

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

function togglePlay() {
  if (!videoRef.value) return

  if (isPlaying.value) {
    videoRef.value.pause()
  } else {
    hasStarted.value = true
    videoRef.value.play()
  }
}

function handlePlay() {
  isPlaying.value = true
}

function handlePause() {
  isPlaying.value = false
}

function handleEnded() {
  isPlaying.value = false
  hasStarted.value = false
  currentTime.value = 0
}

function handleTimeUpdate() {
  if (videoRef.value) {
    currentTime.value = videoRef.value.currentTime
  }
}

function handleLoadedMetadata() {
  if (videoRef.value) {
    duration.value = videoRef.value.duration
  }
  isLoading.value = false
}

function handleLoadStart() {
  isLoading.value = true
}

function handleCanPlay() {
  isLoading.value = false
}

function handleProgressClick(event: MouseEvent) {
  if (!videoRef.value) return

  const progressBar = event.currentTarget as HTMLElement
  const rect = progressBar.getBoundingClientRect()
  const clickPosition = event.clientX - rect.left
  const percentage = clickPosition / rect.width
  const newTime = percentage * duration.value

  videoRef.value.currentTime = newTime
  currentTime.value = newTime
}

async function toggleFullscreen() {
  if (!containerRef.value) return

  if (!isFullscreen.value) {
    try {
      await containerRef.value.requestFullscreen()
      isFullscreen.value = true
    } catch {
      // Fullscreen not supported
    }
  } else {
    try {
      await document.exitFullscreen()
      isFullscreen.value = false
    } catch {
      // Already not fullscreen
    }
  }
}

function handleFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

function handleDownload() {
  const link = document.createElement('a')
  link.href = videoUrl.value
  link.download = `video-${props.message.msgId}.mp4`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function handleMouseEnter() {
  showControls.value = true
}

function handleMouseLeave() {
  if (!isPlaying.value) {
    showControls.value = false
  }
}

// Listen for fullscreen change
document.addEventListener('fullscreenchange', handleFullscreenChange)

// Reset state when message changes
watch(() => props.message.id, () => {
  currentTime.value = 0
  isPlaying.value = false
  hasStarted.value = false
})

// Cleanup on unmount
onUnmounted(() => {
  if (videoRef.value) {
    videoRef.value.pause()
  }
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<template>
  <div
    ref="containerRef"
    class="video-message"
    :class="{ fullscreen: isFullscreen }"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <!-- Thumbnail overlay (before play) -->
    <div
      v-if="!hasStarted && thumbnailUrl"
      class="thumbnail-overlay"
      @click="togglePlay"
    >
      <img :src="thumbnailUrl" alt="Video thumbnail" class="thumbnail" />
      <div class="play-overlay">
        <el-icon :size="48"><VideoPlay /></el-icon>
      </div>
    </div>

    <!-- Video element -->
    <video
      v-show="hasStarted || !thumbnailUrl"
      ref="videoRef"
      :src="videoUrl"
      class="video-player"
      @play="handlePlay"
      @pause="handlePause"
      @ended="handleEnded"
      @timeupdate="handleTimeUpdate"
      @loadedmetadata="handleLoadedMetadata"
      @loadstart="handleLoadStart"
      @canplay="handleCanPlay"
      @click="togglePlay"
    />

    <!-- Controls overlay -->
    <div
      v-if="hasStarted || !thumbnailUrl"
      class="controls-overlay"
      :class="{ visible: showControls || !isPlaying }"
    >
      <div class="controls-top">
        <button class="control-button" @click="handleDownload">
          <el-icon><Download /></el-icon>
        </button>
        <button class="control-button" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
        </button>
      </div>

      <button class="center-button" @click="togglePlay">
        <el-icon v-if="isLoading" class="is-loading" :size="32">
          <VideoPlay />
        </el-icon>
        <el-icon v-else-if="isPlaying" :size="32">
          <VideoPause />
        </el-icon>
        <el-icon v-else :size="32">
          <VideoPlay />
        </el-icon>
      </button>

      <div class="controls-bottom">
        <div
          class="progress-bar"
          @click="handleProgressClick"
        >
          <div class="progress-fill" :style="{ width: `${progress}%` }" />
        </div>
        <div class="time-display">{{ displayTime }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-message {
  position: relative;
  width: 300px;
  max-width: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}

.video-message.fullscreen {
  width: 100vw;
  height: 100vh;
  border-radius: 0;
}

.thumbnail-overlay {
  position: relative;
  cursor: pointer;
}

.thumbnail {
  width: 100%;
  display: block;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  color: white;
}

.video-player {
  width: 100%;
  display: block;
  cursor: pointer;
}

.fullscreen .video-player {
  height: 100%;
  object-fit: contain;
}

.controls-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.controls-overlay.visible {
  opacity: 1;
  pointer-events: auto;
}

.controls-top {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding: 8px;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.5), transparent);
}

.control-button {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.control-button:hover {
  background: rgba(0, 0, 0, 0.7);
}

.center-button {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.center-button:hover {
  background: rgba(0, 0, 0, 0.7);
  transform: translate(-50%, -50%) scale(1.1);
}

.controls-bottom {
  padding: 8px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.5), transparent);
}

.progress-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  cursor: pointer;
  margin-bottom: 4px;
}

.progress-fill {
  height: 100%;
  background: var(--el-color-primary);
  border-radius: 2px;
  transition: width 0.1s;
}

.time-display {
  font-size: 12px;
  color: white;
  text-align: center;
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
