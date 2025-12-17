import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import LocationMessage from '../LocationMessage.vue'

// Mock window.open
const mockWindowOpen = vi.fn()
vi.stubGlobal('open', mockWindowOpen)

describe('LocationMessage', () => {
  const defaultProps = {
    latitude: 39.9042,
    longitude: 116.4074,
    address: 'Beijing, China',
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render location message', () => {
      const wrapper = mount(LocationMessage, {
        props: defaultProps,
      })

      expect(wrapper.find('.location-message').exists()).toBe(true)
    })

    it('should display address', () => {
      const wrapper = mount(LocationMessage, {
        props: defaultProps,
      })

      expect(wrapper.find('.location-address').text()).toBe('Beijing, China')
    })

    it('should display formatted coordinates', () => {
      const wrapper = mount(LocationMessage, {
        props: defaultProps,
      })

      const coords = wrapper.find('.location-coords').text()
      expect(coords).toContain('39.904200')
      expect(coords).toContain('116.407400')
    })

    it('should show "Unknown location" when no address', () => {
      const wrapper = mount(LocationMessage, {
        props: {
          latitude: 39.9042,
          longitude: 116.4074,
        },
      })

      expect(wrapper.find('.location-address').text()).toBe('Unknown location')
    })

    it('should display map preview image with OpenStreetMap tile', () => {
      const wrapper = mount(LocationMessage, {
        props: defaultProps,
      })

      const img = wrapper.find('.map-image')
      expect(img.exists()).toBe(true)
      // Uses OpenStreetMap tile service with zoom/x/y format
      expect(img.attributes('src')).toContain('tile.openstreetmap.org')
      expect(img.attributes('src')).toMatch(/\/\d+\/\d+\/\d+\.png$/)
    })

    it('should use custom mapPreviewUrl when provided', () => {
      const customUrl = 'https://example.com/custom-map.png'
      const wrapper = mount(LocationMessage, {
        props: {
          ...defaultProps,
          mapPreviewUrl: customUrl,
        },
      })

      const img = wrapper.find('.map-image')
      expect(img.attributes('src')).toBe(customUrl)
    })
  })

  describe('Interactions', () => {
    it('should emit click event on click', async () => {
      const wrapper = mount(LocationMessage, {
        props: defaultProps,
      })

      await wrapper.find('.location-message').trigger('click')
      expect(wrapper.emitted('click')).toBeTruthy()
    })

    it('should open maps in new window on click', async () => {
      const wrapper = mount(LocationMessage, {
        props: defaultProps,
      })

      await wrapper.find('.location-message').trigger('click')

      expect(mockWindowOpen).toHaveBeenCalledWith(
        expect.stringContaining('openstreetmap.org'),
        '_blank'
      )
      expect(mockWindowOpen).toHaveBeenCalledWith(
        expect.stringContaining('39.9042'),
        '_blank'
      )
      expect(mockWindowOpen).toHaveBeenCalledWith(
        expect.stringContaining('116.4074'),
        '_blank'
      )
    })
  })

  describe('Edge Cases', () => {
    it('should handle negative coordinates', () => {
      const wrapper = mount(LocationMessage, {
        props: {
          latitude: -33.8688,
          longitude: 151.2093,
          address: 'Sydney, Australia',
        },
      })

      const coords = wrapper.find('.location-coords').text()
      expect(coords).toContain('-33.868800')
      expect(coords).toContain('151.209300')
    })

    it('should handle zero coordinates', () => {
      const wrapper = mount(LocationMessage, {
        props: {
          latitude: 0,
          longitude: 0,
          address: 'Null Island',
        },
      })

      const coords = wrapper.find('.location-coords').text()
      expect(coords).toContain('0.000000')
    })

    it('should truncate long addresses', () => {
      const longAddress =
        'Very Long Address That Should Be Truncated Because It Is Too Long To Display In A Single Line'
      const wrapper = mount(LocationMessage, {
        props: {
          ...defaultProps,
          address: longAddress,
        },
      })

      const addressEl = wrapper.find('.location-address')
      expect(addressEl.attributes('title')).toBe(longAddress)
    })
  })

  describe('Styling', () => {
    it('should have location pin icon', () => {
      const wrapper = mount(LocationMessage, {
        props: defaultProps,
      })

      expect(wrapper.find('.location-pin').exists()).toBe(true)
    })

    it('should have clickable cursor style', () => {
      const wrapper = mount(LocationMessage, {
        props: defaultProps,
      })

      const locationMessage = wrapper.find('.location-message')
      expect(locationMessage.classes()).not.toContain('disabled')
    })
  })
})
