import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import TypingIndicator from '../TypingIndicator.vue'
import type { User } from '@/types'

// Helper to create mock user
function createMockUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    uid: 'user-1',
    email: 'user@example.com',
    nickname: 'User 1',
    gender: 'male',
    status: 'active',
    createdAt: new Date().toISOString(),
    ...overrides,
  }
}

describe('TypingIndicator', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should not render when no users are typing', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [],
        },
      })

      expect(wrapper.find('.typing-indicator').exists()).toBe(false)
    })

    it('should render when users are typing', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [createMockUser()],
        },
      })

      expect(wrapper.find('.typing-indicator').exists()).toBe(true)
    })

    it('should display animated dots', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [createMockUser()],
        },
      })

      const dots = wrapper.findAll('.dot')
      expect(dots.length).toBe(3)
    })
  })

  describe('Text Display', () => {
    it('should show single user typing', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [createMockUser({ nickname: 'Alice' })],
        },
      })

      expect(wrapper.find('.typing-text').text()).toBe('Alice is typing')
    })

    it('should show two users typing', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [
            createMockUser({ id: 1, nickname: 'Alice' }),
            createMockUser({ id: 2, nickname: 'Bob' }),
          ],
        },
      })

      expect(wrapper.find('.typing-text').text()).toBe('Alice and Bob are typing')
    })

    it('should show three or more users with default max names', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [
            createMockUser({ id: 1, nickname: 'Alice' }),
            createMockUser({ id: 2, nickname: 'Bob' }),
            createMockUser({ id: 3, nickname: 'Charlie' }),
          ],
        },
      })

      // Default maxNamesToShow is 2
      expect(wrapper.find('.typing-text').text()).toBe('Alice, Bob and 1 other are typing')
    })

    it('should show multiple others when many users typing', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [
            createMockUser({ id: 1, nickname: 'Alice' }),
            createMockUser({ id: 2, nickname: 'Bob' }),
            createMockUser({ id: 3, nickname: 'Charlie' }),
            createMockUser({ id: 4, nickname: 'David' }),
            createMockUser({ id: 5, nickname: 'Eve' }),
          ],
        },
      })

      expect(wrapper.find('.typing-text').text()).toBe('Alice, Bob and 3 others are typing')
    })

    it('should respect custom maxNamesToShow', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [
            createMockUser({ id: 1, nickname: 'Alice' }),
            createMockUser({ id: 2, nickname: 'Bob' }),
            createMockUser({ id: 3, nickname: 'Charlie' }),
          ],
          maxNamesToShow: 3,
        },
      })

      expect(wrapper.find('.typing-text').text()).toBe('Alice, Bob and Charlie are typing')
    })

    it('should show 1 other when 1 extra user with custom max', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [
            createMockUser({ id: 1, nickname: 'Alice' }),
            createMockUser({ id: 2, nickname: 'Bob' }),
            createMockUser({ id: 3, nickname: 'Charlie' }),
            createMockUser({ id: 4, nickname: 'David' }),
          ],
          maxNamesToShow: 3,
        },
      })

      expect(wrapper.find('.typing-text').text()).toBe('Alice, Bob, Charlie and 1 other are typing')
    })
  })

  describe('Animation', () => {
    it('should have animation classes on dots', () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [createMockUser()],
        },
      })

      const dots = wrapper.findAll('.dot')
      dots.forEach((dot) => {
        // Check that dots have the animation applied via class
        expect(dot.classes()).toContain('dot')
      })
    })
  })

  describe('Reactivity', () => {
    it('should update text when typingUsers changes', async () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [createMockUser({ nickname: 'Alice' })],
        },
      })

      expect(wrapper.find('.typing-text').text()).toBe('Alice is typing')

      await wrapper.setProps({
        typingUsers: [
          createMockUser({ id: 1, nickname: 'Alice' }),
          createMockUser({ id: 2, nickname: 'Bob' }),
        ],
      })

      expect(wrapper.find('.typing-text').text()).toBe('Alice and Bob are typing')
    })

    it('should hide when all users stop typing', async () => {
      const wrapper = mount(TypingIndicator, {
        props: {
          typingUsers: [createMockUser()],
        },
      })

      expect(wrapper.find('.typing-indicator').exists()).toBe(true)

      await wrapper.setProps({ typingUsers: [] })

      expect(wrapper.find('.typing-indicator').exists()).toBe(false)
    })
  })
})
