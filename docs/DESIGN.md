# Focusly · 设计文档

> 需求落地映射 · 写安全策略 · 防漂移算法 · 降级策略 · 图表类型映射

本文档是「代码为什么这么写」的说明。面向架构师、代码 Reviewer、后续接手迭代的开发者。

---

## 📋 需求 ↔ 代码 对照矩阵（PRD 第 2-4 部分 → 实现落点）

| PRD 需求 | 关键说明 | 实现文件（前端 + 后端 + DB） |
|---|---|---|
| **2.1 番茄计时** study/rest 双模式 | 切换不自动开始；完整结束不 auto next | `useCountdown.switchMode()` + `onComplete()` 只改状态不自动 start |
| **2.2 MM:SS 倒计时** | 后台 tab 时间不漂移 | `useCountdown.ts` 绝对时间戳算法（见 §2） |
| **2.3 圆形表盘进度** | `0~1 progress * 2π` → SVG stroke-dasharray | `TimerView.vue` 内联 SVG circle，stroke 品牌色 |
| **2.4 设置弹窗 + 范围校验** | 专注 1~180 / 休息 1~60；空值/负数/超范围提示 | TimerView `validateForm()` + `TimerConfigRequest` `@Min@Max` 双保险 |
| **2.5 完整完成 study 才产生 session** | reset/暂停/手动切换不产生 | `useCountdown.onComplete('study')` 触发处 POST；非 study 只落库 BREAK 但不加总 |
| **2.6 完成弹窗不使用 alert** | 自定义 Modal 组件 | TimerView `FinishModal` + 顶部 mask + 居中卡片；休息完成时同 |
| **3.1 任务 CRUD** | 禁止空任务；完成置灰+删除线；删除/清空确认 | TaskView UI 层 + `TaskCreateRequest` + TaskService 再校验 |
| **3.2 设为当前专注任务** | 当前 taskId 写入完成的 session | `task.currentTaskId` → POST `/sessions` body 里 `taskId` 字段 |
| **4.1 每日打卡显式点击** | ≠"学过即打卡" | CheckinService 不做自动 INSERT（已修复早期 session 自动触发 bug） |
| **4.2 今日累计专注分钟数** | session 聚合 | `/checkins/today` Service 层 SUM(WORK) + 打卡快照组合 |
| **4.3 同一天只能打卡一次** | 三层兜底 | 前端禁用 → Service 先 SELECT → DB `UNIQUE uk_checkin_date`（见 §3） |
| **4.4 最近 14 天可视化** | 已打卡高亮 | CheckinView 14 格 grid + `/dates?days=14` 结果 `Set.has()` |
| **4.5 连续天数 · 今天未打卡允许从昨天起** | 避免白天 streak=0 打击用户 | CheckinService.getStreak() 起始点分支（见 §3） |
| **5.1 7 / 30 天聚合** | 缺日补 0 / 旧→新排序 | StatsService.aggregateLast(n) 日历左连（见 §5） |
| **5.2 接口失败降级** | 本地 sessions 聚合 | `stats.ts` store `loadWeek()` try 接口 / catch 调 `session.list` 本地 reduce |
| **5.3 图表容器 resize 自适应** | ResizeObserver | StatsChart 实例 onMounted 注册；unmount 必 dispose |
| **5.4 props 驱动 / 卸载 dispose** | 避免内存泄漏 | StatsChart `watch(data, notMerge setOption)`；`onBeforeUnmount → dispose()` |
| **5.5 4 种图表类型**（需求追加） | bar / line / area / pie | StatsChart `chartType` prop；每种独立 buildOption；`notMerge:true` 彻底替换 |
| **UI 响应式**（需求追加） | 极简学生风 / <768px 抽屉 | style.css tokens；MainLayout 断点；各 View media query |
| **P0 修复** | logic-delete 移除；TimerConfig 空表兜底 | application.yml 注释三行；TimerConfigService.getConfig() if-null new 对象 |
| **P1 修复** | index.html 元信息；axios 错误拼接 | index.html lang + theme + SEO；http.ts 错误拦截器 HTTP+后端 msg+URL 三段 |

---

## ⏱️ 1. 防漂移倒计时算法（useCountdown.ts 核心设计）

### 问题背景

浏览器后台标签页会**节流 `setInterval`**（Chrome 对不可见 tab 限制到 1Hz 甚至更慢，Firefox 也是类似）。如果用朴素算法：

```
❌ 反例（会漂移）：
remaining = 1500
setInterval(() => remaining -= 1, 1000)   // 后台 tab 实际 1 次/秒 变成 1 次/好几秒
```

