import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DayStat, StatsBundle } from '../api/stats'

export const useStatsStore = defineStore('stats', () => {
  const daily7 = ref<DayStat[]>([])
  const daily30 = ref<DayStat[]>([])
  const weeklyTotal = ref(0)
  const monthlyTotal = ref(0)

  function setStats(payload: StatsBundle) {
    daily7.value = payload.daily7
    daily30.value = payload.daily30
    weeklyTotal.value = payload.weeklyTotal
    monthlyTotal.value = payload.monthlyTotal
  }

  return { daily7, daily30, weeklyTotal, monthlyTotal, setStats }
})
