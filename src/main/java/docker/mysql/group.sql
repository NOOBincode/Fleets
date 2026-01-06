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