用户挂后台去查资料，再切回来「25 分钟的番茄只走了 5 分钟」——完全不可信。

### 正确算法（绝对时间戳）

```
┌────────────────────────────────────────────────────────────┐
│  start() 被调用时：                                          │
│    1. 根据当前模式算出总秒数 total                            │
│    2. 写 endTime = Date.now() + total * 1000   ← 锚点       │
│    3. 启动 interval 250ms tick                              │
│                                                            │
│  tick() 每 250ms（无需精准）：                                │
│    remaining = max(0, round((endTime - Date.now()) / 1000)) │
│    if remaining <= 0: finishCurrentPhase()                  │
└────────────────────────────────────────────────────────────┘
```

**核心洞察**：`Date.now()` 是**系统单调时钟**（浏览器节流影响不到它），锚点 endTime 一旦固定，未来任何时刻剩余时间都是「锚点减当前」——切后台再回来下一帧立即自愈，**漂移 = 0**。

### 暂停怎么实现？

暂停时需要「快照剩余时间」否则 endTime 就过期了：

```
pause():
  1. clearInterval()
  2. remaining = max(0, round((endTime - now) / 1000))   ← 快照

继续 start():
  3. endTime = Date.now() + remaining * 1000              ← 用快照重建锚点
  4. 启 interval
```

### 防 setInterval 叠加

`start()` 开头先 `if (intervalId != null) clearInterval(intervalId)`。无论任何原因重复调用 start，只保留最后一个 interval。加 `onUnmounted` 兜底清。

### 阶段完整结束的「完成态」

`remaining` 碰到 0 → `finishCurrentPhase()`：
- `stop()`（清 interval）
- `completed = true`（进入完成态，禁止再 `start()`，必须先 `reset()` 或 `switchMode()` 清除 completed）
- `onComplete(finishedMode)`（上层才发 POST session / 弹完成 Modal）

这保证了「只有完整走完的才产生记录」（中途 reset / pause / 手动切模式 → 都不会走 finishCurrentPhase）。

---

## 🛡️ 2. 写安全四层级 + 三层打卡兜底

### 通用写安全四层级（任何写接口都套一遍）

```
层 1 · DTO 字段注解 (@Valid)          ← 最轻，捕获 80% 明显非法输入（空/范围/超长）
层 2 · Service 业务规则 if/throw       ← 语义规则（空标题、状态非 0/1、今日已打卡…）
层 3 · DB DEFAULT / NOT NULL           ← 即使 DTO + Service 漏拦，DB 仍然拒绝脏数据（NULL）
层 4 · DB UNIQUE / FK 约束             ← 并发幂等（高并发双写 / 前端被绕过）
```

### 打卡写热点的「三层防重复」（层 1 + 层 2 + 层 4）

```
前端层 1（UI）: todayCheckedIn === true → button.disabled = true
                ↓  正常用户点击不到了
Service 层 2（查重）: doCheckin() 第一句：
               if (lambdaQuery().eq(checkin_date, today).exists())
                   throw IllegalStateException("今日已打卡")   → code=409
                ↓  正常到这里已拦住
DB 层 3（UNIQUE 兜底）: 万一有人绕过前两层并发双写 →
               INSERT 触发 1062 Duplicate entry on uk_checkin_date
               → DuplicateKeyException → GlobalExceptionHandler
               → code=409 同样 "今日已打卡"
```

三层同时生效，**对前端表现完全一样**（code=409），但防御深度逐层高。

---

## 📅 3. 打卡语义 & 连续天数算法

### 3.1 「显式打卡」≠ 「学过即打卡」（已修复 Bug 说明）

早期版本错误：SessionService 在 WORK 完成后 `INSERT daily_checkin ... ON DUPLICATE KEY UPDATE`——用户只要专注完 25 分钟就自动算今天打卡了。与 PRD 语义不符。

**修复后**：`daily_checkin` 只在 `POST /checkins`（用户点击按钮）时 INSERT；sessions 只流水账，与 daily_checkin 解耦。

两表关系：
- sessions = 发生了什么（事实流水）
- daily_checkin = 用户的仪式性行为（意愿记录）

### 3.2 streak 算法：白天未打卡允许从昨天算起

**用户体验问题**：如果算法是「从今天开始往前数」，那**每个新的一天 00:00 到第一次打卡之前，streak 都显示 0**——早上打开 App 就看到「连续 0 天」严重打击积极性。

**优化算法**：

```
cur = today
if today ∉ 已打卡日期集合:
    cur = yesterday   ← 关键分支
streak = 0
while cur ∈ 集合:
    streak += 1
    cur -= 1 day
return streak
```

