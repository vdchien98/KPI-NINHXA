import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Parse datetime string từ backend
 * Backend trả về datetime với timezone Asia/Ho_Chi_Minh (+07:00)
 * Format: ISO 8601 với timezone (ví dụ: 2024-12-21T18:30:00+07:00)
 * 
 * @param dateString Datetime string từ backend (ISO 8601 với timezone)
 * @returns Date object được parse đúng timezone
 */
export function parseBackendDate(dateString: string | null | undefined): Date | null {
  if (!dateString) return null
  
  try {
    // Backend trả về ISO 8601 với timezone (ví dụ: 2024-12-21T18:30:00+07:00)
    // new Date() sẽ tự động parse đúng timezone
    return new Date(dateString)
  } catch (error) {
    console.error('Error parsing date:', dateString, error)
    return null
  }
}

/**
 * Format datetime để hiển thị theo format Việt Nam
 * @param dateString Datetime string từ backend
 * @param options Intl.DateTimeFormatOptions
 * @returns Formatted date string
 */
export function formatDate(
  dateString: string | null | undefined,
  options: Intl.DateTimeFormatOptions = {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }
): string {
  const date = parseBackendDate(dateString)
  if (!date) return ''
  
  return date.toLocaleString('vi-VN', {
    ...options,
    timeZone: 'Asia/Ho_Chi_Minh',
  })
}

/**
 * Format date only (không có giờ)
 */
export function formatDateOnly(dateString: string | null | undefined): string {
  return formatDate(dateString, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

/**
 * Format datetime với đầy đủ thông tin
 */
export function formatDateTime(dateString: string | null | undefined): string {
  return formatDate(dateString, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

/**
 * So sánh datetime với thời gian hiện tại
 * @param dateString Datetime string từ backend
 * @returns true nếu datetime đã qua (quá hạn)
 */
export function isPast(dateString: string | null | undefined): boolean {
  if (!dateString) return false
  const date = parseBackendDate(dateString)
  if (!date) return false
  return date < new Date()
}

/**
 * So sánh hai datetime
 * @param dateString1 Datetime string thứ nhất
 * @param dateString2 Datetime string thứ hai
 * @returns true nếu dateString1 < dateString2
 */
export function isBefore(dateString1: string | null | undefined, dateString2: string | null | undefined): boolean {
  if (!dateString1 || !dateString2) return false
  const date1 = parseBackendDate(dateString1)
  const date2 = parseBackendDate(dateString2)
  if (!date1 || !date2) return false
  return date1 < date2
}

/**
 * Convert datetime string từ backend sang format cho input datetime-local
 * Backend trả về: 2024-12-21T18:30:00+07:00
 * Input cần: 2024-12-21T18:30 (local time, không có timezone)
 * 
 * @param dateString Datetime string từ backend
 * @returns Format cho input datetime-local (YYYY-MM-DDTHH:mm)
 */
export function toDateTimeLocal(dateString: string | null | undefined): string {
  const date = parseBackendDate(dateString)
  if (!date) return ''
  
  // Lấy local time (đã được browser convert từ timezone +07:00)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

/**
 * Convert datetime-local input value sang ISO string để gửi lên backend
 * Input: 2024-12-21T18:30 (local time, user nhập theo giờ VN)
 * Backend cần: 2024-12-21T11:30:00Z (UTC, trừ 7 giờ)
 * 
 * @param dateTimeLocal Format từ input datetime-local (YYYY-MM-DDTHH:mm)
 * @returns ISO string với timezone (để backend parse và convert sang UTC)
 */
export function fromDateTimeLocal(dateTimeLocal: string): string {
  if (!dateTimeLocal) return ''
  
  // Parse như local time (giờ VN)
  const localDate = new Date(dateTimeLocal)
  
  // Convert sang UTC để gửi lên backend
  // Backend sẽ nhận UTC và lưu vào database
  return localDate.toISOString()
}

