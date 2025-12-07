<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Location as LocationIcon, Search, Aim } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface LocationData {
  latitude: number
  longitude: number
  address: string
}

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'select', location: LocationData): void
}>()

const searchQuery = ref('')
const isLoading = ref(false)
const selectedLocation = ref<LocationData | null>(null)
const recentLocations = ref<LocationData[]>([])

// Default location (can be set to user's current location)
const defaultLocation = ref({
  latitude: 39.9042,
  longitude: 116.4074,
  address: 'Beijing, China',
})

// Map preview URL for selected location
const mapPreviewUrl = computed(() => {
  const loc = selectedLocation.value || defaultLocation.value
  return `https://staticmap.openstreetmap.de/staticmap.php?center=${loc.latitude},${loc.longitude}&zoom=15&size=400x200&maptype=mapnik&markers=${loc.latitude},${loc.longitude},red-pushpin`
})

// Load recent locations from localStorage
onMounted(() => {
  try {
    const stored = localStorage.getItem('recentLocations')
    if (stored) {
      recentLocations.value = JSON.parse(stored)
    }
  } catch {
    // Ignore errors
  }
})

// Get current location using browser Geolocation API
async function getCurrentLocation() {
  if (!navigator.geolocation) {
    ElMessage.warning('Geolocation is not supported by your browser')
    return
  }

  isLoading.value = true

  try {
    const position = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      })
    })

    const { latitude, longitude } = position.coords

    // Try to get address from coordinates (reverse geocoding)
    // In a real app, you'd use a geocoding service
    const address = await reverseGeocode(latitude, longitude)

    selectedLocation.value = {
      latitude,
      longitude,
      address,
    }
  } catch (error: any) {
    if (error.code === 1) {
      ElMessage.warning('Location access was denied')
    } else if (error.code === 2) {
      ElMessage.warning('Location unavailable')
    } else if (error.code === 3) {
      ElMessage.warning('Location request timed out')
    } else {
      ElMessage.error('Failed to get current location')
    }
  } finally {
    isLoading.value = false
  }
}

// Reverse geocode coordinates to address
async function reverseGeocode(lat: number, lng: number): Promise<string> {
  try {
    const response = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`,
      {
        headers: {
          'User-Agent': 'LumiChat/1.0',
        },
      }
    )
    const data = await response.json()
    return data.display_name || `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  } catch {
    return `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  }
}

// Search for location by name
async function searchLocation() {
  if (!searchQuery.value.trim()) return

  isLoading.value = true

  try {
    const response = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(searchQuery.value)}&limit=1`,
      {
        headers: {
          'User-Agent': 'LumiChat/1.0',
        },
      }
    )
    const data = await response.json()

    if (data.length > 0) {
      selectedLocation.value = {
        latitude: parseFloat(data[0].lat),
        longitude: parseFloat(data[0].lon),
        address: data[0].display_name,
      }
    } else {
      ElMessage.warning('Location not found')
    }
  } catch {
    ElMessage.error('Search failed')
  } finally {
    isLoading.value = false
  }
}

function selectRecentLocation(location: LocationData) {
  selectedLocation.value = location
}

function confirmSelection() {
  if (!selectedLocation.value) {
    ElMessage.warning('Please select a location')
    return
  }

  // Save to recent locations
  const recent = recentLocations.value.filter(
    (loc) =>
      loc.latitude !== selectedLocation.value!.latitude ||
      loc.longitude !== selectedLocation.value!.longitude
  )
  recent.unshift(selectedLocation.value)
  recentLocations.value = recent.slice(0, 5)
  localStorage.setItem('recentLocations', JSON.stringify(recentLocations.value))

  emit('select', selectedLocation.value)
  close()
}

function close() {
  emit('update:visible', false)
  searchQuery.value = ''
  selectedLocation.value = null
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="location-picker-overlay" @click.self="close">
      <div class="location-picker">
        <div class="picker-header">
          <span class="title">Send Location</span>
          <el-button text @click="close">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <!-- Search bar -->
        <div class="search-bar">
          <el-input
            v-model="searchQuery"
            placeholder="Search for a location..."
            :prefix-icon="Search"
            clearable
            @keyup.enter="searchLocation"
          />
          <el-button type="primary" :loading="isLoading" @click="searchLocation">
            Search
          </el-button>
        </div>

        <!-- Current location button -->
        <div class="current-location">
          <el-button :loading="isLoading" @click="getCurrentLocation">
            <el-icon><Aim /></el-icon>
            Use Current Location
          </el-button>
        </div>

        <!-- Map preview -->
        <div class="map-container">
          <img
            :src="mapPreviewUrl"
            alt="Map Preview"
            class="map-preview"
            @error="($event.target as HTMLImageElement).style.display = 'none'"
          />
          <div class="map-pin">
            <el-icon :size="32" color="#f56c6c"><LocationIcon /></el-icon>
          </div>
        </div>

        <!-- Selected location info -->
        <div v-if="selectedLocation" class="selected-info">
          <el-icon><LocationIcon /></el-icon>
          <span class="address">{{ selectedLocation.address }}</span>
        </div>

        <!-- Recent locations -->
        <div v-if="recentLocations.length > 0 && !selectedLocation" class="recent-locations">
          <div class="section-title">Recent Locations</div>
          <div
            v-for="(location, index) in recentLocations"
            :key="index"
            class="recent-item"
            @click="selectRecentLocation(location)"
          >
            <el-icon><LocationIcon /></el-icon>
            <span>{{ location.address }}</span>
          </div>
        </div>

        <!-- Actions -->
        <div class="picker-actions">
          <el-button @click="close">Cancel</el-button>
          <el-button type="primary" :disabled="!selectedLocation" @click="confirmSelection">
            Send Location
          </el-button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.location-picker-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.location-picker {
  width: 480px;
  max-height: 80vh;
  background: var(--el-bg-color);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.search-bar {
  display: flex;
  gap: 8px;
  padding: 16px 20px;
}

.search-bar .el-input {
  flex: 1;
}

.current-location {
  padding: 0 20px 16px;
}

.map-container {
  position: relative;
  height: 200px;
  margin: 0 20px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.map-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.map-pin {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -100%);
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}

.selected-info {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 20px;
  background: var(--el-fill-color-lighter);
  margin: 16px 20px 0;
  border-radius: 8px;
}

.selected-info .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--el-color-primary);
}

.address {
  font-size: 13px;
  color: var(--el-text-color-primary);
  line-height: 1.4;
}

.recent-locations {
  padding: 16px 20px 0;
}

.section-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.recent-item:hover {
  background: var(--el-fill-color-light);
}

.recent-item .el-icon {
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.recent-item span {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.picker-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: auto;
}
</style>
