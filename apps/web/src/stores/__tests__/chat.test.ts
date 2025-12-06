import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useChatStore } from '@/stores/chat'
import { conversationApi } from '@/api/conversation'
import { messageApi } from '@/api/message'
import type { Conversation, Message } from '@/types'

// Mock the API modules
vi.mock('@/api/conversation', () => ({
  conversationApi: {
    getConversations: vi.fn(),
    markAsRead: vi.fn(),
  },
}))

vi.mock('@/api/message', () => ({
  messageApi: {
    getMessages: vi.fn(),
    sendMessage: vi.fn(),
    recallMessage: vi.fn(),
    forwardMessage: vi.fn(),
    deleteMessage: vi.fn(),
  },
}))

// Helper to create mock conversations
function createMockConversation(overrides: Partial<Conversation> = {}): Conversation {
  return {
    id: 1,
    type: 'private',
    unreadCount: 0,
    isPinned: false,
    isMuted: false,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

// Helper to create mock messages
function createMockMessage(overrides: Partial<Message> = {}): Message {
  return {
    id: 1,
    msgId: 'msg-1',
    conversationId: 1,
    senderId: 1,
    msgType: 'text',
    content: 'Hello',
    serverCreatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

describe('Chat Store', () => {
  let chatStore: ReturnType<typeof useChatStore>

  beforeEach(() => {
    vi.clearAllMocks()
    chatStore = useChatStore()
  })

  describe('Initial State', () => {
    it('should have empty conversations array', () => {
      expect(chatStore.conversations).toEqual([])
    })

    it('should have null currentConversationId', () => {
      expect(chatStore.currentConversationId).toBeNull()
    })

    it('should have empty messages Map', () => {
      expect(chatStore.messages.size).toBe(0)
    })

    it('should have loading as false', () => {
      expect(chatStore.loading).toBe(false)
    })
  })

  describe('fetchConversations', () => {
    it('should fetch and store conversations', async () => {
      const mockConversations = [
        createMockConversation({ id: 1 }),
        createMockConversation({ id: 2 }),
      ]
      vi.mocked(conversationApi.getConversations).mockResolvedValue(mockConversations)

      await chatStore.fetchConversations()

      expect(conversationApi.getConversations).toHaveBeenCalled()
      expect(chatStore.conversations).toEqual(mockConversations)
      expect(chatStore.loading).toBe(false)
    })

    it('should set loading to true during fetch', async () => {
      vi.mocked(conversationApi.getConversations).mockImplementation(async () => {
        expect(chatStore.loading).toBe(true)
        return []
      })

      await chatStore.fetchConversations()
    })

    it('should reset loading on error', async () => {
      vi.mocked(conversationApi.getConversations).mockRejectedValue(new Error('Failed'))

      await expect(chatStore.fetchConversations()).rejects.toThrow('Failed')
      expect(chatStore.loading).toBe(false)
    })
  })

  describe('fetchMessages', () => {
    it('should fetch and store messages for a conversation', async () => {
      const mockMessages = [
        createMockMessage({ msgId: 'msg-1' }),
        createMockMessage({ msgId: 'msg-2' }),
      ]
      vi.mocked(messageApi.getMessages).mockResolvedValue({
        items: mockMessages,
        hasMore: false,
      })

      await chatStore.fetchMessages(1)

      expect(messageApi.getMessages).toHaveBeenCalledWith(1, undefined)
      expect(chatStore.messages.get(1)).toEqual(mockMessages)
      expect(chatStore.hasMoreMessages.get(1)).toBe(false)
    })

    it('should prepend messages when loading older messages', async () => {
      const existingMessages = [createMockMessage({ msgId: 'msg-3' })]
      chatStore.messages.set(1, existingMessages)

      const olderMessages = [
        createMockMessage({ msgId: 'msg-1' }),
        createMockMessage({ msgId: 'msg-2' }),
      ]
      vi.mocked(messageApi.getMessages).mockResolvedValue({
        items: olderMessages,
        hasMore: true,
      })

      await chatStore.fetchMessages(1, 10)

      expect(messageApi.getMessages).toHaveBeenCalledWith(1, 10)
      const messages = chatStore.messages.get(1)
      expect(messages).toHaveLength(3)
      expect(messages![0].msgId).toBe('msg-1')
      expect(messages![2].msgId).toBe('msg-3')
      expect(chatStore.hasMoreMessages.get(1)).toBe(true)
    })
  })

  describe('sendMessage', () => {
    it('should add temp message and then update with sent message', async () => {
      const sentMessage = createMockMessage({ msgId: 'server-msg-1' })
      vi.mocked(messageApi.sendMessage).mockResolvedValue(sentMessage)

      const result = await chatStore.sendMessage(1, 'text', 'Hello')

      expect(result).toEqual(sentMessage)
      const messages = chatStore.messages.get(1)
      expect(messages).toHaveLength(1)
      expect(messages![0].status).toBe('sent')
    })

    it('should mark message as failed on error', async () => {
      vi.mocked(messageApi.sendMessage).mockRejectedValue(new Error('Send failed'))

      await expect(chatStore.sendMessage(1, 'text', 'Hello')).rejects.toThrow('Send failed')

      const messages = chatStore.messages.get(1)
      expect(messages).toHaveLength(1)
      expect(messages![0].status).toBe('failed')
    })

    it('should include optional parameters', async () => {
      const sentMessage = createMockMessage()
      vi.mocked(messageApi.sendMessage).mockResolvedValue(sentMessage)

      await chatStore.sendMessage(1, 'text', 'Hello', { fileName: 'test.txt' }, 'quote-1', [2, 3])

      expect(messageApi.sendMessage).toHaveBeenCalledWith({
        conversationId: 1,
        msgType: 'text',
        content: 'Hello',
        metadata: { fileName: 'test.txt' },
        quoteMsgId: 'quote-1',
        atUserIds: [2, 3],
      })
    })
  })

  describe('recallMessage', () => {
    it('should call API and update message as recalled', async () => {
      chatStore.messages.set(1, [createMockMessage({ msgId: 'msg-1' })])
      vi.mocked(messageApi.recallMessage).mockResolvedValue(undefined)

      await chatStore.recallMessage('msg-1')

      expect(messageApi.recallMessage).toHaveBeenCalledWith('msg-1')
      const messages = chatStore.messages.get(1)
      expect(messages![0].recalledAt).toBeDefined()
      expect(messages![0].msgType).toBe('recall')
    })
  })

  describe('forwardMessage', () => {
    it('should call forward API with correct parameters', async () => {
      vi.mocked(messageApi.forwardMessage).mockResolvedValue(undefined)

      await chatStore.forwardMessage('msg-1', [2, 3, 4])

      expect(messageApi.forwardMessage).toHaveBeenCalledWith('msg-1', [2, 3, 4])
    })
  })

  describe('deleteMessage', () => {
    it('should remove message from store', async () => {
      chatStore.messages.set(1, [
        createMockMessage({ msgId: 'msg-1' }),
        createMockMessage({ msgId: 'msg-2' }),
      ])
      vi.mocked(messageApi.deleteMessage).mockResolvedValue(undefined)

      await chatStore.deleteMessage('msg-1')

      expect(messageApi.deleteMessage).toHaveBeenCalledWith('msg-1')
      const messages = chatStore.messages.get(1)
      expect(messages).toHaveLength(1)
      expect(messages![0].msgId).toBe('msg-2')
    })
  })

  describe('markAsRead', () => {
    it('should call API and reset unread count', async () => {
      chatStore.conversations = [
        createMockConversation({ id: 1, unreadCount: 5, atMsgIds: [1, 2] }),
      ]
      vi.mocked(conversationApi.markAsRead).mockResolvedValue(undefined)

      await chatStore.markAsRead(1)

      expect(conversationApi.markAsRead).toHaveBeenCalledWith(1)
      expect(chatStore.conversations[0].unreadCount).toBe(0)
      expect(chatStore.conversations[0].atMsgIds).toEqual([])
    })
  })

  describe('setCurrentConversation', () => {
    it('should set current conversation ID', () => {
      vi.mocked(conversationApi.markAsRead).mockResolvedValue(undefined)

      chatStore.conversations = [createMockConversation({ id: 5 })]
      chatStore.setCurrentConversation(5)

      expect(chatStore.currentConversationId).toBe(5)
    })

    it('should mark conversation as read when set', () => {
      vi.mocked(conversationApi.markAsRead).mockResolvedValue(undefined)

      chatStore.conversations = [createMockConversation({ id: 5, unreadCount: 3 })]
      chatStore.setCurrentConversation(5)

      expect(conversationApi.markAsRead).toHaveBeenCalledWith(5)
    })

    it('should handle null conversation ID', () => {
      chatStore.setCurrentConversation(null)
      expect(chatStore.currentConversationId).toBeNull()
    })
  })

  describe('handleNewMessage', () => {
    it('should add new message to conversation', () => {
      chatStore.conversations = [createMockConversation({ id: 1 })]
      chatStore.messages.set(1, [])

      const newMessage = createMockMessage({ msgId: 'new-msg' })
      chatStore.handleNewMessage(newMessage)

      const messages = chatStore.messages.get(1)
      expect(messages).toHaveLength(1)
      expect(messages![0].msgId).toBe('new-msg')
    })

    it('should update conversation last message', () => {
      chatStore.conversations = [createMockConversation({ id: 1 })]
      chatStore.messages.set(1, [])

      const newMessage = createMockMessage({ msgId: 'new-msg' })
      chatStore.handleNewMessage(newMessage)

      expect(chatStore.conversations[0].lastMessage).toEqual(newMessage)
    })

    it('should increment unread count if not current conversation', () => {
      chatStore.conversations = [createMockConversation({ id: 1, unreadCount: 0 })]
      chatStore.currentConversationId = 2 // Different conversation
      chatStore.messages.set(1, [])

      chatStore.handleNewMessage(createMockMessage({ conversationId: 1 }))

      expect(chatStore.conversations[0].unreadCount).toBe(1)
    })

    it('should not increment unread count if current conversation', () => {
      chatStore.conversations = [createMockConversation({ id: 1, unreadCount: 0 })]
      chatStore.currentConversationId = 1
      chatStore.messages.set(1, [])

      chatStore.handleNewMessage(createMockMessage({ conversationId: 1 }))

      expect(chatStore.conversations[0].unreadCount).toBe(0)
    })

    it('should not add duplicate messages', () => {
      chatStore.messages.set(1, [createMockMessage({ msgId: 'msg-1' })])

      chatStore.handleNewMessage(createMockMessage({ msgId: 'msg-1' }))

      expect(chatStore.messages.get(1)).toHaveLength(1)
    })
  })

  describe('handleMessageRecalled', () => {
    it('should update message as recalled', () => {
      chatStore.messages.set(1, [createMockMessage({ msgId: 'msg-1', content: 'Hello' })])

      chatStore.handleMessageRecalled('msg-1')

      const messages = chatStore.messages.get(1)
      expect(messages![0].msgType).toBe('recall')
      expect(messages![0].content).toBe('')
      expect(messages![0].recalledAt).toBeDefined()
    })
  })

  describe('handleReadStatusSync', () => {
    it('should reset unread count for conversation', () => {
      chatStore.conversations = [
        createMockConversation({ id: 1, unreadCount: 5, atMsgIds: [1] }),
      ]

      chatStore.handleReadStatusSync(1, 100)

      expect(chatStore.conversations[0].unreadCount).toBe(0)
      expect(chatStore.conversations[0].atMsgIds).toEqual([])
    })
  })

  describe('Getters', () => {
    describe('currentConversation', () => {
      it('should return null when no current conversation', () => {
        expect(chatStore.currentConversation).toBeNull()
      })

      it('should return the current conversation', () => {
        const conv = createMockConversation({ id: 5 })
        chatStore.conversations = [conv]
        chatStore.currentConversationId = 5

        expect(chatStore.currentConversation).toEqual(conv)
      })
    })

    describe('currentMessages', () => {
      it('should return empty array when no current conversation', () => {
        expect(chatStore.currentMessages).toEqual([])
      })

      it('should return messages for current conversation', () => {
        const messages = [createMockMessage()]
        chatStore.messages.set(1, messages)
        chatStore.currentConversationId = 1

        expect(chatStore.currentMessages).toEqual(messages)
      })
    })

    describe('sortedConversations', () => {
      it('should sort pinned conversations first', () => {
        chatStore.conversations = [
          createMockConversation({ id: 1, isPinned: false }),
          createMockConversation({ id: 2, isPinned: true }),
        ]

        const sorted = chatStore.sortedConversations
        expect(sorted[0].id).toBe(2)
        expect(sorted[1].id).toBe(1)
      })

      it('should sort by lastMsgTime within same pin status', () => {
        chatStore.conversations = [
          createMockConversation({ id: 1, lastMsgTime: '2024-01-01T00:00:00Z' }),
          createMockConversation({ id: 2, lastMsgTime: '2024-01-02T00:00:00Z' }),
        ]

        const sorted = chatStore.sortedConversations
        expect(sorted[0].id).toBe(2)
        expect(sorted[1].id).toBe(1)
      })
    })

    describe('totalUnreadCount', () => {
      it('should sum all unread counts', () => {
        chatStore.conversations = [
          createMockConversation({ id: 1, unreadCount: 3 }),
          createMockConversation({ id: 2, unreadCount: 5 }),
          createMockConversation({ id: 3, unreadCount: 2 }),
        ]

        expect(chatStore.totalUnreadCount).toBe(10)
      })
    })

    describe('hasAtMe', () => {
      it('should return false when no @mentions', () => {
        chatStore.conversations = [
          createMockConversation({ id: 1, atMsgIds: [] }),
        ]

        expect(chatStore.hasAtMe).toBe(false)
      })

      it('should return true when @mentions exist', () => {
        chatStore.conversations = [
          createMockConversation({ id: 1, atMsgIds: [1, 2] }),
        ]

        expect(chatStore.hasAtMe).toBe(true)
      })
    })
  })

  describe('clearMessages', () => {
    it('should remove messages for a conversation', () => {
      chatStore.messages.set(1, [createMockMessage()])
      chatStore.messages.set(2, [createMockMessage()])

      chatStore.clearMessages(1)

      expect(chatStore.messages.has(1)).toBe(false)
      expect(chatStore.messages.has(2)).toBe(true)
    })
  })

  describe('reset', () => {
    it('should clear all state', () => {
      chatStore.conversations = [createMockConversation()]
      chatStore.currentConversationId = 1
      chatStore.messages.set(1, [createMockMessage()])
      chatStore.hasMoreMessages.set(1, true)

      chatStore.reset()

      expect(chatStore.conversations).toEqual([])
      expect(chatStore.currentConversationId).toBeNull()
      expect(chatStore.messages.size).toBe(0)
      expect(chatStore.hasMoreMessages.size).toBe(0)
    })
  })
})
