<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { groupApi } from '@/api/group'
import { conversationApi } from '@/api/conversation'
import GroupItem from './GroupItem.vue'
import type { GroupDetail } from '@/api/group'

defineProps<{
  currentUserId: number
}>()

const emit = defineEmits<{
  (e: 'open-conversation', conversationId: number): void
}>()

const groups = ref<GroupDetail[]>([])
const isLoading = ref(false)
const searchQuery = ref('')

const filteredGroups = computed(() => {
  if (!searchQuery.value) return groups.value

  const query = searchQuery.value.toLowerCase()
  return groups.value.filter((group) => {
    return group.name.toLowerCase().includes(query)
  })
})

async function loadGroups() {
  isLoading.value = true
  try {
    groups.value = await groupApi.getGroups()
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to load groups'
    ElMessage.error(message)
  } finally {
    isLoading.value = false
  }
}

async function handleGroupClick(group: GroupDetail) {
  try {
    // Get conversations and find the group conversation
    const conversations = await conversationApi.getConversations()
    const groupConversation = conversations.find(
      (conv) => conv.type === 'group' && conv.groupId === group.id
    )

    if (groupConversation) {
      emit('open-conversation', groupConversation.id)
    } else {
      ElMessage.warning('Group conversation not found')
    }
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to open conversation'
    ElMessage.error(message)
  }
}

// Expose methods for parent to refresh
defineExpose({
  refresh: loadGroups,
})

onMounted(() => {
  loadGroups()
})
</script>

<template>
  <div class="groups-list" v-loading="isLoading">
    <!-- Search -->
    <div class="search-container">
      <el-input
        v-model="searchQuery"
        placeholder="Search groups"
        :prefix-icon="Search"
        clearable
        class="search-input"
      />
    </div>

    <!-- Group List -->
    <div class="groups-content">
      <GroupItem
        v-for="group in filteredGroups"
        :key="group.id"
        :group="group"
        :current-user-id="currentUserId"
        @click="handleGroupClick(group)"
      />

      <div v-if="filteredGroups.length === 0 && !isLoading" class="empty-state">
        <el-empty
          :description="searchQuery ? 'No groups found' : 'No groups yet'"
          :image-size="80"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.groups-list {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-container {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.search-input {
  width: 100%;
}

.groups-content {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}
</style>
