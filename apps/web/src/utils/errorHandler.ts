import { AxiosError } from 'axios'
import type { ApiResponse } from '@/types'

/**
 * Type guard to check if an error is an AxiosError
 */
export function isAxiosError(error: unknown): error is AxiosError<ApiResponse<unknown>> {
  return error instanceof AxiosError
}

/**
 * Extract a user-friendly error message from an unknown error.
 * Handles AxiosError, Error, and unknown types.
 */
export function getErrorMessage(error: unknown): string {
  if (isAxiosError(error)) {
    // API error with response
    if (error.response?.data?.message) {
      return error.response.data.message
    }
    // Network error or no response
    if (error.message) {
      return error.message
    }
  }

  if (error instanceof Error) {
    return error.message
  }

  // Fallback for unknown error types
  return 'An unexpected error occurred'
}

/**
 * Log an error for debugging purposes.
 * In production, this could be sent to an error tracking service.
 */
export function logError(error: unknown, context?: string): void {
  const prefix = context ? `[${context}]` : ''
  console.error(prefix, 'Error:', error)
}

/**
 * Handle an API error - logs it and returns the user-friendly message.
 * Convenience function that combines logging and message extraction.
 */
export function handleApiError(error: unknown, context?: string): string {
  logError(error, context)
  return getErrorMessage(error)
}
