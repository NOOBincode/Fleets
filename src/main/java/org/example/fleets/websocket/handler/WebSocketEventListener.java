package org.example.fleets.websocket.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.websocket.service.UserOnlineService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * WebSocket 事件监听器
 * 监听连接、断开、订阅等事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    
    private final UserOnlineService userOnlineService;
    
    /**
     * 连接建立事件
     * TODO: 处理用户上线
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // TODO: 从事件中获取 sessionId 和 userId
        // TODO: 调用 userOnlineService.userOnline()
        
        log.info("WebSocket 连接建立事件");
    }
    
    /**
     * 连接断开事件
     * TODO: 处理用户离线
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // TODO: 从事件中获取 sessionId 和 userId
        // TODO: 调用 userOnlineService.userOffline()
        
        log.info("WebSocket 连接断开事件");
    }
    
    /**
     * 订阅事件
     * TODO: 处理用户订阅（可选）
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        // TODO: 记录用户订阅的 destination
        
        log.debug("WebSocket 订阅事件");
    }
}
