# Focusly · 前端文档

> Vue 3 + TypeScript + Vite 8 + Pinia + ECharts 6 · 极简清新学生风 SPA

---

## 📦 技术栈一览（精确版本）

| 分类 | 包名 | 版本 | 用途 |
|---|---|---|---|
| **核心框架** | `vue` | 3.5.40 | 组合式 API + `<script setup>` |
| 路由 | `vue-router` | 4.6.4 | History 模式，懒加载路由 |
| 状态 | `pinia` | 4.0.3 | 4 个业务 store |
| 网络 | `axios` | 1.19.0 | 请求/响应拦截，自动剥壳 |
| 图表 | `echarts` | 6.1.0 | 按需注册（Bar / Line / Pie） |
| **构建** | `vite` | 8.2.0 | Rolldown 引擎，~650ms 热启动 |
| TS | `typescript` | 6.0.2 | strict=true |
| 类型检查 | `vue-tsc` | 3.3.8 | SFC 类型化 build 前置 |
| node 类型 | `@types/node` | 24.13.3 | Vite config 可用 `path` / `__dirname` |
| vue 插件 | `@vitejs/plugin-vue` | 6.0.8 | SFC 编译 |

```bash
# 验证所有依赖已安装
cd frontend && node -e "console.log(Object.keys(require('./package.json').dependencies).join('、'))"
# 预期：axios、echarts、pinia、vue、vue-router
```

---

## 🏗️ 前端架构

```
src/
├── main.ts                 ← createApp() → 注册 Pinia + Router，挂载 #app
├── App.vue                 ← 纯 <RouterView />（外壳交给 MainLayout）
├── style.css               ← 设计系统 Tokens + 全局重置 + 通用类（见下文）
│
├── router/index.ts         ← 4 个懒加载路由 + MetaLayout 重定向 / → /timer
│
├── layouts/MainLayout.vue  ← 顶部胶囊导航 + <768px 汉堡菜单抽屉（路由变化自动关）
│
├── views/                  ← 页面层（业务 View）
│   ├── TimerView.vue       ← 番茄计时页：useCountdown + 设置弹窗 + 完成弹窗
│   ├── TaskView.vue        ← 任务清单：CRUD / 设为当前专注 / 清空确认
│   ├── CheckinView.vue     ← 打卡页：今日状态 / 最近 14 天日历 / 连续天数
│   └── StatsView.vue       ← 看板：近 7/30 天切换 × 4 种图表类型切换
│
├── components/
│   └── StatsChart.vue      ← ECharts 封装：props 驱动 + 自动 resize + 空态 + 按需注册
│
├── composables/
│   └── useCountdown.ts     ← 无漂移倒计时核心（绝对时间戳算法）
│
├── stores/                 ← Pinia 业务状态
│   ├── timer.ts            ← 配置缓存（work/break 分钟）
│   ├── task.ts             ← 任务列表 + 当前专注 taskId
│   ├── checkin.ts          ← 打卡日期集合 + 今日状态 + 连续天数
│   └── stats.ts            ← 周/月统计数据缓存 + chartType 选择
│
└── api/                    ← 与后端 Controller 1:1 对应
    ├── http.ts             ← axios 实例（baseURL=/api、拦截器剥壳/拼错）
    ├── timer.ts            ← GET/PUT /api/timer/config
    ├── task.ts             ← CRUD /clearAll /api/tasks
    ├── session.ts          ← POST 保存、GET 列表（支持 startDate/endDate）
    ├── checkin.ts          ← POST 打卡、GET /today /dates /streak
    └── stats.ts            ← GET /week /month 失败时降级调用 session.list 本地聚合
```

---

## 🎨 设计系统 Tokens（CSS Variables）

> 所有组件**必须通过 var() 引用**，禁止硬编码颜色/圆角，确保全站主题一致。

定义位置：[src/style.css](src/style.css)

### 品牌色（学生风 · 低饱和，长时间使用不刺眼）

