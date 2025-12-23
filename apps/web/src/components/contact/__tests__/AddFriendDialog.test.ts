import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import AddFriendDialog from '../AddFriendDialog.vue'
import { userApi, friendApi, conversationApi } from '@/api'
import type { User, Conversation } from '@/types'
import { ElMessage } from 'element-plus'

// Mock the API modules
vi.mock('@/api', () => ({
  userApi: {
    searchUsers: vi.fn(),
  },
  friendApi: {
    sendFriendRequest: vi.fn(),
  },
  conversationApi: {
    createStrangerConversation: vi.fn(),
  },
}))

// Mock chat store
vi.mock('@/stores/chat', () => ({
  useChatStore: vi.fn(() => ({
    fetchConversations: vi.fn().mockResolvedValue(undefined),
  })),
}))

// Mock router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({
    push: mockPush,
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

// Stub Teleport for dialog testing
config.global.stubs = {
  teleport: true,
}

// Helper to create mock user
function createMockUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    uid: 'user123',
    email: 'test@example.com',
    nickname: 'Test User',
    gender: 'male',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

// Helper to create mock conversation
function createMockConversation(overrides: Partial<Conversation> = {}): Conversation {
  return {
    id: 1,
    type: 'stranger',
    name: 'Test User',
    unreadCount: 0,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

// Type definition for component VM
type AddFriendDialogVM = {
  searchQuery: string
  searchResults: User[]
  selectedUser: User | null
  requestMessage: string
  isSearching: boolean
  isSending: boolean
  handleSearch: () => Promise<void>
  selectUser: (user: User) => void
  clearSelection: () => void
  sendFriendRequest: () => Promise<void>
  sendMessageToStranger: () => Promise<void>
  handleClose: () => void
}

// Helper to mount and open dialog
async function mountAndOpen() {
  const wrapper = mount(AddFriendDialog, {
    props: { modelValue: false },
  })

  await wrapper.setProps({ modelValue: true })
  await flushPromises()

  return wrapper
}

describe('AddFriendDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    mockPush.mockClear()
  })

  describe('Initial State', () => {
    it('should have empty search query initially', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      expect(vm.searchQuery).toBe('')
      wrapper.unmount()
    })

    it('should have empty search results initially', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      expect(vm.searchResults).toEqual([])
      wrapper.unmount()
    })

    it('should have no selected user initially', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      expect(vm.selectedUser).toBeNull()
      wrapper.unmount()
    })

    it('should not be searching initially', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      expect(vm.isSearching).toBe(false)
      wrapper.unmount()
    })
  })

  describe('Search Functionality', () => {
    it('should call userApi.searchUsers when handleSearch is called', async () => {
      vi.mocked(userApi.searchUsers).mockResolvedValue([createMockUser()])

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.searchQuery = 'test@example.com'
      await vm.handleSearch()
      await flushPromises()

      expect(userApi.searchUsers).toHaveBeenCalledWith('test@example.com')
      wrapper.unmount()
    })

    it('should populate searchResults with API response', async () => {
      const users = [
        createMockUser({ id: 1, nickname: 'Alice', uid: 'alice1' }),
        createMockUser({ id: 2, nickname: 'Bob', uid: 'bob2' }),
      ]
      vi.mocked(userApi.searchUsers).mockResolvedValue(users)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.searchQuery = 'test'
      await vm.handleSearch()
      await flushPromises()

      expect(vm.searchResults).toHaveLength(2)
      expect(vm.searchResults[0].nickname).toBe('Alice')
      expect(vm.searchResults[1].nickname).toBe('Bob')
      wrapper.unmount()
    })

    it('should set empty results when search returns nothing', async () => {
      vi.mocked(userApi.searchUsers).mockResolvedValue([])

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.searchQuery = 'nonexistent'
      await vm.handleSearch()
      await flushPromises()

      expect(vm.searchResults).toEqual([])
      wrapper.unmount()
    })

    it('should show error message on search failure', async () => {
      vi.mocked(userApi.searchUsers).mockRejectedValue({
        response: { data: { message: 'Search failed' } },
      })

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.searchQuery = 'test'
      await vm.handleSearch()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Search failed')
      wrapper.unmount()
    })

    it('should not search with empty query', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.searchQuery = '   '
      await vm.handleSearch()
      await flushPromises()

      expect(userApi.searchUsers).not.toHaveBeenCalled()
      expect(vm.searchResults).toEqual([])
      wrapper.unmount()
    })

    it('should trim search query before searching', async () => {
      vi.mocked(userApi.searchUsers).mockResolvedValue([])

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.searchQuery = '  test@example.com  '
      await vm.handleSearch()
      await flushPromises()

      expect(userApi.searchUsers).toHaveBeenCalledWith('test@example.com')
      wrapper.unmount()
    })

    it('should set isSearching to true during search', async () => {
      let resolveSearch: (value: User[]) => void
      vi.mocked(userApi.searchUsers).mockReturnValue(
        new Promise((resolve) => {
          resolveSearch = resolve
        })
      )

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.searchQuery = 'test'
      const searchPromise = vm.handleSearch()

      expect(vm.isSearching).toBe(true)

      resolveSearch!([])
      await searchPromise
      await flushPromises()

      expect(vm.isSearching).toBe(false)
      wrapper.unmount()
    })
  })

  describe('User Selection', () => {
    it('should set selectedUser when selectUser is called', async () => {
      const user = createMockUser({ nickname: 'Selected User' })

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)

      expect(vm.selectedUser).toEqual(user)
      wrapper.unmount()
    })

    it('should clear selection when clearSelection is called', async () => {
      const user = createMockUser()

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      expect(vm.selectedUser).not.toBeNull()

      vm.clearSelection()
      expect(vm.selectedUser).toBeNull()
      wrapper.unmount()
    })

    it('should clear request message when clearSelection is called', async () => {
      const user = createMockUser()

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      vm.requestMessage = 'Hello!'
      vm.clearSelection()

      expect(vm.requestMessage).toBe('')
      wrapper.unmount()
    })
  })

  describe('Send Friend Request', () => {
    it('should send friend request with UID', async () => {
      const user = createMockUser({ uid: 'friend-uid' })
      vi.mocked(friendApi.sendFriendRequest).mockResolvedValue({} as any)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendFriendRequest()
      await flushPromises()

      expect(friendApi.sendFriendRequest).toHaveBeenCalledWith({
        uid: 'friend-uid',
        message: undefined,
      })
      wrapper.unmount()
    })

    it('should send friend request with message', async () => {
      const user = createMockUser({ uid: 'friend-uid' })
      vi.mocked(friendApi.sendFriendRequest).mockResolvedValue({} as any)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      vm.requestMessage = 'Hello, let us be friends!'
      await vm.sendFriendRequest()
      await flushPromises()

      expect(friendApi.sendFriendRequest).toHaveBeenCalledWith({
        uid: 'friend-uid',
        message: 'Hello, let us be friends!',
      })
      wrapper.unmount()
    })

    it('should show success message on successful request', async () => {
      const user = createMockUser()
      vi.mocked(friendApi.sendFriendRequest).mockResolvedValue({} as any)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendFriendRequest()
      await flushPromises()

      expect(ElMessage.success).toHaveBeenCalledWith('Friend request sent')
      wrapper.unmount()
    })

    it('should emit request-sent event on success', async () => {
      const user = createMockUser()
      vi.mocked(friendApi.sendFriendRequest).mockResolvedValue({} as any)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendFriendRequest()
      await flushPromises()

      expect(wrapper.emitted('request-sent')).toBeTruthy()
      wrapper.unmount()
    })

    it('should close dialog on successful request', async () => {
      const user = createMockUser()
      vi.mocked(friendApi.sendFriendRequest).mockResolvedValue({} as any)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendFriendRequest()
      await flushPromises()

      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      expect(wrapper.emitted('update:modelValue')![0]).toEqual([false])
      wrapper.unmount()
    })

    it('should show error message on failed request', async () => {
      const user = createMockUser()
      vi.mocked(friendApi.sendFriendRequest).mockRejectedValue({
        response: { data: { message: 'Already friends' } },
      })

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendFriendRequest()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Already friends')
      wrapper.unmount()
    })

    it('should not send request when no user selected', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      await vm.sendFriendRequest()
      await flushPromises()

      expect(friendApi.sendFriendRequest).not.toHaveBeenCalled()
      wrapper.unmount()
    })

    it('should set isSending during request', async () => {
      const user = createMockUser()
      let resolveRequest: () => void
      vi.mocked(friendApi.sendFriendRequest).mockReturnValue(
        new Promise((resolve) => {
          resolveRequest = resolve
        })
      )

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      const requestPromise = vm.sendFriendRequest()

      expect(vm.isSending).toBe(true)

      resolveRequest!()
      await requestPromise
      await flushPromises()

      expect(vm.isSending).toBe(false)
      wrapper.unmount()
    })
  })

  describe('Stranger Chat', () => {
    it('should create stranger conversation', async () => {
      const user = createMockUser({ id: 123 })
      const conversation = createMockConversation({ id: 456 })
      vi.mocked(conversationApi.createStrangerConversation).mockResolvedValue(conversation)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendMessageToStranger()
      await flushPromises()

      expect(conversationApi.createStrangerConversation).toHaveBeenCalledWith(123)
      wrapper.unmount()
    })

    it('should navigate to conversation after creation', async () => {
      const user = createMockUser({ id: 123 })
      const conversation = createMockConversation({ id: 456 })
      vi.mocked(conversationApi.createStrangerConversation).mockResolvedValue(conversation)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendMessageToStranger()
      await flushPromises()

      expect(mockPush).toHaveBeenCalledWith('/chat/456')
      wrapper.unmount()
    })

    it('should close dialog after navigating', async () => {
      const user = createMockUser({ id: 123 })
      const conversation = createMockConversation({ id: 456 })
      vi.mocked(conversationApi.createStrangerConversation).mockResolvedValue(conversation)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendMessageToStranger()
      await flushPromises()

      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      wrapper.unmount()
    })

    it('should show error on stranger conversation failure', async () => {
      const user = createMockUser()
      vi.mocked(conversationApi.createStrangerConversation).mockRejectedValue({
        response: { data: { message: 'Failed to create conversation' } },
      })

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendMessageToStranger()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to create conversation')
      wrapper.unmount()
    })

    it('should not create conversation when no user selected', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      await vm.sendMessageToStranger()
      await flushPromises()

      expect(conversationApi.createStrangerConversation).not.toHaveBeenCalled()
      wrapper.unmount()
    })
  })

  describe('Dialog Close', () => {
    it('should reset state when handleClose is called', async () => {
      const user = createMockUser()
      vi.mocked(userApi.searchUsers).mockResolvedValue([user])

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      // Set up some state
      vm.searchQuery = 'test'
      await vm.handleSearch()
      vm.selectUser(user)
      vm.requestMessage = 'Hello'

      // Close dialog
      vm.handleClose()

      expect(vm.searchQuery).toBe('')
      expect(vm.searchResults).toEqual([])
      expect(vm.selectedUser).toBeNull()
      expect(vm.requestMessage).toBe('')
      wrapper.unmount()
    })

    it('should emit update:modelValue false when handleClose is called', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.handleClose()

      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      expect(wrapper.emitted('update:modelValue')![0]).toEqual([false])
      wrapper.unmount()
    })
  })

  describe('Error Handling', () => {
    it('should show default error message when no message in response', async () => {
      vi.mocked(userApi.searchUsers).mockRejectedValue({
        response: { data: {} },
      })

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.searchQuery = 'test'
      await vm.handleSearch()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Search failed')
      wrapper.unmount()
    })

    it('should show default error for friend request failure', async () => {
      const user = createMockUser()
      vi.mocked(friendApi.sendFriendRequest).mockRejectedValue({
        response: { data: {} },
      })

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendFriendRequest()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to send request')
      wrapper.unmount()
    })

    it('should show default error for stranger conversation failure', async () => {
      const user = createMockUser()
      vi.mocked(conversationApi.createStrangerConversation).mockRejectedValue({
        response: { data: {} },
      })

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as AddFriendDialogVM

      vm.selectUser(user)
      await vm.sendMessageToStranger()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to start conversation')
      wrapper.unmount()
    })
  })
})
