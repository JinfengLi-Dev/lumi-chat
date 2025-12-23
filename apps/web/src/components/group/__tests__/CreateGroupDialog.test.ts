import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import CreateGroupDialog from '../CreateGroupDialog.vue'
import { friendApi, groupApi, fileApi } from '@/api'
import type { Friend } from '@/types'
import type { GroupDetail } from '@/api/group'
import { ElMessage } from 'element-plus'

// Mock the API modules
vi.mock('@/api', () => ({
  friendApi: {
    getFriends: vi.fn(),
  },
  groupApi: {
    createGroup: vi.fn(),
  },
  fileApi: {
    uploadAvatar: vi.fn(),
  },
}))

// Mock ElMessage
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  }
})

// Stub Teleport for dialog testing
config.global.stubs = {
  teleport: true,
}

// Helper to create mock friend
function createMockFriend(overrides: Partial<Friend> = {}): Friend {
  return {
    id: 1,
    friendId: 2,
    uid: 'friend123',
    email: 'friend@example.com',
    nickname: 'Test Friend',
    avatar: 'http://example.com/avatar.jpg',
    status: 'accepted',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

// Helper to create mock group
function createMockGroup(overrides: Partial<GroupDetail> = {}): GroupDetail {
  return {
    id: 1,
    name: 'Test Group',
    ownerId: 1,
    memberCount: 1,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

// Type definition for component VM
type CreateGroupDialogVM = {
  step: number
  groupName: string
  groupAnnouncement: string
  groupAvatar: File | null
  groupAvatarPreview: string
  selectedFriends: number[]
  friends: Friend[]
  searchQuery: string
  filteredFriends: Friend[]
  isLoading: boolean
  isCreating: boolean
  loadFriends: () => Promise<void>
  handleAvatarChange: (event: Event) => void
  toggleFriend: (friendId: number) => void
  nextStep: () => void
  prevStep: () => void
  createGroup: () => Promise<void>
  handleClose: () => void
}

// Helper to mount and open dialog
async function mountAndOpen(friends: Friend[] = []) {
  vi.mocked(friendApi.getFriends).mockResolvedValue(friends)

  const wrapper = mount(CreateGroupDialog, {
    props: { modelValue: false },
  })

  await wrapper.setProps({ modelValue: true })
  await flushPromises()

  return wrapper
}

describe('CreateGroupDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  describe('Initial State', () => {
    it('should start at step 1', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      expect(vm.step).toBe(1)
      wrapper.unmount()
    })

    it('should have empty group name initially', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      expect(vm.groupName).toBe('')
      wrapper.unmount()
    })

    it('should have empty announcement initially', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      expect(vm.groupAnnouncement).toBe('')
      wrapper.unmount()
    })

    it('should have no avatar initially', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      expect(vm.groupAvatar).toBeNull()
      expect(vm.groupAvatarPreview).toBe('')
      wrapper.unmount()
    })

    it('should have no selected friends initially', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      expect(vm.selectedFriends).toEqual([])
      wrapper.unmount()
    })
  })

  describe('Loading Friends', () => {
    it('should load friends when dialog opens', async () => {
      const friends = [createMockFriend()]
      await mountAndOpen(friends)

      expect(friendApi.getFriends).toHaveBeenCalled()
    })

    it('should populate friends list from API', async () => {
      const friends = [
        createMockFriend({ id: 1, nickname: 'Alice' }),
        createMockFriend({ id: 2, nickname: 'Bob' }),
      ]
      const wrapper = await mountAndOpen(friends)
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      expect(vm.friends).toHaveLength(2)
      expect(vm.friends[0].nickname).toBe('Alice')
      expect(vm.friends[1].nickname).toBe('Bob')
      wrapper.unmount()
    })

    it('should show error on load failure', async () => {
      vi.mocked(friendApi.getFriends).mockRejectedValue({
        response: { data: { message: 'Failed to load friends' } },
      })

      const wrapper = mount(CreateGroupDialog, {
        props: { modelValue: false },
      })

      await wrapper.setProps({ modelValue: true })
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to load friends')
      wrapper.unmount()
    })

    it('should set isLoading during load', async () => {
      let resolveLoad: (value: Friend[]) => void
      vi.mocked(friendApi.getFriends).mockReturnValue(
        new Promise((resolve) => {
          resolveLoad = resolve
        })
      )

      const wrapper = mount(CreateGroupDialog, {
        props: { modelValue: false },
      })

      await wrapper.setProps({ modelValue: true })

      const vm = wrapper.vm as unknown as CreateGroupDialogVM
      expect(vm.isLoading).toBe(true)

      resolveLoad!([])
      await flushPromises()

      expect(vm.isLoading).toBe(false)
      wrapper.unmount()
    })
  })

  describe('Friend Filtering', () => {
    it('should filter friends by nickname', async () => {
      const friends = [
        createMockFriend({ id: 1, nickname: 'Alice' }),
        createMockFriend({ id: 2, nickname: 'Bob' }),
      ]
      const wrapper = await mountAndOpen(friends)
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.searchQuery = 'Alice'

      expect(vm.filteredFriends).toHaveLength(1)
      expect(vm.filteredFriends[0].nickname).toBe('Alice')
      wrapper.unmount()
    })

    it('should filter friends by remark', async () => {
      const friends = [
        createMockFriend({ id: 1, nickname: 'Alice', remark: 'Best Friend' }),
        createMockFriend({ id: 2, nickname: 'Bob' }),
      ]
      const wrapper = await mountAndOpen(friends)
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.searchQuery = 'Best'

      expect(vm.filteredFriends).toHaveLength(1)
      expect(vm.filteredFriends[0].remark).toBe('Best Friend')
      wrapper.unmount()
    })

    it('should be case insensitive', async () => {
      const friends = [createMockFriend({ id: 1, nickname: 'Alice' })]
      const wrapper = await mountAndOpen(friends)
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.searchQuery = 'alice'

      expect(vm.filteredFriends).toHaveLength(1)
      wrapper.unmount()
    })

    it('should return all friends when search is empty', async () => {
      const friends = [
        createMockFriend({ id: 1, nickname: 'Alice' }),
        createMockFriend({ id: 2, nickname: 'Bob' }),
      ]
      const wrapper = await mountAndOpen(friends)
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.searchQuery = ''

      expect(vm.filteredFriends).toHaveLength(2)
      wrapper.unmount()
    })
  })

  describe('Friend Selection', () => {
    it('should toggle friend selection', async () => {
      const friends = [createMockFriend({ id: 1 })]
      const wrapper = await mountAndOpen(friends)
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.toggleFriend(1)
      expect(vm.selectedFriends).toEqual([1])

      vm.toggleFriend(1)
      expect(vm.selectedFriends).toEqual([])
      wrapper.unmount()
    })

    it('should allow multiple selections', async () => {
      const friends = [
        createMockFriend({ id: 1 }),
        createMockFriend({ id: 2 }),
        createMockFriend({ id: 3 }),
      ]
      const wrapper = await mountAndOpen(friends)
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.toggleFriend(1)
      vm.toggleFriend(3)

      expect(vm.selectedFriends).toEqual([1, 3])
      wrapper.unmount()
    })
  })

  describe('Step Navigation', () => {
    it('should go to step 2 when nextStep is called with valid name', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      vm.nextStep()

      expect(vm.step).toBe(2)
      wrapper.unmount()
    })

    it('should not go to step 2 with empty group name', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = ''
      vm.nextStep()

      expect(vm.step).toBe(1)
      expect(ElMessage.warning).toHaveBeenCalledWith('Please enter a group name')
      wrapper.unmount()
    })

    it('should not go to step 2 with whitespace-only group name', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = '   '
      vm.nextStep()

      expect(vm.step).toBe(1)
      expect(ElMessage.warning).toHaveBeenCalledWith('Please enter a group name')
      wrapper.unmount()
    })

    it('should go back to step 1 when prevStep is called', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      vm.nextStep()
      expect(vm.step).toBe(2)

      vm.prevStep()
      expect(vm.step).toBe(1)
      wrapper.unmount()
    })
  })

  describe('Avatar Upload', () => {
    it('should accept valid image file', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      const file = new File(['test'], 'avatar.jpg', { type: 'image/jpeg' })
      const event = {
        target: { files: [file] },
      } as unknown as Event

      // Mock URL.createObjectURL
      const mockUrl = 'blob:http://localhost/test-avatar'
      global.URL.createObjectURL = vi.fn().mockReturnValue(mockUrl)

      vm.handleAvatarChange(event)

      expect(vm.groupAvatar).not.toBeNull()
      expect(vm.groupAvatar?.name).toBe('avatar.jpg')
      expect(vm.groupAvatar?.type).toBe('image/jpeg')
      expect(vm.groupAvatarPreview).toBe(mockUrl)
      wrapper.unmount()
    })

    it('should reject non-image file', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      const file = new File(['test'], 'document.pdf', { type: 'application/pdf' })
      const event = {
        target: { files: [file] },
      } as unknown as Event

      vm.handleAvatarChange(event)

      expect(vm.groupAvatar).toBeNull()
      expect(ElMessage.error).toHaveBeenCalledWith('Please select an image file')
      wrapper.unmount()
    })

    it('should reject file larger than 5MB', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      // Create a mock file larger than 5MB
      const largeContent = new Array(6 * 1024 * 1024).fill('a').join('')
      const file = new File([largeContent], 'large.jpg', { type: 'image/jpeg' })
      const event = {
        target: { files: [file] },
      } as unknown as Event

      vm.handleAvatarChange(event)

      expect(vm.groupAvatar).toBeNull()
      expect(ElMessage.error).toHaveBeenCalledWith('Image size must be less than 5MB')
      wrapper.unmount()
    })

    it('should handle no file selected', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      const event = {
        target: { files: [] },
      } as unknown as Event

      vm.handleAvatarChange(event)

      expect(vm.groupAvatar).toBeNull()
      wrapper.unmount()
    })
  })

  describe('Create Group', () => {
    it('should create group with name only', async () => {
      const group = createMockGroup({ id: 1, name: 'My Group' })
      vi.mocked(groupApi.createGroup).mockResolvedValue(group)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      await vm.createGroup()
      await flushPromises()

      expect(groupApi.createGroup).toHaveBeenCalledWith({
        name: 'My Group',
        avatar: undefined,
        announcement: undefined,
        memberIds: undefined,
      })
      wrapper.unmount()
    })

    it('should create group with all options', async () => {
      const group = createMockGroup()
      vi.mocked(groupApi.createGroup).mockResolvedValue(group)
      vi.mocked(fileApi.uploadAvatar).mockResolvedValue({ url: 'http://example.com/avatar.jpg' } as any)

      const wrapper = await mountAndOpen([createMockFriend({ id: 1 }), createMockFriend({ id: 2 })])
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      // Set up group data
      vm.groupName = 'My Group'
      vm.groupAnnouncement = 'Welcome!'
      vm.groupAvatar = new File(['test'], 'avatar.jpg', { type: 'image/jpeg' })
      vm.toggleFriend(1)
      vm.toggleFriend(2)

      await vm.createGroup()
      await flushPromises()

      expect(fileApi.uploadAvatar).toHaveBeenCalled()
      expect(groupApi.createGroup).toHaveBeenCalledWith({
        name: 'My Group',
        avatar: 'http://example.com/avatar.jpg',
        announcement: 'Welcome!',
        memberIds: [1, 2],
      })
      wrapper.unmount()
    })

    it('should show success message on creation', async () => {
      const group = createMockGroup()
      vi.mocked(groupApi.createGroup).mockResolvedValue(group)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      await vm.createGroup()
      await flushPromises()

      expect(ElMessage.success).toHaveBeenCalledWith('Group created successfully')
      wrapper.unmount()
    })

    it('should emit group-created event', async () => {
      const group = createMockGroup({ id: 123 })
      vi.mocked(groupApi.createGroup).mockResolvedValue(group)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      await vm.createGroup()
      await flushPromises()

      expect(wrapper.emitted('group-created')).toBeTruthy()
      expect(wrapper.emitted('group-created')![0]).toEqual([group])
      wrapper.unmount()
    })

    it('should close dialog after creation', async () => {
      const group = createMockGroup()
      vi.mocked(groupApi.createGroup).mockResolvedValue(group)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      await vm.createGroup()
      await flushPromises()

      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      expect(wrapper.emitted('update:modelValue')![0]).toEqual([false])
      wrapper.unmount()
    })

    it('should show error on creation failure', async () => {
      vi.mocked(groupApi.createGroup).mockRejectedValue({
        response: { data: { message: 'Group name already exists' } },
      })

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      await vm.createGroup()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Group name already exists')
      wrapper.unmount()
    })

    it('should not create group with empty name', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = ''
      await vm.createGroup()
      await flushPromises()

      expect(groupApi.createGroup).not.toHaveBeenCalled()
      expect(ElMessage.warning).toHaveBeenCalledWith('Please enter a group name')
      wrapper.unmount()
    })

    it('should set isCreating during creation', async () => {
      let resolveCreate: (value: GroupDetail) => void
      vi.mocked(groupApi.createGroup).mockReturnValue(
        new Promise((resolve) => {
          resolveCreate = resolve
        })
      )

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      const createPromise = vm.createGroup()

      expect(vm.isCreating).toBe(true)

      resolveCreate!(createMockGroup())
      await createPromise
      await flushPromises()

      expect(vm.isCreating).toBe(false)
      wrapper.unmount()
    })

    it('should trim group name before creating', async () => {
      const group = createMockGroup()
      vi.mocked(groupApi.createGroup).mockResolvedValue(group)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = '  My Group  '
      await vm.createGroup()
      await flushPromises()

      expect(groupApi.createGroup).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'My Group',
        })
      )
      wrapper.unmount()
    })

    it('should trim announcement before creating', async () => {
      const group = createMockGroup()
      vi.mocked(groupApi.createGroup).mockResolvedValue(group)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      vm.groupAnnouncement = '  Welcome!  '
      await vm.createGroup()
      await flushPromises()

      expect(groupApi.createGroup).toHaveBeenCalledWith(
        expect.objectContaining({
          announcement: 'Welcome!',
        })
      )
      wrapper.unmount()
    })

    it('should not include empty announcement', async () => {
      const group = createMockGroup()
      vi.mocked(groupApi.createGroup).mockResolvedValue(group)

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      vm.groupAnnouncement = '   '
      await vm.createGroup()
      await flushPromises()

      expect(groupApi.createGroup).toHaveBeenCalledWith(
        expect.objectContaining({
          announcement: undefined,
        })
      )
      wrapper.unmount()
    })
  })

  describe('Dialog Close', () => {
    it('should reset all state when handleClose is called', async () => {
      const wrapper = await mountAndOpen([createMockFriend({ id: 1 })])
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      // Set up some state
      vm.groupName = 'My Group'
      vm.groupAnnouncement = 'Welcome'
      vm.searchQuery = 'test'
      vm.toggleFriend(1)
      vm.nextStep()

      // Close dialog
      vm.handleClose()

      expect(vm.step).toBe(1)
      expect(vm.groupName).toBe('')
      expect(vm.groupAnnouncement).toBe('')
      expect(vm.groupAvatar).toBeNull()
      expect(vm.groupAvatarPreview).toBe('')
      expect(vm.selectedFriends).toEqual([])
      expect(vm.searchQuery).toBe('')
      wrapper.unmount()
    })

    it('should emit update:modelValue false when handleClose is called', async () => {
      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.handleClose()

      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      expect(wrapper.emitted('update:modelValue')![0]).toEqual([false])
      wrapper.unmount()
    })
  })

  describe('Error Handling', () => {
    it('should show default error message when no message in response', async () => {
      vi.mocked(friendApi.getFriends).mockRejectedValue({
        response: { data: {} },
      })

      const wrapper = mount(CreateGroupDialog, {
        props: { modelValue: false },
      })

      await wrapper.setProps({ modelValue: true })
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to load friends')
      wrapper.unmount()
    })

    it('should show default error for group creation failure', async () => {
      vi.mocked(groupApi.createGroup).mockRejectedValue({
        response: { data: {} },
      })

      const wrapper = await mountAndOpen()
      const vm = wrapper.vm as unknown as CreateGroupDialogVM

      vm.groupName = 'My Group'
      await vm.createGroup()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to create group')
      wrapper.unmount()
    })
  })
})
