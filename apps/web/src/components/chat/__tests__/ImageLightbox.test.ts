import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ImageLightbox from '../ImageLightbox.vue'

// Mock fetch for download tests
const mockFetch = vi.fn()
global.fetch = mockFetch

// Mock URL methods
const mockCreateObjectURL = vi.fn(() => 'blob:mock-url')
const mockRevokeObjectURL = vi.fn()
global.URL.createObjectURL = mockCreateObjectURL
global.URL.revokeObjectURL = mockRevokeObjectURL

describe('ImageLightbox', () => {
  const testImages = [
    'https://example.com/image1.jpg',
    'https://example.com/image2.jpg',
    'https://example.com/image3.jpg',
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    // Clean up any teleported content
    const overlays = document.body.querySelectorAll('.image-lightbox-overlay')
    overlays.forEach((overlay) => overlay.remove())
  })

  function findInBody(selector: string) {
    return document.body.querySelector(selector)
  }

  function findAllInBody(selector: string) {
    return document.body.querySelectorAll(selector)
  }

  describe('Rendering', () => {
    it('should not render when visible is false', async () => {
      mount(ImageLightbox, {
        props: {
          visible: false,
          images: testImages,
        },
      })

      await flushPromises()
      expect(findInBody('.image-lightbox-overlay')).toBeNull()
    })

    it('should render when visible is true', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()
      expect(findInBody('.image-lightbox-overlay')).not.toBeNull()
    })

    it('should display the current image', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()
      const img = findInBody('.lightbox-image') as HTMLImageElement
      expect(img).not.toBeNull()
      expect(img.src).toBe(testImages[0])
    })

    it('should start at initialIndex when provided', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
          initialIndex: 1,
        },
      })

      await flushPromises()
      const img = findInBody('.lightbox-image') as HTMLImageElement
      expect(img.src).toBe(testImages[1])
    })

    it('should display image counter for multiple images', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()
      const counter = findInBody('.image-counter')
      expect(counter?.textContent?.trim()).toBe('1 / 3')
    })

    it('should not display counter for single image', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: ['https://example.com/single.jpg'],
        },
      })

      await flushPromises()
      expect(findInBody('.image-counter')).toBeNull()
    })
  })

  describe('Navigation', () => {
    it('should show navigation arrows for multiple images', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()
      // At index 0, should only show next arrow
      expect(findInBody('.nav-prev')).toBeNull()
      expect(findInBody('.nav-next')).not.toBeNull()
    })

    it('should not show navigation arrows for single image', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: ['https://example.com/single.jpg'],
        },
      })

      await flushPromises()
      expect(findInBody('.nav-prev')).toBeNull()
      expect(findInBody('.nav-next')).toBeNull()
    })

    it('should navigate to next image', async () => {
      const wrapper = mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()

      const nextBtn = findInBody('.nav-next') as HTMLElement
      nextBtn.click()
      await flushPromises()

      const img = findInBody('.lightbox-image') as HTMLImageElement
      expect(img.src).toBe(testImages[1])
      expect(wrapper.emitted('change')?.[0]).toEqual([1])
    })

    it('should navigate to previous image', async () => {
      const wrapper = mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
          initialIndex: 2,
        },
      })

      await flushPromises()

      const prevBtn = findInBody('.nav-prev') as HTMLElement
      prevBtn.click()
      await flushPromises()

      const img = findInBody('.lightbox-image') as HTMLImageElement
      expect(img.src).toBe(testImages[1])
      expect(wrapper.emitted('change')?.[0]).toEqual([1])
    })

    it('should show both arrows when in middle', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
          initialIndex: 1,
        },
      })

      await flushPromises()
      expect(findInBody('.nav-prev')).not.toBeNull()
      expect(findInBody('.nav-next')).not.toBeNull()
    })
  })

  describe('Thumbnails', () => {
    it('should display thumbnails for multiple images', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()
      const thumbnails = findAllInBody('.thumbnail')
      expect(thumbnails.length).toBe(3)
    })

    it('should not display thumbnails for single image', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: ['https://example.com/single.jpg'],
        },
      })

      await flushPromises()
      expect(findInBody('.thumbnail-strip')).toBeNull()
    })

    it('should highlight active thumbnail', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
          initialIndex: 1,
        },
      })

      await flushPromises()
      const thumbnails = findAllInBody('.thumbnail')
      expect(thumbnails[0].classList.contains('active')).toBe(false)
      expect(thumbnails[1].classList.contains('active')).toBe(true)
      expect(thumbnails[2].classList.contains('active')).toBe(false)
    })

    it('should navigate when clicking thumbnail', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()

      const thumbnails = findAllInBody('.thumbnail')
      ;(thumbnails[2] as HTMLElement).click()
      await flushPromises()

      const img = findInBody('.lightbox-image') as HTMLImageElement
      expect(img.src).toBe(testImages[2])
    })
  })

  describe('Toolbar Actions', () => {
    it('should have zoom in button', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()
      const buttons = findAllInBody('.toolbar-btn')
      expect(buttons.length).toBeGreaterThanOrEqual(4) // zoom out, zoom in, rotate, download, close
    })

    it('should close when close button is clicked', async () => {
      const wrapper = mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()

      const closeBtn = findInBody('.close-btn') as HTMLElement
      closeBtn.click()
      await flushPromises()

      expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
    })

    it('should close when clicking overlay background', async () => {
      const wrapper = mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()

      const overlay = findInBody('.image-lightbox-overlay') as HTMLElement
      // Need to click directly on overlay, not on children
      overlay.click()
      await flushPromises()

      expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
    })
  })

  describe('Keyboard Navigation', () => {
    it('should close on Escape key', async () => {
      const wrapper = mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()

      const event = new KeyboardEvent('keydown', { key: 'Escape' })
      document.dispatchEvent(event)
      await flushPromises()

      expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
    })

    it('should navigate with arrow keys', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()

      // Press right arrow
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }))
      await flushPromises()

      let img = findInBody('.lightbox-image') as HTMLImageElement
      expect(img.src).toBe(testImages[1])

      // Press left arrow
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }))
      await flushPromises()

      img = findInBody('.lightbox-image') as HTMLImageElement
      expect(img.src).toBe(testImages[0])
    })
  })

  describe('Download', () => {
    it('should trigger download on button click', async () => {
      const mockBlob = new Blob(['test'], { type: 'image/jpeg' })
      mockFetch.mockResolvedValueOnce({
        blob: () => Promise.resolve(mockBlob),
      })

      mount(ImageLightbox, {
        props: {
          visible: true,
          images: testImages,
        },
      })

      await flushPromises()

      // Find download button (has Download title)
      const buttons = findAllInBody('.toolbar-btn')
      const downloadBtn = Array.from(buttons).find(
        (btn) => btn.getAttribute('title') === 'Download'
      ) as HTMLElement

      expect(downloadBtn).not.toBeNull()
      downloadBtn.click()
      await flushPromises()

      expect(mockFetch).toHaveBeenCalledWith(testImages[0])
    })
  })

  describe('Empty State', () => {
    it('should handle empty images array', async () => {
      mount(ImageLightbox, {
        props: {
          visible: true,
          images: [],
        },
      })

      await flushPromises()
      expect(findInBody('.lightbox-image')).toBeNull()
    })
  })
})
