import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import FileMessage from '../FileMessage.vue'
import { fileApi } from '@/api/file'
import type { Message } from '@/types'

// Mock the file API
vi.mock('@/api/file', () => ({
  fileApi: {
    openFile: vi.fn(),
    downloadFile: vi.fn(),
  },
}))

// Helper to create mock message
function createMockMessage(overrides: Partial<Message> = {}): Message {
  return {
    id: 1,
    msgId: 'msg-123',
    conversationId: 1,
    sender: {
      id: 1,
      uid: 'user123',
      email: 'test@example.com',
      nickname: 'Test User',
      gender: 'male',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    msgType: 'file',
    content: 'https://example.com/files/file-123/document.pdf',
    metadata: {
      fileName: 'document.pdf',
      fileSize: 1024 * 1024, // 1 MB
      fileId: 'file-123',
    },
    status: 'sent',
    serverCreatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  }
}

describe('FileMessage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render file message container', () => {
      const wrapper = mount(FileMessage, {
        props: { message: createMockMessage() },
      })

      expect(wrapper.find('.file-message').exists()).toBe(true)
    })

    it('should display file name', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'report.pdf', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-name').text()).toBe('report.pdf')
    })

    it('should display formatted file size in bytes', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.txt', fileSize: 500, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-size').text()).toBe('500 B')
    })

    it('should display formatted file size in KB', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.txt', fileSize: 2048, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-size').text()).toBe('2.0 KB')
    })

    it('should display formatted file size in MB', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.zip', fileSize: 5 * 1024 * 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-size').text()).toBe('5.0 MB')
    })

    it('should display formatted file size in GB', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.iso', fileSize: 2 * 1024 * 1024 * 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-size').text()).toBe('2.0 GB')
    })

    it('should display file type label based on extension', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'document.pdf', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-type-label').text()).toBe('PDF')
    })

    it('should display Word for .docx files', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'report.docx', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-type-label').text()).toBe('Word')
    })

    it('should display Excel for .xlsx files', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'data.xlsx', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-type-label').text()).toBe('Excel')
    })

    it('should display uppercase extension for unknown file types', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.xyz', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-type-label').text()).toBe('XYZ')
    })

    it('should display "File" for files without extension', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'README', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-type-label').text()).toBe('File')
    })

    it('should have file icon', () => {
      const wrapper = mount(FileMessage, {
        props: { message: createMockMessage() },
      })

      expect(wrapper.find('.file-icon').exists()).toBe(true)
    })
  })

  describe('Expiration Display', () => {
    it('should display expiration info when expiresAt is set', () => {
      const futureDate = new Date()
      futureDate.setDate(futureDate.getDate() + 3) // 3 days from now

      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: {
              fileName: 'file.pdf',
              fileSize: 1024,
              fileId: 'f1',
              expiresAt: futureDate.toISOString(),
            },
          }),
        },
      })

      expect(wrapper.find('.expires-in').exists()).toBe(true)
      expect(wrapper.find('.expires-in').text()).toContain('Expires')
    })

    it('should show "Expired" for expired files', () => {
      const pastDate = new Date()
      pastDate.setDate(pastDate.getDate() - 1) // Yesterday

      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: {
              fileName: 'file.pdf',
              fileSize: 1024,
              fileId: 'f1',
              expiresAt: pastDate.toISOString(),
            },
          }),
        },
      })

      expect(wrapper.find('.expires-in').text()).toBe('Expired')
      expect(wrapper.find('.expires-in.expired').exists()).toBe(true)
    })

    it('should not display expiration when expiresAt is not set', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.pdf', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.expires-in').exists()).toBe(false)
    })

    it('should show "today" for files expiring today', () => {
      const laterToday = new Date()
      laterToday.setHours(laterToday.getHours() + 2) // 2 hours from now

      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: {
              fileName: 'file.pdf',
              fileSize: 1024,
              fileId: 'f1',
              expiresAt: laterToday.toISOString(),
            },
          }),
        },
      })

      expect(wrapper.find('.expires-in').text()).toBe('Expires today')
    })
  })

  describe('Upload Status', () => {
    it('should show loading indicator when uploading', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({ status: 'sending' }),
        },
      })

      expect(wrapper.find('.uploading-indicator').exists()).toBe(true)
      expect(wrapper.find('.is-loading').exists()).toBe(true)
    })

    it('should not be clickable when uploading', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({ status: 'sending' }),
        },
      })

      expect(wrapper.find('.file-message.clickable').exists()).toBe(false)
    })

    it('should show retry button when upload failed', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({ status: 'failed' }),
        },
      })

      const retryButton = wrapper.find('.el-button--danger')
      expect(retryButton.exists()).toBe(true)
    })

    it('should emit retry event when retry button clicked', async () => {
      const message = createMockMessage({ status: 'failed' })
      const wrapper = mount(FileMessage, {
        props: { message },
      })

      const retryButton = wrapper.find('.el-button--danger')
      await retryButton.trigger('click')

      expect(wrapper.emitted('retry')).toBeTruthy()
      expect(wrapper.emitted('retry')![0]).toEqual([message])
    })
  })

  describe('File Actions', () => {
    it('should show open and download buttons when file is ready', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({ status: 'sent' }),
        },
      })

      const buttons = wrapper.findAll('.file-actions .el-button')
      expect(buttons.length).toBe(2) // Open and Download
    })

    it('should call fileApi.openFile when open button clicked', async () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.pdf', fileSize: 1024, fileId: 'test-file-id' },
          }),
        },
      })

      const openButton = wrapper.find('.el-button--primary')
      await openButton.trigger('click')

      expect(fileApi.openFile).toHaveBeenCalledWith('test-file-id')
    })

    it('should call fileApi.downloadFile when download button clicked', async () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'report.pdf', fileSize: 1024, fileId: 'test-file-id' },
          }),
        },
      })

      const buttons = wrapper.findAll('.file-actions .el-button')
      const downloadButton = buttons[1] // Second button is download
      await downloadButton.trigger('click')

      expect(fileApi.downloadFile).toHaveBeenCalledWith('test-file-id', 'report.pdf')
    })

    it('should open file when clicking on file message', async () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.pdf', fileSize: 1024, fileId: 'test-file-id' },
            status: 'sent',
          }),
        },
      })

      await wrapper.find('.file-message').trigger('click')

      expect(fileApi.openFile).toHaveBeenCalledWith('test-file-id')
    })

    it('should not open file when clicking on uploading message', async () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.pdf', fileSize: 1024, fileId: 'test-file-id' },
            status: 'sending',
          }),
        },
      })

      await wrapper.find('.file-message').trigger('click')

      expect(fileApi.openFile).not.toHaveBeenCalled()
    })

    it('should not open file when clicking on failed message', async () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'file.pdf', fileSize: 1024, fileId: 'test-file-id' },
            status: 'failed',
          }),
        },
      })

      await wrapper.find('.file-message').trigger('click')

      expect(fileApi.openFile).not.toHaveBeenCalled()
    })
  })

  describe('File ID Extraction', () => {
    it('should extract file ID from URL if not in metadata', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            content: 'https://example.com/files/extracted-id-123/document.pdf',
            metadata: { fileName: 'document.pdf', fileSize: 1024 },
          }),
        },
      })

      // The file should still be clickable
      expect(wrapper.find('.file-message.clickable').exists()).toBe(true)
    })

    it('should use metadata fileId if available', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            content: 'https://example.com/files/url-id/file.pdf',
            metadata: { fileName: 'file.pdf', fileSize: 1024, fileId: 'metadata-id' },
          }),
        },
      })

      wrapper.find('.file-message').trigger('click')

      expect(fileApi.openFile).toHaveBeenCalledWith('metadata-id')
    })
  })

  describe('File Icon Colors', () => {
    it('should have red color for PDF files', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'doc.pdf', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      const iconStyle = wrapper.find('.file-icon').attributes('style')
      expect(iconStyle).toContain('#E53935') // PDF red color
    })

    it('should have blue color for Word files', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'doc.docx', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      const iconStyle = wrapper.find('.file-icon').attributes('style')
      expect(iconStyle).toContain('#1565C0') // Word blue color
    })

    it('should have green color for Excel files', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'data.xlsx', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      const iconStyle = wrapper.find('.file-icon').attributes('style')
      expect(iconStyle).toContain('#2E7D32') // Excel green color
    })

    it('should have orange color for ZIP files', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'archive.zip', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      const iconStyle = wrapper.find('.file-icon').attributes('style')
      expect(iconStyle).toContain('#FFA000') // ZIP orange color
    })
  })

  describe('Edge Cases', () => {
    it('should handle missing metadata gracefully', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: undefined,
            content: 'https://example.com/files/f1/file.pdf',
          }),
        },
      })

      expect(wrapper.find('.file-name').text()).toBe('File')
      expect(wrapper.find('.file-size').text()).toBe('0 B')
    })

    it('should handle empty file name', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: '', fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-name').text()).toBe('File')
    })

    it('should handle zero file size', () => {
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: 'empty.txt', fileSize: 0, fileId: 'f1' },
          }),
        },
      })

      expect(wrapper.find('.file-size').text()).toBe('0 B')
    })

    it('should truncate long file names', () => {
      const longFileName = 'very_long_file_name_that_should_be_truncated.pdf'
      const wrapper = mount(FileMessage, {
        props: {
          message: createMockMessage({
            metadata: { fileName: longFileName, fileSize: 1024, fileId: 'f1' },
          }),
        },
      })

      const fileNameEl = wrapper.find('.file-name')
      expect(fileNameEl.attributes('title')).toBe(longFileName)
    })
  })
})
