<script setup lang="ts">
import { useStatsStore } from '../stores/stats'
const stats = useStatsStore()
</script>

<template>
  <section class="page">
    <h2>数据可视化统计看板</h2>
    <p class="hint">本周专注：{{ stats.weeklyTotal }} 分钟 | 本月专注：{{ stats.monthlyTotal }} 分钟</p>
  </section>
</template>

<style scoped>
.page { padding: 24px 0; }
.hint { color: var(--text-muted, #6b6b78); font-size: 14px; }
</style>
