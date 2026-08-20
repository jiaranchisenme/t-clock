<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useTimerStore } from '../stores/timer'

const timer = useTimerStore()

// 主页面稳定性：onMounted 拉取配置，异常静默回退默认值
onMounted(() => {
  timer.loadConfig()
})

// 卸载必清定时器，避免后台叠加泄漏
onUnmounted(() => {
  timer.clearTimer()
})
</script>

<template>
  <section class="page">
    <h2>番茄专注计时</h2>
    <p class="hint">阶段：{{ timer.phase }} | 运行中：{{ timer.isRunning }}</p>
    <p class="time">
      {{ Math.floor(timer.remainingSeconds / 60) }}:{{ String(timer.remainingSeconds % 60).padStart(2, '0') }}
    </p>
    <p class="sub">学习 {{ timer.workDuration }} 分钟 / 休息 {{ timer.breakDuration }} 分钟</p>
  </section>
</template>

<style scoped>
.page {
  text-align: center;
  padding: 48px 0;
}
.time {
  font-size: 64px;
  font-weight: 700;
  margin-top: 16px;
  letter-spacing: 4px;
}
.hint, .sub {
  color: var(--text-muted, #6b6b78);
  font-size: 14px;
}
</style>
