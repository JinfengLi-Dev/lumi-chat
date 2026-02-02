<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import { getErrorMessage } from '@/utils/errorHandler'
import { useKeyboardShortcuts } from '@/composables/useKeyboardShortcuts'
import DeviceManagement from '@/components/settings/DeviceManagement.vue'
import QuickReplySettings from '@/components/settings/QuickReplySettings.vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const { shortcuts, formatShortcut, isEnabled: shortcutsEnabled, setEnabled: setShortcutsEnabled } = useKeyboardShortcuts()

// Browser info for About section
const platformInfo = computed(() => navigator.platform)
const browserInfo = computed(() => navigator.userAgent.split(' ').slice(-1)[0])

const activeTab = ref('profile')
const loading = ref(false)
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

// UID editing state
const uidForm = reactive({
  uid: userStore.user?.uid || '',
})
const originalUid = ref(userStore.user?.uid || '')
const uidChecking = ref(false)
const uidSaving = ref(false)
const uidAvailable = ref<boolean | null>(null)
const uidError = ref('')
let uidCheckTimeout: ReturnType<typeof setTimeout> | null = null

// Computed: Check if UID has changed
const uidChanged = computed(() => uidForm.uid !== originalUid.value)

// Watch for UID changes to check availability
watch(() => uidForm.uid, (newUid) => {
  // Reset state on each change
  uidError.value = ''
  uidAvailable.value = null

  // Clear previous timeout
  if (uidCheckTimeout) {
    clearTimeout(uidCheckTimeout)
  }

  // If UID is same as original, no need to check
  if (newUid === originalUid.value) {
    return
  }

  // Validate format
  if (!newUid || newUid.length < 3) {
    uidError.value = newUid.length > 0 ? 'UID must be at least 3 characters' : ''
    return
  }

  if (newUid.length > 20) {
    uidError.value = 'UID must be at most 20 characters'
    return
  }

  if (!/^[a-zA-Z0-9_]+$/.test(newUid)) {
    uidError.value = 'UID can only contain letters, numbers, and underscores'
    return
  }

  // Debounce the check
  uidCheckTimeout = setTimeout(() => {
    checkUidAvailability()
  }, 500)
})

async function checkUidAvailability() {
  if (!uidChanged.value) return

  uidChecking.value = true
  try {
    const available = await userApi.checkUidAvailability(uidForm.uid)
    uidAvailable.value = available
    if (!available) {
      uidError.value = 'This UID is already taken'
    }
  } catch (error) {
    uidError.value = getErrorMessage(error)
  } finally {
    uidChecking.value = false
  }
}

async function handleUpdateUid() {
  if (!uidChanged.value || uidAvailable.value === false || uidError.value) {
    return
  }

  uidSaving.value = true
  try {
    const updatedUser = await userApi.updateUid(uidForm.uid)
    userStore.user = updatedUser
    originalUid.value = updatedUser.uid
    uidAvailable.value = null
    ElMessage.success('UID updated successfully')
  } catch (error) {
    uidError.value = getErrorMessage(error)
    ElMessage.error(getErrorMessage(error))
  } finally {
    uidSaving.value = false
  }
}

// Nickname editing state
const nicknameForm = reactive({
  nickname: userStore.user?.nickname || '',
})
const originalNickname = ref(userStore.user?.nickname || '')
const nicknameChecking = ref(false)
const nicknameSaving = ref(false)
const nicknameAvailable = ref<boolean | null>(null)
const nicknameError = ref('')
let nicknameCheckTimeout: ReturnType<typeof setTimeout> | null = null

// Computed: Check if nickname has changed
const nicknameChanged = computed(() => nicknameForm.nickname !== originalNickname.value)

// Watch for nickname changes to check availability
watch(() => nicknameForm.nickname, (newNickname) => {
  // Reset state on each change
  nicknameError.value = ''
  nicknameAvailable.value = null

  // Clear previous timeout
  if (nicknameCheckTimeout) {
    clearTimeout(nicknameCheckTimeout)
  }

  // If nickname is same as original, no need to check
  if (newNickname === originalNickname.value) {
    return
  }

  // Validate format
  if (!newNickname || newNickname.length < 2) {
    nicknameError.value = newNickname.length > 0 ? 'Nickname must be at least 2 characters' : ''
    return
  }

  if (newNickname.length > 30) {
    nicknameError.value = 'Nickname must be at most 30 characters'
    return
  }

  // Debounce the check
  nicknameCheckTimeout = setTimeout(() => {
    checkNicknameAvailability()
  }, 500)
})

