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
     * TODO: 实现心跳处理
     */
    @MessageMapping("/heartbeat")
    public void heartbeat(SimpMessageHeaderAccessor headerAccessor) {
        // TODO: 从 headerAccessor 获取 userId
        // TODO: 调用 userOnlineService.refreshOnlineStatus()
        
        log.debug("收到心跳");
    }
    
    /**
     * 客户端发送消息（可选，也可以直接用 HTTP API）
     * 客户端发送: /app/send
     * TODO: 实现客户端消息处理（可选）
     */
    @MessageMapping("/send")
    public void sendMessage(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        // TODO: 获取 userId
        // TODO: 处理消息（通常建议使用 HTTP API 发送消息）
        
        log.info("收到客户端消息: {}", message);
    }
    
    /**
     * 输入状态通知（正在输入...）
     * 客户端发送: /app/typing
     * TODO: 实现输入状态广播（可选）
     */
    @MessageMapping("/typing")
    public void typing(@Payload TypingMessage typingMessage, SimpMessageHeaderAccessor headerAccessor) {
        // TODO: 获取 userId
        // TODO: 广播给对方用户
        
        log.debug("用户正在输入: conversationId={}", typingMessage.getConversationId());
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
