<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useTimerStore } from '../stores/timer'
import { useCheckinStore } from '../stores/checkin'
import { useCountdown } from '../composables/useCountdown'
import { saveSession } from '../api/session'

const timerStore = useTimerStore()
const checkinStore = useCheckinStore()

// 配置面板草稿（独立于 store 真值，避免输入过程抖动）
const draftWork = ref(timerStore.workDuration)
const draftBreak = ref(timerStore.breakDuration)
const configOpen = ref(false)
const saving = ref(false)
const toast = ref('')

function showToast(msg: string) {
  toast.value = msg
  setTimeout(() => (toast.value = ''), 2500)
}

// 倒计时核心：分钟转秒传入
const countdown = useCountdown({
  studySeconds: timerStore.workDuration * 60,
  restSeconds: timerStore.breakDuration * 60,
  // 6. 任一阶段结束时回调：学习完成落库 + 联动 checkin
  onComplete: async (finishedMode) => {
    if (finishedMode === 'study') {
      try {
        const startISO = new Date(Date.now() - timerStore.workDuration * 60 * 1000).toISOString()
        const endISO = new Date().toISOString()
        const result = await saveSession({
          sessionType: 'WORK',
          startTime: startISO,
          endTime: endISO,
          durationMinutes: timerStore.workDuration,
          taskId: timerStore.activeTaskId ?? undefined,
        })
        // 回写 checkin 今日累计，无需重拉
        checkinStore.setTodayFromSession(result.todayFocusMinutes)
        showToast('学习完成，已记录专注')
      } catch (e) {
        showToast('专注记录保存失败')
        console.warn(e)
      }
    } else {
      showToast('休息结束，开始新的一轮吧')
    }
  },
})

// 监听 store 配置变化（loadConfig 完成后），同步到 composable
watch(
  () => [timerStore.workDuration, timerStore.breakDuration],
  ([w, b]) => {
    draftWork.value = w
    draftBreak.value = b
    countdown.applyConfig(w * 60, b * 60)
  },
)

onMounted(() => {
  timerStore.loadConfig()
  checkinStore.loadToday()
})

// 应用新配置：校验 + 持久化 + 应用到 composable
async function applyConfig() {
  const w = Number(draftWork.value)
  const b = Number(draftBreak.value)
  // 4.1.2 容错：非数字/负数/空值拦截
  if (!Number.isFinite(w) || w <= 0) return showToast('学习时长必须为正数')
  if (!Number.isFinite(b) || b <= 0) return showToast('休息时长必须为正数')
  if (w > 180) return showToast('学习时长最多 180 分钟')
  if (b > 60) return showToast('休息时长最多 60 分钟')

  saving.value = true
  try {
    await timerStore.saveConfig(w, b)
    countdown.applyConfig(w * 60, b * 60)
    showToast('配置已保存')
    configOpen.value = false
  } catch (e) {
    // 网络失败也本地应用，不阻塞 UI
    countdown.applyConfig(w * 60, b * 60)
    showToast('配置已本地应用（保存失败）')
  } finally {
    saving.value = false
  }
}

const modeLabel = computed(() => (countdown.mode.value === 'study' ? '专注' : '休息'))
const modeClass = computed(() => (countdown.mode.value === 'study' ? 'study' : 'rest'))

// 环形表盘 SVG 几何
const RADIUS = 130
const CIRCUM = 2 * Math.PI * RADIUS
const dashOffset = computed(() => CIRCUM * (1 - countdown.progress.value))
</script>