async function checkNicknameAvailability() {
  if (!nicknameChanged.value) return

  nicknameChecking.value = true
  try {
    const available = await userApi.checkNicknameAvailability(nicknameForm.nickname)
    nicknameAvailable.value = available
    if (!available) {
      nicknameError.value = 'This nickname is already taken'
    }
  } catch (error) {
    nicknameError.value = getErrorMessage(error)
  } finally {
    nicknameChecking.value = false
  }
}

async function handleUpdateNickname() {
  if (!nicknameChanged.value || nicknameAvailable.value === false || nicknameError.value) {
    return
  }

  nicknameSaving.value = true
  try {
    const updatedUser = await userApi.updateNickname(nicknameForm.nickname)
    userStore.user = updatedUser
    originalNickname.value = updatedUser.nickname
    nicknameAvailable.value = null
    ElMessage.success('Nickname updated successfully')
  } catch (error) {
    nicknameError.value = getErrorMessage(error)
    ElMessage.error(getErrorMessage(error))
  } finally {
    nicknameSaving.value = false
  }
}

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

function handleThemeChange(val: string | number | boolean | undefined) {
  if (typeof val === 'string' && ['light', 'dark', 'system'].includes(val)) {
    themeStore.setMode(val as ThemeMode)
  }
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
          :class="{ active: activeTab === 'appearance' }"
          @click="activeTab = 'appearance'"
        >
          <el-icon><Sunny /></el-icon>
          <span>Appearance</span>
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
          :class="{ active: activeTab === 'quick-replies' }"
          @click="activeTab = 'quick-replies'"
        >
          <el-icon><ChatLineSquare /></el-icon>
          <span>Quick Replies</span>
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
            <div class="uid-input-wrapper">
              <el-input
                v-model="uidForm.uid"
                placeholder="Enter your unique ID"
                :class="{ 'uid-available': uidAvailable === true && uidChanged, 'uid-taken': uidAvailable === false }"
              >
                <template #suffix>
                  <el-icon v-if="uidChecking" class="is-loading"><Loading /></el-icon>
                  <el-icon v-else-if="uidAvailable === true && uidChanged" class="uid-success-icon"><CircleCheck /></el-icon>
                  <el-icon v-else-if="uidAvailable === false" class="uid-error-icon"><CircleClose /></el-icon>
                </template>
              </el-input>
              <el-button
                v-if="uidChanged"
                type="primary"
                size="small"
                :loading="uidSaving"
                :disabled="uidAvailable !== true || !!uidError"
                @click="handleUpdateUid"
              >
                Save
              </el-button>
            </div>
            <div v-if="uidError" class="uid-status uid-error">{{ uidError }}</div>
            <div v-else-if="uidAvailable === true && uidChanged" class="uid-status uid-success">UID is available</div>
            <div class="uid-hint">3-20 characters, letters, numbers, and underscores only</div>
          </el-form-item>

          <el-form-item label="Email">
            <el-input :value="userStore.user?.email" disabled />
          </el-form-item>

          <el-form-item label="Nickname">
            <div class="nickname-input-wrapper">
              <el-input
                v-model="nicknameForm.nickname"
                placeholder="Enter your display name"
                :class="{
                  'nickname-available': nicknameAvailable === true && nicknameChanged,
                  'nickname-taken': nicknameAvailable === false
                }"
              >
                <template #suffix>
                  <el-icon v-if="nicknameChecking" class="is-loading"><Loading /></el-icon>
                  <el-icon v-else-if="nicknameAvailable === true && nicknameChanged" class="nickname-success-icon">
                    <CircleCheck />
                  </el-icon>
                  <el-icon v-else-if="nicknameAvailable === false" class="nickname-error-icon">
                    <CircleClose />
                  </el-icon>
                </template>
              </el-input>
              <el-button
                v-if="nicknameChanged"
                type="primary"
                size="small"
                :loading="nicknameSaving"
                :disabled="nicknameAvailable !== true || !!nicknameError"
                @click="handleUpdateNickname"
              >
                Save
              </el-button>
            </div>
            <div v-if="nicknameError" class="nickname-status nickname-error">{{ nicknameError }}</div>
            <div v-else-if="nicknameAvailable === true && nicknameChanged" class="nickname-status nickname-success">
              Nickname is available ✓
            </div>
            <div class="nickname-hint">2-30 characters, your display name in conversations</div>
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

      <!-- Quick Replies Tab -->
      <div v-if="activeTab === 'quick-replies'" class="settings-section">
        <h2>Quick Reply Templates</h2>
        <p class="section-desc">Create preset messages for quick responses in chats</p>

        <QuickReplySettings />
      </div>

      <!-- Appearance Tab -->
      <div v-if="activeTab === 'appearance'" class="settings-section">
        <h2>Appearance</h2>
        <p class="section-desc">Customize how Lumi Chat looks</p>

        <div class="appearance-settings">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">Theme</span>
              <span class="setting-desc">Choose your preferred color scheme</span>
            </div>
            <el-radio-group
              :model-value="themeStore.mode"
              @change="handleThemeChange"
            >
              <el-radio-button value="light">
                <el-icon><Sunny /></el-icon>
                Light
              </el-radio-button>
              <el-radio-button value="dark">
                <el-icon><Moon /></el-icon>
                Dark
              </el-radio-button>
              <el-radio-button value="system">
                <el-icon><Monitor /></el-icon>
                System
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="theme-preview">
            <div class="preview-label">Preview</div>
            <div class="preview-card" :class="{ 'dark-preview': themeStore.isDark }">
              <div class="preview-header">
                <div class="preview-avatar"></div>
                <div class="preview-title">Chat Preview</div>
              </div>
              <div class="preview-messages">
                <div class="preview-message other">
                  <div class="preview-bubble">Hello!</div>
                </div>
                <div class="preview-message self">
                  <div class="preview-bubble">Hi there!</div>
                </div>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">Keyboard Shortcuts</span>
              <span class="setting-desc">Enable keyboard shortcuts for faster navigation</span>
            </div>
            <el-switch
              :model-value="shortcutsEnabled"
              @update:model-value="(val) => setShortcutsEnabled(!!val)"
            />
          </div>

          <div v-if="shortcutsEnabled" class="shortcuts-list">
            <div class="shortcuts-list-header">
              <span>Available Shortcuts</span>
              <span class="shortcuts-tip">Press <kbd>Ctrl</kbd> + <kbd>/</kbd> anytime</span>
            </div>
            <div class="shortcut-grid">
              <div v-for="shortcut in shortcuts" :key="shortcut.key" class="shortcut-row">
                <span class="shortcut-keys">{{ formatShortcut(shortcut) }}</span>
                <span class="shortcut-desc">{{ shortcut.description }}</span>
              </div>
            </div>
          </div>
        </div>
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
  background-color: var(--lc-bg-page);
  transition: background-color 0.3s ease;
}

