# Focusly · 后端文档

> Spring Boot 3.5 + MyBatis-Plus 3.5 + MySQL 9.x · 分层清晰 · 写安全兜底

---

## 📦 技术栈（精确版本）

| 依赖 | 版本 | 说明 |
|---|---|---|
| **Spring Boot** | 3.5.0 | BOM 统一管理版本 |
| spring-boot-starter-web | 3.5.0 | Servlet Stack + Jackson |
| spring-boot-starter-validation | 3.5.0 | `@Valid` + Jakarta Validation 3 |
| **MyBatis-Plus** | 3.5.12 | `mybatis-plus-spring-boot3-starter`（不是老的 starter） |
| **mysql-connector-j** | 8.x（由 BOM 管） | MySQL 9.x 也兼容（`characterEncoding=UTF-8`，见下） |
| **Lombok** | 1.x（由 BOM 管） | `@Data` / `@RequiredArgsConstructor`，**显式在 compiler-plugin 配置为 annotationProcessorPaths**（JDK 25 必需） |
| JDK | 25（release=25） | `<java.version>25</java.version>` |

---

## 🏗️ 后端分层架构

```
com.focusly.pomodoro/
├── FocuslyApplication.java              ← 启动类（@SpringBootApplication）
│
├── common/                               ← 公共横切层
│   ├── Result.java                      ← 统一响应壳 { code, msg, data }
│   └── GlobalExceptionHandler.java      ← @RestControllerAdvice，6 类异常 → Result.fail
│
├── config/
│   └── WebMvcConfig.java                ← CORS：允许 http://localhost:5173；可扩展拦截器
│
├── controller/  (@RestController)       ← REST 端点（薄 Controller，只做路由+调用 service）
│   ├── TimerConfigController            ← /api/timer/config       [GET / PUT]
│   ├── TaskController                   ← /api/tasks              [GET / POST / PUT{id} / DELETE{id} / DELETE]
│   ├── SessionController                ← /api/sessions           [POST / GET]
│   ├── CheckinController                ← /api/checkins           [POST /today /dates /streak]
│   └── StatsController                  ← /api/stats              [GET /week /month]
│
├── service/     (@Service)              ← 业务层（唯一允许写业务逻辑的地方）
│   ├── TimerConfigService               ← 配置读写，空表兜底（P0-修复：永不返回 null）
│   ├── TaskService                      ← 任务 CRUD，空标题校验
│   ├── SessionService                   ← 会话落库 + 今日分钟回传
│   ├── CheckinService                   ← 打卡（业务层先查重，DB UNIQUE 再兜底）+ 连续天数
│   └── StatsService                     ← 7/30 天聚合（左连日历，缺日补 0）
│
├── mapper/      (BaseMapper<T>)         ← 泛型 CRUD，本项目零手写 XML
│   ├── TimerConfigMapper
│   ├── TaskMapper
│   ├── SessionMapper
│   └── CheckinMapper
│
├── entity/      (@TableName)            ← 与 schema.sql 字段一一对应，underscore→camel 自动
│   ├── TimerConfig                      ← timer_config 表
│   ├── Task                             ← task 表
│   ├── Session                          ← pomodoro_session 表
│   └── Checkin                          ← daily_checkin 表
│
└── dto/
    ├── request/  (@NotBlank / @Min / @Max)   ← Controller 入参校验
    │   ├── TimerConfigRequest                 work[1..180] / break[1..60]
    │   ├── TaskCreateRequest                  title 非空 100 字 / desc 500 字
    │   ├── TaskUpdateRequest                  title / desc / status ∈ {0,1}
    │   └── SessionSaveRequest                 sessionType / startTime / endTime / durationMinutes[1..600] / taskId
    └── response/
        ├── DayStat                            { date: string, minutes: int }
        ├── SessionSaveResult                  { sessionId, todayFocusMinutes }
        └── StatsBundle                        { daily7[], daily30[], weeklyTotal, monthlyTotal }
```

**分层原则（严禁违反）**：
1. **Controller 层只负责三件事**：路由 / 校验（@Valid）/ 调用 service。不允许手写聚合 SQL、不允许出现 if/else 业务分支。
2. **所有写安全校验下沉到 Service**（即使前端做了，也必须在 service 再判断一次）。
3. **Mapper 层零手写 SQL**。本项目业务简单，全靠 QueryWrapper / LambdaQueryWrapper。真要加 XML 请放 `resources/mapper/` 并在 application.yml 配 `mapper-locations`。

---

## 📡 接口速查（19 端点 · 完整契约见 [docs/API.md](../docs/API.md)）

