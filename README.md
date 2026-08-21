# Focusly · 轻番茄时钟 · 专注每一刻 🍅

> 极简、清新、轻量化的学生风格番茄钟工具 — 把每一次专注都变成可量化的小进步 🌱

[![Vue 3][vue-badge]](https://vuejs.org/)
[![Spring Boot][sb-badge]](https://spring.io/projects/spring-boot)
[![MySQL][mysql-badge]](https://www.mysql.com/)
[![ECharts][echarts-badge]](https://echarts.apache.org/)
[![Vite][vite-badge]](https://vitejs.dev/)
[![License][mit-badge]](LICENSE)

[vue-badge]: https://img.shields.io/badge/Vue-3.5-42b883?logo=vue.js
[sb-badge]: https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?logo=spring-boot
[mysql-badge]: https://img.shields.io/badge/MySQL-9.x-4479a1?logo=mysql&logoColor=fff
[echarts-badge]: https://img.shields.io/badge/ECharts-6-17a2b8
[vite-badge]: https://img.shields.io/badge/Vite-8-646cff?logo=vite&logoColor=fff
[mit-badge]: https://img.shields.io/badge/license-MIT-blue

---

## ✨ 核心功能

| 模块 | 说明 |
|---|---|
| 🍅 **番茄计时** | 专注 / 休息 两种模式切换；圆形表盘进度；开始-暂停-重置-设置；**绝对时间戳无漂移算法**（后台 tab 恢复后时间不飘）；完整完成 STUDY 才产生一条专注记录；自定义弹窗替代 `alert` |
| 📝 **任务管理** | CRUD 清单；空任务禁止；超长自动截断；完成置灰+删除线；编辑实时校验；单个删除/一键清空（含二次确认）；**"设为当前专注"** — 该任务 ID 会自动写入后续 session |
| 📅 **每日打卡** | 本地日期 YYYY-MM-DD；当日累计专注分钟数自动汇总；**显式打卡**（学过不等于已打）；三层唯一性保护（前端禁用 → 后端查库 → DB UNIQUE）；最近 14 天可视化；连续天数计算（允许今天未打卡时从昨天算起）|
| 📊 **数据可视化** | 近 7 天 / 近 30 天切换；**4 种图表类型**（柱状 / 折线 / 面积 / 环形分布）；ECharts 按需注册、自动 resize、props 数据驱动、卸载 dispose；接口失败时**降级本地 sessions 聚合**；空数据优雅空态 |

---

## 🏗️ 技术架构

```
┌──────────────────────────────────────────────────────────────┐
│                       用户浏览器                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Vue 3 + TypeScript + Vite 8 (前端 SPA)                 │  │
│  │  · Views: Timer / Task / Checkin / Stats                │  │
│  │  · Stores: Pinia (timer / task / checkin / stats)       │  │
│  │  · UI: CSS Variables 设计系统 + 响应式 / 移动端抽屉      │  │
│  │  · 图表: ECharts 6 按需注册 (Bar / Line / Pie)          │  │
│  └──────────────────────┬─────────────────────────────────┘  │
│                         │ Vite 代理 (/api → :8080)           │
└─────────────────────────┼────────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────────┐
│                   Spring Boot 3.5 (后端 :8080)                │
│  ┌──────────────┐ ┌──────────────┐ ┌───────────────────────┐ │
│  │ Controllers  │ │   Services   │ │  MyBatis-Plus Mapper  │ │
│  │  5 × REST    │ │  5 × 业务    │ │  4 × 泛型 CRUD        │ │
│  └───────┬──────┘ └──────┬───────┘ └───────────┬───────────┘ │
│          └───────────────┼──────────────────────┘             │
│                          ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  公共层: Result 响应壳 / GlobalExceptionHandler / CORS   │  │
│  └─────────────────────────────────────────────────────────┘  │
└──────────────────────────────────┬───────────────────────────┘
                                   ▼
                        ┌──────────────────────┐
                        │    MySQL 9.x :3306   │
                        │  · pomodoro_task     │
                        │  · pomodoro_session  │
                        │  · daily_checkin     │
                        │  · timer_config      │
                        └──────────────────────┘
```

---

## 📦 环境要求

| 依赖 | 最低版本 | 推荐版本 | 用途 |
|---|---|---|---|
| **Node.js** | 20.x LTS | 24.x | 前端构建与 Vite dev server |
| **npm** / pnpm | 9+ | 10+ | 前端依赖管理 |
| **JDK** | 21 LTS | 25 LTS | Spring Boot 3.5 编译运行（含 Lombok 注解处理器）|
| **Maven** | 3.9+ | 3.9.10 | 后端构建（需配置代理/镜像以避免中央仓库超时）|
| **MySQL** | 8.0+ | 9.x | 数据持久化（字符集 `utf8mb4`）|

> 🟡 沙箱 / 国内网络环境：请参考 [backend/README.md · Maven 配置镜像](backend/README.md#%E9%85%8D%E7%BD%AE-maven-%E9%95%9C%E5%83%8F%E4%B8%8E%E4%BB%A3%E7%90%86) 设置代理和阿里云镜像。

---

## 🚀 快速启动（5 步跑起来）

### Step 1 · 克隆项目 & 准备 MySQL

```bash
git clone <your-repo-url> focusly && cd focusly
```

进入 MySQL 客户端，创建数据库和专属账户（**避免使用 root**，符合写安全原则）：

```sql
CREATE DATABASE focusly DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'focusly'@'%' IDENTIFIED BY 'focusly123';
GRANT ALL PRIVILEGES ON focusly.* TO 'focusly'@'%';
FLUSH PRIVILEGES;
```

> ⚠️ 生产环境务必修改密码并缩小权限范围（仅 `focusly.*` 的 SELECT/INSERT/UPDATE/DELETE，无需 `DROP` / `ALTER`）。

### Step 2 · 导入数据库结构（含初始化默认配置行）

```bash
mysql -u focusly -pfocusly123 focusly < sql/schema.sql
```

脚本详情见 [sql/README.md](sql/README.md)（共 4 张表 + 4 条注释 + 1 条默认配置 INSERT + 1 个 UNIQUE 索引）。

### Step 3 · 启动后端 :8080

```bash
cd backend
mvn spring-boot:run          # 开发模式
# 或 mvn -DskipTests package && java -jar target/focusly-pomodoro-0.0.1-SNAPSHOT.jar
```

健康检查：访问 <http://localhost:8080/api/timer/config>

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "workDuration": 25,
    "breakDuration": 5
  }
}
```

### Step 4 · 启动前端 :5173

```bash
cd ../frontend
npm install                  # 首次请先装依赖
npm run dev                  # Vite dev 服务器
```

访问 <http://localhost:5173/>，你应该能看到：

```
┌──────────────────────────────────────┐
│ 🍅 Focusly 轻番茄 · 专注每一刻  导航…│
├──────────────────────────────────────┤
│          番茄专注计时                │
│        [专注][休息]                  │
│          ╭──────╮                    │
│          │ 25:00│  已暂停             │
│          ╰──────╯                    │
│    [开始] [暂停] [重置] [⚙️设置]     │
│                                      │
│    ⏱ 今日专注 0 分钟                 │
└──────────────────────────────────────┘
```

### Step 5 · （可选）生产构建

```bash
# 前端 — 输出到 frontend/dist/，交给 nginx 或 Spring static
cd frontend && npm run build    # 706 modules，约 650ms

# 后端 — 可执行 jar
cd backend && mvn -DskipTests package
java -jar target/focusly-pomodoro-0.0.1-SNAPSHOT.jar --server.port=8080
```

---

## 📁 项目目录结构

```
focusly/
├── README.md                   # 你正在看的这份
├── docs/
│   ├── API.md                  # 完整 REST API 契约文档（19 端点 × 请求/响应/错误码）
│   └── DESIGN.md               # 设计文档（数据模型/业务流程/写安全/防漂移算法/降级策略）
├── frontend/
│   ├── README.md               # 前端架构、设计 tokens、组件 API、响应式断点
│   ├── package.json            # Vue 3.5 / Vite 8 / Pinia / ECharts 6 / vue-tsc / TypeScript 5.7
│   ├── vite.config.ts          # @ 别名 → src；/api 代理 → localhost:8080
│   ├── tsconfig.json           # Strict = true；types 包含 vite/node
│   ├── index.html              # lang=zh-CN + theme-color 晴空蓝 + SEO description
│   └── src/
│       ├── main.ts             # createApp + pinia + router
│       ├── App.vue             # RouterView
│       ├── style.css           # 全局设计系统 tokens（颜色/圆角/阴影/按钮）
│       ├── router/index.ts     # 4 个懒加载路由 + MetaLayout
│       ├── layouts/MainLayout.vue  # 顶部胶囊导航 + <768px 汉堡菜单抽屉
│       ├── views/              # 4 个核心视图（Timer / Task / Checkin / Stats）
│       ├── components/StatsChart.vue  # ECharts 4 类型图表（props 驱动+自动 resize）
│       ├── composables/useCountdown.ts   # 无漂移倒计时（绝对时间戳算法）
│       ├── stores/             # 4 个 Pinia stores（timer/task/checkin/stats）
│       └── api/                # axios 实例 + 6 个业务 API 文件（与后端一一对应）
├── backend/
│   ├── README.md               # 后端分层、接口速查、异常映射、安全项
│   ├── pom.xml                 # Spring Boot 3.5 + MyBatis-Plus 3.5 + Lombok + MySQL CJ
│   └── src/main/
│       ├── resources/application.yml   # 端口 8080 / Hikari CP / MyBatis-Plus / 日志
│       └── java/com/focusly/pomodoro/
│           ├── FocuslyApplication.java
│           ├── common/         # Result 壳 + GlobalExceptionHandler（6 种异常映射）
│           ├── config/WebMvcConfig.java    # CORS: http://localhost:5173
│           ├── controller/     # 5 个 @RestController（Task/Session/Checkin/Stats/TimerConfig）
│           ├── service/        # 5 个 @Service 业务层
│           ├── mapper/         # 4 个 MyBatis-Plus BaseMapper<T> 泛型接口
│           ├── entity/         # 4 个 @TableName 实体（Task/Session/Checkin/TimerConfig）
│           └── dto/            # request ×5 + response ×3（参数校验注解已覆盖）
└── sql/
    ├── README.md               # 建库顺序、数据字典、字段映射到前端
    └── schema.sql              # 4 张表 DDL + 默认 25/5 TimerConfig 行
```

---

## 🧪 快速自测清单（验收交付标准）

运行完 Step 1-4 后，请逐条对勾：

- [ ] <http://localhost:5173/timer> 显示 25:00 + 模式切换按钮
- [ ] "设置" 改为 `专注=1 / 休息=1` → 保存后表盘立刻变为 01:00
- [ ] 开启 1 分钟专注，点"开始"→切到别的标签 2 分钟 → 切回来，状态应**直接跳转完成弹框**（不漂移）
- [ ] `/task` 添加任务「背单词」→ 复选框勾选 → 显示完成删除线
- [ ] 点"设为当前专注"→ 任务卡片显示「当前专注」嫩绿 tag
- [ ] 完成 STUDY 模式 → `/checkin` 今日专注分钟数应增加
- [ ] 打卡成功 → 按钮切换"今日已打卡"；再点按钮无反应（前端禁用 + 后端 409 双保护）
- [ ] `/stats` 近 7 天柱状图正确显示最近 7 天数据，切到环形分布图不报错
- [ ] 关闭后端 8080 → 所有页面**仍然正常显示**（无白屏 / 控制台 0 红色报错），stats 页显示空态图
- [ ] 刷新 `/stats` 或 `/task` → **不 404 / 不白屏**（SPA History Fallback）

---

## 🔧 常见问题 & 排错（Troubleshooting）

<details>
<summary>Q1. Maven 依赖下载失败 Network is unreachable</summary>

请在 `~/.m2/settings.xml`（Windows 为 `%USERPROFILE%\.m2\settings.xml`）配置代理与阿里云镜像：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <proxies>
    <proxy>
      <active>true</active><protocol>https</protocol>
      <host>127.0.0.1</host><port>18080</port>
      <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
    </proxy>
  </proxies>
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

</details>

<details>
<summary>Q2. MySQL 连接异常 Unknown system variable 'characterEncoding' / SQLException</summary>

原因：MySQL 9.x 的 CJ 驱动对 JDBC URL `characterEncoding=utf8mb4` 不兼容。本项目已修复，正确写法：

```
jdbc:mysql://127.0.0.1:3306/focusly?useUnicode=true&characterEncoding=UTF-8
```

（字符集 utf8mb4 仍会在连接建立后通过 `SET NAMES utf8mb4` 正确生效，两码事。）

</details>

<details>
<summary>Q3. 首次启动后端所有 select 报错：Unknown column 'deleted' in 'where clause'</summary>

原因：旧版 `application.yml` 启用了 logic-delete 但数据库没加列。**P0-3 已修复**：确保当前 `application.yml` 的 `logic-delete-field` 三行是注释状态。若仍报错，清理构建缓存：

```bash
cd backend && mvn clean && mvn compile
```

</details>

<details>
<summary>Q4. 前端 F5 刷新子路由（/task、/stats）出现 404</summary>

开发环境 Vite 已内建 History Fallback，没问题。生产环境 nginx 需要加：

```nginx
location / {
  try_files $uri $uri/ /index.html;
}
```

</details>

<details>
<summary>Q5. axios 错误只看到"网络异常"，看不到真实 HTTP 状态</summary>

**P1-已修复**：`http.ts` 错误拦截器现在会拼接真实信息，例如：

```
HTTP 500 Internal Server Error - 后端：SQL 语法错误 - (/stats/week)
[ERR_NETWORK] - (/tasks)
HTTP 404 Not Found - (/checkins/not-exist)
[ECONNABORTED] - (/timer/config)
```

</details>

<details>
<summary>Q6. StatsView 的 ECharts 包太大（203KB gzip）</summary>

现状是按需注册（已比整包省 60%+）。需要进一步优化可把 `StatsChart.vue` 改为动态导入：

```ts
const StatsChart = defineAsyncComponent(() => import('../components/StatsChart.vue'))
```

这样首次进入 Timer/Task/Checkin 不下载 ECharts。

</details>

---

## 🧭 文档索引

| 文档 | 内容 | 适合读者 |
|---|---|---|
| 👉 [根 README.md](README.md) | 总览、架构图、环境、启动、排错 | 第一次接触本项目的人 |
| [frontend/README.md](frontend/README.md) | 前端项目结构 / 设计 Tokens / 组件 API / 响应式断点 / 路由 | 前端工程师 & UI 设计师 |
| [backend/README.md](backend/README.md) | 后端分层 / 接口速查 / 数据模型 / 业务规则 / 安全项 | 后端工程师 & 运维 |
| [sql/README.md](sql/README.md) | 建库脚本说明 / 数据字典 / 字段含义 / 初始化顺序 | DBA & 全栈 |
| [docs/API.md](docs/API.md) | 完整 REST 契约（19 端点 × 请求/响应/错误码/字段约束）| 联调对接 & 测试 |
| [docs/DESIGN.md](docs/DESIGN.md) | 需求落地映射 / 写安全策略 / 防漂移算法 / 降级策略 / 图表类型映射 | 架构师 & 代码 Reviewer |

---

## 🪪 License & 致谢

MIT License © Focusly Team

- UI 灵感：晴空蓝 `#3b82f6` + 暖米白 `#fefbf4` 学生风配色，长时间使用不刺眼。
- 番茄工作法：Francesco Cirillo 1980 年代提出的 25/5 节奏。
