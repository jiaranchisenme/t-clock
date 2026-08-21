<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed, nextTick } from 'vue'
import * as echarts from 'echarts/core'
// 需求 12：只按需注册使用到的 ECharts 模块——4 种图表 + 共用组件
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  TitleComponent,
  DataZoomComponent,
  LegendComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { DayStat } from '../api/stats'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  TitleComponent,
  DataZoomComponent,
  LegendComponent,
  CanvasRenderer,
])

// ----- props：StatsChart 只通过 props 接收 data/类型（需求 7）-----
export type ChartType = 'bar' | 'line' | 'area' | 'pie'
const CHART_TYPES: readonly ChartType[] = ['bar', 'line', 'area', 'pie'] as const

interface Props {
  data: readonly DayStat[]
  title?: string
  /** 是否需要显示 x 轴滚动条（仅 bar/line/area 生效；饼图不使用 dataZoom） */
  scrollable?: boolean
  chartType?: ChartType
}
const props = withDefaults(defineProps<Props>(), {
  title: '',
  scrollable: false,
  chartType: 'bar',
})

// 派生：一个可靠的 type 值，用于避免 undefined/未知字符串
const type = computed<ChartType>(() =>
  (CHART_TYPES as readonly string[]).includes(props.chartType)
    ? (props.chartType as ChartType)
    : 'bar',
)

// 需求 11：无有效数据的判定
const hasValidData = computed(() =>
  Array.isArray(props.data) && props.data.some((d) => Number.isFinite(d.minutes) && d.minutes > 0),
)

const chartEl = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

// 为饼图每个日期分片生成一个稳定调色板（不依赖随机，保证切换类型颜色一致）
const PALETTE = [
  '#aa3bff',
  '#6c4bd6',
  '#f5a623',
  '#42c29f',
  '#3b82f6',
  '#ef4444',
  '#8b5cf6',
  '#14b8a6',
  '#f97316',
  '#06b6d4',
  '#84cc16',
  '#ec4899',
  '#6366f1',
  '#a3e635',
  '#f43f5e',
  '#eab308',
  '#22d3ee',
  '#facc15',
  '#c084fc',
  '#fde68a',
  '#93c5fd',
  '#86efac',
  '#fca5a5',
  '#f0abfc',
  '#7dd3fc',
  '#fcd34d',
  '#a78bfa',
  '#fca5a5',
  '#99f6e4',
  '#fdba74',
]
function colorFor(index: number): string {
  return PALETTE[index % PALETTE.length]
}

// ----- 四种 option 构建函数（需求：bar / line / area / pie）-----

type ListItem = { date: string; minutes: number }

function prepList(data: readonly DayStat[] | null | undefined): ListItem[] {
  if (!Array.isArray(data)) return []
  return data.map((d) => ({
    date: d?.date ?? '',
    minutes: Number.isFinite(d?.minutes) ? d.minutes : 0,
  }))
}

function buildCartesianCommon(list: ListItem[], scrollable: boolean, titleText: string) {
  const dates = list.map((d) => d.date)
  return {
    title: titleText
      ? {
          text: titleText,
          left: 'left',
          textStyle: { fontSize: 14, fontWeight: 600, color: 'var(--text-h, #08060d)' },
        }
      : undefined,
    tooltip: {
      trigger: 'axis' as const,
      axisPointer: { type: 'shadow' as const },
      valueFormatter: (v: unknown) => `${v as number} 分钟`,
    },
    grid: { left: 48, right: 20, top: titleText ? 40 : 20, bottom: scrollable ? 60 : 44 },
    xAxis: {
      type: 'category' as const,
      boundaryGap: true,
      data: dates,
      axisLabel: {
        color: 'var(--text, #6b6375)',
        fontSize: 11,
        rotate: list.length > 14 ? 45 : 0,
      },
      axisLine: { lineStyle: { color: 'var(--border, #e5e4e7)' } },
    },
    yAxis: {
      type: 'value' as const,
      name: '分钟',
      nameTextStyle: { color: 'var(--text, #6b6375)' },
      axisLabel: { color: 'var(--text, #6b6375)', fontSize: 11 },
      splitLine: { lineStyle: { color: 'var(--border, #e5e4e7)', type: 'dashed' } },
    },
    dataZoom: scrollable
      ? [
          {
            type: 'inside' as const,
            start: 0,
            end: 100,
            zoomOnMouseWheel: false,
            moveOnMouseWheel: true,
            moveOnMouseMove: false,
          },
          {
            type: 'slider' as const,
            height: 18,
            bottom: 8,
            borderColor: 'var(--border, #e5e4e7)',
            textStyle: { color: 'var(--text, #6b6375)' },
          },
        ]
      : undefined,
    backgroundColor: 'transparent',
  }
}

function buildBar(list: ListItem[], scrollable: boolean, title: string): echarts.EChartsCoreOption {
  const values = list.map((d) => d.minutes)
  const common = buildCartesianCommon(list, scrollable, title)
  return {
    ...common,
    series: [
      {
        type: 'bar',
        name: '专注分钟',
        data: values,
        barMaxWidth: list.length > 20 ? 14 : 22,
        itemStyle: {
          color: 'var(--accent, #aa3bff)',
          borderRadius: [4, 4, 0, 0],
        },
        emphasis: {
          itemStyle: { color: 'var(--brand, #6c4bd6)' },
        },
      },
    ],
  }
}

