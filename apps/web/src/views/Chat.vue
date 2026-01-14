<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Top, BellFilled, Operation } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { useWebSocketStore } from '@/stores/websocket'
import { useVirtualizer } from '@tanstack/vue-virtual'
import AddFriendDialog from '@/components/contact/AddFriendDialog.vue'
import FriendRequestsDialog from '@/components/contact/FriendRequestsDialog.vue'
import CreateGroupDialog from '@/components/group/CreateGroupDialog.vue'
import StartConversationDialog from '@/components/chat/StartConversationDialog.vue'
import FriendsList from '@/components/contact/FriendsList.vue'
import GroupsList from '@/components/group/GroupsList.vue'
import ConversationContextMenu from '@/components/chat/ConversationContextMenu.vue'
import ConversationSkeleton from '@/components/common/ConversationSkeleton.vue'
import type { Conversation } from '@/types'
import { getErrorMessage } from '@/utils/errorHandler'
import { conversationApi } from '@/api/conversation'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()
const wsStore = useWebSocketStore()

const activeTab = ref<'messages' | 'contacts' | 'groups'>('messages')
const searchQuery = ref('')
const showSettingsMenu = ref(false)
const showAddFriendDialog = ref(false)
const showFriendRequestsDialog = ref(false)
const showCreateGroupDialog = ref(false)
const showStartConversationDialog = ref(false)

// Mobile navigation state
const isMobileMenuOpen = ref(false)
const keyboardFocusIndex = ref(-1)

// Component refs for FriendsList and GroupsList
const friendsListRef = ref<InstanceType<typeof FriendsList>>()
const groupsListRef = ref<InstanceType<typeof GroupsList>>()
const conversationListRef = ref<HTMLElement>()
const conversationScrollContainerRef = ref<HTMLElement | null>(null)

// Check if we're on a conversation page (for mobile view)
const isConversationActive = computed(() => {
  return route.path.includes('/conversation/')
})

// Context menu state
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuConversation = ref<Conversation | null>(null)

const filteredConversations = computed(() => {
  if (!searchQuery.value) return chatStore.sortedConversations
  const query = searchQuery.value.toLowerCase()
  return chatStore.sortedConversations.filter((conv) => {
    const name = conv.group?.name || conv.targetUser?.nickname || ''
    return name.toLowerCase().includes(query)
  })
})

// Virtual scrolling for conversation list (must be after filteredConversations)
const conversationVirtualizer = useVirtualizer({
  get count() {
    return filteredConversations.value.length
  },
  getScrollElement: () => conversationScrollContainerRef.value,
  estimateSize: () => 72, // Height of conversation item
  overscan: 5,
})

const totalUnread = computed(() => chatStore.totalUnreadCount)

const connectionStatusText = computed(() => {
  switch (wsStore.status) {
    case 'connected':
      return 'Connected'
    case 'connecting':
      return 'Connecting...'
    case 'reconnecting':
      return `Reconnecting (${wsStore.reconnectAttempt})...`
    case 'disconnected':
      return 'Disconnected'
    default:
      return ''
  }
})
const connectionStatusColor = computed(() => {
  switch (wsStore.status) {
    case 'connected':
      return '#67c23a' // green
    case 'connecting':
    case 'reconnecting':
      return '#e6a23c' // yellow
    case 'disconnected':
      return '#f56c6c' // red
    default:
      return '#909399'
  }
})

onMounted(async () => {
  try {
    await chatStore.fetchConversations()
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || 'Failed to load conversations')
  }

  // Add keyboard event listener
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  // Cleanup keyboard event listener
  window.removeEventListener('keydown', handleKeyDown)
})

// Mobile menu toggle
function toggleMobileMenu() {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

function closeMobileMenu() {
  isMobileMenuOpen.value = false
}

// Keyboard navigation
function handleKeyDown(e: KeyboardEvent) {
  // Skip if user is typing in an input
  if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
    return
  }

  const conversations = filteredConversations.value

  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      if (keyboardFocusIndex.value < conversations.length - 1) {
        keyboardFocusIndex.value++
      } else {
        keyboardFocusIndex.value = 0
      }
      break
    case 'ArrowUp':
      e.preventDefault()
      if (keyboardFocusIndex.value > 0) {
        keyboardFocusIndex.value--
      } else {
        keyboardFocusIndex.value = conversations.length - 1
      }
      break
    case 'Enter':
      if (keyboardFocusIndex.value >= 0 && conversations[keyboardFocusIndex.value]) {
        selectConversation(conversations[keyboardFocusIndex.value].id)
      }
      break
    case 'Escape':
      closeMobileMenu()
      contextMenuVisible.value = false
      keyboardFocusIndex.value = -1
      break
  }
}

