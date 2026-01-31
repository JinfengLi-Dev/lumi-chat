import { defineStore } from 'pinia'
import type { Conversation, Message, MessageType, MessageMetadata } from '@/types'
import { conversationApi } from '@/api/conversation'
import { messageApi } from '@/api/message'
import { useUserStore } from './user'

interface TypingUser {
  userId: number
  nickname: string
  avatar?: string
  timeoutId: ReturnType<typeof setTimeout>
}

interface ChatState {
  conversations: Conversation[]
  currentConversationId: number | null
  messages: Map<number, Message[]>
  loading: boolean
  loadingMessages: boolean
  hasMoreMessages: Map<number, boolean>
  typingUsers: Map<number, Map<number, TypingUser>> // conversationId -> userId -> TypingUser
  lastReadByOther: Map<number, number> // conversationId -> lastReadMsgId (by the other user in private chats)
}

// Typing indicator timeout in milliseconds
const TYPING_TIMEOUT_MS = 3000

// Message send timeout in milliseconds (30 seconds)
const SEND_TIMEOUT_MS = 30000

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    conversations: [],
    currentConversationId: null,
    messages: new Map(),
    loading: false,
    loadingMessages: false,
    hasMoreMessages: new Map(),
    typingUsers: new Map(),
    lastReadByOther: new Map(),
  }),

  getters: {
    currentConversation: (state): Conversation | null | undefined => {
      if (!state.currentConversationId) return null
      return state.conversations.find((c: Conversation) => c.id === state.currentConversationId)
    },

    currentMessages: (state): Message[] => {
      if (!state.currentConversationId) return []
      return state.messages.get(state.currentConversationId) || []
    },

    sortedConversations: (state): Conversation[] => {
      return [...state.conversations].sort((a: Conversation, b: Conversation) => {
        if (a.isPinned && !b.isPinned) return -1
        if (!a.isPinned && b.isPinned) return 1
        const timeA = a.lastMsgTime ? new Date(a.lastMsgTime).getTime() : 0
        const timeB = b.lastMsgTime ? new Date(b.lastMsgTime).getTime() : 0
        return timeB - timeA
      })
    },

    totalUnreadCount: (state): number => {
      return state.conversations.reduce((sum: number, c: Conversation) => sum + c.unreadCount, 0)
    },

    hasAtMe: (state): boolean => {
      return state.conversations.some((c: Conversation) => c.atMsgIds && c.atMsgIds.length > 0)
    },

    currentTypingUsers: (state): { userId: number; nickname: string; avatar?: string }[] => {
      if (!state.currentConversationId) return []
      const conversationTyping = state.typingUsers.get(state.currentConversationId)
      if (!conversationTyping) return []
      return Array.from(conversationTyping.values()).map((t) => ({
        userId: t.userId,
        nickname: t.nickname,
        avatar: t.avatar,
      }))
    },

    getTypingUsersForConversation:
      (state) =>
      (conversationId: number): { userId: number; nickname: string; avatar?: string }[] => {
        const conversationTyping = state.typingUsers.get(conversationId)
        if (!conversationTyping) return []
        return Array.from(conversationTyping.values()).map((t) => ({
          userId: t.userId,
          nickname: t.nickname,
          avatar: t.avatar,
        }))
      },

    // Check if a message has been read by the other user (for private chats)
    isMessageReadByOther:
      (state) =>
      (conversationId: number, messageId: number): boolean => {
        const lastReadMsgId = state.lastReadByOther.get(conversationId)
        return lastReadMsgId !== undefined && messageId <= lastReadMsgId
      },

    // Get the last read message ID by the other user for a conversation
    getLastReadByOther:
      (state) =>
      (conversationId: number): number | undefined => {
        return state.lastReadByOther.get(conversationId)
      },
  },

  actions: {
    async fetchConversations() {
      this.loading = true
      try {
        this.conversations = await conversationApi.getConversations()
      } finally {
        this.loading = false
      }
    },

    async fetchMessages(conversationId: number, beforeMsgId?: number) {
      this.loadingMessages = true
      try {
        const messages = await messageApi.getMessages(conversationId, beforeMsgId)
        // Backend returns messages in DESC order (newest first), reverse for chronological display
        const chronological = [...messages].reverse()
        const existing = this.messages.get(conversationId) || []

        if (beforeMsgId) {
          // Older messages go before existing (prepend)
          this.messages.set(conversationId, [...chronological, ...existing])
        } else {
          this.messages.set(conversationId, chronological)
        }

        // API returns array directly; hasMore = true if we got full limit
        this.hasMoreMessages.set(conversationId, messages.length >= 50)
      } finally {
        this.loadingMessages = false
      }
    },

    async sendMessage(
      conversationId: number,
      msgType: MessageType,
      content: string,
      metadata?: MessageMetadata,
      quoteMsgId?: string,
      atUserIds?: number[]
    ) {
      const tempId = `temp_${Date.now()}`
      const tempMessage: Message = {
        id: 0,
        msgId: tempId,
        conversationId,
        senderId: 0,
        msgType,
        content,
        metadata,
        quoteMsgId,
        atUserIds,
        serverCreatedAt: new Date().toISOString(),
        status: 'sending',
      }

      const messages = this.messages.get(conversationId) || []
      this.messages.set(conversationId, [...messages, tempMessage])

      // Create timeout promise that rejects after SEND_TIMEOUT_MS
      const timeoutPromise = new Promise<never>((_, reject) => {
        setTimeout(() => reject(new Error('Send timeout')), SEND_TIMEOUT_MS)
      })

      try {
        // Race between actual send and timeout
        const sent = await Promise.race([
          messageApi.sendMessage({
            conversationId,
            msgType,
            content,
            metadata,
            quoteMsgId,
            atUserIds,
          }),
          timeoutPromise,
        ])

        const updated = this.messages.get(conversationId) || []
        const index = updated.findIndex((m: Message) => m.msgId === tempId)
        if (index !== -1) {
          updated[index] = { ...sent, status: 'sent' }
          this.messages.set(conversationId, [...updated])
        }

        // Update conversation's lastMessage so sender's conversation list shows the sent message
        const conv = this.conversations.find((c: Conversation) => c.id === conversationId)
        if (conv) {
          conv.lastMessage = sent
          conv.lastMsgTime = sent.serverCreatedAt
        }

        return sent
      } catch (error) {
        const updated = this.messages.get(conversationId) || []
        const index = updated.findIndex((m: Message) => m.msgId === tempId)
        if (index !== -1 && updated[index]) {
          const failedMsg = updated[index]!
          updated[index] = {
            id: failedMsg.id,
            msgId: failedMsg.msgId,
            conversationId: failedMsg.conversationId,
            senderId: failedMsg.senderId,
            msgType: failedMsg.msgType,
            content: failedMsg.content,
            serverCreatedAt: failedMsg.serverCreatedAt,
            metadata: failedMsg.metadata,
            quoteMsgId: failedMsg.quoteMsgId,
            atUserIds: failedMsg.atUserIds,
            status: 'failed',
          }
          this.messages.set(conversationId, [...updated])
        }
        throw error
      }
    },

    async recallMessage(msgId: string) {
      await messageApi.recallMessage(msgId)
      this.handleMessageRecalled(msgId)
    },

    async forwardMessage(msgId: string, targetConversationIds: number[]) {
      // Forward to each target conversation sequentially
      for (const targetConversationId of targetConversationIds) {
        await messageApi.forwardMessage(msgId, targetConversationId)
      }
    },

    async deleteMessage(msgId: string) {
      await messageApi.deleteMessage(msgId)
      for (const [convId, messages] of this.messages.entries()) {
        const filtered = messages.filter((m: Message) => m.msgId !== msgId)
        if (filtered.length !== messages.length) {
          this.messages.set(convId, filtered)
          break
        }
      }
    },

    async markAsRead(conversationId: number) {
      await conversationApi.markAsRead(conversationId)
      const conv = this.conversations.find((c: Conversation) => c.id === conversationId)
      if (conv) {
        conv.unreadCount = 0
        conv.atMsgIds = []
      }
    },

    setCurrentConversation(conversationId: number | null) {
      this.currentConversationId = conversationId
      if (conversationId) {
        this.markAsRead(conversationId)
      }
    },

    handleNewMessage(message: Message) {
      const messages = this.messages.get(message.conversationId) || []

      if (messages.some((m: Message) => m.msgId === message.msgId)) return

      this.messages.set(message.conversationId, [...messages, message])

      const conv = this.conversations.find((c: Conversation) => c.id === message.conversationId)

      // If conversation doesn't exist in store, refetch to get it with correct unreadCount
      if (!conv) {
        this.fetchConversations()
        return
      }

      conv.lastMessage = message
      conv.lastMsgTime = message.serverCreatedAt

      if (message.conversationId !== this.currentConversationId) {
        conv.unreadCount++
      }
    },

    handleMessageRecalled(msgId: string) {
      for (const [convId, messages] of this.messages.entries()) {
        const index = messages.findIndex((m: Message) => m.msgId === msgId)
        if (index !== -1 && messages[index]) {
          const recalledMsg = messages[index]!
          messages[index] = {
            id: recalledMsg.id,
            msgId: recalledMsg.msgId,
            conversationId: recalledMsg.conversationId,
            senderId: recalledMsg.senderId,
            sender: recalledMsg.sender,
            msgType: 'recall',
            content: '',
            serverCreatedAt: recalledMsg.serverCreatedAt,
            recalledAt: new Date().toISOString(),
          }
          this.messages.set(convId, [...messages])
          break
        }
      }
    },

    handleReadStatusSync(conversationId: number, lastReadMsgId: number) {
      // This is called when read status is synced from another device of the same user
      // So we should update the local unread count for this conversation
      const conv = this.conversations.find((c: Conversation) => c.id === conversationId)
      if (conv) {
        conv.unreadCount = 0
        conv.atMsgIds = []
      }

      // If the conversation is currently open, no additional action needed
      // The unread count has been synced

      console.log('[Chat] Read status synced from other device:', { conversationId, lastReadMsgId })
    },

    // Handle batch of offline messages received after reconnect
    handleOfflineMessages(messages: { id: number; conversationId: number; messageId: number; message?: Message }[]) {
      console.log('[Chat] Processing', messages.length, 'offline messages')

      // Group messages by conversation
      const messagesByConversation = new Map<number, Message[]>()

      for (const offlineMsg of messages) {
        if (offlineMsg.message) {
          const convId = offlineMsg.conversationId
          if (!messagesByConversation.has(convId)) {
            messagesByConversation.set(convId, [])
          }
          messagesByConversation.get(convId)!.push(offlineMsg.message)
        }
      }

      // Add messages to each conversation
      for (const [conversationId, newMessages] of messagesByConversation.entries()) {
        const existing = this.messages.get(conversationId) || []

        // Filter out duplicates
        const uniqueMessages = newMessages.filter(
          (newMsg) => !existing.some((existingMsg) => existingMsg.msgId === newMsg.msgId)
        )

        if (uniqueMessages.length > 0) {
          // Sort by serverCreatedAt
          const allMessages = [...existing, ...uniqueMessages].sort((a, b) =>
            new Date(a.serverCreatedAt).getTime() - new Date(b.serverCreatedAt).getTime()
          )
          this.messages.set(conversationId, allMessages)
        }
      }

      // Refresh conversation list to update unread counts
      this.fetchConversations()
    },

    // Handle read receipt notification from the other user in a private chat
    handleReadReceiptNotify(conversationId: number, _readerId: number, lastReadMsgId: number) {
      console.log('[Chat] handleReadReceiptNotify:', {
        conversationId,
        lastReadMsgId,
        currentMap: Array.from(this.lastReadByOther.entries()),
      })
      // Update the last read message ID for this conversation
      const currentLastRead = this.lastReadByOther.get(conversationId) || 0
      if (lastReadMsgId > currentLastRead) {
        // Create a new Map to trigger Vue reactivity
        const newMap = new Map(this.lastReadByOther)
        newMap.set(conversationId, lastReadMsgId)
        this.lastReadByOther = newMap
        console.log('[Chat] Updated lastReadByOther:', Array.from(this.lastReadByOther.entries()))
      } else {
        console.log('[Chat] lastReadMsgId not greater than current, skipping update')
      }
    },

    handleTypingIndicator(conversationId: number, userId: number) {
      // Get or create the typing users map for this conversation
      if (!this.typingUsers.has(conversationId)) {
        this.typingUsers.set(conversationId, new Map())
      }
      const conversationTyping = this.typingUsers.get(conversationId)!

      // Clear existing timeout if user is already typing
      const existing = conversationTyping.get(userId)
      if (existing) {
        clearTimeout(existing.timeoutId)
      }

      // Find user info from conversation target or messages
      let nickname = `User ${userId}`
      let avatar: string | undefined

      // Check if it's a private chat and the target user is typing
      const conv = this.conversations.find((c: Conversation) => c.id === conversationId)
      if (conv?.targetUser?.id === userId) {
        nickname = conv.targetUser.nickname
        avatar = conv.targetUser.avatar
      }

      // Check messages for sender info (most reliable source)
      const messages = this.messages.get(conversationId) || []
      const msgWithSender = messages.find((m: Message) => m.senderId === userId && m.sender)
      if (msgWithSender?.sender) {
        nickname = msgWithSender.sender.nickname
        avatar = msgWithSender.sender.avatar
      }

      // Set timeout to remove typing indicator
      const timeoutId = setTimeout(() => {
        this.removeTypingUser(conversationId, userId)
      }, TYPING_TIMEOUT_MS)

      // Add or update typing user
      conversationTyping.set(userId, {
        userId,
        nickname,
        avatar,
        timeoutId,
      })
    },

    removeTypingUser(conversationId: number, userId: number) {
      const conversationTyping = this.typingUsers.get(conversationId)
      if (conversationTyping) {
        const existing = conversationTyping.get(userId)
        if (existing) {
          clearTimeout(existing.timeoutId)
          conversationTyping.delete(userId)
          if (conversationTyping.size === 0) {
            this.typingUsers.delete(conversationId)
          }
        }
      }
    },

    clearTypingUsers(conversationId: number) {
      const conversationTyping = this.typingUsers.get(conversationId)
      if (conversationTyping) {
        for (const user of conversationTyping.values()) {
          clearTimeout(user.timeoutId)
        }
        this.typingUsers.delete(conversationId)
      }
    },

    clearMessages(conversationId: number) {
      this.messages.delete(conversationId)
    },

    async clearChatHistory(conversationId: number) {
      await conversationApi.clearMessages(conversationId)
      this.messages.set(conversationId, [])
      // Update last message in conversation list
      const conv = this.conversations.find((c: Conversation) => c.id === conversationId)
      if (conv) {
        conv.lastMessage = undefined
      }
    },

    async deleteConversation(conversationId: number) {
      await conversationApi.deleteConversation(conversationId)
      // Remove from local state
      this.conversations = this.conversations.filter((c: Conversation) => c.id !== conversationId)
      this.messages.delete(conversationId)
      this.hasMoreMessages.delete(conversationId)
      // Clear current if it was the deleted one
      if (this.currentConversationId === conversationId) {
        this.currentConversationId = null
      }
    },

    // Handle reaction added event from WebSocket
    handleReactionAdded(conversationId: number, messageId: number, userId: number, emoji: string) {
      const messages = this.messages.get(conversationId)
      if (!messages) return

      const message = messages.find((m) => m.id === messageId)
      if (!message) return

      if (!message.reactions) {
        message.reactions = []
      }

      // Find existing aggregated reaction for this emoji
      const existingReaction = message.reactions.find((r) => r.emoji === emoji)

      if (existingReaction) {
        // Update existing aggregated reaction
        if (!existingReaction.userIds.includes(userId)) {
          existingReaction.count++
          existingReaction.userIds.push(userId)
          // Update currentUserReacted if this is the current user
          if (userId === useUserStore().userId) {
            existingReaction.currentUserReacted = true
          }
        }
      } else {
        // Create new aggregated reaction
        message.reactions.push({
          emoji,
          count: 1,
          userIds: [userId],
          currentUserReacted: userId === useUserStore().userId,
        })
      }
    },

    // Handle reaction removed event from WebSocket
    handleReactionRemoved(conversationId: number, messageId: number, userId: number, emoji: string) {
      const messages = this.messages.get(conversationId)
      if (!messages) return

      const message = messages.find((m) => m.id === messageId)
      if (!message || !message.reactions) return

      // Find aggregated reaction for this emoji
      const reaction = message.reactions.find((r) => r.emoji === emoji)
      if (!reaction) return

      // Remove userId from the aggregated reaction
      const userIdIndex = reaction.userIds.indexOf(userId)
      if (userIdIndex !== -1) {
        reaction.userIds.splice(userIdIndex, 1)
        reaction.count--

        // Update currentUserReacted if this is the current user
        if (userId === useUserStore().userId) {
          reaction.currentUserReacted = false
        }

        // Remove the reaction entirely if count reaches 0
        if (reaction.count === 0) {
          const reactionIndex = message.reactions.indexOf(reaction)
          if (reactionIndex !== -1) {
            message.reactions.splice(reactionIndex, 1)
          }
        }
      }
    },

    reset() {
      // Clear all typing timeouts
      for (const conversationTyping of this.typingUsers.values()) {
        for (const user of conversationTyping.values()) {
          clearTimeout(user.timeoutId)
        }
      }
      this.conversations = []
      this.currentConversationId = null
      this.messages.clear()
      this.hasMoreMessages.clear()
      this.typingUsers.clear()
      this.lastReadByOther.clear()
    },
  },
})
