import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/timer',
    children: [
      { path: 'timer', name: 'timer', component: () => import('../views/TimerView.vue'), meta: { title: '番茄计时' } },
      { path: 'task', name: 'task', component: () => import('../views/TaskView.vue'), meta: { title: '任务清单' } },
      { path: 'checkin', name: 'checkin', component: () => import('../views/CheckinView.vue'), meta: { title: '每日打卡' } },
      { path: 'stats', name: 'stats', component: () => import('../views/StatsView.vue'), meta: { title: '统计看板' } },
    ],
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})
