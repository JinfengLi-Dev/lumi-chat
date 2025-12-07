import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import MessageStatus from '../MessageStatus.vue'

describe('MessageStatus', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render message status component', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sent',
        },
      })

      expect(wrapper.find('.message-status').exists()).toBe(true)
    })

    it('should show sending spinner', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sending',
        },
      })

      expect(wrapper.find('.status-icon.sending').exists()).toBe(true)
    })

    it('should show single check for sent', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sent',
        },
      })

      const icon = wrapper.find('.status-icon')
      expect(icon.exists()).toBe(true)
      // Single check SVG has specific path
      expect(wrapper.html()).toContain('svg')
    })

    it('should show double check for delivered', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'delivered',
        },
      })

      const icon = wrapper.find('.status-icon')
      expect(icon.exists()).toBe(true)
    })

    it('should show blue double check for read', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'read',
        },
      })

      const icon = wrapper.find('.status-icon')
      expect(icon.exists()).toBe(true)
      // Read status should have primary color
      expect(icon.attributes('style')).toContain('--el-color-primary')
    })

    it('should show error icon for failed', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'failed',
        },
      })

      expect(wrapper.find('.status-icon.failed').exists()).toBe(true)
    })
  })

  describe('Title/Tooltip', () => {
    it('should have Sending title for sending status', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sending',
        },
      })

      expect(wrapper.find('.message-status').attributes('title')).toBe('Sending...')
    })

    it('should have Sent title for sent status', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sent',
        },
      })

      expect(wrapper.find('.message-status').attributes('title')).toBe('Sent')
    })

    it('should have Delivered title for delivered status', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'delivered',
        },
      })

      expect(wrapper.find('.message-status').attributes('title')).toBe('Delivered')
    })

    it('should have Read title for read status', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'read',
        },
      })

      expect(wrapper.find('.message-status').attributes('title')).toBe('Read')
    })

    it('should have Failed title for failed status', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'failed',
        },
      })

      expect(wrapper.find('.message-status').attributes('title')).toBe('Failed to send')
    })
  })

  describe('Time Display', () => {
    it('should not show time by default', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sent',
        },
      })

      expect(wrapper.find('.status-time').exists()).toBe(false)
    })

    it('should show time when showTime is true', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sent',
          showTime: true,
          time: '2025-12-07T10:30:00Z',
        },
      })

      expect(wrapper.find('.status-time').exists()).toBe(true)
    })

    it('should format time correctly', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sent',
          showTime: true,
          time: '2025-12-07T10:30:00Z',
        },
      })

      // Time should be formatted as HH:MM
      const timeText = wrapper.find('.status-time').text()
      expect(timeText).toMatch(/\d{2}:\d{2}/)
    })

    it('should handle invalid time gracefully', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sent',
          showTime: true,
          time: 'invalid-date',
        },
      })

      // Should show Invalid Date when parsing fails
      expect(wrapper.find('.status-time').text()).toBe('Invalid Date')
    })

    it('should not show time element when time is not provided', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sent',
          showTime: true,
          // No time prop
        },
      })

      expect(wrapper.find('.status-time').exists()).toBe(false)
    })
  })

  describe('Status Colors', () => {
    it('should use secondary color for sent', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'sent',
        },
      })

      const icon = wrapper.find('.status-icon')
      expect(icon.attributes('style')).toContain('--el-text-color-secondary')
    })

    it('should use secondary color for delivered', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'delivered',
        },
      })

      const icon = wrapper.find('.status-icon')
      expect(icon.attributes('style')).toContain('--el-text-color-secondary')
    })

    it('should use primary color for read', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'read',
        },
      })

      const icon = wrapper.find('.status-icon')
      expect(icon.attributes('style')).toContain('--el-color-primary')
    })

    it('should use danger color for failed', () => {
      const wrapper = mount(MessageStatus, {
        props: {
          status: 'failed',
        },
      })

      const icon = wrapper.find('.status-icon')
      expect(icon.attributes('style')).toContain('--el-color-danger')
    })
  })
})
