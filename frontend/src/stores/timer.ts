import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTimerConfig, updateTimerConfig, type TimerConfigDTO } from '../api/timer'

export type TimerPhase = 'WORK' | 'BREAK' | 'IDLE'

export const useTimerStore = defineStore('timer', () => {
  // 默认值：与服务端 schema 一致（25/5）
  const workDuration = ref(25)
  const breakDuration = ref(5)

  const phase = ref<TimerPhase>('IDLE')
  const isRunning = ref(false)
  const remainingSeconds = ref(25 * 60)
  const activeTaskId = ref<number | null>(null)

  let intervalId: number | null = null

  // 初始化：从后端拉取配置；失败保留默认值，主页不崩
  async function loadConfig() {
    try {
      const cfg = await getTimerConfig()
      if (cfg) {
        workDuration.value = cfg.workDuration
        breakDuration.value = cfg.breakDuration
        if (phase.value === 'IDLE') {
          remainingSeconds.value = cfg.workDuration * 60
        }
      }
    } catch (e) {
      // 网络异常时静默回退默认值，主页面仍可正常展示
      console.warn('加载计时器配置失败，使用默认值', e)
    }
  }

  async function saveConfig(work: number, brk: number) {
    const payload: TimerConfigDTO = { workDuration: work, breakDuration: brk }
    return updateTimerConfig(payload)
  }

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
    intervalId,
    loadConfig,
    saveConfig,
    setConfig,
    resetTimer,
    clearTimer,
  }
})
