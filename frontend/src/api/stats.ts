import http from './http'

// 单日统计
export interface DayStat {
  date: string // YYYY-MM-DD
  minutes: number
}

export interface StatsBundle {
  daily7: DayStat[]
  daily30: DayStat[]
  weeklyTotal: number
  monthlyTotal: number
}

export function getStats() {
  return http.get<StatsBundle, StatsBundle>('/stats')
}
