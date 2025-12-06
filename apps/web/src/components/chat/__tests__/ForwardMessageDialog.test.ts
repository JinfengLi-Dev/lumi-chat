import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import ForwardMessageDialog from '../ForwardMessageDialog.vue'
import { conversationApi } from '@/api'
import type { Message, Conversation, User, Group } from '@/types'

// Mock the conversation API
vi.mock('@/api', () => ({
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

// Helper to create mock user
function createMockUser(overrides: Partial<User> = {}): User {
  return {
    id: 2,
    uid: 'user-2',
    email: 'test@example.com',
    nickname: 'Test User',
    gender: 'male',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  }
}

// Helper to create mock group
function createMockGroup(overrides: Partial<Group> = {}): Group {
  return {
    id: 1,
    name: 'Test Group',
    ownerId: 1,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  }
}

// Helper to create mock message
function createMockMessage(overrides: Partial<Message> = {}): Message {
  return {
    id: 1,
    msgId: 'msg-1',
    conversationId: 1,
    senderId: 1,
    msgType: 'text',
    content: 'Hello World',
    serverCreatedAt: new Date().toISOString(),
    ...overrides,
  }
}

// Helper to create mock conversation
function createMockConversation(overrides: Partial<Conversation> = {}): Conversation {
  return {
    id: 1,
    type: 'private',
    unreadCount: 0,
    targetUser: createMockUser(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  }
}

// Helper to mount and trigger dialog visibility
async function mountAndOpen(
  props: { message: Message | null; modelValue?: boolean },
  conversations: Conversation[] = []
) {
  vi.mocked(conversationApi.getConversations).mockResolvedValue(conversations)

  const wrapper = mount(ForwardMessageDialog, {
    props: {
      modelValue: false,
      message: props.message,
    },
  })

  // Open the dialog to trigger the watcher
  await wrapper.setProps({ modelValue: true })
  await flushPromises()

  return wrapper
}

describe('ForwardMessageDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Component Initialization', () => {
    it('should load conversations when dialog becomes visible', async () => {
      const mockConversations = [createMockConversation()]
      vi.mocked(conversationApi.getConversations).mockResolvedValue(mockConversations)

      const wrapper = mount(ForwardMessageDialog, {
        props: {
          modelValue: false,
          message: createMockMessage(),
        },
      })

      // Initially not visible, should not load
      expect(conversationApi.getConversations).not.toHaveBeenCalled()

      // Make visible
      await wrapper.setProps({ modelValue: true })
      await flushPromises()

      expect(conversationApi.getConversations).toHaveBeenCalled()
      wrapper.unmount()
    })

    it('should handle API error when loading conversations', async () => {
      vi.mocked(conversationApi.getConversations).mockRejectedValue(new Error('Network error'))

      const wrapper = mount(ForwardMessageDialog, {
        props: {
          modelValue: false,
          message: createMockMessage(),
        },
      })

      await wrapper.setProps({ modelValue: true })
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to load conversations')
      wrapper.unmount()
    })
  })

  describe('Message Preview Computation', () => {
    it('should compute text message preview', async () => {
      const wrapper = await mountAndOpen({ message: createMockMessage({ content: 'Test message' }) })

      const vm = wrapper.vm as unknown as { messagePreview: string }
      expect(vm.messagePreview).toBe('Test message')
      wrapper.unmount()
    })

    it('should truncate long text message preview', async () => {
      const longContent = 'A'.repeat(100)
      const wrapper = await mountAndOpen({ message: createMockMessage({ content: longContent }) })

      const vm = wrapper.vm as unknown as { messagePreview: string }
      expect(vm.messagePreview).toBe('A'.repeat(50) + '...')
      wrapper.unmount()
    })

    it('should return [Image] for image messages', async () => {
      const wrapper = await mountAndOpen({
        message: createMockMessage({ msgType: 'image', content: 'http://example.com/image.jpg' }),
      })

      const vm = wrapper.vm as unknown as { messagePreview: string }
      expect(vm.messagePreview).toBe('[Image]')
      wrapper.unmount()
    })

    it('should return [File] with filename for file messages', async () => {
      const wrapper = await mountAndOpen({
        message: createMockMessage({
          msgType: 'file',
          content: 'http://example.com/file.pdf',
          metadata: { fileName: 'document.pdf' },
        }),
      })

      const vm = wrapper.vm as unknown as { messagePreview: string }
      expect(vm.messagePreview).toBe('[File] document.pdf')
      wrapper.unmount()
    })

    it('should return [Voice Message] for voice messages', async () => {
      const wrapper = await mountAndOpen({
        message: createMockMessage({ msgType: 'voice' }),
      })

      const vm = wrapper.vm as unknown as { messagePreview: string }
      expect(vm.messagePreview).toBe('[Voice Message]')
      wrapper.unmount()
    })

    it('should return [Video] for video messages', async () => {
      const wrapper = await mountAndOpen({
        message: createMockMessage({ msgType: 'video' }),
      })

      const vm = wrapper.vm as unknown as { messagePreview: string }
      expect(vm.messagePreview).toBe('[Video]')
      wrapper.unmount()
    })

    it('should return [Location] for location messages', async () => {
      const wrapper = await mountAndOpen({
        message: createMockMessage({ msgType: 'location' }),
      })

      const vm = wrapper.vm as unknown as { messagePreview: string }
      expect(vm.messagePreview).toBe('[Location]')
      wrapper.unmount()
    })

    it('should return empty string when message is null', async () => {
      const wrapper = await mountAndOpen({ message: null })

      const vm = wrapper.vm as unknown as { messagePreview: string }
      expect(vm.messagePreview).toBe('')
      wrapper.unmount()
    })
  })

  describe('Conversation Filtering', () => {
    it('should filter conversations by search query', async () => {
      const mockConversations = [
        createMockConversation({ id: 1, targetUser: createMockUser({ nickname: 'Alice' }) }),
        createMockConversation({ id: 2, targetUser: createMockUser({ id: 3, nickname: 'Bob' }) }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        searchQuery: string
        filteredConversations: Conversation[]
      }
      const vm = wrapper.vm as unknown as VM

      // Initially all conversations
      expect(vm.filteredConversations).toHaveLength(2)

      // Search for Alice
      vm.searchQuery = 'Alice'
      await flushPromises()

      expect(vm.filteredConversations).toHaveLength(1)
      expect(vm.filteredConversations[0].targetUser?.nickname).toBe('Alice')

      wrapper.unmount()
    })

    it('should filter case insensitively', async () => {
      const mockConversations = [
        createMockConversation({ id: 1, targetUser: createMockUser({ nickname: 'Alice' }) }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        searchQuery: string
        filteredConversations: Conversation[]
      }
      const vm = wrapper.vm as unknown as VM
      vm.searchQuery = 'alice'
      await flushPromises()

      expect(vm.filteredConversations).toHaveLength(1)
      wrapper.unmount()
    })

    it('should filter group conversations by group name', async () => {
      const mockConversations = [
        createMockConversation({
          id: 1,
          type: 'group',
          targetUser: undefined,
          group: createMockGroup({ name: 'Dev Team' }),
        }),
        createMockConversation({
          id: 2,
          type: 'group',
          targetUser: undefined,
          group: createMockGroup({ id: 2, name: 'Marketing' }),
        }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        searchQuery: string
        filteredConversations: Conversation[]
      }
      const vm = wrapper.vm as unknown as VM
      vm.searchQuery = 'Dev'
      await flushPromises()

      expect(vm.filteredConversations).toHaveLength(1)
      expect(vm.filteredConversations[0].group?.name).toBe('Dev Team')

      wrapper.unmount()
    })

    it('should return all conversations when search is empty', async () => {
      const mockConversations = [
        createMockConversation({ id: 1 }),
        createMockConversation({ id: 2, targetUser: createMockUser({ id: 3 }) }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        searchQuery: string
        filteredConversations: Conversation[]
      }
      const vm = wrapper.vm as unknown as VM
      vm.searchQuery = ''
      await flushPromises()

      expect(vm.filteredConversations).toHaveLength(2)
      wrapper.unmount()
    })
  })

  describe('Conversation Selection', () => {
    it('should toggle conversation selection', async () => {
      const mockConversations = [createMockConversation({ id: 1 })]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        selectedConversations: number[]
        toggleConversation: (id: number) => void
      }
      const vm = wrapper.vm as unknown as VM

      // Initially empty
      expect(vm.selectedConversations).toEqual([])

      // Select
      vm.toggleConversation(1)
      await flushPromises()
      expect(vm.selectedConversations).toEqual([1])

      // Deselect
      vm.toggleConversation(1)
      await flushPromises()
      expect(vm.selectedConversations).toEqual([])

      wrapper.unmount()
    })

    it('should allow multiple selections', async () => {
      const mockConversations = [
        createMockConversation({ id: 1 }),
        createMockConversation({ id: 2, targetUser: createMockUser({ id: 3 }) }),
        createMockConversation({ id: 3, targetUser: createMockUser({ id: 4 }) }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        selectedConversations: number[]
        toggleConversation: (id: number) => void
      }
      const vm = wrapper.vm as unknown as VM

      vm.toggleConversation(1)
      vm.toggleConversation(3)
      await flushPromises()

      expect(vm.selectedConversations).toEqual([1, 3])
      wrapper.unmount()
    })
  })

  describe('Forward Action', () => {
    it('should warn when no conversation selected', async () => {
      const wrapper = await mountAndOpen({ message: createMockMessage() }, [createMockConversation()])

      type VM = {
        handleForward: () => Promise<void>
      }
      const vm = wrapper.vm as unknown as VM
      await vm.handleForward()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('Please select at least one conversation')
      expect(wrapper.emitted('forward')).toBeFalsy()
      wrapper.unmount()
    })

    it('should emit forward event with selected conversation IDs', async () => {
      const mockConversations = [
        createMockConversation({ id: 1 }),
        createMockConversation({ id: 2, targetUser: createMockUser({ id: 3 }) }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        selectedConversations: number[]
        toggleConversation: (id: number) => void
        handleForward: () => Promise<void>
      }
      const vm = wrapper.vm as unknown as VM

      vm.toggleConversation(1)
      vm.toggleConversation(2)
      await vm.handleForward()
      await flushPromises()

      expect(wrapper.emitted('forward')).toBeTruthy()
      expect(wrapper.emitted('forward')![0]).toEqual([[1, 2]])
      expect(ElMessage.success).toHaveBeenCalledWith('Message forwarded to 2 conversation(s)')
      wrapper.unmount()
    })

    it('should close dialog after forward', async () => {
      const mockConversations = [createMockConversation({ id: 1 })]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        toggleConversation: (id: number) => void
        handleForward: () => Promise<void>
      }
      const vm = wrapper.vm as unknown as VM

      vm.toggleConversation(1)
      await vm.handleForward()
      await flushPromises()

      expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
      wrapper.unmount()
    })
  })

  describe('Close/Cancel Action', () => {
    it('should emit update:modelValue false when closed', async () => {
      const wrapper = await mountAndOpen({ message: createMockMessage() })

      type VM = {
        handleClose: () => void
      }
      const vm = wrapper.vm as unknown as VM
      vm.handleClose()
      await flushPromises()

      expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
      wrapper.unmount()
    })

    it('should reset selection when closed', async () => {
      const mockConversations = [createMockConversation({ id: 1 })]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        selectedConversations: number[]
        toggleConversation: (id: number) => void
        handleClose: () => void
      }
      const vm = wrapper.vm as unknown as VM

      vm.toggleConversation(1)
      expect(vm.selectedConversations).toEqual([1])

      vm.handleClose()
      await flushPromises()

      expect(vm.selectedConversations).toEqual([])
      wrapper.unmount()
    })

    it('should reset search query when closed', async () => {
      const wrapper = await mountAndOpen({ message: createMockMessage() })

      type VM = {
        searchQuery: string
        handleClose: () => void
      }
      const vm = wrapper.vm as unknown as VM

      vm.searchQuery = 'test'
      vm.handleClose()
      await flushPromises()

      expect(vm.searchQuery).toBe('')
      wrapper.unmount()
    })
  })

  describe('Helper Functions', () => {
    it('should get conversation name for private conversation', async () => {
      const mockConversations = [
        createMockConversation({ targetUser: createMockUser({ nickname: 'John Doe' }) }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        getConversationName: (conv: Conversation) => string
      }
      const vm = wrapper.vm as unknown as VM

      expect(vm.getConversationName(mockConversations[0])).toBe('John Doe')
      wrapper.unmount()
    })

    it('should get conversation name for group conversation', async () => {
      const mockConversations = [
        createMockConversation({
          type: 'group',
          targetUser: undefined,
          group: createMockGroup({ name: 'Team Chat' }),
        }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        getConversationName: (conv: Conversation) => string
      }
      const vm = wrapper.vm as unknown as VM

      expect(vm.getConversationName(mockConversations[0])).toBe('Team Chat')
      wrapper.unmount()
    })

    it('should return Unknown for conversation without name', async () => {
      const mockConversations = [
        createMockConversation({
          targetUser: undefined,
          group: undefined,
        }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        getConversationName: (conv: Conversation) => string
      }
      const vm = wrapper.vm as unknown as VM

      expect(vm.getConversationName(mockConversations[0])).toBe('Unknown')
      wrapper.unmount()
    })

    it('should get avatar for private conversation', async () => {
      const mockConversations = [
        createMockConversation({
          targetUser: createMockUser({ avatar: 'http://example.com/avatar.jpg' }),
        }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        getConversationAvatar: (conv: Conversation) => string | undefined
      }
      const vm = wrapper.vm as unknown as VM

      expect(vm.getConversationAvatar(mockConversations[0])).toBe('http://example.com/avatar.jpg')
      wrapper.unmount()
    })

    it('should get avatar for group conversation', async () => {
      const mockConversations = [
        createMockConversation({
          type: 'group',
          targetUser: undefined,
          group: createMockGroup({ avatar: 'http://example.com/group.jpg' }),
        }),
      ]
      const wrapper = await mountAndOpen({ message: createMockMessage() }, mockConversations)

      type VM = {
        getConversationAvatar: (conv: Conversation) => string | undefined
      }
      const vm = wrapper.vm as unknown as VM

      expect(vm.getConversationAvatar(mockConversations[0])).toBe('http://example.com/group.jpg')
      wrapper.unmount()
    })
  })
})
