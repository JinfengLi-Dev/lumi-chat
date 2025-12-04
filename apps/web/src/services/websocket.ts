import type { Message, MessageType, MessageMetadata } from '@/types'

// Protocol types matching IM server's ProtocolType.java
export const ProtocolType = {
  // Client to Server
  LOGIN: 1,
  LOGOUT: 2,
  HEARTBEAT: 3,
  CHAT_MESSAGE: 10,
  TYPING: 11,
  READ_ACK: 12,
  RECALL_MESSAGE: 13,
  SYNC_REQUEST: 20,

  // Server to Client
  LOGIN_RESPONSE: 101,
  LOGOUT_RESPONSE: 102,
  HEARTBEAT_RESPONSE: 103,
  CHAT_MESSAGE_ACK: 110,
  RECEIVE_MESSAGE: 111,
  TYPING_NOTIFY: 112,
  RECALL_NOTIFY: 113,
  SYNC_RESPONSE: 120,

  // System Messages
  KICKED_OFFLINE: 200,
  SERVER_ERROR: 500,
} as const

export interface Packet {
  type: number
  seq: string
  data: unknown
  timestamp: number
}

export interface LoginData {
  token: string
  deviceId: string
  deviceType: string
}

export interface ChatMessageData {
  msgId: string
  conversationId: number
  msgType: MessageType
  content: string
  metadata?: MessageMetadata
  quoteMsgId?: string
  atUserIds?: number[]
  clientCreatedAt: number
}

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'reconnecting'

export interface WebSocketEventHandlers {
  onConnected?: () => void
  onDisconnected?: () => void
  onReconnecting?: (attempt: number) => void
  onMessage?: (message: Message) => void
  onMessageAck?: (msgId: string, serverTimestamp: number, success: boolean) => void
  onTyping?: (conversationId: number, userId: number) => void
  onRecall?: (msgId: string) => void
  onReadSync?: (conversationId: number, lastReadMsgId: number) => void
  onKickedOffline?: (reason: string) => void
  onError?: (error: string) => void
}

class WebSocketService {
  private ws: WebSocket | null = null
  private url: string = ''
  private token: string = ''
  private deviceId: string = ''
  private deviceType: string = 'web'

  private status: ConnectionStatus = 'disconnected'
  private seqCounter: number = 0
  private pendingRequests: Map<string, { resolve: (data: unknown) => void; reject: (error: Error) => void; timeout: NodeJS.Timeout }> = new Map()

  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 10
  private reconnectDelay: number = 1000
  private maxReconnectDelay: number = 30000
  private reconnectTimer: NodeJS.Timeout | null = null

  private heartbeatInterval: NodeJS.Timeout | null = null
  private heartbeatIntervalMs: number = 30000

  private handlers: WebSocketEventHandlers = {}

  getStatus(): ConnectionStatus {
    return this.status
  }

  setHandlers(handlers: WebSocketEventHandlers): void {
    this.handlers = { ...this.handlers, ...handlers }
  }

  connect(url: string, token: string, deviceId: string, deviceType: string = 'web'): Promise<void> {
    this.url = url
    this.token = token
    this.deviceId = deviceId
    this.deviceType = deviceType

    return this.doConnect()
  }

