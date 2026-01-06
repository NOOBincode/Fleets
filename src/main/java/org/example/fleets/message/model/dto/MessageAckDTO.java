package org.example.fleets.message.model.dto;

import lombok.Data;

/**
 * 消息确认DTO
 */
@Data
public class MessageAckDTO {
    
    // 消息ID
    private String messageId;
    
    // 确认类型：1-送达ACK, 2-已读ACK
    private Integer ackType;
    
    // 确认时间
    private Long timestamp;
}
