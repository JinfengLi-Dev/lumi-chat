import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../user'
import { useChatStore } from '../chat'
import { useWebSocketStore } from '../websocket'

// Mock all APIs
vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refreshToken: vi.fn(),
  },
}))

vi.mock('@/api/user', () => ({
  userApi: {
    getCurrentUser: vi.fn(),
    getDevices: vi.fn(),
    updateProfile: vi.fn(),
    uploadAvatar: vi.fn(),
    logoutDevice: vi.fn(),
    logoutAllDevices: vi.fn(),
  },
}))

vi.mock('@/api/conversation', () => ({
  conversationApi: {
    getConversations: vi.fn(),
    createPrivateConversation: vi.fn(),
    createGroupConversation: vi.fn(),
    markAsRead: vi.fn(),
    updateConversation: vi.fn(),
  },
}))

vi.mock('@/api/message', () => ({
  messageApi: {
    getMessages: vi.fn(),
    sendMessage: vi.fn(),
    recallMessage: vi.fn(),
    deleteMessage: vi.fn(),
    forwardMessage: vi.fn(),
  },
}))

// Import mocked APIs
import { authApi } from '@/api/auth'
import { conversationApi } from '@/api/conversation'
import { messageApi } from '@/api/message'

describe('Store Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  describe('User Authentication Flow', () => {
    it('should complete login flow and set authentication', async () => {
      const userStore = useUserStore()

      // Mock successful login
      const mockUser = {
        id: 1,
        uid: 'user123',
        email: 'test@example.com',
        nickname: 'TestUser',
        status: 'active' as const,
        gender: 'male' as const,
        createdAt: '2025-01-01T00:00:00Z',
      }
      const mockLoginResponse = {
        token: 'mock-token',
        refreshToken: 'mock-refresh',
        user: mockUser,
        expiresAt: Date.now() + 3600000,
      }

      vi.mocked(authApi.login).mockResolvedValue(mockLoginResponse)

      // Perform login
      await userStore.login({
        email: 'test@example.com',
        password: 'password123',
      })

      // Verify user is authenticated
      expect(userStore.isAuthenticated).toBe(true)
      expect(userStore.user?.email).toBe('test@example.com')
      expect(userStore.token).toBe('mock-token')
    })

    it('should handle registration', async () => {
      const userStore = useUserStore()

      const mockRegisterResponse = {
        id: 2,
        uid: 'newuser123',
        email: 'newuser@example.com',
        nickname: 'NewUser',
        status: 'active' as const,
        gender: 'female' as const,
        createdAt: '2025-01-01T00:00:00Z',
      }

      vi.mocked(authApi.register).mockResolvedValue(mockRegisterResponse)

      const response = await userStore.register({
        email: 'newuser@example.com',
        password: 'password123',
        nickname: 'NewUser',
        gender: 'female',
      })

      expect(response.email).toBe('newuser@example.com')
    })

    it('should clear all stores on logout', async () => {
      const userStore = useUserStore()

      // Setup authenticated state by setting auth directly
      userStore.setAuth({
        token: 'test-token',
        refreshToken: 'test-refresh',
        user: {
          id: 1,
          uid: 'user123',
          email: 'test@example.com',
          nickname: 'TestUser',
          status: 'active',
          gender: 'male',
          createdAt: '2025-01-01T00:00:00Z',
        },
        expiresAt: Date.now() + 3600000,
      })

      expect(userStore.isAuthenticated).toBe(true)

      vi.mocked(authApi.logout).mockResolvedValue(undefined)

      await userStore.logout()

      // Verify stores are cleared
      expect(userStore.isAuthenticated).toBe(false)
      expect(userStore.token).toBeNull()
      expect(userStore.user).toBeNull()
    })
  })

  describe('Conversation and Message Flow', () => {
    beforeEach(async () => {
      const userStore = useUserStore()
      // Setup authenticated user
      userStore.setAuth({
        token: 'test-token',
        refreshToken: 'test-refresh',
        user: {
          id: 1,
          uid: 'user123',
          email: 'test@example.com',
          nickname: 'TestUser',
          status: 'active',
          gender: 'male',
          createdAt: '2025-01-01T00:00:00Z',
        },
        expiresAt: Date.now() + 3600000,
      })
    })

    it('should load conversations and select one', async () => {
      const chatStore = useChatStore()

      const mockConversations = [
        {
          id: 1,
          type: 'private' as const,
          participantIds: [1, 2],
          targetUser: {
            id: 2,
            uid: 'friend123',
            email: 'friend@example.com',
            nickname: 'Friend',
            status: 'active' as const,
            gender: 'male' as const,
            createdAt: '2025-01-01T00:00:00Z',
          },
          unreadCount: 3,
          isMuted: false,
          isPinned: false,
          lastMsgTime: '2025-01-01T12:00:00Z',
        },
        {
          id: 2,
          type: 'group' as const,
          participantIds: [1, 2, 3],
          groupId: 100,
          group: {
            id: 100,
            gid: 'group123',
            name: 'Test Group',
            ownerId: 1,
            creatorId: 1,
            maxMembers: 500,
            createdAt: '2025-01-01T00:00:00Z',
          },
          unreadCount: 0,
          isMuted: true,
          isPinned: true,
          lastMsgTime: '2025-01-01T10:00:00Z',
        },
      ]

      vi.mocked(conversationApi.getConversations).mockResolvedValue(mockConversations)
      vi.mocked(conversationApi.markAsRead).mockResolvedValue(undefined)

      await chatStore.fetchConversations()

      // Verify conversations loaded
      expect(chatStore.conversations).toHaveLength(2)
      expect(chatStore.totalUnreadCount).toBe(3)

      // Verify sorting (pinned first, then by time)
      expect(chatStore.sortedConversations[0].id).toBe(2) // Pinned
      expect(chatStore.sortedConversations[1].id).toBe(1)

      // Select a conversation
      chatStore.setCurrentConversation(1)
      expect(chatStore.currentConversationId).toBe(1)
      expect(chatStore.currentConversation?.id).toBe(1)
    })

    it('should load messages for conversation', async () => {
      const chatStore = useChatStore()

      const mockMessages = [
        {
          id: 1,
          msgId: 'msg-1',
          conversationId: 1,
          senderId: 2,
          msgType: 'text' as const,
          content: 'Hello!',
          serverCreatedAt: '2025-01-01T11:00:00Z',
        },
        {
          id: 2,
          msgId: 'msg-2',
          conversationId: 1,
          senderId: 1,
          msgType: 'text' as const,
          content: 'Hi there!',
          serverCreatedAt: '2025-01-01T11:01:00Z',
        },
      ]

      vi.mocked(messageApi.getMessages).mockResolvedValue({
        items: mockMessages,
        total: 2,
        page: 1,
        pageSize: 50,
        hasMore: false,
      })

      await chatStore.fetchMessages(1)

      // Access messages via store's internal method - messages is a Map
      const messages = chatStore.messages.get(1)
      expect(messages).toHaveLength(2)
      expect(messages?.[0].content).toBe('Hello!')
    })

    it('should send message and update conversation', async () => {
      const chatStore = useChatStore()

      // Setup conversation
      chatStore.conversations = [{
        id: 1,
        type: 'private',
        participantIds: [1, 2],
        unreadCount: 0,
        isMuted: false,
        isPinned: false,
      }]

      const mockSentMessage = {
        id: 3,
        msgId: 'msg-3',
        conversationId: 1,
        senderId: 1,
        msgType: 'text' as const,
        content: 'New message',
        serverCreatedAt: '2025-01-01T12:00:00Z',
        status: 'sent' as const,
      }

      vi.mocked(messageApi.sendMessage).mockResolvedValue(mockSentMessage)

      await chatStore.sendMessage(1, 'text', 'New message')

      // Verify message was added
      const messages = chatStore.messages.get(1)
      expect(messages?.some(m => m.content === 'New message')).toBe(true)
    })
  })

  describe('WebSocket Connection Flow', () => {
    it('should start with disconnected status', () => {
      const wsStore = useWebSocketStore()

      // Initially disconnected
      expect(wsStore.status).toBe('disconnected')
      expect(wsStore.isConnected).toBe(false)
      expect(wsStore.isDisconnected).toBe(true)
    })

    it('should have working disconnect method', () => {
      const wsStore = useWebSocketStore()

      // Set to connected state
      wsStore.status = 'connected'
      expect(wsStore.isConnected).toBe(true)

      // Call disconnect
      wsStore.disconnect()

      expect(wsStore.status).toBe('disconnected')
      expect(wsStore.isConnected).toBe(false)
    })

    it('should track reconnect attempts via state', () => {
      const wsStore = useWebSocketStore()

      // Simulate reconnecting state by setting state directly
      wsStore.status = 'reconnecting'
      wsStore.reconnectAttempt = 3

      expect(wsStore.status).toBe('reconnecting')
      expect(wsStore.reconnectAttempt).toBe(3)
      expect(wsStore.isConnecting).toBe(true)
    })

    it('should track kicked offline reason', () => {
      const wsStore = useWebSocketStore()

      // Simulate kicked offline by setting state
      wsStore.kickedOfflineReason = 'Another device logged in'
      wsStore.status = 'disconnected'

      expect(wsStore.kickedOfflineReason).toBe('Another device logged in')
      expect(wsStore.isConnected).toBe(false)
    })
  })

  describe('Message Handling Integration', () => {
    it('should handle incoming message via handleNewMessage', () => {
      const chatStore = useChatStore()

      // Setup conversation
      chatStore.conversations = [{
        id: 1,
        type: 'private',
        participantIds: [1, 2],
        unreadCount: 0,
        isMuted: false,
        isPinned: false,
      }]

      // Simulate incoming message via WebSocket
      const incomingMessage = {
        id: 100,
        msgId: 'incoming-msg-1',
        conversationId: 1,
        senderId: 2,
        msgType: 'text' as const,
        content: 'Incoming message',
        serverCreatedAt: new Date().toISOString(),
      }

      chatStore.handleNewMessage(incomingMessage)

      // Verify message added
      const messages = chatStore.messages.get(1)
      expect(messages).toContainEqual(expect.objectContaining({
        msgId: 'incoming-msg-1',
      }))
    })

    it('should handle message recall via handleMessageRecalled', () => {
      const chatStore = useChatStore()

      // Setup with existing message - using Map directly
      chatStore.messages.set(1, [{
        id: 1,
        msgId: 'msg-to-recall',
        conversationId: 1,
        senderId: 1,
        msgType: 'text',
        content: 'Original message',
        serverCreatedAt: '2025-01-01T12:00:00Z',
      }])

      // Recall the message
      chatStore.handleMessageRecalled('msg-to-recall')

      // Verify message is marked as recalled
      const messages = chatStore.messages.get(1)
      expect(messages?.[0].recalledAt).toBeDefined()
      expect(messages?.[0].msgType).toBe('recall')
    })
  })

  describe('Data Persistence', () => {
    it('should call localStorage.setItem when setAuth is called', () => {
      const userStore = useUserStore()

      userStore.setAuth({
        token: 'persist-token',
        refreshToken: 'persist-refresh',
        user: {
          id: 1,
          uid: 'user123',
          email: 'test@example.com',
          nickname: 'TestUser',
          status: 'active',
          gender: 'male',
          createdAt: '2025-01-01T00:00:00Z',
        },
        expiresAt: Date.now() + 3600000,
      })

      // Verify localStorage.setItem was called (localStorage is mocked)
      expect(localStorage.setItem).toHaveBeenCalledWith('token', 'persist-token')
      expect(localStorage.setItem).toHaveBeenCalledWith('refreshToken', 'persist-refresh')
    })

    it('should call localStorage.getItem during store initialization', () => {
      // Create new pinia and store - this should trigger localStorage.getItem
      setActivePinia(createPinia())

      // The store's state initializer calls localStorage.getItem('token')
      // Since localStorage is mocked, we verify the call was made
      useUserStore()

      expect(localStorage.getItem).toHaveBeenCalledWith('token')
      expect(localStorage.getItem).toHaveBeenCalledWith('refreshToken')
    })

    it('should call localStorage.removeItem on clearAuth', () => {
      const userStore = useUserStore()

      // Setup some auth data first
      userStore.token = 'test-token'
      userStore.refreshToken = 'test-refresh'
      userStore.isLoggedIn = true

      userStore.clearAuth()

      expect(localStorage.removeItem).toHaveBeenCalledWith('token')
      expect(localStorage.removeItem).toHaveBeenCalledWith('refreshToken')
    })
  })

  describe('Error Handling', () => {
    it('should handle login failure gracefully', async () => {
      const userStore = useUserStore()

      vi.mocked(authApi.login).mockRejectedValue(new Error('Invalid credentials'))

      await expect(userStore.login({
        email: 'wrong@example.com',
        password: 'wrongpass',
      })).rejects.toThrow('Invalid credentials')

      expect(userStore.isAuthenticated).toBe(false)
    })

    it('should handle conversation fetch failure', async () => {
      const chatStore = useChatStore()

      vi.mocked(conversationApi.getConversations).mockRejectedValue(
        new Error('Network error')
      )

      await expect(chatStore.fetchConversations()).rejects.toThrow('Network error')
    })

    it('should handle message send failure and mark message as failed', async () => {
      const chatStore = useChatStore()

      vi.mocked(messageApi.sendMessage).mockRejectedValue(
        new Error('Failed to send')
      )

      await expect(
        chatStore.sendMessage(1, 'text', 'Failed message')
      ).rejects.toThrow('Failed to send')

      // Verify the temp message is marked as failed
      const messages = chatStore.messages.get(1)
      expect(messages?.some(m => m.status === 'failed')).toBe(true)
    })
  })

  describe('Cross-Store Integration', () => {
    it('should update unread count when receiving messages', () => {
      const chatStore = useChatStore()

      // Setup conversation that is not currently selected
      chatStore.conversations = [{
        id: 1,
        type: 'private',
        participantIds: [1, 2],
        unreadCount: 0,
        isMuted: false,
        isPinned: false,
      }]
      chatStore.currentConversationId = 999 // Different conversation

      // Receive message
      chatStore.handleNewMessage({
        id: 100,
        msgId: 'new-msg',
        conversationId: 1,
        senderId: 2,
        msgType: 'text',
        content: 'New message',
        serverCreatedAt: new Date().toISOString(),
      })

      // Unread count should increase
      expect(chatStore.conversations[0].unreadCount).toBe(1)
      expect(chatStore.totalUnreadCount).toBe(1)
    })

    it('should clear unread count when marking as read', async () => {
      const chatStore = useChatStore()

      // Setup conversation with unread
      chatStore.conversations = [{
        id: 1,
        type: 'private',
        participantIds: [1, 2],
        unreadCount: 5,
        isMuted: false,
        isPinned: false,
        atMsgIds: [1, 2],
      }]

      vi.mocked(conversationApi.markAsRead).mockResolvedValue(undefined)

      await chatStore.markAsRead(1)

      expect(chatStore.conversations[0].unreadCount).toBe(0)
      expect(chatStore.conversations[0].atMsgIds).toEqual([])
    })

    it('should reset chat store data on reset', () => {
      const chatStore = useChatStore()

      // Setup some data
      chatStore.conversations = [{
        id: 1,
        type: 'private',
        participantIds: [1, 2],
        unreadCount: 5,
        isMuted: false,
        isPinned: false,
      }]
      chatStore.currentConversationId = 1
      chatStore.messages.set(1, [{
        id: 1,
        msgId: 'msg-1',
        conversationId: 1,
        senderId: 1,
        msgType: 'text',
        content: 'Test',
        serverCreatedAt: '2025-01-01T00:00:00Z',
      }])

      chatStore.reset()

      expect(chatStore.conversations).toEqual([])
      expect(chatStore.currentConversationId).toBeNull()
      expect(chatStore.messages.size).toBe(0)
    })
  })
})
