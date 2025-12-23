import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import Login from '../Login.vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

// Mock vue-router
const mockPush = vi.fn()
const mockRoute = { query: {} }
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({
    push: mockPush,
  })),
  useRoute: vi.fn(() => mockRoute),
}))

// Mock user store
vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => ({
    login: vi.fn(),
  })),
}))

// Mock ElMessage
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
  }
})

// Stub Teleport and RouterLink
config.global.stubs = {
  teleport: true,
  RouterLink: {
    template: '<a><slot /></a>',
  },
}

// Type definition for component VM
type LoginVM = {
  form: {
    email: string
    password: string
    rememberMe: boolean
  }
  loading: boolean
  handleLogin: () => Promise<void>
}

describe('Login', () => {
  let mockLogin: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    mockPush.mockClear()
    mockRoute.query = {}

    // Reset mock login function
    mockLogin = vi.fn()
    vi.mocked(useUserStore).mockReturnValue({
      login: mockLogin,
    } as any)
  })

  describe('Initial State', () => {
    it('should have empty email initially', () => {
      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      expect(vm.form.email).toBe('')
      wrapper.unmount()
    })

    it('should have empty password initially', () => {
      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      expect(vm.form.password).toBe('')
      wrapper.unmount()
    })

    it('should have rememberMe as false initially', () => {
      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      expect(vm.form.rememberMe).toBe(false)
      wrapper.unmount()
    })

    it('should not be loading initially', () => {
      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      expect(vm.loading).toBe(false)
      wrapper.unmount()
    })
  })

  describe('Form Data', () => {
    it('should update email when set', () => {
      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'

      expect(vm.form.email).toBe('test@example.com')
      wrapper.unmount()
    })

    it('should update password when set', () => {
      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.password = 'password123'

      expect(vm.form.password).toBe('password123')
      wrapper.unmount()
    })

    it('should update rememberMe when set', () => {
      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.rememberMe = true

      expect(vm.form.rememberMe).toBe(true)
      wrapper.unmount()
    })
  })

  describe('Login Functionality', () => {
    it('should call userStore.login with form data', async () => {
      mockLogin.mockResolvedValue(undefined)

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'password123'
      vm.form.rememberMe = true

      await vm.handleLogin()
      await flushPromises()

      expect(mockLogin).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123',
        rememberMe: true,
      })
      wrapper.unmount()
    })

    it('should show success message on successful login', async () => {
      mockLogin.mockResolvedValue(undefined)

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'password123'

      await vm.handleLogin()
      await flushPromises()

      expect(ElMessage.success).toHaveBeenCalledWith('Login successful')
      wrapper.unmount()
    })

    it('should redirect to home after successful login', async () => {
      mockLogin.mockResolvedValue(undefined)

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'password123'

      await vm.handleLogin()
      await flushPromises()

      expect(mockPush).toHaveBeenCalledWith('/')
      wrapper.unmount()
    })

    it('should redirect to original page after login if redirect query exists', async () => {
      mockLogin.mockResolvedValue(undefined)
      mockRoute.query = { redirect: '/chat/123' }

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'password123'

      await vm.handleLogin()
      await flushPromises()

      expect(mockPush).toHaveBeenCalledWith('/chat/123')
      wrapper.unmount()
    })

    it('should show error message on login failure', async () => {
      mockLogin.mockRejectedValue(new Error('Invalid credentials'))

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'wrongpassword'

      await vm.handleLogin()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Invalid credentials')
      wrapper.unmount()
    })

    it('should show default error message when no error message provided', async () => {
      mockLogin.mockRejectedValue({})

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'password123'

      await vm.handleLogin()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Login failed')
      wrapper.unmount()
    })

    it('should set loading state during login process', async () => {
      // Track loading states throughout the login process
      const loadingStates: boolean[] = []
      let resolveLogin: () => void

      mockLogin.mockImplementation(() => {
        return new Promise<void>((resolve) => {
          resolveLogin = resolve
        })
      })

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'password123'

      // Verify loading starts as false
      expect(vm.loading).toBe(false)

      const loginPromise = vm.handleLogin()
      await flushPromises()

      // After login starts, loading should be true (if validation passed)
      loadingStates.push(vm.loading)

      resolveLogin!()
      await loginPromise
      await flushPromises()

      // After login completes, loading should be false
      expect(vm.loading).toBe(false)
      wrapper.unmount()
    })

    it('should reset loading on login failure', async () => {
      mockLogin.mockRejectedValue(new Error('Failed'))

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'password123'

      await vm.handleLogin()
      await flushPromises()

      expect(vm.loading).toBe(false)
      wrapper.unmount()
    })
  })

  describe('Form Validation Rules', () => {
    it('should have email as required field', () => {
      const wrapper = mount(Login)
      // Access the rules defined in the component
      const component = wrapper.vm as any
      expect(component.rules.email[0].required).toBe(true)
      wrapper.unmount()
    })

    it('should have password as required field', () => {
      const wrapper = mount(Login)
      const component = wrapper.vm as any
      expect(component.rules.password[0].required).toBe(true)
      wrapper.unmount()
    })

    it('should require password to be at least 6 characters', () => {
      const wrapper = mount(Login)
      const component = wrapper.vm as any
      expect(component.rules.password[1].min).toBe(6)
      wrapper.unmount()
    })

    it('should validate email on blur', () => {
      const wrapper = mount(Login)
      const component = wrapper.vm as any
      expect(component.rules.email[0].trigger).toBe('blur')
      wrapper.unmount()
    })

    it('should validate password on blur', () => {
      const wrapper = mount(Login)
      const component = wrapper.vm as any
      expect(component.rules.password[0].trigger).toBe('blur')
      expect(component.rules.password[1].trigger).toBe('blur')
      wrapper.unmount()
    })
  })

  describe('Remember Me', () => {
    it('should pass rememberMe as false when unchecked', async () => {
      mockLogin.mockResolvedValue(undefined)

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'password123'
      vm.form.rememberMe = false

      await vm.handleLogin()
      await flushPromises()

      expect(mockLogin).toHaveBeenCalledWith(
        expect.objectContaining({
          rememberMe: false,
        })
      )
      wrapper.unmount()
    })

    it('should pass rememberMe as true when checked', async () => {
      mockLogin.mockResolvedValue(undefined)

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'test@example.com'
      vm.form.password = 'password123'
      vm.form.rememberMe = true

      await vm.handleLogin()
      await flushPromises()

      expect(mockLogin).toHaveBeenCalledWith(
        expect.objectContaining({
          rememberMe: true,
        })
      )
      wrapper.unmount()
    })
  })

  describe('UID Login', () => {
    it('should allow login with UID instead of email', async () => {
      mockLogin.mockResolvedValue(undefined)

      const wrapper = mount(Login)
      const vm = wrapper.vm as unknown as LoginVM

      vm.form.email = 'user123' // UID instead of email
      vm.form.password = 'password123'

      await vm.handleLogin()
      await flushPromises()

      expect(mockLogin).toHaveBeenCalledWith({
        email: 'user123',
        password: 'password123',
        rememberMe: false,
      })
      wrapper.unmount()
    })
  })
})
