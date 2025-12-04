import { defineStore } from 'pinia'
import type { User, UserDevice, LoginRequest, LoginResponse } from '@/types'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import { useWebSocketStore } from './websocket'

interface UserState {
  user: User | null
  token: string | null
  refreshToken: string | null
  devices: UserDevice[]
  isLoggedIn: boolean
  loading: boolean
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    user: null,
    token: localStorage.getItem('token'),
    refreshToken: localStorage.getItem('refreshToken'),
    devices: [],
    isLoggedIn: false,
    loading: false,
  }),

  getters: {
    currentUser: (state) => state.user,
    isAuthenticated: (state) => !!state.token && state.isLoggedIn,
    userId: (state) => state.user?.id,
    userUid: (state) => state.user?.uid,
  },

  actions: {
    async login(credentials: { email: string; password: string; rememberMe?: boolean }) {
      this.loading = true
      try {
        // Generate or retrieve device ID
        let deviceId = localStorage.getItem('deviceId')
        if (!deviceId) {
          deviceId = `web_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
          localStorage.setItem('deviceId', deviceId)
        }

        const request: LoginRequest = {
          email: credentials.email,
          password: credentials.password,
          deviceId,
          deviceType: 'web',
          deviceName: navigator.userAgent.substring(0, 100),
          rememberMe: credentials.rememberMe,
        }

        const response = await authApi.login(request)
        this.setAuth(response)

        // Connect WebSocket after successful login
        const wsStore = useWebSocketStore()
        wsStore.connect().catch((error) => {
          console.warn('WebSocket connection failed:', error)
          // Don't fail login if WebSocket fails - user can still use REST APIs
        })

        return response
      } finally {
        this.loading = false
      }
    },

    async register(data: { email: string; password: string; nickname: string; gender: 'male' | 'female' }) {
      this.loading = true
      try {
        return await authApi.register(data)
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        // Disconnect WebSocket first
        const wsStore = useWebSocketStore()
        wsStore.disconnect()

        await authApi.logout()
      } finally {
        this.clearAuth()
      }
    },

    async logoutDevice(deviceId: string) {
      await userApi.logoutDevice(deviceId)
      this.devices = this.devices.filter(d => d.deviceId !== deviceId)
    },

    async logoutAllDevices() {
      await userApi.logoutAllDevices()
      this.clearAuth()
    },

    async fetchCurrentUser() {
      if (!this.token) return
      try {
        const user = await userApi.getCurrentUser()
        this.user = user
        this.isLoggedIn = true

        // Connect WebSocket if not already connected
        const wsStore = useWebSocketStore()
        if (wsStore.isDisconnected) {
          wsStore.connect().catch((error) => {
            console.warn('WebSocket connection failed:', error)
          })
        }
      } catch {
        this.clearAuth()
      }
    },

    async fetchDevices() {
      this.devices = await userApi.getDevices()
    },

    async updateProfile(data: Partial<User>) {
      const updated = await userApi.updateProfile(data)
      this.user = { ...this.user, ...updated } as User
    },

    async updateAvatar(file: File) {
      const avatarUrl = await userApi.uploadAvatar(file)
      if (this.user) {
        this.user.avatar = avatarUrl
      }
      return avatarUrl
    },

    setAuth(response: LoginResponse) {
      this.token = response.token
      this.refreshToken = response.refreshToken
      this.user = response.user
      this.isLoggedIn = true

      localStorage.setItem('token', response.token)
      localStorage.setItem('refreshToken', response.refreshToken)
    },

    clearAuth() {
      this.token = null
      this.refreshToken = null
      this.user = null
      this.isLoggedIn = false
      this.devices = []

      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
    },

    async refreshTokenIfNeeded() {
      if (!this.refreshToken) return false
      try {
        const response = await authApi.refreshToken(this.refreshToken)
        this.token = response.token
        this.refreshToken = response.refreshToken
        localStorage.setItem('token', response.token)
        localStorage.setItem('refreshToken', response.refreshToken)

        // Update WebSocket with new token
        const wsStore = useWebSocketStore()
        wsStore.updateToken(response.token)

        return true
      } catch {
        this.clearAuth()
        return false
      }
    },
  },
})
