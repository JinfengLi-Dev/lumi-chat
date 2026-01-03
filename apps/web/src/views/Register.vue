<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getErrorMessage } from '@/utils/errorHandler'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
  gender: 'male' as 'male' | 'female',
  agreeTerms: false,
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('Passwords do not match'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  nickname: [
    { required: true, message: 'Please enter your nickname', trigger: 'blur' },
    { min: 2, max: 20, message: 'Nickname must be 2-20 characters', trigger: 'blur' },
  ],
  email: [
    { required: true, message: 'Please enter your email', trigger: 'blur' },
    { type: 'email', message: 'Please enter a valid email', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Please enter your password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm your password', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  gender: [
    { required: true, message: 'Please select your gender', trigger: 'change' },
  ],
  agreeTerms: [
    {
      validator: (_rule: unknown, value: boolean, callback: (error?: Error) => void) => {
        if (!value) {
          callback(new Error('You must agree to the terms of service'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register({
      nickname: form.nickname,
      email: form.email,
      password: form.password,
      gender: form.gender,
    })

    ElMessage.success('Registration successful! Please login.')
    router.push('/login')
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
        <p>Create your account</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="auth-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="Nickname"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="Email"
            size="large"
            prefix-icon="Message"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="Password (min 6 characters)"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="Confirm Password"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio value="male">Male</el-radio>
            <el-radio value="female">Female</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item prop="agreeTerms">
          <el-checkbox v-model="form.agreeTerms">
            I agree to the
            <a href="#" class="text-primary" @click.prevent>Terms of Service</a>
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            class="submit-btn"
            :loading="loading"
          >
            Register
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        Already have an account?
        <router-link to="/login">Login</router-link>
      </div>
    </div>
  </div>
</template>
