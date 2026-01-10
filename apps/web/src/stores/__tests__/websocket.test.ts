import { describe, it, expect, vi, beforeEach, type MockInstance } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useWebSocketStore } from '@/stores/websocket'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { websocketService } from '@/services/websocket'
import type { Message } from '@/types'

// Mock the websocket service
vi.mock('@/services/websocket', () => ({
  websocketService: {
    setHandlers: vi.fn(),
    connect: vi.fn(),
    disconnect: vi.fn(),
    updateToken: vi.fn(),
    sendTyping: vi.fn(),
    sendReadAck: vi.fn(),
    requestSync: vi.fn(),
  },
}))

// Mock conversation API to prevent unhandled rejections
vi.mock('@/api/conversation', () => ({
  conversationApi: {
    getConversations: vi.fn().mockResolvedValue([]),
  },
}))

// Helper to create mock message
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

describe('WebSocket Store', () => {
  let wsStore: ReturnType<typeof useWebSocketStore>
  let chatStore: ReturnType<typeof useChatStore>
  let userStore: ReturnType<typeof useUserStore>
  let capturedHandlers: Record<string, (...args: unknown[]) => void>

  beforeEach(() => {
    vi.clearAllMocks()
    capturedHandlers = {}

    // Capture handlers when setHandlers is called
    vi.mocked(websocketService.setHandlers).mockImplementation((handlers: Record<string, (...args: unknown[]) => void>) => {
      capturedHandlers = { ...capturedHandlers, ...handlers }
    })

    localStorage.getItem = vi.fn((key) => {
      if (key === 'deviceId') return 'test-device-id'
      return null
    })

    setActivePinia(createPinia())
    wsStore = useWebSocketStore()
    chatStore = useChatStore()
    userStore = useUserStore()
  })

  describe('Initial State', () => {
    it('should have disconnected status', () => {
      expect(wsStore.status).toBe('disconnected')
    })

    it('should have null lastError', () => {
      expect(wsStore.lastError).toBeNull()
    })

    it('should have 0 reconnectAttempt', () => {
      expect(wsStore.reconnectAttempt).toBe(0)
    })

    it('should have null kickedOfflineReason', () => {
      expect(wsStore.kickedOfflineReason).toBeNull()
    })
  })

  describe('Getters', () => {
    it('isConnected should return true when connected', () => {
      wsStore.status = 'connected'
      expect(wsStore.isConnected).toBe(true)
    })

    it('isConnected should return false when not connected', () => {
      wsStore.status = 'disconnected'
      expect(wsStore.isConnected).toBe(false)
    })

    it('isConnecting should return true when connecting', () => {
      wsStore.status = 'connecting'
      expect(wsStore.isConnecting).toBe(true)
    })

    it('isConnecting should return true when reconnecting', () => {
      wsStore.status = 'reconnecting'
      expect(wsStore.isConnecting).toBe(true)
    })

    it('isDisconnected should return true when disconnected', () => {
      wsStore.status = 'disconnected'
      expect(wsStore.isDisconnected).toBe(true)
    })
  })

  describe('connect', () => {
    it('should not connect without token', async () => {
      userStore.token = null

      await wsStore.connect()

      expect(websocketService.connect).not.toHaveBeenCalled()
    })

    it('should not connect without deviceId', async () => {
      userStore.token = 'test-token'
      localStorage.getItem = vi.fn().mockReturnValue(null)

      await wsStore.connect()

      expect(websocketService.connect).not.toHaveBeenCalled()
    })

    it('should connect with token and deviceId', async () => {
      userStore.token = 'test-token'
      vi.mocked(websocketService.connect).mockResolvedValue(undefined)

      await wsStore.connect()

      expect(websocketService.setHandlers).toHaveBeenCalled()
      expect(websocketService.connect).toHaveBeenCalledWith(
        expect.stringMatching(/^wss?:\/\//),
        'test-token',
        'test-device-id',
        'web'
      )
    })

    it('should set status to connecting', async () => {
      userStore.token = 'test-token'
      vi.mocked(websocketService.connect).mockImplementation(async () => {
        expect(wsStore.status).toBe('connecting')
      })

      await wsStore.connect()
    })

    it('should clear errors on connect', async () => {
      userStore.token = 'test-token'
      wsStore.lastError = 'previous error'
      wsStore.kickedOfflineReason = 'previous reason'
      vi.mocked(websocketService.connect).mockResolvedValue(undefined)

      await wsStore.connect()

      expect(wsStore.lastError).toBeNull()
      expect(wsStore.kickedOfflineReason).toBeNull()
    })

    it('should handle connection error', async () => {
      userStore.token = 'test-token'
      vi.mocked(websocketService.connect).mockRejectedValue(new Error('Connection failed'))

      await expect(wsStore.connect()).rejects.toThrow('Connection failed')

      expect(wsStore.status).toBe('disconnected')
      expect(wsStore.lastError).toBe('Connection failed')
    })
  })

  describe('disconnect', () => {
    it('should disconnect and set status', () => {
      wsStore.status = 'connected'

      wsStore.disconnect()

      expect(websocketService.disconnect).toHaveBeenCalled()
      expect(wsStore.status).toBe('disconnected')
    })
  })

  describe('updateToken', () => {
    it('should update token in websocket service', () => {
      wsStore.updateToken('new-token')

      expect(websocketService.updateToken).toHaveBeenCalledWith('new-token')
    })
  })

  describe('sendTyping', () => {
    it('should send typing when connected', async () => {
      wsStore.status = 'connected'
      vi.mocked(websocketService.sendTyping).mockResolvedValue(undefined)

      await wsStore.sendTyping(1)

      expect(websocketService.sendTyping).toHaveBeenCalledWith(1)
    })

    it('should not send typing when disconnected', async () => {
      wsStore.status = 'disconnected'

      await wsStore.sendTyping(1)

      expect(websocketService.sendTyping).not.toHaveBeenCalled()
    })

    it('should handle sendTyping error gracefully', async () => {
      wsStore.status = 'connected'
      vi.mocked(websocketService.sendTyping).mockRejectedValue(new Error('Failed'))

      // Should not throw
      await wsStore.sendTyping(1)
    })
  })

  describe('sendReadAck', () => {
    it('should send read ack when connected', async () => {
      wsStore.status = 'connected'
      vi.mocked(websocketService.sendReadAck).mockResolvedValue(undefined)

      await wsStore.sendReadAck(1, 100)

      expect(websocketService.sendReadAck).toHaveBeenCalledWith(1, 100)
    })

    it('should not send read ack when disconnected', async () => {
      wsStore.status = 'disconnected'

      await wsStore.sendReadAck(1, 100)

      expect(websocketService.sendReadAck).not.toHaveBeenCalled()
    })
  })

  describe('requestSync', () => {
    it('should request sync when connected', async () => {
      wsStore.status = 'connected'
      vi.mocked(websocketService.requestSync).mockResolvedValue(undefined)

      await wsStore.requestSync(50)

      expect(websocketService.requestSync).toHaveBeenCalledWith(50)
    })

    it('should request sync without cursor', async () => {
      wsStore.status = 'connected'
      vi.mocked(websocketService.requestSync).mockResolvedValue(undefined)

      await wsStore.requestSync()

      expect(websocketService.requestSync).toHaveBeenCalledWith(undefined)
    })

    it('should not request sync when disconnected', async () => {
      wsStore.status = 'disconnected'

      await wsStore.requestSync()

      expect(websocketService.requestSync).not.toHaveBeenCalled()
    })
  })

  describe('Event Handlers', () => {
    beforeEach(async () => {
      userStore.token = 'test-token'
      vi.mocked(websocketService.connect).mockResolvedValue(undefined)
      await wsStore.connect()
    })

    it('should handle onConnected', () => {
      capturedHandlers.onConnected?.()

      expect(wsStore.status).toBe('connected')
      expect(wsStore.reconnectAttempt).toBe(0)
    })

    it('should handle onDisconnected', () => {
      wsStore.status = 'connected'

      capturedHandlers.onDisconnected?.()

      expect(wsStore.status).toBe('disconnected')
    })

    it('should handle onReconnecting', () => {
      capturedHandlers.onReconnecting?.(3)

      expect(wsStore.status).toBe('reconnecting')
      expect(wsStore.reconnectAttempt).toBe(3)
    })

    it('should handle onMessage and forward to chat store', () => {
      const message = createMockMessage()
      const handleNewMessageSpy = vi.spyOn(chatStore, 'handleNewMessage')

      capturedHandlers.onMessage?.(message)

      expect(handleNewMessageSpy).toHaveBeenCalledWith(message)
    })

    it('should handle onTyping and forward to chat store', () => {
      const handleTypingSpy = vi.spyOn(chatStore, 'handleTypingIndicator')

      capturedHandlers.onTyping?.(1, 2)

      expect(handleTypingSpy).toHaveBeenCalledWith(1, 2)
    })

    it('should handle onRecall and forward to chat store', () => {
      const handleRecallSpy = vi.spyOn(chatStore, 'handleMessageRecalled')

      capturedHandlers.onRecall?.('msg-123')

      expect(handleRecallSpy).toHaveBeenCalledWith('msg-123')
    })

    it('should handle onReadSync and forward to chat store', () => {
      const handleSyncSpy = vi.spyOn(chatStore, 'handleReadStatusSync')

      capturedHandlers.onReadSync?.(1, 100)

      expect(handleSyncSpy).toHaveBeenCalledWith(1, 100)
    })

    it('should handle onKickedOffline', () => {
      userStore.token = 'test-token'
      userStore.isLoggedIn = true
      const clearAuthSpy = vi.spyOn(userStore, 'clearAuth')

      capturedHandlers.onKickedOffline?.('Another device logged in')

      expect(wsStore.status).toBe('disconnected')
      expect(wsStore.kickedOfflineReason).toBe('Another device logged in')
      expect(clearAuthSpy).toHaveBeenCalled()
    })

    it('should handle onError', () => {
      capturedHandlers.onError?.('Some error occurred')

      expect(wsStore.lastError).toBe('Some error occurred')
    })
  })
})
