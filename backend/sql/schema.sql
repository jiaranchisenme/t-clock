-- Focusly 番茄时钟数据库 schema
-- 字符集：utf8mb4 + unicode_ci（支持 emoji 与中文排序）
-- 引擎：InnoDB（支持事务、外键、行锁）
-- 写安全原则：
--   1) DEFAULT 兜底，避免 NULL 渗入业务逻辑
--   2) UNIQUE 约束兜底防重复（即使前端被绕过）
--   3) 外键级联，避免孤儿数据
--   4) 索引覆盖统计聚合查询

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 表 1：timer_config 计时器配置（单用户场景，单行设计）
DROP TABLE IF EXISTS `timer_config`;
CREATE TABLE `timer_config` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `work_duration`  INT          NOT NULL DEFAULT 25              COMMENT '学习时长(分钟)',
  `break_duration` INT          NOT NULL DEFAULT 5               COMMENT '休息时长(分钟)',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计时器配置';

-- 表 2：task 学习任务
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `title`       VARCHAR(100) NOT NULL                            COMMENT '任务名称(禁止空)',
  `description` VARCHAR(500) DEFAULT NULL                        COMMENT '简要描述',
  `status`      TINYINT      NOT NULL DEFAULT 0                  COMMENT '0=未完成 1=已完成',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习任务';

-- 表 3：pomodoro_session 番茄会话明细
DROP TABLE IF EXISTS `pomodoro_session`;
CREATE TABLE `pomodoro_session` (
  `id`               BIGINT      NOT NULL AUTO_INCREMENT         COMMENT '主键',
  `session_type`     VARCHAR(10) NOT NULL                       COMMENT 'WORK/BREAK',
  `start_time`       DATETIME    NOT NULL                       COMMENT '会话开始时间',
  `end_time`         DATETIME    NOT NULL                       COMMENT '会话结束时间',
  `duration_minutes` INT         NOT NULL                       COMMENT '实际专注时长(分钟)',
  `task_id`          BIGINT      DEFAULT NULL                   COMMENT '关联任务(可空)',
  `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '落库时间',
  PRIMARY KEY (`id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_task_id` (`task_id`),
  CONSTRAINT `fk_session_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='番茄会话明细';

-- 表 4：daily_checkin 每日打卡
DROP TABLE IF EXISTS `daily_checkin`;
CREATE TABLE `daily_checkin` (
  `id`                  BIGINT  NOT NULL AUTO_INCREMENT           COMMENT '主键',
  `checkin_date`        DATE    NOT NULL                         COMMENT '打卡日期',
  `focus_total_minutes` INT     NOT NULL DEFAULT 0              COMMENT '当日专注总时长(分钟)',
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checkin_date` (`checkin_date`)                 -- 兜底防重复打卡
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日打卡';

-- 初始化默认数据：配置表写入一行默认 25/5（PRD 默认值）
INSERT INTO `timer_config` (`work_duration`, `break_duration`) VALUES (25, 5);

SET FOREIGN_KEY_CHECKS = 1;