.settings-sidebar {
  width: 250px;
  background-color: var(--lc-bg-white);
  border-right: 1px solid var(--lc-border-color);
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.settings-header {
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  border-bottom: 1px solid var(--lc-border-color);
  font-size: 16px;
  font-weight: 500;
  color: var(--lc-text-primary);
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
  color: var(--lc-text-regular);
  transition: all 0.2s;
}

.menu-item:hover {
  background-color: var(--lc-bg-hover);
}

.menu-item.active {
  background-color: var(--lc-bg-active);
  color: var(--lc-primary);
}

.settings-content {
  flex: 1;
  padding: 30px 40px;
  overflow-y: auto;
}

.settings-section h2 {
  margin-bottom: 10px;
  color: var(--lc-text-primary);
}

.section-desc {
  color: var(--lc-text-secondary);
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
  color: var(--lc-text-secondary);
}

.about-info {
  background-color: var(--lc-bg-white);
  padding: 20px;
  border-radius: 8px;
  transition: background-color 0.3s ease;
}

.about-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid var(--lc-border-color);
}

.about-item:last-child {
  border-bottom: none;
}

.about-item .label {
  width: 100px;
  color: var(--lc-text-secondary);
}

/* Appearance Settings */
.appearance-settings {
  max-width: 600px;
}

