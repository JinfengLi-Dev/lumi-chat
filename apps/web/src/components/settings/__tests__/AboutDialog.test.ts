import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, config } from '@vue/test-utils'
import AboutDialog from '../AboutDialog.vue'

// Disable teleport for testing
config.global.stubs = {
  teleport: true,
}

// Define VM interface for type casting
interface AboutDialogVM {
  appName: string
  appVersion: string
  buildDate: string
  copyright: string
  close: () => void
}

describe('AboutDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Static Content', () => {
    it('should expose app name', () => {
      const wrapper = mount(AboutDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as AboutDialogVM
      expect(vm.appName).toBe('Lumi-Chat')
      wrapper.unmount()
    })

    it('should expose app version', () => {
      const wrapper = mount(AboutDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as AboutDialogVM
      expect(vm.appVersion).toBe('1.0.0')
      wrapper.unmount()
    })

    it('should expose build date', () => {
      const wrapper = mount(AboutDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as AboutDialogVM
      expect(vm.buildDate).toBeDefined()
      expect(vm.buildDate).toContain('2025')
      wrapper.unmount()
    })

    it('should expose copyright info', () => {
      const wrapper = mount(AboutDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as AboutDialogVM
      expect(vm.copyright).toContain('Lumi')
      wrapper.unmount()
    })
  })

  describe('Close Action', () => {
    it('should emit update:visible false when close is called', async () => {
      const wrapper = mount(AboutDialog, {
        props: {
          visible: true,
        },
      })

      const vm = wrapper.vm as unknown as AboutDialogVM
      vm.close()
      await flushPromises()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
      wrapper.unmount()
    })
  })

  describe('Props Handling', () => {
    it('should accept visible prop', () => {
      const wrapper = mount(AboutDialog, {
        props: {
          visible: true,
        },
      })

      expect(wrapper.props('visible')).toBe(true)
      wrapper.unmount()
    })

    it('should handle visible false', () => {
      const wrapper = mount(AboutDialog, {
        props: {
          visible: false,
        },
      })

      expect(wrapper.props('visible')).toBe(false)
      wrapper.unmount()
    })
  })
})
