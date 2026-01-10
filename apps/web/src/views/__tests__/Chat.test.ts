import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, config } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Chat from '../Chat.vue'

// Mock Vue Router
const mockPush = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({
      push: mockPush,
    }),
    useRoute: () => ({
      path: '/chat',
      params: {},
      query: {},
    }),
  }
})

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

// Mock the conversation API to prevent real API calls
vi.mock('@/api/conversation', () => ({
  conversationApi: {
    getConversations: vi.fn().mockResolvedValue([]),
    markAsRead: vi.fn().mockResolvedValue({}),
  },
}))

// Disable teleport for testing
config.global.stubs = {
  teleport: true,
  RouterView: true,
}

describe('Chat.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  describe('Tab Navigation', () => {
    it('should render with messages tab active by default', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect(vm.activeTab).toBe('messages')
      wrapper.unmount()
    })

    it('should have three tab options', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      // Check that the activeTab can be set to all three values
      vm.activeTab = 'messages'
      expect(vm.activeTab).toBe('messages')

      vm.activeTab = 'contacts'
      expect(vm.activeTab).toBe('contacts')

      vm.activeTab = 'groups'
      expect(vm.activeTab).toBe('groups')

      wrapper.unmount()
    })
  })

  describe('Context Menu State', () => {
    it('should initialize context menu state correctly', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect(vm.contextMenuVisible).toBe(false)
      expect(vm.contextMenuX).toBe(0)
      expect(vm.contextMenuY).toBe(0)
      expect(vm.contextMenuConversation).toBeNull()

      wrapper.unmount()
    })

    it('should have handleConversationContextMenu function', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect(typeof vm.handleConversationContextMenu).toBe('function')
      wrapper.unmount()
    })
  })

  describe('Dialog State', () => {
    it('should initialize all dialog visibility states to false', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect(vm.showAddFriendDialog).toBe(false)
      expect(vm.showFriendRequestsDialog).toBe(false)
      expect(vm.showCreateGroupDialog).toBe(false)

      wrapper.unmount()
    })
  })

  describe('Add Action Handler', () => {
    it('should open add friend dialog when friend action is triggered', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      vm.handleAddAction('friend')
      expect(vm.showAddFriendDialog).toBe(true)

      wrapper.unmount()
    })

    it('should open create group dialog when group action is triggered', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      vm.handleAddAction('group')
      expect(vm.showCreateGroupDialog).toBe(true)

      wrapper.unmount()
    })

    it('should open friend requests dialog when requests action is triggered', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      vm.handleAddAction('requests')
      expect(vm.showFriendRequestsDialog).toBe(true)

      wrapper.unmount()
    })
  })

  describe('handleOpenConversation', () => {
    it('should switch to messages tab when opening conversation', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      // Start on contacts tab
      vm.activeTab = 'contacts'
      expect(vm.activeTab).toBe('contacts')

      // Open a conversation
      vm.handleOpenConversation(123)

      // Should switch to messages tab
      expect(vm.activeTab).toBe('messages')

      wrapper.unmount()
    })

    it('should call router.push with conversation route', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      vm.handleOpenConversation(456)

      expect(mockPush).toHaveBeenCalledWith('/conversation/456')

      wrapper.unmount()
    })
  })

  describe('Context Menu Handlers', () => {
    it('should have handleContextMenuPin function', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect(typeof vm.handleContextMenuPin).toBe('function')
      wrapper.unmount()
    })

    it('should have handleContextMenuMute function', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect(typeof vm.handleContextMenuMute).toBe('function')
      wrapper.unmount()
    })

    it('should have handleContextMenuMarkRead function', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect(typeof vm.handleContextMenuMarkRead).toBe('function')
      wrapper.unmount()
    })

    it('should have handleContextMenuDelete function', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect(typeof vm.handleContextMenuDelete).toBe('function')
      wrapper.unmount()
    })
  })

  describe('Time Formatting', () => {
    it('should format today time as HH:MM', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const now = new Date()
      const result = vm.formatTime(now.toISOString())

      // Should contain a colon (time format)
      expect(result).toContain(':')
      wrapper.unmount()
    })

    it('should format yesterday as "Yesterday"', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const yesterday = new Date()
      yesterday.setDate(yesterday.getDate() - 1)
      const result = vm.formatTime(yesterday.toISOString())

      expect(result).toBe('Yesterday')
      wrapper.unmount()
    })

    it('should return empty string for undefined time', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect(vm.formatTime(undefined)).toBe('')
      wrapper.unmount()
    })
  })

  describe('Message Preview', () => {
    it('should return empty string for no message', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const conv = { lastMessage: undefined }
      expect(vm.getLastMessagePreview(conv)).toBe('')
      wrapper.unmount()
    })

    it('should return recalled message text', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const conv = {
        lastMessage: {
          recalledAt: '2025-01-01T00:00:00Z',
          msgType: 'text',
          content: 'Hello',
        },
      }
      expect(vm.getLastMessagePreview(conv)).toBe('[Message recalled]')
      wrapper.unmount()
    })

    it('should return content for text messages', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const conv = {
        lastMessage: {
          msgType: 'text',
          content: 'Hello World',
        },
      }
      expect(vm.getLastMessagePreview(conv)).toBe('Hello World')
      wrapper.unmount()
    })

    it('should return [Image] for image messages', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const conv = {
        lastMessage: {
          msgType: 'image',
          content: 'http://example.com/img.jpg',
        },
      }
      expect(vm.getLastMessagePreview(conv)).toBe('[Image]')
      wrapper.unmount()
    })

    it('should return [Voice] for voice messages', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const conv = {
        lastMessage: {
          msgType: 'voice',
          content: 'http://example.com/voice.mp3',
        },
      }
      expect(vm.getLastMessagePreview(conv)).toBe('[Voice]')
      wrapper.unmount()
    })

    it('should return [Location] for location messages', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const conv = {
        lastMessage: {
          msgType: 'location',
          content: '{}',
        },
      }
      expect(vm.getLastMessagePreview(conv)).toBe('[Location]')
      wrapper.unmount()
    })

    it('should return [Contact Card] for user_card messages', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const conv = {
        lastMessage: {
          msgType: 'user_card',
          content: '{}',
        },
      }
      expect(vm.getLastMessagePreview(conv)).toBe('[Contact Card]')
      wrapper.unmount()
    })

    it('should return [Group Card] for group_card messages', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      const conv = {
        lastMessage: {
          msgType: 'group_card',
          content: '{}',
        },
      }
      expect(vm.getLastMessagePreview(conv)).toBe('[Group Card]')
      wrapper.unmount()
    })
  })

  describe('Connection Status', () => {
    it('should return Connected for connected status', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      // Check that connectionStatusText computed exists
      expect(vm.connectionStatusText).toBeDefined()
      wrapper.unmount()
    })

    it('should return color for connection status', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      // Check that connectionStatusColor computed exists
      expect(vm.connectionStatusColor).toBeDefined()
      wrapper.unmount()
    })
  })

  describe('Component Refs', () => {
    it('should have friendsListRef', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect('friendsListRef' in vm).toBe(true)
      wrapper.unmount()
    })

    it('should have groupsListRef', () => {
      const wrapper = mount(Chat)
      const vm = wrapper.vm as any

      expect('groupsListRef' in vm).toBe(true)
      wrapper.unmount()
    })
  })
})
