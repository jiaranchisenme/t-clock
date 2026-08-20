<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useTimerStore } from '../stores/timer'
import { useCheckinStore } from '../stores/checkin'
import { useCountdown, type CountdownMode } from '../composables/useCountdown'
import { saveSession } from '../api/session'

const timerStore = useTimerStore()
const checkinStore = useCheckinStore()

// ----- 弹窗与提示状态 -----
const configOpen = ref(false)            // 设置弹窗
const completedOpen = ref(false)         // 完成弹窗
const completedMode = ref<CountdownMode>('study')
const saving = ref(false)
const toast = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

function showToast(msg: string) {
  toast.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2500)
}

// 设置弹窗草稿（独立于 store 真值，避免输入过程抖动）
const draftWork = ref(timerStore.workDuration)
const draftBreak = ref(timerStore.breakDuration)
const workErr = ref('')
const breakErr = ref('')

// ----- 倒计时核心 -----
const countdown = useCountdown({
  studySeconds: timerStore.workDuration * 60,
  restSeconds: timerStore.breakDuration * 60,
  // 完成回调：仅 study 完整结束才落库 session（需求 10）
  onComplete: async (finishedMode) => {
    completedMode.value = finishedMode
    if (finishedMode === 'study') {
      try {
        const now = Date.now()
        const startISO = new Date(now - timerStore.workDuration * 60 * 1000).toISOString()
        const endISO = new Date(now).toISOString()
        const result = await saveSession({
          sessionType: 'WORK',
          startTime: startISO,
          endTime: endISO,
          durationMinutes: timerStore.workDuration,
          taskId: timerStore.activeTaskId ?? undefined,
        })
        // 回写 checkin 今日累计
        checkinStore.setTodayFromSession(result.todayFocusMinutes)
      } catch (e) {
        console.warn('session 落库失败', e)
        showToast('专注记录保存失败，已记录本地状态')
      }
    }
    // 需求 9：显示自定义完成弹窗，不使用 alert
    completedOpen.value = true
  },
})

// 监听 store 配置变化（loadConfig 完成后），同步到 composable 与草稿
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

// ----- 模式切换（需求 1） -----
function switchToStudy() {
  if (countdown.mode.value !== 'study') countdown.switchMode('study')
}
function switchToRest() {
  if (countdown.mode.value !== 'rest') countdown.switchMode('rest')
}

// ----- 输入校验（需求 6/7） -----
function validateWork(v: number): string {
  if (v === null || v === undefined || Number.isNaN(v)) return '学习时长不能为空'
  if (!Number.isFinite(v)) return '学习时长必须为数字'
  if (v <= 0) return '学习时长必须为正数'
  if (!Number.isInteger(v)) return '学习时长必须为整数'
  if (v < 1 || v > 180) return '学习时长范围 1~180 分钟'
  return ''
}
function validateBreak(v: number): string {
  if (v === null || v === undefined || Number.isNaN(v)) return '休息时长不能为空'
  if (!Number.isFinite(v)) return '休息时长必须为数字'
  if (v <= 0) return '休息时长必须为正数'
  if (!Number.isInteger(v)) return '休息时长必须为整数'
  if (v < 1 || v > 60) return '休息时长范围 1~60 分钟'
  return ''
}

// 草稿实时校验：输入过程即时反馈
watch(draftWork, (v) => {
  const num = Number(v)
  workErr.value = validateWork(num)
})
watch(draftBreak, (v) => {
  const num = Number(v)
  breakErr.value = validateBreak(num)
})

// ----- 保存配置（需求 5/8） -----
async function applyConfig() {
  const w = Number(draftWork.value)
  const b = Number(draftBreak.value)
  workErr.value = validateWork(w)
  breakErr.value = validateBreak(b)
  if (workErr.value || breakErr.value) {
    showToast(workErr.value || breakErr.value)
    return
  }
  saving.value = true
  try {
    await timerStore.saveConfig(w, b)
    // 需求 8：保存后应用配置 + 重置计时（applyConfig 内部已 reset 到 study）
    countdown.applyConfig(w * 60, b * 60)
    showToast('配置已保存，计时已重置')
    configOpen.value = false
  } catch (e) {
    // 网络失败也本地应用，不阻塞 UI
    countdown.applyConfig(w * 60, b * 60)
    showToast('配置已本地应用（保存失败）')
  } finally {
    saving.value = false
  }
}

// ----- 完成弹窗确认（需求 9） -----
function confirmCompleted() {
  // study 完成后切到休息；rest 完成后回到专注
  const next: CountdownMode = completedMode.value === 'study' ? 'rest' : 'study'
  completedOpen.value = false
  countdown.reset(next)
}

// ----- 派生展示 -----
const modeLabel = computed(() => (countdown.mode.value === 'study' ? '专注' : '休息'))
const modeClass = computed(() => countdown.mode.value)