| # | 方法 | 路径 | 入参 | 成功返回 | 典型错误 |
|---|---|---|---|---|---|
| 1 | GET | `/api/timer/config` | — | `TimerConfig` | 500（空表已兜底，正常不会） |
| 2 | PUT | `/api/timer/config` | body: `TimerConfigRequest` | 更新后的 `TimerConfig` | 400（范围/空值） |
| 3 | GET | `/api/tasks` | — | `Task[]`（按创建时间 DESC） | — |
| 4 | POST | `/api/tasks` | body: `TaskCreateRequest` | 新建 `Task` | 400（title 空/超长） |
| 5 | PUT | `/api/tasks/{id}` | body: `TaskUpdateRequest` | 更新后的 `Task` | 400（状态非法/超长）；500（id 不存在） |
| 6 | DELETE | `/api/tasks/{id}` | path: id | `{}` | 500（id 不存在） |
| 7 | DELETE | `/api/tasks` | — | `{}`（清空全表，Controller 已无二次确认，**前端必须弹确认再调用**） | — |
| 8 | POST | `/api/sessions` | body: `SessionSaveRequest` | `SessionSaveResult{sessionId, todayFocusMinutes}` | 400（字段校验） |
| 9 | GET | `/api/sessions` | query: `startDate?, endDate?`（YYYY-MM-DD 闭区间） | `Session[]` | — |
| 10 | POST | `/api/checkins` | — | 新建 `Checkin` | **409（今日已打卡 · 三层兜底）** |
| 11 | GET | `/api/checkins/today` | — | `{ checkedIn, todayMinutes }` | — |
| 12 | GET | `/api/checkins/dates` | query: `days=30` | `string[]`（ISO 日期） | — |
| 13 | GET | `/api/checkins/streak` | — | `{ streak }` | — |
| 14 | GET | `/api/stats` | — | `StatsBundle`（综合包） | — |
| 15 | GET | `/api/stats/week` | — | `DayStat[7]`（含补 0） | — |
| 16 | GET | `/api/stats/month` | — | `DayStat[30]`（含补 0） | — |

> 所有接口统一前缀 `/api`，生产 nginx：`location /api { proxy_pass http://127.0.0.1:8080; }`

---

## 🔐 写安全（Write Safety）6 条策略

> 写操作 = 会修改数据库状态的请求：POST / PUT / DELETE。后端必须层层拦截、层层兜底。

| # | 策略 | 落地位置 | 说明 |
|---|---|---|---|
| 1 | **入参校验（第 1 层）** | DTO 字段注解 + Controller `@Valid` | `@NotBlank` 禁止空标题、`@Min@Max` 限制专注时长 1-180 / 休息 1-60 / 会话时长 1-600 / status ∈ {0,1}。失败 → code=400 不进入业务 |
| 2 | **业务重复检查（第 2 层）** | CheckinService.doCheckin() 先 SELECT | 打卡前 `lambdaQuery().eq(Checkin::getCheckinDate, today).count() > 0` → 抛 `IllegalStateException` → code=409 |
| 3 | **DB UNIQUE 约束（第 3 层兜底）** | `daily_checkin.uk_checkin_date` | 即使有人绕过前端+service 并发双写，DB 兜底拒绝重复 → `DuplicateKeyException` → GlobalExceptionHandler → code=409 |
| 4 | **空表兜底** | TimerConfigService.getConfig() | 表为空 → 返回 `new TimerConfig(work=25, break=5)`，保证 `GET /timer/config` 永不返回 null（前端不再需要判空）。真正更新时若 id=0 再 INSERT |
| 5 | **字段默认值** | schema.sql 的 `NOT NULL DEFAULT xxx` | `work_duration DEFAULT 25` / `status DEFAULT 0` / `focus_total_minutes DEFAULT 0`，即使漏传 DTO 字段也不产生 NULL |
| 6 | **最小权限账号** | application.yml 使用 `focusly` 用户（不是 root） | GRANT 仅 `focusly.*`，禁止 DROP / ALTER。生产环境进一步收缩为只允许 SELECT/INSERT/UPDATE/DELETE |

---

## 🔁 Result 壳 & 异常映射

### Result<T> 结构

```json
{ "code": 0,   "msg": "ok",                          "data": { ... } }    // 成功
{ "code": 400, "msg": "参数错误: 学习时长至少 1 分钟", "data": null }       // 业务失败
```

GlobalExceptionHandler 映射表（精确 code）：