| Token | 色值 | 用途 |
|---|---|---|
| `--brand` | `#3b82f6` | 晴空蓝主色（按钮/链接/主强调） |
| `--brand-600` | `#2563eb` | Hover/Active 深色 |
| `--brand-50` / `100` | `#eff6ff` / `#dbeafe` | 背景/描边浅底 |
| `--leaf` | `#22c55e` | 嫩绿：完成态 / 健康标签 |
| `--petal` | `#ec4899` | 柔粉：打卡 / 热情标记 |
| `--amber` | `#f59e0b` | 暖琥珀：休息模式 / 提醒 |
| `--lilac` | `#8b5cf6` | 浅紫：次要强调 |

### 中性色基底（暖米白）

| Token | 色值 | 用途 |
|---|---|---|
| `--bg` | `#fefbf4` | 页面背景（暖米白，非纯白更护眼） |
| `--surface` | `#ffffff` | 卡片面 |
| `--surface-muted` | `#f7f3eb` | 卡片灰（选中/禁用/分组底） |
| `--border` | `#ece6d9` | 边框：浅暖灰，无冷硬边 |
| `--text-h` | `#1f2937` | 标题（深暖灰，非纯黑） |
| `--text` | `#64748b` | 正文（暖青灰） |
| `--text-muted` | `#94a3b8` | 辅助文字 |

### 圆角梯度（胶囊优先）

| Token | 值 | 典型应用 |
|---|---|---|
| `--r-pill` | `999px` | 按钮 / Tag / Tab（胶囊形） |
| `--r-lg` | `18px` | 卡片 |
| `--r-md` | `14px` | 弹窗 / 抽屉面板 |
| `--r-sm` | `10px` | 输入框 / 小组件 |

### 阴影（极简、单层）

```css
--shadow:      0 10px 24px -12px rgba(15,23,42,.12), 0 4px 10px -4px rgba(15,23,42,.06);  /* 卡片悬浮 */
--shadow-soft: 0 2px 8px -2px rgba(15,23,42,.06);                                            /* 输入/内联 */
```

### 字体

```css
--sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', Roboto, sans-serif;
--mono: ui-monospace, 'JetBrains Mono', Consolas, monospace;   /* 倒计时数字用等宽，避免 MM:SS 左右跳动 */
```

### 深色模式（可选跟随系统）

通过 `@media (prefers-color-scheme: dark)` 自动切换，暖米白 → 深暖灰 `#1a1c22`，品牌色保留但降低对比（避免夜间刺眼）。

---

## 📱 响应式断点

| 断点 | 设备 | 关键行为 |
|---|---|---|
| `≥1024px` | 桌面 / 平板横屏 | 标准字号 15px，胶囊导航水平显示，StatsView 两列卡片 |
| `768px ~ 1024px` | 平板竖屏 | 字号 15px，h1 缩小到 28px，StatsView 变单列 |
| `<768px` | **移动端（关键断点）** | 顶部导航 → 汉堡按钮 + 右侧抽屉；TimerView 表盘使用 `clamp(240px,72vw,360px)` 自适应；设置/完成弹窗 → 从底部滑出 BottomSheet；按钮 flex-wrap |
| `<640px` | 小屏手机 | 字号 14px，h1 24px，卡片 padding 16px |
| `<520px` | 超小屏 | CheckinView 日历强制 7 列 `grid-template-columns: repeat(7, minmax(0, 1fr))` |

**设计哲学**：移动优先（mobile-first）。所有 View 的 CSS 默认按窄屏写，`@media (min-width: 768px)` 内写桌面增强。

---

## 🧩 组件 API

### 1. `StatsChart.vue` — 图表通用组件（唯一跨页复用组件）

```vue
<script setup lang="ts">
import StatsChart, { type ChartType } from '../components/StatsChart.vue'
</script>

<template>
  <StatsChart
    :data="weekData"                 <!-- DayStat[]，必填 -->
    title="近 7 天专注时长"           <!-- 可选标题 -->
    :scrollable="false"              <!-- bar/line 是否显示 x 轴 dataZoom -->
    chart-type="bar"                 <!-- bar | line | area | pie -->
  />
</template>
```

