<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useQuickReplyStore } from '@/stores/quickReply'

const emit = defineEmits<{
  (e: 'select', content: string): void
}>()

const quickReplyStore = useQuickReplyStore()

const quickReplies = computed(() => quickReplyStore.quickReplies)
const hasReplies = computed(() => quickReplies.value.length > 0)

onMounted(() => {
  if (!quickReplyStore.loaded) {
    quickReplyStore.fetchQuickReplies()
  }
})

function handleSelect(content: string) {
  emit('select', content)
}
</script>

<template>
  <div v-if="hasReplies" class="quick-reply-panel">
    <div class="quick-reply-list">
      <el-tag
        v-for="reply in quickReplies"
        :key="reply.id"
        class="quick-reply-tag"
        type="info"
        effect="plain"
        @click="handleSelect(reply.content)"
      >
        {{ reply.content }}
      </el-tag>
    </div>
  </div>
</template>

<style scoped>
.quick-reply-panel {
  padding: 8px 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}

.quick-reply-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-reply-tag {
  cursor: pointer;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-reply-tag:hover {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}
</style>