| 异常类 | HTTP 200 + body.code | msg |
|---|---|---|
| `MethodArgumentNotValidException` | **400** | `参数错误: <所有字段默认消息用 ; 拼接>` |
| `IllegalArgumentException` | **400** | `e.getMessage()` |
| `IllegalStateException` | **409** | `e.getMessage()`（业务状态冲突，如重复打卡） |
| `DuplicateKeyException` | **409** | `"数据重复，今日已打卡"`（UNIQUE 兜底） |
| 其他 `Exception` | **500** | `"服务器内部错误"`（**堆栈只打 log，不泄漏给前端**） |

---

## ⚙️ 配置文件详解（`application.yml`）

```yaml
server:
  port: 8080                             # 前端 Vite 代理 target 必须对应

spring:
  datasource:
    # ⚠️ MySQL 9.x CJ 驱动：characterEncoding=UTF-8（写 utf8mb4 会报错 Unknown system variable）
    # 字符集 utf8mb4 通过 SET NAMES utf8mb4（连接建立后自动由驱动协商）
    url: jdbc:mysql://127.0.0.1:3306/focusly?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: focusly
    password: focusly123
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:                               # HikariCP 连接池（Spring Boot 默认）
      maximum-pool-size: 10               # 学生项目 10 连接足够；生产按并发调
      minimum-idle: 2
      connection-timeout: 30000           # 30s 获取不到连接就抛
      idle-timeout: 600000                # 10min 空闲回收
      max-lifetime: 1800000               # 30min 强制过期，避免与 MySQL wait_timeout 不一致
  jackson:
    property-naming-strategy: LOWER_CAMEL_CASE   # 与前端字段驼峰一致
    default-property-inclusion: non_null         # null 字段不序列化，减小 payload

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl   # 开发环境打印 SQL（排查写安全问题）；生产替换为 NO_LOGGING
    map-underscore-to-camel-case: true                       # DB 下划线 ↔ Java 驼峰 自动
  global-config:
    db-config:
      id-type: AUTO                          # 与 schema.sql AUTO_INCREMENT 对齐
      # ⚠️ 已修复（P0-3）：当前 4 张表没有 deleted 列，以下三行必须保持注释！
      # logic-delete-field: deleted
      # logic-delete-value: 1
      # logic-not-delete-value: 0

logging:
  level:
    com.focusly: DEBUG                      # 生产调回 INFO
```

### 配置 Maven 镜像与代理

> 国内/沙箱环境 Maven 中央仓库经常超时 `Network is unreachable`。

方法 1（项目内）：本项目已提供 `backend/settings.xml` 示例。运行时指定：

```bash
cd backend
mvn -s settings.xml spring-boot:run
```

方法 2（全局）：复制到 `~/.m2/settings.xml`，包含两部分：
- `<mirrors>` 阿里云公共仓库：`https://maven.aliyun.com/repository/public`
- `<proxies>` 公司/沙箱代理（按需填 host/port）

---

## 🧩 核心业务规则（Service 层 · 文档即代码）

### 1. TimerConfigService — 配置空表兜底

```
getConfig():
  1. selectById(1) or lambdaQuery().last("LIMIT 1")
  2. if == null: return new TimerConfig(work=25, break=5)   # 永不返 null
  3. else: return it

updateConfig(work, break):
  1. [1..180] / [1..60] 校验（DTO 已做，但 service 再保险一次）
  2. existing = getConfig()
  3. if existing.getId() == null: insert new row
  4. else: updateById(id, work, break)
  5. return getConfig()
```

### 2. SessionService — 会话落库

```
save(req: SessionSaveRequest):
  1. 仅当 sessionType == 'WORK' 才计入专注；'BREAK' 也落库但不加总
  2. durationMinutes = req.durationMinutes （前端保证实际分钟数，不按 end-start 差，避免时区误差）
  3. insert pomodoro_session → sessionId
  4. todayFocusMinutes = 按当天 DATE(start_time) SUM(duration_minutes) WHERE type='WORK'
  5. return { sessionId, todayFocusMinutes }

list(startDate?, endDate?):
  LambdaQueryWrapper.between(if non-null, Session::getStartTime, startDate 00:00, endDate 23:59:59)
  → ORDER BY start_time DESC
```

### 3. CheckinService — 打卡 & 连续天数

```
doCheckin():
  1. today = LocalDate.now(Asia/Shanghai)
  2. 第 2 层兜底：if exists(select * from daily_checkin where checkin_date=today):
       throw IllegalStateException("今日已打卡")   → 409
  3. todayMinutes = SUM(sessions of today where WORK)
  4. insert daily_checkin(checkin_date, focus_total_minutes)
     → 触发第 3 层 UNIQUE uk_checkin_date 兜底（高并发双写）
  5. return newCheckin

getStreak():
  1. list = 查 daily_checkin 全表 ORDER BY checkin_date DESC，取出 date 集合 S
  2. 今天是否打卡？
       · 已打卡：cur = today, streak = 0
       · 未打卡：cur = today - 1 day, streak = 0   # 允许"昨天往前算"（核心语义）
  3. while S contains cur: streak++, cur -= 1 day
  4. return streak
```