function selectConversation(id: number) {
  chatStore.setCurrentConversation(id)
  router.push(`/conversation/${id}`)
}

function handleAddAction(action: string) {
  if (action === 'conversation') {
    showStartConversationDialog.value = true
  } else if (action === 'friend') {
    showAddFriendDialog.value = true
  } else if (action === 'group') {
    showCreateGroupDialog.value = true
  } else if (action === 'requests') {
    showFriendRequestsDialog.value = true
  }
}

function handleConversationStarted(conversationId: number) {
  handleOpenConversation(conversationId)
}

function handleFriendRequestSent() {
  // Optionally refresh friends list or show notification
}

function handleFriendRequestHandled() {
  // Refresh conversations to show new friend conversations
  chatStore.fetchConversations()
}

function handleGroupCreated() {
  // Refresh conversations to show new group conversation
  chatStore.fetchConversations()
}

async function handleLogout() {
  try {
    await userStore.logout()
    router.push('/login')
  } catch {
    // Force logout even if API fails
    userStore.clearAuth()
    router.push('/login')
  }
}

function formatTime(time?: string) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (days === 1) {
    return 'Yesterday'
  } else if (days < 7) {
    return date.toLocaleDateString('en-US', { weekday: 'short' })
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
  }
}

function getLastMessagePreview(conv: Conversation) {
  const msg = conv.lastMessage
  if (!msg) return ''

  if (msg.recalledAt) return '[Message recalled]'

  switch (msg.msgType) {
    case 'text':
      return msg.content
    case 'image':
      return '[Image]'
    case 'file':
      return '[File]'
    case 'voice':
      return '[Voice]'
    case 'video':
      return '[Video]'
    case 'location':
      return '[Location]'
    case 'user_card':
      return '[Contact Card]'
    case 'group_card':
      return '[Group Card]'
    default:
      return ''
  }
}

// Handle opening conversation from FriendsList or GroupsList
function handleOpenConversation(conversationId: number) {
  selectConversation(conversationId)
  // Switch to messages tab to show the conversation
  activeTab.value = 'messages'
}

// Context menu handlers
function handleConversationContextMenu(e: MouseEvent, conv: Conversation) {
  e.preventDefault()
  contextMenuConversation.value = conv
  contextMenuX.value = e.clientX
  contextMenuY.value = e.clientY
  contextMenuVisible.value = true
}

async function handleContextMenuPin(conv: Conversation) {
  try {
    const newPinned = !conv.isPinned
    await conversationApi.togglePin(conv.id, newPinned)
    // Update in store to trigger reactive re-sort
    const storeConv = chatStore.conversations.find(c => c.id === conv.id)
    if (storeConv) {
      storeConv.isPinned = newPinned
    }
    ElMessage.success(newPinned ? 'Conversation pinned' : 'Conversation unpinned')
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || 'Failed to update pin status')
  }
}

async function handleContextMenuMute(conv: Conversation) {
  try {
    const newMuted = !conv.isMuted
    await conversationApi.toggleMute(conv.id, newMuted)
    // Update in store
    const storeConv = chatStore.conversations.find(c => c.id === conv.id)
    if (storeConv) {
      storeConv.isMuted = newMuted
    }
    ElMessage.success(newMuted ? 'Notifications muted' : 'Notifications unmuted')
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || 'Failed to update mute status')
  }
}

async function handleContextMenuMarkRead(conv: Conversation) {
  try {
    await chatStore.markAsRead(conv.id)
    ElMessage.success('Marked as read')
  } catch {
    ElMessage.error('Failed to mark as read')
  }
}

async function handleContextMenuDelete(conv: Conversation) {
  try {
    await chatStore.deleteConversation(conv.id)
    ElMessage.success('Conversation deleted')
  } catch {
    ElMessage.error('Failed to delete conversation')
  }
}
</script>

