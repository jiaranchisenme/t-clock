# Focusly · REST API 完整契约

> Version 1.0 · 所有接口 HTTP 200，用 `Result.code` 区分业务成功/失败
> Base URL：`http://host:port/api`（开发环境前端 Vite 已代理 `/api` → `localhost:8080/api`）

---

## 📡 通用约定

### 1. 统一响应壳 `Result<T>`

所有接口响应均为 JSON，顶层三字段：

```json
{
  "code": 0,       // 0=成功；非 0=失败；错误码在下方有枚举
  "msg":  "ok",    // 成功=ok / 简要英文标识；失败=中文可读提示
  "data": { ... }  // 成功=T；失败=null
}
```

前端 `api/http.ts` 已做**自动剥壳**：`const t: Task = await http.get<Task>('/api/tasks/1')` 直接拿到 Task。失败时走 `Promise.reject(Error(msg))`。

### 2. 错误码枚举（`Result.code`）

| code | 含义 | 典型触发场景 | 前端建议处理 |
|---|---|---|---|
| **0** | 成功 | 所有正常流程 | 取 `data` 渲染 |
| **400** | 参数错误 / 业务校验失败 | `@Valid` 失败；Service 抛 IllegalArgumentException | 弹 toast `Error.message`，保留用户输入 |
| **409** | 业务状态冲突（幂等/唯一约束） | 重复打卡（三层兜底任一命中） | toast "今日已打卡" + 禁用按钮 |
| **500** | 服务器内部错误 | SQL 错 / NPE / 未预期异常 | toast "系统繁忙，请稍后重试"；**不要把 msg 原样弹给用户** |
| 其他 | 约定外错误码 | 未来扩展 | 按 500 处理 |

> ⚠️ 注意：**HTTP 状态码永远 200**（除 nginx 层 502/504 这种根本到不了应用层的）。前后端联调不要看 HTTP 状态，看 `code`。

### 3. 日期时间格式

| 场景 | 格式 | 示例 |
|---|---|---|
| **仅日期（打卡/统计）** | ISO YYYY-MM-DD（字符串） | `"2025-01-15"` |
| **日期时间（会话 start/end）** | ISO 8601 LocalDateTime | `"2025-01-15T14:30:00"`（不带时区，都按 Asia/Shanghai） |
| **自动时间戳（createdAt/updatedAt）** | 同上，Jackson 序列化为 ISO 字符串 | `"2025-01-15T14:30:05"` |

前端 `new Date('2025-01-15T14:30:00')` 直接解析，时区按浏览器本地。

---

## 0️⃣ 健康检查（非正式接口）

```
GET /api/timer/config   # 最稳定、不依赖任何用户数据的接口，做探针
→ 200 + code=0 + data={id,workDuration,breakDuration,updatedAt}
```

---

## ⏱️ 1. 计时器配置（TimerConfig）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/timer/config` | 读取当前配置（**永不返回 null**，空表会兜底 25/5） |
| PUT | `/timer/config` | 更新配置（影响前端下次 load；TimerView 会 reset 回 study） |

### 1.1 `GET /api/timer/config`

**Query 参数**：无

**成功响应**：
```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "id": 1,
    "workDuration": 25,
    "breakDuration": 5,
    "updatedAt": "2025-01-15T09:00:00"
  }
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| id | number | 主键（预期恒为 1） |
| workDuration | number | 专注时长，**分钟**，[1,180] |
| breakDuration | number | 休息时长，**分钟**，[1,60] |
| updatedAt | string | ISO 8601 时间戳 |

**可能错误**：500（非正常；空表已兜底）

---

### 1.2 `PUT /api/timer/config`

**Body JSON**（`TimerConfigRequest`）：
```json
{
  "workDuration": 45,
  "breakDuration": 10
}
```

| 字段 | 必填 | 约束 | 错误时返回 |
|---|---|---|---|
| workDuration | ✅ | number，1 ~ 180（整数） | 400：`参数错误: 学习时长至少 1 分钟` / `最多 180 分钟` / `不能为空` |
| breakDuration | ✅ | number，1 ~ 60（整数） | 400：`休息时长至少 1 分钟` / `最多 60 分钟` / `不能为空` |

> ⚠️ 非数字、NaN、字符串会被 Jackson 拒绝，返回 500（`HttpMessageNotReadableException` → handleAll → code=500）。前端需要在输入框先把字符串转数字 + 校验。

**成功响应**：返回更新后的完整 TimerConfig（同 GET 结构）

**前端副作用**：TimerView 调 PUT 成功后，会调用 `useCountdown.applyConfig(newWork*60, newRest*60)` → 停止当前运行中的计时 + 回到 study 初始态。

---

## 📝 2. 任务管理（Task）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/tasks` | 列出全部任务（按创建时间倒序） |
| POST | `/tasks` | 新建任务 |
| PUT | `/tasks/{id}` | 更新任务（标题/描述/状态；按字段合并，只改传了的） |
| DELETE | `/tasks/{id}` | 删除单个任务 |
| DELETE | `/tasks` | 清空全部任务（⚠️ 无二次确认，**前端必须弹确认再调用**） |

