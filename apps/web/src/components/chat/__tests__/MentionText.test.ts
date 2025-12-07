import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import MentionText from '../MentionText.vue'
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

describe('MentionText', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Plain Text', () => {
    it('should render plain text without mentions', () => {
      const wrapper = mount(MentionText, {
        props: {
          content: 'Hello world',
        },
      })

      expect(wrapper.text()).toBe('Hello world')
      expect(wrapper.find('.mention').exists()).toBe(false)
    })

    it('should render empty content', () => {
      const wrapper = mount(MentionText, {
        props: {
          content: '',
        },
      })

      expect(wrapper.text()).toBe('')
    })

    it('should not highlight @text when no atUserIds', () => {
      const wrapper = mount(MentionText, {
        props: {
          content: 'Hello @world',
        },
      })

      expect(wrapper.text()).toBe('Hello @world')
      expect(wrapper.find('.mention').exists()).toBe(false)
    })

    it('should not highlight @text when atUserIds is empty', () => {
      const wrapper = mount(MentionText, {
        props: {
          content: 'Hello @world',
          atUserIds: [],
        },
      })

      expect(wrapper.text()).toBe('Hello @world')
      expect(wrapper.find('.mention').exists()).toBe(false)
    })
  })

  describe('@All Mention', () => {
    it('should highlight @all mention', () => {
      const wrapper = mount(MentionText, {
        props: {
          content: '@all Check this out!',
          atUserIds: [-1],
        },
      })

      expect(wrapper.find('.mention').exists()).toBe(true)
      expect(wrapper.find('.mention').text()).toBe('@All')
      expect(wrapper.find('.mention-me').exists()).toBe(true)
    })

    it('should highlight @All (case insensitive)', () => {
      const wrapper = mount(MentionText, {
        props: {
          content: '@ALL Important announcement',
          atUserIds: [-1],
        },
      })

      expect(wrapper.find('.mention').text()).toBe('@All')
    })

    it('should render text before and after @all', () => {
      const wrapper = mount(MentionText, {
        props: {
          content: 'Hey @all please read this',
          atUserIds: [-1],
        },
      })

      expect(wrapper.text()).toBe('Hey @All please read this')
    })
  })

  describe('User Mentions', () => {
    it('should highlight @username when user is in members', () => {
      const members = new Map<number, User>()
      members.set(10, createMockUser({ id: 10, nickname: 'Alice' }))

      const wrapper = mount(MentionText, {
        props: {
          content: 'Hello @Alice',
          atUserIds: [10],
          members,
        },
      })

      expect(wrapper.find('.mention').exists()).toBe(true)
      expect(wrapper.find('.mention').text()).toBe('@Alice')
    })

    it('should highlight multiple mentions', () => {
      const members = new Map<number, User>()
      members.set(10, createMockUser({ id: 10, nickname: 'Alice' }))
      members.set(20, createMockUser({ id: 20, nickname: 'Bob' }))

      const wrapper = mount(MentionText, {
        props: {
          content: '@Alice and @Bob please help',
          atUserIds: [10, 20],
          members,
        },
      })

      const mentions = wrapper.findAll('.mention')
      expect(mentions.length).toBe(2)
      expect(mentions[0].text()).toBe('@Alice')
      expect(mentions[1].text()).toBe('@Bob')
    })

    it('should highlight @me differently', () => {
      const members = new Map<number, User>()
      members.set(5, createMockUser({ id: 5, nickname: 'Me' }))

      const wrapper = mount(MentionText, {
        props: {
          content: '@Me check this',
          atUserIds: [5],
          members,
          currentUserId: 5,
        },
      })

      expect(wrapper.find('.mention-me').exists()).toBe(true)
    })

    it('should not highlight @me for other users', () => {
      const members = new Map<number, User>()
      members.set(10, createMockUser({ id: 10, nickname: 'Alice' }))

      const wrapper = mount(MentionText, {
        props: {
          content: '@Alice check this',
          atUserIds: [10],
          members,
          currentUserId: 5,
        },
      })

      expect(wrapper.find('.mention').exists()).toBe(true)
      expect(wrapper.find('.mention-me').exists()).toBe(false)
    })

    it('should not highlight @name when user not in atUserIds', () => {
      const members = new Map<number, User>()
      members.set(10, createMockUser({ id: 10, nickname: 'Alice' }))

      const wrapper = mount(MentionText, {
        props: {
          content: '@Alice hello',
          atUserIds: [99], // Different user ID
          members,
        },
      })

      // The @Alice text should appear as plain text, not highlighted
      expect(wrapper.find('.mention').exists()).toBe(false)
    })

    it('should not highlight @name when members map missing', () => {
      const wrapper = mount(MentionText, {
        props: {
          content: '@Alice hello',
          atUserIds: [10],
          // No members provided
        },
      })

      expect(wrapper.find('.mention').exists()).toBe(false)
    })
  })

  describe('Click Events', () => {
    it('should emit mention-click when clicking user mention', async () => {
      const members = new Map<number, User>()
      members.set(10, createMockUser({ id: 10, nickname: 'Alice' }))

      const wrapper = mount(MentionText, {
        props: {
          content: '@Alice check this',
          atUserIds: [10],
          members,
        },
      })

      await wrapper.find('.mention').trigger('click')

      expect(wrapper.emitted('mention-click')).toBeTruthy()
      expect(wrapper.emitted('mention-click')![0]).toEqual([10])
    })

    it('should not emit mention-click for @all', async () => {
      const wrapper = mount(MentionText, {
        props: {
          content: '@all announcement',
          atUserIds: [-1],
        },
      })

      await wrapper.find('.mention').trigger('click')

      // @all click should not emit userId
      expect(wrapper.emitted('mention-click')).toBeFalsy()
    })
  })

  describe('Complex Content', () => {
    it('should handle mixed content with mentions and text', () => {
      const members = new Map<number, User>()
      members.set(10, createMockUser({ id: 10, nickname: 'Alice' }))
      members.set(20, createMockUser({ id: 20, nickname: 'Bob' }))

      const wrapper = mount(MentionText, {
        props: {
          content: 'Hello @Alice, can you talk to @Bob about the project?',
          atUserIds: [10, 20],
          members,
        },
      })

      expect(wrapper.text()).toBe('Hello @Alice, can you talk to @Bob about the project?')
      expect(wrapper.findAll('.mention').length).toBe(2)
    })

    it('should handle @all with user mentions', () => {
      const members = new Map<number, User>()
      members.set(10, createMockUser({ id: 10, nickname: 'Alice' }))

      const wrapper = mount(MentionText, {
        props: {
          content: '@all especially @Alice',
          atUserIds: [-1, 10],
          members,
        },
      })

      const mentions = wrapper.findAll('.mention')
      expect(mentions.length).toBe(2)
      expect(mentions[0].text()).toBe('@All')
      expect(mentions[1].text()).toBe('@Alice')
    })

    it('should preserve whitespace and punctuation', () => {
      const members = new Map<number, User>()
      members.set(10, createMockUser({ id: 10, nickname: 'Alice' }))

      const wrapper = mount(MentionText, {
        props: {
          content: '  @Alice!  Check this out?  ',
          atUserIds: [10],
          members,
        },
      })

      // Text should be mostly preserved (mention text may differ)
      expect(wrapper.text()).toContain('@Alice')
      expect(wrapper.text()).toContain('Check this out?')
    })
  })

  describe('Case Sensitivity', () => {
    it('should match mentions case-insensitively', () => {
      const members = new Map<number, User>()
      members.set(10, createMockUser({ id: 10, nickname: 'Alice' }))

      const wrapper = mount(MentionText, {
        props: {
          content: '@alice hello',
          atUserIds: [10],
          members,
        },
      })

      expect(wrapper.find('.mention').exists()).toBe(true)
    })
  })
})
