import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import GroupCardMessage from '../GroupCardMessage.vue'
import type { Group } from '@/types'

// Helper to create mock group
function createMockGroup(overrides: Partial<Group> = {}): Group {
  return {
    id: 1,
    gid: 'group-123',
    name: 'Test Group',
    ownerId: 1,
    creatorId: 1,
    maxMembers: 200,
    memberCount: 25,
    createdAt: new Date().toISOString(),
    ...overrides,
  }
}

describe('GroupCardMessage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render group card', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup(),
        },
      })

      expect(wrapper.find('.group-card-message').exists()).toBe(true)
    })

    it('should display group name', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ name: 'My Awesome Group' }),
        },
      })

      expect(wrapper.find('.group-name').text()).toBe('My Awesome Group')
    })

    it('should display group gid', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ gid: 'awesome-456' }),
        },
      })

      expect(wrapper.find('.group-gid').text()).toBe('ID: awesome-456')
    })

    it('should display member count', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ memberCount: 42 }),
        },
      })

      expect(wrapper.find('.member-count').text()).toContain('42 members')
    })

    it('should show singular member for count of 1', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ memberCount: 1 }),
        },
      })

      expect(wrapper.find('.member-count').text()).toContain('1 member')
    })

    it('should display announcement when provided', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ announcement: 'Welcome to our group!' }),
        },
      })

      expect(wrapper.find('.group-announcement').text()).toBe('Welcome to our group!')
    })

    it('should not display announcement when not provided', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ announcement: undefined }),
        },
      })

      expect(wrapper.find('.group-announcement').exists()).toBe(false)
    })

    it('should display avatar with first letter when no avatar url', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ name: 'Developers', avatar: undefined }),
        },
      })

      const avatar = wrapper.find('.group-avatar')
      expect(avatar.exists()).toBe(true)
      expect(avatar.text()).toBe('D')
    })

    it('should display Group Card indicator', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup(),
        },
      })

      expect(wrapper.find('.card-type-indicator').text()).toContain('Group Card')
    })
  })

  describe('Action Buttons', () => {
    it('should show Open Chat button for members', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup(),
          isMember: true,
        },
      })

      const btn = wrapper.find('.card-actions .el-button')
      expect(btn.text()).toContain('Open Chat')
    })

    it('should show Join Group button for non-members', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup(),
          isMember: false,
        },
      })

      const btn = wrapper.find('.card-actions .el-button')
      expect(btn.text()).toContain('Join Group')
    })
  })

  describe('Events', () => {
    it('should emit view-group on card click', async () => {
      const group = createMockGroup()
      const wrapper = mount(GroupCardMessage, {
        props: { group },
      })

      await wrapper.find('.group-card-message').trigger('click')

      expect(wrapper.emitted('view-group')).toBeTruthy()
      expect(wrapper.emitted('view-group')![0]).toEqual([group])
    })

    it('should emit open-chat on Open Chat button click', async () => {
      const group = createMockGroup()
      const wrapper = mount(GroupCardMessage, {
        props: {
          group,
          isMember: true,
        },
      })

      await wrapper.find('.card-actions .el-button').trigger('click')

      expect(wrapper.emitted('open-chat')).toBeTruthy()
      expect(wrapper.emitted('open-chat')![0]).toEqual([group])
      // Should not also emit view-group (stopPropagation)
      expect(wrapper.emitted('view-group')).toBeFalsy()
    })

    it('should emit join-group on Join Group button click', async () => {
      const group = createMockGroup()
      const wrapper = mount(GroupCardMessage, {
        props: {
          group,
          isMember: false,
        },
      })

      await wrapper.find('.card-actions .el-button').trigger('click')

      expect(wrapper.emitted('join-group')).toBeTruthy()
      expect(wrapper.emitted('join-group')![0]).toEqual([group])
      // Should not also emit view-group (stopPropagation)
      expect(wrapper.emitted('view-group')).toBeFalsy()
    })
  })

  describe('Edge Cases', () => {
    it('should handle missing name gracefully', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ name: '' }),
        },
      })

      expect(wrapper.find('.group-name').text()).toBe('Unknown Group')
    })

    it('should handle zero member count', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ memberCount: 0 }),
        },
      })

      expect(wrapper.find('.member-count').text()).toContain('0 members')
    })

    it('should handle undefined member count', () => {
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ memberCount: undefined }),
        },
      })

      expect(wrapper.find('.member-count').text()).toContain('0 members')
    })

    it('should truncate long group names', () => {
      const longName = 'This Is A Very Long Group Name That Should Be Truncated'
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ name: longName }),
        },
      })

      const nameEl = wrapper.find('.group-name')
      expect(nameEl.text()).toBe(longName)
    })

    it('should truncate long announcements', () => {
      const longAnnouncement = 'This is a very long announcement that should be truncated with ellipsis in the UI'
      const wrapper = mount(GroupCardMessage, {
        props: {
          group: createMockGroup({ announcement: longAnnouncement }),
        },
      })

      const announcementEl = wrapper.find('.group-announcement')
      expect(announcementEl.text()).toBe(longAnnouncement)
    })
  })
})
