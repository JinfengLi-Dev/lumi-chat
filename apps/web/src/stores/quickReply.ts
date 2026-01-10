import { defineStore } from 'pinia'
import { ref } from 'vue'
import { quickReplyApi, type QuickReply } from '@/api/quickReply'

export const useQuickReplyStore = defineStore('quickReply', () => {
  const quickReplies = ref<QuickReply[]>([])
  const loading = ref(false)
  const loaded = ref(false)

  async function fetchQuickReplies() {
    if (loading.value) return
    loading.value = true
    try {
      quickReplies.value = await quickReplyApi.getAll()
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  async function createQuickReply(content: string) {
    const reply = await quickReplyApi.create({ content })
    quickReplies.value.push(reply)
    return reply
  }

  async function updateQuickReply(id: number, content: string) {
    const reply = await quickReplyApi.update(id, { content })
    const index = quickReplies.value.findIndex((r) => r.id === id)
    if (index !== -1) {
      quickReplies.value[index] = reply
    }
    return reply
  }

  async function deleteQuickReply(id: number) {
    await quickReplyApi.delete(id)
    quickReplies.value = quickReplies.value.filter((r) => r.id !== id)
  }

  async function reorderQuickReplies(ids: number[]) {
    const replies = await quickReplyApi.reorder(ids)
    quickReplies.value = replies
    return replies
  }

  function reset() {
    quickReplies.value = []
    loaded.value = false
  }

  return {
    quickReplies,
    loading,
    loaded,
    fetchQuickReplies,
    createQuickReply,
    updateQuickReply,
    deleteQuickReply,
    reorderQuickReplies,
    reset,
  }
})
