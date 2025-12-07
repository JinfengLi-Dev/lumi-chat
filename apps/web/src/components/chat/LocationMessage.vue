<script setup lang="ts">
import { computed } from 'vue'
import { Location as LocationIcon } from '@element-plus/icons-vue'

const props = defineProps<{
  latitude: number
  longitude: number
  address?: string
  mapPreviewUrl?: string
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

// Generate a static map preview URL if none provided
// Using OpenStreetMap static image service
const mapImageUrl = computed(() => {
  if (props.mapPreviewUrl) {
    return props.mapPreviewUrl
  }
  // Fallback to a placeholder or OpenStreetMap embed
  return `https://staticmap.openstreetmap.de/staticmap.php?center=${props.latitude},${props.longitude}&zoom=15&size=200x120&maptype=mapnik&markers=${props.latitude},${props.longitude},red-pushpin`
})

// Format coordinates for display
const formattedCoords = computed(() => {
  const lat = props.latitude.toFixed(6)
  const lng = props.longitude.toFixed(6)
  return `${lat}, ${lng}`
})

// Open in external map service
function openInMaps() {
  const url = `https://www.openstreetmap.org/?mlat=${props.latitude}&mlon=${props.longitude}#map=16/${props.latitude}/${props.longitude}`
  window.open(url, '_blank')
}

function handleClick() {
  emit('click')
  openInMaps()
}
</script>

<template>
  <div class="location-message" @click="handleClick">
    <!-- Map preview -->
    <div class="map-preview">
      <img
        :src="mapImageUrl"
        :alt="address || 'Location'"
        class="map-image"
        @error="($event.target as HTMLImageElement).style.display = 'none'"
      />
      <div class="map-overlay">
        <el-icon :size="24" class="location-pin"><LocationIcon /></el-icon>
      </div>
    </div>

    <!-- Location info -->
    <div class="location-info">
      <div class="location-address" :title="address">
        {{ address || 'Unknown location' }}
      </div>
      <div class="location-coords">
        <el-icon :size="12"><LocationIcon /></el-icon>
        {{ formattedCoords }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.location-message {
  width: 200px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.location-message:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.map-preview {
  position: relative;
  width: 100%;
  height: 120px;
  background-color: var(--el-fill-color-light);
}

.map-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.map-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.1);
  pointer-events: none;
}

.location-pin {
  color: var(--el-color-danger);
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}

.location-info {
  padding: 8px 12px;
}

.location-address {
  font-size: 13px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.location-coords {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
</style>
