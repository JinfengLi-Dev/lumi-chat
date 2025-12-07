import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import VideoMessage from '../VideoMessage.vue'
import type { Message } from '@/types'

// Disable teleport for testing
config.global.stubs = {
  teleport: true,
}

// Helper to create mock message
function createMockMessage(overrides: Partial<Message> = {}): Message {
  return {
    id: 1,
    msgId: 'video-1',
    conversationId: 1,
    senderId: 1,
    msgType: 'video',
    content: 'https://example.com/video.mp4',
    serverCreatedAt: new Date().toISOString(),
    metadata: {
      duration: 120, // 2 minutes
      thumbnailUrl: 'https://example.com/thumbnail.jpg',
    },
    ...overrides,
  }
}

describe('VideoMessage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock requestFullscreen and exitFullscreen
    Element.prototype.requestFullscreen = vi.fn().mockResolvedValue(undefined)
    document.exitFullscreen = vi.fn().mockResolvedValue(undefined)
  })

  afterEach(() => {
    // Cleanup
  })

  describe('Rendering', () => {
    it('should render video message component', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      expect(wrapper.find('.video-message').exists()).toBe(true)

      wrapper.unmount()
    })

    it('should show thumbnail when not started', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      expect(wrapper.find('.thumbnail-overlay').exists()).toBe(true)
      expect(wrapper.find('.thumbnail').exists()).toBe(true)

      wrapper.unmount()
    })

    it('should show video when no thumbnail', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({
            metadata: { duration: 120 }, // No thumbnailUrl
          }),
        },
      })

      // Video should be visible
      expect(wrapper.find('.video-player').isVisible()).toBe(true)

      wrapper.unmount()
    })

    it('should show play overlay on thumbnail', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      expect(wrapper.find('.play-overlay').exists()).toBe(true)

      wrapper.unmount()
    })
  })

  describe('Controls', () => {
    it('should have download button', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ metadata: {} }), // No thumbnail
        },
      })

      expect(wrapper.find('.controls-overlay').exists()).toBe(true)

      wrapper.unmount()
    })

    it('should have fullscreen button', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ metadata: {} }),
        },
      })

      const controlButtons = wrapper.findAll('.control-button')
      expect(controlButtons.length).toBeGreaterThanOrEqual(2)

      wrapper.unmount()
    })

    it('should have progress bar', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ metadata: {} }),
        },
      })

      expect(wrapper.find('.progress-bar').exists()).toBe(true)

      wrapper.unmount()
    })

    it('should have time display', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ metadata: { duration: 65 } }),
        },
      })

      expect(wrapper.find('.time-display').exists()).toBe(true)

      wrapper.unmount()
    })
  })

  describe('Play/Pause', () => {
    it('should have center play button', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ metadata: {} }),
        },
      })

      expect(wrapper.find('.center-button').exists()).toBe(true)

      wrapper.unmount()
    })

    it('should toggle play state', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ metadata: {} }),
        },
      })

      type VM = { isPlaying: boolean; hasStarted: boolean }
      const vm = wrapper.vm as unknown as VM

      expect(vm.isPlaying).toBe(false)
      expect(vm.hasStarted).toBe(false)

      wrapper.unmount()
    })
  })

  describe('Progress', () => {
    it('should start with 0 progress', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      type VM = { progress: number }
      const vm = wrapper.vm as unknown as VM

      expect(vm.progress).toBe(0)

      wrapper.unmount()
    })
  })

  describe('Time Display', () => {
    it('should format time correctly', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      type VM = { formatTime: (seconds: number) => string }
      const vm = wrapper.vm as unknown as VM

      expect(vm.formatTime(0)).toBe('0:00')
      expect(vm.formatTime(5)).toBe('0:05')
      expect(vm.formatTime(65)).toBe('1:05')
      expect(vm.formatTime(3665)).toBe('61:05')

      wrapper.unmount()
    })
  })

  describe('Download', () => {
    it('should trigger download on click', async () => {
      const createElementSpy = vi.spyOn(document, 'createElement')
      const appendChildSpy = vi.spyOn(document.body, 'appendChild')
      const removeChildSpy = vi.spyOn(document.body, 'removeChild')

      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ msgId: 'test-video-id', metadata: {} }),
        },
      })

      const downloadButton = wrapper.findAll('.control-button')[0]
      await downloadButton.trigger('click')

      expect(createElementSpy).toHaveBeenCalledWith('a')
      expect(appendChildSpy).toHaveBeenCalled()
      expect(removeChildSpy).toHaveBeenCalled()

      createElementSpy.mockRestore()
      appendChildSpy.mockRestore()
      removeChildSpy.mockRestore()

      wrapper.unmount()
    })
  })

  describe('Fullscreen', () => {
    it('should toggle fullscreen on button click', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ metadata: {} }),
        },
      })

      const fullscreenButton = wrapper.findAll('.control-button')[1]
      await fullscreenButton.trigger('click')

      expect(Element.prototype.requestFullscreen).toHaveBeenCalled()

      wrapper.unmount()
    })
  })

  describe('State Reset', () => {
    it('should reset state when message changes', async () => {
      const message1 = createMockMessage({ id: 1 })
      const wrapper = mount(VideoMessage, {
        props: {
          message: message1,
        },
      })

      type VM = { currentTime: number; isPlaying: boolean; hasStarted: boolean }
      const vm = wrapper.vm as unknown as VM

      // Simulate some state
      vm.currentTime = 30
      vm.isPlaying = true
      vm.hasStarted = true

      // Change message
      await wrapper.setProps({
        message: createMockMessage({ id: 2 }),
      })

      expect(vm.currentTime).toBe(0)
      expect(vm.isPlaying).toBe(false)
      expect(vm.hasStarted).toBe(false)

      wrapper.unmount()
    })
  })

  describe('Controls Visibility', () => {
    it('should show controls on mouse enter', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ metadata: {} }),
        },
      })

      await wrapper.find('.video-message').trigger('mouseenter')

      type VM = { showControls: boolean }
      const vm = wrapper.vm as unknown as VM

      expect(vm.showControls).toBe(true)

      wrapper.unmount()
    })

    it('should hide controls on mouse leave when not playing', async () => {
      const wrapper = mount(VideoMessage, {
        props: {
          message: createMockMessage({ metadata: {} }),
        },
      })

      await wrapper.find('.video-message').trigger('mouseenter')
      await wrapper.find('.video-message').trigger('mouseleave')

      type VM = { showControls: boolean }
      const vm = wrapper.vm as unknown as VM

      expect(vm.showControls).toBe(false)

      wrapper.unmount()
    })
  })
})
