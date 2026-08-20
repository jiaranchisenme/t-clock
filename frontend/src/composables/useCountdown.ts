import { ref, computed, onUnmounted, shallowRef } from 'vue'

export type CountdownMode = 'study' | 'rest'

export interface CountdownOptions {
  /** 学习时长（秒） */
  studySeconds: number
  /** 休息时长（秒） */
  restSeconds: number
  /**
   * 任一阶段完整结束时回调，参数为刚结束的模式。
   * "完整结束"指从 running=true 走到 remaining=0，不含中途重置/暂停/手动切换。
   */
  onComplete?: (finishedMode: CountdownMode) => void
}

/**
 * 无漂移倒计时核心：
 * 1) 剩余时间 = endTime - Date.now()，绝对时间戳驱动；
 *    后台标签页 setInterval 虽被节流到 ~1Hz，前台恢复后下一帧立即自愈。
 * 2) 单一 intervalId 引用，开始前先 clear，杜绝叠加。
 * 3) 组件卸载 onUnmounted 必清，避免泄漏。
 * 4) 阶段完整结束后进入"完成态"，不自动切模式 / 不自动开始，由调用方决定下一步。
 */
export function useCountdown(options: CountdownOptions) {
  const { studySeconds, restSeconds, onComplete } = options

  const mode = ref<CountdownMode>('study')
  const running = ref(false)
  // 是否处于"阶段完整结束、待用户确认"态
  const completed = ref(false)
  const remaining = ref(studySeconds)

  // endTime 用 shallowRef 持有原始 number，避免响应式包装开销
  const endTime = shallowRef<number>(0)
  // intervalId 不需要响应式，用普通 let
  let intervalId: ReturnType<typeof setInterval> | null = null

  // 进度 0~1，用于环形表盘
  const progress = computed(() => {
    const total = mode.value === 'study' ? studySeconds : restSeconds
    if (total <= 0) return 0
    return Math.max(0, Math.min(1, 1 - remaining.value / total))
  })

  // 显示文本 mm:ss
  const display = computed(() => {
    const s = Math.max(0, remaining.value)
    const m = Math.floor(s / 60)
    const sec = s % 60
    return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
  })

  /** 推算剩余秒数；时间到则收尾 */
  function tick() {
    if (endTime.value === 0) return
    const remain = Math.max(0, Math.round((endTime.value - Date.now()) / 1000))
    remaining.value = remain
    if (remain <= 0) {
      finishCurrentPhase()
    }
  }

  /**
   * 当前阶段完整结束：
   * - 停止计时
   * - 不自动切换 mode（留给 View 在弹窗确认后决定）
   * - 置 completed=true，进入"待确认"态
   * - 调用 onComplete(finishedMode)
   */
  function finishCurrentPhase() {
    stop()
    const finished = mode.value
    completed.value = true
    onComplete?.(finished)
  }

  /** 开始 / 继续计时（暂停后继续沿用剩余秒数重建 endTime） */
  function start() {
    if (running.value) return
    // 完成态不允许直接 start，必须先 reset/switchMode 清除 completed
    if (completed.value) return
    // 若 endTime 已耗尽或未设置，按当前模式重算
    if (endTime.value === 0 || remaining.value <= 0) {
      const total = mode.value === 'study' ? studySeconds : restSeconds
      endTime.value = Date.now() + total * 1000
    } else {
      // 暂停继续：把剩余秒数转成新的 endTime，避免丢失
      endTime.value = Date.now() + remaining.value * 1000
    }
    running.value = true
    // 防叠加：开始前先清旧定时器
    if (intervalId !== null) clearInterval(intervalId)
    intervalId = setInterval(tick, 250)
    // 立即 tick 一次，避免首帧延迟
    tick()
  }

  /** 暂停：清定时器，保留 remaining（不重置） */
  function pause() {
    if (!running.value) return
    stop()
    // 暂停瞬间快照剩余时间，下次 start 据此重建 endTime
    if (endTime.value > 0) {
      remaining.value = Math.max(0, Math.round((endTime.value - Date.now()) / 1000))
    }
  }

  /**
   * 重置：清定时器 + 清完成态 + 回到指定模式初始态
   * @param targetMode 默认 'study'，便于完成弹窗"确认"后回到专注
   */
  function reset(targetMode: CountdownMode = 'study') {
    stop()
    completed.value = false
    mode.value = targetMode
    running.value = false
    endTime.value = 0
    remaining.value = targetMode === 'study' ? studySeconds : restSeconds
  }

  /** 手动切换模式（不自动开始）：会清完成态 */
  function switchMode(target: CountdownMode) {
    stop()
    completed.value = false
    mode.value = target
    running.value = false
    endTime.value = 0
    remaining.value = target === 'study' ? studySeconds : restSeconds
  }

  /** 应用新配置：清完成态 + 回到 study 初始态，运行中也会停止 */
  function applyConfig(newStudy: number, newRest: number) {
    stop()
    completed.value = false
    mode.value = 'study'
    running.value = false
    endTime.value = 0
    // 直接改 options 字段，progress 计算会自动跟随
    ;(options.studySeconds = newStudy), (options.restSeconds = newRest)
    remaining.value = newStudy
  }

  function stop() {
    if (intervalId !== null) {
      clearInterval(intervalId)
      intervalId = null
    }
    running.value = false
  }

  // 组件卸载必清，避免后台泄漏
  onUnmounted(() => {
    if (intervalId !== null) clearInterval(intervalId)
  })

  return {
    mode,
    running,
    completed,
    remaining,
    progress,
    display,
    start,
    pause,
    reset,
    switchMode,
    applyConfig,
  }
}
