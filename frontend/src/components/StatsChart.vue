<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed, nextTick } from 'vue'
import * as echarts from 'echarts/core'
// 需求 12：只按需注册使用到的 ECharts 模块
import { BarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  TitleComponent,
  DataZoomComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { DayStat } from '../api/stats'

echarts.use([
  BarChart,
  GridComponent,
  TooltipComponent,
  TitleComponent,
  DataZoomComponent,
  CanvasRenderer,
])

// ----- props：StatsChart 只通过 props 接收 data（需求 7）-----
interface Props {
  data: readonly DayStat[]
  title?: string
  /** 是否需要显示 x 轴滚动条（当 30 天且窗口较小时） */
  scrollable?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  title: '',
  scrollable: false,
})

// 需求 11：无有效数据的判定（所有分钟数都为 0 或 data 为空）
const hasValidData = computed(() =>
  Array.isArray(props.data) && props.data.some((d) => Number.isFinite(d.minutes) && d.minutes > 0),
)

const chartEl = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

/** 核心绘制：基于 props data 构建 option（需求 9：props 变化自动重绘） */
function render() {
  if (!chartInstance) return
  const list = Array.isArray(props.data) ? props.data : []
  const dates = list.map((d) => d.date)
  const values = list.map((d) => (Number.isFinite(d.minutes) ? d.minutes : 0))

  const option: echarts.EChartsCoreOption = {
    title: props.title
      ? {
          text: props.title,
          left: 'left',
          textStyle: { fontSize: 14, fontWeight: 600, color: 'var(--text-h, #08060d)' },
        }
      : undefined,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v: unknown) => `${v as number} 分钟`,
    },
    grid: { left: 40, right: 20, top: props.title ? 40 : 20, bottom: props.scrollable ? 50 : 40 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        color: 'var(--text, #6b6375)',
        fontSize: 11,
        rotate: list.length > 14 ? 45 : 0,
      },
      axisLine: { lineStyle: { color: 'var(--border, #e5e4e7)' } },
    },
    yAxis: {
      type: 'value',
      name: '分钟',
      nameTextStyle: { color: 'var(--text, #6b6375)' },
      axisLabel: { color: 'var(--text, #6b6375)', fontSize: 11 },
      splitLine: { lineStyle: { color: 'var(--border, #e5e4e7)', type: 'dashed' } },
    },
    dataZoom: props.scrollable
      ? [
          {
            type: 'inside',
            start: 0,
            end: 100,
            zoomOnMouseWheel: false,
            moveOnMouseWheel: true,
            moveOnMouseMove: false,
          },
          {
            type: 'slider',
            height: 18,
            bottom: 8,
            borderColor: 'var(--border, #e5e4e7)',
            textStyle: { color: 'var(--text, #6b6375)' },
          },
        ]
      : undefined,
    series: [
      {
        type: 'bar',
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
    backgroundColor: 'transparent',
  }

  chartInstance.setOption(option, { notMerge: true, lazyUpdate: false })
}

// 需求 8：图表容器尺寸变化时自动 resize
function onResize() {
  chartInstance?.resize({ animation: { duration: 300 } })
}

function initChart() {
  if (!chartEl.value) return
  chartInstance = echarts.init(chartEl.value, undefined, { renderer: 'canvas' })

  // ResizeObserver：监听容器 DOM 尺寸变化（比 window.resize 更精确）
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect
        if (width > 0 && height > 0) onResize()
      }
    })
    resizeObserver.observe(chartEl.value)
  }
  // 兜底：窗口 resize 也触发
  window.addEventListener('resize', onResize, { passive: true })

  render()
}

// 需求 9：props data 变化后自动重绘（先等 DOM 完成）
watch(
  () => [props.data, props.title, props.scrollable] as const,
  async () => {
    if (chartInstance) {
      await nextTick()
      render()
    }
  },
  { deep: true },
)

// 需求 11：hasValidData 切换到空状态（或从空状态回来），也需要重绘/清理
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
      <p class="empty-sub">完成番茄钟后这里会展示你的专注时长柱状图</p>
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
