import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useUserStore } from '@/stores/user'

describe('Test Setup Verification', () => {
  it('should run a basic test', () => {
    expect(1 + 1).toBe(2)
  })

  it('should mount a Vue component', () => {
    const TestComponent = defineComponent({
      render() {
        return h('div', { class: 'test' }, 'Hello Test')
      },
    })

    const wrapper = mount(TestComponent)
    expect(wrapper.text()).toBe('Hello Test')
    expect(wrapper.classes()).toContain('test')
  })

  it('should have Pinia store available', () => {
    const userStore = useUserStore()
    expect(userStore).toBeDefined()
    expect(userStore.isLoggedIn).toBe(false)
  })

  it('should have mocked localStorage', () => {
    localStorage.setItem('test', 'value')
    expect(localStorage.setItem).toHaveBeenCalledWith('test', 'value')
  })

  it('should have mocked clipboard', async () => {
    await navigator.clipboard.writeText('test')
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('test')
  })
})
