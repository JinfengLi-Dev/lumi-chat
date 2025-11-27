<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()

const messageInput = ref('')
const messageListRef = ref<HTMLDivElement>()
const loading = ref(false)
const showInfoPanel = ref(true)

const conversationId = computed(() => Number(route.params.id))
const conversation = computed(() => chatStore.currentConversation)
const messages = computed(() => chatStore.currentMessages)
const hasMore = computed(() => chatStore.hasMoreMessages.get(conversationId.value) ?? true)

watch(conversationId, async (id) => {
  if (id) {
    chatStore.setCurrentConversation(id)
    await loadMessages()
  }
}, { immediate: true })

async function loadMessages() {
  if (!conversationId.value) return
  loading.value = true
  try {
    await chatStore.fetchMessages(conversationId.value)
    await nextTick()
    scrollToBottom()
  } catch (error: any) {
    ElMessage.error('Failed to load messages')
  } finally {
    loading.value = false
  }
}

async function loadMoreMessages() {
  if (!hasMore.value || loading.value) return
  const firstMsg = messages.value[0]
  if (!firstMsg) return

  loading.value = true
  try {
    await chatStore.fetchMessages(conversationId.value, firstMsg.id)
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  const content = messageInput.value.trim()
  if (!content) return

  messageInput.value = ''

  try {
    await chatStore.sendMessage(conversationId.value, 'text', content)
    await nextTick()
    scrollToBottom()
  } catch (error: any) {
    ElMessage.error('Failed to send message')
    messageInput.value = content
  }
}

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.ctrlKey && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  } else if (e.key === 'Enter' && e.ctrlKey) {
    messageInput.value += '\n'
  }
}

