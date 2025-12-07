import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import GroupsList from '../GroupsList.vue'
import { groupApi } from '@/api/group'
import { conversationApi } from '@/api/conversation'
import type { GroupDetail } from '@/api/group'
import type { Conversation } from '@/types'

// Mock the APIs
vi.mock('@/api/group', () => ({
  groupApi: {
    getGroups: vi.fn(),
  },
}))

vi.mock('@/api/conversation', () => ({
  conversationApi: {
    getConversations: vi.fn(),
  },
}))

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      warning: vi.fn(),
      error: vi.fn(),
    },
  }
})

// Disable teleport for testing
config.global.stubs = {
  teleport: true,
}

// Helper to create mock group
function createMockGroup(overrides: Partial<GroupDetail> = {}): GroupDetail {
  return {
    id: 1,
    gid: 'group-1',
    name: 'Test Group',
    ownerId: 10,
    maxMembers: 200,
    memberCount: 5,
    createdAt: new Date().toISOString(),
    ...overrides,
  }
}

// Helper to create mock conversation
function createMockConversation(overrides: Partial<Conversation> = {}): Conversation {
  return {
    id: 1,
    type: 'group',
    participantIds: [1],
    groupId: 1,
    unreadCount: 0,
    isMuted: false,
    isPinned: false,
    ...overrides,
  }
}

describe('GroupsList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Loading', () => {
    it('should load groups on mount', async () => {
      const groups = [createMockGroup()]
      vi.mocked(groupApi.getGroups).mockResolvedValue(groups)

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      expect(groupApi.getGroups).toHaveBeenCalled()
      wrapper.unmount()
    })

    it('should handle loading error', async () => {
      vi.mocked(groupApi.getGroups).mockRejectedValue(new Error('Network error'))

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Network error')
      wrapper.unmount()
    })

    it('should show empty state when no groups', async () => {
      vi.mocked(groupApi.getGroups).mockResolvedValue([])

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      expect(wrapper.find('.empty-state').exists()).toBe(true)
      wrapper.unmount()
    })
  })

  describe('Filtering', () => {
    it('should filter groups by name', async () => {
      const groups = [
        createMockGroup({ id: 1, name: 'Engineering Team' }),
        createMockGroup({ id: 2, name: 'Marketing Team' }),
      ]
      vi.mocked(groupApi.getGroups).mockResolvedValue(groups)

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      type VM = { searchQuery: string; filteredGroups: GroupDetail[] }
      const vm = wrapper.vm as unknown as VM

      expect(vm.filteredGroups).toHaveLength(2)

      vm.searchQuery = 'Engineering'
      await flushPromises()

      expect(vm.filteredGroups).toHaveLength(1)
      expect(vm.filteredGroups[0].name).toBe('Engineering Team')

      wrapper.unmount()
    })

    it('should be case insensitive', async () => {
      const groups = [
        createMockGroup({ id: 1, name: 'ENGINEERING' }),
      ]
      vi.mocked(groupApi.getGroups).mockResolvedValue(groups)

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      type VM = { searchQuery: string; filteredGroups: GroupDetail[] }
      const vm = wrapper.vm as unknown as VM

      vm.searchQuery = 'engineering'
      await flushPromises()

      expect(vm.filteredGroups).toHaveLength(1)
      wrapper.unmount()
    })

    it('should show all groups when search is empty', async () => {
      const groups = [
        createMockGroup({ id: 1 }),
        createMockGroup({ id: 2 }),
      ]
      vi.mocked(groupApi.getGroups).mockResolvedValue(groups)

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      type VM = { searchQuery: string; filteredGroups: GroupDetail[] }
      const vm = wrapper.vm as unknown as VM

      vm.searchQuery = ''
      await flushPromises()

      expect(vm.filteredGroups).toHaveLength(2)
      wrapper.unmount()
    })
  })

  describe('Click Actions', () => {
    it('should open conversation when group is clicked', async () => {
      const groups = [createMockGroup({ id: 5 })]
      const conversations = [createMockConversation({ id: 10, groupId: 5 })]
      vi.mocked(groupApi.getGroups).mockResolvedValue(groups)
      vi.mocked(conversationApi.getConversations).mockResolvedValue(conversations)

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      // Find and click the group item
      const groupItem = wrapper.findComponent({ name: 'GroupItem' })
      await groupItem.vm.$emit('click')
      await flushPromises()

      expect(conversationApi.getConversations).toHaveBeenCalled()
      expect(wrapper.emitted('open-conversation')).toBeTruthy()
      expect(wrapper.emitted('open-conversation')![0]).toEqual([10])

      wrapper.unmount()
    })

    it('should show warning when group conversation not found', async () => {
      const groups = [createMockGroup({ id: 5 })]
      const conversations: Conversation[] = [] // No matching conversation
      vi.mocked(groupApi.getGroups).mockResolvedValue(groups)
      vi.mocked(conversationApi.getConversations).mockResolvedValue(conversations)

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      const groupItem = wrapper.findComponent({ name: 'GroupItem' })
      await groupItem.vm.$emit('click')
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('Group conversation not found')
      wrapper.unmount()
    })

    it('should handle conversation load error', async () => {
      const groups = [createMockGroup({ id: 5 })]
      vi.mocked(groupApi.getGroups).mockResolvedValue(groups)
      vi.mocked(conversationApi.getConversations).mockRejectedValue(
        new Error('Failed to load conversations')
      )

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      const groupItem = wrapper.findComponent({ name: 'GroupItem' })
      await groupItem.vm.$emit('click')
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to load conversations')
      wrapper.unmount()
    })
  })

  describe('Refresh', () => {
    it('should expose refresh method', async () => {
      vi.mocked(groupApi.getGroups).mockResolvedValue([])

      const wrapper = mount(GroupsList, {
        props: { currentUserId: 1 },
      })
      await flushPromises()

      expect(groupApi.getGroups).toHaveBeenCalledTimes(1)

      // Reset mock and call refresh
      vi.mocked(groupApi.getGroups).mockClear()
      vi.mocked(groupApi.getGroups).mockResolvedValue([createMockGroup()])

      type ExposedMethods = { refresh: () => Promise<void> }
      const exposed = wrapper.vm as unknown as ExposedMethods
      await exposed.refresh()
      await flushPromises()

      expect(groupApi.getGroups).toHaveBeenCalledTimes(1)
      wrapper.unmount()
    })
  })
})