| Prop | 类型 | 默认 | 说明 |
|---|---|---|---|
| `data` | `DayStat[]`（`{ date: string; minutes: number }[]`） | —— 必填 | 空数组或全 0 → 自动显示空态插图 |
| `title` | `string` | `''` | 图表上方标题（饼图会自动隐藏） |
| `scrollable` | `boolean` | `false` | 30 天数据建议设 true，出现 x 轴滚动条 |
| `chartType` | `'bar' \| 'line' \| 'area' \| 'pie'` | `'bar'` | 非法值回退 bar，切换时 `setOption(..., { notMerge: true })` 彻底替换避免残留 |

**内部能力**：
- ECharts 按需注册：`BarChart + LineChart + PieChart + Grid/Tooltip/Title/DataZoom/Legend + CanvasRenderer`（比 `echarts` 整包省 60%+）
- `ResizeObserver` 监听容器尺寸，自动 `chart.resize()`（含 ResizeObserver 在 100ms 内抖动 debounce）
- `watch(data, deep+immediate)` + `watch(chartType)` 自动重绘
- `onBeforeUnmount` 必调 `chart.dispose()` 防内存泄漏
- 饼图分片用稳定调色板数组（按索引取，不随机，保证切换类型颜色一致）

### 2. `useCountdown()` — 无漂移倒计时 composable

```ts
const { mode, running, completed, remaining, progress, display,
        start, pause, reset, switchMode, applyConfig } = useCountdown({
  studySeconds: 25 * 60,
  restSeconds:  5 * 60,
  onComplete(finishedMode) {
    // 只有完整走完（从 running 走到 0）才触发；
    // 中途 reset / 暂停 / 手动切模式不会触发。
    // 参数 finishedMode = 'study' | 'rest'
  }
})
```

**对外状态**（均为 `Ref`）：

| 状态 | 类型 | 说明 |
|---|---|---|
| `mode` | `'study' \| 'rest'` | 当前模式 |
| `running` | `boolean` | 计时中 |
| `completed` | `boolean` | 阶段完整结束，弹窗期间为 true，此时 `start()` 无动作 |
| `remaining` | `number` | 剩余秒数 |
| `progress` | `0 ~ 1` | 表盘百分比（computed） |
| `display` | `'MM:SS'` | 格式化字符串，等宽字体不跳动（computed） |

**对外方法**：

| 方法 | 行为 |
|---|---|
| `start()` | 运行中 / 完成态 → 忽略；否则重建 `endTime = Date.now() + remaining*1000`，启 250ms interval |
| `pause()` | 清 interval，快照 `remaining`（下次 start 据此重建 endTime，不丢失） |
| `reset(targetMode='study')` | 清 interval，`completed=false`，回到 targetMode 初始秒数 |
| `switchMode(target)` | 切换模式，清完成态，不自动开始 |
| `applyConfig(newStudy, newRest)` | 应用新配置；当前运行中会被 stop，回到 study 初始态 |

> **防漂移原理**：不做 `remaining -= interval_ms`（后台 tab setInterval 被节流到 1Hz 甚至更低，累计会飘）。而是用 **绝对时间戳 endTime**，每 tick 都 `remaining = max(0, round((endTime - Date.now())/1000))`，切后台再回来下一帧立刻自愈。

### 3. Pinia Stores

| Store | 关键状态 | 动作 |
|---|---|---|
| `timer` | `workDuration` / `breakDuration` | `fetchConfig()` / `updateConfig()` 写 API 并更新本地 |
| `task` | `tasks: Task[]` / `currentTaskId` | `load/create/update/delete/clearAll` / `setCurrent(id)` |
| `checkin` | `todayCheckedIn` / `todayMinutes` / `dates: Set<string>` / `streak` | `doCheckin()` / `refreshToday()` / `loadDates(days)` |
| `stats` | `weekData` / `monthData` / `chartType: ChartType` | `loadWeek()` / `loadMonth()` / `setChartType(type)` |

