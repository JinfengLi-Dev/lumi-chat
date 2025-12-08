import { apiClient, getDeviceInfo } from './client'
import type { LoginRequest, LoginResponse, RegisterRequest, ApiResponse } from '@/types'

export const authApi = {
  async login(request: Omit<LoginRequest, 'deviceId' | 'deviceType' | 'deviceName'>): Promise<LoginResponse> {
    const deviceInfo = getDeviceInfo()
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', {
      ...request,
      ...deviceInfo,
    })
    return response.data.data
  },

  async register(request: RegisterRequest): Promise<void> {
    await apiClient.post('/auth/register', request)
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/logout')
  },

  async refreshToken(refreshToken: string): Promise<{ token: string; refreshToken: string }> {
    const response = await apiClient.post<ApiResponse<{ token: string; refreshToken: string }>>('/auth/refresh', {
      refreshToken,
    })
    return response.data.data
  },

  async forgotPassword(email: string): Promise<void> {
    await apiClient.post('/auth/forgot-password', { email })
  },

  async resetPassword(token: string, newPassword: string): Promise<void> {
    await apiClient.post('/auth/reset-password', { token, newPassword })
  },

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await apiClient.put('/users/me/password', { currentPassword, newPassword })
  },
}
