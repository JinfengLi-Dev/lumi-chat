<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import { useFriendStore } from '@/stores/friend'
import { useUserStore } from '@/stores/user'
import { useWebSocketStore } from '@/stores/websocket'
import { useDebounceFn } from '@vueuse/core'
import { fileApi, friendApi, messageApi, conversationApi } from '@/api'
import type { UploadProgress } from '@/api/file'
import type { Message, User, Group } from '@/types'
import MessageContextMenu from '@/components/chat/MessageContextMenu.vue'
import ForwardMessageDialog from '@/components/chat/ForwardMessageDialog.vue'
import EmojiPicker from '@/components/chat/EmojiPicker.vue'
import ImageLightbox from '@/components/chat/ImageLightbox.vue'
import LocationMessage from '@/components/chat/LocationMessage.vue'
import LocationPicker from '@/components/chat/LocationPicker.vue'
import UserCardMessage from '@/components/chat/UserCardMessage.vue'
import UserCardPicker from '@/components/chat/UserCardPicker.vue'
import GroupCardMessage from '@/components/chat/GroupCardMessage.vue'
import GroupCardPicker from '@/components/chat/GroupCardPicker.vue'
import TypingIndicator from '@/components/chat/TypingIndicator.vue'
import MessageStatus from '@/components/chat/MessageStatus.vue'
import FileMessage from '@/components/chat/FileMessage.vue'
import { getErrorMessage } from '@/utils/errorHandler'

const route = useRoute()
const chatStore = useChatStore()
const friendStore = useFriendStore()
const userStore = useUserStore()
const wsStore = useWebSocketStore()

const messageInput = ref('')
const messageListRef = ref<HTMLDivElement>()
const loading = ref(false)
const showInfoPanel = ref(true)
const imageInputRef = ref<HTMLInputElement>()
const fileInputRef = ref<HTMLInputElement>()
const uploadProgress = ref<UploadProgress | null>(null)
const isUploading = ref(false)

// Context menu state
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const selectedMessage = ref<Message | null>(null)

// Forward dialog state
const showForwardDialog = ref(false)
const messageToForward = ref<Message | null>(null)

// Quote state
const quotedMessage = ref<Message | null>(null)

// Emoji picker state
const showEmojiPicker = ref(false)

// Image lightbox state
const lightboxVisible = ref(false)
const lightboxImages = ref<string[]>([])
const lightboxIndex = ref(0)

// Location picker state
const showLocationPicker = ref(false)

// User card picker state
const showUserCardPicker = ref(false)
const availableFriends = ref<User[]>([])

// Group card picker state
const showGroupCardPicker = ref(false)
const availableGroups = ref<Group[]>([])

// Remark and memo editing state
const isEditingRemark = ref(false)
const editingRemark = ref('')
const editingMemo = ref('')
const isSavingRemark = ref(false)
const isSavingMemo = ref(false)

// Search state
const searchQuery = ref('')
const searchResults = ref<Message[]>([])
const isSearching = ref(false)
const showSearchResults = ref(false)

// Background state
const backgroundInputRef = ref<HTMLInputElement>()
const isUploadingBackground = ref(false)

// Typing indicator - connected to WebSocket store
const typingUsers = computed(() =>
  chatStore.currentTypingUsers.map((t) => ({
    id: t.userId,
    uid: '',
    nickname: t.nickname,
    avatar: t.avatar,
    email: '',
    gender: 'male' as const,
    status: 'active' as const,
    createdAt: '',
  }))
)

// Debounced typing notification sender
const sendTypingNotification = useDebounceFn(() => {
  if (conversationId.value && wsStore.isConnected) {
    wsStore.sendTyping(conversationId.value)
  }
}, 1000, { maxWait: 2000 })

// Debounced read receipt sender to prevent rapid-fire calls
const debouncedSendReadReceipt = useDebounceFn(() => {
  sendReadReceipt()
}, 500)

const conversationId = computed(() => Number(route.params.id))
const conversation = computed(() => chatStore.currentConversation)
const messages = computed(() => chatStore.currentMessages)
const hasMore = computed(() => chatStore.hasMoreMessages.get(conversationId.value) ?? true)

