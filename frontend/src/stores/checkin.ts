import { defineStore } from 'pinia'
import { ref } from 'vue'
import { doCheckin, getTodayStatus, listCheckinDates, getStreak } from '../api/checkin'

export const useCheckinStore = defineStore('checkin', () => {
  const todayChecked = ref(false)
  const todayFocusMinutes = ref(0)
  const calendarDates = ref<string[]>([]) // 近 14 天已打卡日期集合（YYYY-MM-DD）
  const streak = ref(0)

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

  // 需求 7：展示最近 14 天日期，默认拉取 14 天
  async function loadCalendar(days = 14) {
    try {
      calendarDates.value = await listCheckinDates(days)
    } catch (e) {
      console.warn('加载日历失败', e)
      calendarDates.value = []
    }
  }

  async function loadStreak() {
    try {
      const r = await getStreak()
      streak.value = r?.streak ?? 0
    } catch (e) {
      console.warn('加载连续打卡天数失败', e)
    }
  }

  // 一次性刷新所有统计：页面挂载与「新增打卡后」复用（需求 11）
  async function refreshAll() {
    await Promise.all([loadToday(), loadCalendar(14), loadStreak()])
  }

  // 需求 4/6：同一日期只能打卡一次；重复提交由后端转 409，调用方捕获后友好提示
  // 需求 11：新增打卡后自动刷新统计数据
  async function doCheckinAction() {
    const checkin = await doCheckin()
    await refreshAll()
    return checkin
  }

  // SessionService 落库后回写「今日累计专注分钟数」。
  // 注意：仅更新分钟数，不置 todayChecked——打卡是用户显式点击「打卡」按钮的动作，
  // 会话累计专注 ≠ 已打卡，避免「学过即已打卡」的语义混淆（满足需求 4/5）。
  function setTodayFromSession(focusMinutes: number) {
    todayFocusMinutes.value = focusMinutes
  }

  return {
    todayChecked,
    todayFocusMinutes,
    calendarDates,
    streak,
    loadToday,
    loadCalendar,
    loadStreak,
    refreshAll,
    doCheckinAction,
    setTodayFromSession,
  }
})
