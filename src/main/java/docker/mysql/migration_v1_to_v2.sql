-- ========================================
-- 数据库优化迁移脚本 v1 -> v2
-- 执行前请备份数据库！
-- ========================================

USE fleets;

-- ========================================
-- 1. 修改 conversation 表
-- ========================================
ALTER TABLE `conversation` 
  -- 修改 last_message_id 类型（bigint -> varchar）
  MODIFY COLUMN `last_message_id` varchar(64) DEFAULT NULL COMMENT '最后一条消息ID（MongoDB ObjectId）',
  
  -- 添加冗余字段
  ADD COLUMN `last_message_content` varchar(500) DEFAULT NULL COMMENT '最后一条消息内容（冗余）' AFTER `last_message_id`,
  ADD COLUMN `last_sender_id` bigint(20) DEFAULT NULL COMMENT '最后一条消息发送者ID' AFTER `last_message_content`,
  ADD COLUMN `last_sender_name` varchar(64) DEFAULT NULL COMMENT '最后一条消息发送者昵称（冗余）' AFTER `last_sender_id`;

-- 添加复合索引
ALTER TABLE `conversation`
  ADD KEY `idx_owner_time` (`owner_id`, `last_message_time` DESC),
  ADD KEY `idx_owner_top_time` (`owner_id`, `is_top` DESC, `last_message_time` DESC);

-- ========================================
-- 2. 修改 friendship 表
-- ========================================
ALTER TABLE `friendship`
  -- 添加分组字段
  ADD COLUMN `group_name` varchar(64) DEFAULT '我的好友' COMMENT '好友分组' AFTER `remark`;

-- 添加分组索引
ALTER TABLE `friendship`
  ADD KEY `idx_user_group` (`user_id`, `group_name`);

-- ========================================
-- 3. 修改 group 表
-- ========================================
ALTER TABLE `group`
  -- 统一命名：name -> group_name
  CHANGE COLUMN `name` `group_name` varchar(64) NOT NULL COMMENT '群组名称',
  
  -- 添加群公告字段
  ADD COLUMN `announcement` varchar(500) DEFAULT NULL COMMENT '群公告' AFTER `description`,
  
  -- 添加加群方式字段
  ADD COLUMN `join_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '加群方式：0-无需验证，1-需要验证，2-禁止加群' AFTER `current_member_count`;

-- 添加群名搜索索引
ALTER TABLE `group`
  ADD KEY `idx_group_name` (`group_name`);

-- ========================================
-- 4. 修改 group_member 表
-- ========================================
ALTER TABLE `group_member`
  -- 统一命名：nickname -> group_nickname
  CHANGE COLUMN `nickname` `group_nickname` varchar(64) DEFAULT NULL COMMENT '群内昵称',
  
  -- 添加禁言状态字段
  ADD COLUMN `mute_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '禁言状态：0-正常，1-禁言' AFTER `role`;

-- 添加角色索引
ALTER TABLE `group_member`
  ADD KEY `idx_group_role` (`group_id`, `role`);

-- ========================================
-- 验证修改结果
-- ========================================
SHOW CREATE TABLE conversation;
SHOW CREATE TABLE friendship;
SHOW CREATE TABLE `group`;
SHOW CREATE TABLE group_member;

-- ========================================
-- 完成！
-- ========================================
