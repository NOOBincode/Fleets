package org.example.fleets.message.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 消息发送DTO
 */
@Data
public class MessageSendDTO {
    
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;  // 1-单聊，2-群聊
    
    @NotNull(message = "内容类型不能为空")
    private Integer contentType;  // 1-文本，2-图片，3-语音，4-视频，5-文件
    
    private Long receiverId;  // 单聊时使用
    
    private Long groupId;  // 群聊时使用
    
    @NotBlank(message = "消息内容不能为空")
    private String content;
    
    private String extra;  // 扩展信息（JSON格式）
}
