import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import ChangePasswordDialog from '../ChangePasswordDialog.vue'

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      warning: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
    },
  }
})

// Disable teleport for testing
config.global.stubs = {
  teleport: true,
}

// Define VM interface for type casting
interface ChangePasswordDialogVM {
  form: {
    currentPassword: string
    newPassword: string
    confirmPassword: string
  }
  showCurrentPassword: boolean
  showNewPassword: boolean
  showConfirmPassword: boolean
  isSubmitting: boolean
  currentPasswordError: string
  newPasswordError: string
  confirmPasswordError: string
  isFormValid: boolean
  passwordStrength: {
    level: number
    text: string
    class: string
  }
  close: () => void
  handleSubmit: () => Promise<void>
}

describe('ChangePasswordDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Form Initialization', () => {
    it('should initialize with empty form when dialog opens', async () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: false,
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      expect(vm.form.currentPassword).toBe('')
      expect(vm.form.newPassword).toBe('')
      expect(vm.form.confirmPassword).toBe('')
      wrapper.unmount()
    })

    it('should reset form when dialog reopens', async () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.currentPassword = 'old123'
      vm.form.newPassword = 'new123'
      vm.form.confirmPassword = 'new123'

      // Close and reopen
      await wrapper.setProps({ visible: false })
      await wrapper.setProps({ visible: true })
      await flushPromises()

      expect(vm.form.currentPassword).toBe('')
      expect(vm.form.newPassword).toBe('')
      expect(vm.form.confirmPassword).toBe('')
      wrapper.unmount()
    })

    it('should reset password visibility when dialog opens', async () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: false,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.showCurrentPassword = true
      vm.showNewPassword = true
      vm.showConfirmPassword = true

      await wrapper.setProps({ visible: true })
      await flushPromises()

      expect(vm.showCurrentPassword).toBe(false)
      expect(vm.showNewPassword).toBe(false)
      expect(vm.showConfirmPassword).toBe(false)
      wrapper.unmount()
    })
  })

  describe('Validation - Current Password', () => {
    it('should show error when current password is empty', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.currentPassword = ''

      expect(vm.currentPasswordError).toBe('Current password is required')
      wrapper.unmount()
    })

    it('should not show error when current password is provided', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.currentPassword = 'mypassword'

      expect(vm.currentPasswordError).toBe('')
      wrapper.unmount()
    })
  })

  describe('Validation - New Password', () => {
    it('should show error when new password is empty', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.newPassword = ''

      expect(vm.newPasswordError).toBe('New password is required')
      wrapper.unmount()
    })

    it('should show error when new password is too short', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.newPassword = '12345'

      expect(vm.newPasswordError).toBe('Password must be at least 6 characters')
      wrapper.unmount()
    })

    it('should show error when new password is too long', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.newPassword = 'a'.repeat(51)

      expect(vm.newPasswordError).toBe('Password must be 50 characters or less')
      wrapper.unmount()
    })

    it('should show error when new password matches current', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.currentPassword = 'samepassword'
      vm.form.newPassword = 'samepassword'

      expect(vm.newPasswordError).toBe('New password must be different from current password')
      wrapper.unmount()
    })

    it('should not show error for valid new password', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.currentPassword = 'oldpassword'
      vm.form.newPassword = 'newpassword123'

      expect(vm.newPasswordError).toBe('')
      wrapper.unmount()
    })
  })

  describe('Validation - Confirm Password', () => {
    it('should show error when confirm password is empty', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.confirmPassword = ''

      expect(vm.confirmPasswordError).toBe('Please confirm your new password')
      wrapper.unmount()
    })

    it('should show error when passwords do not match', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.newPassword = 'password123'
      vm.form.confirmPassword = 'different'

      expect(vm.confirmPasswordError).toBe('Passwords do not match')
      wrapper.unmount()
    })

    it('should not show error when passwords match', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.newPassword = 'password123'
      vm.form.confirmPassword = 'password123'

      expect(vm.confirmPasswordError).toBe('')
      wrapper.unmount()
    })
  })

  describe('Form Validity', () => {
    it('should be invalid when fields are empty', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      expect(vm.isFormValid).toBe(false)
      wrapper.unmount()
    })

    it('should be valid when all fields are correct', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.currentPassword = 'oldpassword'
      vm.form.newPassword = 'newpassword123'
      vm.form.confirmPassword = 'newpassword123'

      expect(vm.isFormValid).toBe(true)
      wrapper.unmount()
    })
  })

  describe('Password Strength', () => {
    it('should show no strength for empty password', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.newPassword = ''

      expect(vm.passwordStrength.level).toBe(0)
      expect(vm.passwordStrength.text).toBe('')
      wrapper.unmount()
    })

    it('should show weak for simple password', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.newPassword = 'abc123'

      expect(vm.passwordStrength.level).toBe(1)
      expect(vm.passwordStrength.text).toBe('Weak')
      expect(vm.passwordStrength.class).toBe('strength-weak')
      wrapper.unmount()
    })

    it('should show medium for moderate password', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.newPassword = 'Password1'

      expect(vm.passwordStrength.level).toBe(2)
      expect(vm.passwordStrength.text).toBe('Medium')
      expect(vm.passwordStrength.class).toBe('strength-medium')
      wrapper.unmount()
    })

    it('should show strong for complex password', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.newPassword = 'MyP@ssword123!'

      expect(vm.passwordStrength.level).toBe(3)
      expect(vm.passwordStrength.text).toBe('Strong')
      expect(vm.passwordStrength.class).toBe('strength-strong')
      wrapper.unmount()
    })
  })

  describe('Submit Action', () => {
    it('should warn when form is invalid', async () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      await vm.handleSubmit()

      expect(ElMessage.warning).toHaveBeenCalledWith('Please fix the validation errors')
      expect(wrapper.emitted('change-password')).toBeFalsy()
      wrapper.unmount()
    })

    it('should emit change-password event with data when valid', async () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.currentPassword = 'oldpassword'
      vm.form.newPassword = 'newpassword123'
      vm.form.confirmPassword = 'newpassword123'

      await vm.handleSubmit()

      expect(wrapper.emitted('change-password')).toBeTruthy()
      const emitData = wrapper.emitted('change-password')![0][0] as Record<string, string>
      expect(emitData.currentPassword).toBe('oldpassword')
      expect(emitData.newPassword).toBe('newpassword123')
      wrapper.unmount()
    })

    it('should close dialog after submit', async () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.form.currentPassword = 'oldpassword'
      vm.form.newPassword = 'newpassword123'
      vm.form.confirmPassword = 'newpassword123'

      await vm.handleSubmit()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
      wrapper.unmount()
    })
  })

  describe('Close Action', () => {
    it('should emit update:visible false when close is called', async () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as ChangePasswordDialogVM
      vm.close()
      await flushPromises()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
      wrapper.unmount()
    })
  })

  describe('Props Handling', () => {
    it('should accept visible prop', () => {
      const wrapper = mount(ChangePasswordDialog, {
        props: {
          visible: true,
        },
      })

      expect(wrapper.props('visible')).toBe(true)
      wrapper.unmount()
    })
  })
})