<template>
  <section class="page">
    <h2>番茄专注计时</h2>

    <!-- 环形表盘 -->
    <div class="dial" :class="modeClass">
      <svg viewBox="0 0 300 300" class="dial-svg">
        <circle cx="150" cy="150" :r="RADIUS" class="dial-bg" />
        <circle
          cx="150"
          cy="150"
          :r="RADIUS"
          class="dial-fg"
          :stroke-dasharray="CIRCUM"
          :stroke-dashoffset="dashOffset"
          transform="rotate(-90 150 150)"
        />
      </svg>
      <div class="dial-center">
        <div class="dial-mode">{{ modeLabel }}</div>
        <div class="dial-time">{{ countdown.display.value }}</div>
        <div class="dial-state">{{ countdown.running.value ? '运行中' : '已暂停' }}</div>
      </div>
    </div>

    <!-- 控制按钮 -->
    <div class="controls">
      <button class="btn" :disabled="countdown.running.value" @click="countdown.start">开始</button>
      <button class="btn" :disabled="!countdown.running.value" @click="countdown.pause">暂停</button>
      <button class="btn" @click="countdown.reset">重置</button>
      <button class="btn" @click="countdown.switchMode(countdown.mode.value === 'study' ? 'rest' : 'study')">
        切换模式
      </button>
      <button class="btn ghost" @click="configOpen = !configOpen">配置</button>
    </div>

    <!-- 配置面板 -->
    <div v-if="configOpen" class="config">
      <label class="field">
        <span>学习时长(分)</span>
        <input v-model.number="draftWork" type="number" min="1" max="180" />
      </label>
      <label class="field">
        <span>休息时长(分)</span>
        <input v-model.number="draftBreak" type="number" min="1" max="60" />
      </label>
      <button class="btn primary" :disabled="saving" @click="applyConfig">
        {{ saving ? '保存中...' : '应用配置' }}
      </button>
    </div>

    <!-- 今日累计 -->
    <p class="today">今日专注 {{ checkinStore.todayFocusMinutes }} 分钟</p>

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </section>
</template>

<style scoped>
.page {
  text-align: center;
  padding: 32px 16px;
  position: relative;
}
.dial {
  position: relative;
  width: 300px;
  height: 300px;
  margin: 24px auto;
}
.dial-svg {
  width: 100%;
  height: 100%;
}
.dial-bg {
  fill: none;
  stroke: var(--border, #e2e2e8);
  stroke-width: 12;
}
.dial-fg {
  fill: none;
  stroke: var(--brand, #6c4bd6);
  stroke-width: 12;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.3s ease;
}
.dial.study .dial-fg {
  stroke: var(--brand, #6c4bd6);
}
.dial.rest .dial-fg {
  stroke: #2eb872;
}
.dial-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.dial-mode {
  font-size: 14px;
  color: var(--text-muted, #6b6b78);
  letter-spacing: 2px;
}
.dial-time {
  font-size: 56px;
  font-weight: 700;
  letter-spacing: 4px;
  margin: 8px 0;
}
.dial-state {
  font-size: 12px;
  color: var(--text-muted, #6b6b78);
}
.controls {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
  margin: 16px 0;
}
.btn {
  padding: 8px 20px;
  border: 1px solid var(--border, #e2e2e8);
  background: var(--surface, #fff);
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s;
}
.btn:hover:not(:disabled) {
  border-color: var(--brand, #6c4bd6);
  color: var(--brand, #6c4bd6);
}
.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.btn.primary {
  background: var(--brand, #6c4bd6);
  color: #fff;
  border-color: var(--brand, #6c4bd6);
}
.btn.ghost {
  background: transparent;
}
.config {
  display: inline-flex;
  gap: 16px;
  align-items: end;
  padding: 16px;
  border: 1px solid var(--border, #e2e2e8);
  border-radius: 12px;
  margin: 16px auto;
  flex-wrap: wrap;
  justify-content: center;
}
.field {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted, #6b6b78);
}
.field input {
  width: 90px;
  padding: 6px 8px;
  border: 1px solid var(--border, #e2e2e8);
  border-radius: 6px;
  font-size: 14px;
}
.today {
  color: var(--text-muted, #6b6b78);
  font-size: 13px;
  margin-top: 16px;
}
.toast {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--surface, #333);
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
