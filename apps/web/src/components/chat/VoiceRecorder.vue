<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { AudioVisualizer } from '@/utils/audioVisualizer'
import {
  formatDuration,
  isAudioRecordingSupported,
  getBestAudioMimeType,
  createAudioFile,
  requestMicrophonePermission,
  stopMediaStream,
  getAudioDuration,
} from '@/utils/audioConverter'

const emit = defineEmits<{
  (e: 'recorded', file: File, duration: number): void
  (e: 'cancel'): void
}>()

// State
const isRecording = ref(false)
const isPaused = ref(false)
const hasRecording = ref(false)
const recordingTime = ref(0)
const audioBlob = ref<Blob | null>(null)
const audioUrl = ref<string | null>(null)
const isPlayingPreview = ref(false)

// Refs
const canvasRef = ref<HTMLCanvasElement | null>(null)
const audioRef = ref<HTMLAudioElement | null>(null)

// Internal state
let mediaRecorder: MediaRecorder | null = null
let mediaStream: MediaStream | null = null
let visualizer: AudioVisualizer | null = null
let timerInterval: number | null = null
let recordedChunks: Blob[] = []

// Max recording duration (5 minutes)
const MAX_DURATION = 300

const canRecord = computed(() => isAudioRecordingSupported())

const formattedTime = computed(() => formatDuration(recordingTime.value))

const remainingTime = computed(() => formatDuration(MAX_DURATION - recordingTime.value))

const progressPercent = computed(() => (recordingTime.value / MAX_DURATION) * 100)

async function startRecording() {
  if (!canRecord.value) {
    ElMessage.error('Audio recording is not supported in this browser')
    return
  }

  try {
    // Request microphone permission
    mediaStream = await requestMicrophonePermission()

    // Reset state
    recordedChunks = []
    recordingTime.value = 0
    hasRecording.value = false

    if (audioUrl.value) {
      URL.revokeObjectURL(audioUrl.value)
      audioUrl.value = null
    }
    audioBlob.value = null

    // Create MediaRecorder
    const mimeType = getBestAudioMimeType()
    mediaRecorder = new MediaRecorder(mediaStream, { mimeType })

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        recordedChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = async () => {
      // Create blob from recorded chunks
      const mimeType = mediaRecorder?.mimeType || 'audio/webm'
      audioBlob.value = new Blob(recordedChunks, { type: mimeType })
      audioUrl.value = URL.createObjectURL(audioBlob.value)
      hasRecording.value = true
    }

    // Start recording
    mediaRecorder.start(100) // Collect data every 100ms
    isRecording.value = true
    isPaused.value = false

    // Start visualizer
    if (canvasRef.value) {
      visualizer = new AudioVisualizer({
        canvas: canvasRef.value,
        barColor: '#409eff',
        backgroundColor: '#2c2c2c',
      })
      await visualizer.connect(mediaStream)
    }

    // Start timer
    startTimer()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to start recording'
    ElMessage.error(message)
  }
}

function pauseRecording() {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.pause()
    isPaused.value = true
    stopTimer()
  }
}

function resumeRecording() {
  if (mediaRecorder && mediaRecorder.state === 'paused') {
    mediaRecorder.resume()
    isPaused.value = false
    startTimer()
  }
}

function stopRecording() {
  stopTimer()

  if (visualizer) {
    visualizer.disconnect()
    visualizer = null
  }

  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }

  stopMediaStream(mediaStream)
  mediaStream = null

  isRecording.value = false
  isPaused.value = false
}

function cancelRecording() {
  stopRecording()
  hasRecording.value = false
  recordingTime.value = 0

  if (audioUrl.value) {
    URL.revokeObjectURL(audioUrl.value)
    audioUrl.value = null
  }
  audioBlob.value = null

  emit('cancel')
}

async function sendRecording() {
  if (!audioBlob.value) return

  try {
    const duration = await getAudioDuration(audioBlob.value)
    const file = createAudioFile(audioBlob.value)

    emit('recorded', file, duration)

    // Reset state
    hasRecording.value = false
    recordingTime.value = 0
    if (audioUrl.value) {
      URL.revokeObjectURL(audioUrl.value)
      audioUrl.value = null
    }
    audioBlob.value = null
  } catch {
    ElMessage.error('Failed to process recording')
  }
}