// Track the last read status to force re-renders when read receipts arrive
const lastReadByOther = computed(() => {
  if (!conversationId.value) return undefined
  return chatStore.lastReadByOther.get(conversationId.value)
})

watch(conversationId, async (id) => {
  if (id) {
    chatStore.setCurrentConversation(id)
    await loadMessages()
  }
}, { immediate: true })

// Watch for new messages and send read receipt
watch(
  () => messages.value.length,
  (newLen, oldLen) => {
    if (newLen > (oldLen ?? 0) && conversationId.value) {
      // New message arrived, send debounced read receipt
      debouncedSendReadReceipt()
    }
  }
)

async function loadMessages() {
  if (!conversationId.value) return
  loading.value = true
  try {
    await chatStore.fetchMessages(conversationId.value)
    await nextTick()
    scrollToBottom()
    // Send read receipt after messages are loaded
    sendReadReceipt()
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || 'Failed to load messages')
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
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || 'Failed to send message')
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

function handleInput() {
  // Send typing notification when user types
  sendTypingNotification()
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

function handleContextMenu(e: MouseEvent, msg: Message) {
  e.preventDefault()
  selectedMessage.value = msg
  contextMenuX.value = e.clientX
  contextMenuY.value = e.clientY
  contextMenuVisible.value = true
}

function handleContextMenuCopy() {
  // Copy is handled in the context menu component
}

async function handleContextMenuRecall() {
  if (!selectedMessage.value) return

  try {
    await chatStore.recallMessage(selectedMessage.value.msgId)
    ElMessage.success('Message recalled')
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || 'Failed to recall message')
  }
}

function handleContextMenuForward() {
  if (!selectedMessage.value) return
  messageToForward.value = selectedMessage.value
  showForwardDialog.value = true
}

async function handleForwardConfirm(conversationIds: number[]) {
  if (!messageToForward.value) return

  try {
    await chatStore.forwardMessage(messageToForward.value.msgId, conversationIds)
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || 'Failed to forward message')
  }
}

async function handleContextMenuDelete() {
  if (!selectedMessage.value) return

  try {
    await chatStore.deleteMessage(selectedMessage.value.msgId)
    ElMessage.success('Message deleted')
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || 'Failed to delete message')
  }
}

function handleContextMenuQuote() {
  if (!selectedMessage.value) return
  quotedMessage.value = selectedMessage.value
  // Focus the input
}

function clearQuotedMessage() {
  quotedMessage.value = null
}

function triggerImageUpload() {
  imageInputRef.value?.click()
}

function triggerFileUpload() {
  fileInputRef.value?.click()
}

async function handleImageSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('Please select an image file')
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('Image size must be less than 10MB')
    return
  }

  await uploadAndSendFile(file, 'image')
  if (imageInputRef.value) {
    imageInputRef.value.value = ''
  }
}

async function handleFileSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('File size must be less than 50MB')
    return
  }

  await uploadAndSendFile(file, 'file')
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

async function uploadAndSendFile(file: File, type: 'image' | 'file') {
  isUploading.value = true
  uploadProgress.value = { loaded: 0, total: file.size, percent: 0 }

  try {
    const fileInfo = await fileApi.uploadFile(file, type, (progress) => {
      uploadProgress.value = progress
    })

    // Prepare message metadata
    const metadata = {
      fileName: fileInfo.fileName || file.name,
      fileSize: fileInfo.fileSize || file.size,
      fileType: file.type,
      fileUrl: fileInfo.url,
      fileId: fileInfo.fileId,
      mimeType: fileInfo.mimeType,
      expiresAt: fileInfo.expiresAt,
      ...(type === 'image' && fileInfo.width && {
        width: fileInfo.width,
        height: fileInfo.height,
        thumbnailUrl: fileInfo.thumbnailUrl,
      }),
    }

    // Send message with file URL as content
    await chatStore.sendMessage(
      conversationId.value,
      type,
      fileInfo.url,
      metadata
    )

    await nextTick()
    scrollToBottom()
    ElMessage.success(`${type === 'image' ? 'Image' : 'File'} sent successfully`)
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error) || `Failed to upload ${type}`)
  } finally {
    isUploading.value = false
    uploadProgress.value = null
  }
}

