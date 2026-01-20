package org.example.fleets.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
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
        
        log.warn("WebSocket 握手拦截器未实现");
        return false;
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
