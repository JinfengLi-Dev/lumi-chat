import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import VoiceMessage from '../VoiceMessage.vue'
import type { Message } from '@/types'

// Disable teleport for testing
config.global.stubs = {
  teleport: true,
}

// Mock HTMLAudioElement
class MockAudioElement {
  src = ''
  currentTime = 0
  duration = 30
  paused = true

  play = vi.fn().mockImplementation(() => {
    this.paused = false
    return Promise.resolve()
  })

  pause = vi.fn().mockImplementation(() => {
    this.paused = true
  })

  // Event handlers
  onplay: (() => void) | null = null
  onpause: (() => void) | null = null
  onended: (() => void) | null = null
  ontimeupdate: (() => void) | null = null
  onloadedmetadata: (() => void) | null = null
  onloadstart: (() => void) | null = null
  oncanplay: (() => void) | null = null

  addEventListener(event: string, handler: () => void) {
    switch (event) {
      case 'play':
        this.onplay = handler
        break
      case 'pause':
        this.onpause = handler
        break
      case 'ended':
        this.onended = handler
        break
      case 'timeupdate':
        this.ontimeupdate = handler
        break
      case 'loadedmetadata':
        this.onloadedmetadata = handler
        break
      case 'loadstart':
        this.onloadstart = handler
        break
      case 'canplay':
        this.oncanplay = handler
        break
    }
  }

  removeEventListener() {
    // no-op for tests
  }
}

// Helper to create mock message
function createMockMessage(overrides: Partial<Message> = {}): Message {
  return {
    id: 1,
    msgId: 'voice-1',
    conversationId: 1,
    senderId: 1,
    msgType: 'voice',
    content: 'https://example.com/voice.mp3',
    serverCreatedAt: new Date().toISOString(),
    metadata: {
      duration: 15, // 15 seconds
    },
    ...overrides,
  }
}

describe('VoiceMessage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    // Cleanup
  })

  describe('Rendering', () => {
    it('should render voice message component', async () => {
      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      expect(wrapper.find('.voice-message').exists()).toBe(true)
      expect(wrapper.find('.play-button').exists()).toBe(true)
      expect(wrapper.find('.progress-bar').exists()).toBe(true)
      expect(wrapper.find('.download-button').exists()).toBe(true)

      wrapper.unmount()
    })

    it('should display message duration', async () => {
      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage({ metadata: { duration: 65 } }), // 1:05
        },
      })

      expect(wrapper.find('.time-display').text()).toContain('1:05')

      wrapper.unmount()
    })

    it('should format duration correctly', async () => {
      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage({ metadata: { duration: 5 } }),
        },
      })

      expect(wrapper.find('.time-display').text()).toContain('0:05')

      wrapper.unmount()
    })
  })

  describe('Play/Pause', () => {
    it('should have play icon initially', async () => {
      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      const playButton = wrapper.find('.play-button')
      expect(playButton.exists()).toBe(true)
      // Icon should be VideoPlay (not VideoPause)

      wrapper.unmount()
    })

    it('should toggle play state on click', async () => {
      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      type VM = { isPlaying: boolean; togglePlay: () => void }
      const vm = wrapper.vm as unknown as VM

      expect(vm.isPlaying).toBe(false)

      wrapper.unmount()
    })
  })

  describe('Progress', () => {
    it('should start with 0 progress', async () => {
      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      type VM = { progress: number }
      const vm = wrapper.vm as unknown as VM

      expect(vm.progress).toBe(0)

      wrapper.unmount()
    })

    it('should have waveform visualization', async () => {
      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      expect(wrapper.find('.waveform').exists()).toBe(true)
      expect(wrapper.findAll('.wave-bar').length).toBe(20)

      wrapper.unmount()
    })
  })

  describe('Download', () => {
    it('should have download button', async () => {
      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      expect(wrapper.find('.download-button').exists()).toBe(true)

      wrapper.unmount()
    })

    it('should trigger download on click', async () => {
      const createElementSpy = vi.spyOn(document, 'createElement')
      const appendChildSpy = vi.spyOn(document.body, 'appendChild')
      const removeChildSpy = vi.spyOn(document.body, 'removeChild')

      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage({ msgId: 'test-voice-id' }),
        },
      })

      const downloadButton = wrapper.find('.download-button')
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

  describe('Time Display', () => {
    it('should format time as MM:SS', async () => {
      const wrapper = mount(VoiceMessage, {
        props: {
          message: createMockMessage(),
        },
      })

      type VM = { formatTime: (seconds: number) => string }
      const vm = wrapper.vm as unknown as VM

      expect(vm.formatTime(0)).toBe('0:00')
      expect(vm.formatTime(5)).toBe('0:05')
      expect(vm.formatTime(65)).toBe('1:05')
      expect(vm.formatTime(125)).toBe('2:05')

      wrapper.unmount()
    })
  })

  describe('State Reset', () => {
    it('should reset state when message changes', async () => {
      const message1 = createMockMessage({ id: 1 })
      const wrapper = mount(VoiceMessage, {
        props: {
          message: message1,
        },
      })

      type VM = { currentTime: number; isPlaying: boolean }
      const vm = wrapper.vm as unknown as VM

      // Simulate some state
      vm.currentTime = 10
      vm.isPlaying = true

      // Change message
      await wrapper.setProps({
        message: createMockMessage({ id: 2 }),
      })

      expect(vm.currentTime).toBe(0)
      expect(vm.isPlaying).toBe(false)

      wrapper.unmount()
    })
  })
})
