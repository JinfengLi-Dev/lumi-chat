import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import type { User, UserDevice, LoginResponse } from '@/types'

// Mock the API modules
vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refreshToken: vi.fn(),
  },
}))

vi.mock('@/api/user', () => ({
  userApi: {
    getCurrentUser: vi.fn(),
    getDevices: vi.fn(),
    updateProfile: vi.fn(),
    uploadAvatar: vi.fn(),
    logoutDevice: vi.fn(),
    logoutAllDevices: vi.fn(),
  },
}))

// Mock the WebSocket store
vi.mock('@/stores/websocket', () => ({
  useWebSocketStore: vi.fn(() => ({
    connect: vi.fn().mockResolvedValue(undefined),
    disconnect: vi.fn(),
    isDisconnected: true,
    updateToken: vi.fn(),
  })),
}))

// Helper to create mock user
function createMockUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    uid: 'user123',
    email: 'test@example.com',
    nickname: 'Test User',
    gender: 'male',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

// Helper to create mock login response
function createMockLoginResponse(overrides: Partial<LoginResponse> = {}): LoginResponse {
  return {
    token: 'test-token',
    refreshToken: 'test-refresh-token',
    user: createMockUser(),
    ...overrides,
  }
}

// Helper to create mock device
function createMockDevice(overrides: Partial<UserDevice> = {}): UserDevice {
  return {
    id: 1,
    deviceId: 'device-1',
    deviceType: 'web',
    deviceName: 'Chrome on Windows',
    lastActiveAt: '2024-01-01T00:00:00Z',
    createdAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

describe('User Store', () => {
  let userStore: ReturnType<typeof useUserStore>

  beforeEach(() => {
    vi.clearAllMocks()
    // Clear localStorage mock
    localStorage.getItem = vi.fn().mockReturnValue(null)
    localStorage.setItem = vi.fn()
    localStorage.removeItem = vi.fn()

    // Create fresh pinia instance
    setActivePinia(createPinia())
    userStore = useUserStore()
  })

  describe('Initial State', () => {
    it('should have null user', () => {
      expect(userStore.user).toBeNull()
    })

    it('should have null tokens when localStorage empty', () => {
      expect(userStore.token).toBeNull()
      expect(userStore.refreshToken).toBeNull()
    })

    it('should have isLoggedIn as false', () => {
      expect(userStore.isLoggedIn).toBe(false)
    })

    it('should have empty devices array', () => {
      expect(userStore.devices).toEqual([])
    })

    it('should load token from localStorage on init', () => {
      localStorage.getItem = vi.fn((key) => {
        if (key === 'token') return 'stored-token'
        if (key === 'refreshToken') return 'stored-refresh'
        return null
      })

      // Need to recreate store to pick up localStorage values
      setActivePinia(createPinia())
      const store = useUserStore()

      expect(store.token).toBe('stored-token')
      expect(store.refreshToken).toBe('stored-refresh')
    })
  })

  describe('Getters', () => {
    it('currentUser should return user', () => {
      const user = createMockUser()
      userStore.user = user
      expect(userStore.currentUser).toEqual(user)
    })

    it('isAuthenticated should return false when not logged in', () => {
      expect(userStore.isAuthenticated).toBe(false)
    })

    it('isAuthenticated should return true when logged in with token', () => {
      userStore.token = 'test-token'
      userStore.isLoggedIn = true
      expect(userStore.isAuthenticated).toBe(true)
    })

    it('userId should return user id', () => {
      userStore.user = createMockUser({ id: 42 })
      expect(userStore.userId).toBe(42)
    })

    it('userUid should return user uid', () => {
      userStore.user = createMockUser({ uid: 'abc123' })
      expect(userStore.userUid).toBe('abc123')
    })
  })

  describe('login', () => {
    it('should login successfully and store tokens', async () => {
      const mockResponse = createMockLoginResponse()
      vi.mocked(authApi.login).mockResolvedValue(mockResponse)

      await userStore.login({ email: 'test@example.com', password: 'password' })

      expect(authApi.login).toHaveBeenCalled()
      expect(userStore.token).toBe('test-token')
      expect(userStore.refreshToken).toBe('test-refresh-token')
      expect(userStore.user).toEqual(mockResponse.user)
      expect(userStore.isLoggedIn).toBe(true)
      expect(localStorage.setItem).toHaveBeenCalledWith('token', 'test-token')
      expect(localStorage.setItem).toHaveBeenCalledWith('refreshToken', 'test-refresh-token')
    })

    it('should set loading during login', async () => {
      vi.mocked(authApi.login).mockImplementation(async () => {
        expect(userStore.loading).toBe(true)
        return createMockLoginResponse()
      })

      await userStore.login({ email: 'test@example.com', password: 'password' })
      expect(userStore.loading).toBe(false)
    })

    it('should generate device ID if not exists', async () => {
      vi.mocked(authApi.login).mockResolvedValue(createMockLoginResponse())

      await userStore.login({ email: 'test@example.com', password: 'password' })

      expect(localStorage.setItem).toHaveBeenCalledWith('deviceId', expect.stringMatching(/^web_\d+_/))
    })

    it('should use existing device ID', async () => {
      localStorage.getItem = vi.fn((key) => {
        if (key === 'deviceId') return 'existing-device-id'
        return null
      })
      vi.mocked(authApi.login).mockResolvedValue(createMockLoginResponse())

      await userStore.login({ email: 'test@example.com', password: 'password' })

      expect(authApi.login).toHaveBeenCalledWith(expect.objectContaining({
        deviceId: 'existing-device-id',
      }))
    })

    it('should reset loading on error', async () => {
      vi.mocked(authApi.login).mockRejectedValue(new Error('Login failed'))

      await expect(userStore.login({ email: 'test@example.com', password: 'password' }))
        .rejects.toThrow('Login failed')
      expect(userStore.loading).toBe(false)
    })
  })

  describe('register', () => {
    it('should register successfully', async () => {
      const mockUser = createMockUser()
      vi.mocked(authApi.register).mockResolvedValue(mockUser)

      const result = await userStore.register({
        email: 'test@example.com',
        password: 'password',
        nickname: 'Test',
        gender: 'male',
      })

      expect(authApi.register).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password',
        nickname: 'Test',
        gender: 'male',
      })
      expect(result).toEqual(mockUser)
    })

    it('should set loading during registration', async () => {
      vi.mocked(authApi.register).mockImplementation(async () => {
        expect(userStore.loading).toBe(true)
        return createMockUser()
      })

      await userStore.register({
        email: 'test@example.com',
        password: 'password',
        nickname: 'Test',
        gender: 'male',
      })
      expect(userStore.loading).toBe(false)
    })
  })

  describe('logout', () => {
    it('should logout and clear auth', async () => {
      userStore.token = 'test-token'
      userStore.user = createMockUser()
      userStore.isLoggedIn = true
      vi.mocked(authApi.logout).mockResolvedValue(undefined)

      await userStore.logout()

      expect(authApi.logout).toHaveBeenCalled()
      expect(userStore.token).toBeNull()
      expect(userStore.refreshToken).toBeNull()
      expect(userStore.user).toBeNull()
      expect(userStore.isLoggedIn).toBe(false)
      expect(localStorage.removeItem).toHaveBeenCalledWith('token')
      expect(localStorage.removeItem).toHaveBeenCalledWith('refreshToken')
    })

    it('should clear auth even if logout API fails', async () => {
      userStore.token = 'test-token'
      userStore.isLoggedIn = true
      vi.mocked(authApi.logout).mockRejectedValue(new Error('Network error'))

      // The logout throws but still clears auth in finally block
      await expect(userStore.logout()).rejects.toThrow('Network error')

      expect(userStore.token).toBeNull()
      expect(userStore.isLoggedIn).toBe(false)
    })
  })

  describe('logoutDevice', () => {
    it('should logout device and remove from list', async () => {
      userStore.devices = [
        createMockDevice({ deviceId: 'device-1' }),
        createMockDevice({ deviceId: 'device-2' }),
      ]
      vi.mocked(userApi.logoutDevice).mockResolvedValue(undefined)

      await userStore.logoutDevice('device-1')

      expect(userApi.logoutDevice).toHaveBeenCalledWith('device-1')
      expect(userStore.devices).toHaveLength(1)
      expect(userStore.devices[0].deviceId).toBe('device-2')
    })
  })

  describe('logoutAllDevices', () => {
    it('should logout all devices and clear auth', async () => {
      userStore.token = 'test-token'
      userStore.isLoggedIn = true
      userStore.devices = [createMockDevice()]
      vi.mocked(userApi.logoutAllDevices).mockResolvedValue(undefined)

      await userStore.logoutAllDevices()

      expect(userApi.logoutAllDevices).toHaveBeenCalled()
      expect(userStore.token).toBeNull()
      expect(userStore.isLoggedIn).toBe(false)
      expect(userStore.devices).toEqual([])
    })
  })

  describe('fetchCurrentUser', () => {
    it('should fetch and store current user', async () => {
      userStore.token = 'test-token'
      const mockUser = createMockUser()
      vi.mocked(userApi.getCurrentUser).mockResolvedValue(mockUser)

      await userStore.fetchCurrentUser()

      expect(userApi.getCurrentUser).toHaveBeenCalled()
      expect(userStore.user).toEqual(mockUser)
      expect(userStore.isLoggedIn).toBe(true)
    })

    it('should not fetch if no token', async () => {
      userStore.token = null

      await userStore.fetchCurrentUser()

      expect(userApi.getCurrentUser).not.toHaveBeenCalled()
    })

    it('should clear auth on error', async () => {
      userStore.token = 'test-token'
      userStore.isLoggedIn = true
      vi.mocked(userApi.getCurrentUser).mockRejectedValue(new Error('Unauthorized'))

      await userStore.fetchCurrentUser()

      expect(userStore.token).toBeNull()
      expect(userStore.isLoggedIn).toBe(false)
    })
  })

  describe('fetchDevices', () => {
    it('should fetch and store devices', async () => {
      const mockDevices = [
        createMockDevice({ deviceId: 'device-1' }),
        createMockDevice({ deviceId: 'device-2' }),
      ]
      vi.mocked(userApi.getDevices).mockResolvedValue(mockDevices)

      await userStore.fetchDevices()

      expect(userApi.getDevices).toHaveBeenCalled()
      expect(userStore.devices).toEqual(mockDevices)
    })
  })

  describe('updateProfile', () => {
    it('should update profile and merge with existing user', async () => {
      userStore.user = createMockUser({ nickname: 'Old Name' })
      const updatedData = { nickname: 'New Name', signature: 'Hello' }
      vi.mocked(userApi.updateProfile).mockResolvedValue(updatedData as User)

      await userStore.updateProfile(updatedData)

      expect(userApi.updateProfile).toHaveBeenCalledWith(updatedData)
      expect(userStore.user?.nickname).toBe('New Name')
    })
  })

  describe('updateAvatar', () => {
    it('should upload avatar and update user', async () => {
      userStore.user = createMockUser()
      const mockFile = new File([''], 'avatar.jpg', { type: 'image/jpeg' })
      vi.mocked(userApi.uploadAvatar).mockResolvedValue('https://example.com/avatar.jpg')

      const result = await userStore.updateAvatar(mockFile)

      expect(userApi.uploadAvatar).toHaveBeenCalledWith(mockFile)
      expect(result).toBe('https://example.com/avatar.jpg')
      expect(userStore.user?.avatar).toBe('https://example.com/avatar.jpg')
    })

    it('should not update if no user', async () => {
      userStore.user = null
      const mockFile = new File([''], 'avatar.jpg', { type: 'image/jpeg' })
      vi.mocked(userApi.uploadAvatar).mockResolvedValue('https://example.com/avatar.jpg')

      await userStore.updateAvatar(mockFile)

      // Should not throw, just not update
      expect(userStore.user).toBeNull()
    })
  })

  describe('setAuth', () => {
    it('should set auth data and store in localStorage', () => {
      const response = createMockLoginResponse()

      userStore.setAuth(response)

      expect(userStore.token).toBe('test-token')
      expect(userStore.refreshToken).toBe('test-refresh-token')
      expect(userStore.user).toEqual(response.user)
      expect(userStore.isLoggedIn).toBe(true)
      expect(localStorage.setItem).toHaveBeenCalledWith('token', 'test-token')
      expect(localStorage.setItem).toHaveBeenCalledWith('refreshToken', 'test-refresh-token')
    })
  })

  describe('clearAuth', () => {
    it('should clear all auth data', () => {
      userStore.token = 'test-token'
      userStore.refreshToken = 'test-refresh'
      userStore.user = createMockUser()
      userStore.isLoggedIn = true
      userStore.devices = [createMockDevice()]

      userStore.clearAuth()

      expect(userStore.token).toBeNull()
      expect(userStore.refreshToken).toBeNull()
      expect(userStore.user).toBeNull()
      expect(userStore.isLoggedIn).toBe(false)
      expect(userStore.devices).toEqual([])
      expect(localStorage.removeItem).toHaveBeenCalledWith('token')
      expect(localStorage.removeItem).toHaveBeenCalledWith('refreshToken')
    })
  })

  describe('refreshTokenIfNeeded', () => {
    it('should refresh token successfully', async () => {
      userStore.refreshToken = 'old-refresh-token'
      const newResponse = {
        token: 'new-token',
        refreshToken: 'new-refresh-token',
      }
      vi.mocked(authApi.refreshToken).mockResolvedValue(newResponse)

      const result = await userStore.refreshTokenIfNeeded()

      expect(result).toBe(true)
      expect(authApi.refreshToken).toHaveBeenCalledWith('old-refresh-token')
      expect(userStore.token).toBe('new-token')
      expect(userStore.refreshToken).toBe('new-refresh-token')
      expect(localStorage.setItem).toHaveBeenCalledWith('token', 'new-token')
      expect(localStorage.setItem).toHaveBeenCalledWith('refreshToken', 'new-refresh-token')
    })

    it('should return false if no refresh token', async () => {
      userStore.refreshToken = null

      const result = await userStore.refreshTokenIfNeeded()

      expect(result).toBe(false)
      expect(authApi.refreshToken).not.toHaveBeenCalled()
    })

    it('should clear auth on refresh failure', async () => {
      userStore.refreshToken = 'old-refresh-token'
      userStore.token = 'old-token'
      userStore.isLoggedIn = true
      vi.mocked(authApi.refreshToken).mockRejectedValue(new Error('Refresh failed'))

      const result = await userStore.refreshTokenIfNeeded()

      expect(result).toBe(false)
      expect(userStore.token).toBeNull()
      expect(userStore.isLoggedIn).toBe(false)
    })
  })
})