// Emoji picker handler
function handleEmojiSelect(emoji: string) {
  messageInput.value += emoji
  showEmojiPicker.value = false
}

// Image lightbox handler
function openLightbox(imageUrl: string) {
  // Collect all image URLs from messages
  lightboxImages.value = messages.value
    .filter((m) => m.msgType === 'image' && !m.recalledAt)
    .map((m) => m.content)

  lightboxIndex.value = lightboxImages.value.indexOf(imageUrl)
  if (lightboxIndex.value === -1) {
    lightboxImages.value = [imageUrl]
    lightboxIndex.value = 0
  }
  lightboxVisible.value = true
}

// File retry handler
function handleRetryFileUpload(_message: Message) {
  // For retry, we would need to have stored the original file
  // Since we don't have it, we show a message to the user
  ElMessage.info('Please re-send the file by selecting it again')
}

// Location picker handler
async function handleLocationSelect(location: { latitude: number; longitude: number; address: string }) {
  showLocationPicker.value = false
  try {
    await chatStore.sendMessage(conversationId.value, 'location', location.address, {
      latitude: location.latitude,
      longitude: location.longitude,
      address: location.address,
    })
    await nextTick()
    scrollToBottom()
  } catch {
    ElMessage.error('Failed to send location')
  }
}

// User card picker handler
async function handleUserCardSelect(user: User) {
  showUserCardPicker.value = false
  try {
    await chatStore.sendMessage(conversationId.value, 'user_card', user.uid, {
      userId: user.id,
      uid: user.uid,
      nickname: user.nickname,
      avatar: user.avatar,
    })
    await nextTick()
    scrollToBottom()
  } catch {
    ElMessage.error('Failed to send contact card')
  }
}

// Group card picker handler
async function handleGroupCardSelect(group: Group) {
  showGroupCardPicker.value = false
  try {
    await chatStore.sendMessage(conversationId.value, 'group_card', group.gid || String(group.id), {
      groupId: group.id,
      gid: group.gid,
      name: group.name,
      avatar: group.avatar,
      memberCount: group.memberCount,
    })
    await nextTick()
    scrollToBottom()
  } catch {
    ElMessage.error('Failed to send group card')
  }
}

// Message status helper
function getMessageStatus(msg: Message): 'sending' | 'sent' | 'delivered' | 'read' | 'failed' {
  if (msg.recalledAt) return 'sent'
  if (msg.status === 'failed') return 'failed'
  if (msg.status === 'sending') return 'sending'

  // Check if the message has been read by the other user (private chats only)
  if (
    msg.id &&
    conversation.value?.type === 'private_chat' &&
    chatStore.isMessageReadByOther(conversationId.value, msg.id)
  ) {
    return 'read'
  }

  // Default to delivered for persisted messages
  if (msg.id) return 'delivered'
  return 'sending'
}

// Auto-send read receipt when messages are viewed
function sendReadReceipt() {
  if (!conversationId.value || !wsStore.isConnected) {
    console.log('[UI] sendReadReceipt skipped: not connected or no conversationId')
    return
  }
  if (!conversation.value || conversation.value.type === 'group') {
    console.log('[UI] sendReadReceipt skipped: no conversation or is group')
    return
  }

  // Find the last message from the other user
  const otherUserMessages = messages.value.filter((m) => !isSelf(m.senderId) && m.id)
  if (otherUserMessages.length === 0) {
    console.log('[UI] sendReadReceipt skipped: no messages from other user')
    return
  }

  const lastMessage = otherUserMessages[otherUserMessages.length - 1]
  if (!lastMessage?.id) return

  console.log('[UI] sendReadReceipt: sending for lastMessage.id=', lastMessage.id, 'conversationId=', conversationId.value)
  wsStore.sendReadAck(conversationId.value, lastMessage.id)
}

// Get friend data for the current conversation (if it's a private chat with a friend)
const currentFriend = computed(() => {
  if (!conversation.value || conversation.value.type !== 'private_chat') return null
  if (!conversation.value.targetUser) return null
  return friendStore.friends.find((f) => f.id === conversation.value?.targetUser?.id) || null
})