效果：
- 昨天打了、今天还没打 → streak 仍然延续昨天的数（体验友好），等今天 23:59 一过还没打，明天自然掉 1
- 昨天也没打 → cur 昨天 ∉ 集合 → streak=0，正确

---

## 📉 4. 统计降级策略（接口失败 ↔ 本地聚合）

### 4.1 为什么需要降级？

- 场景：后端挂了 / 网络断了 / 用户 F5 后后端还没起来
- 诉求：StatsView 不要白屏，**至少把前端 session store 里已有的数据展示出来**
- 同时 TimerView / TaskView / CheckinView 也不白屏（try/catch + error ignored）

### 4.2 实现（StatsStore）

```ts
// src/stores/stats.ts · loadWeek()
async function loadWeek() {
  try {
    weekData.value = await statsApi.week()          // 先走 /stats/week
    return
  } catch (err) {
    // 降级：从 sessions 拉整个区间，前端 reduce
    const today = dayjs()
    const start = today.subtract(6, 'day').format('YYYY-MM-DD')
    const end   = today.format('YYYY-MM-DD')
    const sessions = await sessionApi.list({ startDate: start, endDate: end })
    weekData.value = aggregateLocal(sessions, startDate, endDate, 7)  // 同样补 0 对齐
  }
}
```

### 4.3 本地聚合与后端聚合等价（aggregateLocal 必须对齐 StatsService.aggregateLast）

两者产出完全一致的 DayStat[]：
1. 长度恒为 n（7/30）
2. 只加总 `sessionType === 'WORK'`
3. 没专注的日期补 `{ date, minutes: 0 }`
4. 顺序：旧 → 新

这样无论走接口还是降级，StatsChart 拿到的 data 结构完全一致，无需改渲染逻辑。

---

## 📈 5. 图表类型映射与实现设计（StatsChart）

### 5.1 chartType → ECharts 系列类型对照表

| chartType | ECharts series.type | 关键 option 差异 |
|---|---|---|
| `bar`（默认） | `'bar'` | barWidth: 24，itemStyle 圆角 6px，晴空蓝线性渐变 |
| `line` | `'line'` | smooth: true，symbol: 'circle'，lineStyle 2px 晴空蓝 |
| `area` | `'line'` + areaStyle | 同上 + `areaStyle: {color: linearGradient(brand 65%→transparent)}`，面积图观感 |
| `pie` | `'pie'` + radius: `['55%', '75%']` | 环形图；只统计 **> 0 分钟** 的日期；每片 label `日期 n分钟`；稳定调色板按索引取色 |

### 5.2 关键实现点

**1. 按需注册，不整包 import**（ECharts 6 的 tree-shaking 方式）
```ts
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, TitleComponent,
         DataZoomComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent,
             TitleComponent, DataZoomComponent, LegendComponent, CanvasRenderer])
```
体积：整包 ~700KB gzip 203KB → 按需 ~240KB gzip 65KB（省 65%+）。

**2. 切换类型时残留旧图表问题**

从 pie 切回 bar 时，之前 pie 的 legend / tooltip 可能残留（因为 xAxis 在 pie 中没有）。修复：
```ts
chartInstance.setOption(newOption, { notMerge: true })   // 彻底替换，不做增量 merge
```

**3. 空数据判定与空态**
```ts
const hasValidData = computed(() =>
  Array.isArray(props.data) && props.data.some(d => Number.isFinite(d.minutes) && d.minutes > 0)
)
```
`!hasValidData` → 不渲染 ECharts canvas，渲染纯 CSS 空态卡片（emoji 📊 + "还没有专注数据，先去学一个番茄吧～"），减少首屏 ECharts 初始化开销。

**4. ResizeObserver debounce**
窗口 drag 改变大小时 ResizeObserver 会高频触发。100ms 内重复调用忽略：
```ts
let t: ReturnType<typeof setTimeout> | null = null
resizeObserver = new ResizeObserver(() => {
  if (t) clearTimeout(t)
  t = setTimeout(() => chartInstance?.resize(), 100)
})
```

**5. 卸载必 dispose**
`onBeforeUnmount` 中三选一：
```
if (resizeObserver) resizeObserver.disconnect()
if (chartInstance)  chartInstance.dispose()
chartInstance = null   // 避免闭包持有引用
```

---

## 📐 6. 架构决策记录（ADR）

### ADR-1：为什么前端用 Pinia 而不是 localStorage？

> 决策：**状态统一进 Pinia，持久化交给后端 API**。只有极少配置（如"用户偏好的图表类型"）未来可加 PiniaPluginPersistedstate。

