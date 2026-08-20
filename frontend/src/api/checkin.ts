import http from './http'

// 打卡实体
export interface Checkin {
  id?: number
  checkinDate: string // YYYY-MM-DD
  focusTotalMinutes: number
  createdAt?: string
}

export function doCheckin() {
  return http.post<Checkin, Checkin>('/checkins')
}

export function getTodayStatus() {
  return http.get<{ checked: boolean; focusTotalMinutes: number }>('/checkins/today')
}

export function listCheckinDates(days = 30) {
  return http.get<string[], string[]>('/checkins/dates', { params: { days } })
}
