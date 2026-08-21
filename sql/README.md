# Focusly · 数据库文档

> InnoDB / utf8mb4_unicode_ci / 4 张表 · 写安全三兜底（Default + Service 查重 + UNIQUE）

---

## 🗄️ 数据库基本信息

| 项 | 值 |
|---|---|
| 数据库名 | `focusly` |
| 字符集 | `utf8mb4`（完整支持 emoji / 中文 / 罕见汉字） |
| 排序规则 | `utf8mb4_unicode_ci`（基于 Unicode 标准排序，中文按拼音；比 general_ci 准确且开销可接受） |
| 引擎 | 所有表统一 **InnoDB**（支持事务 / 行锁 / 外键 / 崩溃恢复） |
| 时区 | MySQL `@@global.time_zone = Asia/Shanghai`（应用层 serverTimezone 也一致） |
| 建库脚本 | [schema.sql](schema.sql)（**幂等**，每张表 DROP TABLE IF EXISTS 后重建） |

---

## 🧭 初始化顺序（Step by Step）

```bash
# 1. 创建数据库 + 专属业务账户（不使用 root，写安全 #6）
mysql -u root -p <<'SQL'
CREATE DATABASE focusly DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'focusly'@'%' IDENTIFIED BY 'focusly123';
GRANT ALL PRIVILEGES ON focusly.* TO 'focusly'@'%';
FLUSH PRIVILEGES;
SQL

# 2. 导入 schema.sql（4 张表 DDL + 1 行默认 INSERT）
mysql -u focusly -pfocusly123 focusly < sql/schema.sql

# 3. 验证表是否都在
mysql -u focusly -pfocusly123 focusly -e "SHOW TABLES;"
# 预期输出：
# +-------------------+
# | Tables_in_focusly |
# +-------------------+
# | daily_checkin     |
# | pomodoro_session  |
# | task              |
# | timer_config      |
# +-------------------+

# 4. 验证默认配置行
mysql -u focusly -pfocusly123 -e "SELECT * FROM focusly.timer_config;"
# +----+---------------+----------------+---------------------+
# | id | work_duration | break_duration | updated_at          |
# +----+---------------+----------------+---------------------+
# |  1 |            25 |              5 | 2025-xx-xx xx:xx:xx |
# +----+---------------+----------------+---------------------+
```

---

## 📚 数据字典（4 表 × 全字段）

### 表 1：`timer_config` — 计时器配置（单表单用户，只存 1 行）

| 字段 | 类型 | 默认 | 约束 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | AUTO_INCREMENT | PK | 主键（预期只有 id=1 一行） |
| `work_duration` | INT | **25** | NOT NULL，[1,180] 由 DTO/Service 双重校验 | 专注模式时长（分钟） |
| `break_duration` | INT | **5** | NOT NULL，[1,60] 由 DTO/Service 双重校验 | 休息模式时长（分钟） |
| `updated_at` | DATETIME | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | NOT NULL | 自动更新时间戳 |

- **写安全 #4 空表兜底**：即使这行被误删，`TimerConfigService.getConfig()` 仍会返回 `{work=25, break=5}` 给前端，然后更新会把行 INSERT 回来。
- **前端 → 字段映射**：TimerStore `workDuration / breakDuration` ↔ MySQL `work_duration / break_duration`（MyBatis-Plus `map-underscore-to-camel-case=true` 自动转）。

---

### 表 2：`task` — 学习任务清单

| 字段 | 类型 | 默认 | 约束 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | AUTO_INCREMENT | PK | 主键 |
| `title` | VARCHAR(100) | — | NOT NULL | 任务名称（禁止空；前端空提交 → 后端 400） |
| `description` | VARCHAR(500) | NULL | 可空 | 简要描述（可选） |
| `status` | TINYINT | **0** | NOT NULL，{0,1} 由 DTO @Min@Max + Service 双校验 | **0 = 未完成**（默认），**1 = 已完成** |
| `created_at` | DATETIME | CURRENT_TIMESTAMP | NOT NULL | 创建时间（后端写，前端不提交） |
| `updated_at` | DATETIME | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | NOT NULL | 更新时间（后端写） |

**索引**：
- `idx_status (status)` — 按状态过滤时走索引（`WHERE status=0` 未完成清单）
- `idx_created_at (created_at)` — 列表默认 ORDER BY created_at DESC

**外键引用**：
- 被 `pomodoro_session.task_id` 外键引用（`ON DELETE SET NULL`，见下）

---

### 表 3：`pomodoro_session` — 番茄会话明细（所有专注/休息完成记录的流水账）

