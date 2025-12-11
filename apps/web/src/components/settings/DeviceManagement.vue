<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Monitor, Iphone, Delete, SuccessFilled } from '@element-plus/icons-vue'
import { deviceApi, type Device } from '@/api'

const devices = ref<Device[]>([])
const loading = ref(false)
const currentDeviceId = ref<string>('')

// Get current device ID from localStorage
onMounted(() => {
  currentDeviceId.value = localStorage.getItem('deviceId') || ''
  fetchDevices()
})

async function fetchDevices() {
  loading.value = true
  try {
    devices.value = await deviceApi.getDevices()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || 'Failed to load devices')
  } finally {
    loading.value = false
  }
}

const sortedDevices = computed(() => {
  // Sort: current device first, then by lastActiveAt
  return [...devices.value].sort((a, b) => {
    if (a.deviceId === currentDeviceId.value) return -1
    if (b.deviceId === currentDeviceId.value) return 1
    return new Date(b.lastActiveAt).getTime() - new Date(a.lastActiveAt).getTime()
  })
})

function getDeviceIcon(deviceType: string) {
  switch (deviceType.toLowerCase()) {
    case 'ios':
    case 'android':
      return Iphone
    case 'web':
    case 'pc':
    default:
      return Monitor
  }
}

function getDeviceTypeName(deviceType: string) {
  switch (deviceType.toLowerCase()) {
    case 'ios':
      return 'iPhone/iPad'
    case 'android':
      return 'Android'
    case 'web':
      return 'Web Browser'
    case 'pc':
      return 'Desktop'
    default:
      return deviceType
  }
}

function formatLastActive(dateStr: string) {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins} min ago`
  if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`
  if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`

  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
  })
}

async function handleLogoutDevice(device: Device) {
  if (device.deviceId === currentDeviceId.value) {
    ElMessage.warning('You cannot log out of your current device from here')
    return
  }

  try {
    await ElMessageBox.confirm(
      `Are you sure you want to log out "${device.deviceName || getDeviceTypeName(device.deviceType)}"? This will end the session on that device.`,
      'Log Out Device',
      {
        confirmButtonText: 'Log Out',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )

    await deviceApi.logoutDevice(device.deviceId)
    ElMessage.success('Device logged out successfully')
    await fetchDevices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || 'Failed to log out device')
    }
  }
}

async function handleLogoutAllDevices() {
  try {
    await ElMessageBox.confirm(
      'Are you sure you want to log out of all other devices? This will end all sessions except your current one.',
      'Log Out All Devices',
      {
        confirmButtonText: 'Log Out All',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )

    await deviceApi.logoutAllDevices()
    ElMessage.success('All other devices logged out')
    await fetchDevices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || 'Failed to log out devices')
    }
  }
}
</script>

<template>
  <div class="device-management">
    <div class="section-header">
      <h3 class="section-title">Logged-in Devices</h3>
      <el-button
        v-if="devices.length > 1"
        type="danger"
        text
        size="small"
        @click="handleLogoutAllDevices"
      >
        Log Out All Others
      </el-button>
    </div>

    <div v-loading="loading" class="devices-list">
      <div
        v-for="device in sortedDevices"
        :key="device.deviceId"
        class="device-item"
        :class="{ current: device.deviceId === currentDeviceId }"
      >
        <div class="device-icon">
          <el-icon :size="28" :color="device.isOnline ? '#67c23a' : '#909399'">
            <component :is="getDeviceIcon(device.deviceType)" />
          </el-icon>
        </div>

        <div class="device-info">
          <div class="device-name-row">
            <span class="device-name">
              {{ device.deviceName || getDeviceTypeName(device.deviceType) }}
            </span>
            <el-tag
              v-if="device.deviceId === currentDeviceId"
              type="success"
              size="small"
              effect="plain"
            >
              <el-icon><SuccessFilled /></el-icon>
              Current
            </el-tag>
            <el-tag v-else-if="device.isOnline" type="success" size="small" effect="light">
              Online
            </el-tag>
          </div>
          <div class="device-meta">
            <span class="device-type">{{ getDeviceTypeName(device.deviceType) }}</span>
            <span class="separator">-</span>
            <span class="last-active">{{ formatLastActive(device.lastActiveAt) }}</span>
          </div>
        </div>

        <div class="device-actions">
          <el-button
            v-if="device.deviceId !== currentDeviceId"
            type="danger"
            text
            :icon="Delete"
            @click="handleLogoutDevice(device)"
          >
            Log Out
          </el-button>
        </div>
      </div>

      <div v-if="!loading && devices.length === 0" class="empty-state">
        <el-icon :size="48" color="#c0c4cc"><Monitor /></el-icon>
        <p>No devices found</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.device-management {
  padding: 4px 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--el-text-color-primary);
}

.devices-list {
  min-height: 100px;
}

.device-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin-bottom: 12px;
  transition: border-color 0.2s;
}

.device-item:hover {
  border-color: var(--el-border-color);
}

.device-item.current {
  border-color: var(--el-color-success-light-3);
  background: var(--el-color-success-light-9);
}

.device-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  border-radius: 12px;
}

.device-item.current .device-icon {
  background: var(--el-color-success-light-7);
}

.device-info {
  flex: 1;
  min-width: 0;
}

.device-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.device-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.device-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.separator {
  color: var(--el-border-color);
}

.device-actions {
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--el-text-color-secondary);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}
</style>
