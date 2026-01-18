package org.example.fleets.mailbox.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 同步消息DTO
 */
@Data
public class SyncMessageDTO {
    
    @NotNull(message = "会话ID不能为空")
    private String conversationId;
    
    @NotNull(message = "起始序列号不能为空")
    private Long fromSequence;
    
    // 每页数量（默认100）
    private Integer pageSize;
}
