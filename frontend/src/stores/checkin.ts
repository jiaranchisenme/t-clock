import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCheckinStore = defineStore('checkin', () => {
  const todayChecked = ref(false)
  const todayFocusMinutes = ref(0)
  const calendarDates = ref<string[]>([])

  function setToday(payload: { checked: boolean; focusTotalMinutes: number }) {
    todayChecked.value = payload.checked
    todayFocusMinutes.value = payload.focusTotalMinutes
  }

  function setCalendarDates(dates: string[]) {
    calendarDates.value = dates
  }

  return { todayChecked, todayFocusMinutes, calendarDates, setToday, setCalendarDates }
})
