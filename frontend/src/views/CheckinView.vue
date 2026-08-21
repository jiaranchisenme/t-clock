<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useCheckinStore } from '../stores/checkin'

const checkin = useCheckinStore()

// ----- 本地日期工具（需求 1：today 使用本地日期 YYYY-MM-DD）-----
const WEEK = ['日', '一', '二', '三', '四', '五', '六']

function localISO(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// 今日（本地日期）
const today = new Date()
const todayISO = localISO(today)

// ----- 近 14 天日历（需求 7/8）-----
// 从 13 天前到今天，共 14 格；每格标注日期、星期、是否今日、是否已打卡
const calendar = computed(() => {
  const list: {
    iso: string
    day: number
    weekday: string
    isToday: boolean
    checked: boolean
  }[] = []
  for (let i = 13; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const iso = localISO(d)
    list.push({
      iso,
      day: d.getDate(),
      weekday: WEEK[d.getDay()],
      isToday: iso === todayISO,
      checked: checkin.calendarDates.includes(iso), // 需求 8：已打卡日期高亮
    })
  }
  return list
})

// ----- Toast 提示（需求 6：重复点击给出友好提示）-----
const toast = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null
function showToast(msg: string) {
  toast.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2500)
}

// ----- 打卡动作（需求 3/4/5/6/11）-----
const submitting = ref(false)

async function handleCheckin() {
  // 需求 5：已打卡时按钮已禁用，此处为兜底（如快速双击未及时禁用）
  if (checkin.todayChecked) {
    showToast('今日已打卡，明天再来吧')
    return
  }
  if (submitting.value) return // 防重复提交
  submitting.value = true
  try {
    await checkin.doCheckinAction()
    // 需求 11：刷新已在 store 内完成；这里给出成功反馈
    showToast('打卡成功，继续加油')
  } catch (e: unknown) {
    // 需求 6：后端转 409「今日已打卡」时给出友好提示
    const msg = e instanceof Error ? e.message : '打卡失败'
    showToast(msg.includes('已打卡') ? '今日已打卡，请勿重复提交' : msg)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 需求 11：进入页面即拉取最新统计
  checkin.refreshAll()
})
</script>

<template>
  <section class="page">
    <div class="page-head">
      <h2>🌸 每日学习打卡</h2>
      <p class="sub">坚持打卡，记录每一次专注的脚印 ✨</p>
    </div>

    <!-- 今日状态卡片 · 清新渐变 -->
    <div class="status-card card">
      <div class="status-row">
        <div class="status-item">
          <div class="item-icon date-icon">📅</div>
          <div class="item-body">
            <span class="label">今日日期</span>
            <span class="value">{{ todayISO }}</span>
          </div>
        </div>
        <div class="status-item">
          <div class="item-icon focus-icon">⏱️</div>
          <div class="item-body">
            <span class="label">今日专注</span>
            <span class="value">{{ checkin.todayFocusMinutes }} <small>分钟</small></span>
          </div>
        </div>
        <div class="status-item">
          <div class="item-icon streak-icon">🔥</div>
          <div class="item-body">
            <span class="label">连续打卡</span>
            <span class="value streak">{{ checkin.streak }} <small>天</small></span>
          </div>
        </div>
      </div>

      <!-- 打卡按钮：柔粉主色 · 胶囊 -->
      <button
        class="checkin-btn"
        :class="{ done: checkin.todayChecked }"
        :disabled="checkin.todayChecked || submitting"
        @click="handleCheckin"
      >
        <span class="btn-icon" aria-hidden="true">{{ checkin.todayChecked ? '✅' : '🌸' }}</span>
        <template v-if="checkin.todayChecked">今日已打卡，明天见～</template>
        <template v-else-if="submitting">提交中…</template>
        <template v-else>立即打卡，完成今日成就</template>
      </button>
    </div>

    <!-- 近 14 天日历（需求 7/8）-->
    <div class="calendar card">
      <div class="cal-head">
        <p class="calendar-title">📆 最近 14 天打卡记录</p>
        <div class="legend">
          <span class="legend-item"><span class="legend-dot checked"></span>已打卡</span>
          <span class="legend-item"><span class="legend-dot today"></span>今天</span>
        </div>
      </div>
      <ul class="day-grid">
        <li
          v-for="cell in calendar"
          :key="cell.iso"
          class="day-cell"
          :class="{ checked: cell.checked, today: cell.isToday }"
          :title="cell.iso + (cell.checked ? '（已打卡）' : '')"
        >
          <span class="weekday">{{ cell.weekday }}</span>
          <span class="date-num">{{ cell.day }}</span>
          <span v-if="cell.checked" class="check-svg" aria-hidden="true">
            <svg viewBox="0 0 16 16" width="10" height="10">
              <path d="M3 8.5L6.5 12L13 5" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            </svg>
          </span>
          <span v-else class="dot"></span>
        </li>
      </ul>
    </div>

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </section>
</template>

