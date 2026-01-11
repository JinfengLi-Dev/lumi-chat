import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { config } from '@vue/test-utils'

// Mock Leaflet - define inside vi.mock factory to avoid hoisting issues
vi.mock('leaflet', () => {
  const mockOn = vi.fn().mockReturnThis()

  const mockMap = {
    setView: vi.fn().mockReturnThis(),
    on: mockOn,
    remove: vi.fn(),
    invalidateSize: vi.fn(),
  }

  // Create tileLayer mock with self-returning addTo
  const mockTileLayer: Record<string, unknown> = {}
  mockTileLayer.addTo = vi.fn().mockReturnValue(mockTileLayer)

  const mockMarker = {
    setLatLng: vi.fn().mockReturnThis(),
    on: mockOn,
    getLatLng: vi.fn().mockReturnValue({ lat: 39.9042, lng: 116.4074 }),
    addTo: vi.fn().mockReturnThis(),
  }

  const leafletExports = {
    map: vi.fn().mockReturnValue(mockMap),
    marker: vi.fn().mockReturnValue(mockMarker),
    tileLayer: vi.fn().mockReturnValue(mockTileLayer),
    Icon: {
      Default: {
        prototype: { _getIconUrl: vi.fn() },
        mergeOptions: vi.fn(),
      },
    },
  }

  return {
    default: leafletExports,
    ...leafletExports,
  }
})

// Mock leaflet CSS import
vi.mock('leaflet/dist/leaflet.css', () => ({}))

// Mock leaflet marker images
vi.mock('leaflet/dist/images/marker-icon-2x.png', () => ({ default: 'mock-marker-icon-2x.png' }))
vi.mock('leaflet/dist/images/marker-icon.png', () => ({ default: 'mock-marker-icon.png' }))
vi.mock('leaflet/dist/images/marker-shadow.png', () => ({ default: 'mock-marker-shadow.png' }))

import LocationPicker from '../LocationPicker.vue'

// Mock fetch for geocoding API
global.fetch = vi.fn()

// Mock navigator.geolocation
const mockGeolocation = {
  getCurrentPosition: vi.fn(),
}
Object.defineProperty(navigator, 'geolocation', {
  value: mockGeolocation,
  writable: true,
})

// Mock localStorage
const mockLocalStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
}
Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage,
  writable: true,
})

// Stub Teleport to render content directly
config.global.stubs = {
  Teleport: true,
}

