<script setup lang="ts">
defineProps<{
  count?: number
}>()
</script>

<template>
  <div class="conversation-skeleton-list">
    <div
      v-for="i in (count ?? 5)"
      :key="i"
      class="skeleton-conversation"
    >
      <div class="skeleton-avatar skeleton"></div>
      <div class="skeleton-content">
        <div class="skeleton-line skeleton"></div>
        <div class="skeleton-line skeleton"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.conversation-skeleton-list {
  padding: 0;
}
</style>
