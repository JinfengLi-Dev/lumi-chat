import { apiClient } from './client'
import type { ApiResponse } from '@/types'

export interface GroupDetail {
  id: number
  gid: string
  name: string
  avatar?: string
  ownerId: number
  ownerNickname?: string
  announcement?: string
  maxMembers: number
  memberCount: number
  createdAt: string
}

export interface GroupMember {
  id: number
  userId: number
  uid: string
  nickname: string
  groupNickname?: string
  avatar?: string
  role: 'owner' | 'admin' | 'member'
  joinedAt: string
}

export interface CreateGroupParams {
  name: string
  avatar?: string
  announcement?: string
  memberIds?: number[]
}

export interface UpdateGroupParams {
  name?: string
  avatar?: string
  announcement?: string
}

export const groupApi = {
  /**
   * Get all groups for current user
   * GET /groups
   */
  async getGroups(): Promise<GroupDetail[]> {
    const response = await apiClient.get<ApiResponse<GroupDetail[]>>('/groups')
    return response.data.data
  },

  /**
   * Create a new group
   * POST /groups
   */
  async createGroup(params: CreateGroupParams): Promise<GroupDetail> {
    const response = await apiClient.post<ApiResponse<GroupDetail>>('/groups', params)
    return response.data.data
  },

  /**
   * Get group details
   * GET /groups/{id}
   */
  async getGroup(groupId: number): Promise<GroupDetail> {
    const response = await apiClient.get<ApiResponse<GroupDetail>>(`/groups/${groupId}`)
    return response.data.data
  },

  /**
   * Update group info (owner/admin only)
   * PUT /groups/{id}
   */
  async updateGroup(groupId: number, params: UpdateGroupParams): Promise<GroupDetail> {
    const response = await apiClient.put<ApiResponse<GroupDetail>>(`/groups/${groupId}`, params)
    return response.data.data
  },

  /**
   * Delete group (owner only)
   * DELETE /groups/{id}
   */
  async deleteGroup(groupId: number): Promise<void> {
    await apiClient.delete(`/groups/${groupId}`)
  },

  /**
   * Get group members
   * GET /groups/{id}/members
   */
  async getGroupMembers(groupId: number): Promise<GroupMember[]> {
    const response = await apiClient.get<ApiResponse<GroupMember[]>>(`/groups/${groupId}/members`)
    return response.data.data
  },

  /**
   * Add members to group
   * POST /groups/{id}/members
   */
  async addMembers(groupId: number, memberIds: number[]): Promise<GroupMember[]> {
    const response = await apiClient.post<ApiResponse<GroupMember[]>>(
      `/groups/${groupId}/members`,
      { memberIds }
    )
    return response.data.data
  },

  /**
   * Remove member from group (owner/admin only)
   * DELETE /groups/{id}/members/{uid}
   */
  async removeMember(groupId: number, userId: number): Promise<void> {
    await apiClient.delete(`/groups/${groupId}/members/${userId}`)
  },

  /**
   * Transfer group ownership (owner only)
   * POST /groups/{id}/transfer
   */
  async transferOwnership(groupId: number, newOwnerId: number): Promise<GroupDetail> {
    const response = await apiClient.post<ApiResponse<GroupDetail>>(
      `/groups/${groupId}/transfer`,
      { newOwnerId }
    )
    return response.data.data
  },

  /**
   * Leave group
   * POST /groups/{id}/leave
   */
  async leaveGroup(groupId: number): Promise<void> {
    await apiClient.post(`/groups/${groupId}/leave`)
  },
}
