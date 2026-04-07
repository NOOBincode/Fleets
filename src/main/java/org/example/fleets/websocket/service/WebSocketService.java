package org.example.fleets.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.websocket.model.dto.OnlineStatusDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void sendMessageToUser(Long userId, MessageVO message) {

        String dest = "/queue/messages";
        messagingTemplate.convertAndSendToUser(String.valueOf(userId),dest,message);
        log.debug("推送消息到用户: userId={}, messageId={}", userId, message.getId());
    }
    
    public void sendMessageToAllSessions(Long userId, MessageVO message) {


        String dest = "/queue/messages";
        messagingTemplate.convertAndSendToUser(String.valueOf(userId),dest,message);
        log.debug("推送消息到所有会话: userId={}, messageId={}", userId, message.getId());
    }
    
    public void sendMessageToGroup(Long groupId, MessageVO message) {

        String dest = "/topic/group/" + groupId;
        messagingTemplate.convertAndSend(dest,message);
        
        log.debug("广播消息到群组: groupId={}, messageId={}", groupId, message.getId());
    }
    
    public void sendNotification(Long userId, Object notification) {
        String dest = "/queue/notifications";
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), dest, notification);
        log.debug("推送通知: userId={}", userId);
    }
    
    public void sendOnlineStatusChange(Long userId, boolean online) {

        String dest = "/topic/online-status";
        messagingTemplate.convertAndSend(dest, new OnlineStatusDTO(userId, online));
        
        log.debug("广播在线状态: userId={}, online={}", userId, online);
    }
}
