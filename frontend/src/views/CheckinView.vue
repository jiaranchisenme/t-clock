<script setup lang="ts">
import { onMounted } from 'vue'
import { useCheckinStore } from '../stores/checkin'

const checkin = useCheckinStore()
onMounted(() => {
  checkin.loadToday()
  checkin.loadCalendar()
})
</script>

<template>
  <section class="page">
    <h2>每日学习打卡</h2>
    <p class="hint">今日已打卡：{{ checkin.todayChecked }} | 今日专注：{{ checkin.todayFocusMinutes }} 分钟</p>
    <p class="sub">近 30 天打卡 {{ checkin.calendarDates.length }} 天</p>
  </section>
</template>

<style scoped>
.page { padding: 24px 0; }
.hint, .sub { color: var(--text-muted, #6b6b78); font-size: 14px; }
</style>
