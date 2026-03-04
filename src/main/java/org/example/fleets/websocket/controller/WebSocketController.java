package org.example.fleets.websocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.websocket.service.UserOnlineService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 消息处理控制器
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    
    private final UserOnlineService userOnlineService;
    /**
     * 心跳检测
     * 客户端发送: /app/heartbeat
     */
    @MessageMapping("/heartbeat")
    public void heartbeat(SimpMessageHeaderAccessor headerAccessor) {
        Long userId = resolveUserId(headerAccessor);
        if (userId == null) {
            log.warn("心跳请求无法解析 userId");
            return;
        }
        userOnlineService.refreshOnlineStatus(userId);
        log.debug("收到心跳: userId={}", userId);
    }

    /**
     * 从 STOMP 会话中解析当前用户 ID（Principal.name 即 userId）
     */
    private Long resolveUserId(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor == null) {
            return null;
        }
        java.security.Principal principal = headerAccessor.getUser();
        if (principal == null || !principal.getName().matches("\\d+")) {
            return null;
        }
        return Long.parseLong(principal.getName());
    }
    
    /**
     * 客户端发送消息（可选，通常用 HTTP API 发消息）
     * 客户端发送: /app/send
     */
    @MessageMapping("/send")
    public void sendMessage(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = resolveUserId(headerAccessor);
        log.info("收到客户端消息: userId={}, message={}", userId, message);
    }
    
    /**
     * 输入状态通知（正在输入...）
     * 客户端发送: /app/typing
     */
    @MessageMapping("/typing")
    public void typing(@Payload TypingMessage typingMessage, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = resolveUserId(headerAccessor);
        log.debug("用户正在输入: userId={}, conversationId={}", userId, typingMessage != null ? typingMessage.getConversationId() : null);
    }
    
    /**
     * 输入状态消息
     */
    public static class TypingMessage {
        private String conversationId;
        private boolean typing;
        
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        
        public boolean isTyping() { return typing; }
        public void setTyping(boolean typing) { this.typing = typing; }
    }
}
