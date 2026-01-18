package org.example.fleets.mailbox.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 用户信箱实体
 * 存储每个用户在每个会话中的元数据
 */
@Data
@Document(collection = "user_mailbox")
public class UserMailbox {
    
    @Id
    private String id;
    
    // 用户ID
    private Long userId;
    
    // 会话ID（单聊：user_A_B，群聊：group_789）
    private String conversationId;
    
    // 会话类型：1-单聊，2-群聊
    private Integer conversationType;
    
    // 当前会话的最大序列号
    private Long sequence;
    
    // 最后一条消息ID
    private String lastMessageId;
    
    // 最后一条消息时间
    private Date lastMessageTime;
    
    // 未读消息数
    private Integer unreadCount;
    
    // 创建时间
    private Date createTime;
    
    // 更新时间
    private Date updateTime;
}
