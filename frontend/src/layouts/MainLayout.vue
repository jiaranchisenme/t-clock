<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'

// 顶部导航 + 内容区 Flex 布局
const navItems = [
  { to: '/timer', label: '番茄计时' },
  { to: '/task', label: '任务清单' },
  { to: '/checkin', label: '每日打卡' },
  { to: '/stats', label: '统计看板' },
]
</script>

<template>
  <div class="layout">
    <header class="header">
      <div class="brand">Focusly</div>
      <nav class="nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="nav-link">
          {{ item.label }}
        </RouterLink>
      </nav>
    </header>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 24px;
  height: 56px;
  border-bottom: 1px solid var(--border, #e2e2e8);
}
.brand {
  font-weight: 700;
  font-size: 18px;
  color: var(--brand, #6c4bd6);
}
.nav {
  display: flex;
  gap: 16px;
}
.nav-link {
  padding: 6px 12px;
  border-radius: 999px;
  color: inherit;
  text-decoration: none;
  font-size: 14px;
}
.nav-link.router-link-active {
  background: var(--brand-soft, #efe9fb);
  color: var(--brand, #6c4bd6);
}
.content {
  flex: 1;
  padding: 24px;
}
</style>
