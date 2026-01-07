package org.example.fleets.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    
    /**
     * 密码加密器（保留 BCrypt）
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 注册 Sa-Token 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/user/register",           // 用户注册
                        "/api/user/login",              // 用户登录
                        "/api/user/verify-code",        // 发送验证码
                        "/api/user/password/reset",     // 重置密码
                        "/api/user/check/**",           // 检查用户名/手机/邮箱是否存在
                        "/health",                      // 健康检查
                        "/ws/**",                       // WebSocket
                        "/error"                        // 错误页面
                );
    }
}
