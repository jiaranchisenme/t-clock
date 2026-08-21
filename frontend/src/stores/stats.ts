import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getWeekStats, getMonthStats, type DayStat, type StatsBundle, getStats } from '../api/stats'
import { listSessions, type Session } from '../api/session'

// ----- 日期工具（纯函数，需求 4/5/6）-----

function localISO(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 生成 [startISO, today] 区间的日期序列（YYYY-MM-DD），按从旧到新（需求 6）。 */
function dateRange(days: number): string[] {
  const list: string[] = []
  const today = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    list.push(localISO(d))
  }
  return list
}

/** 从 sessions 列表按日期求和专注时长（需求 4）。 */
function sumByDateFromSessions(sessions: readonly Session[], days: number): DayStat[] {
  const map = new Map<string, number>()
  for (const s of sessions) {
    if (s.sessionType !== 'WORK') continue
    if (!s.startTime) continue
    const iso = new Date(s.startTime)
    if (Number.isNaN(iso.getTime())) continue
    const key = localISO(iso)
    map.set(key, (map.get(key) ?? 0) + (s.durationMinutes ?? 0))
  }
  return dateRange(days).map((date) => ({ date, minutes: map.get(date) ?? 0 })) // 需求 5/6：补 0 + 升序
}

export const useStatsStore = defineStore('stats', () => {
  const daily7 = ref<DayStat[]>([])
  const daily30 = ref<DayStat[]>([])
  const weeklyTotal = ref(0)
  const monthlyTotal = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 派生汇总：min ≥ 0 并安全求和
  const computedWeeklyTotal = computed(() =>
    daily7.value.reduce((acc, d) => acc + Math.max(0, d.minutes), 0),
  )
  const computedMonthlyTotal = computed(() =>
    daily30.value.reduce((acc, d) => acc + Math.max(0, d.minutes), 0),
  )

  /**
   * 需求 2/3：优先远程接口；失败时 fallback 到本地 sessions 按日聚合。
   * - 正常：直接用远程结果（服务端已补 0/排序，仍保证格式）
   * 异常：拉 sessions，前端按日求和、补 0、升序
   */
  async function loadRange(days: 7 | 30): Promise<DayStat[]> {
    const remote = days === 7 ? getWeekStats : getMonthStats
    try {
      const data = await remote()
      return normalize(data, days)
    } catch (e) {
      console.warn(`/stats/${days === 7 ? 'week' : 'month'} 失败，本地 sessions 聚合`, e)
      try {
        // 仅当接口失败时才走 sessions 聚合（需求 3）
        const sessions = await listSessions()
        return sumByDateFromSessions(sessions ?? [], days)
      } catch (errSessions) {
        console.warn('sessions 列表也失败，返回全 0 序列', errSessions)
        return dateRange(days).map((date) => ({ date, minutes: 0 }))
      }
    }
  }

  /** 需求 5/6：保证结果覆盖完整日期区间、缺失补 0、升序 */
  function normalize(raw: DayStat[] | null | undefined, days: number): DayStat[] {
    const arr = Array.isArray(raw) ? raw : []
    const map = new Map<string, number>()
    for (const d of arr) {
      if (!d?.date) continue
      const m = Number.isFinite(d.minutes) ? d.minutes : 0
      map.set(d.date, (map.get(d.date) ?? 0) + Math.max(0, m))
    }
    return dateRange(days).map((date) => ({ date, minutes: map.get(date) ?? 0 }))
  }

  async function loadWeek() {
    daily7.value = await loadRange(7)
    weeklyTotal.value = computedWeeklyTotal.value
  }
  async function loadMonth() {
    daily30.value = await loadRange(30)
    monthlyTotal.value = computedMonthlyTotal.value
  }

  // 一次性加载周+月，并刷新 bundle 汇总（StatsView onMounted 调用）
  async function loadAll() {
    loading.value = true
    error.value = null
    try {
      // 周/月独立错误：任一侧失败不阻塞另一侧
      const [w, m] = await Promise.allSettled([loadWeek(), loadMonth()])
      if (w.status === 'rejected' && m.status === 'rejected') {
        error.value = '统计数据加载失败，请稍后重试'
      }
    } finally {
      loading.value = false
    }
  }

  // 保留老兼容接口（给其他模块复用）
  async function load() {
    try {
      const data: StatsBundle = await getStats()
      daily7.value = normalize(data.daily7, 7)
      daily30.value = normalize(data.daily30, 30)
      weeklyTotal.value = data.weeklyTotal ?? 0
      monthlyTotal.value = data.monthlyTotal ?? 0
    } catch (e) {
      console.warn('加载统计失败', e)
      // 老接口失败时 fallback 到新流程（按 range 聚合）
      await loadAll()
    }
  }

  function setStats(payload: StatsBundle) {
    daily7.value = normalize(payload.daily7, 7)
    daily30.value = normalize(payload.daily30, 30)
    weeklyTotal.value = payload.weeklyTotal ?? 0
    monthlyTotal.value = payload.monthlyTotal ?? 0
  }

  return {
    daily7,
    daily30,
    weeklyTotal,
    monthlyTotal,
    loading,
    error,
    loadWeek,
    loadMonth,
    loadAll,
    load,
    setStats,
  }
})
