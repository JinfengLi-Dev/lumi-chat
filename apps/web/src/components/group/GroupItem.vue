<script setup lang="ts">
import { Medal } from '@element-plus/icons-vue'
import type { GroupDetail } from '@/api/group'

const props = defineProps<{
  group: GroupDetail
  currentUserId: number
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

function getRoleBadge(): { icon: typeof Medal; color: string; label: string } | null {
  if (props.group.ownerId === props.currentUserId) {
    return { icon: Medal, color: '#e6a23c', label: 'Owner' }
  }
  // Note: Admin detection would require member data with role
  // For now, we only show owner badge
  return null
}
</script>

<template>
  <div class="group-item" @click="emit('click')">
    <el-avatar :src="group.avatar" :size="45" shape="square">
      {{ group.name.charAt(0) }}
    </el-avatar>

    <div class="group-info">
      <div class="group-name">
        {{ group.name }}
        <span v-if="getRoleBadge()" class="role-badge" :style="{ color: getRoleBadge()!.color }">
          <el-icon :size="12"><component :is="getRoleBadge()!.icon" /></el-icon>
        </span>
      </div>
      <div class="group-meta">
        {{ group.memberCount || 0 }} members
      </div>
    </div>
  </div>
</template>

<style scoped>
.group-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  gap: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.group-item:hover {
  background-color: var(--el-fill-color-light);
}

.group-info {
  flex: 1;
  min-width: 0;
}

.group-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-badge {
  display: inline-flex;
  align-items: center;
}

.group-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
