<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/utils/errorHandler'
import type { FormInstance, FormRules } from 'element-plus'

const formRef = ref<FormInstance>()
const loading = ref(false)
const sent = ref(false)

const form = reactive({
  email: '',
})

const rules: FormRules = {
  email: [
    { required: true, message: 'Please enter your email', trigger: 'blur' },
    { type: 'email', message: 'Please enter a valid email', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.forgotPassword(form.email)
    sent.value = true
    ElMessage.success('Password reset email sent!')
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="logo">
        <h1>Lumi Chat</h1>
        <p>Reset your password</p>
      </div>

      <template v-if="!sent">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="auth-form"
          @submit.prevent="handleSubmit"
        >
          <el-form-item prop="email">
            <el-input
              v-model="form.email"
              placeholder="Enter your email"
              size="large"
              prefix-icon="Message"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              native-type="submit"
              class="submit-btn"
              :loading="loading"
            >
              Send Reset Email
            </el-button>
          </el-form-item>
        </el-form>
      </template>

      <template v-else>
        <div class="success-message">
          <el-icon :size="60" color="#67c23a">
            <CircleCheck />
          </el-icon>
          <h3>Email Sent!</h3>
          <p>
            We've sent a password reset link to <strong>{{ form.email }}</strong>.
            Please check your inbox.
          </p>
        </div>
      </template>

      <div class="auth-footer">
        <router-link to="/login">Back to Login</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.success-message {
  text-align: center;
  padding: 20px 0;
}

.success-message h3 {
  margin: 20px 0 10px;
  color: #303133;
}

.success-message p {
  color: #606266;
  line-height: 1.6;
}
</style>
