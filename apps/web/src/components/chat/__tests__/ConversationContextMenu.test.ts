import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ConversationContextMenu from '../ConversationContextMenu.vue'
import type { Conversation } from '@/types'

// Helper to create mock conversation
function createMockConversation(overrides: Partial<Conversation> = {}): Conversation {
  return {
    id: 1,
    type: 'private',
    participantIds: [1, 2],
    unreadCount: 0,
    isMuted: false,
    isPinned: false,
    ...overrides,
  }
}

describe('ConversationContextMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    // Clean up teleported content
    const menus = document.body.querySelectorAll('.conversation-context-menu')
    menus.forEach((menu) => menu.remove())
  })

  function findInBody(selector: string) {
    return document.body.querySelector(selector)
  }

  function findAllInBody(selector: string) {
    return document.body.querySelectorAll(selector)
  }

  describe('Rendering', () => {
    it('should not render when visible is false', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: false,
          x: 100,
          y: 100,
          conversation: createMockConversation(),
        },
      })

      await flushPromises()
      expect(findInBody('.conversation-context-menu')).toBeNull()
    })

    it('should render when visible is true', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation(),
        },
      })

      await flushPromises()
      expect(findInBody('.conversation-context-menu')).not.toBeNull()
    })

    it('should position menu at x, y coordinates', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 150,
          y: 200,
          conversation: createMockConversation(),
        },
      })

      await flushPromises()
      const menu = findInBody('.conversation-context-menu') as HTMLElement
      expect(menu.style.left).toBe('150px')
      expect(menu.style.top).toBe('200px')
    })

    it('should not render when conversation is null', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: null,
        },
      })

      await flushPromises()
      expect(findInBody('.conversation-context-menu')).toBeNull()
    })
  })

  describe('Pin/Unpin', () => {
    it('should show "Pin to top" when not pinned', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation({ isPinned: false }),
        },
      })

      await flushPromises()
      const menu = findInBody('.conversation-context-menu')
      expect(menu?.textContent).toContain('Pin to top')
    })

    it('should show "Unpin" when pinned', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation({ isPinned: true }),
        },
      })

      await flushPromises()
      const menu = findInBody('.conversation-context-menu')
      expect(menu?.textContent).toContain('Unpin')
    })

    it('should emit pin event when clicking Pin', async () => {
      const conversation = createMockConversation({ isPinned: false })
      const wrapper = mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation,
        },
      })

      await flushPromises()
      const items = findAllInBody('.menu-item')
      ;(items[0] as HTMLElement).click()
      await flushPromises()

      expect(wrapper.emitted('pin')).toBeTruthy()
      expect(wrapper.emitted('pin')![0]).toEqual([conversation])
    })

    it('should emit unpin event when clicking Unpin', async () => {
      const conversation = createMockConversation({ isPinned: true })
      const wrapper = mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation,
        },
      })

      await flushPromises()
      const items = findAllInBody('.menu-item')
      ;(items[0] as HTMLElement).click()
      await flushPromises()

      expect(wrapper.emitted('unpin')).toBeTruthy()
      expect(wrapper.emitted('unpin')![0]).toEqual([conversation])
    })
  })

  describe('Mute/Unmute', () => {
    it('should show "Mute notifications" when not muted', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation({ isMuted: false }),
        },
      })

      await flushPromises()
      const menu = findInBody('.conversation-context-menu')
      expect(menu?.textContent).toContain('Mute notifications')
    })

    it('should show "Unmute" when muted', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation({ isMuted: true }),
        },
      })

      await flushPromises()
      const menu = findInBody('.conversation-context-menu')
      expect(menu?.textContent).toContain('Unmute')
    })

    it('should emit mute event when clicking Mute', async () => {
      const conversation = createMockConversation({ isMuted: false })
      const wrapper = mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation,
        },
      })

      await flushPromises()
      const items = findAllInBody('.menu-item')
      ;(items[1] as HTMLElement).click()
      await flushPromises()

      expect(wrapper.emitted('mute')).toBeTruthy()
      expect(wrapper.emitted('mute')![0]).toEqual([conversation])
    })

    it('should emit unmute event when clicking Unmute', async () => {
      const conversation = createMockConversation({ isMuted: true })
      const wrapper = mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation,
        },
      })

      await flushPromises()
      const items = findAllInBody('.menu-item')
      ;(items[1] as HTMLElement).click()
      await flushPromises()

      expect(wrapper.emitted('unmute')).toBeTruthy()
      expect(wrapper.emitted('unmute')![0]).toEqual([conversation])
    })
  })

  describe('Mark as Read', () => {
    it('should show "Mark as read" when has unread', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation({ unreadCount: 5 }),
        },
      })

      await flushPromises()
      const menu = findInBody('.conversation-context-menu')
      expect(menu?.textContent).toContain('Mark as read')
    })

    it('should not show "Mark as read" when no unread', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation({ unreadCount: 0 }),
        },
      })

      await flushPromises()
      const menu = findInBody('.conversation-context-menu')
      expect(menu?.textContent).not.toContain('Mark as read')
    })

    it('should emit mark-read event when clicking Mark as read', async () => {
      const conversation = createMockConversation({ unreadCount: 5 })
      const wrapper = mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation,
        },
      })

      await flushPromises()
      const items = findAllInBody('.menu-item')
      // Mark as read is the 3rd item when unread count > 0
      ;(items[2] as HTMLElement).click()
      await flushPromises()

      expect(wrapper.emitted('mark-read')).toBeTruthy()
      expect(wrapper.emitted('mark-read')![0]).toEqual([conversation])
    })
  })

  describe('Delete', () => {
    it('should show delete option', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation(),
        },
      })

      await flushPromises()
      const menu = findInBody('.conversation-context-menu')
      expect(menu?.textContent).toContain('Delete conversation')
    })

    it('should have danger styling on delete', async () => {
      mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation(),
        },
      })

      await flushPromises()
      const dangerItem = findInBody('.menu-item.danger')
      expect(dangerItem).not.toBeNull()
    })

    it('should emit delete event when clicking Delete', async () => {
      const conversation = createMockConversation()
      const wrapper = mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation,
        },
      })

      await flushPromises()
      const dangerItem = findInBody('.menu-item.danger') as HTMLElement
      dangerItem.click()
      await flushPromises()

      expect(wrapper.emitted('delete')).toBeTruthy()
      expect(wrapper.emitted('delete')![0]).toEqual([conversation])
    })
  })

  describe('Close Behavior', () => {
    it('should emit update:visible false after action', async () => {
      const wrapper = mount(ConversationContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          conversation: createMockConversation(),
        },
      })

      await flushPromises()
      const items = findAllInBody('.menu-item')
      ;(items[0] as HTMLElement).click()
      await flushPromises()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
    })
  })
})
