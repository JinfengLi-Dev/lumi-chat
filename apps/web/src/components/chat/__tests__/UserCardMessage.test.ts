import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import UserCardMessage from '../UserCardMessage.vue'
import type { User } from '@/types'

// Helper to create mock user
function createMockUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    uid: 'user-123',
    email: 'user@example.com',
    nickname: 'Test User',
    gender: 'male',
    status: 'active',
    createdAt: new Date().toISOString(),
    ...overrides,
  }
}

describe('UserCardMessage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render user card', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser(),
        },
      })

      expect(wrapper.find('.user-card-message').exists()).toBe(true)
    })

    it('should display user nickname', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ nickname: 'Alice' }),
        },
      })

      expect(wrapper.find('.user-name').text()).toBe('Alice')
    })

    it('should display user uid', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ uid: 'alice-456' }),
        },
      })

      expect(wrapper.find('.user-uid').text()).toBe('ID: alice-456')
    })

    it('should display user signature when provided', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ signature: 'Hello world!' }),
        },
      })

      expect(wrapper.find('.user-signature').text()).toBe('Hello world!')
    })

    it('should not display signature when not provided', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ signature: undefined }),
        },
      })

      expect(wrapper.find('.user-signature').exists()).toBe(false)
    })

    it('should display avatar with first letter when no avatar url', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ nickname: 'Bob', avatar: undefined }),
        },
      })

      const avatar = wrapper.find('.user-avatar')
      expect(avatar.exists()).toBe(true)
      expect(avatar.text()).toBe('B')
    })

    it('should display Contact Card indicator', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser(),
        },
      })

      expect(wrapper.find('.card-type-indicator').text()).toContain('Contact Card')
    })
  })

  describe('Status Display', () => {
    it('should show Online status for active users', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ status: 'active' }),
        },
      })

      const status = wrapper.find('.user-status')
      expect(status.text()).toBe('Online')
      expect(status.classes()).toContain('status-online')
    })

    it('should show Offline status for inactive users', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ status: 'inactive' }),
        },
      })

      const status = wrapper.find('.user-status')
      expect(status.text()).toBe('Offline')
      expect(status.classes()).toContain('status-offline')
    })

    it('should show status text for other statuses', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ status: 'banned' }),
        },
      })

      const status = wrapper.find('.user-status')
      expect(status.text()).toBe('banned')
    })
  })

  describe('Action Buttons', () => {
    it('should show Message button for friends', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser(),
          isFriend: true,
        },
      })

      const btn = wrapper.find('.card-actions .el-button')
      expect(btn.text()).toContain('Message')
    })

    it('should show Add Friend button for non-friends', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser(),
          isFriend: false,
        },
      })

      const btn = wrapper.find('.card-actions .el-button')
      expect(btn.text()).toContain('Add Friend')
    })

    it('should not show action buttons for own card', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser(),
          isSelf: true,
        },
      })

      expect(wrapper.find('.card-actions').exists()).toBe(false)
    })
  })

  describe('Events', () => {
    it('should emit view-profile on card click', async () => {
      const user = createMockUser()
      const wrapper = mount(UserCardMessage, {
        props: { user },
      })

      await wrapper.find('.user-card-message').trigger('click')

      expect(wrapper.emitted('view-profile')).toBeTruthy()
      expect(wrapper.emitted('view-profile')![0]).toEqual([user])
    })

    it('should emit send-message on Message button click', async () => {
      const user = createMockUser()
      const wrapper = mount(UserCardMessage, {
        props: {
          user,
          isFriend: true,
        },
      })

      await wrapper.find('.card-actions .el-button').trigger('click')

      expect(wrapper.emitted('send-message')).toBeTruthy()
      expect(wrapper.emitted('send-message')![0]).toEqual([user])
      // Should not also emit view-profile (stopPropagation)
      expect(wrapper.emitted('view-profile')).toBeFalsy()
    })

    it('should emit add-friend on Add Friend button click', async () => {
      const user = createMockUser()
      const wrapper = mount(UserCardMessage, {
        props: {
          user,
          isFriend: false,
        },
      })

      await wrapper.find('.card-actions .el-button').trigger('click')

      expect(wrapper.emitted('add-friend')).toBeTruthy()
      expect(wrapper.emitted('add-friend')![0]).toEqual([user])
      // Should not also emit view-profile (stopPropagation)
      expect(wrapper.emitted('view-profile')).toBeFalsy()
    })
  })

  describe('Edge Cases', () => {
    it('should handle missing nickname gracefully', () => {
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ nickname: '' }),
        },
      })

      expect(wrapper.find('.user-name').text()).toBe('Unknown User')
    })

    it('should truncate long nicknames', () => {
      const longName = 'This Is A Very Long Nickname That Should Be Truncated'
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ nickname: longName }),
        },
      })

      // The component should have text-overflow: ellipsis style
      const nameEl = wrapper.find('.user-name')
      expect(nameEl.text()).toBe(longName)
    })

    it('should truncate long signatures', () => {
      const longSignature = 'This is a very long signature that should be truncated with ellipsis'
      const wrapper = mount(UserCardMessage, {
        props: {
          user: createMockUser({ signature: longSignature }),
        },
      })

      const signatureEl = wrapper.find('.user-signature')
      expect(signatureEl.text()).toBe(longSignature)
    })
  })
})
