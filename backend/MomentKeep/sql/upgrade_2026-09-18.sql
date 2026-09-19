-- ============================================================================
-- MomentKeep 数据库升级脚本
-- 适用版本：本次安全与稳定性修复（令牌吊销 + AI 每日配额）
-- 执行环境：TiDB Cloud（MySQL 兼容）
-- 执行方式：在 TiDB Cloud 控制台的 SQL Editor 中逐段执行，或在部署机上用
--           mysql -h <host> -P 4000 -u <user> -p momentkeep < upgrade_2026-09-18.sql
-- 注意：脚本不可重复执行；若某列/表已存在请跳过对应语句。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1) 用户表新增令牌版本号
--    用途：JWT 无状态，登出/改密/注销后无法主动失效令牌。
--          通过自增该字段并与 JWT 中的 tv 声明比对，实现即时吊销，
--          避免为了黑名单再引入 Redis。
-- ---------------------------------------------------------------------------
ALTER TABLE `user`
    ADD COLUMN `token_version` INT NOT NULL DEFAULT 0 COMMENT '令牌版本号，登出/改密/注销时自增';

-- ---------------------------------------------------------------------------
-- 2) AI 对话每日配额表
--    用途：单用户每日 3 次的严格限额；对 quota_date 求和即为全局当日用量，
--          用于防止被批量注册的账号刷爆 DeepSeek 额度。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_quota`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `quota_date`  DATE     NOT NULL COMMENT '配额所属自然日（北京时间）',
    `used_count`  INT      NOT NULL DEFAULT 0 COMMENT '当日已用次数',
    `create_time` DATETIME          DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME          DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_quota_user_date` (`user_id`, `quota_date`),
    KEY `idx_ai_quota_date` (`quota_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 对话每日配额';

-- ---------------------------------------------------------------------------
-- 3) 索引：支撑所有按用户维度的查询
--    注意：这些语句此前是注释状态（从未执行），导致按 user_id 的区间查询
--          与时间分布聚合都是全表扫描。若索引已存在，执行会报 Duplicate key name，
--          属于预期情况，直接跳过即可。
-- ---------------------------------------------------------------------------
ALTER TABLE `checkin`      ADD INDEX `idx_checkin_user_time` (`user_id`, `checkin_time`);
ALTER TABLE `todo`         ADD INDEX `idx_todo_user` (`user_id`, `completed`);
ALTER TABLE `countdown`    ADD INDEX `idx_countdown_user` (`user_id`, `target_time`);
ALTER TABLE `focus_record` ADD INDEX `idx_focus_user_time` (`user_id`, `start_time`);
ALTER TABLE `ai_chat`      ADD INDEX `idx_ai_chat_user_time` (`user_id`, `create_time`);
ALTER TABLE `user_setting` ADD UNIQUE KEY `uk_setting_user` (`user_id`);
