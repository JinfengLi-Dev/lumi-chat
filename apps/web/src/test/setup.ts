import { vi } from 'vitest'
import { config } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'

// Mock Element Plus globally
config.global.plugins = [ElementPlus]

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

// Mock navigator.clipboard
Object.defineProperty(navigator, 'clipboard', {
  value: {
    writeText: vi.fn().mockResolvedValue(undefined),
    readText: vi.fn().mockResolvedValue(''),
  },
  writable: true,
})

// Mock localStorage - must be defined before beforeEach
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
}
Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
})

// Create a fresh Pinia instance for each test
beforeEach(() => {
  setActivePinia(createPinia())

  // Clear localStorage mock calls between tests (now defined)
  if (localStorageMock.getItem.mockClear) {
    localStorageMock.getItem.mockClear()
    localStorageMock.setItem.mockClear()
    localStorageMock.removeItem.mockClear()
    localStorageMock.clear.mockClear()
  }
})

// Clean up after each test
afterEach(() => {
  // Reset all mocks
  vi.clearAllMocks()
})

// Mock URL.createObjectURL
URL.createObjectURL = vi.fn(() => 'blob:mock-url')
URL.revokeObjectURL = vi.fn()

// Global console error suppression for expected errors in tests
const originalConsoleError = console.error
console.error = (...args: unknown[]) => {
  // Suppress Vue warnings during tests
  if (
    typeof args[0] === 'string' &&
    (args[0].includes('[Vue warn]') || args[0].includes('Invalid prop'))
  ) {
    return
  }
  originalConsoleError.apply(console, args)
}
