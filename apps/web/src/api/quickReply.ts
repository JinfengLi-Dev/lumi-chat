import { apiClient } from './client'

export interface QuickReply {
  id: number
  content: string
  sortOrder: number
  createdAt: string
}

export interface QuickReplyRequest {
  content: string
}

export interface ReorderRequest {
  ids: number[]
}

export const quickReplyApi = {
  async getAll(): Promise<QuickReply[]> {
    const response = await apiClient.get<QuickReply[]>('/quick-replies')
    return response.data
  },

  async create(request: QuickReplyRequest): Promise<QuickReply> {
    const response = await apiClient.post<QuickReply>('/quick-replies', request)
    return response.data
  },

  async update(id: number, request: QuickReplyRequest): Promise<QuickReply> {
    const response = await apiClient.put<QuickReply>(`/quick-replies/${id}`, request)
    return response.data
  },

  async delete(id: number): Promise<void> {
    await apiClient.delete(`/quick-replies/${id}`)
  },

  async reorder(ids: number[]): Promise<QuickReply[]> {
    const response = await apiClient.put<QuickReply[]>('/quick-replies/reorder', { ids })
    return response.data
  },
}
