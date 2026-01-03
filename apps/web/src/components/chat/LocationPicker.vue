<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Location as LocationIcon, Search, Aim } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// Fix Leaflet default marker icon issue (webpack/vite bundling breaks default icon paths)
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

// @ts-ignore - Leaflet internal API
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

interface LocationData {
  latitude: number
  longitude: number
  address: string
}

const props = defineProps<{
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
const mapContainer = ref<HTMLDivElement | null>(null)

// Leaflet map and marker instances
let map: L.Map | null = null
let marker: L.Marker | null = null

// Default location (Beijing, China)
const defaultLocation = ref({
  latitude: 39.9042,
  longitude: 116.4074,
  address: 'Beijing, China',
})

// Initialize Leaflet map when component becomes visible
watch(
  () => props.visible,
  async (visible) => {
    if (visible) {
      await nextTick()
      initMap()
    } else {
      destroyMap()
    }
  }
)

// Update map when location changes
watch(selectedLocation, (loc) => {
  if (loc && map && marker) {
    map.setView([loc.latitude, loc.longitude], 15)
    marker.setLatLng([loc.latitude, loc.longitude])
  }
})

function initMap() {
  if (!mapContainer.value || map) return

  const loc = selectedLocation.value || defaultLocation.value

  map = L.map(mapContainer.value, {
    center: [loc.latitude, loc.longitude],
    zoom: 15,
    zoomControl: true,
    attributionControl: false,
  })

  // Add OpenStreetMap tiles
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
  }).addTo(map)

  // Add draggable marker
  marker = L.marker([loc.latitude, loc.longitude], {
    draggable: true,
  }).addTo(map)

  // Update location when marker is dragged
  marker.on('dragend', async () => {
    if (marker) {
      const pos = marker.getLatLng()
      isLoading.value = true
      try {
        const address = await reverseGeocode(pos.lat, pos.lng)
        selectedLocation.value = {
          latitude: pos.lat,
          longitude: pos.lng,
          address,
        }
      } finally {
        isLoading.value = false
      }
    }
  })

  // Update location when map is clicked
  map.on('click', async (e: L.LeafletMouseEvent) => {
    if (marker) {
      marker.setLatLng(e.latlng)
      isLoading.value = true
      try {
        const address = await reverseGeocode(e.latlng.lat, e.latlng.lng)
        selectedLocation.value = {
          latitude: e.latlng.lat,
          longitude: e.latlng.lng,
          address,
        }
      } finally {
        isLoading.value = false
      }
    }
  })

  // Invalidate size after map is shown (fixes rendering issues in modals)
  setTimeout(() => {
    map?.invalidateSize()
  }, 100)
}

function destroyMap() {
  if (map) {
    map.remove()
    map = null
    marker = null
  }
}

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

onUnmounted(() => {
  destroyMap()
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

    // Get address from coordinates (reverse geocoding)
    const address = await reverseGeocode(latitude, longitude)

    selectedLocation.value = {
      latitude,
      longitude,
      address,
    }

    // Center map on new location
    if (map && marker) {
      map.setView([latitude, longitude], 15)
      marker.setLatLng([latitude, longitude])
    }
  } catch (error: unknown) {
    const geoError = error as GeolocationPositionError
    if (geoError.code === 1) {
      ElMessage.warning('Location access was denied')
    } else if (geoError.code === 2) {
      ElMessage.warning('Location unavailable')
    } else if (geoError.code === 3) {
      ElMessage.warning('Location request timed out')
    } else {
      ElMessage.error('Failed to get current location')
    }
  } finally {
    isLoading.value = false
  }
}

// Reverse geocode coordinates to address using OpenStreetMap Nominatim
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

// Search for location by name using OpenStreetMap Nominatim
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
      const result = {
        latitude: parseFloat(data[0].lat),
        longitude: parseFloat(data[0].lon),
        address: data[0].display_name,
      }
      selectedLocation.value = result

      // Center map on search result
      if (map && marker) {
        map.setView([result.latitude, result.longitude], 15)
        marker.setLatLng([result.latitude, result.longitude])
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

  // Center map on selected location
  if (map && marker) {
    map.setView([location.latitude, location.longitude], 15)
    marker.setLatLng([location.latitude, location.longitude])
  }
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

        <!-- Interactive Leaflet Map -->
        <div ref="mapContainer" class="map-container">
          <div v-if="!map" class="map-loading">
            <el-icon :size="32" class="loading-icon"><LocationIcon /></el-icon>
            <span>Loading map...</span>
          </div>
        </div>

        <div class="map-hint">
          Click on the map or drag the marker to select a location
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
  width: 520px;
  max-height: 85vh;
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
  height: 280px;
  margin: 0 20px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
}

.loading-icon {
  color: var(--el-color-primary);
}

.map-hint {
  padding: 8px 20px 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.selected-info {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 20px;
  background: var(--el-fill-color-lighter);
  margin: 12px 20px 0;
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
  padding: 12px 20px 0;
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

/* Leaflet specific styles */
:deep(.leaflet-container) {
  height: 100%;
  width: 100%;
  border-radius: 8px;
  font-family: inherit;
}

:deep(.leaflet-control-zoom) {
  border: none !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15) !important;
}

:deep(.leaflet-control-zoom a) {
  border-radius: 4px !important;
  border: none !important;
}

:deep(.leaflet-control-zoom a:first-child) {
  border-radius: 4px 4px 0 0 !important;
}

:deep(.leaflet-control-zoom a:last-child) {
  border-radius: 0 0 4px 4px !important;
}
</style>