### 2.1 `GET /api/tasks`

**Query**：无

**成功响应**：
```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "id": 12,
      "title": "背单词 Unit 3",
      "description": "List 1-20，每个 3 遍",
      "status": 0,
      "createdAt": "2025-01-15T10:00:00",
      "updatedAt": "2025-01-15T10:00:00"
    },
    {
      "id": 11,
      "title": "复习高数错题",
      "description": null,
      "status": 1,
      "createdAt": "2025-01-14T22:13:11",
      "updatedAt": "2025-01-14T23:00:59"
    }
  ]
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| id | number | 主键 |
| title | string | 任务名（1~100 字） |
| description | string \| null | 描述（最多 500 字，可空） |
| status | 0 \| 1 | 0=未完成，1=已完成 |
| createdAt | string | 创建时间 |
| updatedAt | string | 最后更新时间 |

排序：`ORDER BY created_at DESC`（最新创建在前）

---

### 2.2 `POST /api/tasks`

**Body JSON**（`TaskCreateRequest`）：
```json
{ "title": "背单词", "description": "20 个新词" }
```

| 字段 | 必填 | 约束 | 错误 |
|---|---|---|---|
| title | ✅ | string，trim 后非空，长度 ≤ 100 | 400：`任务名称不能为空` / `最多 100 字` |
| description | ❌ | string 或 null，长度 ≤ 500 | 400：`任务描述最多 500 字` |

**成功响应**：返回新创建任务完整对象（含 id / createdAt），code=0

**前端业务约束**：
- 空字符串 / 全空格禁止提交（Service 层再次判断，仍然 400）
- 超长自动截断建议做在前端 UI（显示剩余字数）

---

### 2.3 `PUT /api/tasks/{id}`

**Path**：`{id}` = 任务主键（number）

**Body JSON**（`TaskUpdateRequest`，字段合并式更新：只改传了的字段，不传的保持原值）：
```json
{
  "title": "改个名字",       // 可选
  "description": "新描述",   // 可选
  "status": 1                // 可选，0 或 1
}
```

| 字段 | 必填 | 约束 | 错误 |
|---|---|---|---|
| title | ❌ | 若传：trim 非空，≤ 100 | 400：`任务名称不能为空`（传了空串）/ `最多 100 字` |
| description | ❌ | 若传：≤ 500 | 400：`最多 500 字` |
| status | ❌ | 若传：number 0 或 1 | 400：`状态值非法` |

**成功响应**：返回更新后的完整 Task 对象

**错误 500**：id 不存在（`updateById` 返回 false → service 抛 IllegalStateException，实际会返回 500）

---

### 2.4 `DELETE /api/tasks/{id}`

**Path**：`{id}` = 任务主键

**Body**：无

**成功响应**：
```json
{ "code": 0, "msg": "ok", "data": null }
```

**外键副作用**：`pomodoro_session.task_id` FK 为 `ON DELETE SET NULL` → 历史 session 不删，task_id 清空。统计数据不变。

**错误 500**：id 不存在

---

### 2.5 `DELETE /api/tasks`（清空全部）

**Path/Body/Query**：无

**成功响应**：同上 code=0

> ⚠️ **写安全提醒**：后端无二次确认。前端 TaskView 清空按钮必须弹 Modal "确定删除全部任务吗？此操作不可撤销"，用户点"确定"后才发这个 DELETE。

---

## 🍅 3. 番茄会话（Session）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/sessions` | 保存一条完成的会话（仅完整结束时调用；中途 reset 不产生记录） |
| GET | `/sessions` | 列出会话（支持按日期区间过滤，用于 stats 降级本地聚合） |

### 3.1 `POST /api/sessions`

**触发时机**：`useCountdown.onComplete(finishedMode)` 回调里，`finishedMode === 'study'` 或 `'rest'` 都落库（只有 WORK 加总统计）。

