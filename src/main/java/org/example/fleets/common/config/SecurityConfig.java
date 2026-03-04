package org.example.fleets.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全配置类
 * 提供密码加密等安全相关的Bean
 */
@Configuration
public class SecurityConfig {
    
    /**
     * 密码加密器
     * 使用BCrypt算法进行密码加密
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
