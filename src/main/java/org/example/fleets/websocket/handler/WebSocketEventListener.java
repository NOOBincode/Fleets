package org.example.fleets.websocket.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.websocket.service.UserOnlineService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

/**
 * WebSocket 事件监听器
 * 监听连接、断开、订阅等事件，与 UserOnlineService 联动维护在线状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserOnlineService userOnlineService;

    /**
     * 连接建立事件：从 Principal 取 userId，记录上线
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();
        if (sessionId == null || principal == null || !principal.getName().matches("\\d+")) {
            log.warn("WebSocket 连接建立但无法解析 userId: sessionId={}, principal={}", sessionId, principal != null ? principal.getName() : null);
            return;
        }
        Long userId = Long.parseLong(principal.getName());
        userOnlineService.userOnline(userId, sessionId);
        log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 连接断开事件：从 Principal 或 Redis 反查 userId，记录离线
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Long userId = null;
        Principal principal = accessor.getUser();
        if (principal != null && principal.getName().matches("\\d+")) {
            userId = Long.parseLong(principal.getName());
        }
        if (userId == null && sessionId != null) {
            userId = userOnlineService.getUserIdBySessionId(sessionId);
        }
        if (userId != null && sessionId != null) {
            userOnlineService.userOffline(userId, sessionId);
            log.info("WebSocket 连接断开: userId={}, sessionId={}", userId, sessionId);
        } else {
            log.warn("WebSocket 连接断开但无法解析 userId: sessionId={}", sessionId);
        }
    }

    /**
     * 订阅事件（可选，仅打日志）
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        log.debug("WebSocket 订阅: destination={}, sessionId={}", destination, accessor.getSessionId());
    }
}
