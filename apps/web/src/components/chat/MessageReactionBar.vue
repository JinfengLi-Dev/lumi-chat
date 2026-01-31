<script setup lang="ts">
import { computed, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { MessageReaction } from '@/types'
import { apiClient } from '@/api/client'

const props = defineProps<{
  messageId: number
  reactions: MessageReaction[]
}>()

const emit = defineEmits<{
  (e: 'reaction-added', messageId: number, emoji: string): void
  (e: 'reaction-removed', messageId: number, emoji: string): void
}>()

const userStore = useUserStore()
const showEmojiPicker = ref(false)

// Common emojis for quick access
const quickEmojis = ['👍', '❤️', '😂', '😮', '😢', '🙏', '🎉', '🔥']

// Group reactions by emoji
const groupedReactions = computed(() => {
  const groups = new Map<string, MessageReaction>()
  const currentUserId = userStore.userId

  for (const reaction of props.reactions) {
    const existing = groups.get(reaction.emoji)
    if (existing && reaction.userId !== undefined) {
      existing.count++
      existing.userIds.push(reaction.userId)
      if (currentUserId && reaction.userId === currentUserId) {
        existing.currentUserReacted = true
      }
    } else if (reaction.userId !== undefined) {
      groups.set(reaction.emoji, {
        emoji: reaction.emoji,
        count: 1,
        userIds: [reaction.userId],
        currentUserReacted: currentUserId !== undefined && reaction.userId === currentUserId
      })
    }
  }

  return Array.from(groups.values())
})

async function handleReactionClick(emoji: string, currentUserReacted: boolean) {
  if (!userStore.userId) return

  try {
    if (currentUserReacted) {
      // Remove reaction
      await apiClient.delete(`/messages/${props.messageId}/reactions/${encodeURIComponent(emoji)}`)
      emit('reaction-removed', props.messageId, emoji)
    } else {
      // Add reaction
      await apiClient.post(`/messages/${props.messageId}/reactions`, { emoji })
      emit('reaction-added', props.messageId, emoji)
    }
  } catch (error) {
    console.error('Failed to toggle reaction:', error)
    ElMessage.error('Failed to update reaction')
  }
}

async function handleEmojiSelect(emoji: string) {
  showEmojiPicker.value = false

  // Check if user already reacted with this emoji
  const existing = groupedReactions.value.find(r => r.emoji === emoji)
  if (existing?.currentUserReacted) {
    return // Don't add duplicate reaction
  }

  try {
    await apiClient.post(`/messages/${props.messageId}/reactions`, { emoji })
    emit('reaction-added', props.messageId, emoji)
  } catch (error) {
    console.error('Failed to add reaction:', error)
    ElMessage.error('Failed to add reaction')
  }
}
</script>

<template>
  <div v-if="groupedReactions.length > 0 || showEmojiPicker" class="message-reaction-bar">
    <div class="reactions">
      <div
        v-for="reaction in groupedReactions"
        :key="reaction.emoji"
        class="reaction-item"
        :class="{ 'user-reacted': reaction.currentUserReacted }"
        @click="handleReactionClick(reaction.emoji, reaction.currentUserReacted)"
      >
        <span class="emoji">{{ reaction.emoji }}</span>
        <span class="count">{{ reaction.count }}</span>
      </div>

      <el-popover
        v-model:visible="showEmojiPicker"
        placement="top-start"
        :width="280"
        trigger="click"
      >
        <template #reference>
          <div class="add-reaction-btn">
            <el-icon><Plus /></el-icon>
          </div>
        </template>
        <div class="emoji-picker">
          <div
            v-for="emoji in quickEmojis"
            :key="emoji"
            class="emoji-option"
            @click="handleEmojiSelect(emoji)"
          >
            {{ emoji }}
          </div>
        </div>
      </el-popover>
    </div>
  </div>
</template>

<style scoped>
.message-reaction-bar {
  margin-top: 4px;
  margin-bottom: 4px;
}

.reactions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.reaction-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 12px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.reaction-item:hover {
  background: var(--el-fill-color);
  border-color: var(--el-border-color);
}

.reaction-item.user-reacted {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

.reaction-item.user-reacted:hover {
  background: var(--el-color-primary-light-8);
  border-color: var(--el-color-primary-light-3);
}

.emoji {
  font-size: 14px;
  line-height: 1;
}

.count {
  font-size: 11px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.reaction-item.user-reacted .count {
  color: var(--el-color-primary);
}

.add-reaction-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 12px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.add-reaction-btn:hover {
  background: var(--el-fill-color);
  border-color: var(--el-border-color);
  color: var(--el-text-color-primary);
}

.emoji-picker {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 8px;
}

.emoji-option {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  font-size: 24px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.emoji-option:hover {
  background: var(--el-fill-color-light);
}
</style>