  private doConnect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        resolve()
        return
      }

      this.status = this.reconnectAttempts > 0 ? 'reconnecting' : 'connecting'

      try {
        this.ws = new WebSocket(this.url)

        this.ws.onopen = () => {
          console.log('[WS] Connected to server')
          this.login()
            .then(() => {
              this.status = 'connected'
              this.reconnectAttempts = 0
              this.startHeartbeat()
              this.handlers.onConnected?.()
              resolve()
            })
            .catch((error) => {
              console.error('[WS] Login failed:', error)
              this.ws?.close()
              reject(error)
            })
        }

        this.ws.onclose = (event) => {
          console.log('[WS] Connection closed:', event.code, event.reason)
          this.handleDisconnect()
        }

        this.ws.onerror = (error) => {
          console.error('[WS] Error:', error)
          this.handlers.onError?.('WebSocket error')
        }

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data)
        }
      } catch (error) {
        this.status = 'disconnected'
        reject(error)
      }
    })
  }

  private async login(): Promise<void> {
    const data: LoginData = {
      token: this.token,
      deviceId: this.deviceId,
      deviceType: this.deviceType,
    }

    const response = await this.sendRequest<{ success: boolean; userId?: number; error?: string }>(
      ProtocolType.LOGIN,
      data
    )

    if (!response.success) {
      throw new Error(response.error || 'Login failed')
    }

    console.log('[WS] Logged in as user:', response.userId)
  }

  disconnect(): void {
    this.stopReconnect()
    this.stopHeartbeat()
    this.clearPendingRequests()

    if (this.ws) {
      // Send logout before closing
      if (this.ws.readyState === WebSocket.OPEN) {
        this.send(ProtocolType.LOGOUT, {})
      }
      this.ws.close()
      this.ws = null
    }

    this.status = 'disconnected'
  }

  private handleDisconnect(): void {
    this.stopHeartbeat()
    this.clearPendingRequests()
    this.status = 'disconnected'
    this.handlers.onDisconnected?.()

    // Attempt reconnection
    if (this.reconnectAttempts < this.maxReconnectAttempts && this.token) {
      this.scheduleReconnect()
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) return

    this.reconnectAttempts++
    const delay = Math.min(
      this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1),
      this.maxReconnectDelay
    )

    console.log(`[WS] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
    this.handlers.onReconnecting?.(this.reconnectAttempts)

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.doConnect().catch(() => {
        // Reconnect failed, will retry via handleDisconnect
      })
    }, delay)
  }

  private stopReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.reconnectAttempts = 0
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatInterval = setInterval(() => {
      this.sendHeartbeat()
    }, this.heartbeatIntervalMs)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  private sendHeartbeat(): void {
    if (this.ws?.readyState !== WebSocket.OPEN) return

    this.send(ProtocolType.HEARTBEAT, {})
      .catch(() => {
        console.warn('[WS] Heartbeat failed')
      })
  }

  private handleMessage(data: string): void {
    try {
      const packet: Packet = JSON.parse(data)

      // Handle response to pending request
      if (packet.seq && this.pendingRequests.has(packet.seq)) {
        const pending = this.pendingRequests.get(packet.seq)!
        clearTimeout(pending.timeout)
        this.pendingRequests.delete(packet.seq)
        pending.resolve(packet.data)
        return
      }

      // Handle server-initiated messages
      switch (packet.type) {
        case ProtocolType.RECEIVE_MESSAGE:
          this.handleReceiveMessage(packet.data as Message)
          break

        case ProtocolType.CHAT_MESSAGE_ACK:
          this.handleMessageAck(packet.data as { msgId: string; serverTimestamp: number; success: boolean })
          break

        case ProtocolType.TYPING_NOTIFY:
          this.handleTypingNotify(packet.data as { conversationId: number; userId: number })
          break

        case ProtocolType.RECALL_NOTIFY:
          this.handleRecallNotify(packet.data as { msgId: string })
          break

        case ProtocolType.KICKED_OFFLINE:
          this.handleKickedOffline(packet.data as { reason: string })
          break

        case ProtocolType.SERVER_ERROR:
          this.handlers.onError?.((packet.data as { error: string }).error)
          break

        default:
          console.log('[WS] Unhandled message type:', packet.type)
      }
    } catch (error) {
      console.error('[WS] Failed to parse message:', error)
    }
  }

  private handleReceiveMessage(data: Message): void {
    this.handlers.onMessage?.(data)
  }

  private handleMessageAck(data: { msgId: string; serverTimestamp: number; success: boolean }): void {
    this.handlers.onMessageAck?.(data.msgId, data.serverTimestamp, data.success)
  }

  private handleTypingNotify(data: { conversationId: number; userId: number }): void {
    this.handlers.onTyping?.(data.conversationId, data.userId)
  }

  private handleRecallNotify(data: { msgId: string }): void {
    this.handlers.onRecall?.(data.msgId)
  }

  private handleKickedOffline(data: { reason: string }): void {
    this.stopReconnect()
    this.disconnect()
    this.handlers.onKickedOffline?.(data.reason)
  }

  private generateSeq(): string {
    return `${Date.now()}_${++this.seqCounter}`
  }

  private send(type: number, data: unknown): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.ws?.readyState !== WebSocket.OPEN) {
        reject(new Error('WebSocket not connected'))
        return
      }

      const packet: Packet = {
        type,
        seq: this.generateSeq(),
        data,
        timestamp: Date.now(),
      }

      try {
        this.ws.send(JSON.stringify(packet))
        resolve()
      } catch (error) {
        reject(error)
      }
    })
  }

  private sendRequest<T>(type: number, data: unknown, timeout: number = 10000): Promise<T> {
    return new Promise((resolve, reject) => {
      if (this.ws?.readyState !== WebSocket.OPEN) {
        reject(new Error('WebSocket not connected'))
        return
      }

      const seq = this.generateSeq()
      const packet: Packet = {
        type,
        seq,
        data,
        timestamp: Date.now(),
      }

      const timeoutHandle = setTimeout(() => {
        this.pendingRequests.delete(seq)
        reject(new Error('Request timeout'))
      }, timeout)

      this.pendingRequests.set(seq, {
        resolve: resolve as (data: unknown) => void,
        reject,
        timeout: timeoutHandle,
      })

      try {
        this.ws.send(JSON.stringify(packet))
      } catch (error) {
        clearTimeout(timeoutHandle)
        this.pendingRequests.delete(seq)
        reject(error)
      }
    })
  }

  private clearPendingRequests(): void {
    for (const [, pending] of this.pendingRequests) {
      clearTimeout(pending.timeout)
      pending.reject(new Error('Connection closed'))
    }
    this.pendingRequests.clear()
  }

  // Public methods for sending messages

  sendChatMessage(data: ChatMessageData): Promise<void> {
    return this.send(ProtocolType.CHAT_MESSAGE, data)
  }

  sendTyping(conversationId: number): Promise<void> {
    return this.send(ProtocolType.TYPING, { conversationId })
  }

  sendReadAck(conversationId: number, lastReadMsgId: number): Promise<void> {
    return this.send(ProtocolType.READ_ACK, { conversationId, lastReadMsgId })
  }

  sendRecall(msgId: string): Promise<void> {
    return this.send(ProtocolType.RECALL_MESSAGE, { msgId })
  }

  requestSync(lastSyncCursor?: number): Promise<void> {
    return this.send(ProtocolType.SYNC_REQUEST, { lastSyncCursor })
  }

  updateToken(newToken: string): void {
    this.token = newToken
  }
}

// Singleton instance
export const websocketService = new WebSocketService()