---

## 🔌 axios 约定（`api/http.ts`）

**请求**：`baseURL = '/api'`，Vite 开发代理到 `http://localhost:8080`，生产 nginx 同路径转发。

**响应**：统一返回壳 `{ code: number, msg: string, data: T }`：
- 响应拦截器自动剥壳 → View / Store 层直接 `const data = await http.get<Task[]>('/tasks')` 拿到 `Task[]`
- `code !== 0` → `Promise.reject(new Error(msg))`，View 层 `try/catch` 弹 toast

**错误拦截增强（P1 已修复）**：HTTP 层错误尽量把所有可定位信息塞到 Error message，格式：

```
HTTP <status> <statusText> - 后端：<resp.msg> - (<url>)
[<axios_err_code>] - (<url>)        # 连不上时，如 [ERR_NETWORK] / [ECONNABORTED]
```

典型错误：
```
HTTP 500 Internal Server Error - 后端：SQL 语法错误 - (/stats/week)
[ERR_NETWORK] - (/tasks)
HTTP 409 Conflict - 后端：数据重复，今日已打卡 - (/checkins)
```

---

## 🛣️ 路由

```
/            → 重定向 → /timer
/timer       → TimerView       （meta.title: 番茄计时）
/task        → TaskView        （任务清单）
/checkin     → CheckinView     （每日打卡）
/stats       → StatsView       （统计看板）
```

全部为懒加载：`() => import('../views/XxxView.vue')`，首屏仅下载 Timer chunk。

> 生产部署 History Fallback：nginx 需加 `try_files $uri $uri/ /index.html;`（根 README Q4 有模板）。

---

## 🧪 开发脚本

```bash
cd frontend

npm install          # 首次
npm run dev          # :5173 开发（Vite HMR）
npm run build        # 先 vue-tsc -b 类型检查，再 vite build → dist/
npm run preview      # :4173 本地预览构建产物（验证 Tree-shaking / chunk 合理）
```

---

## ❓ 前端常见问题

<details>
<summary>Q1. vue-tsc 报错 "Cannot find module '@/xxx'" 怎么办？</summary>

检查 `vite.config.ts` 的 `resolve.alias` 配了 `'@' → fileURLToPath(new URL('./src', import.meta.url))`，同时 `tsconfig.app.json` 的 `compilerOptions.paths` 也要对应：

```json
{ "paths": { "@/*": ["./src/*"] } }
```

</details>

<details>
<summary>Q2. ECharts 切换 bar → pie 之后再切回 bar，xAxis 还残留着？</summary>

**已修复**：所有 `setOption` 调用均传第二参数 `{ notMerge: true }`，彻底替换配置。若仍有问题，调用 `chart.clear()` 再 `setOption()`。

</details>

<details>
<summary>Q3. 移动端 MainLayout 的汉堡抽屉，点路由后不会自动关？</summary>

MainLayout 已 `watch(() => route.path, () => drawer = false)`。如果自己加了新路由但没触发，检查是否 `router.push` 后 path 确实变化。

</details>

<details>
<summary>Q4. TimerView 倒计时的 MM:SS 数字左右跳动？</summary>

`.counter` 类已用 `font-family: var(--mono)` 等宽字体。如果自定义了计时器样式，记得保持等宽。

</details>

<details>
<summary>Q5. 开发环境跨域？Vite 代理不生效？</summary>

前端请求必须相对路径 `/api/tasks`（**不是** `http://localhost:8080/api/tasks`），这样才走 Vite 代理。查看 `vite.config.ts`：

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

</details>

<details>
<summary>Q6. build 后 chunk 太大？StatsChart 的 ECharts 占了主要体积？</summary>

根 README Q6 已给出方案：把 StatsChart 改为异步组件，首次进入 `/stats` 才下载 ECharts 代码（Timer/Task/Checkin 首屏更快）。

```ts
const StatsChart = defineAsyncComponent(() => import('../components/StatsChart.vue'))
```

</details>
