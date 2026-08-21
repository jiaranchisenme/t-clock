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

/** 需求 2：优先请求 /stats/week（近 7 天柱状图） */
export function getWeekStats() {
  return http.get<DayStat[], DayStat[]>('/stats/week')
}

/** 需求 2：优先请求 /stats/month（近 30 天柱状图） */
export function getMonthStats() {
  return http.get<DayStat[], DayStat[]>('/stats/month')
}
