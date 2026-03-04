package org.example.fleets.websocket.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 用于认证和设置用户信息
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private static final String ATTR_USER_ID = "userId";
    
    /**
     * 握手前
     * TODO: 实现 token 验证和用户信息设置
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                  ServerHttpResponse response,
                                  WebSocketHandler wsHandler, 
                                  Map<String, Object> attributes) throws Exception {
        
        // TODO: 从请求中获取 token
        // TODO: 验证 token
        // TODO: 将用户 ID 存入 attributes
        // TODO: 返回 true 允许握手，false 拒绝握手
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("websocket握手失败:未提供token");
            return false;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            Long userId = loginId instanceof Long ? (Long) loginId : Long.parseLong(String.valueOf(loginId));
            attributes.put(ATTR_USER_ID,userId);
            log.debug("websocket 握手成功: userId = {}",userId);
            return true;
        }catch (NotLoginException e){
            log.warn("websocket 握手失败:token 无效,{}",e.getMessage());
            return false;
        }
    }

    private String resolveToken(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (StringUtils.hasText(query)) {
            for (String param: query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6).trim();
                }
            }
        }

        String auth = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(auth)&&auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        String xToken = request.getHeaders().getFirst("X-Token");
        if (StringUtils.hasText(xToken)) {
            return xToken.trim();
        }
        return null;
    }
    
    /**
     * 握手后
     * TODO: 握手完成后的处理（可选）
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, 
                              ServerHttpResponse response,
                              WebSocketHandler wsHandler, 
                              Exception exception) {
        // TODO: 握手完成后的处理
    }
}
