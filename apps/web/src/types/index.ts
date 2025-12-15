// User types
export interface User {
  id: number
  uid: string
  email: string
  nickname: string
  avatar?: string
  gender: 'male' | 'female' | 'unknown'
  signature?: string
  description?: string
  phone?: string
  status: 'active' | 'inactive' | 'banned'
  createdAt: string
  lastLoginAt?: string
  lastLoginIp?: string
}

export interface UserDevice {
  id: number
  deviceId: string
  deviceType: 'web' | 'ios' | 'android' | 'pc' | 'tablet'
  deviceName?: string
  lastActiveAt: string
  isOnline: boolean
}

// Authentication types
export interface LoginRequest {
  email: string
  password: string
  deviceId: string
  deviceType: string
  deviceName?: string
  rememberMe?: boolean
}

export interface LoginResponse {
  token: string
  refreshToken: string
  user: User
  expiresAt: number
}

export interface RegisterRequest {
  email: string
  password: string
  nickname: string
  gender: 'male' | 'female' | 'unknown'
}

// Friend types
export interface Friend extends User {
  remark?: string
  memo?: string
  friendshipCreatedAt: string
}

export interface FriendRequest {
  id: number
  fromUser: User
  toUser: User
  message?: string
  status: 'pending' | 'accepted' | 'rejected'
  createdAt: string
  handledAt?: string
}

// Group types
export interface Group {
  id: number
  gid: string
  name: string
  avatar?: string
  ownerId: number
  owner?: User
  creatorId: number
  creator?: User
  announcement?: string
  maxMembers: number
  memberCount?: number
  createdAt: string
}

export interface GroupMember {
  id: number
  userId: number
  user?: User
  nickname?: string
  role: 'owner' | 'admin' | 'member'
  joinedAt: string
  invitedBy?: number
}

// Conversation types
// Note: Backend uses 'private_chat' for private conversations
export type ConversationType = 'private_chat' | 'group' | 'system' | 'stranger'

export interface Conversation {
  id: number
  type: ConversationType
  participantIds: number[]
  groupId?: number
  group?: Group
  targetUser?: User
  lastMessage?: Message
  lastMsgTime?: string
  unreadCount: number
  isMuted: boolean
  isPinned: boolean
  atMsgIds?: number[]
  draft?: string
  backgroundUrl?: string
}

// Message types
export type MessageType =
  | 'text'
  | 'image'
  | 'file'
  | 'voice'
  | 'video'
  | 'location'
  | 'user_card'
  | 'group_card'
  | 'system'
  | 'recall'

export interface Message {
  id: number
  msgId: string
  conversationId: number
  senderId: number
  sender?: User
  senderDeviceId?: string
  msgType: MessageType
  content: string
  metadata?: MessageMetadata
  quoteMsgId?: string
  quoteMessage?: Message
  atUserIds?: number[]
  clientCreatedAt?: string
  serverCreatedAt: string
  recalledAt?: string
  status?: 'sending' | 'sent' | 'delivered' | 'read' | 'failed'
}

export interface MessageMetadata {
  // Image
  width?: number
  height?: number
  thumbnailUrl?: string

  // File
  fileName?: string
  fileSize?: number
  fileType?: string
  fileUrl?: string
  fileId?: string
  mimeType?: string
  expiresAt?: string

  // Voice/Video
  duration?: number

  // Location
  latitude?: number
  longitude?: number
  address?: string
  mapPreviewUrl?: string

  // User card
  cardUserId?: number
  cardUser?: User
  userId?: number
  uid?: string
  nickname?: string
  avatar?: string

  // Group card
  cardGroupId?: number
  cardGroup?: Group
  groupId?: number
  gid?: string
  name?: string
  memberCount?: number
}

// WebSocket message types
export interface WSMessage {
  type: string
  data: any
  timestamp: number
}

export interface WSChatMessage {
  msgId: string
  conversationId: number
  senderId: number
  msgType: MessageType
  content: string
  metadata?: MessageMetadata
  quoteMsgId?: string
  atUserIds?: number[]
  clientCreatedAt: string
}

// Sync types
export interface SyncResponse {
  newMessages: Message[]
  recalledMessages: { msgId: string; recalledAt: string }[]
  readStatusUpdates: { conversationId: number; lastReadMsgId: number }[]
  conversationUpdates: Conversation[]
  syncCursor: number
}

// API response wrapper
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PagedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  hasMore: boolean
}
