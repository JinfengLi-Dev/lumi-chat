import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import MentionSelector from '../MentionSelector.vue'
import type { GroupMember } from '@/api/group'

// Create teleport target
let teleportTarget: HTMLElement

// Helper to create mock member
function createMockMember(overrides: Partial<GroupMember> = {}): GroupMember {
  return {
    id: 1,
    userId: 10,
    uid: 'user-1',
    nickname: 'Member 1',
    role: 'member',
    joinedAt: new Date().toISOString(),
    ...overrides,
  }
}

describe('MentionSelector', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    teleportTarget = document.createElement('div')
    teleportTarget.id = 'teleport-target'
    document.body.appendChild(teleportTarget)
  })

  afterEach(() => {
    const menus = document.body.querySelectorAll('.mention-selector')
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
      mount(MentionSelector, {
        props: {
          visible: false,
          x: 100,
          y: 100,
          members: [createMockMember()],
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()
      expect(findInBody('.mention-selector')).toBeNull()
    })

    it('should not render when no members and no all option', async () => {
      mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members: [],
          searchText: 'xyz',
          isOwner: false,
        },
      })

      await flushPromises()
      expect(findInBody('.mention-selector')).toBeNull()
    })

    it('should render at correct position', async () => {
      mount(MentionSelector, {
        props: {
          visible: true,
          x: 150,
          y: 200,
          members: [createMockMember()],
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()
      const menu = findInBody('.mention-selector') as HTMLElement
      expect(menu).not.toBeNull()
      expect(menu.style.left).toBe('150px')
    })

    it('should render member items', async () => {
      const members = [
        createMockMember({ id: 1, nickname: 'Alice' }),
        createMockMember({ id: 2, nickname: 'Bob' }),
      ]

      mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()
      const items = findAllInBody('.mention-item')
      expect(items.length).toBe(2)
    })
  })

  describe('@All Option', () => {
    it('should show @all option when isOwner is true', async () => {
      mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members: [createMockMember()],
          searchText: '',
          isOwner: true,
        },
      })

      await flushPromises()
      const allOption = findInBody('.all-option')
      expect(allOption).not.toBeNull()
      expect(allOption?.textContent).toContain('@All Members')
    })

    it('should not show @all option when isOwner is false', async () => {
      mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members: [createMockMember()],
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()
      const allOption = findInBody('.all-option')
      expect(allOption).toBeNull()
    })

    it('should emit select with "all" when @all is clicked', async () => {
      const wrapper = mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members: [createMockMember()],
          searchText: '',
          isOwner: true,
        },
      })

      await flushPromises()
      const allOption = findInBody('.all-option') as HTMLElement
      allOption.click()
      await flushPromises()

      expect(wrapper.emitted('select')).toBeTruthy()
      expect(wrapper.emitted('select')![0]).toEqual(['all'])
    })
  })

  describe('Filtering', () => {
    it('should filter members by search text', async () => {
      const members = [
        createMockMember({ id: 1, nickname: 'Alice' }),
        createMockMember({ id: 2, nickname: 'Bob' }),
      ]

      const wrapper = mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: 'Ali',
          isOwner: false,
        },
      })

      await flushPromises()

      type VM = { filteredMembers: GroupMember[] }
      const vm = wrapper.vm as unknown as VM

      expect(vm.filteredMembers).toHaveLength(1)
      expect(vm.filteredMembers[0].nickname).toBe('Alice')
    })

    it('should filter by group nickname', async () => {
      const members = [
        createMockMember({ id: 1, nickname: 'John', groupNickname: 'Team Lead' }),
      ]

      const wrapper = mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: 'Lead',
          isOwner: false,
        },
      })

      await flushPromises()

      type VM = { filteredMembers: GroupMember[] }
      const vm = wrapper.vm as unknown as VM

      expect(vm.filteredMembers).toHaveLength(1)
    })

    it('should be case insensitive', async () => {
      const members = [
        createMockMember({ id: 1, nickname: 'ALICE' }),
      ]

      const wrapper = mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: 'alice',
          isOwner: false,
        },
      })

      await flushPromises()

      type VM = { filteredMembers: GroupMember[] }
      const vm = wrapper.vm as unknown as VM

      expect(vm.filteredMembers).toHaveLength(1)
    })
  })

  describe('Selection', () => {
    it('should emit select event on member click', async () => {
      const members = [createMockMember({ id: 1, nickname: 'Alice' })]

      const wrapper = mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()
      const memberItem = findInBody('.mention-item') as HTMLElement
      memberItem.click()
      await flushPromises()

      expect(wrapper.emitted('select')).toBeTruthy()
      expect(wrapper.emitted('select')![0][0]).toEqual(members[0])
    })

    it('should emit update:visible false on selection', async () => {
      const members = [createMockMember()]

      const wrapper = mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()
      const memberItem = findInBody('.mention-item') as HTMLElement
      memberItem.click()
      await flushPromises()

      expect(wrapper.emitted('update:visible')).toEqual([[false]])
    })
  })

  describe('Display', () => {
    it('should display group nickname when available', async () => {
      const members = [
        createMockMember({
          id: 1,
          nickname: 'John',
          groupNickname: 'Team Lead',
        }),
      ]

      mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()
      const nameEl = findInBody('.mention-name')
      const realNameEl = findInBody('.mention-real-name')

      expect(nameEl?.textContent).toBe('Team Lead')
      expect(realNameEl?.textContent).toBe('John')
    })

    it('should display nickname when no group nickname', async () => {
      const members = [
        createMockMember({ id: 1, nickname: 'John' }),
      ]

      mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()
      const nameEl = findInBody('.mention-name')
      const realNameEl = findInBody('.mention-real-name')

      expect(nameEl?.textContent).toBe('John')
      expect(realNameEl).toBeNull()
    })
  })

  describe('Keyboard Navigation', () => {
    it('should have selected index starting at 0', async () => {
      const members = [
        createMockMember({ id: 1 }),
        createMockMember({ id: 2 }),
      ]

      const wrapper = mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()

      type VM = { selectedIndex: number }
      const vm = wrapper.vm as unknown as VM

      expect(vm.selectedIndex).toBe(0)
    })

    it('should reset selected index on search text change', async () => {
      const members = [
        createMockMember({ id: 1 }),
        createMockMember({ id: 2 }),
      ]

      const wrapper = mount(MentionSelector, {
        props: {
          visible: true,
          x: 100,
          y: 100,
          members,
          searchText: '',
          isOwner: false,
        },
      })

      await flushPromises()

      type VM = { selectedIndex: number }
      const vm = wrapper.vm as unknown as VM
      vm.selectedIndex = 1

      await wrapper.setProps({ searchText: 'a' })
      await flushPromises()

      expect(vm.selectedIndex).toBe(0)
    })
  })
})
