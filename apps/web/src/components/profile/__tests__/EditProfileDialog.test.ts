import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import EditProfileDialog from '../EditProfileDialog.vue'
import type { User } from '@/types'

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

// Helper to create mock user
function createMockUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    uid: 'user-123',
    email: 'user@example.com',
    nickname: 'Test User',
    gender: 'male',
    status: 'active',
    createdAt: '2025-01-15T10:00:00Z',
    signature: 'Hello world',
    description: 'I love coding',
    ...overrides,
  }
}

// Define VM interface for type casting
interface EditProfileDialogVM {
  form: {
    nickname: string
    gender: '' | 'male' | 'female'
    signature: string
    description: string
  }
  avatarFile: File | null
  avatarPreview: string
  isSaving: boolean
  nicknameError: string
  signatureError: string
  descriptionError: string
  isFormValid: boolean
  hasChanges: boolean
  handleAvatarChange: (event: Event) => void
  removeAvatar: () => void
  close: () => void
  handleSave: () => Promise<void>
}

describe('EditProfileDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Form Initialization', () => {
    it('should initialize form with user data when dialog opens', async () => {
      const user = createMockUser({
        nickname: 'Alice',
        gender: 'female',
        signature: 'My signature',
        description: 'About me',
      })

      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user,
        },
      })

      // Open the dialog
      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      expect(vm.form.nickname).toBe('Alice')
      expect(vm.form.gender).toBe('female')
      expect(vm.form.signature).toBe('My signature')
      expect(vm.form.description).toBe('About me')
      wrapper.unmount()
    })

    it('should initialize form with empty values when user fields are undefined', async () => {
      const user = createMockUser({
        nickname: 'Bob',
        gender: undefined,
        signature: undefined,
        description: undefined,
      })

      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user,
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      expect(vm.form.nickname).toBe('Bob')
      expect(vm.form.gender).toBe('')
      expect(vm.form.signature).toBe('')
      expect(vm.form.description).toBe('')
      wrapper.unmount()
    })

    it('should reset avatar file when dialog opens', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser({ avatar: 'http://example.com/avatar.jpg' }),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      expect(vm.avatarFile).toBeNull()
      expect(vm.avatarPreview).toBe('http://example.com/avatar.jpg')
      wrapper.unmount()
    })
  })

  describe('Validation - Nickname', () => {
    it('should show error when nickname is empty', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.nickname = ''
      await flushPromises()

      expect(vm.nicknameError).toBe('Nickname is required')
      expect(vm.isFormValid).toBe(false)
      wrapper.unmount()
    })

    it('should show error when nickname is too long', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.nickname = 'A'.repeat(31)
      await flushPromises()

      expect(vm.nicknameError).toBe('Nickname must be 30 characters or less')
      expect(vm.isFormValid).toBe(false)
      wrapper.unmount()
    })

    it('should not show error for valid nickname', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.nickname = 'Valid Name'
      await flushPromises()

      expect(vm.nicknameError).toBe('')
      wrapper.unmount()
    })
  })

  describe('Validation - Signature', () => {
    it('should show error when signature is too long', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.signature = 'A'.repeat(101)
      await flushPromises()

      expect(vm.signatureError).toBe('Signature must be 100 characters or less')
      expect(vm.isFormValid).toBe(false)
      wrapper.unmount()
    })

    it('should not show error for valid signature', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.signature = 'Valid signature'
      await flushPromises()

      expect(vm.signatureError).toBe('')
      wrapper.unmount()
    })
  })

  describe('Validation - Description', () => {
    it('should show error when description is too long', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.description = 'A'.repeat(501)
      await flushPromises()

      expect(vm.descriptionError).toBe('Description must be 500 characters or less')
      expect(vm.isFormValid).toBe(false)
      wrapper.unmount()
    })

    it('should not show error for valid description', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.description = 'Valid description'
      await flushPromises()

      expect(vm.descriptionError).toBe('')
      wrapper.unmount()
    })
  })

  describe('Change Detection', () => {
    it('should detect changes in nickname', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser({ nickname: 'Original' }),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      expect(vm.hasChanges).toBe(false)

      vm.form.nickname = 'Changed'
      await flushPromises()

      expect(vm.hasChanges).toBe(true)
      wrapper.unmount()
    })

    it('should detect changes in gender', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser({ gender: 'male' }),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.gender = 'female'
      await flushPromises()

      expect(vm.hasChanges).toBe(true)
      wrapper.unmount()
    })

    it('should detect changes in signature', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser({ signature: 'Original' }),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.signature = 'Changed'
      await flushPromises()

      expect(vm.hasChanges).toBe(true)
      wrapper.unmount()
    })

    it('should not detect changes when form matches user', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser({
            nickname: 'Test',
            gender: 'male',
            signature: 'Sig',
            description: 'Desc',
          }),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      expect(vm.hasChanges).toBe(false)
      wrapper.unmount()
    })
  })

  describe('Avatar Handling', () => {
    it('should reject non-image files', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      const mockEvent = {
        target: {
          files: [new File(['content'], 'test.pdf', { type: 'application/pdf' })],
        },
      } as unknown as Event

      vm.handleAvatarChange(mockEvent)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Please select an image file')
      expect(vm.avatarFile).toBeNull()
      wrapper.unmount()
    })

    it('should reject files larger than 5MB', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM

      // Create a mock file that's 6MB
      const largeContent = new Array(6 * 1024 * 1024).fill('a').join('')
      const mockEvent = {
        target: {
          files: [new File([largeContent], 'large.jpg', { type: 'image/jpeg' })],
        },
      } as unknown as Event

      vm.handleAvatarChange(mockEvent)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Image size must be less than 5MB')
      expect(vm.avatarFile).toBeNull()
      wrapper.unmount()
    })

    it('should accept valid image files', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      const file = new File(['image'], 'test.jpg', { type: 'image/jpeg' })
      const mockEvent = {
        target: {
          files: [file],
        },
      } as unknown as Event

      vm.handleAvatarChange(mockEvent)
      await flushPromises()

      expect(vm.avatarFile).not.toBeNull()
      expect(vm.avatarFile?.name).toBe('test.jpg')
      expect(vm.avatarFile?.type).toBe('image/jpeg')
      expect(vm.hasChanges).toBe(true)
      wrapper.unmount()
    })

    it('should reset avatar when removeAvatar is called', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser({ avatar: 'http://example.com/avatar.jpg' }),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM

      // Simulate avatar file selection
      const file = new File(['image'], 'test.jpg', { type: 'image/jpeg' })
      vm.avatarFile = file

      // Remove avatar
      vm.removeAvatar()
      await flushPromises()

      expect(vm.avatarFile).toBeNull()
      expect(vm.avatarPreview).toBe('http://example.com/avatar.jpg')
      wrapper.unmount()
    })
  })

  describe('Save Action', () => {
    it('should warn when form is invalid', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.nickname = '' // Invalid
      await vm.handleSave()

      expect(ElMessage.warning).toHaveBeenCalledWith('Please fix the validation errors')
      expect(wrapper.emitted('save')).toBeFalsy()
      wrapper.unmount()
    })

    it('should show info when no changes made', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser({ nickname: 'Test' }),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      await vm.handleSave()

      expect(ElMessage.info).toHaveBeenCalledWith('No changes to save')
      wrapper.unmount()
    })

    it('should emit save event with profile data', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser({ nickname: 'Original' }),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.nickname = 'Updated'
      vm.form.gender = 'female'
      vm.form.signature = 'New signature'
      vm.form.description = 'New description'

      await vm.handleSave()

      expect(wrapper.emitted('save')).toBeTruthy()
      const saveData = wrapper.emitted('save')![0][0] as Record<string, unknown>
      expect(saveData.nickname).toBe('Updated')
      expect(saveData.gender).toBe('female')
      expect(saveData.signature).toBe('New signature')
      expect(saveData.description).toBe('New description')
      wrapper.unmount()
    })

    it('should close dialog after save', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser({ nickname: 'Original' }),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.form.nickname = 'Updated'

      await vm.handleSave()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
      wrapper.unmount()
    })

    it('should include avatar file when changed', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: false,
          user: createMockUser(),
        },
      })

      await wrapper.setProps({ visible: true })
      await flushPromises()

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      const file = new File(['image'], 'test.jpg', { type: 'image/jpeg' })
      vm.avatarFile = file
      vm.form.nickname = 'Updated'

      await vm.handleSave()

      const saveData = wrapper.emitted('save')![0][0] as Record<string, unknown>
      expect(saveData.avatar).not.toBeNull()
      expect((saveData.avatar as File).name).toBe('test.jpg')
      wrapper.unmount()
    })
  })

  describe('Close Action', () => {
    it('should emit update:visible false when close is called', async () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      vm.close()
      await flushPromises()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
      wrapper.unmount()
    })
  })

  describe('Props Handling', () => {
    it('should accept required props', () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      expect(wrapper.props('visible')).toBe(true)
      expect(wrapper.props('user')).not.toBeNull()
      wrapper.unmount()
    })

    it('should handle null user', () => {
      const wrapper = mount(EditProfileDialog, {
        props: {
          visible: true,
          user: null,
        },
      })

      const vm = wrapper.vm as unknown as EditProfileDialogVM
      expect(vm.hasChanges).toBe(false)
      wrapper.unmount()
    })
  })
})
