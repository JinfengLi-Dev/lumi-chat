import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'lumi-chat-theme'

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>('system')
  const isDark = ref(false)

  // Get system preference
  function getSystemPreference(): boolean {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  // Apply theme to DOM
  function applyTheme(dark: boolean) {
    isDark.value = dark
    const html = document.documentElement

    if (dark) {
      html.classList.add('dark')
      html.setAttribute('data-theme', 'dark')
    } else {
      html.classList.remove('dark')
      html.setAttribute('data-theme', 'light')
    }
  }

  // Update theme based on mode
  function updateTheme() {
    if (mode.value === 'system') {
      applyTheme(getSystemPreference())
    } else {
      applyTheme(mode.value === 'dark')
    }
  }

  // Set theme mode
  function setMode(newMode: ThemeMode) {
    mode.value = newMode
    localStorage.setItem(STORAGE_KEY, newMode)
    updateTheme()
  }

  // Toggle between light and dark (ignores system)
  function toggle() {
    if (mode.value === 'system') {
      // If on system, switch to opposite of current
      setMode(isDark.value ? 'light' : 'dark')
    } else {
      setMode(mode.value === 'light' ? 'dark' : 'light')
    }
  }

  // Initialize from localStorage and system preference
  function init() {
    const stored = localStorage.getItem(STORAGE_KEY) as ThemeMode | null
    if (stored && ['light', 'dark', 'system'].includes(stored)) {
      mode.value = stored
    }
    updateTheme()

    // Listen for system preference changes
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', () => {
      if (mode.value === 'system') {
        updateTheme()
      }
    })
  }

  // Watch for mode changes
  watch(mode, updateTheme)

  return {
    mode,
    isDark,
    setMode,
    toggle,
    init,
  }
})
