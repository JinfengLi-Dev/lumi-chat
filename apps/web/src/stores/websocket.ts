import { defineStore } from 'pinia'
import { websocketService, type ConnectionStatus } from '@/services/websocket'
import { useChatStore } from './chat'
import { useUserStore } from './user'
import { useFriendStore } from './friend'
import type { Message } from '@/types'

interface WebSocketState {
  status: ConnectionStatus
  lastError: string | null
  reconnectAttempt: number
  kickedOfflineReason: string | null
}

export const useWebSocketStore = defineStore('websocket', {
  state: (): WebSocketState => ({
    status: 'disconnected',
    lastError: null,
    reconnectAttempt: 0,
    kickedOfflineReason: null,
  }),

  getters: {
    isConnected: (state) => state.status === 'connected',
    isConnecting: (state) => state.status === 'connecting' || state.status === 'reconnecting',
    isDisconnected: (state) => state.status === 'disconnected',
  },

  actions: {
    async connect() {
      const userStore = useUserStore()

      if (!userStore.token) {
        console.warn('[WebSocket Store] Cannot connect: no token')
        return
      }

      const deviceId = localStorage.getItem('deviceId') || ''
      if (!deviceId) {
        console.warn('[WebSocket Store] Cannot connect: no deviceId')
        return
      }

      // Determine WebSocket URL
      // In development, use Vite's proxy (connect to same host/port as the app)
      // In production, use environment variables or direct connection
      const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      let wsUrl: string

      if (import.meta.env.VITE_WS_URL) {
        // Use explicit URL if provided
        wsUrl = import.meta.env.VITE_WS_URL
      } else if (import.meta.env.DEV) {
        // In development, use Vite's proxy (same origin)
        wsUrl = `${wsProtocol}//${window.location.host}/ws`
      } else {
        // In production, configure via environment
        const wsHost = import.meta.env.VITE_WS_HOST || window.location.hostname
        const wsPort = import.meta.env.VITE_WS_PORT || '17901'
        wsUrl = `${wsProtocol}//${wsHost}:${wsPort}/ws`
      }

      this.lastError = null
      this.kickedOfflineReason = null

      // Set up event handlers
      websocketService.setHandlers({
        onConnected: () => {
          this.status = 'connected'
          this.reconnectAttempt = 0
          console.log('[WebSocket Store] Connected')
        },

        onDisconnected: () => {
          this.status = 'disconnected'
          console.log('[WebSocket Store] Disconnected')
        },

        onReconnecting: (attempt) => {
          this.status = 'reconnecting'
          this.reconnectAttempt = attempt
          console.log(`[WebSocket Store] Reconnecting (attempt ${attempt})`)
        },

        onMessage: (message: Message) => {
          const chatStore = useChatStore()
          chatStore.handleNewMessage(message)
        },

        onMessageAck: (msgId, serverTimestamp, success) => {
          console.log(`[WebSocket Store] Message ACK: ${msgId}, success: ${success}, time: ${serverTimestamp}`)
          // Message status is already handled in chat store
        },

        onTyping: (conversationId, userId) => {
          const chatStore = useChatStore()
          chatStore.handleTypingIndicator(conversationId, userId)
        },

        onRecall: (msgId) => {
          const chatStore = useChatStore()
          chatStore.handleMessageRecalled(msgId)
        },

        onReadSync: (conversationId, lastReadMsgId) => {
          const chatStore = useChatStore()
          chatStore.handleReadStatusSync(conversationId, lastReadMsgId)
        },

        onOnlineStatusResponse: (statuses) => {
          const friendStore = useFriendStore()
          friendStore.handleOnlineStatusResponse(statuses)
        },

        onOnlineStatusChange: (userId, isOnline) => {
          const friendStore = useFriendStore()
          friendStore.handleOnlineStatusChange(userId, isOnline)
        },

        onReadReceiptNotify: (conversationId, readerId, lastReadMsgId) => {
          const chatStore = useChatStore()
          chatStore.handleReadReceiptNotify(conversationId, readerId, lastReadMsgId)
        },

        onKickedOffline: (reason) => {
          this.status = 'disconnected'
          this.kickedOfflineReason = reason
          console.warn('[WebSocket Store] Kicked offline:', reason)

          // Clear auth and redirect to login
          const userStore = useUserStore()
          userStore.clearAuth()
        },

        onError: (error) => {
          this.lastError = error
          console.error('[WebSocket Store] Error:', error)
        },
      })

      try {
        this.status = 'connecting'
        await websocketService.connect(wsUrl, userStore.token, deviceId, 'web')
      } catch (error) {
        this.status = 'disconnected'
        this.lastError = error instanceof Error ? error.message : 'Connection failed'
        throw error
      }
    },

    disconnect() {
      websocketService.disconnect()
      this.status = 'disconnected'
    },

    updateToken(newToken: string) {
      websocketService.updateToken(newToken)
    },

    // Proxy methods to websocketService
    async sendTyping(conversationId: number) {
      if (this.status !== 'connected') return
      try {
        await websocketService.sendTyping(conversationId)
      } catch (error) {
        console.warn('[WebSocket Store] Failed to send typing:', error)
      }
    },

    async sendReadAck(conversationId: number, lastReadMsgId: number) {
      if (this.status !== 'connected') return
      try {
        await websocketService.sendReadAck(conversationId, lastReadMsgId)
      } catch (error) {
        console.warn('[WebSocket Store] Failed to send read ack:', error)
      }
    },

    async requestSync(lastSyncCursor?: number) {
      if (this.status !== 'connected') return
      try {
        await websocketService.requestSync(lastSyncCursor)
      } catch (error) {
        console.warn('[WebSocket Store] Failed to request sync:', error)
      }
    },
  },
})