function togglePlayPreview() {
  if (!audioRef.value || !audioUrl.value) return

  if (isPlayingPreview.value) {
    audioRef.value.pause()
    audioRef.value.currentTime = 0
    isPlayingPreview.value = false
  } else {
    audioRef.value.onended = () => {
      isPlayingPreview.value = false
    }
    audioRef.value.play()
    isPlayingPreview.value = true
  }
}

function startTimer() {
  stopTimer()
  timerInterval = window.setInterval(() => {
    recordingTime.value++
    if (recordingTime.value >= MAX_DURATION) {
      stopRecording()
      ElMessage.warning('Maximum recording duration reached')
    }
  }, 1000)
}

function stopTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

onMounted(() => {
  // Check support on mount
  if (!canRecord.value) {
    ElMessage.warning('Audio recording is not supported in this browser')
  }
})

onUnmounted(() => {
  stopTimer()
  if (visualizer) {
    visualizer.disconnect()
  }
  stopMediaStream(mediaStream)
  if (audioUrl.value) {
    URL.revokeObjectURL(audioUrl.value)
  }
})
</script>

<template>
  <div class="voice-recorder">
    <!-- Recording state -->
    <div v-if="isRecording" class="recording-state">
      <div class="visualizer-container">
        <canvas ref="canvasRef" width="200" height="40" class="visualizer"></canvas>
      </div>

      <div class="recording-info">
        <span class="recording-indicator">
          <span class="dot" :class="{ paused: isPaused }"></span>
          {{ isPaused ? 'Paused' : 'Recording' }}
        </span>
        <span class="time">{{ formattedTime }}</span>
        <span class="remaining">({{ remainingTime }} left)</span>
      </div>

      <el-progress
        :percentage="progressPercent"
        :stroke-width="3"
        :show-text="false"
        status="exception"
        class="progress-bar"
      />

      <div class="recording-controls">
        <el-button v-if="!isPaused" text circle @click="pauseRecording">
          <el-icon :size="20"><VideoPause /></el-icon>
        </el-button>
        <el-button v-else text circle @click="resumeRecording">
          <el-icon :size="20"><VideoPlay /></el-icon>
        </el-button>

        <el-button type="danger" text circle @click="stopRecording">
          <el-icon :size="24"><Close /></el-icon>
        </el-button>

        <el-button type="primary" circle @click="stopRecording">
          <el-icon :size="20"><Check /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- Preview state -->
    <div v-else-if="hasRecording" class="preview-state">
      <audio ref="audioRef" :src="audioUrl || undefined" preload="metadata"></audio>

      <div class="preview-info">
        <el-icon :size="20" class="audio-icon"><Headset /></el-icon>
        <span class="duration">{{ formattedTime }}</span>
      </div>

      <div class="preview-controls">
        <el-button text circle @click="togglePlayPreview">
          <el-icon :size="18">
            <VideoPause v-if="isPlayingPreview" />
            <VideoPlay v-else />
          </el-icon>
        </el-button>

        <el-button text type="danger" @click="cancelRecording">
          Cancel
        </el-button>

        <el-button type="primary" @click="sendRecording">
          <el-icon><Promotion /></el-icon>
          Send
        </el-button>
      </div>
    </div>

    <!-- Initial state -->
    <div v-else class="initial-state">
      <el-button
        type="primary"
        circle
        :disabled="!canRecord"
        @click="startRecording"
      >
        <el-icon :size="20"><Microphone /></el-icon>
      </el-button>
      <span class="hint">Click to start recording</span>
    </div>
  </div>
</template>

<style scoped>
.voice-recorder {
  background-color: #2c2c2c;
  border-radius: 8px;
  padding: 12px 16px;
  margin: 8px 0;
}

.initial-state {
  display: flex;
  align-items: center;
  gap: 12px;
}

.initial-state .hint {
  color: #909399;
  font-size: 13px;
}

.recording-state {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.visualizer-container {
  display: flex;
  justify-content: center;
}

.visualizer {
  border-radius: 4px;
}

.recording-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 13px;
  color: #e0e0e0;
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f56c6c;
  font-weight: 500;
}

.recording-indicator .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
  animation: pulse 1s infinite;
}

.recording-indicator .dot.paused {
  animation: none;
  background-color: #e6a23c;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.time {
  font-family: monospace;
  font-size: 14px;
}

.remaining {
  color: #909399;
  font-size: 12px;
}

.progress-bar {
  margin: 4px 0;
}

.recording-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.preview-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.preview-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e0e0e0;
}

.audio-icon {
  color: #409eff;
}

.duration {
  font-family: monospace;
  font-size: 14px;
}

.preview-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