<style scoped>
.page {
  max-width: 760px;
  margin: 0 auto;
  padding: 20px 18px 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- 标题 ---------- */
.page-head {
  padding: 4px 2px 0;
}
.page-head h2 {
  margin: 0;
}
.page-head .sub {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

/* ---------- 状态卡片 ---------- */
.status-card {
  background:
    linear-gradient(135deg, var(--petal-50) 0%, #ffffff 45%, var(--brand-50) 100%);
  border-color: var(--border);
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
  overflow: hidden;
}
.status-card::before {
  content: '';
  position: absolute;
  top: -40px;
  right: -40px;
  width: 140px;
  height: 140px;
  background: radial-gradient(circle, var(--petal-50) 0%, transparent 70%);
  opacity: 0.9;
  pointer-events: none;
}

.status-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  position: relative;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--r-md);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--border-soft);
  backdrop-filter: blur(6px);
}
.item-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--r-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.date-icon   { background: var(--brand-50); }
.focus-icon  { background: var(--amber-50); }
.streak-icon { background: var(--petal-50); }

.item-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.label {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
}
.value {
  font-size: 18px;
  color: var(--text-h);
  font-weight: 700;
  line-height: 1.3;
  margin-top: 2px;
  word-break: break-all;
}
.value small {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  margin-left: 2px;
}
.value.streak {
  color: var(--petal);
}

/* ---------- 打卡按钮 ---------- */
.checkin-btn {
  align-self: stretch;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 20px;
  font-size: 15px;
  font-family: inherit;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--petal) 0%, #f472b6 100%);
  border: none;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.2s, opacity 0.2s;
  box-shadow: 0 8px 22px -8px rgba(236, 72, 153, 0.55);
  position: relative;
}
.btn-icon {
  line-height: 1;
  font-size: 17px;
}
.checkin-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px -10px rgba(236, 72, 153, 0.65);
}
.checkin-btn:active:not(:disabled) {
  transform: translateY(0);
}
.checkin-btn:disabled {
  cursor: not-allowed;
}
.checkin-btn.done {
  background: var(--leaf-50);
  color: var(--leaf);
  border: 1px solid rgba(34, 197, 94, 0.25);
  box-shadow: none;
}

/* ---------- 日历卡片 ---------- */
.calendar {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.calendar-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-h);
}
.legend {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: var(--text-muted);
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 4px;
  display: inline-block;
}
.legend-dot.checked {
  background: var(--leaf);
  box-shadow: 0 0 0 2px var(--leaf-50);
}
.legend-dot.today {
  border: 2px solid var(--petal);
  background: transparent;
}

.day-grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.day-cell {
  aspect-ratio: 1 / 1.15;
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  padding: 6px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  background: var(--surface-muted);
  transition: all 0.2s;
  position: relative;
}
.day-cell .weekday {
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1;
  margin-top: 2px;
}
.day-cell .date-num {
  font-size: 17px;
  color: var(--text-h);
  font-weight: 700;
  line-height: 1.2;
}
.day-cell .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--border);
  margin-bottom: 2px;
}
.day-cell .check-svg {
  color: var(--leaf);
  margin-bottom: 1px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 已打卡 */
.day-cell.checked {
  border-color: rgba(34, 197, 94, 0.35);
  background: var(--leaf-50);
}
.day-cell.checked .date-num {
  color: #15803d;
}
.day-cell.checked .weekday {
  color: #4ade80;
}

/* 今天 · 柔粉描边 */
.day-cell.today {
  border-width: 2px;
  border-color: var(--petal);
  box-shadow: 0 0 0 3px var(--petal-50);
}
.day-cell.today::after {
  content: '今';
  position: absolute;
  top: -8px;
  right: -4px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: var(--petal);
  padding: 1px 6px;
  border-radius: var(--r-pill);
  line-height: 1.4;
}

/* ---------- Toast ---------- */
.toast {
  position: fixed;
  left: 50%;
  bottom: 36px;
  transform: translateX(-50%);
  background: var(--text-h);
  color: var(--bg);
  padding: 10px 22px;
  border-radius: var(--r-pill);
  font-size: 13px;
  box-shadow: var(--shadow);
  z-index: 100;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ---------- 响应式：平板 & 小桌面 ---------- */
@media (max-width: 720px) {
  .status-row {
    grid-template-columns: 1fr;
  }
}

/* ---------- 响应式：移动端 ---------- */
@media (max-width: 520px) {
  .page { padding: 14px 12px 40px; gap: 12px; }
  .status-card { padding: 16px; gap: 16px; }
  .status-row { gap: 8px; }
  .status-item { padding: 10px; gap: 10px; }
  .item-icon { width: 36px; height: 36px; font-size: 18px; }
  .value { font-size: 16px; }
  .checkin-btn { padding: 12px 18px; font-size: 14px; }
  .calendar { padding: 16px; gap: 12px; }
  .cal-head { flex-direction: column; align-items: flex-start; }
  /* 移动端：2 行 × 7 列 改为 4 列（紧凑但不挤） */
  .day-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 7px;
  }
  .day-cell { aspect-ratio: 1 / 1.1; }
  .day-cell .date-num { font-size: 15px; }
  .day-cell .weekday { font-size: 10px; }
}
</style>
