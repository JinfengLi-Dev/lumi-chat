<script setup lang="ts">
import { ref, computed } from 'vue'

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'select', emoji: string): void
  (e: 'update:visible', value: boolean): void
}>()

// Emoji categories with commonly used emojis
const categories = [
  {
    name: 'Smileys',
    icon: '😀',
    emojis: [
      '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂',
      '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩',
      '😘', '😗', '😚', '😙', '🥲', '😋', '😛', '😜',
      '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐',
      '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬',
      '🤥', '😌', '😔', '😪', '🤤', '😴', '😷', '🤒',
      '🤕', '🤢', '🤮', '🤧', '🥵', '🥶', '🥴', '😵',
      '🤯', '🤠', '🥳', '🥸', '😎', '🤓', '🧐', '😕',
    ],
  },
  {
    name: 'Gestures',
    icon: '👋',
    emojis: [
      '👋', '🤚', '🖐️', '✋', '🖖', '👌', '🤌', '🤏',
      '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆',
      '🖕', '👇', '☝️', '👍', '👎', '✊', '👊', '🤛',
      '🤜', '👏', '🙌', '👐', '🤲', '🤝', '🙏', '✍️',
      '💪', '🦾', '🦵', '🦶', '👂', '🦻', '👃', '🧠',
      '🫀', '🫁', '🦷', '🦴', '👀', '👁️', '👅', '👄',
    ],
  },
  {
    name: 'People',
    icon: '👨',
    emojis: [
      '👶', '👧', '🧒', '👦', '👩', '🧑', '👨', '👱',
      '🧔', '👵', '🧓', '👴', '👲', '👳', '🧕', '👮',
      '👷', '💂', '🕵️', '👩‍⚕️', '👨‍⚕️', '👩‍🌾', '👨‍🌾', '👩‍🍳',
      '👨‍🍳', '👩‍🎓', '👨‍🎓', '👩‍🎤', '👨‍🎤', '👩‍🏫', '👨‍🏫', '👩‍🏭',
      '👨‍🏭', '👩‍💻', '👨‍💻', '👩‍💼', '👨‍💼', '👩‍🔧', '👨‍🔧', '👩‍🔬',
      '👨‍🔬', '👩‍🎨', '👨‍🎨', '👩‍🚒', '👨‍🚒', '👩‍✈️', '👨‍✈️', '👩‍🚀',
    ],
  },
  {
    name: 'Animals',
    icon: '🐱',
    emojis: [
      '🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼',
      '🐻‍❄️', '🐨', '🐯', '🦁', '🐮', '🐷', '🐽', '🐸',
      '🐵', '🙈', '🙉', '🙊', '🐒', '🐔', '🐧', '🐦',
      '🐤', '🐣', '🐥', '🦆', '🦅', '🦉', '🦇', '🐺',
      '🐗', '🐴', '🦄', '🐝', '🪲', '🐛', '🦋', '🐌',
      '🐞', '🐜', '🪰', '🪱', '🦟', '🦗', '🕷️', '🦂',
    ],
  },
  {
    name: 'Food',
    icon: '🍔',
    emojis: [
      '🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓',
      '🫐', '🍈', '🍒', '🍑', '🥭', '🍍', '🥥', '🥝',
      '🍅', '🍆', '🥑', '🥦', '🥬', '🥒', '🌶️', '🫑',
      '🌽', '🥕', '🫒', '🧄', '🧅', '🥔', '🍠', '🥐',
      '🥯', '🍞', '🥖', '🥨', '🧀', '🥚', '🍳', '🧈',
      '🥞', '🧇', '🥓', '🥩', '🍗', '🍖', '🌭', '🍔',
      '🍟', '🍕', '🫓', '🥪', '🥙', '🧆', '🌮', '🌯',
      '🫔', '🥗', '🥘', '🫕', '🍝', '🍜', '🍲', '🍛',
    ],
  },
  {
    name: 'Activities',
    icon: '⚽',
    emojis: [
      '⚽', '🏀', '🏈', '⚾', '🥎', '🎾', '🏐', '🏉',
      '🥏', '🎱', '🪀', '🏓', '🏸', '🏒', '🏑', '🥍',
      '🏏', '🪃', '🥅', '⛳', '🪁', '🏹', '🎣', '🤿',
      '🥊', '🥋', '🎽', '🛹', '🛼', '🛷', '⛸️', '🥌',
      '🎿', '⛷️', '🏂', '🪂', '🏋️', '🤸', '🤼', '🤽',
      '🤾', '🤺', '⛹️', '🧗', '🏇', '🚴', '🚵', '🎪',
    ],
  },
  {
    name: 'Objects',
    icon: '💡',
    emojis: [
      '⌚', '📱', '📲', '💻', '⌨️', '🖥️', '🖨️', '🖱️',
      '🖲️', '💽', '💾', '💿', '📀', '📼', '📷', '📸',
      '📹', '🎥', '📽️', '🎞️', '📞', '☎️', '📟', '📠',
      '📺', '📻', '🎙️', '🎚️', '🎛️', '🧭', '⏱️', '⏲️',
      '⏰', '🕰️', '⌛', '⏳', '📡', '🔋', '🔌', '💡',
      '🔦', '🕯️', '🪔', '🧯', '🛢️', '💸', '💵', '💴',
    ],
  },
  {
    name: 'Symbols',
    icon: '❤️',
    emojis: [
      '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍',
      '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖',
      '💘', '💝', '💟', '☮️', '✝️', '☪️', '🕉️', '☸️',
      '✡️', '🔯', '🕎', '☯️', '☦️', '🛐', '⛎', '♈',
      '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐',
      '♑', '♒', '♓', '🆔', '⚛️', '🉑', '☢️', '☣️',
      '📴', '📳', '🈶', '🈚', '🈸', '🈺', '🈷️', '✴️',
      '🆚', '💮', '🉐', '㊙️', '㊗️', '🈴', '🈵', '🈹',
    ],
  },
]

