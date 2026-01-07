<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getErrorMessage } from '@/utils/errorHandler'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  email: '',
  password: '',
  rememberMe: false,
})

const rules: FormRules = {
  email: [
    { required: true, message: 'Please enter your email or UID', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Please enter your password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
  ],
}

function fillDemo(email: string, password: string) {
  form.email = email
  form.password = password
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({
      email: form.email,
      password: form.password,
      rememberMe: form.rememberMe,
    })

    ElMessage.success('Login successful')

    // Redirect to original page or home
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
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
        <p>Cross-platform Instant Messaging</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="auth-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="Email or UID"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="Password"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <div class="flex-between" style="width: 100%">
            <el-checkbox v-model="form.rememberMe">Remember me</el-checkbox>
            <router-link to="/forgot-password" class="text-primary">
              Forgot password?
            </router-link>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            class="submit-btn"
            :loading="loading"
          >
            Login
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        Don't have an account?
        <router-link to="/register">Register now</router-link>
      </div>

      <el-divider>Demo Accounts</el-divider>

      <div class="demo-credentials">
        <div class="demo-account" @click="fillDemo('alice@demo.com', 'demo123')">
          <el-tag type="info" size="small">Alice</el-tag>
          <span class="demo-info">alice@demo.com / demo123</span>
        </div>
        <div class="demo-account" @click="fillDemo('bob@demo.com', 'demo123')">
          <el-tag type="success" size="small">Bob</el-tag>
          <span class="demo-info">bob@demo.com / demo123</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.demo-credentials {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.demo-account {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.demo-account:hover {
  background-color: var(--el-fill-color-light);
  border-color: var(--el-color-primary-light-5);
}

.demo-info {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
