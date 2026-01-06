-- ========================================
-- Fleets IM 数据库初始化脚本（优化版）
-- 执行方式：
-- 1. docker exec -i mysql mysql -uroot -proot < init_database.sql
-- 2. 或在 MySQL 客户端中：source init_database.sql
-- ========================================

-- 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS fleets;

-- 创建数据库
CREATE DATABASE fleets DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE fleets;

-- ========================================
-- 1. 用户表
-- ========================================
CREATE TABLE `user` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(64) NOT NULL COMMENT '用户名',
    `password` varchar(128) NOT NULL COMMENT '密码（加密存储）',
    `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
    `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
    `gender` tinyint(1) DEFAULT '0' COMMENT '性别：0-未知，1-男，2-女',
    `birth_date` date DEFAULT NULL COMMENT '出生日期',
    `signature` varchar(255) DEFAULT NULL COMMENT '个性签名',
    `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(64) DEFAULT NULL COMMENT '最后登录IP',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_username` (`username`),
    KEY `idx_phone` (`phone`),
    KEY `idx_email` (`email`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================
-- 2. 好友关系表（优化版）
-- ========================================
CREATE TABLE `friendship` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关系ID',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `friend_id` bigint(20) NOT NULL COMMENT '好友ID',
    `remark` varchar(64) DEFAULT NULL COMMENT '好友备注',
    `group_name` varchar(64) DEFAULT '我的好友' COMMENT '好友分组',
    `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：0-待确认，1-已确认，2-已拒绝，3-已拉黑',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_user_friend` (`user_id`,`friend_id`),
    KEY `idx_friend_id` (`friend_id`),
    KEY `idx_user_group` (`user_id`, `group_name`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- ========================================
-- 3. 群组表（优化版）
-- ========================================
CREATE TABLE `group` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '群组ID',
    `group_name` varchar(64) NOT NULL COMMENT '群组名称',
    `avatar` varchar(255) DEFAULT NULL COMMENT '群头像URL',
    `description` varchar(255) DEFAULT NULL COMMENT '群描述',
    `announcement` varchar(500) DEFAULT NULL COMMENT '群公告',
    `owner_id` bigint(20) NOT NULL COMMENT '群主ID',
    `max_member_count` int(11) NOT NULL DEFAULT '200' COMMENT '最大成员数',
    `current_member_count` int(11) NOT NULL DEFAULT '1' COMMENT '当前成员数',
    `join_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '加群方式：0-无需验证，1-需要验证，2-禁止加群',
    `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_group_name` (`group_name`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

-- ========================================
-- 4. 群组成员表（优化版）
-- ========================================
CREATE TABLE `group_member` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `group_id` bigint(20) NOT NULL COMMENT '群组ID',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `group_nickname` varchar(64) DEFAULT NULL COMMENT '群内昵称',
    `role` tinyint(1) NOT NULL DEFAULT '0' COMMENT '角色：0-普通成员，1-管理员，2-群主',
    `mute_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '禁言状态：0-正常，1-禁言',
    `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `mute_end_time` datetime DEFAULT NULL COMMENT '禁言结束时间',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_group_user` (`group_id`,`user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_group_role` (`group_id`, `role`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组成员表';

-- ========================================
-- 5. 会话表（优化版 - 连接 MySQL 和 MongoDB 的桥梁）
-- ========================================
CREATE TABLE `conversation` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `conversation_id` varchar(64) NOT NULL COMMENT '会话唯一标识',
    `type` tinyint(1) NOT NULL COMMENT '会话类型：0-单聊，1-群聊',
    `owner_id` bigint(20) NOT NULL COMMENT '会话所有者ID',
    `target_id` bigint(20) NOT NULL COMMENT '目标ID（用户ID或群组ID）',
    `unread_count` int(11) NOT NULL DEFAULT '0' COMMENT '未读消息数',
    `last_message_id` varchar(64) DEFAULT NULL COMMENT '最后一条消息ID（MongoDB ObjectId）',
    `last_message_content` varchar(500) DEFAULT NULL COMMENT '最后一条消息内容（冗余）',
    `last_message_time` datetime DEFAULT NULL COMMENT '最后一条消息时间',
    `last_sender_id` bigint(20) DEFAULT NULL COMMENT '最后一条消息发送者ID',
    `last_sender_name` varchar(64) DEFAULT NULL COMMENT '最后一条消息发送者昵称（冗余）',
    `is_top` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否置顶：0-否，1-是',
    `is_mute` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否免打扰：0-否，1-是',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_conversation_owner` (`conversation_id`,`owner_id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_target_id` (`target_id`),
    KEY `idx_owner_time` (`owner_id`, `last_message_time`),
    KEY `idx_owner_top_time` (`owner_id`, `is_top`, `last_message_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- ========================================
-- 6. 文件表
-- ========================================
CREATE TABLE `file` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `user_id` bigint(20) NOT NULL COMMENT '上传用户ID',
    `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
    `file_name` varchar(255) NOT NULL COMMENT '存储文件名',
    `file_path` varchar(255) NOT NULL COMMENT '文件路径',
    `file_size` bigint(20) NOT NULL COMMENT '文件大小(字节)',
    `file_type` varchar(64) NOT NULL COMMENT '文件类型',
    `file_md5` varchar(32) NOT NULL COMMENT '文件MD5值',
    `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-上传中，1-已上传，2-上传失败',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_md5` (`file_md5`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- ========================================
-- 注意：消息序列号使用雪花算法生成，不再需要 sequence 表
-- 雪花算法优点：
-- 1. 高性能：每秒可生成400万个ID
-- 2. 趋势递增：ID按时间递增
-- 3. 分布式：不同机器生成的ID不会重复
-- 4. 无需数据库：不依赖数据库，性能更好
-- ========================================

-- ========================================
-- 完成！
-- ========================================
SELECT '数据库初始化完成！' AS message;
SHOW TABLES;
