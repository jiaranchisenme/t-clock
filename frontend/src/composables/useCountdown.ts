import { ref, onUnmounted } from 'vue'
import { useTimerStore } from '../stores/timer'

/**
 * 倒计时核心逻辑：基于 endTime - Date.now() 反推剩余秒数。
 * 不使用每秒 -- 是为了避免后台节流导致漂移；
 * 组件卸载或暂停时清理 interval，避免叠加泄漏。
 */
export function useCountdown() {
  const timerStore = useTimerStore()
  const endTime = ref<number>(0)
  let intervalId: number | null = null

  function tick() {
    if (endTime.value === 0) return
    const remain = Math.max(0, Math.floor((endTime.value - Date.now()) / 1000))
    timerStore.remainingSeconds = remain
    if (remain <= 0) {
      stop()
      // 阶段切换：WORK -> BREAK -> 终态由 View 监听处理
      timerStore.phase = timerStore.phase === 'WORK' ? 'BREAK' : 'IDLE'
      if (timerStore.phase === 'IDLE') timerStore.isRunning = false
    }
  }

  function start() {
    const total =
      (timerStore.phase === 'BREAK' ? timerStore.breakDuration : timerStore.workDuration) * 60
    endTime.value = Date.now() + total * 1000
    timerStore.isRunning = true
    stop()
    intervalId = window.setInterval(tick, 250) // 250ms 刷新，平衡流畅度与性能
  }

  function stop() {
    if (intervalId !== null) {
      clearInterval(intervalId)
      intervalId = null
    }
  }

  onUnmounted(() => stop())

  return { start, stop }
}
