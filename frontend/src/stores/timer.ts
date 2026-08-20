import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTimerConfig, updateTimerConfig, type TimerConfigDTO } from '../api/timer'

/**
 * Timer Store：仅作为配置持久化层与跨页面共享的配置容器。
 * 计时核心逻辑（running/remaining/progress/display）由 useCountdown composable 管理，
 * 避免双写与状态分裂。
 *
 * 默认值：与服务端 schema.sql 一致（25/5），保证后端不可达时主页仍可渲染。
 */
export const useTimerStore = defineStore('timer', () => {
  const workDuration = ref(25) // 分钟
  const breakDuration = ref(5) // 分钟

  // 当前关联任务（可选）
  const activeTaskId = ref<number | null>(null)

  // 容错加载：异常静默回退默认值，主页面不崩
  async function loadConfig() {
    try {
      const cfg = await getTimerConfig()
      if (cfg) {
        workDuration.value = cfg.workDuration
        breakDuration.value = cfg.breakDuration
      }
    } catch (e) {
      console.warn('加载计时器配置失败，使用默认值', e)
    }
  }

  // 写后端持久化；写失败由调用方捕获，不影响 UI
  async function saveConfig(work: number, brk: number) {
    const payload: TimerConfigDTO = { workDuration: work, breakDuration: brk }
    return updateTimerConfig(payload)
  }

  function setActiveTask(id: number | null) {
    activeTaskId.value = id
  }

  return {
    workDuration,
    breakDuration,
    activeTaskId,
    loadConfig,
    saveConfig,
    setActiveTask,
  }
})
