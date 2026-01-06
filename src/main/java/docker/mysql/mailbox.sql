CREATE TABLE `mailbox` (
                           `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '信箱ID',
                           `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                           `sequence_id` bigint(20) NOT NULL COMMENT '序列ID',
                           `message_id` bigint(20) NOT NULL COMMENT '消息ID',
                           `conversation_id` varchar(64) NOT NULL COMMENT '会话ID',
                           `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读：0-未读，1-已读',
                           `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `idx_user_sequence` (`user_id`,`sequence_id`),
                           KEY `idx_user_conversation` (`user_id`,`conversation_id`),
                           KEY `idx_message_id` (`message_id`),
                           KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信箱表';