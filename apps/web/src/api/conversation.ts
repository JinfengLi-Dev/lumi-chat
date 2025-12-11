import { apiClient } from './client'
import type { Conversation, ApiResponse } from '@/types'

export const conversationApi = {
  async getConversations(): Promise<Conversation[]> {
    const response = await apiClient.get<ApiResponse<Conversation[]>>('/conversations')
    return response.data.data
  },

  async getConversation(id: number): Promise<Conversation> {
    const response = await apiClient.get<ApiResponse<Conversation>>(`/conversations/${id}`)
    return response.data.data
  },

  async createPrivateConversation(targetUserId: number): Promise<Conversation> {
    const response = await apiClient.post<ApiResponse<Conversation>>('/conversations/private', {
      targetUserId,
    })
    return response.data.data
  },

  async createStrangerConversation(targetUserId: number): Promise<Conversation> {
    const response = await apiClient.post<ApiResponse<Conversation>>('/conversations/stranger', {
      targetUserId,
    })
    return response.data.data
  },

  async deleteConversation(id: number): Promise<void> {
    await apiClient.delete(`/conversations/${id}`)
  },

  async markAsRead(id: number): Promise<void> {
    await apiClient.post(`/conversations/${id}/read`)
  },

  async toggleMute(id: number, muted: boolean): Promise<void> {
    await apiClient.put(`/conversations/${id}/mute`, { muted })
  },

  async togglePin(id: number, pinned: boolean): Promise<void> {
    await apiClient.put(`/conversations/${id}/pin`, { pinned })
  },

  async saveDraft(id: number, draft: string): Promise<void> {
    await apiClient.put(`/conversations/${id}/draft`, { draft })
  },

  async clearMessages(id: number): Promise<void> {
    await apiClient.delete(`/conversations/${id}/messages`)
  },
}
