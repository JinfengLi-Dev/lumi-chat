<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { friendApi, groupApi, fileApi } from '@/api'
import type { Friend } from '@/types'
import type { GroupDetail } from '@/api/group'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'group-created', group: GroupDetail): void
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const step = ref(1)
const groupName = ref('')
const groupAnnouncement = ref('')
const groupAvatar = ref<File | null>(null)
const groupAvatarPreview = ref('')
const selectedFriends = ref<number[]>([])
const friends = ref<Friend[]>([])
const searchQuery = ref('')
const isLoading = ref(false)
const isCreating = ref(false)

const filteredFriends = computed(() => {
  if (!searchQuery.value) return friends.value
  const query = searchQuery.value.toLowerCase()
  return friends.value.filter(
    (friend) =>
      friend.nickname.toLowerCase().includes(query) ||
      (friend.remark && friend.remark.toLowerCase().includes(query))
  )
})

async function loadFriends() {
  isLoading.value = true
  try {
    friends.value = await friendApi.getFriends()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || 'Failed to load friends')
  } finally {
    isLoading.value = false
  }
}

function handleAvatarChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (file) {
    if (!file.type.startsWith('image/')) {
      ElMessage.error('Please select an image file')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.error('Image size must be less than 5MB')
      return
    }
    groupAvatar.value = file
    groupAvatarPreview.value = URL.createObjectURL(file)
  }
}

function toggleFriend(friendId: number) {
  const index = selectedFriends.value.indexOf(friendId)
  if (index > -1) {
    selectedFriends.value.splice(index, 1)
  } else {
    selectedFriends.value.push(friendId)
  }
}

function nextStep() {
  if (step.value === 1) {
    if (!groupName.value.trim()) {
      ElMessage.warning('Please enter a group name')
      return
    }
    step.value = 2
  }
}

function prevStep() {
  if (step.value === 2) {
    step.value = 1
  }
}

async function createGroup() {
  if (!groupName.value.trim()) {
    ElMessage.warning('Please enter a group name')
    return
  }

  isCreating.value = true
  try {
    let avatarUrl: string | undefined

    // Upload avatar if selected
    if (groupAvatar.value) {
      const fileInfo = await fileApi.uploadAvatar(groupAvatar.value)
      avatarUrl = fileInfo.url
    }

    // Create group
    const group = await groupApi.createGroup({
      name: groupName.value.trim(),
      avatar: avatarUrl,
      announcement: groupAnnouncement.value.trim() || undefined,
      memberIds: selectedFriends.value.length > 0 ? selectedFriends.value : undefined,
    })

    ElMessage.success('Group created successfully')
    emit('group-created', group)
    handleClose()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || 'Failed to create group')
  } finally {
    isCreating.value = false
  }
}

function handleClose() {
  step.value = 1
  groupName.value = ''
  groupAnnouncement.value = ''
  groupAvatar.value = null
  groupAvatarPreview.value = ''
  selectedFriends.value = []
  searchQuery.value = ''
  dialogVisible.value = false
}

watch(dialogVisible, (visible) => {
  if (visible) {
    loadFriends()
  }
})

onMounted(() => {
  if (dialogVisible.value) {
    loadFriends()
  }
})
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="Create Group"
    width="520px"
    :before-close="handleClose"
    destroy-on-close
  >
    <!-- Step 1: Group Info -->
    <div v-if="step === 1" class="step-content">
      <div class="avatar-section">
        <div class="avatar-wrapper" @click="($refs.avatarInput as HTMLInputElement).click()">
          <el-avatar v-if="groupAvatarPreview" :src="groupAvatarPreview" :size="80" />
          <div v-else class="avatar-placeholder">
            <el-icon :size="32"><Plus /></el-icon>
          </div>
          <div class="avatar-overlay">
            <el-icon :size="20"><Camera /></el-icon>
          </div>
        </div>
        <input
          ref="avatarInput"
          type="file"
          accept="image/*"
          hidden
          @change="handleAvatarChange"
        />
        <span class="avatar-hint">Group Avatar (Optional)</span>
      </div>

      <el-form label-position="top">
        <el-form-item label="Group Name" required>
          <el-input
            v-model="groupName"
            placeholder="Enter group name"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="Announcement (Optional)">
          <el-input
            v-model="groupAnnouncement"
            type="textarea"
            :rows="3"
            placeholder="Enter group announcement"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <!-- Step 2: Select Members -->
    <div v-if="step === 2" class="step-content">
      <el-input
        v-model="searchQuery"
        placeholder="Search friends"
        prefix-icon="Search"
        clearable
        class="search-input"
      />

      <div class="selected-count">
        Selected: {{ selectedFriends.length }} friend(s)
      </div>

      <div v-loading="isLoading" class="friends-list">
        <div v-if="filteredFriends.length === 0 && !isLoading" class="empty-state">
          <el-empty :description="searchQuery ? 'No friends found' : 'No friends yet'" />
        </div>

        <div
          v-for="friend in filteredFriends"
          :key="friend.id"
          class="friend-item"
          :class="{ selected: selectedFriends.includes(friend.id) }"
          @click="toggleFriend(friend.id)"
        >
          <el-checkbox
            :model-value="selectedFriends.includes(friend.id)"
            @click.stop
            @change="toggleFriend(friend.id)"
          />
          <el-avatar :src="friend.avatar" :size="40">
            {{ friend.nickname?.charAt(0) }}
          </el-avatar>
          <div class="friend-info">
            <div class="friend-name">
              {{ friend.remark || friend.nickname }}
            </div>
            <div v-if="friend.remark" class="friend-nickname">
              {{ friend.nickname }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">Cancel</el-button>
        <el-button v-if="step === 2" @click="prevStep">Back</el-button>
        <el-button v-if="step === 1" type="primary" @click="nextStep">
          Next
        </el-button>
        <el-button
          v-if="step === 2"
          type="primary"
          :loading="isCreating"
          @click="createGroup"
        >
          Create Group
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.step-content {
  min-height: 300px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
}

.avatar-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
}

.avatar-overlay {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.avatar-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.search-input {
  margin-bottom: 12px;
}

.selected-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.friends-list {
  max-height: 350px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
  gap: 12px;
}

.friend-item:hover {
  background-color: var(--el-fill-color-light);
}

.friend-item.selected {
  background-color: var(--el-color-primary-light-9);
}

.friend-info {
  flex: 1;
}

.friend-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.friend-nickname {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.empty-state {
  padding: 32px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