**Body JSON**（`SessionSaveRequest`）：
```json
{
  "sessionType": "WORK",
  "startTime": "2025-01-15T14:00:00",
  "endTime": "2025-01-15T14:25:00",
  "durationMinutes": 25,
  "taskId": 12
}
```

| 字段 | 必填 | 约束 | 错误 |
|---|---|---|---|
| sessionType | ✅ | 字符串非空，约定 `'WORK'` \| `'BREAK'`（未做严格枚举，前端保证） | 400：`会话类型不能为空` |
| startTime | ✅ | ISO 8601 LocalDateTime | 400：`开始时间不能为空` |
| endTime | ✅ | ISO 8601 LocalDateTime | 400：`结束时间不能为空` |
| durationMinutes | ✅ | number，[1, 600] 整数分钟 | 400：`时长至少 1 分钟` / `最多 600 分钟` / `不能为空` |
| taskId | ❌ | number 或 null / 不传 | — |

> ⚠️ **以 durationMinutes 为准**，不按 endTime-startTime 差算实际分钟（避免用户电脑时钟偏、DST 跳变）。start/endTime 仅用于"按 DATE 归组"和审计。

**成功响应**（`SessionSaveResult`）：
```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "sessionId": 101,
    "todayFocusMinutes": 75
  }
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| sessionId | number | 新落库行 id（调试用） |
| todayFocusMinutes | number | **今日累计专注分钟数**（截止本次 session），TimerView 底部直接展示 |

---

### 3.2 `GET /api/sessions`

用于：StatsStore 加载周/月数据失败时，从 sessions 列表**降级本地聚合**。

**Query 参数（均可选）**：

| 参数 | 类型 | 格式 | 说明 |
|---|---|---|---|
| startDate | string | `YYYY-MM-DD` | 开始日期（含）。null = 不做下限 |
| endDate | string | `YYYY-MM-DD` | 结束日期（含）。null = 不做上限 |

```
GET /api/sessions?startDate=2025-01-09&endDate=2025-01-15
```

**成功响应**：
```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "id": 101,
      "sessionType": "WORK",
      "startTime": "2025-01-15T14:00:00",
      "endTime": "2025-01-15T14:25:00",
      "durationMinutes": 25,
      "taskId": 12,
      "createdAt": "2025-01-15T14:25:01"
    }
  ]
}
```

排序：`ORDER BY start_time DESC`（最近在前）

---

## 📅 4. 每日打卡（Checkin）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/checkins` | 执行打卡（核心：三层防重复 → 409） |
| GET | `/checkins/today` | 查询今日状态 + 今日累计专注分钟 |
| GET | `/checkins/dates` | 最近 N 天已打卡的日期字符串集合（用于日历高亮） |
| GET | `/checkins/streak` | 计算连续打卡天数 |

### 4.1 `POST /api/checkins`

**Path/Body/Query**：无。后端取 `LocalDate.now(ZoneId.of("Asia/Shanghai"))` 作为今日。

**成功响应**：返回新建 Checkin
```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "id": 20,
    "checkinDate": "2025-01-15",
    "focusTotalMinutes": 75,
    "createdAt": "2025-01-15T15:00:00"
  }
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| id | number | 主键 |
| checkinDate | string | 打卡日期 YYYY-MM-DD |
| focusTotalMinutes | number | **打卡快照**：打卡瞬间的当日累计 WORK 分钟。之后继续专注的新分钟数不回写本表。 |
| createdAt | string | 实际点击打卡的时间戳 |

**典型冲突响应（409）**：前端 `todayCheckedIn=true` 后按钮已经禁用，但用户绕过 UI 或并发双写 → 409：

```json
{
  "code": 409,
  "msg": "今日已打卡",
  "data": null
}
```

触发路径（可能是任一）：
- Service 层 SELECT 发现已存在 → `IllegalStateException` → GlobalExceptionHandler
- DB `UNIQUE uk_checkin_date` 拒绝 → `DuplicateKeyException` → GlobalExceptionHandler

**前端处理**：toast 提示 + 禁用按钮 + 刷新 `/checkins/today`

---

### 4.2 `GET /api/checkins/today`

**成功响应**：
```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "checkedIn": false,
    "todayMinutes": 75
  }
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| checkedIn | boolean | 今天是否已打卡 |
| todayMinutes | number | 今日累计专注分钟数（已打卡 + 打卡后新增 session 的组合值） |

> 这个 `todayMinutes` 是 CheckinView / TimerView 底部 "今日专注 X 分钟" 的统一数据源（不用前端自己加总）。

---

### 4.3 `GET /api/checkins/dates`

**Query**：

