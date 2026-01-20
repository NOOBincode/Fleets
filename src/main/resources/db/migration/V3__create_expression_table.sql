-- 创建表情包表
CREATE TABLE IF NOT EXISTS `expression` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NULL COMMENT '用户ID，NULL表示系统表情',
    `name` VARCHAR(50) NOT NULL COMMENT '表情名称',
    `url` VARCHAR(500) NOT NULL COMMENT '表情图片URL',
    `category` VARCHAR(50) DEFAULT 'default' COMMENT '分类：emoji/custom/system',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表情包表';

-- 插入系统表情包（示例）
INSERT INTO `expression` (`id`, `user_id`, `name`, `url`, `category`, `sort`) VALUES
(1, NULL, '微笑', 'emoji/smile.png', 'emoji', 1),
(2, NULL, '大笑', 'emoji/laugh.png', 'emoji', 2),
(3, NULL, '爱心', 'emoji/heart.png', 'emoji', 3),
(4, NULL, '点赞', 'emoji/thumbs-up.png', 'emoji', 4),
(5, NULL, '加油', 'emoji/muscle.png', 'emoji', 5);
