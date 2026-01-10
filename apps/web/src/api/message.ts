import { apiClient } from './client'
import type { Message, MessageType, MessageMetadata, ApiResponse } from '@/types'

export interface SendMessageRequest {
  conversationId: number
  msgType: MessageType
  content: string
  metadata?: MessageMetadata
  quoteMsgId?: string
  atUserIds?: number[]
}

// Backend returns metadata as JSON string, so we need to parse it
interface RawMessage extends Omit<Message, 'metadata'> {
  metadata?: string | MessageMetadata
}

function parseMessageMetadata(raw: RawMessage): Message {
  const message = raw as Message
  if (typeof raw.metadata === 'string' && raw.metadata) {
    try {
      message.metadata = JSON.parse(raw.metadata)
    } catch {
      message.metadata = undefined
    }
  }
  return message
}

export const messageApi = {
  /**
   * Get messages for a conversation
   * Backend returns List<MessageResponse>, not PagedResponse
   * @param before - The ID of the message to load before (for pagination)
   */
  async getMessages(conversationId: number, before?: number, limit = 50): Promise<Message[]> {
    const response = await apiClient.get<ApiResponse<RawMessage[]>>(
      `/conversations/${conversationId}/messages`,
      {
        params: { before, limit },
      }
    )
    return response.data.data.map(parseMessageMetadata)
  },

  async sendMessage(request: SendMessageRequest): Promise<Message> {
    // Backend expects metadata as JSON string, not object
    const payload = {
      ...request,
      metadata: request.metadata ? JSON.stringify(request.metadata) : undefined,
    }
    const response = await apiClient.post<ApiResponse<RawMessage>>('/messages', payload)
    return parseMessageMetadata(response.data.data)
  },

  async recallMessage(msgId: string): Promise<void> {
    await apiClient.put(`/messages/${msgId}/recall`)
  },

  async forwardMessage(msgId: string, targetConversationId: number): Promise<void> {
    await apiClient.post(`/messages/${msgId}/forward`, { targetConversationId })
  },

  async deleteMessage(msgId: string): Promise<void> {
    await apiClient.delete(`/messages/${msgId}`)
  },

  /**
   * Search messages in a conversation
   * GET /conversations/{id}/messages/search?q=xxx
   */
  async searchMessages(conversationId: number, query: string, page = 0, limit = 20): Promise<Message[]> {
    const response = await apiClient.get<ApiResponse<RawMessage[]>>(
      `/conversations/${conversationId}/messages/search`,
      {
        params: { q: query, page, limit },
      }
    )
    return response.data.data.map(parseMessageMetadata)
  },

  /**
   * Get media messages (images, videos, files) for a conversation
   * GET /conversations/{id}/media?type=image
   */
  async getMediaMessages(
    conversationId: number,
    type: 'image' | 'video' | 'file' | 'voice' | 'all' = 'image',
    page = 0,
    limit = 20
  ): Promise<Message[]> {
    const response = await apiClient.get<ApiResponse<RawMessage[]>>(
      `/conversations/${conversationId}/media`,
      {
        params: { type, page, limit },
      }
    )
    return response.data.data.map(parseMessageMetadata)
  },

  async uploadFile(file: File, onProgress?: (percent: number) => void): Promise<{ fileId: string; url: string }> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post<ApiResponse<{ fileId: string; url: string }>>('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (event) => {
        if (event.total && onProgress) {
          onProgress(Math.round((event.loaded * 100) / event.total))
        }
      },
    })
    return response.data.data
  },
}