### 4. StatsService — 7/30 天聚合（缺日补 0）

```
week():   return aggregateLast(7)
month():  return aggregateLast(30)

aggregateLast(n):
  1. endDate   = today
     startDate = today - (n-1) days
  2. sessions  = SELECT DATE(start_time) AS day, SUM(duration_minutes) AS mins
                 WHERE session_type='WORK' AND DATE BETWEEN startDate AND endDate
                 GROUP BY DATE(start_time)     →  Map<LocalDate, Integer>
  3. 构建长度 n 的 DayStat[]：
       for i = 0..n-1:
         d = startDate + i days
         mins = map.getOrDefault(d, 0)
         list.add( DayStat(d.toISO, mins) )
  4. return list   # 保证旧→新排序，前端 xAxis 直接用
```

---

## 🧪 开发脚本

```bash
cd backend

# 编译（验证 Lombok 注解处理器 + 泛型类型）
mvn compile

# 启动
mvn spring-boot:run                     # 用默认 ~/.m2/settings.xml
mvn -s settings.xml spring-boot:run     # 用项目自带镜像配置（国内环境）

# 打包可执行 jar
mvn -DskipTests package
java -jar target/focusly-pomodoro-0.0.1-SNAPSHOT.jar

# 健康检查
curl http://localhost:8080/api/timer/config
# 期望 { "code":0,"msg":"ok","data":{"id":1,"workDuration":25,"breakDuration":5,"updatedAt":"..."}}
```

---

## ❓ 后端常见问题

<details>
<summary>Q1. Maven 编译报错找不到符号：Lombok @Data 的 getter/setter？</summary>

JDK 25 下必须在 `maven-compiler-plugin` 的 `<annotationProcessorPaths>` 显式声明 Lombok（不是只写 `<optional>true`）。本项目 pom.xml 已配置好。若仍报错，执行：

```bash
cd backend && mvn clean compile
```

</details>

<details>
<summary>Q2. 所有 SELECT 都报 Unknown column 'deleted' in 'where clause'？</summary>

application.yml 的 `logic-delete-field: deleted` 没有注释。本项目**未启用**逻辑删除（4 张表都没有 deleted 列）。确认以下三行是注释：

```yaml
# logic-delete-field: deleted
# logic-delete-value: 1
# logic-not-delete-value: 0
```

然后 `mvn clean compile` 清缓存重启。

</details>

<details>
<summary>Q3. 连接 MySQL 报 Unknown system variable 'characterEncoding'？</summary>

MySQL 9.x 的 JDBC URL 不要写 `characterEncoding=utf8mb4`，写 `characterEncoding=UTF-8`（详见根 README Q2）。utf8mb4 字符集会通过 `SET NAMES` 在连接建立后由驱动自动协商好。

</details>

<details>
<summary>Q4. MyBatis-Plus SQL 日志太多看不清？</summary>

开发环境 `StdOutImpl` 是为了便于排查写安全问题。生产环境把 `log-impl` 改为 `org.apache.ibatis.logging.nologging.NoLoggingImpl` 即可。

</details>

<details>
<summary>Q5. `PUT /tasks/{id}` 传了空标题 → 400 还是 500？</summary>

400。`TaskUpdateRequest` 的 `title` 字段加了 `@Size(max = 100)`（没加 `@NotBlank` 是因为允许**不传**以保持原字段）。但如果传了 `title=""` 或全空白，**Service.update() 方法会做二次校验**：

```java
if (req.getTitle() != null && req.getTitle().isBlank())
    throw new IllegalArgumentException("任务名称不能为空");
```

这种 service 层主动抛 `IllegalArgumentException` → 会被 GlobalExceptionHandler 捕获 → code=400。

</details>

<details>
<summary>Q6. 连续打卡天数 streak 怎么计算？今天没打卡会断吗？</summary>

不会立即断。算法核心（CheckinService.getStreak）：

```
cur = 今天是否已打卡 ? 今天 : 昨天
streak = 0
while cur ∈ 打卡日期集合: streak++, cur = cur - 1 day
return streak
```

即：今天还没打卡，可以「延续昨天的连续」（用户体验友好，避免白天 streak=0 影响激励）。等今天过了还没打，明天自然会从 streak-1 开始。

</details>
