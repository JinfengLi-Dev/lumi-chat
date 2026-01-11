import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useThemeStore } from '@/stores/theme'

export interface KeyboardShortcut {
  key: string
  ctrl?: boolean
  alt?: boolean
  shift?: boolean
  meta?: boolean
  description: string
  action: () => void
  global?: boolean
}

const STORAGE_KEY = 'lumi-chat-shortcuts-enabled'

export function useKeyboardShortcuts() {
  const router = useRouter()
  const chatStore = useChatStore()
  const themeStore = useThemeStore()

  const isEnabled = ref(true)
  const showHelp = ref(false)

  // Load preference from localStorage
  function loadPreference() {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored !== null) {
      isEnabled.value = stored === 'true'
    }
  }

  // Save preference to localStorage
  function setEnabled(enabled: boolean) {
    isEnabled.value = enabled
    localStorage.setItem(STORAGE_KEY, String(enabled))
  }

  // Define keyboard shortcuts
  const shortcuts: KeyboardShortcut[] = [
    // Navigation shortcuts
    {
      key: '1',
      ctrl: true,
      description: 'Go to Messages',
      action: () => router.push('/chat'),
      global: true,
    },
    {
      key: '2',
      ctrl: true,
      description: 'Go to Contacts',
      action: () => router.push('/chat?tab=contacts'),
      global: true,
    },
    {
      key: '3',
      ctrl: true,
      description: 'Go to Groups',
      action: () => router.push('/chat?tab=groups'),
      global: true,
    },
    {
      key: ',',
      ctrl: true,
      description: 'Open Settings',
      action: () => router.push('/settings'),
      global: true,
    },
    // Theme toggle
    {
      key: 'd',
      ctrl: true,
      shift: true,
      description: 'Toggle Dark Mode',
      action: () => themeStore.toggle(),
      global: true,
    },
    // Help
    {
      key: '/',
      ctrl: true,
      description: 'Show Keyboard Shortcuts',
      action: () => {
        showHelp.value = !showHelp.value
      },
      global: true,
    },
    // Escape to close dialogs/panels
    {
      key: 'Escape',
      description: 'Close dialog/panel',
      action: () => {
        showHelp.value = false
      },
      global: true,
    },
    // Search in conversation list
    {
      key: 'f',
      ctrl: true,
      description: 'Focus search',
      action: () => {
        const searchInput = document.querySelector('.conversation-list-header input') as HTMLInputElement
        searchInput?.focus()
      },
      global: true,
    },
    // Navigate conversations
    {
      key: 'ArrowUp',
      alt: true,
      description: 'Previous conversation',
      action: () => {
        navigateConversation(-1)
      },
      global: true,
    },
    {
      key: 'ArrowDown',
      alt: true,
      description: 'Next conversation',
      action: () => {
        navigateConversation(1)
      },
      global: true,
    },
  ]

  function navigateConversation(direction: number) {
    const conversations = chatStore.sortedConversations
    if (conversations.length === 0) return

    const currentConvId = chatStore.currentConversationId
    if (!currentConvId) {
      // Select first conversation
      if (conversations.length > 0) {
        router.push(`/chat/conversation/${conversations[0].id}`)
      }
      return
    }

    const currentIndex = conversations.findIndex((c) => c.id === currentConvId)
    if (currentIndex === -1) return

    const newIndex = Math.max(0, Math.min(conversations.length - 1, currentIndex + direction))
    if (newIndex !== currentIndex) {
      router.push(`/chat/conversation/${conversations[newIndex].id}`)
    }
  }

  function matchesShortcut(event: KeyboardEvent, shortcut: KeyboardShortcut): boolean {
    const ctrlOrMeta = event.ctrlKey || event.metaKey

    return (
      event.key.toLowerCase() === shortcut.key.toLowerCase() &&
      (shortcut.ctrl ? ctrlOrMeta : !ctrlOrMeta) &&
      (shortcut.alt ? event.altKey : !event.altKey) &&
      (shortcut.shift ? event.shiftKey : !event.shiftKey)
    )
  }

  function handleKeydown(event: KeyboardEvent) {
    if (!isEnabled.value) return

    // Don't trigger shortcuts when typing in input fields (except Escape and Help)
    const target = event.target as HTMLElement
    const isInputField =
      target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable

    for (const shortcut of shortcuts) {
      if (matchesShortcut(event, shortcut)) {
        // Allow certain shortcuts even in input fields
        if (isInputField && shortcut.key !== 'Escape' && shortcut.key !== '/') {
          continue
        }

        event.preventDefault()
        shortcut.action()
        return
      }
    }
  }

  function formatShortcut(shortcut: KeyboardShortcut): string {
    const parts: string[] = []
    if (shortcut.ctrl) parts.push('Ctrl')
    if (shortcut.alt) parts.push('Alt')
    if (shortcut.shift) parts.push('Shift')
    if (shortcut.meta) parts.push('Cmd')

    let key = shortcut.key
    if (key === 'ArrowUp') key = '↑'
    else if (key === 'ArrowDown') key = '↓'
    else if (key === 'ArrowLeft') key = '←'
    else if (key === 'ArrowRight') key = '→'
    else if (key === 'Escape') key = 'Esc'

    parts.push(key.toUpperCase())
    return parts.join(' + ')
  }

  // Install global keyboard handler
  onMounted(() => {
    loadPreference()
    document.addEventListener('keydown', handleKeydown)
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
  })

  return {
    isEnabled,
    setEnabled,
    showHelp,
    shortcuts,
    formatShortcut,
  }
}
