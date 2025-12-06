<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { userApi, friendApi } from '@/api'
import type { User } from '@/types'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'request-sent'): void
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const searchQuery = ref('')
const searchResults = ref<User[]>([])
const isSearching = ref(false)
const requestMessage = ref('')
const selectedUser = ref<User | null>(null)
const isSending = ref(false)

async function handleSearch() {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }

  isSearching.value = true
  try {
    const users = await userApi.searchUsers(searchQuery.value.trim())
    searchResults.value = users
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || 'Search failed')
  } finally {
    isSearching.value = false
  }
}

function selectUser(user: User) {
  selectedUser.value = user
}

function clearSelection() {
  selectedUser.value = null
  requestMessage.value = ''
}

async function sendFriendRequest() {
  if (!selectedUser.value) return

  isSending.value = true
  try {
    await friendApi.sendFriendRequest({
      targetUserId: selectedUser.value.id,
      message: requestMessage.value || undefined,
    })
    ElMessage.success('Friend request sent')
    emit('request-sent')
    handleClose()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || 'Failed to send request')
  } finally {
    isSending.value = false
  }
}

function handleClose() {
  searchQuery.value = ''
  searchResults.value = []
  selectedUser.value = null
  requestMessage.value = ''
  dialogVisible.value = false
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="Add Friend"
    width="480px"
    :before-close="handleClose"
    destroy-on-close
  >
    <div v-if="!selectedUser" class="search-section">
      <el-input
        v-model="searchQuery"
        placeholder="Search by email or UID"
        :prefix-icon="Search"
        clearable
        @keyup.enter="handleSearch"
        @clear="searchResults = []"
      />
      <el-button
        type="primary"
        :loading="isSearching"
        :disabled="!searchQuery.trim()"
        @click="handleSearch"
        style="margin-left: 12px"
      >
        Search
      </el-button>
    </div>

    <div v-if="!selectedUser && searchResults.length > 0" class="search-results">
      <div
        v-for="user in searchResults"
        :key="user.id"
        class="user-item"
        @click="selectUser(user)"
      >
        <el-avatar :src="user.avatar" :size="40">
          {{ user.nickname?.charAt(0) }}
        </el-avatar>
        <div class="user-info">
          <div class="user-name">{{ user.nickname }}</div>
          <div class="user-uid">UID: {{ user.uid }}</div>
        </div>
      </div>
    </div>

    <div v-if="!selectedUser && searchQuery && !isSearching && searchResults.length === 0" class="no-results">
      No users found
    </div>

    <div v-if="selectedUser" class="selected-user">
      <div class="selected-header">
        <el-button text @click="clearSelection">
          <el-icon><ArrowLeft /></el-icon>
          Back to search
        </el-button>
      </div>

      <div class="user-card">
        <el-avatar :src="selectedUser.avatar" :size="64">
          {{ selectedUser.nickname?.charAt(0) }}
        </el-avatar>
        <div class="user-details">
          <div class="user-name">{{ selectedUser.nickname }}</div>
          <div class="user-uid">UID: {{ selectedUser.uid }}</div>
          <div v-if="selectedUser.signature" class="user-signature">
            {{ selectedUser.signature }}
          </div>
        </div>
      </div>

      <el-input
        v-model="requestMessage"
        type="textarea"
        :rows="3"
        placeholder="Add a message (optional)"
        maxlength="200"
        show-word-limit
        style="margin-top: 16px"
      />
    </div>

    <template #footer>
      <el-button @click="handleClose">Cancel</el-button>
      <el-button
        v-if="selectedUser"
        type="primary"
        :loading="isSending"
        @click="sendFriendRequest"
      >
        Send Request
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.search-section {
  display: flex;
  align-items: center;
}

.search-results {
  margin-top: 16px;
  max-height: 300px;
  overflow-y: auto;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-item:hover {
  background-color: var(--el-fill-color-light);
}

.user-info {
  margin-left: 12px;
}

.user-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.user-uid {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.no-results {
  text-align: center;
  padding: 32px;
  color: var(--el-text-color-secondary);
}

.selected-header {
  margin-bottom: 16px;
}

.user-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
}

.user-details {
  margin-left: 16px;
}

.user-details .user-name {
  font-size: 18px;
  font-weight: 600;
}

.user-signature {
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
