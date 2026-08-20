import { defineStore } from 'pinia'
import { ref } from 'vue'
import { doCheckin, getTodayStatus, listCheckinDates } from '../api/checkin'

export const useCheckinStore = defineStore('checkin', () => {
  const todayChecked = ref(false)
  const todayFocusMinutes = ref(0)
  const calendarDates = ref<string[]>([])

  // 容错加载：异常保持默认状态，主页不崩
  async function loadToday() {
    try {
      const status = await getTodayStatus()
      todayChecked.value = status.checked
      todayFocusMinutes.value = status.focusTotalMinutes
    } catch (e) {
      console.warn('加载今日打卡状态失败', e)
    }
  }

  async function loadCalendar(days = 30) {
    try {
      calendarDates.value = await listCheckinDates(days)
    } catch (e) {
      console.warn('加载日历失败', e)
      calendarDates.value = []
    }
  }

  async function doCheckinAction() {
    const checkin = await doCheckin()
    todayChecked.value = true
    return checkin
  }

  // 由 SessionService 落库后回写更新（避免再拉一次）
  function setTodayFromSession(focusMinutes: number) {
    todayFocusMinutes.value = focusMinutes
    todayChecked.value = true
    const today = new Date().toISOString().slice(0, 10)
    if (!calendarDates.value.includes(today)) {
      calendarDates.value.push(today)
    }
  }

  return {
    todayChecked,
    todayFocusMinutes,
    calendarDates,
    loadToday,
    loadCalendar,
    doCheckinAction,
    setTodayFromSession,
  }
})
