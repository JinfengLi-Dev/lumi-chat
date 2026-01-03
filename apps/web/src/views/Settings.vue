<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/utils/errorHandler'
import DeviceManagement from '@/components/settings/DeviceManagement.vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// Browser info for About section
const platformInfo = computed(() => navigator.platform)
const browserInfo = computed(() => navigator.userAgent.split(' ').slice(-1)[0])

const activeTab = ref('profile')
const loading = ref(false)
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

const profileForm = reactive({
  nickname: userStore.user?.nickname || '',
  gender: userStore.user?.gender || 'unknown',
  signature: userStore.user?.signature || '',
  description: userStore.user?.description || '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules: FormRules = {
  nickname: [
    { required: true, message: 'Please enter your nickname', trigger: 'blur' },
    { min: 2, max: 20, message: 'Nickname must be 2-20 characters', trigger: 'blur' },
  ],
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: 'Please enter your current password', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: 'Please enter your new password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm your new password', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('Passwords do not match'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleUpdateProfile() {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.updateProfile(profileForm)
    ElMessage.success('Profile updated successfully')
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    ElMessage.success('Password changed successfully')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function handleUploadAvatar(file: File) {
  try {
    await userStore.updateAvatar(file)
    ElMessage.success('Avatar updated successfully')
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error))
  }
  return false // Prevent default upload
}

</script>

<template>
  <div class="settings-page">
    <div class="settings-sidebar">
      <div class="settings-header">
        <el-icon :size="20" @click="router.push('/')" style="cursor: pointer">
          <ArrowLeft />
        </el-icon>
        <span>Settings</span>
      </div>

      <div class="settings-menu">
        <div
          class="menu-item"
          :class="{ active: activeTab === 'profile' }"
          @click="activeTab = 'profile'"
        >
          <el-icon><User /></el-icon>
          <span>Profile</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: activeTab === 'password' }"
          @click="activeTab = 'password'"
        >
          <el-icon><Lock /></el-icon>
          <span>Password</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: activeTab === 'devices' }"
          @click="activeTab = 'devices'"
        >
          <el-icon><Monitor /></el-icon>
          <span>Devices</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: activeTab === 'about' }"
          @click="activeTab = 'about'"
        >
          <el-icon><InfoFilled /></el-icon>
          <span>About</span>
        </div>
      </div>
    </div>

    <div class="settings-content">
      <!-- Profile Tab -->
      <div v-if="activeTab === 'profile'" class="settings-section">
        <h2>Profile Settings</h2>

        <div class="avatar-section">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :before-upload="handleUploadAvatar"
            accept="image/*"
          >
            <el-avatar :src="userStore.user?.avatar" :size="100">
              {{ userStore.user?.nickname?.charAt(0) }}
            </el-avatar>
            <div class="avatar-overlay">
              <el-icon><Camera /></el-icon>
            </div>
          </el-upload>
          <p class="avatar-tip">Click to change avatar</p>
        </div>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="100px"
          style="max-width: 500px"
        >
          <el-form-item label="UID">
            <el-input :value="userStore.user?.uid" disabled />
          </el-form-item>

          <el-form-item label="Email">
            <el-input :value="userStore.user?.email" disabled />
          </el-form-item>

          <el-form-item label="Nickname" prop="nickname">
            <el-input v-model="profileForm.nickname" />
          </el-form-item>

          <el-form-item label="Gender" prop="gender">
            <el-radio-group v-model="profileForm.gender">
              <el-radio value="male">Male</el-radio>
              <el-radio value="female">Female</el-radio>
              <el-radio value="unknown">Unknown</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="Signature">
            <el-input v-model="profileForm.signature" maxlength="100" show-word-limit />
          </el-form-item>

          <el-form-item label="Description">
            <el-input
              v-model="profileForm.description"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleUpdateProfile">
              Save Changes
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- Password Tab -->
      <div v-if="activeTab === 'password'" class="settings-section">
        <h2>Change Password</h2>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="150px"
          style="max-width: 500px"
        >
          <el-form-item label="Current Password" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>

          <el-form-item label="New Password" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>

          <el-form-item label="Confirm Password" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleChangePassword">
              Change Password
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- Devices Tab -->
      <div v-if="activeTab === 'devices'" class="settings-section">
        <h2>Logged-in Devices</h2>
        <p class="section-desc">Manage devices that are logged into your account</p>

        <DeviceManagement />
      </div>

      <!-- About Tab -->
      <div v-if="activeTab === 'about'" class="settings-section">
        <h2>About Lumi Chat</h2>

        <div class="about-info">
          <div class="about-item">
            <span class="label">Version:</span>
            <span>1.0.0</span>
          </div>
          <div class="about-item">
            <span class="label">Platform:</span>
            <span>{{ platformInfo }}</span>
          </div>
          <div class="about-item">
            <span class="label">Browser:</span>
            <span>{{ browserInfo }}</span>
          </div>
        </div>

        <p style="margin-top: 30px; color: #909399">
          Lumi Chat - Cross-platform Instant Messaging
          <br>
          Built with MobileIMSDK
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
}

.settings-sidebar {
  width: 250px;
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
}

.settings-header {
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 16px;
  font-weight: 500;
}

.settings-menu {
  padding: 10px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  color: #606266;
  transition: all 0.2s;
}

.menu-item:hover {
  background-color: #f5f7fa;
}

.menu-item.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.settings-content {
  flex: 1;
  padding: 30px 40px;
  overflow-y: auto;
}

.settings-section h2 {
  margin-bottom: 10px;
  color: #303133;
}

.section-desc {
  color: #909399;
  margin-bottom: 30px;
}

.avatar-section {
  margin: 30px 0;
  text-align: center;
}

.avatar-uploader {
  display: inline-block;
  position: relative;
  cursor: pointer;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 24px;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-uploader:hover .avatar-overlay {
  opacity: 1;
}

.avatar-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.about-info {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
}

.about-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #e4e7ed;
}

.about-item:last-child {
  border-bottom: none;
}

.about-item .label {
  width: 100px;
  color: #909399;
}
</style>