| 参数 | 默认 | 说明 |
|---|---|---|
| days | 30 | 取最近多少天内已打卡的日期 |

```
GET /api/checkins/dates?days=14   # CheckinView 最近 14 天日历
```

**成功响应**：
```json
{
  "code": 0,
  "msg": "ok",
  "data": ["2025-01-10","2025-01-11","2025-01-12","2025-01-14"]
}
```

CheckinView 渲染 14 格日历时：每个格子的日期在数组中 → 高亮嫩绿打卡点。

---

### 4.4 `GET /api/checkins/streak`

**成功响应**：
```json
{
  "code": 0,
  "msg": "ok",
  "data": { "streak": 3 }
}
```

streak 算法语义（详见 [DESIGN.md · 打卡连续天数算法](./DESIGN.md#3-打卡语义--连续天数算法)）：
- **今天已打卡**：从今天开始往前数
- **今天未打卡**：从昨天开始往前数（白天不会显示 streak=0 打击用户）
- 空记录：`streak = 0`

---

## 📊 5. 数据统计（Stats）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/stats` | 综合聚合包（daily7 + daily30 + weeklyTotal + monthlyTotal，老接口兼容） |
| GET | `/stats/week` | 近 7 天按日专注分钟（**日期补 0**，从旧→新排序） |
| GET | `/stats/month` | 近 30 天按日专注分钟（同上） |

### 通用 `DayStat` 结构

```json
{ "date": "2025-01-09", "minutes": 25 }
```

| 字段 | 类型 | 说明 |
|---|---|---|
| date | string | ISO 日期（前端 xAxis 类别直接用） |
| minutes | number | 当日所有 WORK session 分钟之和（可能为 0，表示那天没专注） |

### 5.1 `GET /api/stats/week`

**Query**：无。自动取 `today - 6 days` → `today` 共 7 天。

**成功响应**：
```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    { "date": "2025-01-09", "minutes": 25 },
    { "date": "2025-01-10", "minutes": 50 },
    { "date": "2025-01-11", "minutes":  0 },   // 缺日补 0
    { "date": "2025-01-12", "minutes": 75 },
    { "date": "2025-01-13", "minutes":  0 },
    { "date": "2025-01-14", "minutes": 25 },
    { "date": "2025-01-15", "minutes": 25 }
  ]
}
```

- **长度恒为 7**（StatsChart 不需要自己补，xAxis 直接用 data[i].date）
- **顺序**：旧 → 新（从 6 天前到今天）

### 5.2 `GET /api/stats/month`

同 `/week`，长度恒为 30。建议前端 StatsView `scrollable=true`（dataZoom 拖动看 30 天）。

### 5.3 `GET /api/stats`（综合包，兼容老前端）

**成功响应**：
```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "daily7": [ /* DayStat × 7 同上 */ ],
    "daily30": [ /* DayStat × 30 同上 */ ],
    "weeklyTotal": 200,
    "monthlyTotal": 850
  }
}
```

---

## 🚪 CORS 说明

`WebMvcConfig` 允许来源 `http://localhost:5173`（Vite dev）。生产部署建议：
- 前端 dist 由 nginx 同域（80/443）托管
- `location /api` 反向代理到 `127.0.0.1:8080`
- 不依赖 CORS，也就没有预检请求 / 跨域 Cookie 问题

---

## 🧪 快速联调 · curl 测试脚本

```bash
BASE=http://localhost:8080/api

# 1. 健康检查
curl -s $BASE/timer/config | jq .

# 2. 改配置（45/10）
curl -s -X PUT $BASE/timer/config \
  -H 'Content-Type: application/json' \
  -d '{"workDuration":45,"breakDuration":10}' | jq .

# 3. 建任务
curl -s -X POST $BASE/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"背单词","description":"Unit 3"}' | jq .

# 4. 列任务
curl -s $BASE/tasks | jq '.data | length'   # 任务数

# 5. 完成一次 WORK（假设任务 id=1，时长 25min）
curl -s -X POST $BASE/sessions \
  -H 'Content-Type: application/json' \
  -d '{"sessionType":"WORK","startTime":"2025-01-15T14:00:00","endTime":"2025-01-15T14:25:00","durationMinutes":25,"taskId":1}' | jq .

# 6. 打卡
curl -s -X POST $BASE/checkins | jq .
# 再打一次 → 预期 code=409
curl -s -X POST $BASE/checkins | jq .

# 7. 查今日状态
curl -s $BASE/checkins/today | jq .

# 8. 周统计
curl -s $BASE/stats/week | jq .
```
