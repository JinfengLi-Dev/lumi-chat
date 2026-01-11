<script setup lang="ts">
import { computed } from 'vue'
import { useKeyboardShortcuts } from '@/composables/useKeyboardShortcuts'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const { shortcuts, formatShortcut, isEnabled, setEnabled } = useKeyboardShortcuts()

// Group shortcuts by category
const navigationShortcuts = computed(() =>
  shortcuts.filter((s) => ['1', '2', '3', ','].includes(s.key))
)

const actionShortcuts = computed(() =>
  shortcuts.filter((s) => ['d', '/', 'Escape', 'f'].includes(s.key))
)

const conversationShortcuts = computed(() =>
  shortcuts.filter((s) => ['ArrowUp', 'ArrowDown'].includes(s.key))
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="Keyboard Shortcuts"
    width="500px"
    destroy-on-close
  >
    <div class="shortcuts-content">
      <div class="shortcuts-toggle">
        <span>Enable keyboard shortcuts</span>
        <el-switch
          :model-value="isEnabled"
          @update:model-value="setEnabled"
        />
      </div>

      <div class="shortcut-section">
        <h4>Navigation</h4>
        <div class="shortcut-list">
          <div
            v-for="shortcut in navigationShortcuts"
            :key="shortcut.key"
            class="shortcut-item"
          >
            <span class="shortcut-keys">{{ formatShortcut(shortcut) }}</span>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        </div>
      </div>

      <div class="shortcut-section">
        <h4>Actions</h4>
        <div class="shortcut-list">
          <div
            v-for="shortcut in actionShortcuts"
            :key="shortcut.key"
            class="shortcut-item"
          >
            <span class="shortcut-keys">{{ formatShortcut(shortcut) }}</span>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        </div>
      </div>

      <div class="shortcut-section">
        <h4>Conversations</h4>
        <div class="shortcut-list">
          <div
            v-for="shortcut in conversationShortcuts"
            :key="shortcut.key"
            class="shortcut-item"
          >
            <span class="shortcut-keys">{{ formatShortcut(shortcut) }}</span>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        </div>
      </div>

      <div class="shortcut-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>Press <kbd>Ctrl</kbd> + <kbd>/</kbd> anytime to show this help</span>
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">Close</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.shortcuts-content {
  padding: 0 10px;
}

.shortcuts-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: var(--lc-bg-hover);
  border-radius: 8px;
  margin-bottom: 20px;
}

.shortcut-section {
  margin-bottom: 20px;
}

.shortcut-section h4 {
  margin-bottom: 12px;
  color: var(--lc-text-secondary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.shortcut-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.shortcut-keys {
  font-family: monospace;
  font-size: 12px;
  padding: 4px 8px;
  background-color: var(--lc-bg-hover);
  border: 1px solid var(--lc-border-color);
  border-radius: 4px;
  color: var(--lc-text-primary);
}

.shortcut-desc {
  color: var(--lc-text-regular);
  font-size: 14px;
}

.shortcut-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: var(--lc-bg-active);
  border-radius: 8px;
  font-size: 13px;
  color: var(--lc-text-secondary);
}

.shortcut-tip kbd {
  font-family: monospace;
  font-size: 11px;
  padding: 2px 6px;
  background-color: var(--lc-bg-white);
  border: 1px solid var(--lc-border-color);
  border-radius: 3px;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.1);
}
</style>
