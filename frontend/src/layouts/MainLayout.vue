<script setup lang="ts">
import { ref, onBeforeUnmount, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

// 顶部导航 + 内容区 Flex 布局
const navItems = [
  { to: '/timer',   label: '番茄计时', icon: '🍅' },
  { to: '/task',    label: '任务清单', icon: '📝' },
  { to: '/checkin', label: '每日打卡', icon: '📅' },
  { to: '/stats',   label: '统计看板', icon: '📊' },
] as const

// 移动端抽屉开关
const drawerOpen = ref(false)
const route = useRoute()

function toggleDrawer() {
  drawerOpen.value = !drawerOpen.value
  // 锁 body 滚动（打开时）
  if (drawerOpen.value) document.body.style.overflow = 'hidden'
  else document.body.style.overflow = ''
}
function closeDrawer() {
  drawerOpen.value = false
  document.body.style.overflow = ''
}

// 路由变化时自动关抽屉（移动端点了链接）
let unwatch: (() => void) | null = null
onMounted(() => {
  // 路由跳转即关
  unwatch = (() => {
    let last = route.path
    const id = window.setInterval(() => {
      if (route.path !== last) {
        last = route.path
        closeDrawer()
      }
    }, 120)
    return () => window.clearInterval(id)
  }) as unknown as () => void
  // 窗口 >=768 时抽屉强制关
  const onResize = () => {
    if (window.innerWidth >= 768 && drawerOpen.value) closeDrawer()
  }
  window.addEventListener('resize', onResize, { passive: true })
  ;(window as any).__layoutResizeOff = () => window.removeEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  unwatch?.()
  ;(window as any).__layoutResizeOff?.()
  document.body.style.overflow = ''
})
</script>

<template>
  <div class="layout">
    <!-- ================= 顶栏（响应式） ================= -->
    <header class="app-header">
      <div class="header-inner">
        <!-- 品牌区：左对齐，移动端也有 -->
        <RouterLink to="/timer" class="brand" @click="closeDrawer">
          <span class="brand-mark">🍅</span>
          <span class="brand-name">Focusly</span>
          <span class="brand-slogan">轻番茄 · 专注每一刻</span>
        </RouterLink>

        <!-- 桌面导航（≥768px 显示） -->
        <nav class="nav-desktop" aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="nav-link"
          >
            <span class="icon">{{ item.icon }}</span>
            <span class="label">{{ item.label }}</span>
          </RouterLink>
        </nav>

        <!-- 移动端汉堡按钮（<768px 显示） -->
        <button
          class="hamburger"
          :class="{ open: drawerOpen }"
          aria-label="打开菜单"
          :aria-expanded="drawerOpen"
          @click="toggleDrawer"
        >
          <span></span><span></span><span></span>
        </button>
      </div>
    </header>

    <!-- 内容区：max-width 居中，两侧留白更像学生笔记本 -->
    <main class="app-main">
      <div class="main-inner">
        <RouterView v-slot="{ Component }">
          <transition name="fade-page" mode="out-in">
            <component :is="Component" />
          </transition>
        </RouterView>
      </div>
    </main>

    <!-- 页脚：极简一句 -->
    <footer class="app-footer">
      <span>© Focusly · 陪你一步步成长</span>
    </footer>

    <!-- ================= 移动端抽屉遮罩+抽屉 ================= -->
    <transition name="fade">
      <div
        v-if="drawerOpen"
        class="drawer-mask"
        @click.self="closeDrawer"
      ></div>
    </transition>
    <aside
      class="drawer"
      :class="{ open: drawerOpen }"
      role="dialog"
      aria-label="菜单"
    >
      <div class="drawer-brand">
        <span class="brand-mark">🍅</span>
        <span class="brand-name">Focusly</span>
      </div>
      <nav class="drawer-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="drawer-link"
          @click="closeDrawer"
        >
          <span class="icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="drawer-footer">
        <span class="tag leaf">学生版</span>
        <span class="tag muted">v1.0</span>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  background: var(--bg);
}