.setting-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px;
  background-color: var(--lc-bg-white);
  border-radius: 8px;
  margin-bottom: 20px;
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.setting-label {
  font-size: 15px;
  font-weight: 500;
  color: var(--lc-text-primary);
}

.setting-desc {
  font-size: 13px;
  color: var(--lc-text-secondary);
}

.theme-preview {
  margin-top: 20px;
}

.preview-label {
  font-size: 13px;
  color: var(--lc-text-secondary);
  margin-bottom: 12px;
}

.preview-card {
  width: 300px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: var(--lc-shadow);
  background-color: #f5f7fa;
  transition: all 0.3s ease;
}

.preview-card.dark-preview {
  background-color: #1a1a1a;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.dark-preview .preview-header {
  background-color: #242424;
  border-bottom-color: #3a3a3a;
}

.preview-avatar {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  background-color: #409eff;
}

.preview-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.dark-preview .preview-title {
  color: #e5e5e5;
}

.preview-messages {
  padding: 16px;
}

.preview-message {
  margin-bottom: 12px;
}

.preview-message:last-child {
  margin-bottom: 0;
}

.preview-message.self {
  display: flex;
  justify-content: flex-end;
}

.preview-bubble {
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  max-width: 70%;
}

.preview-message.other .preview-bubble {
  background-color: #fff;
  color: #303133;
}

.dark-preview .preview-message.other .preview-bubble {
  background-color: #2c2c2c;
  color: #e5e5e5;
}

.preview-message.self .preview-bubble {
  background-color: #95ec69;
  color: #303133;
}

.dark-preview .preview-message.self .preview-bubble {
  background-color: #2d5a1e;
  color: #e5e5e5;
}

/* Keyboard Shortcuts List */
.shortcuts-list {
  margin-top: 20px;
  padding: 16px;
  background-color: var(--lc-bg-hover);
  border-radius: 8px;
}

.shortcuts-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  font-weight: 500;
  color: var(--lc-text-primary);
}

.shortcuts-tip {
  font-size: 12px;
  font-weight: normal;
  color: var(--lc-text-secondary);
}

.shortcuts-tip kbd {
  font-family: monospace;
  font-size: 11px;
  padding: 2px 6px;
  background-color: var(--lc-bg-white);
  border: 1px solid var(--lc-border-color);
  border-radius: 3px;
}

.shortcut-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shortcut-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--lc-border-color);
}

.shortcut-row:last-child {
  border-bottom: none;
}

.shortcut-keys {
  font-family: monospace;
  font-size: 12px;
  padding: 4px 8px;
  background-color: var(--lc-bg-white);
  border: 1px solid var(--lc-border-color);
  border-radius: 4px;
  color: var(--lc-text-primary);
}

.shortcut-desc {
  color: var(--lc-text-regular);
  font-size: 13px;
}

/* UID Input Styles */
.uid-input-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.uid-input-wrapper .el-input {
  flex: 1;
}

.uid-status {
  font-size: 12px;
  margin-top: 4px;
}

.uid-status.uid-error {
  color: var(--el-color-danger);
}

.uid-status.uid-success {
  color: var(--el-color-success);
}

.uid-hint {
  font-size: 11px;
  color: var(--lc-text-secondary);
  margin-top: 4px;
}

.uid-success-icon {
  color: var(--el-color-success);
}

.uid-error-icon {
  color: var(--el-color-danger);
}

.uid-available :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-success) inset;
}

.uid-taken :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset;
}

/* Nickname Input Styles */
.nickname-input-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.nickname-input-wrapper .el-input {
  flex: 1;
}

.nickname-status {
  font-size: 12px;
  margin-top: 4px;
}

.nickname-status.nickname-error {
  color: var(--el-color-danger);
}

.nickname-status.nickname-success {
  color: var(--el-color-success);
}

.nickname-hint {
  font-size: 11px;
  color: var(--lc-text-secondary);
  margin-top: 4px;
}

.nickname-success-icon {
  color: var(--el-color-success);
}

.nickname-error-icon {
  color: var(--el-color-danger);
}

.nickname-available :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-success) inset;
  transition: all 0.3s ease;
}

.nickname-taken :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset;
  transition: all 0.3s ease;
}
</style>
