<script setup lang="ts">
import { computed } from 'vue'
import type { User } from '@/types'

const props = defineProps<{
  // The text content to parse for mentions
  content: string
  // Array of user IDs that were mentioned
  atUserIds?: number[]
  // Current user ID to highlight @me mentions differently
  currentUserId?: number
  // Optional member lookup map for group messages
  members?: Map<number, User>
}>()

const emit = defineEmits<{
  (e: 'mention-click', userId: number): void
}>()

// Parse content and identify mention patterns
// Mentions are in the format @nickname or @all
const parsedContent = computed(() => {
  if (!props.content) return []

  // If no mentions, just return plain text
  if (!props.atUserIds || props.atUserIds.length === 0) {
    return [{ type: 'text' as const, content: props.content }]
  }

  // Match @mentions pattern - @word characters (letters, numbers, underscore, chinese, etc.)
  const mentionPattern = /@([\w\u4e00-\u9fa5]+)/g
  const parts: Array<
    | { type: 'text'; content: string }
    | { type: 'mention'; userId: number | 'all'; displayName: string; isMe: boolean }
  > = []

  let lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = mentionPattern.exec(props.content)) !== null) {
    // Add text before the mention
    if (match.index > lastIndex) {
      parts.push({
        type: 'text',
        content: props.content.slice(lastIndex, match.index),
      })
    }

    const mentionName = match[1]

    // Check if this is @all
    if (mentionName.toLowerCase() === 'all' && props.atUserIds.includes(-1)) {
      parts.push({
        type: 'mention',
        userId: 'all',
        displayName: '@All',
        isMe: true, // @all always highlights for current user
      })
    } else {
      // Try to find the user in members
      const user = findUserByNickname(mentionName)
      if (user && props.atUserIds.includes(user.id)) {
        parts.push({
          type: 'mention',
          userId: user.id,
          displayName: `@${user.nickname}`,
          isMe: user.id === props.currentUserId,
        })
      } else {
        // Not a valid mention, treat as text
        parts.push({
          type: 'text',
          content: match[0],
        })
      }
    }

    lastIndex = match.index + match[0].length
  }

  // Add remaining text
  if (lastIndex < props.content.length) {
    parts.push({
      type: 'text',
      content: props.content.slice(lastIndex),
    })
  }

  return parts
})

function findUserByNickname(nickname: string): User | undefined {
  if (!props.members) return undefined

  for (const [, user] of props.members) {
    if (user.nickname.toLowerCase() === nickname.toLowerCase()) {
      return user
    }
  }
  return undefined
}

function handleMentionClick(userId: number | 'all') {
  if (userId !== 'all') {
    emit('mention-click', userId)
  }
}
</script>

<template>
  <span class="mention-text">
    <template v-for="(part, index) in parsedContent" :key="index">
      <span v-if="part.type === 'text'">{{ part.content }}</span>
      <span
        v-else
        class="mention"
        :class="{ 'mention-me': part.isMe }"
        @click="handleMentionClick(part.userId)"
      >
        {{ part.displayName }}
      </span>
    </template>
  </span>
</template>

<style scoped>
.mention-text {
  word-break: break-word;
}

.mention {
  color: var(--el-color-primary);
  cursor: pointer;
  border-radius: 2px;
  padding: 0 2px;
}

.mention:hover {
  background-color: var(--el-color-primary-light-9);
}

.mention-me {
  color: var(--el-color-warning);
  font-weight: 500;
}

.mention-me:hover {
  background-color: var(--el-color-warning-light-9);
}
</style>
