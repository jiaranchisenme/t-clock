<script setup lang="ts">
import { onMounted } from 'vue'
import { useStatsStore } from '../stores/stats'

const stats = useStatsStore()
onMounted(() => stats.load())
</script>

<template>
  <section class="page">
    <h2>数据可视化统计看板</h2>
    <p class="hint">本周专注：{{ stats.weeklyTotal }} 分钟 | 本月专注：{{ stats.monthlyTotal }} 分钟</p>
    <p class="sub">近 7 天数据点 {{ stats.daily7.length }} 个 / 近 30 天数据点 {{ stats.daily30.length }} 个</p>
  </section>
</template>

<style scoped>
.page { padding: 24px 0; }
.hint, .sub { color: var(--text-muted, #6b6b78); font-size: 14px; }
</style>