// Initialize remark and memo when conversation changes
watch(currentFriend, (friend) => {
  if (friend) {
    editingRemark.value = friend.remark || ''
    editingMemo.value = friend.memo || ''
  } else {
    editingRemark.value = ''
    editingMemo.value = ''
  }
}, { immediate: true })

// Start editing remark
function startEditingRemark() {
  editingRemark.value = currentFriend.value?.remark || ''
  isEditingRemark.value = true
}

// Cancel editing remark
function cancelEditingRemark() {
  editingRemark.value = currentFriend.value?.remark || ''
  isEditingRemark.value = false
}

// Save remark
async function saveRemark() {
  if (!currentFriend.value) return
  isSavingRemark.value = true
  try {
    await friendApi.updateRemark(currentFriend.value.id, editingRemark.value)
    // Update local friend data
    await friendStore.fetchFriends()
    isEditingRemark.value = false
    ElMessage.success('Remark updated')
  } catch {
    ElMessage.error('Failed to update remark')
  } finally {
    isSavingRemark.value = false
  }
}

// Save memo (auto-save on blur)
async function saveMemo() {
  if (!currentFriend.value) return
  if (editingMemo.value === (currentFriend.value.memo || '')) return
  isSavingMemo.value = true
  try {
    await friendApi.updateMemo(currentFriend.value.id, editingMemo.value)
    // Update local friend data
    await friendStore.fetchFriends()
    ElMessage.success('Memo saved')
  } catch {
    ElMessage.error('Failed to save memo')
  } finally {
    isSavingMemo.value = false
  }
}

// Search messages in conversation
async function handleSearch() {
  if (!conversationId.value || !searchQuery.value.trim()) {
    searchResults.value = []
    showSearchResults.value = false
    return
  }

  isSearching.value = true
  try {
    const results = await messageApi.searchMessages(conversationId.value, searchQuery.value.trim())
    searchResults.value = results
    showSearchResults.value = true
  } catch {
    ElMessage.error('Failed to search messages')
  } finally {
    isSearching.value = false
  }
}

// Clear search
function clearSearch() {
  searchQuery.value = ''
  searchResults.value = []
  showSearchResults.value = false
}

// Scroll to a specific message
function scrollToMessage(msgId: string) {
  const messageElement = document.querySelector(`[data-msg-id="${msgId}"]`)
  if (messageElement) {
    messageElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    // Highlight the message briefly
    messageElement.classList.add('highlight')
    setTimeout(() => {
      messageElement.classList.remove('highlight')
    }, 2000)
  }
  showSearchResults.value = false
}

// Background handlers
function triggerBackgroundUpload() {
  backgroundInputRef.value?.click()
}

async function handleBackgroundSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !conversationId.value) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('Please select an image file')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('Image size must be less than 5MB')
    return
  }

  isUploadingBackground.value = true
  try {
    // Upload to file storage
    const fileInfo = await fileApi.uploadFile(file, 'image')
    // Update conversation background
    await conversationApi.updateBackground(conversationId.value, fileInfo.url)
    // Refresh conversations to get updated data
    await chatStore.fetchConversations()
    ElMessage.success('Background updated')
  } catch {
    ElMessage.error('Failed to update background')
  } finally {
    isUploadingBackground.value = false
    if (backgroundInputRef.value) {
      backgroundInputRef.value.value = ''
    }
  }
}

async function removeBackground() {
  if (!conversationId.value) return
  try {
    await conversationApi.updateBackground(conversationId.value, null)
    await chatStore.fetchConversations()
    ElMessage.success('Background removed')
  } catch {
    ElMessage.error('Failed to remove background')
  }
}