function buildLine(
  list: ListItem[],
  scrollable: boolean,
  title: string,
  area: boolean,
): echarts.EChartsCoreOption {
  const values = list.map((d) => d.minutes)
  const common = buildCartesianCommon(list, scrollable, title)
  return {
    ...common,
    tooltip: {
      ...common.tooltip,
      axisPointer: { type: 'line' as const },
    },
    series: [
      {
        type: 'line',
        name: '专注分钟',
        data: values,
        smooth: true,
        showSymbol: list.length <= 14,
        symbolSize: 6,
        lineStyle: {
          width: 2.5,
          color: 'var(--accent, #aa3bff)',
        },
        itemStyle: { color: 'var(--accent, #aa3bff)' },
        // 面积图：开启 areaStyle（area 类型要求）
        areaStyle: area
          ? {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(170, 59, 255, 0.35)' },
                  { offset: 1, color: 'rgba(170, 59, 255, 0.02)' },
                ],
              },
            }
          : undefined,
        emphasis: { focus: 'series' as const },
      },
    ],
  }
}

function buildPie(list: ListItem[], title: string): echarts.EChartsCoreOption {
  // 饼图（环形分布）：把 0 分钟的日期剔除，避免占 0 的项干扰饼图
  const nonZero = list.filter((d) => d.minutes > 0)
  const pieData = nonZero.map((d, i) => ({
    name: d.date,
    value: d.minutes,
    itemStyle: { color: colorFor(i) },
  }))

  return {
    title: title
      ? {
          text: title,
          left: 'left',
          textStyle: { fontSize: 14, fontWeight: 600, color: 'var(--text-h, #08060d)' },
        }
      : undefined,
    tooltip: {
      trigger: 'item',
      formatter: (p: any) =>
        `${p.name as string}<br/>专注: ${p.value as number} 分钟<br/>占比: ${
          p.percent as number
        }%`,
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      bottom: 4,
      textStyle: { color: 'var(--text, #6b6375)', fontSize: 11 },
    },
    series: [
      {
        type: 'pie',
        name: '专注分钟',
        // 环形图：内外半径
        radius: ['42%', '68%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 4,
          borderColor: 'var(--bg, #faf8ef)',
          borderWidth: 2,
        },
        label: {
          formatter: '{b}\n{d}%',
          color: 'var(--text-h, #08060d)',
          fontSize: 11,
        },
        labelLine: { length: 8, length2: 6 },
        emphasis: {
          scale: true,
          scaleSize: 6,
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.15)' },
        },
        data: pieData,
      },
    ],
    backgroundColor: 'transparent',
  }
}

/** 核心绘制：基于 props.data + chartType 构建对应 option（需求 9：props 变化自动重绘） */
function render() {
  if (!chartInstance) return
  const list = prepList(props.data)
  let option: echarts.EChartsCoreOption

  switch (type.value) {
    case 'line':
      option = buildLine(list, props.scrollable, props.title, false)
      break
    case 'area':
      option = buildLine(list, props.scrollable, props.title, true)
      break
    case 'pie':
      option = buildPie(list, props.title)
      break
    case 'bar':
    default:
      option = buildBar(list, props.scrollable, props.title)
      break
  }

  // 切换 chartType 时结构差异很大（pie vs cartesian），notMerge:true 防止残留 xAxis/dataZoom/legend
  chartInstance.setOption(option, { notMerge: true, lazyUpdate: false })
}

// 需求 8：图表容器尺寸变化时自动 resize
function onResize() {
  chartInstance?.resize({ animation: { duration: 300 } })
}

function initChart() {
  if (!chartEl.value) return
  chartInstance = echarts.init(chartEl.value, undefined, { renderer: 'canvas' })

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect
        if (width > 0 && height > 0) onResize()
      }
    })
    resizeObserver.observe(chartEl.value)
  }
  window.addEventListener('resize', onResize, { passive: true })

  render()
}

// 需求 9：props 变化后自动重绘（chartType/data/scrollable/title 任一变化都重绘）
watch(
  () => [props.data, props.title, props.scrollable, props.chartType] as const,
  async () => {
    if (chartInstance) {
      await nextTick()
      render()
    }
  },
  { deep: true },
)

watch(hasValidData, async (has) => {
  if (has && chartInstance) {
    await nextTick()
    render()
  }
})

onMounted(async () => {
  await nextTick()
  initChart()
})

// 需求 10：组件卸载时 dispose 图表实例
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<template>
  <div class="stats-chart-wrapper">
    <!-- 需求 11：没有有效数据时显示空状态提示 -->
    <div v-if="!hasValidData" class="empty-state">
      <div class="empty-icon">📊</div>
      <p class="empty-text">暂无专注数据</p>
      <p class="empty-sub">完成番茄钟后这里会展示你的专注时长统计图</p>
    </div>
    <div
      ref="chartEl"
      class="chart-canvas"
      :class="{ hidden: !hasValidData }"
      :style="{ minHeight: '320px' }"
    ></div>
  </div>
</template>

<style scoped>
.stats-chart-wrapper {
  position: relative;
  width: 100%;
  min-height: 320px;
  border: 1px solid var(--border, #e5e4e7);
  border-radius: 12px;
  background: var(--code-bg, #f4f3ec);
  overflow: hidden;
  padding: 12px;
  box-sizing: border-box;
}

.chart-canvas {
  width: 100%;
  height: 360px;
}
.chart-canvas.hidden {
  display: none;
}

.empty-state {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  text-align: center;
}
.empty-icon {
  font-size: 44px;
  opacity: 0.6;
}
.empty-text {
  font-size: 16px;
  color: var(--text-h, #08060d);
  font-weight: 600;
}
.empty-sub {
  font-size: 13px;
  color: var(--text, #6b6375);
  margin: 0;
}
</style>
