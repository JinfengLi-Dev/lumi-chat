<script setup lang="ts">
import { computed } from 'vue'
import type { User } from '@/types'

const props = defineProps<{
  // Users currently typing
  typingUsers: User[]
  // Maximum number of names to show before truncating
  maxNamesToShow?: number
}>()

const maxNames = computed(() => props.maxNamesToShow ?? 2)

const displayText = computed(() => {
  const count = props.typingUsers.length
  if (count === 0) return ''

  const names = props.typingUsers.slice(0, maxNames.value).map((u) => u.nickname)

  if (count === 1) {
    return `${names[0]} is typing`
  }

  if (count === 2) {
    return `${names[0]} and ${names[1]} are typing`
  }

  if (count <= maxNames.value) {
    const lastIndex = names.length - 1
    return `${names.slice(0, lastIndex).join(', ')} and ${names[lastIndex]} are typing`
  }

  // More users than maxNamesToShow
  const othersCount = count - maxNames.value
  return `${names.join(', ')} and ${othersCount} other${othersCount > 1 ? 's' : ''} are typing`
})
</script>

<template>
  <Transition name="fade">
    <div v-if="typingUsers.length > 0" class="typing-indicator">
      <div class="typing-dots">
        <span class="dot"></span>
        <span class="dot"></span>
        <span class="dot"></span>
      </div>
      <span class="typing-text">{{ displayText }}</span>
    </div>
  </Transition>
</template>

<style scoped>
.typing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.typing-dots {
  display: flex;
  gap: 3px;
}

.dot {
  width: 5px;
  height: 5px;
  background: var(--el-text-color-secondary);
  border-radius: 50%;
  animation: bounce 1.4s ease-in-out infinite;
}

.dot:nth-child(1) {
  animation-delay: 0s;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.typing-text {
  white-space: nowrap;
}

/* Fade transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
