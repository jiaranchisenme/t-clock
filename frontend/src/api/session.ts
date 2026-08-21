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

// 需求 3：接口失败时「从本地 sessions 聚合」——先拉 sessions 列表再前端按日求和
export function listSessions(params?: { startDate?: string; endDate?: string }) {
  return http.get<Session[], Session[]>('/sessions', { params })
}
