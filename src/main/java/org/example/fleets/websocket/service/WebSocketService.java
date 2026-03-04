package org.example.fleets.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.message.model.entity.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 消息推送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final UserOnlineService userOnlineService;
    
    /**
     * 向指定用户推送消息（点对点）
     * 提示：使用 messagingTemplate.convertAndSendToUser()
     */
    public void sendMessageToUser(Long userId, Message message) {

        String dest = "/queue/messages";
        messagingTemplate.convertAndSendToUser(String.valueOf(userId),dest,message);
        log.debug("推送消息到用户: userId={}, messageId={}", userId, message.getId());
    }
    
    /**
     * 向指定用户的所有会话推送消息（多端同步）
     * TODO: 实现多端消息推送
     */
    public void sendMessageToAllSessions(Long userId, Message message) {


        String dest = "/queue/messages";
        messagingTemplate.convertAndSendToUser(String.valueOf(userId),dest,message);
        log.debug("推送消息到所有会话: userId={}, messageId={}", userId, message.getId());
    }
    
    /**
     * 广播消息到群组
     * TODO: 实现群组消息广播
     * 提示：使用 messagingTemplate.convertAndSend()
     */
    public void sendMessageToGroup(Long groupId, Message message) {

        String dest = "/topic/group/" + groupId;
        messagingTemplate.convertAndSend(dest,message);
        
        log.debug("广播消息到群组: groupId={}, messageId={}", groupId, message.getId());
    }
    
    /**
     * 发送系统通知（客户端订阅 /user/queue/notifications）
     */
    public void sendNotification(Long userId, Object notification) {
        String dest = "/queue/notifications";
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), dest, notification);
        log.debug("推送通知: userId={}", userId);
    }
    
    /**
     * 发送在线状态变更通知
     * TODO: 实现在线状态广播
     */
    public void sendOnlineStatusChange(Long userId, boolean online) {

        String dest = "/topic/online-status";
        messagingTemplate.convertAndSend(dest,userId);
        
        log.debug("广播在线状态: userId={}, online={}", userId, online);
    }
}
