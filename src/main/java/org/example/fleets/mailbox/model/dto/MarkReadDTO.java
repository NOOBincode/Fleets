package org.example.fleets.mailbox.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 标记已读DTO
 */
@Data
public class MarkReadDTO {
    
    @NotNull(message = "会话ID不能为空")
    private String conversationId;
    
    // 单个序列号（标记单条消息）
    private Long sequence;
    
    // 截止序列号（批量标记到该序列号）
    private Long toSequence;
}
