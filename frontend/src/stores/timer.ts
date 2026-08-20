import { defineStore } from 'pinia'
import { ref } from 'vue'

export type TimerPhase = 'WORK' | 'BREAK' | 'IDLE'

export const useTimerStore = defineStore('timer', () => {
  // 配置
  const workDuration = ref(25)
  const breakDuration = ref(5)

  // 运行态
  const phase = ref<TimerPhase>('IDLE')
  const isRunning = ref(false)
  const remainingSeconds = ref(25 * 60)
  const activeTaskId = ref<number | null>(null)

  // setInterval 句柄：卸载/暂停必清，避免叠加泄漏
  let intervalId: number | null = null

  function setConfig(work: number, brk: number) {
    workDuration.value = work
    breakDuration.value = brk
    if (phase.value === 'IDLE') {
      remainingSeconds.value = work * 60
    }
  }

  function resetTimer() {
    clearTimer()
    phase.value = 'IDLE'
    isRunning.value = false
    remainingSeconds.value = workDuration.value * 60
  }

  function clearTimer() {
    if (intervalId !== null) {
      clearInterval(intervalId)
      intervalId = null
    }
  }

  return {
    workDuration,
    breakDuration,
    phase,
    isRunning,
    remainingSeconds,
    activeTaskId,
    intervalId, // 暴露引用便于组件卸载清理
    setConfig,
    resetTimer,
    clearTimer,
  }
})
