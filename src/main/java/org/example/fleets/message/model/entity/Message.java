package org.example.fleets.message.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 消息实体类（存储在MongoDB）
 */
@Data
@Document(collection = "message")
public class Message {
    @Id
    private String id;
    
    // 消息类型：1-单聊 2-群聊
    private Integer messageType;
    
    // 内容类型：1-文本 2-图片 3-语音 4-视频 5-文件
    private Integer contentType;
    
    // 发送者ID
    private Long senderId;
    
    // 接收者ID（单聊时使用）
    private Long receiverId;
    
    // 群组ID（群聊时使用）
    private Long groupId;
    
    // 消息内容
    private String content;
    
    // 消息序列号
    private Long sequence;
    
    // 消息状态：0-发送中 1-已发送 2-已送达 3-已读 4-撤回
    private Integer status;
    
    // 发送时间
    private Date sendTime;
    
    // 扩展信息（JSON格式）
    private String extra;
}
