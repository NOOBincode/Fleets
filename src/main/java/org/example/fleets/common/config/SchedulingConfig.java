package org.example.fleets.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 * 
 * 启用 Spring 定时任务功能
 * 用于：
 * - 消息重试
 * - 超时检查
 * - 数据清理
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 启用定时任务
}
