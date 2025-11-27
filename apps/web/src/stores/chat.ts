import { defineStore } from 'pinia'
import type { Conversation, Message, MessageType, MessageMetadata } from '@/types'
import { conversationApi } from '@/api/conversation'
import { messageApi } from '@/api/message'

interface ChatState {
  conversations: Conversation[]
  currentConversationId: number | null
  messages: Map<number, Message[]>
  loading: boolean
  loadingMessages: boolean
  hasMoreMessages: Map<number, boolean>
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    conversations: [],
    currentConversationId: null,
    messages: new Map(),
    loading: false,
    loadingMessages: false,
    hasMoreMessages: new Map(),
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
        const response = await messageApi.getMessages(conversationId, beforeMsgId)
        const existing = this.messages.get(conversationId) || []

        if (beforeMsgId) {
          this.messages.set(conversationId, [...response.items, ...existing])
        } else {
          this.messages.set(conversationId, response.items)
        }

        this.hasMoreMessages.set(conversationId, response.hasMore)
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

      try {
        const sent = await messageApi.sendMessage({
          conversationId,
          msgType,
          content,
          metadata,
          quoteMsgId,
          atUserIds,
        })

        const updated = this.messages.get(conversationId) || []
        const index = updated.findIndex((m: Message) => m.msgId === tempId)
        if (index !== -1) {
          updated[index] = { ...sent, status: 'sent' }
          this.messages.set(conversationId, [...updated])
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
      await messageApi.forwardMessage(msgId, targetConversationIds)
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
      if (conv) {
        conv.lastMessage = message
        conv.lastMsgTime = message.serverCreatedAt

        if (message.conversationId !== this.currentConversationId) {
          conv.unreadCount++
        }
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

    handleReadStatusSync(conversationId: number, _lastReadMsgId: number) {
      const conv = this.conversations.find((c: Conversation) => c.id === conversationId)
      if (conv) {
        conv.unreadCount = 0
        conv.atMsgIds = []
      }
    },

    handleTypingIndicator(_conversationId: number, _userId: number) {
      // Handle typing indicator
    },

    clearMessages(conversationId: number) {
      this.messages.delete(conversationId)
    },

    reset() {
      this.conversations = []
      this.currentConversationId = null
      this.messages.clear()
      this.hasMoreMessages.clear()
    },
  },
})
