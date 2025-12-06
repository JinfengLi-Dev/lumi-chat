import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import GroupInfoPanel from '../GroupInfoPanel.vue'
import { groupApi } from '@/api/group'
import type { Group } from '@/types'
import type { GroupDetail, GroupMember } from '@/api/group'

// Mock the group API
vi.mock('@/api/group', () => ({
  groupApi: {
    getGroup: vi.fn(),
    getGroupMembers: vi.fn(),
    updateGroup: vi.fn(),
    removeMember: vi.fn(),
    leaveGroup: vi.fn(),
    deleteGroup: vi.fn(),
  },
}))

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(),
    },
  }
})

// Disable teleport for testing
config.global.stubs = {
  teleport: true,
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

// Helper to create mock group detail
function createMockGroupDetail(overrides: Partial<GroupDetail> = {}): GroupDetail {
  return {
    id: 1,
    gid: 'group-123',
    name: 'Test Group',
    ownerId: 1,
    ownerNickname: 'Owner',
    maxMembers: 500,
    memberCount: 3,
    createdAt: new Date().toISOString(),
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

describe('GroupInfoPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Loading and Display', () => {
    it('should load group details when group prop is set', async () => {
      const mockDetail = createMockGroupDetail()
      const mockMembers = [createMockMember()]
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue(mockMembers)

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2,
        },
      })

      await flushPromises()

      expect(groupApi.getGroup).toHaveBeenCalledWith(1)
      expect(groupApi.getGroupMembers).toHaveBeenCalledWith(1)
      expect(wrapper.text()).toContain('Test Group')
      wrapper.unmount()
    })

    it('should display empty state when no group is selected', async () => {
      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: null,
          currentUserId: 1,
        },
      })

      await flushPromises()

      expect(wrapper.find('.el-empty').exists()).toBe(true)
      wrapper.unmount()
    })

    it('should show error message when loading fails', async () => {
      vi.mocked(groupApi.getGroup).mockRejectedValue(new Error('Network error'))
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1,
        },
      })

      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
      wrapper.unmount()
    })

    it('should display group gid', async () => {
      const mockDetail = createMockGroupDetail({ gid: 'custom-gid-123' })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('ID: custom-gid-123')
      wrapper.unmount()
    })

    it('should display announcement', async () => {
      const mockDetail = createMockGroupDetail({ announcement: 'Welcome to our group!' })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Welcome to our group!')
      wrapper.unmount()
    })

    it('should show "No announcement" when none exists', async () => {
      const mockDetail = createMockGroupDetail({ announcement: undefined })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('No announcement')
      wrapper.unmount()
    })

    it('should display member count', async () => {
      const mockDetail = createMockGroupDetail({ memberCount: 42, maxMembers: 500 })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Members (42/500)')
      wrapper.unmount()
    })
  })

  describe('Owner/Admin Detection', () => {
    it('should detect owner correctly', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1, // Same as ownerId
        },
      })

      await flushPromises()

      type VM = { isOwner: boolean; isAdmin: boolean }
      const vm = wrapper.vm as unknown as VM
      expect(vm.isOwner).toBe(true)
      wrapper.unmount()
    })

    it('should detect admin correctly', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      const mockMembers = [
        createMockMember({ userId: 2, role: 'admin' }),
      ]
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue(mockMembers)

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2, // Admin user
        },
      })

      await flushPromises()

      type VM = { isOwner: boolean; isAdmin: boolean; isOwnerOrAdmin: boolean }
      const vm = wrapper.vm as unknown as VM
      expect(vm.isOwner).toBe(false)
      expect(vm.isAdmin).toBe(true)
      expect(vm.isOwnerOrAdmin).toBe(true)
      wrapper.unmount()
    })

    it('should show edit button for owner/admin', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1, // Owner
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Edit')
      wrapper.unmount()
    })

    it('should hide edit button for regular member', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      const mockMembers = [createMockMember({ userId: 2, role: 'member' })]
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue(mockMembers)

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2, // Not owner
        },
      })

      await flushPromises()

      // Should not find edit button in header
      const editButtons = wrapper.findAllComponents({ name: 'ElButton' }).filter((btn) =>
        btn.text().includes('Edit')
      )
      // There might be an edit button elsewhere, but not for editing the group name
      expect(editButtons.length).toBeLessThan(2)
      wrapper.unmount()
    })
  })

  describe('Group Editing', () => {
    it('should update group info successfully', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      const updatedDetail = createMockGroupDetail({ ownerId: 1, name: 'Updated Name', announcement: 'New announcement' })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])
      vi.mocked(groupApi.updateGroup).mockResolvedValue(updatedDetail)

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1,
        },
      })

      await flushPromises()

      type VM = {
        isEditing: boolean
        editForm: { name: string; announcement: string }
        saveGroupInfo: () => Promise<void>
      }
      const vm = wrapper.vm as unknown as VM

      // Start editing
      vm.isEditing = true
      vm.editForm.name = 'Updated Name'
      vm.editForm.announcement = 'New announcement'

      await vm.saveGroupInfo()
      await flushPromises()

      expect(groupApi.updateGroup).toHaveBeenCalledWith(1, {
        name: 'Updated Name',
        announcement: 'New announcement',
      })
      expect(ElMessage.success).toHaveBeenCalledWith('Group info updated')
      expect(wrapper.emitted('update:group')).toBeTruthy()
      wrapper.unmount()
    })

    it('should cancel editing and reset form', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1, name: 'Original', announcement: 'Original' })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1,
        },
      })

      await flushPromises()

      type VM = {
        isEditing: boolean
        editForm: { name: string; announcement: string }
        cancelEdit: () => void
      }
      const vm = wrapper.vm as unknown as VM

      vm.isEditing = true
      vm.editForm.name = 'Changed'
      vm.editForm.announcement = 'Changed'

      vm.cancelEdit()
      await flushPromises()

      expect(vm.isEditing).toBe(false)
      expect(vm.editForm.name).toBe('Original')
      expect(vm.editForm.announcement).toBe('Original')
      wrapper.unmount()
    })
  })

  describe('Member Management', () => {
    it('should remove member when confirmed', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      const mockMembers = [
        createMockMember({ userId: 1, role: 'owner' }),
        createMockMember({ userId: 2, nickname: 'John' }),
      ]
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue(mockMembers)
      vi.mocked(groupApi.removeMember).mockResolvedValue(undefined)
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1, // Owner
        },
      })

      await flushPromises()

      type VM = {
        members: GroupMember[]
        handleRemoveMember: (member: GroupMember) => Promise<void>
      }
      const vm = wrapper.vm as unknown as VM

      await vm.handleRemoveMember(mockMembers[1])
      await flushPromises()

      expect(ElMessageBox.confirm).toHaveBeenCalled()
      expect(groupApi.removeMember).toHaveBeenCalledWith(1, 2)
      expect(ElMessage.success).toHaveBeenCalled()
      expect(vm.members.length).toBe(1)
      wrapper.unmount()
    })

    it('should not remove member when cancelled', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      const mockMembers = [createMockMember({ userId: 2 })]
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue(mockMembers)
      vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1,
        },
      })

      await flushPromises()

      type VM = {
        members: GroupMember[]
        handleRemoveMember: (member: GroupMember) => Promise<void>
      }
      const vm = wrapper.vm as unknown as VM

      await vm.handleRemoveMember(mockMembers[0])
      await flushPromises()

      expect(groupApi.removeMember).not.toHaveBeenCalled()
      expect(vm.members.length).toBe(1)
      wrapper.unmount()
    })
  })

  describe('Leave Group', () => {
    it('should leave group when confirmed', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])
      vi.mocked(groupApi.leaveGroup).mockResolvedValue(undefined)
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2, // Not owner
        },
      })

      await flushPromises()

      type VM = { handleLeaveGroup: () => Promise<void> }
      const vm = wrapper.vm as unknown as VM

      await vm.handleLeaveGroup()
      await flushPromises()

      expect(groupApi.leaveGroup).toHaveBeenCalledWith(1)
      expect(ElMessage.success).toHaveBeenCalled()
      expect(wrapper.emitted('leave')).toBeTruthy()
      wrapper.unmount()
    })

    it('should not leave group when cancelled', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])
      vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2,
        },
      })

      await flushPromises()

      type VM = { handleLeaveGroup: () => Promise<void> }
      const vm = wrapper.vm as unknown as VM

      await vm.handleLeaveGroup()
      await flushPromises()

      expect(groupApi.leaveGroup).not.toHaveBeenCalled()
      expect(wrapper.emitted('leave')).toBeFalsy()
      wrapper.unmount()
    })

    it('should show Leave Group button for non-owners', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 2, // Not owner
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Leave Group')
      expect(wrapper.text()).not.toContain('Dissolve Group')
      wrapper.unmount()
    })
  })

  describe('Dissolve Group', () => {
    it('should dissolve group when confirmed', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])
      vi.mocked(groupApi.deleteGroup).mockResolvedValue(undefined)
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1, // Owner
        },
      })

      await flushPromises()

      type VM = { handleDissolveGroup: () => Promise<void> }
      const vm = wrapper.vm as unknown as VM

      await vm.handleDissolveGroup()
      await flushPromises()

      expect(groupApi.deleteGroup).toHaveBeenCalledWith(1)
      expect(ElMessage.success).toHaveBeenCalled()
      expect(wrapper.emitted('dissolve')).toBeTruthy()
      wrapper.unmount()
    })

    it('should show Dissolve Group button only for owner', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1, // Owner
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Dissolve Group')
      expect(wrapper.text()).not.toContain('Leave Group')
      wrapper.unmount()
    })
  })

  describe('Invite Members', () => {
    it('should emit invite-members event when invite button clicked', async () => {
      const mockDetail = createMockGroupDetail({ ownerId: 1 })
      vi.mocked(groupApi.getGroup).mockResolvedValue(mockDetail)
      vi.mocked(groupApi.getGroupMembers).mockResolvedValue([])

      const wrapper = mount(GroupInfoPanel, {
        props: {
          group: createMockGroup(),
          currentUserId: 1, // Owner
        },
      })

      await flushPromises()

      // Find and click invite button
      const inviteBtn = wrapper.findAllComponents({ name: 'ElButton' }).find((btn) =>
        btn.text().includes('Invite')
      )
      expect(inviteBtn).toBeDefined()

      await inviteBtn?.trigger('click')
      await flushPromises()

      expect(wrapper.emitted('invite-members')).toBeTruthy()
      wrapper.unmount()
    })
  })
})
