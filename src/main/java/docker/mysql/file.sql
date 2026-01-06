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