// Clear chat history with confirmation
async function handleClearChatHistory() {
  if (!conversationId.value) return

  try {
    await ElMessageBox.confirm(
      'This will clear all messages in this conversation. This action cannot be undone.',
      'Clear Chat History',
      {
        confirmButtonText: 'Clear',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )

    await chatStore.clearChatHistory(conversationId.value)
    ElMessage.success('Chat history cleared')
  } catch (error) {
    // User cancelled or error occurred
    if (error !== 'cancel') {
      ElMessage.error('Failed to clear chat history')
    }
  }
}

onMounted(() => {
  scrollToBottom()
})

onUnmounted(() => {
  // Clear typing indicators when leaving conversation
  if (conversationId.value) {
    chatStore.clearTypingUsers(conversationId.value)
  }
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
    <div
      ref="messageListRef"
      class="chat-area-messages"
      :style="conversation.backgroundUrl ? { backgroundImage: `url(${conversation.backgroundUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}"
      @scroll="loadMoreMessages"
    >
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
        :key="`${msg.msgId}-${lastReadByOther}`"
        :data-msg-id="msg.msgId"
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
          <div v-else-if="msg.msgType === 'image'" class="content image-content">
            <img
              :src="msg.metadata?.thumbnailUrl || msg.content"
              style="max-width: 200px; max-height: 200px; border-radius: 4px; cursor: pointer"
              @click="openLightbox(msg.content)"
            />
          </div>

          <!-- File message -->
          <div v-else-if="msg.msgType === 'file'" class="content file-content">
            <FileMessage
              :message="msg"
              @retry="handleRetryFileUpload"
            />
          </div>

          <!-- Location message -->
          <div v-else-if="msg.msgType === 'location'" class="content">
            <LocationMessage
              :latitude="msg.metadata?.latitude || 0"
              :longitude="msg.metadata?.longitude || 0"
              :address="msg.metadata?.address || 'Unknown location'"
            />
          </div>

          <!-- User card message -->
          <div v-else-if="msg.msgType === 'user_card'" class="content card-content">
            <UserCardMessage
              :user="{
                id: msg.metadata?.userId || 0,
                uid: msg.metadata?.uid || '',
                nickname: msg.metadata?.nickname || 'Unknown',
                avatar: msg.metadata?.avatar,
                email: '',
                gender: 'male',
                status: 'active',
                createdAt: ''
              }"
              @view-profile="() => {}"
              @send-message="() => {}"
              @add-friend="() => {}"
            />
          </div>

          <!-- Group card message -->
          <div v-else-if="msg.msgType === 'group_card'" class="content card-content">
            <GroupCardMessage
              :group="{
                id: msg.metadata?.groupId || 0,
                gid: msg.metadata?.gid || '',
                name: msg.metadata?.name || 'Unknown Group',
                avatar: msg.metadata?.avatar,
                ownerId: 0,
                creatorId: 0,
                maxMembers: 200,
                memberCount: msg.metadata?.memberCount || 0,
                createdAt: ''
              }"
              @view-group="() => {}"
              @open-chat="() => {}"
              @join-group="() => {}"
            />
          </div>

          <!-- Other message types -->
          <div v-else class="content">
            [{{ msg.msgType }}]
          </div>

          <div class="message-footer">
            <span class="time">{{ formatTime(msg.serverCreatedAt) }}</span>
            <MessageStatus
              v-if="isSelf(msg.senderId)"
              :status="getMessageStatus(msg)"
              class="message-status"
            />
          </div>
        </div>
      </div>

      <!-- Typing Indicator -->
      <div v-if="typingUsers.length > 0" class="typing-indicator-wrapper">
        <TypingIndicator :typing-users="typingUsers" />
      </div>
    </div>

    <!-- Input Area -->
    <div class="chat-area-input">
      <!-- Hidden file inputs -->
      <input
        ref="imageInputRef"
        type="file"
        accept="image/*"
        hidden
        @change="handleImageSelect"
      />
      <input
        ref="fileInputRef"
        type="file"
        hidden
        @change="handleFileSelect"
      />

      <div class="toolbar">
        <el-tooltip content="Emoji" placement="top">
          <el-icon class="tool-btn" @click="showEmojiPicker = !showEmojiPicker"><Sunny /></el-icon>
        </el-tooltip>
        <el-tooltip content="Send Image" placement="top">
          <el-icon class="tool-btn" @click="triggerImageUpload"><PictureFilled /></el-icon>
        </el-tooltip>
        <el-tooltip content="Send File" placement="top">
          <el-icon class="tool-btn" @click="triggerFileUpload"><Folder /></el-icon>
        </el-tooltip>
        <el-tooltip content="Video (Coming Soon)" placement="top">
          <el-icon class="tool-btn disabled"><VideoCamera /></el-icon>
        </el-tooltip>
        <el-tooltip content="Send Location" placement="top">
          <el-icon class="tool-btn" @click="showLocationPicker = true"><Location /></el-icon>
        </el-tooltip>
        <el-tooltip content="Send Contact Card" placement="top">
          <el-icon class="tool-btn" @click="showUserCardPicker = true"><User /></el-icon>
        </el-tooltip>
        <el-tooltip content="Send Group Card" placement="top">
          <el-icon class="tool-btn" @click="showGroupCardPicker = true"><Postcard /></el-icon>
        </el-tooltip>
      </div>

      <!-- Emoji Picker -->
      <EmojiPicker
        v-if="showEmojiPicker"
        :visible="showEmojiPicker"
        @select="handleEmojiSelect"
        @close="showEmojiPicker = false"
        class="emoji-picker-popup"
      />

      <!-- Upload Progress -->
      <div v-if="isUploading && uploadProgress" class="upload-progress">
        <el-progress
          :percentage="uploadProgress.percent"
          :stroke-width="4"
          :show-text="true"
        />
      </div>

      <!-- Quoted Message Preview -->
      <div v-if="quotedMessage" class="quoted-message-preview">
        <div class="quoted-content">
          <span class="quoted-label">Replying to {{ quotedMessage.sender?.nickname || 'Unknown' }}:</span>
          <span class="quoted-text">
            {{ quotedMessage.msgType === 'text'
               ? (quotedMessage.content.length > 50 ? quotedMessage.content.slice(0, 50) + '...' : quotedMessage.content)
               : `[${quotedMessage.msgType}]`
            }}
          </span>
        </div>
        <el-icon class="close-quote" @click="clearQuotedMessage"><Close /></el-icon>
      </div>

      <div class="input-area">
        <textarea
          v-model="messageInput"
          placeholder="Type a message... (Enter to send, Ctrl+Enter for new line)"
          @keydown="handleKeydown"
          @input="handleInput"
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

      <!-- Remark (Tag) Section - Only for private chats with friends -->
      <div v-if="currentFriend" class="info-panel-section">
        <div class="section-title">Remark/Tag</div>
        <div v-if="!isEditingRemark" class="remark-display">
          <span class="remark-text">{{ currentFriend.remark || 'No remark' }}</span>
          <el-button type="primary" link size="small" @click="startEditingRemark">
            <el-icon><Edit /></el-icon>
          </el-button>
        </div>
        <div v-else class="remark-edit">
          <el-input
            v-model="editingRemark"
            size="small"
            placeholder="Enter remark"
            maxlength="50"
            show-word-limit
            style="flex: 1"
          />
          <el-button
            type="primary"
            size="small"
            :loading="isSavingRemark"
            @click="saveRemark"
          >
            Save
          </el-button>
          <el-button size="small" @click="cancelEditingRemark">Cancel</el-button>
        </div>
      </div>

      <!-- Memo Section - Only for private chats with friends -->
      <div v-if="currentFriend" class="info-panel-section">
        <div class="section-title">Memo</div>
        <el-input
          v-model="editingMemo"
          type="textarea"
          :rows="4"
          placeholder="Add notes about this contact..."
          maxlength="2000"
          show-word-limit
          @blur="saveMemo"
          :disabled="isSavingMemo"
        />
        <p v-if="isSavingMemo" style="font-size: 12px; color: #909399; margin-top: 5px">
          Saving...
        </p>
      </div>

      <!-- Search Chat History Section -->
      <div class="info-panel-section">
        <div class="section-title">Search Chat History</div>
        <div class="search-box">
          <el-input
            v-model="searchQuery"
            size="small"
            placeholder="Search messages..."
            clearable
            @keyup.enter="handleSearch"
            @clear="clearSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button
            type="primary"
            size="small"
            :loading="isSearching"
            @click="handleSearch"
            style="margin-left: 8px"
          >
            Search
          </el-button>
        </div>
        <div v-if="showSearchResults" class="search-results">
          <div v-if="searchResults.length === 0" class="no-results">
            No messages found
          </div>
          <div
            v-for="result in searchResults"
            :key="result.msgId"
            class="search-result-item"
            @click="scrollToMessage(result.msgId)"
          >
            <div class="result-sender">{{ result.sender?.nickname || 'Unknown' }}</div>
            <div class="result-content">{{ result.content }}</div>
            <div class="result-time">{{ formatTime(result.serverCreatedAt) }}</div>
          </div>
        </div>
      </div>

      <!-- Chat Background Section -->
      <div class="info-panel-section">
        <div class="section-title">Chat Background</div>
        <input
          ref="backgroundInputRef"
          type="file"
          accept="image/*"
          hidden
          @change="handleBackgroundSelect"
        />
        <div class="background-preview">
          <div
            v-if="conversation.backgroundUrl"
            class="background-image"
            :style="{ backgroundImage: `url(${conversation.backgroundUrl})` }"
          ></div>
          <div v-else class="no-background">No background set</div>
        </div>
        <div class="background-actions">
          <el-button
            size="small"
            :loading="isUploadingBackground"
            @click="triggerBackgroundUpload"
          >
            {{ conversation.backgroundUrl ? 'Change' : 'Set Background' }}
          </el-button>
          <el-button
            v-if="conversation.backgroundUrl"
            size="small"
            type="danger"
            plain
            @click="removeBackground"
          >
            Remove
          </el-button>
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

      <!-- Actions Section -->
      <div class="info-panel-section" style="border-top: 1px solid #ebeef5; padding-top: 15px; margin-top: 10px">
        <el-button
          type="danger"
          plain
          size="small"
          style="width: 100%"
          @click="handleClearChatHistory"
        >
          Clear Chat History
        </el-button>
      </div>
    </div>
  </div>

  <!-- Context Menu -->
  <MessageContextMenu
    v-model:visible="contextMenuVisible"
    :x="contextMenuX"
    :y="contextMenuY"
    :message="selectedMessage"
    :is-self="selectedMessage ? isSelf(selectedMessage.senderId) : false"
    @copy="handleContextMenuCopy"
    @recall="handleContextMenuRecall"
    @forward="handleContextMenuForward"
    @delete="handleContextMenuDelete"
    @quote="handleContextMenuQuote"
  />

  <!-- Forward Dialog -->
  <ForwardMessageDialog
    v-model="showForwardDialog"
    :message="messageToForward"
    @forward="handleForwardConfirm"
  />

  <!-- Image Lightbox -->
  <ImageLightbox
    v-model:visible="lightboxVisible"
    :images="lightboxImages"
    :initial-index="lightboxIndex"
  />

  <!-- Location Picker -->
  <LocationPicker
    v-model:visible="showLocationPicker"
    @select="handleLocationSelect"
  />

  <!-- User Card Picker -->
  <UserCardPicker
    :visible="showUserCardPicker"
    :friends="availableFriends"
    @update:visible="showUserCardPicker = $event"
    @select="handleUserCardSelect"
  />

  <!-- Group Card Picker -->
  <GroupCardPicker
    :visible="showGroupCardPicker"
    :groups="availableGroups"
    @update:visible="showGroupCardPicker = $event"
    @select="handleGroupCardSelect"
  />
</template>

<style scoped>
.remark-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.remark-text {
  color: #606266;
  font-size: 13px;
}

.remark-edit {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.info-panel-section {
  padding: 12px 15px;
  border-bottom: 1px solid #f0f0f0;
}

.info-panel-section:last-child {
  border-bottom: none;
}

/* Search styles */
.search-box {
  display: flex;
  align-items: center;
}

.search-results {
  margin-top: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.no-results {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 15px;
}

.search-result-item {
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:hover {
  background-color: #f5f7fa;
}

.search-result-item:last-child {
  border-bottom: none;
}

.result-sender {
  font-size: 12px;
  font-weight: 500;
  color: #303133;
}

.result-content {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.result-time {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* Message highlight animation */
:deep(.message.highlight) {
  animation: highlightMessage 2s ease-out;
}

@keyframes highlightMessage {
  0%, 50% {
    background-color: rgba(64, 158, 255, 0.2);
  }
  100% {
    background-color: transparent;
  }
}

/* Background section styles */
.background-preview {
  width: 100%;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 10px;
  border: 1px solid #e4e7ed;
}

.background-image {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
}

.no-background {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
  background-color: #f5f7fa;
}

.background-actions {
  display: flex;
  gap: 8px;
}
</style>
