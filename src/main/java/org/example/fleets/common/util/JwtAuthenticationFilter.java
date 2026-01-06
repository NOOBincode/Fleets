package org.example.fleets.common.util;

import org.example.fleets.common.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(jwtUtils.getHeader());

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(jwtUtils.getTokenHead())) {
            // 从authHeader中提取JWT token
            String token = jwtUtils.getTokenFromFullToken(authHeader);

            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                // token有效，设置用户认证信息到上下文
                Long userId = jwtUtils.getUserIdFromToken(token);
                String username = jwtUtils.getUsernameFromToken(token);

                // 将用户信息存入请求属性，供后续使用
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
            }
        }

        filterChain.doFilter(request, response);
    }
}