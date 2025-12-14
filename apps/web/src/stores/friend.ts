import { defineStore } from 'pinia'
import type { Friend, FriendRequest } from '@/types'
import { friendApi } from '@/api/friend'
import { websocketService } from '@/services/websocket'

interface FriendState {
  friends: Friend[]
  friendRequests: FriendRequest[]
  onlineStatus: Map<number, boolean> // userId -> isOnline
  loading: boolean
  loadingRequests: boolean
}

export const useFriendStore = defineStore('friend', {
  state: (): FriendState => ({
    friends: [],
    friendRequests: [],
    onlineStatus: new Map(),
    loading: false,
    loadingRequests: false,
  }),

  getters: {
    sortedFriends: (state): Friend[] => {
      // Sort: online first, then alphabetically
      return [...state.friends].sort((a, b) => {
        const aOnline = state.onlineStatus.get(a.id) || false
        const bOnline = state.onlineStatus.get(b.id) || false

        if (aOnline && !bOnline) return -1
        if (!aOnline && bOnline) return 1

        const aName = a.remark || a.nickname
        const bName = b.remark || b.nickname
        return aName.localeCompare(bName)
      })
    },

    onlineFriendsCount: (state): number => {
      let count = 0
      for (const friend of state.friends) {
        if (state.onlineStatus.get(friend.id)) {
          count++
        }
      }
      return count
    },

    pendingRequestsCount: (state): number => {
      return state.friendRequests.filter((r) => r.status === 'pending').length
    },

    isUserOnline:
      (state) =>
      (userId: number): boolean => {
        return state.onlineStatus.get(userId) || false
      },
  },

  actions: {
    async fetchFriends() {
      this.loading = true
      try {
        this.friends = await friendApi.getFriends()

        // Request online status for all friends
        this.requestOnlineStatus()
      } finally {
        this.loading = false
      }
    },

    async fetchFriendRequests() {
      this.loadingRequests = true
      try {
        this.friendRequests = await friendApi.getFriendRequests()
      } finally {
        this.loadingRequests = false
      }
    },

    requestOnlineStatus() {
      const friendIds = this.friends.map((f) => f.id)
      if (friendIds.length > 0) {
        websocketService.requestOnlineStatus(friendIds).catch((err) => {
          console.warn('[FriendStore] Failed to request online status:', err)
        })

        // Also subscribe to status updates
        websocketService.subscribeOnlineStatus(friendIds).catch((err) => {
          console.warn('[FriendStore] Failed to subscribe to online status:', err)
        })
      }
    },

    handleOnlineStatusResponse(statuses: Record<number, boolean>) {
      for (const [userIdStr, isOnline] of Object.entries(statuses)) {
        const userId = parseInt(userIdStr, 10)
        this.onlineStatus.set(userId, isOnline)
      }
    },

    handleOnlineStatusChange(userId: number, isOnline: boolean) {
      this.onlineStatus.set(userId, isOnline)
    },

    async sendFriendRequest(uid: string, message?: string) {
      await friendApi.sendFriendRequest({ uid, message })
    },

    async acceptFriendRequest(requestId: number) {
      await friendApi.acceptFriendRequest(requestId)
      const request = this.friendRequests.find((r) => r.id === requestId)
      if (request) {
        request.status = 'accepted'
        // Refresh friends list to include the new friend
        await this.fetchFriends()
      }
    },

    async rejectFriendRequest(requestId: number) {
      await friendApi.rejectFriendRequest(requestId)
      const request = this.friendRequests.find((r) => r.id === requestId)
      if (request) {
        request.status = 'rejected'
      }
    },

    async deleteFriend(friendId: number) {
      await friendApi.deleteFriend(friendId)
      this.friends = this.friends.filter((f) => f.id !== friendId)
      this.onlineStatus.delete(friendId)
    },

    async setFriendRemark(friendId: number, remark: string) {
      await friendApi.updateRemark(friendId, remark)
      const friend = this.friends.find((f) => f.id === friendId)
      if (friend) {
        friend.remark = remark
      }
    },

    reset() {
      this.friends = []
      this.friendRequests = []
      this.onlineStatus.clear()
    },
  },
})