| 字段 | 类型 | 默认 | 约束 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | AUTO_INCREMENT | PK | 主键 |
| `session_type` | VARCHAR(10) | — | NOT NULL，`'WORK' \| 'BREAK'` | 会话类型。只有 **WORK** 计入专注时长与打卡汇总 |
| `start_time` | DATETIME | — | NOT NULL | 会话开始时间（**前端传**，来自浏览器本地时间转 ISO，服务端不做校正） |
| `end_time` | DATETIME | — | NOT NULL | 会话结束时间 |
| `duration_minutes` | INT | — | NOT NULL，[1,600]（@Min/@Max） | 实际专注分钟数（**前端计算**，不按 end-start 差值，避免时钟偏差） |
| `task_id` | BIGINT | NULL | FK → `task(id)` **ON DELETE SET NULL** | 关联的任务 ID。若完成时没选"当前专注任务"则为 NULL |
| `created_at` | DATETIME | CURRENT_TIMESTAMP | NOT NULL | 落库时间 |

**索引**：
- `idx_start_time (start_time)` — 统计聚合按日期 GROUP BY DATE(start_time) 必用
- `idx_task_id (task_id)` — 反查"这个任务历史上花了多少番茄"

**关键业务语义**：
- 只有 STUDY 模式**完整完成**（`useCountdown.onComplete('study')` 触发）才 POST `/sessions`。中途 reset / 暂停 / 手动切休息 → 不产生记录。
- BREAK 模式完成也会落库（sessionType='BREAK'），但 SUM 统计时被排除，仅用于未来"作息分析"扩展。
- **ON DELETE SET NULL**：删除任务 → 历史 session 不删，task_id 置空。保护历史统计不丢失。

---

### 表 4：`daily_checkin` — 每日打卡（1 天最多 1 行 · 三层防重复）

| 字段 | 类型 | 默认 | 约束 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | AUTO_INCREMENT | PK | 主键 |
| `checkin_date` | DATE | — | NOT NULL，**UNIQUE(uk_checkin_date)** | 打卡日期（YYYY-MM-DD，不带时分秒） |
| `focus_total_minutes` | INT | **0** | NOT NULL | 当日累计专注分钟数（打卡瞬间 SUM sessions 得到，之后不再变动） |
| `created_at` | DATETIME | CURRENT_TIMESTAMP | NOT NULL | 实际打卡动作时间戳 |

**三层防重复（打卡是典型写安全热点）**：

| 层级 | 位置 | 机制 | 失败后果 |
|---|---|---|---|
| 第 1 层 · 前端 UI | CheckinView `:disabled="todayCheckedIn"` | 今日已打卡 → 按钮置灰，根本点不动 | 无请求发出 |
| 第 2 层 · Service | CheckinService.doCheckin() 先 SELECT | `if exists(today): throw IllegalStateException("今日已打卡")` | HTTP 200，Result.code=**409**，前端 toast |
| 第 3 层 · DB 兜底 | `UNIQUE KEY uk_checkin_date (checkin_date)` | 并发双写（两人同时点/绕过前端/绕过 service） | MySQL 1062 Duplicate entry → `DuplicateKeyException` → GlobalExceptionHandler → Result.code=**409** |

> 三层机制同时生效，正常用户只感知第 1 层；第 2、3 层为防御性编程。

---

## 🔗 表关系 ER 图

```
 ┌──────────────────────┐
 │   timer_config       │        （单表单行，无外键）
 │  PK id               │
 │     work_duration    │        [1 row expected]
 │     break_duration   │
 └──────────────────────┘

 ┌──────────────────────┐          1             N
 │   task               │ ────────────────────┐
 │  PK id               │                      │
 │     title (100)      │                      │
 │     description(500) │                      │
 │     status {0,1}     │                      │ FK ON DELETE SET NULL
 │  IDX idx_status      │                      ▼
 │  IDX idx_created_at  │       ┌──────────────────────────────┐
 └──────────────────────┘       │   pomodoro_session           │
                                │  PK id                        │
                                │     session_type ∈ {W,B}      │
                                │     start_time  / end_time    │
                                │     duration_minutes [1,600]  │
                                │  FK task_id  ─────────────────┘
                                │  IDX idx_start_time           │
                                └──────────────┬───────────────┘
                                               │
                                               │ 聚合：SUM(WORK) GROUP BY DATE(start_time)
                                               ▼
                                ┌──────────────────────────────┐
                                │   daily_checkin              │
                                │  PK id                        │
                                │  UK uk_checkin_date  ◄────── 第 3 层 UNIQUE 兜底
                                │     checkin_date              │
                                │     focus_total_minutes       │
                                └──────────────────────────────┘
```

---

