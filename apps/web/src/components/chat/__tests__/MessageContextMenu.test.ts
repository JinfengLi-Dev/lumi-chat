import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessageBox, ElMessage } from 'element-plus'
import MessageContextMenu from '../MessageContextMenu.vue'
import type { Message } from '@/types'

// Mock Element Plus components
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue(true),
    },
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
  }
})

// Helper to create mock message
function createMockMessage(overrides: Partial<Message> = {}): Message {
  return {
    id: 1,
    msgId: 'msg-1',
    conversationId: 1,
    senderId: 1,
    msgType: 'text',
    content: 'Hello World',
    serverCreatedAt: new Date().toISOString(), // Recent message for recall
    ...overrides,
  }
}

// Create a teleport target for testing
let teleportTarget: HTMLElement

describe('MessageContextMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Reset clipboard mock
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
      writable: true,
    })

    // Create teleport target
    teleportTarget = document.createElement('div')
    teleportTarget.id = 'teleport-target'
    document.body.appendChild(teleportTarget)
  })

  afterEach(() => {
    // Clean up any remaining context menus
    const menus = document.body.querySelectorAll('.context-menu')
    menus.forEach((menu) => menu.remove())
    if (teleportTarget.parentNode) {
      document.body.removeChild(teleportTarget)
    }
  })

  // Helper to find elements in teleported content
  function findInBody(selector: string) {
    return document.body.querySelector(selector)
  }

  function findAllInBody(selector: string) {
    return document.body.querySelectorAll(selector)
  }

  describe('Rendering', () => {
    it('should not render when visible is false', async () => {
      mount(MessageContextMenu, {
        props: {
          visible: false,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      expect(findInBody('.context-menu')).toBeNull()
    })

    it('should not render when message is null', async () => {
      mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: null,
          isSelf: true,
        },
      })

      await flushPromises()
      expect(findInBody('.context-menu')).toBeNull()
    })

    it('should not render when message is recalled', async () => {
      mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage({ recalledAt: new Date().toISOString() }),
          isSelf: true,
        },
      })

      await flushPromises()
      expect(findInBody('.context-menu')).toBeNull()
    })

    it('should render menu at correct position', async () => {
      mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 150,
          y: 200,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      const menu = findInBody('.context-menu') as HTMLElement
      expect(menu).not.toBeNull()
      expect(menu.style.left).toBe('150px')
      expect(menu.style.top).toBe('200px')
    })

    it('should show copy, quote, forward, delete for all messages', async () => {
      mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: false, // Not own message
        },
      })

      await flushPromises()
      const menu = findInBody('.context-menu')
      expect(menu?.textContent).toContain('Copy')
      expect(menu?.textContent).toContain('Quote')
      expect(menu?.textContent).toContain('Forward')
      expect(menu?.textContent).toContain('Delete')
    })

    it('should show recall for own messages within 2 minutes', async () => {
      mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage({ serverCreatedAt: new Date().toISOString() }),
          isSelf: true,
        },
      })

      await flushPromises()
      const menu = findInBody('.context-menu')
      expect(menu?.textContent).toContain('Recall')
    })

    it('should not show recall for other users messages', async () => {
      // Clean up any previous menus first
      const existingMenus = document.body.querySelectorAll('.context-menu')
      existingMenus.forEach((menu) => menu.remove())

      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage({ serverCreatedAt: new Date().toISOString() }),
          isSelf: false,
        },
      })

      await flushPromises()

      // Find the recall item specifically
      const recallItem = findInBody('.context-menu-item.warning')
      expect(recallItem).toBeNull()

      wrapper.unmount()
    })

    it('should not show recall after 2 minutes', async () => {
      // Clean up any previous menus first
      const existingMenus = document.body.querySelectorAll('.context-menu')
      existingMenus.forEach((menu) => menu.remove())

      const oldTime = new Date(Date.now() - 3 * 60 * 1000).toISOString() // 3 minutes ago
      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage({ serverCreatedAt: oldTime }),
          isSelf: true,
        },
      })

      await flushPromises()

      // Find the recall item specifically
      const recallItem = findInBody('.context-menu-item.warning')
      expect(recallItem).toBeNull()

      wrapper.unmount()
    })
  })

  describe('Copy Action', () => {
    it('should copy text message content to clipboard', async () => {
      const testContent = 'Test message to copy'
      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage({ content: testContent }),
          isSelf: true,
        },
      })

      await flushPromises()
      const copyItem = findInBody('.context-menu-item') as HTMLElement
      copyItem.click()
      await flushPromises()

      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(testContent)
      expect(ElMessage.success).toHaveBeenCalledWith('Copied to clipboard')

      wrapper.unmount()
    })

    it('should emit copy and update:visible events', async () => {
      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      const copyItem = findInBody('.context-menu-item') as HTMLElement
      copyItem.click()
      await flushPromises()

      expect(wrapper.emitted('copy')).toHaveLength(1)
      expect(wrapper.emitted('update:visible')).toEqual([[false]])

      wrapper.unmount()
    })

    it('should handle clipboard error', async () => {
      Object.defineProperty(navigator, 'clipboard', {
        value: {
          writeText: vi.fn().mockRejectedValue(new Error('Failed')),
        },
        writable: true,
      })

      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      const copyItem = findInBody('.context-menu-item') as HTMLElement
      copyItem.click()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to copy')

      wrapper.unmount()
    })
  })

  describe('Quote Action', () => {
    it('should emit quote event', async () => {
      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      // Quote is the second item
      const items = findAllInBody('.context-menu-item')
      ;(items[1] as HTMLElement).click()
      await flushPromises()

      expect(wrapper.emitted('quote')).toHaveLength(1)
      expect(wrapper.emitted('update:visible')).toEqual([[false]])

      wrapper.unmount()
    })
  })

  describe('Forward Action', () => {
    it('should emit forward event', async () => {
      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      // Forward is the third item
      const items = findAllInBody('.context-menu-item')
      ;(items[2] as HTMLElement).click()
      await flushPromises()

      expect(wrapper.emitted('forward')).toHaveLength(1)
      expect(wrapper.emitted('update:visible')).toEqual([[false]])

      wrapper.unmount()
    })
  })

  describe('Recall Action', () => {
    it('should show confirmation dialog and emit recall event', async () => {
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')

      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage({ serverCreatedAt: new Date().toISOString() }),
          isSelf: true,
        },
      })

      await flushPromises()
      // Find the recall button (should have warning class)
      const recallItem = findInBody('.context-menu-item.warning') as HTMLElement
      expect(recallItem).not.toBeNull()

      recallItem.click()
      await flushPromises()

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        expect.stringContaining('recall this message'),
        'Recall Message',
        expect.any(Object)
      )
      expect(wrapper.emitted('recall')).toHaveLength(1)

      wrapper.unmount()
    })

    it('should not emit recall when cancelled', async () => {
      vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage({ serverCreatedAt: new Date().toISOString() }),
          isSelf: true,
        },
      })

      await flushPromises()
      const recallItem = findInBody('.context-menu-item.warning') as HTMLElement
      recallItem.click()
      await flushPromises()

      expect(wrapper.emitted('recall')).toBeFalsy()
      expect(wrapper.emitted('update:visible')).toEqual([[false]])

      wrapper.unmount()
    })
  })

  describe('Delete Action', () => {
    it('should show confirmation dialog and emit delete event', async () => {
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')

      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      // Find the delete button (should have danger class)
      const deleteItem = findInBody('.context-menu-item.danger') as HTMLElement
      expect(deleteItem).not.toBeNull()

      deleteItem.click()
      await flushPromises()

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        expect.stringContaining('Delete this message'),
        'Delete Message',
        expect.any(Object)
      )
      expect(wrapper.emitted('delete')).toHaveLength(1)

      wrapper.unmount()
    })

    it('should not emit delete when cancelled', async () => {
      vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

      const wrapper = mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      const deleteItem = findInBody('.context-menu-item.danger') as HTMLElement
      deleteItem.click()
      await flushPromises()

      expect(wrapper.emitted('delete')).toBeFalsy()
      expect(wrapper.emitted('update:visible')).toEqual([[false]])

      wrapper.unmount()
    })
  })

  describe('Menu Item Styling', () => {
    it('should have warning class for recall item', async () => {
      mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage({ serverCreatedAt: new Date().toISOString() }),
          isSelf: true,
        },
      })

      await flushPromises()
      const recallItem = findInBody('.context-menu-item.warning')
      expect(recallItem).not.toBeNull()
      expect(recallItem?.textContent).toContain('Recall')
    })

    it('should have danger class for delete item', async () => {
      mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      const deleteItem = findInBody('.context-menu-item.danger')
      expect(deleteItem).not.toBeNull()
      expect(deleteItem?.textContent).toContain('Delete')
    })

    it('should have divider before delete', async () => {
      mount(MessageContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          message: createMockMessage(),
          isSelf: true,
        },
      })

      await flushPromises()
      expect(findInBody('.context-menu-divider')).not.toBeNull()
    })
  })
})
