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