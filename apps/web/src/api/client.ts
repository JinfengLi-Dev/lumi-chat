import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'
import router from '@/router'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

// Create axios instance
// NOTE: Don't set default Content-Type here - it must be set dynamically
// to allow FormData uploads to work correctly (axios auto-sets multipart/form-data with boundary)
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
})

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // Add device ID header
    const deviceId = getOrCreateDeviceId()
    config.headers['X-Device-Id'] = deviceId

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor - handle errors
apiClient.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResponse<unknown>
    if (data.code !== 0 && data.code !== 200) {
      return Promise.reject(new Error(data.message || 'Request failed'))
    }
    return response
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config

    // Handle 401 - try to refresh token
    if (error.response?.status === 401 && originalRequest) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          })
          const { token, refreshToken: newRefreshToken } = response.data.data
          localStorage.setItem('token', token)
          localStorage.setItem('refreshToken', newRefreshToken)

          // Retry original request
          originalRequest.headers.Authorization = `Bearer ${token}`
          return apiClient(originalRequest)
        } catch {
          // Refresh failed, clear auth
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
          router.push('/login')
        }
      } else {
        router.push('/login')
      }
    }

    const message = error.response?.data?.message || error.message || 'Network error'
    return Promise.reject(new Error(message))
  }
)

// Generate a cryptographically secure device ID
function generateSecureDeviceId(): string {
  const array = new Uint8Array(16)
  crypto.getRandomValues(array)
  const hex = Array.from(array, b => b.toString(16).padStart(2, '0')).join('')
  return `web_${Date.now()}_${hex}`
}

// Helper to get or create device ID
function getOrCreateDeviceId(): string {
  let deviceId = localStorage.getItem('deviceId')
  if (!deviceId) {
    deviceId = generateSecureDeviceId()
    localStorage.setItem('deviceId', deviceId)
  }
  return deviceId
}

export function getDeviceId(): string {
  return getOrCreateDeviceId()
}

export function getDeviceInfo(): { deviceId: string; deviceType: string; deviceName: string } {
  return {
    deviceId: getDeviceId(),
    deviceType: 'web',
    deviceName: `${navigator.userAgent.split(' ')[0]} on ${navigator.platform}`,
  }
}
