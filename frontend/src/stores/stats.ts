import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getStats, type StatsBundle, type DayStat } from '../api/stats'

export const useStatsStore = defineStore('stats', () => {
  const daily7 = ref<DayStat[]>([])
  const daily30 = ref<DayStat[]>([])
  const weeklyTotal = ref(0)
  const monthlyTotal = ref(0)

  // 容错加载：异常时全归零，主页不崩
  async function load() {
    try {
      const data: StatsBundle = await getStats()
      daily7.value = data.daily7 ?? []
      daily30.value = data.daily30 ?? []
      weeklyTotal.value = data.weeklyTotal ?? 0
      monthlyTotal.value = data.monthlyTotal ?? 0
    } catch (e) {
      console.warn('加载统计失败', e)
      daily7.value = []
      daily30.value = []
      weeklyTotal.value = 0
      monthlyTotal.value = 0
    }
  }

  function setStats(payload: StatsBundle) {
    daily7.value = payload.daily7
    daily30.value = payload.daily30
    weeklyTotal.value = payload.weeklyTotal
    monthlyTotal.value = payload.monthlyTotal
  }

  return { daily7, daily30, weeklyTotal, monthlyTotal, load, setStats }
})
