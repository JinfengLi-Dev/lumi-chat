import { apiClient } from './client'
import type { ApiResponse } from '@/types'

export interface Device {
  id: number
  deviceId: string
  deviceType: 'web' | 'ios' | 'android' | 'pc'
  deviceName: string | null
  isOnline: boolean
  lastActiveAt: string
  createdAt: string
}

export const deviceApi = {
  async getDevices(): Promise<Device[]> {
    const response = await apiClient.get<ApiResponse<Device[]>>('/devices')
    return response.data.data
  },

  async logoutDevice(deviceId: string): Promise<void> {
    await apiClient.delete(`/devices/${deviceId}`)
  },

  async logoutAllDevices(): Promise<void> {
    await apiClient.delete('/devices')
  },
}
