package org.example.fleets.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    
    /**
     * 注册Sa-Token拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token拦截器，校验规则为StpUtil.checkLogin()登录校验
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 指定需要拦截的路径
            SaRouter.match("/**")
                // 排除不需要登录的路径
                .notMatch("/api/user/register")
                .notMatch("/api/user/login")
                .notMatch("/ws/**")
                .notMatch("/error")
                .notMatch("/favicon.ico")
                // 执行登录校验
                .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
