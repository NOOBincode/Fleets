package org.example.fleets.mailbox.model.vo;

import lombok.Data;

import java.util.Map;

/**
 * 未读数VO
 */
@Data
public class UnreadCountVO {
    
    // 总未读数
    private Integer totalUnread;
    
    // 各会话的未读数
    // Key: conversationId, Value: unreadCount
    private Map<String, Integer> conversationUnread;
}
