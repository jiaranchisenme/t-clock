import http from './http'

// 番茄会话实体
export interface Session {
  id?: number
  sessionType: 'WORK' | 'BREAK'
  startTime: string
  endTime: string
  durationMinutes: number
  taskId?: number | null
}

// 后端在落库后回写的当日累计
export interface SessionSaveResult {
  sessionId: number
  todayFocusMinutes: number
}

export function saveSession(payload: Session) {
  return http.post<SessionSaveResult, SessionSaveResult>('/sessions', payload)
}
