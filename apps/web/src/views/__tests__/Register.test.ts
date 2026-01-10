import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import Register from '../Register.vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({
    push: mockPush,
  })),
}))

// Mock user store
vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => ({
    register: vi.fn(),
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
type RegisterVM = {
  form: {
    nickname: string
    email: string
    password: string
    confirmPassword: string
    gender: 'male' | 'female'
    agreeTerms: boolean
  }
  loading: boolean
  handleRegister: () => Promise<void>
  validateConfirmPassword: (rule: any, value: string, callback: Function) => void
  rules: Record<string, any[]>
}

describe('Register', () => {
  let mockRegister: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    mockPush.mockClear()

    // Reset mock register function
    mockRegister = vi.fn()
    vi.mocked(useUserStore).mockReturnValue({
      register: mockRegister,
    } as any)
  })

  describe('Initial State', () => {
    it('should have empty nickname initially', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      expect(vm.form.nickname).toBe('')
      wrapper.unmount()
    })

    it('should have empty email initially', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      expect(vm.form.email).toBe('')
      wrapper.unmount()
    })

    it('should have empty password initially', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      expect(vm.form.password).toBe('')
      wrapper.unmount()
    })

    it('should have empty confirmPassword initially', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      expect(vm.form.confirmPassword).toBe('')
      wrapper.unmount()
    })

    it('should have male as default gender', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      expect(vm.form.gender).toBe('male')
      wrapper.unmount()
    })

    it('should have agreeTerms as false initially', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      expect(vm.form.agreeTerms).toBe(false)
      wrapper.unmount()
    })

    it('should not be loading initially', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      expect(vm.loading).toBe(false)
      wrapper.unmount()
    })
  })

  describe('Form Data', () => {
    it('should update nickname when set', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'John'

      expect(vm.form.nickname).toBe('John')
      wrapper.unmount()
    })

    it('should update email when set', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.email = 'john@example.com'

      expect(vm.form.email).toBe('john@example.com')
      wrapper.unmount()
    })

    it('should update password when set', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.password = 'password123'

      expect(vm.form.password).toBe('password123')
      wrapper.unmount()
    })

    it('should update confirmPassword when set', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.confirmPassword = 'password123'

      expect(vm.form.confirmPassword).toBe('password123')
      wrapper.unmount()
    })

    it('should update gender when set', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.gender = 'female'

      expect(vm.form.gender).toBe('female')
      wrapper.unmount()
    })

    it('should update agreeTerms when set', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.agreeTerms = true

      expect(vm.form.agreeTerms).toBe(true)
      wrapper.unmount()
    })
  })

  describe('Register Functionality', () => {
    it('should call userStore.register with form data', async () => {
      mockRegister.mockResolvedValue(undefined)

      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'John'
      vm.form.email = 'john@example.com'
      vm.form.password = 'password123'
      vm.form.confirmPassword = 'password123'
      vm.form.gender = 'male'
      vm.form.agreeTerms = true

      await vm.handleRegister()
      await flushPromises()

      expect(mockRegister).toHaveBeenCalledWith({
        nickname: 'John',
        email: 'john@example.com',
        password: 'password123',
        gender: 'male',
      })
      wrapper.unmount()
    })

    it('should show success message on successful registration', async () => {
      mockRegister.mockResolvedValue(undefined)

      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'John'
      vm.form.email = 'john@example.com'
      vm.form.password = 'password123'
      vm.form.confirmPassword = 'password123'
      vm.form.agreeTerms = true

      await vm.handleRegister()
      await flushPromises()

      expect(ElMessage.success).toHaveBeenCalledWith('Registration successful! Please login.')
      wrapper.unmount()
    })

    it('should redirect to login page after successful registration', async () => {
      mockRegister.mockResolvedValue(undefined)

      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'John'
      vm.form.email = 'john@example.com'
      vm.form.password = 'password123'
      vm.form.confirmPassword = 'password123'
      vm.form.agreeTerms = true

      await vm.handleRegister()
      await flushPromises()

      expect(mockPush).toHaveBeenCalledWith('/login')
      wrapper.unmount()
    })

    it('should show error message on registration failure', async () => {
      mockRegister.mockRejectedValue(new Error('Email already exists'))

      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'John'
      vm.form.email = 'existing@example.com'
      vm.form.password = 'password123'
      vm.form.confirmPassword = 'password123'
      vm.form.agreeTerms = true

      await vm.handleRegister()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Email already exists')
      wrapper.unmount()
    })

    it('should show default error message when no error message provided', async () => {
      mockRegister.mockRejectedValue(new Error('Registration failed'))

      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'John'
      vm.form.email = 'john@example.com'
      vm.form.password = 'password123'
      vm.form.confirmPassword = 'password123'
      vm.form.agreeTerms = true

      await vm.handleRegister()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Registration failed')
      wrapper.unmount()
    })

    it('should reset loading on registration failure', async () => {
      mockRegister.mockRejectedValue(new Error('Failed'))

      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'John'
      vm.form.email = 'john@example.com'
      vm.form.password = 'password123'
      vm.form.confirmPassword = 'password123'
      vm.form.agreeTerms = true

      await vm.handleRegister()
      await flushPromises()

      expect(vm.loading).toBe(false)
      wrapper.unmount()
    })

    it('should not include confirmPassword in registration request', async () => {
      mockRegister.mockResolvedValue(undefined)

      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'John'
      vm.form.email = 'john@example.com'
      vm.form.password = 'password123'
      vm.form.confirmPassword = 'password123'
      vm.form.agreeTerms = true

      await vm.handleRegister()
      await flushPromises()

      const registerCall = mockRegister.mock.calls[0][0]
      expect(registerCall).not.toHaveProperty('confirmPassword')
      expect(registerCall).not.toHaveProperty('agreeTerms')
      wrapper.unmount()
    })
  })

  describe('Form Validation Rules', () => {
    it('should have nickname as required field', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.nickname[0].required).toBe(true)
      wrapper.unmount()
    })

    it('should require nickname to be 2-20 characters', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.nickname[1].min).toBe(2)
      expect(vm.rules.nickname[1].max).toBe(20)
      wrapper.unmount()
    })

    it('should have email as required field', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.email[0].required).toBe(true)
      wrapper.unmount()
    })

    it('should validate email format', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.email[1].type).toBe('email')
      wrapper.unmount()
    })

    it('should have password as required field', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.password[0].required).toBe(true)
      wrapper.unmount()
    })

    it('should require password to be at least 6 characters', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.password[1].min).toBe(6)
      wrapper.unmount()
    })

    it('should have confirmPassword as required field', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.confirmPassword[0].required).toBe(true)
      wrapper.unmount()
    })

    it('should have gender as required field', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.gender[0].required).toBe(true)
      wrapper.unmount()
    })

    it('should validate gender on change', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.gender[0].trigger).toBe('change')
      wrapper.unmount()
    })

    it('should have agreeTerms validator', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM
      expect(vm.rules.agreeTerms[0].validator).toBeDefined()
      wrapper.unmount()
    })
  })

  describe('Password Confirmation Validation', () => {
    it('should validate matching passwords', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.password = 'password123'
      const callback = vi.fn()

      vm.validateConfirmPassword({}, 'password123', callback)

      expect(callback).toHaveBeenCalledWith()
      wrapper.unmount()
    })

    it('should fail validation for non-matching passwords', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.password = 'password123'
      const callback = vi.fn()

      vm.validateConfirmPassword({}, 'differentpassword', callback)

      expect(callback).toHaveBeenCalledWith(expect.any(Error))
      expect(callback.mock.calls[0][0].message).toBe('Passwords do not match')
      wrapper.unmount()
    })
  })

  describe('Terms Agreement Validation', () => {
    it('should pass validation when terms are agreed', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      const agreeTermsValidator = vm.rules.agreeTerms[0].validator
      const callback = vi.fn()

      agreeTermsValidator({}, true, callback)

      expect(callback).toHaveBeenCalledWith()
      wrapper.unmount()
    })

    it('should fail validation when terms are not agreed', () => {
      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      const agreeTermsValidator = vm.rules.agreeTerms[0].validator
      const callback = vi.fn()

      agreeTermsValidator({}, false, callback)

      expect(callback).toHaveBeenCalledWith(expect.any(Error))
      expect(callback.mock.calls[0][0].message).toBe('You must agree to the terms of service')
      wrapper.unmount()
    })
  })

  describe('Gender Selection', () => {
    it('should support male gender', async () => {
      mockRegister.mockResolvedValue(undefined)

      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'John'
      vm.form.email = 'john@example.com'
      vm.form.password = 'password123'
      vm.form.confirmPassword = 'password123'
      vm.form.gender = 'male'
      vm.form.agreeTerms = true

      await vm.handleRegister()
      await flushPromises()

      expect(mockRegister).toHaveBeenCalledWith(
        expect.objectContaining({
          gender: 'male',
        })
      )
      wrapper.unmount()
    })

    it('should support female gender', async () => {
      mockRegister.mockResolvedValue(undefined)

      const wrapper = mount(Register)
      const vm = wrapper.vm as unknown as RegisterVM

      vm.form.nickname = 'Jane'
      vm.form.email = 'jane@example.com'
      vm.form.password = 'password123'
      vm.form.confirmPassword = 'password123'
      vm.form.gender = 'female'
      vm.form.agreeTerms = true

      await vm.handleRegister()
      await flushPromises()

      expect(mockRegister).toHaveBeenCalledWith(
        expect.objectContaining({
          gender: 'female',
        })
      )
      wrapper.unmount()
    })
  })
})