/* ============ 顶栏 ============ */
.app-header {
  position: sticky;
  top: 0;
  z-index: 30;
  background: color-mix(in srgb, var(--surface) 80%, transparent);
  backdrop-filter: saturate(180%) blur(12px);
  -webkit-backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--border-soft);
}
.header-inner {
  max-width: 1120px;
  margin: 0 auto;
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
@media (max-width: 640px) {
  .header-inner { height: 54px; padding: 0 14px; }
}

.brand {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  text-decoration: none;
  color: inherit;
}
.brand-mark { font-size: 20px; transform: translateY(2px); }
.brand-name {
  font-weight: 800;
  color: var(--text-h);
  font-size: 18px;
  letter-spacing: -0.01em;
}
.brand-slogan {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 0.02em;
}
@media (max-width: 640px) {
  .brand-slogan { display: none; }
}

/* 桌面水平导航 */
.nav-desktop {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--r-pill);
}
.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: var(--r-pill);
  color: var(--text);
  text-decoration: none;
  font-size: 13.5px;
  transition: all 0.18s ease;
}
.nav-link .icon { font-size: 14px; }
.nav-link:hover {
  color: var(--text-h);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}
.nav-link.router-link-active {
  background: var(--surface);
  color: var(--brand);
  font-weight: 600;
  box-shadow: var(--shadow-soft);
}

/* 汉堡按钮（移动端） */
.hamburger {
  display: none;
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: var(--r-sm);
  border: 1px solid var(--border);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.hamburger span {
  display: block;
  width: 18px;
  height: 2px;
  border-radius: 2px;
  background: var(--text-h);
  transition: transform 0.2s, opacity 0.2s;
}
.hamburger.open span:nth-child(1) { transform: translateY(6px) rotate(45deg); }
.hamburger.open span:nth-child(2) { opacity: 0; }
.hamburger.open span:nth-child(3) { transform: translateY(-6px) rotate(-45deg); }
@media (max-width: 767px) {
  .hamburger { display: inline-flex; }
  .nav-desktop { display: none; }
}

/* ============ 主体：max-width 居中（学生笔记本宽度） ============ */
.app-main {
  flex: 1;
  width: 100%;
}
.main-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 20px 20px 40px;
}
@media (max-width: 640px) {
  .main-inner { padding: 14px 14px 32px; }
}

/* 页面切换淡入淡出 */
.fade-page-enter-active,
.fade-page-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.fade-page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ============ 页脚 ============ */
.app-footer {
  border-top: 1px solid var(--border-soft);
  padding: 14px 20px 24px;
  color: var(--text-muted);
  font-size: 12px;
  text-align: center;
}

/* ============ 抽屉（移动端） ============ */
.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  backdrop-filter: blur(2px);
  z-index: 60;
}
.drawer {
  position: fixed;
  top: 0;
  right: 0;
  width: min(80vw, 300px);
  height: 100dvh;
  background: var(--surface);
  border-left: 1px solid var(--border);
  box-shadow: -10px 0 30px -10px rgba(15, 23, 42, 0.12);
  transform: translateX(100%);
  transition: transform 0.26s cubic-bezier(0.3, 0.9, 0.3, 1);
  display: flex;
  flex-direction: column;
  z-index: 61;
}
.drawer.open { transform: translateX(0); }
.drawer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 20px 14px;
  border-bottom: 1px dashed var(--divider);
}
.drawer-nav {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}
.drawer-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--r-md);
  text-decoration: none;
  color: var(--text-h);
  font-size: 15px;
  font-weight: 500;
  transition: all 0.15s ease;
}
.drawer-link .icon { font-size: 18px; }
.drawer-link:hover { background: var(--surface-muted); }
.drawer-link.router-link-active {
  background: var(--brand-50);
  color: var(--brand);
  font-weight: 600;
}
.drawer-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px 20px;
  border-top: 1px dashed var(--divider);
}

/* 抽屉/遮罩淡入淡出 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
