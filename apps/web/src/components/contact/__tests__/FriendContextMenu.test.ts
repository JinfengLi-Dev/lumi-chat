import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessageBox, ElMessage } from 'element-plus'
import FriendContextMenu from '../FriendContextMenu.vue'
import { friendApi } from '@/api/friend'
import type { Friend } from '@/types'

// Mock APIs
vi.mock('@/api/friend', () => ({
  friendApi: {
    updateRemark: vi.fn(),
    blockFriend: vi.fn(),
    unblockFriend: vi.fn(),
    deleteFriend: vi.fn(),
  },
}))

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue('confirm'),
    },
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
  }
})

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

// Create teleport target
let teleportTarget: HTMLElement

describe('FriendContextMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    teleportTarget = document.createElement('div')
    teleportTarget.id = 'teleport-target'
    document.body.appendChild(teleportTarget)
  })

  afterEach(() => {
    const menus = document.body.querySelectorAll('.friend-context-menu')
    menus.forEach((menu) => menu.remove())
    if (teleportTarget.parentNode) {
      document.body.removeChild(teleportTarget)
    }
  })

  function findInBody(selector: string) {
    return document.body.querySelector(selector)
  }

  function findAllInBody(selector: string) {
    return document.body.querySelectorAll(selector)
  }

  describe('Rendering', () => {
    it('should not render when visible is false', async () => {
      mount(FriendContextMenu, {
        props: {
          visible: false,
          x: 100,
          y: 100,
          friend: createMockFriend(),
        },
      })

      await flushPromises()
      expect(findInBody('.friend-context-menu')).toBeNull()
    })

    it('should not render when friend is null', async () => {
      mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: null,
        },
      })

      await flushPromises()
      expect(findInBody('.friend-context-menu')).toBeNull()
    })

    it('should render at correct position', async () => {
      mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 150,
          y: 200,
          friend: createMockFriend(),
        },
      })

      await flushPromises()
      const menu = findInBody('.friend-context-menu') as HTMLElement
      expect(menu).not.toBeNull()
      expect(menu.style.left).toBe('150px')
      expect(menu.style.top).toBe('200px')
    })

    it('should show all menu items', async () => {
      mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: createMockFriend(),
        },
      })

      await flushPromises()
      const menu = findInBody('.friend-context-menu')
      expect(menu?.textContent).toContain('Set Remark')
      expect(menu?.textContent).toContain('Block')
      expect(menu?.textContent).toContain('Delete Friend')
    })
  })

  describe('Set Remark Action', () => {
    it('should open remark dialog and save remark', async () => {
      vi.mocked(friendApi.updateRemark).mockResolvedValue()

      const wrapper = mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: createMockFriend({ id: 10, remark: 'Old Remark' }),
        },
      })

      await flushPromises()

      // Click Set Remark
      const setRemarkItem = findInBody('.context-menu-item') as HTMLElement
      setRemarkItem.click()
      await flushPromises()

      // Dialog should be open
      type VM = { showRemarkDialog: boolean; newRemark: string; saveRemark: () => Promise<void> }
      const vm = wrapper.vm as unknown as VM
      expect(vm.showRemarkDialog).toBe(true)
      expect(vm.newRemark).toBe('Old Remark')

      // Update remark and save
      vm.newRemark = 'New Remark'
      await vm.saveRemark()
      await flushPromises()

      expect(friendApi.updateRemark).toHaveBeenCalledWith(10, 'New Remark')
      expect(ElMessage.success).toHaveBeenCalledWith('Remark updated')
      expect(wrapper.emitted('remark-updated')).toBeTruthy()
      expect(wrapper.emitted('remark-updated')![0]).toEqual([10, 'New Remark'])

      wrapper.unmount()
    })

    it('should handle remark update error', async () => {
      vi.mocked(friendApi.updateRemark).mockRejectedValue(new Error('Failed to update'))

      const wrapper = mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: createMockFriend(),
        },
      })

      await flushPromises()

      type VM = { showRemarkDialog: boolean; newRemark: string; saveRemark: () => Promise<void> }
      const vm = wrapper.vm as unknown as VM
      vm.showRemarkDialog = true
      vm.newRemark = 'Test'
      await vm.saveRemark()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to update')
      wrapper.unmount()
    })
  })

  describe('Block Action', () => {
    it('should show confirmation and block friend', async () => {
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')
      vi.mocked(friendApi.blockFriend).mockResolvedValue()

      const wrapper = mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: createMockFriend({ id: 10, nickname: 'John' }),
        },
      })

      await flushPromises()

      // Find and click Block button
      const items = findAllInBody('.context-menu-item')
      const blockItem = Array.from(items).find((i) => i.textContent?.includes('Block')) as HTMLElement
      blockItem.click()
      await flushPromises()

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        expect.stringContaining('block John'),
        'Block Friend',
        expect.any(Object)
      )
      expect(friendApi.blockFriend).toHaveBeenCalledWith(10)
      expect(ElMessage.success).toHaveBeenCalledWith('Friend blocked')
      expect(wrapper.emitted('blocked')).toBeTruthy()
      expect(wrapper.emitted('blocked')![0]).toEqual([10])

      wrapper.unmount()
    })

    it('should not block when cancelled', async () => {
      vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

      const wrapper = mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: createMockFriend(),
        },
      })

      await flushPromises()

      const items = findAllInBody('.context-menu-item')
      const blockItem = Array.from(items).find((i) => i.textContent?.includes('Block')) as HTMLElement
      blockItem.click()
      await flushPromises()

      expect(friendApi.blockFriend).not.toHaveBeenCalled()
      expect(wrapper.emitted('blocked')).toBeFalsy()

      wrapper.unmount()
    })
  })

  describe('Delete Action', () => {
    it('should show confirmation and delete friend', async () => {
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')
      vi.mocked(friendApi.deleteFriend).mockResolvedValue()

      const wrapper = mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: createMockFriend({ id: 10, nickname: 'John' }),
        },
      })

      await flushPromises()

      // Find and click Delete button (has danger class)
      const deleteItem = findInBody('.context-menu-item.danger') as HTMLElement
      deleteItem.click()
      await flushPromises()

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        expect.stringContaining('delete John'),
        'Delete Friend',
        expect.any(Object)
      )
      expect(friendApi.deleteFriend).toHaveBeenCalledWith(10)
      expect(ElMessage.success).toHaveBeenCalledWith('Friend deleted')
      expect(wrapper.emitted('deleted')).toBeTruthy()
      expect(wrapper.emitted('deleted')![0]).toEqual([10])

      wrapper.unmount()
    })

    it('should not delete when cancelled', async () => {
      vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

      const wrapper = mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: createMockFriend(),
        },
      })

      await flushPromises()

      const deleteItem = findInBody('.context-menu-item.danger') as HTMLElement
      deleteItem.click()
      await flushPromises()

      expect(friendApi.deleteFriend).not.toHaveBeenCalled()
      expect(wrapper.emitted('deleted')).toBeFalsy()

      wrapper.unmount()
    })

    it('should handle delete error', async () => {
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')
      vi.mocked(friendApi.deleteFriend).mockRejectedValue(new Error('Failed to delete'))

      const wrapper = mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: createMockFriend(),
        },
      })

      await flushPromises()

      const deleteItem = findInBody('.context-menu-item.danger') as HTMLElement
      deleteItem.click()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to delete')
      expect(wrapper.emitted('deleted')).toBeFalsy()

      wrapper.unmount()
    })
  })

  describe('Close Menu', () => {
    it('should emit update:visible false when action completes', async () => {
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')
      vi.mocked(friendApi.deleteFriend).mockResolvedValue()

      const wrapper = mount(FriendContextMenu, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          friend: createMockFriend(),
        },
      })

      await flushPromises()

      const deleteItem = findInBody('.context-menu-item.danger') as HTMLElement
      deleteItem.click()
      await flushPromises()

      expect(wrapper.emitted('update:visible')).toEqual([[false]])

      wrapper.unmount()
    })
  })
})
