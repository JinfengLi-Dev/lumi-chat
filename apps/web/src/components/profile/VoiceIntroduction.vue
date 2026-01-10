<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { formatDuration } from '@/utils/audioConverter'
import VoiceRecorder from '@/components/chat/VoiceRecorder.vue'
import type { User } from '@/types'

const props = defineProps<{
  user: User
  isOwnProfile?: boolean
}>()

const emit = defineEmits<{
  (e: 'updated', user: User): void
}>()

const isEditing = ref(false)
const isPlaying = ref(false)
const isLoading = ref(false)
const audioRef = ref<HTMLAudioElement | null>(null)

const hasVoiceIntro = computed(() => !!props.user.voiceIntroUrl)

const formattedDuration = computed(() => {
  if (props.user.voiceIntroDuration) {
    return formatDuration(props.user.voiceIntroDuration)
  }
  return '0:00'
})

function togglePlayback() {
  if (!audioRef.value || !props.user.voiceIntroUrl) return

  if (isPlaying.value) {
    audioRef.value.pause()
    audioRef.value.currentTime = 0
    isPlaying.value = false
  } else {
    audioRef.value.onended = () => {
      isPlaying.value = false
    }
    audioRef.value.play()
    isPlaying.value = true
  }
}

async function handleRecorded(file: File, duration: number) {
  isLoading.value = true
  try {
    const updatedUser = await userApi.uploadVoiceIntro(file, duration)
    emit('updated', updatedUser)
    isEditing.value = false
    ElMessage.success('Voice introduction uploaded successfully')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to upload voice introduction'
    ElMessage.error(message)
  } finally {
    isLoading.value = false
  }
}

function handleCancelRecording() {
  isEditing.value = false
}

async function handleDelete() {
  isLoading.value = true
  try {
    const updatedUser = await userApi.deleteVoiceIntro()
    emit('updated', updatedUser)
    ElMessage.success('Voice introduction deleted')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to delete voice introduction'
    ElMessage.error(message)
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="voice-introduction">
    <!-- Has voice intro - show playback -->
    <div v-if="hasVoiceIntro && !isEditing" class="playback-section">
      <audio
        ref="audioRef"
        :src="user.voiceIntroUrl"
        preload="metadata"
      ></audio>

      <div class="playback-controls">
        <el-button
          circle
          type="primary"
          :loading="isLoading"
          @click="togglePlayback"
        >
          <el-icon :size="18">
            <VideoPause v-if="isPlaying" />
            <VideoPlay v-else />
          </el-icon>
        </el-button>

        <div class="playback-info">
          <span class="label">Voice Introduction</span>
          <span class="duration">{{ formattedDuration }}</span>
        </div>

        <div v-if="isOwnProfile" class="actions">
          <el-button text type="primary" size="small" @click="isEditing = true">
            Re-record
          </el-button>
          <el-button text type="danger" size="small" @click="handleDelete">
            Delete
          </el-button>
        </div>
      </div>
    </div>

    <!-- Recording mode -->
    <div v-else-if="isEditing" class="recording-section">
      <div class="recording-header">
        <span>Record Voice Introduction</span>
        <el-button text size="small" @click="handleCancelRecording">
          Cancel
        </el-button>
      </div>
      <VoiceRecorder
        @recorded="handleRecorded"
        @cancel="handleCancelRecording"
      />
    </div>

    <!-- No voice intro yet -->
    <div v-else class="empty-section">
      <div class="empty-content">
        <el-icon :size="32" class="empty-icon"><Microphone /></el-icon>
        <p v-if="isOwnProfile">No voice introduction yet</p>
        <p v-else>No voice introduction</p>
      </div>

      <el-button
        v-if="isOwnProfile"
        type="primary"
        @click="isEditing = true"
      >
        <el-icon><Microphone /></el-icon>
        Record Introduction
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.voice-introduction {
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.playback-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.playback-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.playback-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.playback-info .label {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.playback-info .duration {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}

.actions {
  display: flex;
  gap: 4px;
}

.recording-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recording-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.empty-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.empty-icon {
  color: #c0c4cc;
}

.empty-content p {
  margin: 0;
  font-size: 14px;
}
</style>
