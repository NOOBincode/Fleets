package org.example.fleets.common.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ID生成器配置
 * 使用Hutool的雪花算法
 */
@Configuration
public class IdGeneratorConfig {
    
    @Value("${snowflake.workerId:1}")
    private long workerId;
    
    @Value("${snowflake.datacenterId:1}")
    private long datacenterId;
    
    /**
     * 创建雪花算法ID生成器
     */
    @Bean
    public Snowflake snowflake() {
        return IdUtil.getSnowflake(workerId, datacenterId);
    }
}
