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
     * TODO: 实现点对点消息推送
     * 提示：使用 messagingTemplate.convertAndSendToUser()
     */
    public void sendMessageToUser(Long userId, Message message) {
        // TODO: 检查用户是否在线
        // TODO: 推送到用户的私有队列 /user/queue/messages
        
        log.info("推送消息到用户: userId={}, messageId={}", userId, message.getId());
    }
    
    /**
     * 向指定用户的所有会话推送消息（多端同步）
     * TODO: 实现多端消息推送
     */
    public void sendMessageToAllSessions(Long userId, Message message) {
        // TODO: 获取用户的所有会话
        // TODO: 遍历推送到每个会话
        
        log.info("推送消息到所有会话: userId={}", userId);
    }
    
    /**
     * 广播消息到群组
     * TODO: 实现群组消息广播
     * 提示：使用 messagingTemplate.convertAndSend()
     */
    public void sendMessageToGroup(Long groupId, Message message) {
        // TODO: 推送到群组的公共主题 /topic/group/{groupId}
        
        log.info("广播消息到群组: groupId={}, messageId={}", groupId, message.getId());
    }
    
    /**
     * 发送系统通知
     * TODO: 实现系统通知推送
     */
    public void sendNotification(Long userId, Object notification) {
        // TODO: 检查用户是否在线
        // TODO: 推送到 /user/queue/notifications
        
        log.info("推送通知: userId={}", userId);
    }
    
    /**
     * 发送在线状态变更通知
     * TODO: 实现在线状态广播
     */
    public void sendOnlineStatusChange(Long userId, boolean online) {
        // TODO: 广播到 /topic/online-status
        
        log.info("广播在线状态: userId={}, online={}", userId, online);
    }
}
