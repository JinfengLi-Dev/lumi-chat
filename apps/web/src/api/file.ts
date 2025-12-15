import { apiClient } from './client'
import type { ApiResponse } from '@/types'

export type FileType = 'image' | 'file' | 'voice' | 'video' | 'avatar'

export interface FileInfo {
  fileId: string
  fileName: string
  fileSize: number
  bucket: string
  mimeType: string
  url: string
  thumbnailUrl?: string
  width?: number
  height?: number
  duration?: number
  createdAt: string
  expiresAt?: string
  fileType?: string
}

export interface UploadProgress {
  loaded: number
  total: number
  percent: number
}

export const fileApi = {
  /**
   * Upload a file
   * POST /files/upload
   */
  async uploadFile(
    file: File,
    type: FileType = 'file',
    onProgress?: (progress: UploadProgress) => void
  ): Promise<FileInfo> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)

    const response = await apiClient.post<ApiResponse<FileInfo>>('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          onProgress({
            loaded: progressEvent.loaded,
            total: progressEvent.total,
            percent: Math.round((progressEvent.loaded * 100) / progressEvent.total),
          })
        }
      },
    })
    return response.data.data
  },

  /**
   * Upload an avatar image
   * POST /files/avatar
   */
  async uploadAvatar(
    file: File,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<FileInfo> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post<ApiResponse<FileInfo>>('/files/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          onProgress({
            loaded: progressEvent.loaded,
            total: progressEvent.total,
            percent: Math.round((progressEvent.loaded * 100) / progressEvent.total),
          })
        }
      },
    })
    return response.data.data
  },

  /**
   * Get file URL (for direct access/display)
   * Returns the full URL to access the file
   */
  getFileUrl(fileId: string): string {
    return `${apiClient.defaults.baseURL}/files/${fileId}`
  },

  /**
   * Get download URL
   * Returns the URL for downloading the file as attachment
   */
  getDownloadUrl(fileId: string): string {
    return `${apiClient.defaults.baseURL}/files/${fileId}/download`
  },

  /**
   * Get file metadata
   * GET /files/{id}/info
   */
  async getFileInfo(fileId: string): Promise<FileInfo> {
    const response = await apiClient.get<ApiResponse<FileInfo>>(`/files/${fileId}/info`)
    return response.data.data
  },

  /**
   * Get user's files
   * GET /files
   */
  async getUserFiles(type?: FileType): Promise<FileInfo[]> {
    const response = await apiClient.get<ApiResponse<FileInfo[]>>('/files', {
      params: type ? { type } : undefined,
    })
    return response.data.data
  },

  /**
   * Delete file
   * DELETE /files/{id}
   */
  async deleteFile(fileId: string): Promise<void> {
    await apiClient.delete(`/files/${fileId}`)
  },

  /**
   * Download file programmatically
   * Triggers browser download with original filename
   */
  downloadFile(fileId: string, fileName?: string): void {
    const link = document.createElement('a')
    link.href = this.getDownloadUrl(fileId)
    link.download = fileName || ''
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  },

  /**
   * Save file as - allows user to choose destination
   * Uses the download attribute with the specified filename
   */
  saveFileAs(fileId: string, fileName: string): void {
    this.downloadFile(fileId, fileName)
  },

  /**
   * Open file in new tab/window
   * Browser will handle based on file type
   */
  openFile(fileId: string): void {
    window.open(this.getFileUrl(fileId), '_blank')
  },

  /**
   * Upload image for chat message
   * Convenience method that uploads as 'image' type
   */
  async uploadImage(
    file: File,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<FileInfo> {
    return this.uploadFile(file, 'image', onProgress)
  },

  /**
   * Upload voice message
   * Convenience method that uploads as 'voice' type
   */
  async uploadVoice(
    file: File,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<FileInfo> {
    return this.uploadFile(file, 'voice', onProgress)
  },

  /**
   * Upload video
   * Convenience method that uploads as 'video' type
   */
  async uploadVideo(
    file: File,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<FileInfo> {
    return this.uploadFile(file, 'video', onProgress)
  },
}
