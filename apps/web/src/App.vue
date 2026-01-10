<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

const userStore = useUserStore()
const themeStore = useThemeStore()

onMounted(async () => {
  // Initialize theme
  themeStore.init()

  // Try to restore session on app load
  if (userStore.token && !userStore.isLoggedIn) {
    try {
      await userStore.fetchCurrentUser()
    } catch {
      // Token invalid, will be redirected to login
    }
  }
})
</script>

<template>
  <router-view />
</template>

<style>
/* Global styles are in main.scss */
</style>
