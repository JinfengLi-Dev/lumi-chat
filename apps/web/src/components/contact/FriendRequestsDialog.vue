<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { friendApi } from '@/api'
import type { FriendRequest } from '@/types'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'request-handled'): void
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const requests = ref<FriendRequest[]>([])
const isLoading = ref(false)
const processingIds = ref<Set<number>>(new Set())

async function loadRequests() {
  isLoading.value = true
  try {
    requests.value = await friendApi.getFriendRequests(true)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || 'Failed to load requests')
  } finally {
    isLoading.value = false
  }
}

async function acceptRequest(request: FriendRequest) {
  processingIds.value.add(request.id)
  try {
    await friendApi.acceptFriendRequest(request.id)
    ElMessage.success(`Added ${request.fromUser.nickname} as friend`)
    requests.value = requests.value.filter((r) => r.id !== request.id)
    emit('request-handled')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || 'Failed to accept request')
  } finally {
    processingIds.value.delete(request.id)
  }
}

async function rejectRequest(request: FriendRequest) {
  try {
    await ElMessageBox.confirm(
      `Reject friend request from ${request.fromUser.nickname}?`,
      'Confirm',
      {
        confirmButtonText: 'Reject',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )

    processingIds.value.add(request.id)
    await friendApi.rejectFriendRequest(request.id)
    ElMessage.success('Request rejected')
    requests.value = requests.value.filter((r) => r.id !== request.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || 'Failed to reject request')
    }
  } finally {
    processingIds.value.delete(request.id)
  }
}

function formatTime(time: string) {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    return 'Today'
  } else if (days === 1) {
    return 'Yesterday'
  } else if (days < 7) {
    return `${days} days ago`
  } else {
    return date.toLocaleDateString()
  }
}

watch(dialogVisible, (visible) => {
  if (visible) {
    loadRequests()
  }
})

onMounted(() => {
  if (dialogVisible.value) {
    loadRequests()
  }
})
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="Friend Requests"
    width="500px"
    destroy-on-close
  >
    <div v-loading="isLoading" class="requests-container">
      <div v-if="requests.length === 0 && !isLoading" class="empty-state">
        <el-empty description="No pending friend requests" />
      </div>

      <div v-else class="requests-list">
        <div
          v-for="request in requests"
          :key="request.id"
          class="request-item"
        >
          <el-avatar :src="request.fromUser.avatar" :size="48">
            {{ request.fromUser.nickname?.charAt(0) }}
          </el-avatar>

          <div class="request-content">
            <div class="request-header">
              <span class="user-name">{{ request.fromUser.nickname }}</span>
              <span class="request-time">{{ formatTime(request.createdAt) }}</span>
            </div>
            <div v-if="request.message" class="request-message">
              "{{ request.message }}"
            </div>
            <div class="user-uid">UID: {{ request.fromUser.uid }}</div>
          </div>

          <div class="request-actions">
            <el-button
              type="primary"
              size="small"
              :loading="processingIds.has(request.id)"
              @click="acceptRequest(request)"
            >
              Accept
            </el-button>
            <el-button
              type="default"
              size="small"
              :disabled="processingIds.has(request.id)"
              @click="rejectRequest(request)"
            >
              Reject
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">Close</el-button>
      <el-button type="primary" text @click="loadRequests" :loading="isLoading">
        Refresh
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.requests-container {
  min-height: 200px;
}

.requests-list {
  max-height: 400px;
  overflow-y: auto;
}

.request-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.request-item:last-child {
  border-bottom: none;
}

.request-content {
  flex: 1;
  margin-left: 12px;
  margin-right: 12px;
}

.request-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.request-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.request-message {
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-style: italic;
}

.user-uid {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.request-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-state {
  padding: 32px 0;
}
</style>
