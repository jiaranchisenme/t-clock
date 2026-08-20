<script setup lang="ts">
import { useTimerStore } from '../stores/timer'

// M1 番茄计时页 - 占位骨架，后续阶段填充完整逻辑
const timer = useTimerStore()
</script>

<template>
  <section class="page">
    <h2>番茄专注计时</h2>
    <p class="hint">阶段：{{ timer.phase }} | 运行中：{{ timer.isRunning }}</p>
    <p class="time">
      {{ Math.floor(timer.remainingSeconds / 60) }}:{{ String(timer.remainingSeconds % 60).padStart(2, '0') }}
    </p>
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
.hint {
  color: var(--text-muted, #6b6b78);
  font-size: 14px;
}
</style>
