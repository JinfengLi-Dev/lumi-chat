import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import FriendsList from '../FriendsList.vue'
import { friendApi } from '@/api/friend'
import { conversationApi } from '@/api/conversation'
import type { Friend, Conversation } from '@/types'

// Mock the APIs
vi.mock('@/api/friend', () => ({
  friendApi: {
    getFriends: vi.fn(),
  },
}))

vi.mock('@/api/conversation', () => ({
  conversationApi: {
    createPrivateConversation: vi.fn(),
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

// Helper to create mock friend
function createMockFriend(overrides: Partial<Friend> = {}): Friend {
  return {
    id: 10,
    uid: 'friend-1',
    email: 'friend@example.com',
    nickname: 'Friend 1',
    avatar: undefined,
    gender: 'male',
    status: 'active',
    createdAt: new Date().toISOString(),
    friendshipCreatedAt: new Date().toISOString(),
    ...overrides,
  }
}

// Helper to create mock conversation
function createMockConversation(overrides: Partial<Conversation> = {}): Conversation {
  return {
    id: 1,
    type: 'private',
    participantIds: [1, 10],
    unreadCount: 0,
    isMuted: false,
    isPinned: false,
    ...overrides,
  }
}

describe('FriendsList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Loading', () => {
    it('should load friends on mount', async () => {
      const friends = [createMockFriend()]
      vi.mocked(friendApi.getFriends).mockResolvedValue(friends)

      const wrapper = mount(FriendsList)
      await flushPromises()

      expect(friendApi.getFriends).toHaveBeenCalled()
      wrapper.unmount()
    })

    it('should handle loading error', async () => {
      vi.mocked(friendApi.getFriends).mockRejectedValue(new Error('Network error'))

      const wrapper = mount(FriendsList)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Network error')
      wrapper.unmount()
    })

    it('should show empty state when no friends', async () => {
      vi.mocked(friendApi.getFriends).mockResolvedValue([])

      const wrapper = mount(FriendsList)
      await flushPromises()

      expect(wrapper.find('.empty-state').exists()).toBe(true)
      wrapper.unmount()
    })
  })

  describe('Filtering', () => {
    it('should filter friends by nickname', async () => {
      const friends = [
        createMockFriend({ id: 10, nickname: 'Alice' }),
        createMockFriend({ id: 20, nickname: 'Bob' }),
      ]
      vi.mocked(friendApi.getFriends).mockResolvedValue(friends)

      const wrapper = mount(FriendsList)
      await flushPromises()

      type VM = { searchQuery: string; filteredFriends: Friend[] }
      const vm = wrapper.vm as unknown as VM

      expect(vm.filteredFriends).toHaveLength(2)

      vm.searchQuery = 'Alice'
      await flushPromises()

      expect(vm.filteredFriends).toHaveLength(1)
      expect(vm.filteredFriends[0].nickname).toBe('Alice')

      wrapper.unmount()
    })

    it('should filter friends by remark', async () => {
      const friends = [
        createMockFriend({ id: 10, nickname: 'John', remark: 'My Boss' }),
      ]
      vi.mocked(friendApi.getFriends).mockResolvedValue(friends)

      const wrapper = mount(FriendsList)
      await flushPromises()

      type VM = { searchQuery: string; filteredFriends: Friend[] }
      const vm = wrapper.vm as unknown as VM

      vm.searchQuery = 'Boss'
      await flushPromises()

      expect(vm.filteredFriends).toHaveLength(1)
      wrapper.unmount()
    })

    it('should be case insensitive', async () => {
      const friends = [
        createMockFriend({ id: 10, nickname: 'ALICE' }),
      ]
      vi.mocked(friendApi.getFriends).mockResolvedValue(friends)

      const wrapper = mount(FriendsList)
      await flushPromises()

      type VM = { searchQuery: string; filteredFriends: Friend[] }
      const vm = wrapper.vm as unknown as VM

      vm.searchQuery = 'alice'
      await flushPromises()

      expect(vm.filteredFriends).toHaveLength(1)
      wrapper.unmount()
    })

    it('should show all friends when search is empty', async () => {
      const friends = [
        createMockFriend({ id: 10 }),
        createMockFriend({ id: 20 }),
      ]
      vi.mocked(friendApi.getFriends).mockResolvedValue(friends)

      const wrapper = mount(FriendsList)
      await flushPromises()

      type VM = { searchQuery: string; filteredFriends: Friend[] }
      const vm = wrapper.vm as unknown as VM

      vm.searchQuery = ''
      await flushPromises()

      expect(vm.filteredFriends).toHaveLength(2)
      wrapper.unmount()
    })
  })

  describe('Click Actions', () => {
    it('should open conversation when friend is clicked', async () => {
      const friends = [createMockFriend({ id: 10 })]
      const conversation = createMockConversation({ id: 5 })
      vi.mocked(friendApi.getFriends).mockResolvedValue(friends)
      vi.mocked(conversationApi.createPrivateConversation).mockResolvedValue(conversation)

      const wrapper = mount(FriendsList)
      await flushPromises()

      // Find and click the friend item
      const friendItem = wrapper.findComponent({ name: 'FriendItem' })
      await friendItem.vm.$emit('click')
      await flushPromises()

      expect(conversationApi.createPrivateConversation).toHaveBeenCalledWith(10)
      expect(wrapper.emitted('open-conversation')).toBeTruthy()
      expect(wrapper.emitted('open-conversation')![0]).toEqual([5])

      wrapper.unmount()
    })

    it('should handle conversation error', async () => {
      const friends = [createMockFriend({ id: 10 })]
      vi.mocked(friendApi.getFriends).mockResolvedValue(friends)
      vi.mocked(conversationApi.createPrivateConversation).mockRejectedValue(
        new Error('Failed to create conversation')
      )

      const wrapper = mount(FriendsList)
      await flushPromises()

      const friendItem = wrapper.findComponent({ name: 'FriendItem' })
      await friendItem.vm.$emit('click')
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to create conversation')
      wrapper.unmount()
    })
  })

  describe('Context Menu', () => {
    it('should emit context-menu event', async () => {
      const friends = [createMockFriend({ id: 10 })]
      vi.mocked(friendApi.getFriends).mockResolvedValue(friends)

      const wrapper = mount(FriendsList)
      await flushPromises()

      const friendItem = wrapper.findComponent({ name: 'FriendItem' })
      const mockEvent = new MouseEvent('contextmenu')
      await friendItem.vm.$emit('context-menu', mockEvent)

      expect(wrapper.emitted('context-menu')).toBeTruthy()
      expect(wrapper.emitted('context-menu')![0][0]).toEqual(friends[0])
      expect(wrapper.emitted('context-menu')![0][1]).toBe(mockEvent)

      wrapper.unmount()
    })
  })

  describe('Refresh', () => {
    it('should expose refresh method', async () => {
      vi.mocked(friendApi.getFriends).mockResolvedValue([])

      const wrapper = mount(FriendsList)
      await flushPromises()

      expect(friendApi.getFriends).toHaveBeenCalledTimes(1)

      // Reset mock and call refresh
      vi.mocked(friendApi.getFriends).mockClear()
      vi.mocked(friendApi.getFriends).mockResolvedValue([createMockFriend()])

      type ExposedMethods = { refresh: () => Promise<void> }
      const exposed = wrapper.vm as unknown as ExposedMethods
      await exposed.refresh()
      await flushPromises()

      expect(friendApi.getFriends).toHaveBeenCalledTimes(1)
      wrapper.unmount()
    })
  })
})
