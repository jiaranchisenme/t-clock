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
    <h2>每日学习打卡</h2>

    <!-- 今日状态卡片 -->
    <div class="status-card">
      <div class="status-row">
        <div class="status-item">
          <span class="label">今日日期</span>
          <span class="value">{{ todayISO }}</span>
        </div>
        <div class="status-item">
          <span class="label">今日专注</span>
          <span class="value">{{ checkin.todayFocusMinutes }} 分钟</span>
        </div>
        <div class="status-item">
          <span class="label">连续打卡</span>
          <span class="value streak">{{ checkin.streak }} 天</span>
        </div>
      </div>

      <!-- 需求 5：已打卡时按钮禁用并显示「今日已打卡」 -->
      <button
        class="checkin-btn"
        :class="{ done: checkin.todayChecked }"
        :disabled="checkin.todayChecked || submitting"
        @click="handleCheckin"
      >
        <template v-if="checkin.todayChecked">今日已打卡</template>
        <template v-else-if="submitting">提交中…</template>
        <template v-else>立即打卡</template>
      </button>
    </div>

    <!-- 近 14 天日历（需求 7/8）-->
    <div class="calendar">
      <p class="calendar-title">最近 14 天打卡记录</p>
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
          <span class="dot" :class="{ on: cell.checked }"></span>
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
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
  background: var(--code-bg);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.label {
  font-size: 13px;
  color: var(--text);
}

.value {
  font-size: 20px;
  color: var(--text-h);
  font-weight: 600;
}

.value.streak {
  color: var(--accent);
}

.checkin-btn {
  align-self: flex-start;
  padding: 10px 24px;
  font-size: 16px;
  font-family: inherit;
  color: #fff;
  background: var(--accent);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s;
}

.checkin-btn:hover:not(:disabled) {
  opacity: 0.9;
}
.checkin-btn:active:not(:disabled) {
  transform: scale(0.98);
}
.checkin-btn:disabled {
  cursor: not-allowed;
  background: var(--border);
  color: var(--text);
}
.checkin-btn.done {
  background: var(--accent-bg);
  color: var(--accent);
  border: 1px solid var(--accent-border);
}

.calendar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.calendar-title {
  font-size: 14px;
  color: var(--text);
}

.day-grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}

.day-cell {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: var(--bg);
  transition: background 0.2s, border-color 0.2s;
}

.day-cell .weekday {
  font-size: 12px;
  color: var(--text);
}

.day-cell .date-num {
  font-size: 16px;
  color: var(--text-h);
  font-weight: 600;
}

.day-cell .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--border);
}

/* 需求 8：已打卡日期高亮 */
.day-cell.checked {
  border-color: var(--accent-border);
  background: var(--accent-bg);
}
.day-cell.checked .dot.on {
  background: var(--accent);
}

.day-cell.today {
  border-width: 2px;
  border-color: var(--accent);
}

.toast {
  position: fixed;
  left: 50%;
  bottom: 48px;
  transform: translateX(-50%);
  background: var(--text-h);
  color: var(--bg);
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 14px;
  box-shadow: var(--shadow);
  z-index: 50;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