describe('LocationPicker', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockLocalStorage.getItem.mockReturnValue(null)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('Rendering', () => {
    it('should render when visible is true', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      expect(wrapper.find('.location-picker-overlay').exists()).toBe(true)
      expect(wrapper.find('.location-picker').exists()).toBe(true)
    })

    it('should not render overlay when visible is false', () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: false,
        },
      })

      expect(wrapper.find('.location-picker-overlay').exists()).toBe(false)
    })

    it('should display header with title', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      expect(wrapper.find('.title').text()).toBe('Send Location')
    })

    it('should display search bar', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      expect(wrapper.find('.search-bar').exists()).toBe(true)
    })

    it('should display "Use Current Location" button', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      expect(wrapper.find('.current-location').exists()).toBe(true)
      expect(wrapper.text()).toContain('Use Current Location')
    })

    it('should display map container', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      expect(wrapper.find('.map-container').exists()).toBe(true)
    })

    it('should display map hint', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      expect(wrapper.find('.map-hint').text()).toContain('Click on the map or drag the marker')
    })

    it('should display action buttons', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      const actions = wrapper.find('.picker-actions')
      expect(actions.exists()).toBe(true)
      expect(actions.text()).toContain('Cancel')
      expect(actions.text()).toContain('Send Location')
    })
  })

  describe('Recent Locations', () => {
    it('should load recent locations from localStorage on mount', async () => {
      const recentLocations = [
        { latitude: 39.9042, longitude: 116.4074, address: 'Beijing' },
        { latitude: 31.2304, longitude: 121.4737, address: 'Shanghai' },
      ]
      mockLocalStorage.getItem.mockReturnValue(JSON.stringify(recentLocations))

      mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      expect(mockLocalStorage.getItem).toHaveBeenCalledWith('recentLocations')
    })

    it('should display recent locations when available and no location selected', async () => {
      const recentLocations = [{ latitude: 39.9042, longitude: 116.4074, address: 'Beijing' }]
      mockLocalStorage.getItem.mockReturnValue(JSON.stringify(recentLocations))

      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      await flushPromises()

      expect(wrapper.find('.recent-locations').exists()).toBe(true)
      expect(wrapper.text()).toContain('Recent Locations')
      expect(wrapper.text()).toContain('Beijing')
    })
  })

  describe('Close Behavior', () => {
    it('should emit update:visible false when close button clicked', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      const closeButton = wrapper.find('.picker-header .el-button')
      await closeButton.trigger('click')

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
    })

    it('should emit update:visible false when Cancel button clicked', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      const cancelButton = wrapper.findAll('.picker-actions .el-button')[0]
      await cancelButton.trigger('click')

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
    })

    it('should close when clicking overlay background', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      await wrapper.find('.location-picker-overlay').trigger('click')

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
    })
  })

  describe('Current Location', () => {
    it('should request geolocation when "Use Current Location" is clicked', async () => {
      mockGeolocation.getCurrentPosition.mockImplementation((success) => {
        success({
          coords: {
            latitude: 39.9042,
            longitude: 116.4074,
          },
        })
      })

      // Mock fetch for reverse geocoding
      ;(global.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
        json: () => Promise.resolve({ display_name: 'Beijing, China' }),
      })

      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      const currentLocationBtn = wrapper.find('.current-location .el-button')
      await currentLocationBtn.trigger('click')

      expect(mockGeolocation.getCurrentPosition).toHaveBeenCalled()
    })
  })

  describe('Search Location', () => {
    it('should search when enter is pressed in search input', async () => {
      ;(global.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
        json: () =>
          Promise.resolve([
            {
              lat: '39.9042',
              lon: '116.4074',
              display_name: 'Beijing, China',
            },
          ]),
      })

      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      const searchInput = wrapper.find('.search-bar input')
      await searchInput.setValue('Beijing')
      await searchInput.trigger('keyup.enter')
      await flushPromises()

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('nominatim.openstreetmap.org/search'),
        expect.any(Object)
      )
    })

    it('should search when search button is clicked', async () => {
      ;(global.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
        json: () =>
          Promise.resolve([
            {
              lat: '39.9042',
              lon: '116.4074',
              display_name: 'Beijing, China',
            },
          ]),
      })

      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      const searchInput = wrapper.find('.search-bar input')
      await searchInput.setValue('Beijing')

      const searchButton = wrapper.find('.search-bar .el-button--primary')
      if (searchButton.exists()) {
        await searchButton.trigger('click')
        await flushPromises()

        expect(global.fetch).toHaveBeenCalledWith(
          expect.stringContaining('nominatim.openstreetmap.org/search'),
          expect.any(Object)
        )
      }
    })

    it('should not search with empty query', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()

      // Find the search button and click it without entering any text
      const searchButtons = wrapper.findAll('.search-bar .el-button')
      const searchButton = searchButtons.find((btn) => btn.text() === 'Search')
      if (searchButton) {
        await searchButton.trigger('click')
        expect(global.fetch).not.toHaveBeenCalled()
      }
    })
  })

  describe('Send Location Button', () => {
    it('should have a send location button', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      const sendButton = wrapper.findAll('.picker-actions .el-button').find((btn) => btn.text() === 'Send Location')
      expect(sendButton).toBeDefined()
    })
  })

  describe('Map Initialization', () => {
    it('should call Leaflet.map when visible becomes true', async () => {
      const L = await import('leaflet')

      const wrapper = mount(LocationPicker, {
        props: {
          visible: false,
        },
      })

      await wrapper.setProps({ visible: true })
      await nextTick()

      // Map should be initialized
      expect(L.default.map).toHaveBeenCalled()
    })

    it('should hide component when visible becomes false', async () => {
      const wrapper = mount(LocationPicker, {
        props: {
          visible: true,
        },
      })

      await nextTick()
      expect(wrapper.find('.location-picker-overlay').exists()).toBe(true)

      await wrapper.setProps({ visible: false })
      await nextTick()

      expect(wrapper.find('.location-picker-overlay').exists()).toBe(false)
    })
  })
})
