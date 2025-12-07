import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import UserProfileDialog from '../UserProfileDialog.vue'
import type { User } from '@/types'

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
    ...overrides,
  }
}

// Define VM interface for type casting
interface UserProfileDialogVM {
  displayName: string
  genderText: string
  statusText: string
  statusClass: string
  formattedCreatedAt: string
  formattedLastLogin: string
  handleSendMessage: () => void
  handleAddFriend: () => void
  handleEditProfile: () => void
  close: () => void
}

describe('UserProfileDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Computed Properties - Display Name', () => {
    it('should return nickname as displayName', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ nickname: 'Alice' }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.displayName).toBe('Alice')
      wrapper.unmount()
    })

    it('should return "Unknown User" when nickname is empty', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ nickname: '' }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.displayName).toBe('Unknown User')
      wrapper.unmount()
    })

    it('should return "Unknown User" when user is null', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: null,
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.displayName).toBe('Unknown User')
      wrapper.unmount()
    })
  })

  describe('Computed Properties - Gender', () => {
    it('should return "Male" for male gender', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ gender: 'male' }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.genderText).toBe('Male')
      wrapper.unmount()
    })

    it('should return "Female" for female gender', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ gender: 'female' }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.genderText).toBe('Female')
      wrapper.unmount()
    })

    it('should return empty string for unset gender', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ gender: undefined }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.genderText).toBe('')
      wrapper.unmount()
    })
  })

  describe('Computed Properties - Status', () => {
    it('should return "Online" for active status', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ status: 'active' }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.statusText).toBe('Online')
      expect(vm.statusClass).toBe('status-online')
      wrapper.unmount()
    })

    it('should return "Offline" for inactive status', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ status: 'inactive' }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.statusText).toBe('Offline')
      expect(vm.statusClass).toBe('status-offline')
      wrapper.unmount()
    })

    it('should return empty string when status is undefined', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ status: undefined }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.statusText).toBe('')
      wrapper.unmount()
    })
  })

  describe('Computed Properties - Date Formatting', () => {
    it('should format createdAt date', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ createdAt: '2025-01-15T10:00:00Z' }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.formattedCreatedAt).toContain('2025')
      expect(vm.formattedCreatedAt).toContain('January')
      wrapper.unmount()
    })

    it('should return empty string when createdAt is missing', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ createdAt: undefined }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.formattedCreatedAt).toBe('')
      wrapper.unmount()
    })

    it('should format lastLoginAt date', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ lastLoginAt: '2025-12-07T10:30:00Z' }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.formattedLastLogin).toContain('2025')
      wrapper.unmount()
    })

    it('should return empty string when lastLoginAt is missing', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser({ lastLoginAt: undefined }),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      expect(vm.formattedLastLogin).toBe('')
      wrapper.unmount()
    })
  })

  describe('Methods - Send Message', () => {
    it('should emit send-message event with user', async () => {
      const user = createMockUser()
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user,
          isFriend: true,
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      vm.handleSendMessage()
      await flushPromises()

      expect(wrapper.emitted('send-message')).toBeTruthy()
      expect(wrapper.emitted('send-message')![0]).toEqual([user])
      wrapper.unmount()
    })

    it('should emit update:visible false after send-message', async () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
          isFriend: true,
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      vm.handleSendMessage()
      await flushPromises()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
      wrapper.unmount()
    })

    it('should not emit send-message when user is null', async () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: null,
          isFriend: true,
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      vm.handleSendMessage()
      await flushPromises()

      expect(wrapper.emitted('send-message')).toBeFalsy()
      wrapper.unmount()
    })
  })

  describe('Methods - Add Friend', () => {
    it('should emit add-friend event with user', async () => {
      const user = createMockUser()
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user,
          isFriend: false,
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      vm.handleAddFriend()
      await flushPromises()

      expect(wrapper.emitted('add-friend')).toBeTruthy()
      expect(wrapper.emitted('add-friend')![0]).toEqual([user])
      wrapper.unmount()
    })

    it('should not emit add-friend when user is null', async () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: null,
          isFriend: false,
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      vm.handleAddFriend()
      await flushPromises()

      expect(wrapper.emitted('add-friend')).toBeFalsy()
      wrapper.unmount()
    })
  })

  describe('Methods - Edit Profile', () => {
    it('should emit edit-profile event', async () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
          isSelf: true,
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      vm.handleEditProfile()
      await flushPromises()

      expect(wrapper.emitted('edit-profile')).toBeTruthy()
      wrapper.unmount()
    })

    it('should emit update:visible false after edit-profile', async () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
          isSelf: true,
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      vm.handleEditProfile()
      await flushPromises()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
      wrapper.unmount()
    })
  })

  describe('Methods - Close', () => {
    it('should emit update:visible false when close is called', async () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      const vm = wrapper.vm as unknown as UserProfileDialogVM
      vm.close()
      await flushPromises()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
      wrapper.unmount()
    })
  })

  describe('Props Validation', () => {
    it('should accept all required props', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
        },
      })

      expect(wrapper.props('visible')).toBe(true)
      expect(wrapper.props('user')).not.toBeNull()
      wrapper.unmount()
    })

    it('should accept optional isFriend prop', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
          isFriend: true,
        },
      })

      expect(wrapper.props('isFriend')).toBe(true)
      wrapper.unmount()
    })

    it('should accept optional isSelf prop', () => {
      const wrapper = mount(UserProfileDialog, {
        props: {
          visible: true,
          user: createMockUser(),
          isSelf: true,
        },
      })

      expect(wrapper.props('isSelf')).toBe(true)
      wrapper.unmount()
    })
  })
})
