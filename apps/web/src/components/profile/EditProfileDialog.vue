<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Upload, User as UserIcon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { User } from '@/types'

const props = defineProps<{
  visible: boolean
  user: User | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save', data: ProfileUpdateData): void
}>()

export interface ProfileUpdateData {
  nickname: string
  gender?: 'male' | 'female'
  signature?: string
  description?: string
  avatar?: File | null
}

// Form data
const form = ref({
  nickname: '',
  gender: '' as '' | 'male' | 'female',
  signature: '',
  description: '',
})

// Avatar handling
const avatarFile = ref<File | null>(null)
const avatarPreview = ref<string>('')

// Loading state
const isSaving = ref(false)

// Validation
const nicknameError = computed(() => {
  if (!form.value.nickname.trim()) {
    return 'Nickname is required'
  }
  if (form.value.nickname.length > 30) {
    return 'Nickname must be 30 characters or less'
  }
  return ''
})

const signatureError = computed(() => {
  if (form.value.signature.length > 100) {
    return 'Signature must be 100 characters or less'
  }
  return ''
})

const descriptionError = computed(() => {
  if (form.value.description.length > 500) {
    return 'Description must be 500 characters or less'
  }
  return ''
})

const isFormValid = computed(() => {
  return !nicknameError.value && !signatureError.value && !descriptionError.value
})

const hasChanges = computed(() => {
  if (!props.user) return false

  return (
    form.value.nickname !== (props.user.nickname || '') ||
    form.value.gender !== (props.user.gender || '') ||
    form.value.signature !== (props.user.signature || '') ||
    form.value.description !== (props.user.description || '') ||
    avatarFile.value !== null
  )
})

// Initialize form when dialog opens
watch(
  () => props.visible,
  (visible) => {
    if (visible && props.user) {
      form.value = {
        nickname: props.user.nickname || '',
        gender: (props.user.gender as '' | 'male' | 'female') || '',
        signature: props.user.signature || '',
        description: props.user.description || '',
      }
      avatarFile.value = null
      avatarPreview.value = props.user.avatar || ''
    }
  }
)

function handleAvatarChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  // Validate file type
  if (!file.type.startsWith('image/')) {
    ElMessage.error('Please select an image file')
    return
  }

  // Validate file size (max 5MB)
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('Image size must be less than 5MB')
    return
  }

  avatarFile.value = file

  // Create preview URL
  const reader = new FileReader()
  reader.onload = (e) => {
    avatarPreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

function removeAvatar() {
  avatarFile.value = null
  avatarPreview.value = props.user?.avatar || ''
}

function close() {
  emit('update:visible', false)
}

async function handleSave() {
  if (!isFormValid.value) {
    ElMessage.warning('Please fix the validation errors')
    return
  }

  if (!hasChanges.value) {
    ElMessage.info('No changes to save')
    close()
    return
  }

  isSaving.value = true

  try {
    const data: ProfileUpdateData = {
      nickname: form.value.nickname.trim(),
    }

    if (form.value.gender) {
      data.gender = form.value.gender
    }

    if (form.value.signature.trim()) {
      data.signature = form.value.signature.trim()
    }

    if (form.value.description.trim()) {
      data.description = form.value.description.trim()
    }

    if (avatarFile.value) {
      data.avatar = avatarFile.value
    }

    emit('save', data)
    close()
  } finally {
    isSaving.value = false
  }
}

// Expose for testing
defineExpose({
  form,
  avatarFile,
  avatarPreview,
  isSaving,
  nicknameError,
  signatureError,
  descriptionError,
  isFormValid,
  hasChanges,
  handleAvatarChange,
  removeAvatar,
  close,
  handleSave,
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title="Edit Profile"
    width="450px"
    :close-on-click-modal="false"
    class="edit-profile-dialog"
  >
    <div class="edit-profile-form">
      <!-- Avatar Section -->
      <div class="avatar-section">
        <div class="avatar-wrapper">
          <el-avatar
            :src="avatarPreview"
            :size="100"
            shape="square"
            class="profile-avatar"
          >
            <el-icon :size="40"><UserIcon /></el-icon>
          </el-avatar>
          <div class="avatar-overlay">
            <label class="upload-label">
              <el-icon><Upload /></el-icon>
              <span>Change</span>
              <input
                type="file"
                accept="image/*"
                class="file-input"
                @change="handleAvatarChange"
              />
            </label>
          </div>
        </div>
        <p class="avatar-hint">Click to change avatar (max 5MB)</p>
      </div>

      <!-- Form Fields -->
      <div class="form-fields">
        <div class="form-item">
          <label class="form-label">Nickname <span class="required">*</span></label>
          <el-input
            v-model="form.nickname"
            placeholder="Enter your nickname"
            maxlength="30"
            show-word-limit
            :class="{ 'is-error': nicknameError }"
          />
          <p v-if="nicknameError" class="error-text">{{ nicknameError }}</p>
        </div>

        <div class="form-item">
          <label class="form-label">Gender</label>
          <el-radio-group v-model="form.gender">
            <el-radio value="male">Male</el-radio>
            <el-radio value="female">Female</el-radio>
          </el-radio-group>
        </div>

        <div class="form-item">
          <label class="form-label">Signature</label>
          <el-input
            v-model="form.signature"
            placeholder="A brief status or quote"
            maxlength="100"
            show-word-limit
            :class="{ 'is-error': signatureError }"
          />
          <p v-if="signatureError" class="error-text">{{ signatureError }}</p>
        </div>

        <div class="form-item">
          <label class="form-label">About Me</label>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="Tell others about yourself"
            maxlength="500"
            show-word-limit
            :class="{ 'is-error': descriptionError }"
          />
          <p v-if="descriptionError" class="error-text">{{ descriptionError }}</p>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">Cancel</el-button>
        <el-button
          type="primary"
          :loading="isSaving"
          :disabled="!isFormValid || !hasChanges"
          @click="handleSave"
        >
          Save Changes
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.edit-profile-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
}

.profile-avatar {
  background: linear-gradient(135deg, var(--el-color-primary-light-3), var(--el-color-primary));
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.upload-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: white;
  font-size: 12px;
  cursor: pointer;
}

.file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
}

.avatar-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.required {
  color: var(--el-color-danger);
}

.error-text {
  margin: 0;
  font-size: 12px;
  color: var(--el-color-danger);
}

.is-error :deep(.el-input__wrapper),
.is-error :deep(.el-textarea__inner) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