## 🔢 初始化数据（仅 1 条）

```sql
INSERT INTO timer_config (work_duration, break_duration) VALUES (25, 5);
```

- 来源于 PRD 默认"25 分钟专注 + 5 分钟休息"（番茄工作法经典值）
- 这条 INSERT 与 `TimerConfigService.getConfig()` 的空表兜底**对齐**：无论是否执行了这行，前端都拿到 25/5。
- 如要改默认，**同时改两处**：schema.sql 的 INSERT VALUES + Service 兜底 `new TimerConfig(25,5)` 的参数。

---

## 🛡️ 已应用的 4 条写安全数据库原则

| # | 原则 | 本项目落地 |
|---|---|---|
| 1 | **DEFAULT 兜底**，避免 NULL 渗入业务逻辑 | 所有 INT / TINYINT / DATETIME 都有 `NOT NULL DEFAULT xxx`（除了 task.description 和 session.task_id 语义上允许空） |
| 2 | **UNIQUE 约束**做最后防线 | `daily_checkin.uk_checkin_date` |
| 3 | **外键级联**避免孤儿数据 | `pomodoro_session.task_id` → `task.id` ON DELETE SET NULL |
| 4 | **索引覆盖**统计聚合查询 | `idx_start_time`（stats GROUP BY DATE）、`idx_status`（任务筛选）、`idx_created_at`（任务排序） |

---

## ❓ 数据库常见问题

<details>
<summary>Q1. schema.sql 开头的 SET FOREIGN_KEY_CHECKS = 0 是干嘛的？</summary>

因为 DROP TABLE 顺序可能和外键依赖不一致（先删 task 再删 pomodoro_session 会触发外键约束报错）。临时关掉 FK 检查 → 所有表按任意顺序 DROP → CREATE → 最后 `SET FOREIGN_KEY_CHECKS = 1` 恢复。

> 只在建库脚本里用，运行时绝对不要关 FOREIGN_KEY_CHECKS。

</details>

<details>
<summary>Q2. 字符集排序 utf8mb4_unicode_ci 和 utf8mb4_0900_ai_ci 有啥区别？用哪个？</summary>

- `utf8mb4_0900_ai_ci`：MySQL 8.0+ 新默认，基于 Unicode 9.0，排序更准、更快，`ai` 表示口音不敏感。**数据库级别**用它（CREATE DATABASE 用的是这个）。
- `utf8mb4_unicode_ci`：老标准，兼容更早版本。**表级别**声明它是为了如果库的 collation 被改成别的，表内排序规则依然可预期。

两者完全兼容，混着用不会错。

</details>

<details>
<summary>Q3. pomodoro_session 存的是 startTime/endTime + durationMinutes，会不会不一致？</summary>

可能会（例如用户电脑时钟不准 / NTP 跳变）。**以 durationMinutes 为准**——它是前端 `useCountdown` 里完整走完后 `studySeconds/60` 算出来的，不受时钟影响。start/endTime 只用于"按哪一天归组"和未来审计。

因此 StatsService / CheckinService 都是 `SUM(duration_minutes)`，不用 `TIMESTAMPDIFF(MINUTE, start_time, end_time)`。

</details>

<details>
<summary>Q4. 想启用"逻辑删除"，只给 task 加 deleted 列，别的表不加，可以吗？</summary>

可以。分别做：

1. schema.sql：给 task 表加 `deleted TINYINT NOT NULL DEFAULT 0`，并给所有查询加 `AND deleted=0` 条件（MP 会自动加）。
2. Task Entity：加字段 `@TableLogic private Integer deleted;`。
3. application.yml：**不能全局**开 `logic-delete-field`（会影响所有 4 张表，其余表没列会报错）。而是**只在 Task 实体上用 `@TableLogic` 注解**，单表启用。
4. pomodoro_session 的外键：`ON DELETE SET NULL` 逻辑删除不会触发（因为实际是 UPDATE）。保持现状即可。

</details>

<details>
<summary>Q5. daily_checkin 只在打卡时 INSERT，但当天继续专注的分钟数怎么体现？</summary>

本项目语义是「**打卡快照**」：`focus_total_minutes` 存的是**打卡那一刻**已经专注的分钟数，当天后续新 session 不再更新这张表（避免 UPDATE 写放大与冲突）。

前端「今日累计专注分钟数」来自两部分：
- 未打卡：实时 `SUM(sessions of today where WORK)`
- 已打卡：`focus_total_minutes` + 打卡后新增 sessions 的 SUM

CheckinController `/api/checkins/today` 已按此语义返回组合值 `todayMinutes`（Service 里做了加总），前端直接用即可。

</details>
