<script setup lang="ts">
import { computed } from 'vue'
import { ChatDotRound, Plus, User as UserIcon } from '@element-plus/icons-vue'
import type { User } from '@/types'

const props = defineProps<{
  visible: boolean
  user: User | null
  // Whether the current user is friends with this user
  isFriend?: boolean
  // Whether this is the current user's own profile
  isSelf?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'send-message', user: User): void
  (e: 'add-friend', user: User): void
  (e: 'edit-profile'): void
}>()

const displayName = computed(() => props.user?.nickname || 'Unknown User')

const genderText = computed(() => {
  if (!props.user?.gender) return ''
  return props.user.gender === 'male' ? 'Male' : props.user.gender === 'female' ? 'Female' : ''
})

const statusText = computed(() => {
  if (!props.user?.status) return ''
  if (props.user.status === 'active') return 'Online'
  if (props.user.status === 'inactive') return 'Offline'
  return props.user.status
})

const statusClass = computed(() => {
  if (props.user?.status === 'active') return 'status-online'
  return 'status-offline'
})

// Show stranger badge when not self and not a friend
const isStranger = computed(() => !props.isSelf && !props.isFriend)

const formattedCreatedAt = computed(() => {
  if (!props.user?.createdAt) return ''
  try {
    return new Date(props.user.createdAt).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    })
  } catch {
    return props.user.createdAt
  }
})

const formattedLastLogin = computed(() => {
  if (!props.user?.lastLoginAt) return ''
  try {
    return new Date(props.user.lastLoginAt).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return props.user.lastLoginAt
  }
})

function close() {
  emit('update:visible', false)
}

function handleSendMessage() {
  if (!props.user) return
  emit('send-message', props.user)
  close()
}

function handleAddFriend() {
  if (!props.user) return
  emit('add-friend', props.user)
}

function handleEditProfile() {
  emit('edit-profile')
  close()
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title="User Profile"
    width="400px"
    :close-on-click-modal="true"
    class="user-profile-dialog"
  >
    <div v-if="user" class="profile-content">
      <!-- Avatar and basic info -->
      <div class="profile-header">
        <el-avatar :src="user.avatar" :size="80" shape="square" class="profile-avatar">
          {{ displayName.charAt(0) }}
        </el-avatar>

        <div class="profile-basic">
          <div class="profile-name-row">
            <h2 class="profile-name">{{ displayName }}</h2>
            <el-tag v-if="isStranger" type="info" size="small" class="stranger-badge">
              Stranger
            </el-tag>
          </div>
          <div class="profile-uid">ID: {{ user.uid }}</div>
          <div class="profile-status" :class="statusClass">
            <span class="status-dot"></span>
            {{ statusText }}
          </div>
        </div>
      </div>

      <!-- Details -->
      <div class="profile-details">
        <div v-if="user.signature" class="detail-item">
          <span class="detail-label">Signature</span>
          <span class="detail-value">{{ user.signature }}</span>
        </div>

        <div v-if="user.description" class="detail-item">
          <span class="detail-label">About</span>
          <span class="detail-value">{{ user.description }}</span>
        </div>

        <div v-if="genderText" class="detail-item">
          <span class="detail-label">Gender</span>
          <span class="detail-value">{{ genderText }}</span>
        </div>

        <div v-if="user.email" class="detail-item">
          <span class="detail-label">Email</span>
          <span class="detail-value">{{ user.email }}</span>
        </div>

        <div v-if="formattedCreatedAt" class="detail-item">
          <span class="detail-label">Joined</span>
          <span class="detail-value">{{ formattedCreatedAt }}</span>
        </div>

        <div v-if="formattedLastLogin" class="detail-item">
          <span class="detail-label">Last seen</span>
          <span class="detail-value">{{ formattedLastLogin }}</span>
        </div>
      </div>

      <!-- Actions -->
      <div class="profile-actions">
        <template v-if="isSelf">
          <el-button type="primary" @click="handleEditProfile">
            Edit Profile
          </el-button>
        </template>
        <template v-else>
          <el-button
            type="primary"
            @click="handleSendMessage"
          >
            <el-icon><ChatDotRound /></el-icon>
            Send Message
          </el-button>
          <el-button
            v-if="!isFriend"
            @click="handleAddFriend"
          >
            <el-icon><Plus /></el-icon>
            Add Friend
          </el-button>
        </template>
      </div>
    </div>

    <div v-else class="profile-empty">
      <el-icon :size="48" color="#c0c4cc"><UserIcon /></el-icon>
      <p>No user selected</p>
    </div>
  </el-dialog>
</template>

<style scoped>
.profile-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.profile-avatar {
  flex-shrink: 0;
}

.profile-basic {
  flex: 1;
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: var(--el-text-color-primary);
}

.stranger-badge {
  flex-shrink: 0;
}

.profile-uid {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.profile-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-online {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.status-online .status-dot {
  background: var(--el-color-success);
}

.status-offline {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}

.status-offline .status-dot {
  background: var(--el-text-color-secondary);
}

.profile-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-value {
  font-size: 14px;
  color: var(--el-text-color-primary);
  word-break: break-word;
}

.profile-actions {
  display: flex;
  gap: 12px;
}

.profile-actions .el-button {
  flex: 1;
}

.profile-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--el-text-color-secondary);
}

.profile-empty p {
  margin-top: 12px;
  font-size: 14px;
}
</style>
