<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import type { GroupMember } from '@/api/group'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  members: GroupMember[]
  searchText: string
  isOwner: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'select', member: GroupMember | 'all'): void
}>()

const selectedIndex = ref(0)
const listRef = ref<HTMLElement | null>(null)

const filteredMembers = computed(() => {
  if (!props.searchText) return props.members

  const query = props.searchText.toLowerCase()
  return props.members.filter((member) => {
    const name = member.groupNickname || member.nickname
    return name.toLowerCase().includes(query)
  })
})

const showAllOption = computed(() => {
  return props.isOwner && (!props.searchText || 'all'.includes(props.searchText.toLowerCase()))
})

const totalItems = computed(() => {
  return filteredMembers.value.length + (showAllOption.value ? 1 : 0)
})

const menuStyle = computed(() => ({
  position: 'fixed' as const,
  left: `${props.x}px`,
  bottom: `${window.innerHeight - props.y + 10}px`,
  zIndex: 9999,
}))

function getDisplayName(member: GroupMember): string {
  return member.groupNickname || member.nickname
}

function handleSelect(member: GroupMember | 'all') {
  emit('select', member)
  emit('update:visible', false)
}

function handleKeyDown(event: KeyboardEvent) {
  if (!props.visible) return

  switch (event.key) {
    case 'ArrowUp':
      event.preventDefault()
      selectedIndex.value = Math.max(0, selectedIndex.value - 1)
      scrollToSelected()
      break
    case 'ArrowDown':
      event.preventDefault()
      selectedIndex.value = Math.min(totalItems.value - 1, selectedIndex.value + 1)
      scrollToSelected()
      break
    case 'Enter':
      event.preventDefault()
      selectCurrent()
      break
    case 'Escape':
      event.preventDefault()
      emit('update:visible', false)
      break
  }
}

function selectCurrent() {
  if (showAllOption.value && selectedIndex.value === 0) {
    handleSelect('all')
  } else {
    const memberIndex = showAllOption.value ? selectedIndex.value - 1 : selectedIndex.value
    if (memberIndex >= 0 && memberIndex < filteredMembers.value.length) {
      handleSelect(filteredMembers.value[memberIndex])
    }
  }
}

function scrollToSelected() {
  nextTick(() => {
    if (listRef.value) {
      const selectedItem = listRef.value.querySelector('.mention-item.selected') as HTMLElement
      if (selectedItem) {
        selectedItem.scrollIntoView({ block: 'nearest' })
      }
    }
  })
}

// Reset selected index when list changes
watch([() => props.visible, () => props.searchText], () => {
  selectedIndex.value = 0
})

// Add keyboard listener when visible
watch(() => props.visible, (visible) => {
  if (visible) {
    window.addEventListener('keydown', handleKeyDown)
  } else {
    window.removeEventListener('keydown', handleKeyDown)
  }
})

// Expose for parent to call
defineExpose({
  handleKeyDown,
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible && totalItems > 0"
      ref="listRef"
      class="mention-selector"
      :style="menuStyle"
    >
      <!-- @all option for owner -->
      <div
        v-if="showAllOption"
        class="mention-item all-option"
        :class="{ selected: selectedIndex === 0 }"
        @click="handleSelect('all')"
        @mouseenter="selectedIndex = 0"
      >
        <el-avatar :size="28" shape="circle" class="all-avatar">@</el-avatar>
        <div class="mention-name">@All Members</div>
      </div>

      <!-- Member list -->
      <div
        v-for="(member, index) in filteredMembers"
        :key="member.id"
        class="mention-item"
        :class="{ selected: selectedIndex === (showAllOption ? index + 1 : index) }"
        @click="handleSelect(member)"
        @mouseenter="selectedIndex = showAllOption ? index + 1 : index"
      >
        <el-avatar :src="member.avatar" :size="28" shape="circle">
          {{ getDisplayName(member).charAt(0) }}
        </el-avatar>
        <div class="mention-info">
          <div class="mention-name">{{ getDisplayName(member) }}</div>
          <div v-if="member.groupNickname && member.groupNickname !== member.nickname" class="mention-real-name">
            {{ member.nickname }}
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="filteredMembers.length === 0 && !showAllOption" class="mention-empty">
        No members found
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.mention-selector {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-height: 240px;
  overflow-y: auto;
  min-width: 200px;
  max-width: 280px;
}

.mention-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.mention-item:hover,
.mention-item.selected {
  background-color: var(--el-fill-color-light);
}

.all-option {
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.all-avatar {
  background: var(--el-color-primary);
  color: white;
  font-weight: bold;
}

.mention-info {
  flex: 1;
  min-width: 0;
}

.mention-name {
  font-size: 14px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mention-real-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mention-empty {
  padding: 16px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style>
