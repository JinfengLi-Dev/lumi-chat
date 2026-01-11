<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, Warning } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/utils/errorHandler'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)
const resetSuccess = ref(false)
const tokenError = ref('')
const token = ref('')

const form = reactive({
  password: '',
  confirmPassword: '',
})

// Validate password confirmation matches
const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('Passwords do not match'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  password: [
    { required: true, message: 'Please enter a new password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm your password', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

onMounted(() => {
  // Get token from URL query parameter
  const urlToken = route.query.token as string
  if (!urlToken) {
    tokenError.value = 'Invalid Reset Link'
  } else {
    token.value = urlToken
  }
})

async function handleReset() {
  if (tokenError.value) return

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.resetPassword(token.value, form.password)
    resetSuccess.value = true
    ElMessage.success('Password reset successful!')
  } catch (error: unknown) {
    const message = getErrorMessage(error)
    // Check for common token errors
    if (message.toLowerCase().includes('expired')) {
      tokenError.value = 'Reset Link Expired'
    } else if (message.toLowerCase().includes('invalid')) {
      tokenError.value = 'Invalid Reset Link'
    } else {
      ElMessage.error(message)
    }
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}

function requestNewLink() {
  router.push('/forgot-password')
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="logo">
        <h1>Lumi Chat</h1>
        <p>Reset your password</p>
      </div>

      <!-- Success State -->
      <template v-if="resetSuccess">
        <div class="success-message">
          <el-icon :size="60" color="#67c23a">
            <CircleCheck />
          </el-icon>
          <h3>Password Reset Successful!</h3>
          <p>
            Your password has been reset successfully.
            You can now login with your new password.
          </p>
          <el-button type="primary" size="large" @click="goToLogin">
            Go to Login
          </el-button>
        </div>
      </template>

      <!-- Error State (invalid or expired token) -->
      <template v-else-if="tokenError">
        <div class="error-message">
          <el-icon :size="60" color="#f56c6c">
            <Warning />
          </el-icon>
          <h3>{{ tokenError }}</h3>
          <p>
            This password reset link is invalid or has expired.
            Please request a new one.
          </p>
          <el-button type="primary" size="large" @click="requestNewLink">
            Request New Link
          </el-button>
        </div>
      </template>

      <!-- Reset Form -->
      <template v-else>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="auth-form"
          @submit.prevent="handleReset"
        >
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="New Password"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="Confirm New Password"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              native-type="submit"
              class="submit-btn"
              :loading="loading"
            >
              Reset Password
            </el-button>
          </el-form-item>
        </el-form>
      </template>

      <div class="auth-footer">
        <router-link to="/login">Back to Login</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.success-message,
.error-message {
  text-align: center;
  padding: 20px 0;
}

.success-message h3,
.error-message h3 {
  margin: 20px 0 10px;
  color: var(--lc-text-primary, #303133);
}

.success-message p,
.error-message p {
  color: var(--lc-text-secondary, #606266);
  line-height: 1.6;
  margin-bottom: 20px;
}
</style>