const activeCategory = ref(0)
const recentEmojis = ref<string[]>(getRecentEmojis())

// Get emojis for current category
const currentEmojis = computed(() => {
  if (activeCategory.value === -1) {
    return recentEmojis.value
  }
  return categories[activeCategory.value]?.emojis || []
})

// Load recent emojis from localStorage
function getRecentEmojis(): string[] {
  try {
    const stored = localStorage.getItem('recentEmojis')
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

// Save emoji to recent
function saveToRecent(emoji: string) {
  const recent = recentEmojis.value.filter((e) => e !== emoji)
  recent.unshift(emoji)
  recentEmojis.value = recent.slice(0, 32) // Keep last 32
  localStorage.setItem('recentEmojis', JSON.stringify(recentEmojis.value))
}

function handleEmojiClick(emoji: string) {
  saveToRecent(emoji)
  emit('select', emoji)
}

function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="emoji-picker-overlay" @click="handleClose">
      <div class="emoji-picker" @click.stop>
        <!-- Category tabs -->
        <div class="emoji-categories">
          <div
            v-if="recentEmojis.length > 0"
            class="category-tab"
            :class="{ active: activeCategory === -1 }"
            @click="activeCategory = -1"
            title="Recent"
          >
            🕐
          </div>
          <div
            v-for="(cat, index) in categories"
            :key="cat.name"
            class="category-tab"
            :class="{ active: activeCategory === index }"
            @click="activeCategory = index"
            :title="cat.name"
          >
            {{ cat.icon }}
          </div>
        </div>

        <!-- Category name -->
        <div class="emoji-category-name">
          {{ activeCategory === -1 ? 'Recent' : categories[activeCategory]?.name }}
        </div>

        <!-- Emoji grid -->
        <div class="emoji-grid">
          <div
            v-for="emoji in currentEmojis"
            :key="emoji"
            class="emoji-item"
            @click="handleEmojiClick(emoji)"
          >
            {{ emoji }}
          </div>
          <div v-if="currentEmojis.length === 0" class="emoji-empty">
            No recent emojis
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.emoji-picker-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
}

.emoji-picker {
  position: fixed;
  bottom: 120px;
  left: 50%;
  transform: translateX(-50%);
  width: 340px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.emoji-categories {
  display: flex;
  gap: 2px;
  padding: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  overflow-x: auto;
}

.category-tab {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.category-tab:hover {
  background-color: var(--el-fill-color-light);
}

.category-tab.active {
  background-color: var(--el-color-primary-light-9);
}

.emoji-category-name {
  padding: 8px 12px 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
  padding: 8px 12px 12px;
  max-height: 240px;
  overflow-y: auto;
}

.emoji-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s, transform 0.1s;
}

.emoji-item:hover {
  background-color: var(--el-fill-color-light);
  transform: scale(1.1);
}

.emoji-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 20px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

/* Custom scrollbar */
.emoji-grid::-webkit-scrollbar {
  width: 6px;
}

.emoji-grid::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color);
  border-radius: 3px;
}

.emoji-grid::-webkit-scrollbar-track {
  background-color: transparent;
}
</style>
