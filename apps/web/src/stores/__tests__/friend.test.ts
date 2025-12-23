import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useFriendStore } from '@/stores/friend'
import { friendApi } from '@/api/friend'
import { websocketService } from '@/services/websocket'
import type { Friend, FriendRequest, User } from '@/types'

// Mock the API modules
vi.mock('@/api/friend', () => ({
  friendApi: {
    getFriends: vi.fn(),
    getFriendRequests: vi.fn(),
    sendFriendRequest: vi.fn(),
    acceptFriendRequest: vi.fn(),
    rejectFriendRequest: vi.fn(),
    deleteFriend: vi.fn(),
    updateRemark: vi.fn(),
    updateMemo: vi.fn(),
    blockFriend: vi.fn(),
    unblockFriend: vi.fn(),
  },
}))

// Mock WebSocket service
vi.mock('@/services/websocket', () => ({
  websocketService: {
    requestOnlineStatus: vi.fn().mockResolvedValue(undefined),
    subscribeOnlineStatus: vi.fn().mockResolvedValue(undefined),
  },
}))

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

// Helper to create mock friend
function createMockFriend(overrides: Partial<Friend> = {}): Friend {
  return {
    ...createMockUser(),
    friendshipCreatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

// Helper to create mock friend request
function createMockFriendRequest(overrides: Partial<FriendRequest> = {}): FriendRequest {
  return {
    id: 1,
    fromUser: createMockUser({ id: 2, nickname: 'Requester' }),
    toUser: createMockUser({ id: 1, nickname: 'Me' }),
    message: 'Hello, let\'s be friends!',
    status: 'pending',
    createdAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

describe('Friend Store', () => {
  let friendStore: ReturnType<typeof useFriendStore>

  beforeEach(() => {
    vi.clearAllMocks()

    // Create fresh pinia instance
    setActivePinia(createPinia())
    friendStore = useFriendStore()
  })

  describe('Initial State', () => {
    it('should have empty friends array', () => {
      expect(friendStore.friends).toEqual([])
    })

    it('should have empty friendRequests array', () => {
      expect(friendStore.friendRequests).toEqual([])
    })

    it('should have empty onlineStatus map', () => {
      expect(friendStore.onlineStatus.size).toBe(0)
    })

    it('should have loading as false', () => {
      expect(friendStore.loading).toBe(false)
    })

    it('should have loadingRequests as false', () => {
      expect(friendStore.loadingRequests).toBe(false)
    })
  })

  describe('Getters', () => {
    describe('sortedFriends', () => {
      it('should sort online friends first', () => {
        friendStore.friends = [
          createMockFriend({ id: 1, nickname: 'Alice' }),
          createMockFriend({ id: 2, nickname: 'Bob' }),
          createMockFriend({ id: 3, nickname: 'Charlie' }),
        ]
        friendStore.onlineStatus.set(2, true) // Bob is online

        const sorted = friendStore.sortedFriends
        expect(sorted[0].nickname).toBe('Bob') // Online first
      })

      it('should sort alphabetically within same online status', () => {
        friendStore.friends = [
          createMockFriend({ id: 1, nickname: 'Charlie' }),
          createMockFriend({ id: 2, nickname: 'Alice' }),
          createMockFriend({ id: 3, nickname: 'Bob' }),
        ]
        // All offline

        const sorted = friendStore.sortedFriends
        expect(sorted[0].nickname).toBe('Alice')
        expect(sorted[1].nickname).toBe('Bob')
        expect(sorted[2].nickname).toBe('Charlie')
      })

      it('should use remark for sorting if available', () => {
        friendStore.friends = [
          createMockFriend({ id: 1, nickname: 'Zach', remark: 'Alice (Zach)' }),
          createMockFriend({ id: 2, nickname: 'Bob' }),
        ]

        const sorted = friendStore.sortedFriends
        expect(sorted[0].id).toBe(1) // "Alice (Zach)" comes before "Bob"
      })

      it('should return empty array when no friends', () => {
        expect(friendStore.sortedFriends).toEqual([])
      })
    })

    describe('onlineFriendsCount', () => {
      it('should return count of online friends', () => {
        friendStore.friends = [
          createMockFriend({ id: 1 }),
          createMockFriend({ id: 2 }),
          createMockFriend({ id: 3 }),
        ]
        friendStore.onlineStatus.set(1, true)
        friendStore.onlineStatus.set(2, true)
        friendStore.onlineStatus.set(3, false)

        expect(friendStore.onlineFriendsCount).toBe(2)
      })

      it('should return 0 when no friends online', () => {
        friendStore.friends = [createMockFriend({ id: 1 })]
        expect(friendStore.onlineFriendsCount).toBe(0)
      })

      it('should return 0 when no friends', () => {
        expect(friendStore.onlineFriendsCount).toBe(0)
      })
    })

    describe('pendingRequestsCount', () => {
      it('should return count of pending requests', () => {
        friendStore.friendRequests = [
          createMockFriendRequest({ id: 1, status: 'pending' }),
          createMockFriendRequest({ id: 2, status: 'accepted' }),
          createMockFriendRequest({ id: 3, status: 'pending' }),
        ]

        expect(friendStore.pendingRequestsCount).toBe(2)
      })

      it('should return 0 when no pending requests', () => {
        friendStore.friendRequests = [
          createMockFriendRequest({ id: 1, status: 'accepted' }),
        ]
        expect(friendStore.pendingRequestsCount).toBe(0)
      })

      it('should return 0 when no requests', () => {
        expect(friendStore.pendingRequestsCount).toBe(0)
      })
    })

    describe('isUserOnline', () => {
      it('should return true for online user', () => {
        friendStore.onlineStatus.set(1, true)
        expect(friendStore.isUserOnline(1)).toBe(true)
      })

      it('should return false for offline user', () => {
        friendStore.onlineStatus.set(1, false)
        expect(friendStore.isUserOnline(1)).toBe(false)
      })

      it('should return false for unknown user', () => {
        expect(friendStore.isUserOnline(999)).toBe(false)
      })
    })
  })

  describe('Actions', () => {
    describe('fetchFriends', () => {
      it('should fetch friends and store them', async () => {
        const mockFriends = [
          createMockFriend({ id: 1, nickname: 'Friend 1' }),
          createMockFriend({ id: 2, nickname: 'Friend 2' }),
        ]
        vi.mocked(friendApi.getFriends).mockResolvedValue(mockFriends)

        await friendStore.fetchFriends()

        expect(friendApi.getFriends).toHaveBeenCalled()
        expect(friendStore.friends).toEqual(mockFriends)
      })

      it('should set loading during fetch', async () => {
        vi.mocked(friendApi.getFriends).mockImplementation(async () => {
          expect(friendStore.loading).toBe(true)
          return []
        })

        await friendStore.fetchFriends()
        expect(friendStore.loading).toBe(false)
      })

      it('should request online status after fetching', async () => {
        const mockFriends = [
          createMockFriend({ id: 1 }),
          createMockFriend({ id: 2 }),
        ]
        vi.mocked(friendApi.getFriends).mockResolvedValue(mockFriends)

        await friendStore.fetchFriends()

        expect(websocketService.requestOnlineStatus).toHaveBeenCalledWith([1, 2])
        expect(websocketService.subscribeOnlineStatus).toHaveBeenCalledWith([1, 2])
      })

      it('should not request online status if no friends', async () => {
        vi.mocked(friendApi.getFriends).mockResolvedValue([])

        await friendStore.fetchFriends()

        expect(websocketService.requestOnlineStatus).not.toHaveBeenCalled()
      })

      it('should reset loading on error', async () => {
        vi.mocked(friendApi.getFriends).mockRejectedValue(new Error('Network error'))

        await expect(friendStore.fetchFriends()).rejects.toThrow('Network error')
        expect(friendStore.loading).toBe(false)
      })
    })

    describe('fetchFriendRequests', () => {
      it('should fetch friend requests and store them', async () => {
        const mockRequests = [
          createMockFriendRequest({ id: 1 }),
          createMockFriendRequest({ id: 2 }),
        ]
        vi.mocked(friendApi.getFriendRequests).mockResolvedValue(mockRequests)

        await friendStore.fetchFriendRequests()

        expect(friendApi.getFriendRequests).toHaveBeenCalled()
        expect(friendStore.friendRequests).toEqual(mockRequests)
      })

      it('should set loadingRequests during fetch', async () => {
        vi.mocked(friendApi.getFriendRequests).mockImplementation(async () => {
          expect(friendStore.loadingRequests).toBe(true)
          return []
        })

        await friendStore.fetchFriendRequests()
        expect(friendStore.loadingRequests).toBe(false)
      })

      it('should reset loadingRequests on error', async () => {
        vi.mocked(friendApi.getFriendRequests).mockRejectedValue(new Error('Network error'))

        await expect(friendStore.fetchFriendRequests()).rejects.toThrow('Network error')
        expect(friendStore.loadingRequests).toBe(false)
      })
    })

    describe('sendFriendRequest', () => {
      it('should send friend request', async () => {
        vi.mocked(friendApi.sendFriendRequest).mockResolvedValue(createMockFriendRequest())

        await friendStore.sendFriendRequest('user456', 'Hello!')

        expect(friendApi.sendFriendRequest).toHaveBeenCalledWith({
          uid: 'user456',
          message: 'Hello!',
        })
      })

      it('should send friend request without message', async () => {
        vi.mocked(friendApi.sendFriendRequest).mockResolvedValue(createMockFriendRequest())

        await friendStore.sendFriendRequest('user456')

        expect(friendApi.sendFriendRequest).toHaveBeenCalledWith({
          uid: 'user456',
          message: undefined,
        })
      })
    })

    describe('acceptFriendRequest', () => {
      it('should accept friend request and update status', async () => {
        friendStore.friendRequests = [
          createMockFriendRequest({ id: 1, status: 'pending' }),
        ]
        vi.mocked(friendApi.acceptFriendRequest).mockResolvedValue(
          createMockFriendRequest({ id: 1, status: 'accepted' })
        )
        vi.mocked(friendApi.getFriends).mockResolvedValue([])

        await friendStore.acceptFriendRequest(1)

        expect(friendApi.acceptFriendRequest).toHaveBeenCalledWith(1)
        expect(friendStore.friendRequests[0].status).toBe('accepted')
        expect(friendApi.getFriends).toHaveBeenCalled() // Should refresh friends
      })

      it('should not update status if request not found', async () => {
        friendStore.friendRequests = []
        vi.mocked(friendApi.acceptFriendRequest).mockResolvedValue(
          createMockFriendRequest({ id: 1, status: 'accepted' })
        )

        await friendStore.acceptFriendRequest(999)

        // Should not throw, just not update anything
        expect(friendStore.friendRequests).toEqual([])
      })
    })

    describe('rejectFriendRequest', () => {
      it('should reject friend request and update status', async () => {
        friendStore.friendRequests = [
          createMockFriendRequest({ id: 1, status: 'pending' }),
        ]
        vi.mocked(friendApi.rejectFriendRequest).mockResolvedValue(
          createMockFriendRequest({ id: 1, status: 'rejected' })
        )

        await friendStore.rejectFriendRequest(1)

        expect(friendApi.rejectFriendRequest).toHaveBeenCalledWith(1)
        expect(friendStore.friendRequests[0].status).toBe('rejected')
      })

      it('should not refresh friends list after rejection', async () => {
        friendStore.friendRequests = [
          createMockFriendRequest({ id: 1, status: 'pending' }),
        ]
        vi.mocked(friendApi.rejectFriendRequest).mockResolvedValue(
          createMockFriendRequest({ id: 1, status: 'rejected' })
        )

        await friendStore.rejectFriendRequest(1)

        expect(friendApi.getFriends).not.toHaveBeenCalled()
      })
    })

    describe('deleteFriend', () => {
      it('should delete friend and remove from list', async () => {
        friendStore.friends = [
          createMockFriend({ id: 1, nickname: 'Friend 1' }),
          createMockFriend({ id: 2, nickname: 'Friend 2' }),
        ]
        friendStore.onlineStatus.set(1, true)
        vi.mocked(friendApi.deleteFriend).mockResolvedValue(undefined)

        await friendStore.deleteFriend(1)

        expect(friendApi.deleteFriend).toHaveBeenCalledWith(1)
        expect(friendStore.friends).toHaveLength(1)
        expect(friendStore.friends[0].id).toBe(2)
        expect(friendStore.onlineStatus.has(1)).toBe(false)
      })

      it('should handle deleting non-existent friend gracefully', async () => {
        friendStore.friends = [createMockFriend({ id: 1 })]
        vi.mocked(friendApi.deleteFriend).mockResolvedValue(undefined)

        await friendStore.deleteFriend(999)

        expect(friendStore.friends).toHaveLength(1)
      })
    })

    describe('setFriendRemark', () => {
      it('should update friend remark', async () => {
        friendStore.friends = [createMockFriend({ id: 1, remark: '' })]
        vi.mocked(friendApi.updateRemark).mockResolvedValue(undefined)

        await friendStore.setFriendRemark(1, 'My Best Friend')

        expect(friendApi.updateRemark).toHaveBeenCalledWith(1, 'My Best Friend')
        expect(friendStore.friends[0].remark).toBe('My Best Friend')
      })

      it('should not update if friend not found', async () => {
        friendStore.friends = [createMockFriend({ id: 1 })]
        vi.mocked(friendApi.updateRemark).mockResolvedValue(undefined)

        await friendStore.setFriendRemark(999, 'Test')

        // Should not throw
        expect(friendStore.friends[0].remark).toBeUndefined()
      })
    })

    describe('handleOnlineStatusResponse', () => {
      it('should update online status from response', () => {
        friendStore.handleOnlineStatusResponse({
          1: true,
          2: false,
          3: true,
        })

        expect(friendStore.onlineStatus.get(1)).toBe(true)
        expect(friendStore.onlineStatus.get(2)).toBe(false)
        expect(friendStore.onlineStatus.get(3)).toBe(true)
      })

      it('should handle empty response', () => {
        friendStore.handleOnlineStatusResponse({})
        expect(friendStore.onlineStatus.size).toBe(0)
      })

      it('should update existing status', () => {
        friendStore.onlineStatus.set(1, false)
        friendStore.handleOnlineStatusResponse({ 1: true })
        expect(friendStore.onlineStatus.get(1)).toBe(true)
      })
    })

    describe('handleOnlineStatusChange', () => {
      it('should update single user online status to online', () => {
        friendStore.handleOnlineStatusChange(1, true)
        expect(friendStore.onlineStatus.get(1)).toBe(true)
      })

      it('should update single user online status to offline', () => {
        friendStore.onlineStatus.set(1, true)
        friendStore.handleOnlineStatusChange(1, false)
        expect(friendStore.onlineStatus.get(1)).toBe(false)
      })
    })

    describe('requestOnlineStatus', () => {
      it('should call websocketService for online status', () => {
        friendStore.friends = [
          createMockFriend({ id: 1 }),
          createMockFriend({ id: 2 }),
        ]

        friendStore.requestOnlineStatus()

        expect(websocketService.requestOnlineStatus).toHaveBeenCalledWith([1, 2])
        expect(websocketService.subscribeOnlineStatus).toHaveBeenCalledWith([1, 2])
      })

      it('should not call websocketService if no friends', () => {
        friendStore.friends = []

        friendStore.requestOnlineStatus()

        expect(websocketService.requestOnlineStatus).not.toHaveBeenCalled()
        expect(websocketService.subscribeOnlineStatus).not.toHaveBeenCalled()
      })

      it('should handle websocket errors gracefully', async () => {
        friendStore.friends = [createMockFriend({ id: 1 })]
        vi.mocked(websocketService.requestOnlineStatus).mockRejectedValue(
          new Error('WebSocket error')
        )

        // Should not throw
        friendStore.requestOnlineStatus()

        // Give time for the promise to settle
        await new Promise((resolve) => setTimeout(resolve, 10))
        // No assertion needed - just verifying it doesn't throw
      })
    })

    describe('reset', () => {
      it('should reset all state to initial values', () => {
        friendStore.friends = [createMockFriend({ id: 1 })]
        friendStore.friendRequests = [createMockFriendRequest({ id: 1 })]
        friendStore.onlineStatus.set(1, true)

        friendStore.reset()

        expect(friendStore.friends).toEqual([])
        expect(friendStore.friendRequests).toEqual([])
        expect(friendStore.onlineStatus.size).toBe(0)
      })
    })
  })

  describe('Integration', () => {
    it('should maintain correct online count after fetching friends', async () => {
      const mockFriends = [
        createMockFriend({ id: 1 }),
        createMockFriend({ id: 2 }),
        createMockFriend({ id: 3 }),
      ]
      vi.mocked(friendApi.getFriends).mockResolvedValue(mockFriends)

      await friendStore.fetchFriends()

      // Simulate online status response
      friendStore.handleOnlineStatusResponse({
        1: true,
        2: true,
        3: false,
      })

      expect(friendStore.onlineFriendsCount).toBe(2)
      expect(friendStore.sortedFriends[0].id).toBe(1) // Online user first
    })

    it('should update sorting when online status changes', async () => {
      friendStore.friends = [
        createMockFriend({ id: 1, nickname: 'Alice' }),
        createMockFriend({ id: 2, nickname: 'Bob' }),
      ]

      // Initially all offline - sorted alphabetically
      expect(friendStore.sortedFriends[0].nickname).toBe('Alice')

      // Bob comes online
      friendStore.handleOnlineStatusChange(2, true)

      // Now Bob should be first
      expect(friendStore.sortedFriends[0].nickname).toBe('Bob')
    })

    it('should handle accept friend request flow', async () => {
      const newFriend = createMockFriend({ id: 2, nickname: 'New Friend' })
      friendStore.friendRequests = [
        createMockFriendRequest({ id: 1, status: 'pending', fromUser: createMockUser({ id: 2 }) }),
      ]

      vi.mocked(friendApi.acceptFriendRequest).mockResolvedValue(
        createMockFriendRequest({ id: 1, status: 'accepted' })
      )
      vi.mocked(friendApi.getFriends).mockResolvedValue([newFriend])

      await friendStore.acceptFriendRequest(1)

      expect(friendStore.friendRequests[0].status).toBe('accepted')
      expect(friendStore.friends).toContainEqual(newFriend)
    })
  })
})
