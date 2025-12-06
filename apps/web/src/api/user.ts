import { apiClient } from './client'
import type { User, UserDevice, ApiResponse } from '@/types'

export const userApi = {
  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>('/users/me')
    return response.data.data
  },

  async updateProfile(data: Partial<User>): Promise<User> {
    const response = await apiClient.put<ApiResponse<User>>('/users/me', data)
    return response.data.data
  },

  async uploadAvatar(file: File): Promise<string> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await apiClient.post<ApiResponse<{ url: string }>>('/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data.data.url
  },

  async getDevices(): Promise<UserDevice[]> {
    const response = await apiClient.get<ApiResponse<UserDevice[]>>('/devices')
    return response.data.data
  },

  async logoutDevice(deviceId: string): Promise<void> {
    await apiClient.delete(`/devices/${deviceId}`)
  },

  async logoutAllDevices(): Promise<void> {
    await apiClient.delete('/devices')
  },

  async searchUsers(query: string): Promise<User[]> {
    const response = await apiClient.get<ApiResponse<User[]>>('/users/search', {
      params: { q: query },
    })
    return response.data.data
  },

  async getUserByUid(uid: string): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>(`/users/${uid}`)
    return response.data.data
  },
}
