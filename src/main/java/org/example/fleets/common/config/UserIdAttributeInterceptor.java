package org.example.fleets.common.config;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 将当前登录用户 ID 写入 request 属性，供控制器通过 request.getAttribute("userId") 获取。
 * 需在 Sa-Token 校验之后执行，故注册顺序应在 SaInterceptor 之后。
 */
public class UserIdAttributeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (StpUtil.isLogin()) {
            request.setAttribute("userId", StpUtil.getLoginIdAsLong());
        }
        return true;
    }
}
