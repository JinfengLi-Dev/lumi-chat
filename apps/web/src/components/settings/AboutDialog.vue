<script setup lang="ts">
import { ChatDotRound, InfoFilled } from '@element-plus/icons-vue'

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

// App info
const appName = 'Lumi-Chat'
const appVersion = '1.0.0'
const buildDate = '2025-12-07'
const copyright = '2025 Lumi Technologies'

function close() {
  emit('update:visible', false)
}

// Expose for testing
defineExpose({
  appName,
  appVersion,
  buildDate,
  copyright,
  close,
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title=""
    width="360px"
    :show-close="true"
    class="about-dialog"
  >
    <div class="about-content">
      <!-- App Logo -->
      <div class="app-logo">
        <el-icon :size="64" color="var(--el-color-primary)">
          <ChatDotRound />
        </el-icon>
      </div>

      <!-- App Name & Version -->
      <h2 class="app-name">{{ appName }}</h2>
      <p class="app-version">Version {{ appVersion }}</p>

      <!-- App Description -->
      <p class="app-description">
        A modern cross-platform instant messaging application for seamless communication.
      </p>

      <!-- Info Section -->
      <div class="info-section">
        <div class="info-item">
          <span class="info-label">Build Date</span>
          <span class="info-value">{{ buildDate }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">Platform</span>
          <span class="info-value">Web</span>
        </div>
      </div>

      <!-- Links -->
      <div class="links-section">
        <a href="https://github.com/lumichat" target="_blank" rel="noopener" class="about-link">
          <el-icon><InfoFilled /></el-icon>
          View on GitHub
        </a>
      </div>

      <!-- Copyright -->
      <p class="copyright">&copy; {{ copyright }}</p>
    </div>

    <template #footer>
      <el-button type="primary" @click="close">Close</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.about-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px 0;
}

.app-logo {
  margin-bottom: 16px;
}

.app-name {
  margin: 0 0 4px 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.app-version {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.app-description {
  margin: 0 0 20px 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  max-width: 280px;
  line-height: 1.5;
}

.info-section {
  width: 100%;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
}

.info-item:not(:last-child) {
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 6px;
}

.info-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.info-value {
  font-size: 13px;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.links-section {
  margin-bottom: 16px;
}

.about-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--el-color-primary);
  text-decoration: none;
  transition: opacity 0.2s;
}

.about-link:hover {
  opacity: 0.8;
}

.copyright {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
