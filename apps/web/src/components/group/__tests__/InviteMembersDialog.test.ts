import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import InviteMembersDialog from '../InviteMembersDialog.vue'
import { friendApi } from '@/api/friend'
import { groupApi } from '@/api/group'
import type { Friend } from '@/types'
import type { GroupMember } from '@/api/group'

// Mock the APIs
vi.mock('@/api/friend', () => ({
  friendApi: {
    getFriends: vi.fn(),
  },
}))

vi.mock('@/api/group', () => ({
  groupApi: {
    getGroupMembers: vi.fn(),
    addMembers: vi.fn(),
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

// Helper to create mock member
function createMockMember(overrides: Partial<GroupMember> = {}): GroupMember {
  return {
    id: 1,
    userId: 1,
    uid: 'user-1',
    nickname: 'Member 1',
    role: 'member',
    joinedAt: new Date().toISOString(),
    ...overrides,
  }
}

// Helper to mount and open dialog
async function mountAndOpen(
  groupId: number | null = 1,
  friends: Friend[] = [],
  members: GroupMember[] = []
) {
  vi.mocked(friendApi.getFriends).mockResolvedValue(friends)
  vi.mocked(groupApi.getGroupMembers).mockResolvedValue(members)

  const wrapper = mount(InviteMembersDialog, {
    props: {
      modelValue: false,
      groupId,
    },
  })

  await wrapper.setProps({ modelValue: true })
  await flushPromises()

  return wrapper
}

describe('InviteMembersDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Loading', () => {
    it('should load friends and members when dialog opens', async () => {
      const friends = [createMockFriend()]
      const members = [createMockMember()]

      const wrapper = await mountAndOpen(1, friends, members)

      expect(friendApi.getFriends).toHaveBeenCalled()
      expect(groupApi.getGroupMembers).toHaveBeenCalledWith(1)

      wrapper.unmount()
    })

    it('should handle friends loading error', async () => {
      vi.mocked(friendApi.getFriends).mockRejectedValue(new Error('Network error'))
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(InviteMembersDialog, {
        props: {
          modelValue: false,
          groupId: 1,
        },
      })

      await wrapper.setProps({ modelValue: true })
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
      wrapper.unmount()
    })

    it('should handle members loading error', async () => {
      vi.mocked(friendApi.getFriends).mockResolvedValue([])
      vi.mocked(groupApi.getGroupMembers).mockRejectedValue(new Error('Network error'))

      const wrapper = mount(InviteMembersDialog, {
        props: {
          modelValue: false,
          groupId: 1,
        },
      })

      await wrapper.setProps({ modelValue: true })
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
      wrapper.unmount()
    })
  })

  describe('Filtering', () => {
    it('should filter out existing members from friend list', async () => {
      const friends = [
        createMockFriend({ id: 10, nickname: 'Friend 10' }),
        createMockFriend({ id: 20, nickname: 'Friend 20' }),
      ]
      const members = [
        createMockMember({ userId: 10 }), // Friend 10 is already a member
      ]

      const wrapper = await mountAndOpen(1, friends, members)

      type VM = { availableFriends: Friend[] }
      const vm = wrapper.vm as unknown as VM

      expect(vm.availableFriends).toHaveLength(1)
      expect(vm.availableFriends[0].id).toBe(20)

      wrapper.unmount()
    })

    it('should filter friends by search query', async () => {
      const friends = [
        createMockFriend({ id: 10, nickname: 'Alice' }),
        createMockFriend({ id: 20, nickname: 'Bob' }),
      ]

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = { searchQuery: string; filteredFriends: Friend[] }
      const vm = wrapper.vm as unknown as VM

      expect(vm.filteredFriends).toHaveLength(2)

      vm.searchQuery = 'Alice'
      await flushPromises()

      expect(vm.filteredFriends).toHaveLength(1)
      expect(vm.filteredFriends[0].nickname).toBe('Alice')

      wrapper.unmount()
    })

    it('should filter by remark name', async () => {
      const friends = [
        createMockFriend({
          id: 10,
          remark: 'My Boss',
          nickname: 'John',
        }),
      ]

      const wrapper = await mountAndOpen(1, friends, [])

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

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = { searchQuery: string; filteredFriends: Friend[] }
      const vm = wrapper.vm as unknown as VM

      vm.searchQuery = 'alice'
      await flushPromises()

      expect(vm.filteredFriends).toHaveLength(1)

      wrapper.unmount()
    })

    it('should show all available friends when search is empty', async () => {
      const friends = [
        createMockFriend({ id: 10 }),
        createMockFriend({ id: 20 }),
      ]

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = { searchQuery: string; filteredFriends: Friend[] }
      const vm = wrapper.vm as unknown as VM

      vm.searchQuery = ''
      await flushPromises()

      expect(vm.filteredFriends).toHaveLength(2)

      wrapper.unmount()
    })
  })

  describe('Selection', () => {
    it('should toggle friend selection', async () => {
      const friends = [createMockFriend({ id: 10 })]

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = {
        selectedFriendIds: number[]
        toggleFriend: (id: number) => void
        isSelected: (id: number) => boolean
      }
      const vm = wrapper.vm as unknown as VM

      expect(vm.selectedFriendIds).toEqual([])
      expect(vm.isSelected(10)).toBe(false)

      vm.toggleFriend(10)
      await flushPromises()

      expect(vm.selectedFriendIds).toEqual([10])
      expect(vm.isSelected(10)).toBe(true)

      vm.toggleFriend(10)
      await flushPromises()

      expect(vm.selectedFriendIds).toEqual([])
      expect(vm.isSelected(10)).toBe(false)

      wrapper.unmount()
    })

    it('should allow multiple selections', async () => {
      const friends = [
        createMockFriend({ id: 10 }),
        createMockFriend({ id: 20 }),
        createMockFriend({ id: 30 }),
      ]

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = { selectedFriendIds: number[]; toggleFriend: (id: number) => void }
      const vm = wrapper.vm as unknown as VM

      vm.toggleFriend(10)
      vm.toggleFriend(30)
      await flushPromises()

      expect(vm.selectedFriendIds).toEqual([10, 30])

      wrapper.unmount()
    })
  })

  describe('Invite Action', () => {
    it('should warn when no friends selected', async () => {
      const friends = [createMockFriend()]

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = { handleInvite: () => Promise<void> }
      const vm = wrapper.vm as unknown as VM

      await vm.handleInvite()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('Please select at least one friend')
      expect(groupApi.addMembers).not.toHaveBeenCalled()

      wrapper.unmount()
    })

    it('should error when no group selected', async () => {
      const friends = [createMockFriend({ id: 10 })]

      const wrapper = await mountAndOpen(null, friends, [])

      type VM = { selectedFriendIds: number[]; handleInvite: () => Promise<void> }
      const vm = wrapper.vm as unknown as VM

      vm.selectedFriendIds = [10]
      await vm.handleInvite()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('No group selected')
      expect(groupApi.addMembers).not.toHaveBeenCalled()

      wrapper.unmount()
    })

    it('should invite selected friends successfully', async () => {
      const friends = [
        createMockFriend({ id: 10 }),
        createMockFriend({ id: 20 }),
      ]
      vi.mocked(groupApi.addMembers).mockResolvedValue([])

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = {
        selectedFriendIds: number[]
        toggleFriend: (id: number) => void
        handleInvite: () => Promise<void>
      }
      const vm = wrapper.vm as unknown as VM

      vm.toggleFriend(10)
      vm.toggleFriend(20)
      await vm.handleInvite()
      await flushPromises()

      expect(groupApi.addMembers).toHaveBeenCalledWith(1, [10, 20])
      expect(ElMessage.success).toHaveBeenCalledWith('2 member(s) invited successfully')
      expect(wrapper.emitted('invited')).toBeTruthy()
      expect(wrapper.emitted('invited')![0]).toEqual([[10, 20]])

      wrapper.unmount()
    })

    it('should handle invite error', async () => {
      const friends = [createMockFriend({ id: 10 })]
      vi.mocked(groupApi.addMembers).mockRejectedValue(new Error('Failed to add members'))

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = { selectedFriendIds: number[]; handleInvite: () => Promise<void> }
      const vm = wrapper.vm as unknown as VM

      vm.selectedFriendIds = [10]
      await vm.handleInvite()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('Failed to add members')
      expect(wrapper.emitted('invited')).toBeFalsy()

      wrapper.unmount()
    })

    it('should close dialog after successful invite', async () => {
      const friends = [createMockFriend({ id: 10 })]
      vi.mocked(groupApi.addMembers).mockResolvedValue([])

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = { selectedFriendIds: number[]; handleInvite: () => Promise<void> }
      const vm = wrapper.vm as unknown as VM

      vm.selectedFriendIds = [10]
      await vm.handleInvite()
      await flushPromises()

      expect(wrapper.emitted('update:modelValue')).toEqual([[false]])

      wrapper.unmount()
    })
  })

  describe('Close/Cancel Action', () => {
    it('should close dialog and reset state', async () => {
      const friends = [createMockFriend({ id: 10 })]

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = {
        selectedFriendIds: number[]
        searchQuery: string
        handleClose: () => void
      }
      const vm = wrapper.vm as unknown as VM

      vm.selectedFriendIds = [10]
      vm.searchQuery = 'test'

      vm.handleClose()
      await flushPromises()

      expect(vm.selectedFriendIds).toEqual([])
      expect(vm.searchQuery).toBe('')
      expect(wrapper.emitted('update:modelValue')).toEqual([[false]])

      wrapper.unmount()
    })
  })

  describe('Display', () => {
    it('should display friend with remark name', async () => {
      const friends = [
        createMockFriend({
          id: 10,
          remark: 'My Best Friend',
          nickname: 'John',
        }),
      ]

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = { getDisplayName: (friend: Friend) => string }
      const vm = wrapper.vm as unknown as VM

      expect(vm.getDisplayName(friends[0])).toBe('My Best Friend')

      wrapper.unmount()
    })

    it('should display friend with nickname when no remark', async () => {
      const friends = [
        createMockFriend({
          id: 10,
          remark: undefined,
          nickname: 'John',
        }),
      ]

      const wrapper = await mountAndOpen(1, friends, [])

      type VM = { getDisplayName: (friend: Friend) => string }
      const vm = wrapper.vm as unknown as VM

      expect(vm.getDisplayName(friends[0])).toBe('John')

      wrapper.unmount()
    })

    it('should show empty state when all friends are already members', async () => {
      const friends = [
        createMockFriend({ id: 10 }),
      ]
      const members = [
        createMockMember({ userId: 10 }), // Same as friend's id
      ]

      const wrapper = await mountAndOpen(1, friends, members)

      type VM = { availableFriends: Friend[] }
      const vm = wrapper.vm as unknown as VM

      expect(vm.availableFriends).toHaveLength(0)

      wrapper.unmount()
    })
  })
})
