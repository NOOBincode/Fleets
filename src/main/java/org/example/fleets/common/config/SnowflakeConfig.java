package org.example.fleets.common.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花算法配置类 - 使用Hutool
 */
@Configuration
public class SnowflakeConfig {
    
    @Value("${snowflake.workerId:1}")
    private long workerId;
    
    @Value("${snowflake.datacenterId:1}")
    private long datacenterId;
    
    @Bean
    public Snowflake snowflake() {
        return IdUtil.getSnowflake(workerId, datacenterId);
    }
}
