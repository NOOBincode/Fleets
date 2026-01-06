package org.example.fleets.common.config;

import org.example.fleets.common.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花算法配置类
 */
@Configuration
public class SnowflakeConfig {
    
    @Value("${snowflake.worker-id:1}")
    private long workerId;
    
    @Value("${snowflake.datacenter-id:1}")
    private long datacenterId;
    
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(workerId, datacenterId);
    }
}
