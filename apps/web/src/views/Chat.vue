<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { useWebSocketStore } from '@/stores/websocket'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
const wsStore = useWebSocketStore()

const activeTab = ref<'messages' | 'contacts' | 'groups'>('messages')
const searchQuery = ref('')
const showSettingsMenu = ref(false)

const filteredConversations = computed(() => {
  if (!searchQuery.value) return chatStore.sortedConversations
  const query = searchQuery.value.toLowerCase()
  return chatStore.sortedConversations.filter((conv) => {
    const name = conv.group?.name || conv.targetUser?.nickname || ''
    return name.toLowerCase().includes(query)
  })
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
  } catch (error: any) {
    ElMessage.error('Failed to load conversations')
  }
})

onUnmounted(() => {
  // Cleanup WebSocket connection if needed
})

function selectConversation(id: number) {
  chatStore.setCurrentConversation(id)
  router.push(`/conversation/${id}`)
}

function handleAddAction(action: string) {
  if (action === 'friend') {
    // Open add friend dialog
  } else if (action === 'group') {
    // Open create group dialog
  }
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

function getLastMessagePreview(conv: any) {
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
</script>

<template>
  <div class="app-container">
    <!-- Left Sidebar -->
    <div class="sidebar">
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
          <el-icon :size="24"><UserFilled /></el-icon>
        </div>
      </div>

      <div class="sidebar-bottom">
        <el-dropdown trigger="click" @command="handleAddAction">
          <div class="sidebar-item">
            <el-icon :size="24"><Plus /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="friend">Add Friend</el-dropdown-item>
              <el-dropdown-item command="group">Create Group</el-dropdown-item>
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
    <div class="conversation-list">
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

      <div class="conversation-list-content">
        <div
          v-for="conv in filteredConversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: chatStore.currentConversationId === conv.id }"
          @click="selectConversation(conv.id)"
        >
          <div class="conversation-item-avatar">
            <el-avatar
              :src="conv.group?.avatar || conv.targetUser?.avatar"
              :size="45"
              shape="square"
            >
              {{ (conv.group?.name || conv.targetUser?.nickname || '?').charAt(0) }}
            </el-avatar>
          </div>

          <div class="conversation-item-content">
            <div class="conversation-item-header">
              <span class="name ellipsis">
                {{ conv.group?.name || conv.targetUser?.nickname || 'Unknown' }}
              </span>
              <span class="time">{{ formatTime(conv.lastMsgTime) }}</span>
            </div>

            <div class="conversation-item-message">
              <span v-if="conv.atMsgIds?.length" class="at-badge">[Someone @'d you]</span>
              <span class="text ellipsis">{{ getLastMessagePreview(conv) }}</span>
              <span v-if="conv.unreadCount > 0" class="badge">
                {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="filteredConversations.length === 0" class="flex-center" style="height: 200px; color: #909399">
          <span v-if="searchQuery">No results found</span>
          <span v-else>No conversations yet</span>
        </div>
      </div>
    </div>

    <!-- Chat Area -->
    <router-view v-slot="{ Component }">
      <component :is="Component" />
      <div v-if="!Component" class="chat-area flex-center" style="color: #909399">
        Select a conversation to start chatting
      </div>
    </router-view>
  </div>
</template>
