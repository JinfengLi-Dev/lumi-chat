<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search, UserFilled, Close } from '@element-plus/icons-vue'
import type { Group } from '@/types'

const props = defineProps<{
  visible: boolean
  groups: Group[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'select', group: Group): void
}>()

const searchQuery = ref('')

const filteredGroups = computed(() => {
  if (!searchQuery.value.trim()) {
    return props.groups
  }

  const query = searchQuery.value.toLowerCase()
  return props.groups.filter(
    (group) =>
      group.name.toLowerCase().includes(query) ||
      group.gid.toLowerCase().includes(query)
  )
})

function selectGroup(group: Group) {
  emit('select', group)
  close()
}

function close() {
  emit('update:visible', false)
  searchQuery.value = ''
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="group-card-picker-overlay" @click.self="close">
      <div class="group-card-picker">
        <div class="picker-header">
          <span class="title">Send Group Card</span>
          <el-button text @click="close">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <!-- Search bar -->
        <div class="search-bar">
          <el-input
            v-model="searchQuery"
            placeholder="Search groups..."
            :prefix-icon="Search"
            clearable
          />
        </div>

        <!-- Groups list -->
        <div class="groups-list">
          <div v-if="filteredGroups.length === 0" class="empty-state">
            <el-icon :size="48" color="#c0c4cc"><UserFilled /></el-icon>
            <p v-if="groups.length === 0">No groups yet</p>
            <p v-else>No matching groups found</p>
          </div>

          <div
            v-for="group in filteredGroups"
            :key="group.id"
            class="group-item"
            @click="selectGroup(group)"
          >
            <el-avatar :src="group.avatar" :size="40" shape="square" class="group-avatar">
              {{ group.name.charAt(0) }}
            </el-avatar>

            <div class="group-info">
              <div class="group-name">{{ group.name }}</div>
              <div class="group-meta">
                <span class="group-gid">ID: {{ group.gid }}</span>
                <span class="member-count">
                  <el-icon :size="10"><UserFilled /></el-icon>
                  {{ group.memberCount || 0 }}
                </span>
              </div>
            </div>

            <el-icon class="select-icon"><el-icon-arrow-right /></el-icon>
          </div>
        </div>

        <!-- Actions -->
        <div class="picker-actions">
          <el-button @click="close">Cancel</el-button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.group-card-picker-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.group-card-picker {
  width: 400px;
  max-height: 80vh;
  background: var(--el-bg-color);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.search-bar {
  padding: 16px 20px;
}

.groups-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
  min-height: 200px;
  max-height: 400px;
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

.group-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.group-item:hover {
  background: var(--el-fill-color-light);
}

.group-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--el-color-primary-light-3), var(--el-color-primary));
  color: white;
}

.group-info {
  flex: 1;
  min-width: 0;
}

.group-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.group-gid {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.member-count {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: var(--el-text-color-regular);
}

.select-icon {
  color: var(--el-text-color-placeholder);
}

.picker-actions {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* Scrollbar */
.groups-list::-webkit-scrollbar {
  width: 6px;
}

.groups-list::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color);
  border-radius: 3px;
}

.groups-list::-webkit-scrollbar-track {
  background-color: transparent;
}
</style>
