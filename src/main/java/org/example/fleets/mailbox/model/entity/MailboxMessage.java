package org.example.fleets.mailbox.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 信箱消息实体
 * 存储每个用户收到的消息（带序列号）
 */
@Data
@Document(collection = "mailbox_message")
public class MailboxMessage {
    
    @Id
    private String id;
    
    // 关联的信箱ID
    private String mailboxId;
    
    // 接收者用户ID
    private Long userId;
    
    // 会话ID
    private String conversationId;
    
    // 该用户在该会话中的序列号
    private Long sequence;
    
    // 消息ID（关联message表）
    private String messageId;
    
    // 发送者ID
    private Long senderId;
    
    // 消息类型：1-单聊，2-群聊
    private Integer messageType;
    
    // 内容类型：1-文本，2-图片，3-语音，4-视频，5-文件
    private Integer contentType;
    
    // 消息内容（冗余存储，提高查询效率）
    private String content;
    
    // 状态：0-未读，1-已读，2-已删除
    private Integer status;
    
    // 发送时间
    private Date sendTime;
    
    // 阅读时间
    private Date readTime;
    
    // 创建时间
    private Date createTime;
    
    // 过期时间（7天后自动删除）
    private Date expireTime;
}
