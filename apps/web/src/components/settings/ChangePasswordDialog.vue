<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { View, Hide } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'change-password', data: PasswordChangeData): void
}>()

export interface PasswordChangeData {
  currentPassword: string
  newPassword: string
}

// Form data
const form = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// Password visibility toggles
const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

// Loading state
const isSubmitting = ref(false)

// Validation
const currentPasswordError = computed(() => {
  if (!form.value.currentPassword) {
    return 'Current password is required'
  }
  return ''
})

const newPasswordError = computed(() => {
  if (!form.value.newPassword) {
    return 'New password is required'
  }
  if (form.value.newPassword.length < 6) {
    return 'Password must be at least 6 characters'
  }
  if (form.value.newPassword.length > 50) {
    return 'Password must be 50 characters or less'
  }
  if (form.value.newPassword === form.value.currentPassword) {
    return 'New password must be different from current password'
  }
  return ''
})

const confirmPasswordError = computed(() => {
  if (!form.value.confirmPassword) {
    return 'Please confirm your new password'
  }
  if (form.value.confirmPassword !== form.value.newPassword) {
    return 'Passwords do not match'
  }
  return ''
})

const isFormValid = computed(() => {
  return (
    !currentPasswordError.value &&
    !newPasswordError.value &&
    !confirmPasswordError.value &&
    form.value.currentPassword.length > 0 &&
    form.value.newPassword.length > 0 &&
    form.value.confirmPassword.length > 0
  )
})

// Password strength indicator
const passwordStrength = computed(() => {
  const pwd = form.value.newPassword
  if (!pwd) return { level: 0, text: '', class: '' }

  let score = 0

  // Length check
  if (pwd.length >= 8) score++
  if (pwd.length >= 12) score++

  // Character diversity
  if (/[a-z]/.test(pwd)) score++
  if (/[A-Z]/.test(pwd)) score++
  if (/[0-9]/.test(pwd)) score++
  if (/[^a-zA-Z0-9]/.test(pwd)) score++

  if (score <= 2) {
    return { level: 1, text: 'Weak', class: 'strength-weak' }
  } else if (score <= 4) {
    return { level: 2, text: 'Medium', class: 'strength-medium' }
  } else {
    return { level: 3, text: 'Strong', class: 'strength-strong' }
  }
})

// Reset form when dialog opens
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      form.value = {
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      }
      showCurrentPassword.value = false
      showNewPassword.value = false
      showConfirmPassword.value = false
    }
  }
)

function close() {
  emit('update:visible', false)
}

async function handleSubmit() {
  if (!isFormValid.value) {
    ElMessage.warning('Please fix the validation errors')
    return
  }

  isSubmitting.value = true

  try {
    emit('change-password', {
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword,
    })
    close()
  } finally {
    isSubmitting.value = false
  }
}

// Expose for testing
defineExpose({
  form,
  showCurrentPassword,
  showNewPassword,
  showConfirmPassword,
  isSubmitting,
  currentPasswordError,
  newPasswordError,
  confirmPasswordError,
  isFormValid,
  passwordStrength,
  close,
  handleSubmit,
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title="Change Password"
    width="400px"
    :close-on-click-modal="false"
    class="change-password-dialog"
  >
    <div class="password-form">
      <!-- Current Password -->
      <div class="form-item">
        <label class="form-label">Current Password <span class="required">*</span></label>
        <el-input
          v-model="form.currentPassword"
          :type="showCurrentPassword ? 'text' : 'password'"
          placeholder="Enter current password"
          :class="{ 'is-error': currentPasswordError && form.currentPassword }"
        >
          <template #suffix>
            <el-icon class="password-toggle" @click="showCurrentPassword = !showCurrentPassword">
              <View v-if="showCurrentPassword" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
        <p v-if="currentPasswordError && form.currentPassword" class="error-text">
          {{ currentPasswordError }}
        </p>
      </div>

      <!-- New Password -->
      <div class="form-item">
        <label class="form-label">New Password <span class="required">*</span></label>
        <el-input
          v-model="form.newPassword"
          :type="showNewPassword ? 'text' : 'password'"
          placeholder="Enter new password (min 6 characters)"
          :class="{ 'is-error': newPasswordError && form.newPassword }"
        >
          <template #suffix>
            <el-icon class="password-toggle" @click="showNewPassword = !showNewPassword">
              <View v-if="showNewPassword" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
        <p v-if="newPasswordError && form.newPassword" class="error-text">
          {{ newPasswordError }}
        </p>

        <!-- Password Strength -->
        <div v-if="form.newPassword" class="password-strength">
          <div class="strength-bar">
            <div
              class="strength-fill"
              :class="passwordStrength.class"
              :style="{ width: `${(passwordStrength.level / 3) * 100}%` }"
            ></div>
          </div>
          <span class="strength-text" :class="passwordStrength.class">
            {{ passwordStrength.text }}
          </span>
        </div>
      </div>

      <!-- Confirm Password -->
      <div class="form-item">
        <label class="form-label">Confirm Password <span class="required">*</span></label>
        <el-input
          v-model="form.confirmPassword"
          :type="showConfirmPassword ? 'text' : 'password'"
          placeholder="Re-enter new password"
          :class="{ 'is-error': confirmPasswordError && form.confirmPassword }"
        >
          <template #suffix>
            <el-icon class="password-toggle" @click="showConfirmPassword = !showConfirmPassword">
              <View v-if="showConfirmPassword" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
        <p v-if="confirmPasswordError && form.confirmPassword" class="error-text">
          {{ confirmPasswordError }}
        </p>
      </div>

      <!-- Password Requirements -->
      <div class="password-requirements">
        <p class="requirements-title">Password requirements:</p>
        <ul>
          <li :class="{ met: form.newPassword.length >= 6 }">At least 6 characters</li>
          <li :class="{ met: /[A-Z]/.test(form.newPassword) }">One uppercase letter</li>
          <li :class="{ met: /[0-9]/.test(form.newPassword) }">One number</li>
        </ul>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">Cancel</el-button>
        <el-button
          type="primary"
          :loading="isSubmitting"
          :disabled="!isFormValid"
          @click="handleSubmit"
        >
          Change Password
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.password-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.is-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset;
}

.password-toggle {
  cursor: pointer;
  color: var(--el-text-color-placeholder);
  transition: color 0.2s;
}

.password-toggle:hover {
  color: var(--el-text-color-primary);
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background: var(--el-fill-color);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: width 0.3s, background-color 0.3s;
}

.strength-weak {
  background: var(--el-color-danger);
  color: var(--el-color-danger);
}

.strength-medium {
  background: var(--el-color-warning);
  color: var(--el-color-warning);
}

.strength-strong {
  background: var(--el-color-success);
  color: var(--el-color-success);
}

.strength-text {
  font-size: 12px;
  font-weight: 500;
  min-width: 60px;
}

.password-requirements {
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.requirements-title {
  margin: 0 0 8px 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}

.password-requirements ul {
  margin: 0;
  padding-left: 20px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.password-requirements li {
  margin-bottom: 4px;
  transition: color 0.2s;
}

.password-requirements li.met {
  color: var(--el-color-success);
}

.password-requirements li.met::marker {
  content: '✓ ';
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
