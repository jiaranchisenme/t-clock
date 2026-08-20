<script setup lang="ts">
import { useTaskStore } from '../stores/task'
const task = useTaskStore()
</script>

<template>
  <section class="page">
    <h2>学习任务清单</h2>
    <p class="hint">当前任务数：{{ task.list.length }}</p>
  </section>
</template>

<style scoped>
.page { padding: 24px 0; }
.hint { color: var(--text-muted, #6b6b78); font-size: 14px; }
</style>
