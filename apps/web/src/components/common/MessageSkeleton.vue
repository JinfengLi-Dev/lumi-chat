<script setup lang="ts">
defineProps<{
  count?: number
}>()

// Generate a pattern to make skeletons look more natural
const patterns = [
  { type: 'other', lines: 2 },
  { type: 'self', lines: 1 },
  { type: 'other', lines: 3 },
  { type: 'self', lines: 2 },
  { type: 'other', lines: 1 },
]
</script>

<template>
  <div class="message-skeleton-list">
    <div
      v-for="i in (count ?? 5)"
      :key="i"
      class="skeleton-message"
      :class="patterns[(i - 1) % patterns.length]?.type"
    >
      <div class="skeleton-avatar skeleton"></div>
      <div class="skeleton-bubble skeleton">
        <div
          v-for="j in (patterns[(i - 1) % patterns.length]?.lines ?? 1)"
          :key="j"
          class="skeleton-line"
          :style="{ width: j === (patterns[(i - 1) % patterns.length]?.lines ?? 1) ? '60%' : '100%' }"
        ></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.message-skeleton-list {
  padding: 20px;
}
</style>
