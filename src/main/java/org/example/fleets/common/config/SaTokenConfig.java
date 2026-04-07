package org.example.fleets.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;

/**
 * Sa-Token 配置类
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    
    /**
     * 注册Sa-Token拦截器
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 注册Sa-Token拦截器，校验规则为 StpUtil.checkLogin() 登录校验
        // 注意：前端路由 /login、/register 仅用于展示登录/注册页，本身不需要携带 token，因此在这里放行
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 对浏览器的 CORS 预检请求（OPTIONS）直接放行，避免因未登录导致预检失败
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    return;
                }
            }

            SaRouter.match("/**")
                .notMatch("/api/user/register")
                .notMatch("/api/user/login")
                .notMatch("/api/user/logout")
                .notMatch("/ws/**")
                .notMatch("/files/**")
                .notMatch("/error")
                .notMatch("/favicon.ico")
                .notMatch("/login")
                .notMatch("/register")
                .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");

        // 在 Sa-Token 校验之后，将登录用户 ID 写入 request，供控制器使用
        registry.addInterceptor(new UserIdAttributeInterceptor()).addPathPatterns("/**");
    }
}
