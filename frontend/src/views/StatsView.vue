<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useStatsStore } from '../stores/stats'
import StatsChart, { type ChartType } from '../components/StatsChart.vue'

type Range = 'week' | 'month'

const stats = useStatsStore()
const range = ref<Range>('week')
// 新增：图表类型选择（bar 柱 / line 折 / area 面积 / pie 环形）
const chartType = ref<ChartType>('bar')
const CHART_TYPES: readonly { key: ChartType; label: string }[] = [
  { key: 'bar', label: '柱状图' },
  { key: 'line', label: '折线图' },
  { key: 'area', label: '面积图' },
  { key: 'pie', label: '环形分布图' },
] as const

// 需求 1：7 天 vs 30 天；当前 tab 数据 & 汇总
const currentData = computed(() =>
  range.value === 'week' ? stats.daily7 : stats.daily30,
)
const currentTotal = computed(() =>
  range.value === 'week' ? stats.weeklyTotal : stats.monthlyTotal,
)
const currentLabel = computed(() => (range.value === 'week' ? '近 7 天' : '近 30 天'))

// 图表标题 + 是否启用滚动条（30 天时显示 slider zoom）
const chartTitle = computed(() => `${currentLabel.value}专注时长`)
const scrollable = computed(() => range.value === 'month')

// 派生指标：平均每日 + 最高单日
const avgMinutes = computed(() => {
  const arr = currentData.value
  if (!arr.length) return 0
  const sum = arr.reduce((acc, d) => acc + (d.minutes ?? 0), 0)
  return Math.round(sum / arr.length)
})
const maxMinutes = computed(() => {
  const arr = currentData.value
  if (!arr.length) return 0
  return Math.max(...arr.map((d) => d.minutes ?? 0))
})

async function switchRange(next: Range) {
  if (next === range.value) return
  range.value = next
  // 已加载则直接用 store 中缓存；未加载则懒加载
  if (next === 'week' && !stats.daily7.length) await stats.loadWeek()
  if (next === 'month' && !stats.daily30.length) await stats.loadMonth()
}

onMounted(async () => {
  await stats.loadAll()
})

// 切 Tab 时按需补拉
watch(range, (r) => {
  if (r === 'week' && !stats.daily7.length) stats.loadWeek()
  if (r === 'month' && !stats.daily30.length) stats.loadMonth()
})
</script>

<template>
  <section class="page">
    <div class="page-head">
      <div>
        <h2>📊 数据可视化统计</h2>
        <p class="sub">用数据见证每一次努力，量变终会达成质变 📈</p>
      </div>
      <!-- 需求 1：近 7 天 / 近 30 天切换 · 全局分段 tab -->
      <div class="segment" role="tablist" aria-label="统计周期">
        <button
          class="seg"
          :class="{ active: range === 'week' }"
          role="tab"
          @click="switchRange('week')"
        >近 7 天</button>
        <button
          class="seg"
          :class="{ active: range === 'month' }"
          role="tab"
          @click="switchRange('month')"
        >近 30 天</button>
      </div>
    </div>

    <!-- 统计汇总卡片 · 3 列网格 -->
    <div class="summary-row">
      <div class="summary-card card">
        <div class="summary-head">
          <span class="summary-icon brand">🧮</span>
          <span class="summary-label">{{ currentLabel }}累计</span>
        </div>
        <span class="summary-value">{{ currentTotal }}<small>分钟</small></span>
      </div>
      <div class="summary-card card">
        <div class="summary-head">
          <span class="summary-icon leaf">🌱</span>
          <span class="summary-label">日均专注</span>
        </div>
        <span class="summary-value">{{ avgMinutes }}<small>分钟</small></span>
      </div>
      <div class="summary-card card">
        <div class="summary-head">
          <span class="summary-icon amber">🏆</span>
          <span class="summary-label">最高单日</span>
        </div>
        <span class="summary-value">{{ maxMinutes }}<small>分钟</small></span>
      </div>
    </div>

    <div v-if="stats.error" class="error-tip">
      <span class="error-ico">⚠️</span>
      <span class="error-txt">{{ stats.error }}</span>
    </div>

    <!-- 图表类型选择：柱状图 / 折线图 / 面积图 / 环形分布图 -->
    <div class="segment chart-tabs" role="tablist" aria-label="图表类型">
      <button
        v-for="t in CHART_TYPES"
        :key="t.key"
        class="seg"
        :class="{ active: chartType === t.key }"
        role="tab"
        @click="chartType = t.key"
      >{{ t.label }}</button>
    </div>

    <!-- 图表卡片：加大边距，给呼吸感 -->
    <div class="chart-card card">
      <StatsChart
        :data="currentData"
        :title="chartTitle"
        :scrollable="scrollable"
        :chart-type="chartType"
      />
    </div>
  </section>
</template>

<style scoped>
.page {
  max-width: 920px;
  margin: 0 auto;
  padding: 20px 18px 48px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ---------- 标题 ---------- */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.page-head h2 {
  margin: 0;
}
.page-head .sub {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

/* ---------- 汇总卡片 ---------- */
.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.summary-card {
  padding: 18px 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: transform 0.2s, box-shadow 0.2s;
}
.summary-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}
.summary-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.summary-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--r-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  line-height: 1;
  flex-shrink: 0;
}
.summary-icon.brand { background: var(--brand-50); }
.summary-icon.leaf  { background: var(--leaf-50);  }
.summary-icon.amber { background: var(--amber-50); }

.summary-label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}
.summary-value {
  font-size: 30px;
  font-weight: 700;
  color: var(--text-h);
  line-height: 1.1;
  letter-spacing: -0.01em;
}
.summary-value small {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  margin-left: 4px;
  letter-spacing: 0;
}

/* ---------- 错误提示 ---------- */
.error-tip {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  background: var(--danger-50);
  color: var(--danger);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: var(--r-md);
  font-size: 13px;
  line-height: 1.5;
}
.error-ico { line-height: 1.4; }

/* ---------- 图表类型分段 tab ---------- */
.chart-tabs {
  align-self: flex-start;
  overflow-x: auto;
  max-width: 100%;
  scrollbar-width: none;
}
.chart-tabs::-webkit-scrollbar { display: none; }

/* ---------- 图表卡片：更大 padding，留出呼吸空间 ---------- */
.chart-card {
  padding: 24px;
}

/* ---------- 响应式：移动端 ---------- */
@media (max-width: 640px) {
  .summary-row { grid-template-columns: 1fr; gap: 10px; }
}
@media (max-width: 520px) {
  .page { padding: 14px 12px 40px; gap: 14px; }
  .summary-card { padding: 14px 14px 12px; gap: 8px; }
  .summary-value { font-size: 24px; }
  .chart-card { padding: 14px 12px 16px; }
  .page-head { flex-direction: column; }
  .page-head .segment { align-self: stretch; justify-content: center; display: flex; }
  .page-head .segment .seg { flex: 1; text-align: center; }
}
</style>
