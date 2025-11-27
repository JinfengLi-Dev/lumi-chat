import { apiClient } from './client'
import type { Message, MessageType, MessageMetadata, ApiResponse, PagedResponse } from '@/types'

export interface SendMessageRequest {
  conversationId: number
  msgType: MessageType
  content: string
  metadata?: MessageMetadata
  quoteMsgId?: string
  atUserIds?: number[]
}

export const messageApi = {
  async getMessages(conversationId: number, beforeMsgId?: number, limit = 50): Promise<PagedResponse<Message>> {
    const response = await apiClient.get<ApiResponse<PagedResponse<Message>>>(
      `/conversations/${conversationId}/messages`,
      {
        params: { beforeMsgId, limit },
      }
    )
    return response.data.data
  },

  async sendMessage(request: SendMessageRequest): Promise<Message> {
    const response = await apiClient.post<ApiResponse<Message>>('/messages', request)
    return response.data.data
  },

  async recallMessage(msgId: string): Promise<void> {
    await apiClient.post(`/messages/${msgId}/recall`)
  },

  async forwardMessage(msgId: string, targetConversationIds: number[]): Promise<void> {
    await apiClient.post(`/messages/${msgId}/forward`, { targetConversationIds })
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