原因：
1. 任务、打卡、统计数据**需要跨设备同步**（虽然现在是单用户，但架构留了口子）
2. 写操作都要经 Service 校验/防重复，localStorage 直接改会越写越脏
3. 离线场景只要求"不白屏、展示旧数据"，降级路径（§4）已经够了，不引入多余的持久化复杂度

### ADR-2：为什么选 MyBatis-Plus 不写 Mapper XML？

决策：本项目只有 4 张表的基础 CRUD + 简单查询，`QueryWrapper` / `LambdaQueryWrapper` 已经能覆盖全部需求（`eq / between / orderByDesc / last limit 1`）。零 XML = 零维护成本。

如果未来出现复杂 SQL（JOIN 3 张表以上），再在 `resources/mapper/` 加 XML 即可，不用推翻现有。

### ADR-3：为什么 MySQL 驱动 characterEncoding 写 UTF-8 不是 utf8mb4？

MySQL 9.x 的 CJ JDBC 驱动不再识别 URL 参数 `characterEncoding=utf8mb4`（会抛 `Unknown system variable 'characterEncoding'`）。正确写法：`characterEncoding=UTF-8`（写 Java 端）。但实际传输字符集仍会通过 `SET NAMES utf8mb4` 在连接建立后生效，两码事——两者不冲突。

### ADR-4：为什么逻辑删除全局关掉，只注释？

原 application.yml 启用了 `logic-delete-field: deleted`，但 schema.sql 4 张表都没有 deleted 列 → 所有 SELECT 自动加 `WHERE deleted=0` → **Unknown column 'deleted' in 'where clause'**（P0 Bug，已修复）。

如果未来只有某张表要启用逻辑删除（如 task），方案是**单表用 `@TableLogic` 注解**（Entity 字段上加），application.yml 全局保持关闭。见 [sql/README.md Q4](../sql/README.md#q4-想启用逻辑删除只给-task-加-deleted-列别的表不加可以吗)。

### ADR-5：为什么前后端没有做登录/鉴权？

单用户版 PRD 明确是「桌面级番茄钟」——使用者一个人。鉴权（Sa-Token / Spring Security）会引入 4~5 张表（user / role / permission / user_role）和整套注册登录流程，对于当前 scope 是过度工程。后续如果升级为 SaaS 多用户：
1. 数据库加 `user_id` 列到所有 4 张业务表 + 索引
2. 前端加登录页，axios 请求拦截器加 `Authorization: Bearer <token>`
3. 后端加 HandlerMethodArgumentResolver 从 token 解析 userId，注入到所有 Service 方法参数

**所有现有 Service 只需要加一个 userId 过滤条件就能平移**，架构没有大改。

---

## 🧪 7. 自测用例矩阵（代码 Review 时必检）

| 模块 | 用例 | 通过标准 | 测试点映射设计文档 |
|---|---|---|---|
| **计时** | 改配置 25→1，开始，切后台 tab 3 分钟，切回来 | 立刻完成弹窗，不飘 | 防漂移算法 §1 |
| **计时** | 1 分钟专注，30 秒时按 reset | 不产生 session（DB pomodoro_session 行数不变） | 完整完成才 onComplete §1 |
| **任务** | POST /tasks 空 title | 前端禁用提交 + 后端 code=400 | 写安全层 1 §2 |
| **任务** | task 设为当前专注 → 完成 study → POST session | session.taskId = 该任务 id；删除该任务 → session.taskId 变 null | FK ON DELETE SET NULL 数据字典 |
| **打卡** | 连续两次 POST /checkins | 第一次 200，第二次 code=409 | 三层防重复 §2 §3 |
| **打卡** | 昨天打了，今天没打 → streak | 显示昨天连续数（不是 0） | 白天连续天数算法 §3.2 |
| **统计** | 关后端（8080 停），刷新 /stats | 不白屏；接口失败后走本地 sessions 聚合；全 0 显示空态 | 降级策略 §4 |
| **统计** | chartType 在 bar / line / area / pie 间快速切换 | 无残留 xAxis/dataZoom；饼图每个分片稳定颜色不随机 | notMerge + 调色板 §5.2 |
| **响应式** | 浏览器宽度拖到 375px / 767px / 1024px | MainLayout 汉堡菜单；Timer 表盘自适应；弹窗底部滑出 | 前端断点表 / frontend/README.md §响应式 |
| **写安全** | 前端抓包改请求重放 POST /checkins | 仍然 409（层 2 或 层 3 兜底命中） | 三层防重复 §2 |
