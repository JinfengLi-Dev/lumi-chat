<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue'
import { Download, Microphone, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import type { Message } from '@/types'

const props = defineProps<{
  message: Message
}>()

const audioRef = ref<HTMLAudioElement | null>(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const isLoading = ref(false)

const audioUrl = computed(() => props.message.content)
const messageDuration = computed(() => props.message.metadata?.duration || 0)

const progress = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const displayTime = computed(() => {
  if (isPlaying.value || currentTime.value > 0) {
    return formatTime(currentTime.value)
  }
  return formatTime(messageDuration.value)
})

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

function togglePlay() {
  if (!audioRef.value) return

  if (isPlaying.value) {
    audioRef.value.pause()
  } else {
    audioRef.value.play()
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
  currentTime.value = 0
}

function handleTimeUpdate() {
  if (audioRef.value) {
    currentTime.value = audioRef.value.currentTime
  }
}

function handleLoadedMetadata() {
  if (audioRef.value) {
    duration.value = audioRef.value.duration
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
  if (!audioRef.value) return

  const progressBar = event.currentTarget as HTMLElement
  const rect = progressBar.getBoundingClientRect()
  const clickPosition = event.clientX - rect.left
  const percentage = clickPosition / rect.width
  const newTime = percentage * duration.value

  audioRef.value.currentTime = newTime
  currentTime.value = newTime
}

function handleDownload() {
  const link = document.createElement('a')
  link.href = audioUrl.value
  link.download = `voice-${props.message.msgId}.mp3`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// Reset state when message changes
watch(() => props.message.id, () => {
  currentTime.value = 0
  isPlaying.value = false
})

// Cleanup on unmount
onUnmounted(() => {
  if (audioRef.value) {
    audioRef.value.pause()
  }
})
</script>

<template>
  <div class="voice-message">
    <audio
      ref="audioRef"
      :src="audioUrl"
      @play="handlePlay"
      @pause="handlePause"
      @ended="handleEnded"
      @timeupdate="handleTimeUpdate"
      @loadedmetadata="handleLoadedMetadata"
      @loadstart="handleLoadStart"
      @canplay="handleCanPlay"
    />

    <button
      class="play-button"
      :class="{ playing: isPlaying }"
      :disabled="isLoading"
      @click="togglePlay"
    >
      <el-icon v-if="isLoading" class="is-loading">
        <Microphone />
      </el-icon>
      <el-icon v-else-if="isPlaying">
        <VideoPause />
      </el-icon>
      <el-icon v-else>
        <VideoPlay />
      </el-icon>
    </button>

    <div class="voice-content">
      <div
        class="progress-bar"
        @click="handleProgressClick"
      >
        <div
          class="progress-fill"
          :style="{ width: `${progress}%` }"
        />
        <div class="waveform">
          <span v-for="i in 20" :key="i" class="wave-bar" />
        </div>
      </div>

      <div class="time-display">
        {{ displayTime }}
      </div>
    </div>

    <button class="download-button" @click="handleDownload">
      <el-icon><Download /></el-icon>
    </button>
  </div>
</template>

<style scoped>
.voice-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  min-width: 200px;
  max-width: 300px;
}

.play-button {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.play-button:hover {
  background: var(--el-color-primary-light-3);
}

.play-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.play-button.playing {
  background: var(--el-color-warning);
}

.voice-content {
  flex: 1;
  min-width: 0;
}

.progress-bar {
  height: 24px;
  background: var(--el-fill-color);
  border-radius: 4px;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.progress-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: var(--el-color-primary-light-7);
  transition: width 0.1s;
}

.waveform {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: space-evenly;
  height: 16px;
  padding: 0 4px;
}

.wave-bar {
  width: 2px;
  background: var(--el-color-primary);
  border-radius: 1px;
  height: 4px;
}

.wave-bar:nth-child(odd) {
  height: 8px;
}

.wave-bar:nth-child(3n) {
  height: 12px;
}

.wave-bar:nth-child(5n) {
  height: 6px;
}

.time-display {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  text-align: right;
}

.download-button {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.download-button:hover {
  background: var(--el-fill-color);
  color: var(--el-text-color-primary);
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
