<script setup lang="ts">
import type { Friend } from '@/types'

defineProps<{
  friend: Friend
}>()

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'context-menu', event: MouseEvent): void
}>()

function getDisplayName(friend: Friend): string {
  return friend.remark || friend.nickname || 'Unknown'
}

function handleContextMenu(event: MouseEvent) {
  event.preventDefault()
  emit('context-menu', event)
}
</script>

<template>
  <div
    class="friend-item"
    @click="emit('click')"
    @contextmenu="handleContextMenu"
  >
    <el-avatar :src="friend.avatar" :size="45" shape="circle">
      {{ getDisplayName(friend).charAt(0) }}
    </el-avatar>

    <div class="friend-info">
      <div class="friend-name">{{ getDisplayName(friend) }}</div>
      <div v-if="friend.remark && friend.remark !== friend.nickname" class="friend-real-name">
        {{ friend.nickname }}
      </div>
      <div v-else-if="friend.signature" class="friend-signature">
        {{ friend.signature }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.friend-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  gap: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.friend-item:hover {
  background-color: var(--el-fill-color-light);
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-real-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.friend-signature {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