function formatTime(time: string) {
  return new Date(time).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function isSelf(senderId: number) {
  return senderId === userStore.userId
}

function handleContextMenu(e: MouseEvent, _msg: any) {
  e.preventDefault()
  // Show context menu
}

onMounted(() => {
  scrollToBottom()
})
</script>

<template>
  <div class="chat-area" v-if="conversation">
    <!-- Header -->
    <div class="chat-area-header">
      <div class="title">
        {{ conversation.group?.name || conversation.targetUser?.nickname || 'Chat' }}
        <el-tag v-if="conversation.type === 'stranger'" size="small" type="info">
          Stranger
        </el-tag>
        <el-tag v-if="conversation.type === 'group'" size="small">
          {{ conversation.group?.memberCount || 0 }} members
        </el-tag>
      </div>

      <div class="actions">
        <el-button :icon="showInfoPanel ? 'DArrowRight' : 'DArrowLeft'" text @click="showInfoPanel = !showInfoPanel" />
      </div>
    </div>

    <!-- Messages -->
    <div ref="messageListRef" class="chat-area-messages" @scroll="loadMoreMessages">
      <div v-if="loading && messages.length === 0" class="flex-center" style="height: 100%">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      </div>

      <div v-if="hasMore && messages.length > 0" class="flex-center" style="padding: 10px">
        <el-button text :loading="loading" @click="loadMoreMessages">
          Load more
        </el-button>
      </div>

      <div
        v-for="msg in messages"
        :key="msg.msgId"
        class="message"
        :class="{ self: isSelf(msg.senderId), other: !isSelf(msg.senderId) }"
        @contextmenu="handleContextMenu($event, msg)"
      >
        <div class="message-avatar">
          <el-avatar :src="msg.sender?.avatar" :size="40" shape="square">
            {{ msg.sender?.nickname?.charAt(0) || '?' }}
          </el-avatar>
        </div>

        <div class="message-bubble">
          <div v-if="!isSelf(msg.senderId) && conversation.type === 'group'" class="sender-name">
            {{ msg.sender?.nickname || 'Unknown' }}
          </div>

          <!-- Recalled message -->
          <div v-if="msg.recalledAt" class="message-recalled">
            {{ isSelf(msg.senderId) ? 'You recalled a message' : `${msg.sender?.nickname} recalled a message` }}
          </div>

          <!-- Text message -->
          <div v-else-if="msg.msgType === 'text'" class="content">
            {{ msg.content }}
          </div>

          <!-- Image message -->
          <div v-else-if="msg.msgType === 'image'" class="content">
            <el-image
              :src="msg.metadata?.thumbnailUrl || msg.content"
              fit="cover"
              style="max-width: 200px; max-height: 200px; border-radius: 4px"
              :preview-src-list="[msg.content]"
            />
          </div>

          <!-- File message -->
          <div v-else-if="msg.msgType === 'file'" class="content">
            <div style="display: flex; align-items: center; gap: 10px">
              <el-icon :size="32"><Document /></el-icon>
              <div>
                <div>{{ msg.metadata?.fileName || 'File' }}</div>
                <div style="font-size: 12px; color: #909399">
                  {{ ((msg.metadata?.fileSize || 0) / 1024).toFixed(1) }} KB
                </div>
              </div>
            </div>
          </div>

          <!-- Location message -->
          <div v-else-if="msg.msgType === 'location'" class="content">
            <div style="display: flex; align-items: center; gap: 10px">
              <el-icon :size="24"><Location /></el-icon>
              <span>{{ msg.metadata?.address || 'Location' }}</span>
            </div>
          </div>

          <!-- Other message types -->
          <div v-else class="content">
            [{{ msg.msgType }}]
          </div>

          <div class="time">{{ formatTime(msg.serverCreatedAt) }}</div>
        </div>
      </div>
    </div>

    <!-- Input Area -->
    <div class="chat-area-input">
      <div class="toolbar">
        <el-icon class="tool-btn"><PictureFilled /></el-icon>
        <el-icon class="tool-btn"><Folder /></el-icon>
        <el-icon class="tool-btn"><VideoCamera /></el-icon>
        <el-icon class="tool-btn"><Location /></el-icon>
        <el-icon class="tool-btn"><User /></el-icon>
        <el-icon class="tool-btn"><Postcard /></el-icon>
      </div>

      <div class="input-area">
        <textarea
          v-model="messageInput"
          placeholder="Type a message... (Enter to send, Ctrl+Enter for new line)"
          @keydown="handleKeydown"
        />
      </div>

      <div class="send-area">
        <span class="tip">Press Enter to send</span>
        <el-button type="primary" @click="sendMessage" :disabled="!messageInput.trim()">
          Send
        </el-button>
      </div>
    </div>
  </div>

  <!-- Info Panel -->
  <div v-if="conversation && showInfoPanel" class="info-panel">
    <div class="info-panel-header">
      <span class="title">
        {{ conversation.type === 'group' ? 'Group Info' : 'Contact Info' }}
      </span>
    </div>

    <div class="info-panel-content">
      <div class="info-panel-section">
        <div class="flex-center" style="flex-direction: column; padding: 20px 0">
          <el-avatar
            :src="conversation.group?.avatar || conversation.targetUser?.avatar"
            :size="80"
            shape="square"
          >
            {{ (conversation.group?.name || conversation.targetUser?.nickname || '?').charAt(0) }}
          </el-avatar>
          <h3 style="margin-top: 15px">
            {{ conversation.group?.name || conversation.targetUser?.nickname }}
          </h3>
          <p v-if="conversation.targetUser" style="color: #909399; font-size: 12px">
            ID: {{ conversation.targetUser.uid }}
          </p>
        </div>
      </div>

      <div v-if="conversation.group" class="info-panel-section">
        <div class="section-title">Announcement</div>
        <p style="color: #606266; font-size: 13px">
          {{ conversation.group.announcement || 'No announcement' }}
        </p>
      </div>

      <div v-if="conversation.group" class="info-panel-section">
        <div class="section-title">Members ({{ conversation.group.memberCount }})</div>
        <div style="display: flex; align-items: center; gap: 10px">
          <el-button size="small">Manage Members</el-button>
          <el-button size="small">Invite</el-button>
        </div>
      </div>
    </div>
  </div>
</template>