<template>
  <div class="app-container">
    <!-- Mobile Menu Toggle Button -->
    <button
      class="mobile-menu-toggle"
      @click="toggleMobileMenu"
      aria-label="Toggle navigation menu"
    >
      <el-icon :size="24"><Operation /></el-icon>
    </button>

    <!-- Mobile Overlay Backdrop -->
    <div
      class="mobile-overlay"
      :class="{ visible: isMobileMenuOpen }"
      @click="closeMobileMenu"
    ></div>

    <!-- Left Sidebar -->
    <div class="sidebar" :class="{ 'mobile-open': isMobileMenuOpen }">
      <el-avatar
        :src="userStore.user?.avatar"
        :size="45"
        class="sidebar-avatar"
        @click="router.push('/settings')"
      >
        {{ userStore.user?.nickname?.charAt(0) }}
      </el-avatar>

      <div class="sidebar-nav">
        <div
          class="sidebar-item"
          :class="{ active: activeTab === 'messages' }"
          @click="activeTab = 'messages'"
        >
          <el-icon :size="24"><ChatDotRound /></el-icon>
          <span v-if="totalUnread > 0" class="badge">
            {{ totalUnread > 99 ? '99+' : totalUnread }}
          </span>
        </div>

        <div
          class="sidebar-item"
          :class="{ active: activeTab === 'contacts' }"
          @click="activeTab = 'contacts'"
        >
          <el-icon :size="24"><User /></el-icon>
        </div>

        <div
          class="sidebar-item"
          :class="{ active: activeTab === 'groups' }"
          @click="activeTab = 'groups'"
        >
          <el-icon :size="24"><Coordinate /></el-icon>
        </div>
      </div>

      <div class="sidebar-bottom">
        <!-- Plus button with context-aware options based on active tab -->
        <el-dropdown
          trigger="click"
          @command="handleAddAction"
        >
          <div class="sidebar-item">
            <el-icon :size="24"><Plus /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="activeTab === 'messages'" command="conversation">Start Conversation</el-dropdown-item>
              <el-dropdown-item v-if="activeTab === 'contacts'" command="friend">Add Friend</el-dropdown-item>
              <el-dropdown-item v-if="activeTab === 'contacts'" command="requests">Friend Requests</el-dropdown-item>
              <el-dropdown-item v-if="activeTab === 'groups'" command="group">Create Group</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-dropdown trigger="click" v-model:visible="showSettingsMenu">
          <div class="sidebar-item">
            <el-icon :size="24"><Setting /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/settings')">
                Profile Settings
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                Logout
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Conversation List -->
    <div
      class="conversation-list"
      :class="{ 'hidden-mobile': isConversationActive }"
      ref="conversationListRef"
    >
      <div class="conversation-list-header">
        <div class="connection-status">
          <span
            class="status-dot"
            :style="{ backgroundColor: connectionStatusColor }"
          ></span>
          <span class="status-text">{{ connectionStatusText }}</span>
        </div>
        <el-input
          v-model="searchQuery"
          placeholder="Search"
          prefix-icon="Search"
          clearable
          class="search-input"
        />
      </div>

      <div class="conversation-list-content" ref="conversationScrollContainerRef">
        <!-- Messages Tab - Conversation List -->
        <template v-if="activeTab === 'messages'">
          <!-- Loading Skeleton -->
          <ConversationSkeleton v-if="chatStore.loading" :count="6" />

          <!-- Conversation Items with Virtual Scrolling -->
          <template v-else-if="filteredConversations.length > 0">
            <div
              :style="{
                height: `${conversationVirtualizer.getTotalSize()}px`,
                position: 'relative',
                width: '100%'
              }"
            >
              <div
                v-for="virtualRow in conversationVirtualizer.getVirtualItems()"
                :key="String(virtualRow.key)"
                :style="{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  transform: `translateY(${virtualRow.start}px)`,
                  height: `${virtualRow.size}px`
                }"
              >
                <div
                  class="conversation-item"
                  :class="{
                    active: chatStore.currentConversationId === filteredConversations[virtualRow.index].id,
                    pinned: filteredConversations[virtualRow.index].isPinned,
                    muted: filteredConversations[virtualRow.index].isMuted,
                    'keyboard-focus': keyboardFocusIndex === virtualRow.index
                  }"
                  :tabindex="0"
                  role="button"
                  :aria-selected="chatStore.currentConversationId === filteredConversations[virtualRow.index].id"
                  @click="selectConversation(filteredConversations[virtualRow.index].id)"
                  @contextmenu="(e) => handleConversationContextMenu(e, filteredConversations[virtualRow.index])"
                >
                  <div class="conversation-item-avatar">
                    <el-avatar
                      :src="filteredConversations[virtualRow.index].group?.avatar || filteredConversations[virtualRow.index].targetUser?.avatar"
                      :size="45"
                      shape="square"
                    >
                      {{ (filteredConversations[virtualRow.index].group?.name || filteredConversations[virtualRow.index].targetUser?.nickname || '?').charAt(0) }}
                    </el-avatar>
                  </div>

                  <div class="conversation-item-content">
                    <div class="conversation-item-header">
                      <span class="name ellipsis">
                        <el-icon v-if="filteredConversations[virtualRow.index].isPinned" class="pin-icon"><Top /></el-icon>
                        {{ filteredConversations[virtualRow.index].group?.name || filteredConversations[virtualRow.index].targetUser?.nickname || 'Unknown' }}
                      </span>
                      <span class="time">{{ formatTime(filteredConversations[virtualRow.index].lastMsgTime) }}</span>
                    </div>

                    <div class="conversation-item-message">
                      <span v-if="filteredConversations[virtualRow.index].atMsgIds?.length" class="at-badge">[Someone @'d you]</span>
                      <span class="text ellipsis">{{ getLastMessagePreview(filteredConversations[virtualRow.index]) }}</span>
                      <el-icon v-if="filteredConversations[virtualRow.index].isMuted" class="mute-icon"><BellFilled /></el-icon>
                      <span v-if="filteredConversations[virtualRow.index].unreadCount > 0" class="badge" :class="{ muted: filteredConversations[virtualRow.index].isMuted }">
                        {{ filteredConversations[virtualRow.index].unreadCount > 99 ? '99+' : filteredConversations[virtualRow.index].unreadCount }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- Enhanced Empty State -->
          <div v-else class="empty-state">
            <el-icon class="empty-state-icon"><ChatDotRound /></el-icon>
            <div class="empty-state-title">
              {{ searchQuery ? 'No results found' : 'No conversations yet' }}
            </div>
            <div class="empty-state-description">
              {{ searchQuery
                ? 'Try a different search term'
                : 'Start chatting by adding friends or creating a group'
              }}
            </div>
            <div v-if="!searchQuery" class="empty-state-action">
              <el-button type="primary" @click="showAddFriendDialog = true">
                Add Friend
              </el-button>
            </div>
          </div>
        </template>

        <!-- Contacts Tab - Friends List -->
        <FriendsList
          v-else-if="activeTab === 'contacts'"
          ref="friendsListRef"
          @open-conversation="handleOpenConversation"
        />

        <!-- Groups Tab - Groups List -->
        <GroupsList
          v-else-if="activeTab === 'groups'"
          ref="groupsListRef"
          :current-user-id="userStore.user?.id ?? 0"
          @open-conversation="handleOpenConversation"
        />
      </div>
    </div>

    <!-- Chat Area -->
    <router-view v-slot="{ Component }">
      <component :is="Component" />
      <div v-if="!Component" class="chat-area flex-center" style="color: #909399">
        Select a conversation to start chatting
      </div>
    </router-view>

    <!-- Dialogs -->
    <AddFriendDialog
      v-model="showAddFriendDialog"
      @request-sent="handleFriendRequestSent"
    />
    <FriendRequestsDialog
      v-model="showFriendRequestsDialog"
      @request-handled="handleFriendRequestHandled"
    />
    <CreateGroupDialog
      v-model="showCreateGroupDialog"
      @group-created="handleGroupCreated"
    />
    <StartConversationDialog
      v-model="showStartConversationDialog"
      @conversation-started="handleConversationStarted"
    />

    <!-- Conversation Context Menu -->
    <ConversationContextMenu
      v-model:visible="contextMenuVisible"
      :x="contextMenuX"
      :y="contextMenuY"
      :conversation="contextMenuConversation"
      @pin="handleContextMenuPin"
      @unpin="handleContextMenuPin"
      @mute="handleContextMenuMute"
      @unmute="handleContextMenuMute"
      @mark-read="handleContextMenuMarkRead"
      @delete="handleContextMenuDelete"
    />
  </div>
</template>
