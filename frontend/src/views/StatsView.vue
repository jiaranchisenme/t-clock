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
    <header class="header">
      <h2>数据可视化统计看板</h2>
      <!-- 需求 1：近 7 天 / 近 30 天切换 -->
      <div class="tabs" role="tablist" aria-label="统计周期">
        <button
          class="tab"
          :class="{ active: range === 'week' }"
          role="tab"
          @click="switchRange('week')"
        >近 7 天</button>
        <button
          class="tab"
          :class="{ active: range === 'month' }"
          role="tab"
          @click="switchRange('month')"
        >近 30 天</button>
      </div>
    </header>

    <div class="summary-row">
      <div class="summary-card">
        <span class="summary-label">{{ currentLabel }}累计</span>
        <span class="summary-value">{{ currentTotal }}<small>分钟</small></span>
      </div>
      <div class="summary-card">
        <span class="summary-label">日均专注</span>
        <span class="summary-value">{{ avgMinutes }}<small>分钟</small></span>
      </div>
      <div class="summary-card">
        <span class="summary-label">最高单日</span>
        <span class="summary-value">{{ maxMinutes }}<small>分钟</small></span>
      </div>
    </div>

    <p v-if="stats.error" class="error">{{ stats.error }}</p>

    <!-- 图表类型选择：柱状图 / 折线图 / 面积图 / 环形分布图 -->
    <div class="tabs" role="tablist" aria-label="图表类型">
      <button
        v-for="t in CHART_TYPES"
        :key="t.key"
        class="tab"
        :class="{ active: chartType === t.key }"
        role="tab"
        @click="chartType = t.key"
      >{{ t.label }}</button>
    </div>

    <!-- 需求 7：StatsChart 只通过 props 接收 data 与类型 -->
    <StatsChart
      :data="currentData"
      :title="chartTitle"
      :scrollable="scrollable"
      :chart-type="chartType"
    />
  </section>
</template>

<style scoped>
.page {
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.tabs {
  display: inline-flex;
  padding: 4px;
  gap: 4px;
  background: var(--code-bg, #f4f3ec);
  border: 1px solid var(--border, #e5e4e7);
  border-radius: 999px;
}
.tab {
  border: none;
  background: transparent;
  padding: 6px 16px;
  font-size: 14px;
  border-radius: 999px;
  color: var(--text, #6b6375);
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.tab.active {
  background: var(--accent, #aa3bff);
  color: #fff;
  box-shadow: 0 2px 6px rgba(170, 59, 255, 0.3);
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
@media (max-width: 640px) {
  .summary-row { grid-template-columns: 1fr; }
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 20px;
  border: 1px solid var(--border, #e5e4e7);
  border-radius: 12px;
  background: var(--code-bg, #f4f3ec);
}
.summary-label {
  font-size: 13px;
  color: var(--text, #6b6375);
}
.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-h, #08060d);
  line-height: 1.2;
}
.summary-value small {
  font-size: 13px;
  font-weight: 500;
  color: var(--text, #6b6375);
  margin-left: 4px;
}

.error {
  padding: 10px 14px;
  background: #fff4f0;
  color: #d84a1b;
  border: 1px solid #f2d6c6;
  border-radius: 8px;
  font-size: 14px;
}
</style>
