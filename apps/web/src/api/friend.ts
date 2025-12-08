import { apiClient } from './client'
import type { Friend, FriendRequest, ApiResponse } from '@/types'

export interface SendFriendRequestParams {
  uid: string
  message?: string
}

export const friendApi = {
  /**
   * Get all friends
   * GET /friends
   */
  async getFriends(includeBlocked = false): Promise<Friend[]> {
    const response = await apiClient.get<ApiResponse<Friend[]>>('/friends', {
      params: { includeBlocked },
    })
    return response.data.data
  },

  /**
   * Send a friend request
   * POST /friends/request
   */
  async sendFriendRequest(params: SendFriendRequestParams): Promise<FriendRequest> {
    const response = await apiClient.post<ApiResponse<FriendRequest>>('/friends/request', params)
    return response.data.data
  },

  /**
   * Get friend requests (received)
   * GET /friends/requests
   */
  async getFriendRequests(pendingOnly = true): Promise<FriendRequest[]> {
    const response = await apiClient.get<ApiResponse<FriendRequest[]>>('/friends/requests', {
      params: { pendingOnly },
    })
    return response.data.data
  },

  /**
   * Accept a friend request
   * POST /friends/requests/{id}/accept
   */
  async acceptFriendRequest(requestId: number): Promise<FriendRequest> {
    const response = await apiClient.post<ApiResponse<FriendRequest>>(
      `/friends/requests/${requestId}/accept`
    )
    return response.data.data
  },

  /**
   * Reject a friend request
   * POST /friends/requests/{id}/reject
   */
  async rejectFriendRequest(requestId: number): Promise<FriendRequest> {
    const response = await apiClient.post<ApiResponse<FriendRequest>>(
      `/friends/requests/${requestId}/reject`
    )
    return response.data.data
  },

  /**
   * Delete a friend (unfriend)
   * DELETE /friends/{id}
   */
  async deleteFriend(friendId: number): Promise<void> {
    await apiClient.delete(`/friends/${friendId}`)
  },

  /**
   * Update friend remark/alias
   * PUT /friends/{id}/remark
   */
  async updateRemark(friendId: number, remark: string): Promise<void> {
    await apiClient.put(`/friends/${friendId}/remark`, { remark })
  },

  /**
   * Block a friend
   * POST /friends/{id}/block
   */
  async blockFriend(friendId: number): Promise<void> {
    await apiClient.post(`/friends/${friendId}/block`)
  },

  /**
   * Unblock a friend
   * POST /friends/{id}/unblock
   */
  async unblockFriend(friendId: number): Promise<void> {
    await apiClient.post(`/friends/${friendId}/unblock`)
  },
}
