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