// 环形表盘 SVG 几何
const RADIUS = 130
const CIRCUM = 2 * Math.PI * RADIUS
const dashOffset = computed(() => CIRCUM * (1 - countdown.progress.value))

// 按钮禁用逻辑
const canStart = computed(() => !countdown.running.value && !countdown.completed.value)
const canPause = computed(() => countdown.running.value)
</script>

<template>
  <section class="page">
    <h2>番茄专注计时</h2>

    <!-- 1. 模式切换 -->
    <div class="mode-switch">
      <button
        class="mode-btn"
        :class="{ active: countdown.mode.value === 'study' }"
        @click="switchToStudy"
      >专注</button>
      <button
        class="mode-btn"
        :class="{ active: countdown.mode.value === 'rest' }"
        @click="switchToRest"
      >休息</button>
    </div>

    <!-- 3. 圆形进度表盘 + 2. MM:SS 倒计时 -->
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
        <div class="dial-state">
          <span v-if="countdown.completed.value">已完成</span>
          <span v-else-if="countdown.running.value">运行中</span>
          <span v-else>已暂停</span>
        </div>
      </div>
    </div>

    <!-- 4. 开始/暂停、重置、设置按钮 -->
    <div class="controls">
      <button class="btn" :disabled="!canStart" @click="countdown.start">开始</button>
      <button class="btn" :disabled="!canPause" @click="countdown.pause">暂停</button>
      <button class="btn" @click="countdown.reset('study')">重置</button>
      <button class="btn ghost" @click="configOpen = true">设置</button>
    </div>

    <!-- 今日累计 -->
    <p class="today">今日专注 {{ checkinStore.todayFocusMinutes }} 分钟</p>

    <!-- 5. 设置弹窗 -->
    <transition name="fade">
      <div v-if="configOpen" class="modal-mask" @click.self="configOpen = false">
        <div class="modal">
          <h3>计时器设置</h3>
          <label class="field">
            <span>专注时长（分钟）<em>1~180</em></span>
            <input
              v-model.number="draftWork"
              type="number"
              min="1"
              max="180"
              step="1"
              placeholder="1~180"
            />
            <small v-if="workErr" class="err">{{ workErr }}</small>
          </label>
          <label class="field">
            <span>休息时长（分钟）<em>1~60</em></span>
            <input
              v-model.number="draftBreak"
              type="number"
              min="1"
              max="60"
              step="1"
              placeholder="1~60"
            />
            <small v-if="breakErr" class="err">{{ breakErr }}</small>
          </label>
          <div class="modal-actions">
            <button class="btn" @click="configOpen = false">取消</button>
            <button class="btn primary" :disabled="saving" @click="applyConfig">
              {{ saving ? '保存中...' : '保存并重置' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 9. 完成自定义弹窗（不使用浏览器 alert） -->
    <transition name="fade">
      <div v-if="completedOpen" class="modal-mask">
        <div class="modal">
          <div class="completed-icon" :class="completedMode">{{ completedMode === 'study' ? '✓' : '☕' }}</div>
          <h3>{{ completedMode === 'study' ? '专注完成！' : '休息结束！' }}</h3>
          <p class="completed-tip">
            <template v-if="completedMode === 'study'">
              已记录本次专注 {{ timerStore.workDuration }} 分钟，进入休息一下吧
            </template>
            <template v-else>
              休息充足，开始新一轮专注吧
            </template>
          </p>
          <div class="modal-actions">
            <button class="btn primary" @click="confirmCompleted">确定</button>
          </div>
        </div>
      </div>
    </transition>

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
.mode-switch {
  display: inline-flex;
  border: 1px solid var(--border, #e2e2e8);
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 8px;
}
.mode-btn {
  padding: 8px 28px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-muted, #6b6b78);
  transition: all 0.15s;
}
.mode-btn.active {
  background: var(--brand, #6c4bd6);
  color: #fff;
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
.today {
  color: var(--text-muted, #6b6b78);
  font-size: 13px;
  margin-top: 16px;
}
/* 弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: var(--surface, #fff);
  border-radius: 12px;
  padding: 24px;
  min-width: 320px;
  max-width: 90vw;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}
.modal h3 {
  margin: 0 0 16px;
  font-size: 18px;
}
.field {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted, #6b6b78);
  margin-bottom: 12px;
}
.field span em {
  color: var(--text-muted, #9a9aa8);
  font-style: normal;
  margin-left: 4px;
}
.field input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--border, #e2e2e8);
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}
.field input:focus {
  outline: none;
  border-color: var(--brand, #6c4bd6);
}
.err {
  color: #e53935;
  font-size: 11px;
}
.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
}
.completed-icon {
  font-size: 40px;
  margin: 8px 0;
}
.completed-icon.study {
  color: var(--brand, #6c4bd6);
}
.completed-icon.rest {
  color: #2eb872;
}
.completed-tip {
  font-size: 13px;
  color: var(--text-muted, #6b6b78);
  margin: 8px 0 0;
}
.toast {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: #333;
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1100;
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
