-- ============================================================================
-- MomentKeep 数据库结构（全量 DDL）
-- ============================================================================
-- 用途：新环境一键建库。仓库原本只有增量脚本 upgrade_2026-09-18.sql，无法从零重建。
-- 生成方式：读取 TiDB Cloud 现有库的 SHOW CREATE TABLE 导出
-- 执行方式：mysql -h <host> -P 4000 -u <user> -p < schema.sql
-- 注意：已去掉 AUTO_INCREMENT 计数器值，避免与新环境的自增起点冲突。
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `momentkeep` DEFAULT CHARSET utf8mb4;
USE `momentkeep`;

-- ---------- admin ----------
CREATE TABLE `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ADMIN',
  `permissions` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT '0',
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  UNIQUE KEY `idx_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- ai_chat ----------
CREATE TABLE `ai_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `messages` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_ai_chat_user_time` (`user_id`,`create_time`),
  CONSTRAINT `fk_ai_chat_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- ai_chat_quota ----------
CREATE TABLE `ai_chat_quota` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `quota_date` date NOT NULL COMMENT '配额所属自然日（北京时间）',
  `used_count` int NOT NULL DEFAULT '0' COMMENT '当日已用次数',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  UNIQUE KEY `uk_ai_quota_user_date` (`user_id`,`quota_date`),
  KEY `idx_ai_quota_date` (`quota_date`)
) ENGINE=InnoDB COMMENT='AI 对话每日配额';

-- ---------- checkin ----------
CREATE TABLE `checkin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checkin_time` datetime NOT NULL,
  `meal_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exercise_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `custom_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT '0',
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_checkin_time` (`checkin_time`),
  KEY `idx_type` (`type`),
  KEY `idx_user_time` (`user_id`,`checkin_time`),
  KEY `idx_checkin_user_time` (`user_id`,`checkin_time`),
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- countdown ----------
CREATE TABLE `countdown` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_time` datetime NOT NULL,
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '#3498db',
  `sort_order` int DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT '0',
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_target_time` (`target_time`),
  KEY `idx_user_target` (`user_id`,`target_time`),
  KEY `idx_countdown_user` (`user_id`,`target_time`),
  CONSTRAINT `fk_countdown_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- feedback ----------
CREATE TABLE `feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint DEFAULT '0',
  `reply` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT '0',
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- focus_record ----------
CREATE TABLE `focus_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计时模式（stopwatch/countdown/pomodoro）',
  `duration` int NOT NULL COMMENT '专注时长（秒）',
  `todo_id` bigint NOT NULL COMMENT '关联待办ID',
  `todo_title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联待办标题',
  `start_time` datetime NOT NULL,
  `end_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT '0',
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  KEY `idx_focus_user_id` (`user_id`),
  KEY `idx_focus_start_time` (`start_time`),
  KEY `idx_focus_mode` (`mode`),
  KEY `idx_focus_todo_id` (`todo_id`),
  KEY `idx_focus_user_time` (`user_id`,`start_time`),
  CONSTRAINT `fk_focus_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_focus_todo` FOREIGN KEY (`todo_id`) REFERENCES `todo` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- todo ----------
CREATE TABLE `todo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` tinyint DEFAULT '0',
  `completed` tinyint(1) DEFAULT '0',
  `completed_time` datetime DEFAULT NULL,
  `completion_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT '0',
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_completed` (`completed`),
  KEY `idx_todo_user` (`user_id`,`completed`),
  CONSTRAINT `fk_todo_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- user ----------
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint DEFAULT '1',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'USER',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT '0',
  `token_version` int NOT NULL DEFAULT '0' COMMENT '令牌版本号，登出/改密/注销时自增',
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  UNIQUE KEY `idx_username` (`username`),
  UNIQUE KEY `idx_email` (`email`),
  UNIQUE KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- user_setting ----------
CREATE TABLE `user_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `theme` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'light',
  `background_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ai_auto_fill` tinyint(1) DEFAULT '1',
  `notifications` tinyint(1) DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  UNIQUE KEY `idx_user_id` (`user_id`),
  UNIQUE KEY `uk_setting_user` (`user_id`),
  CONSTRAINT `fk_setting_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

