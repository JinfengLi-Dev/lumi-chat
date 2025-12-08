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

export const messageApi = {
  /**
   * Get messages for a conversation
   * Backend returns List<MessageResponse>, not PagedResponse
   * @param before - The ID of the message to load before (for pagination)
   */
  async getMessages(conversationId: number, before?: number, limit = 50): Promise<Message[]> {
    const response = await apiClient.get<ApiResponse<Message[]>>(
      `/conversations/${conversationId}/messages`,
      {
        params: { before, limit },
      }
    )
    return response.data.data
  },

  async sendMessage(request: SendMessageRequest): Promise<Message> {
    const response = await apiClient.post<ApiResponse<Message>>('/messages', request)
    return response.data.data
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
