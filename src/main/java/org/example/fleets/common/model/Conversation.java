package org.example.fleets.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 会话实体类（连接 MySQL 和 MongoDB 的桥梁）
 */
@Data
@TableName("conversation")
public class Conversation {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    // 会话唯一标识（单聊：conv_小ID_大ID，群聊：conv_group_群ID）
    private String conversationId;
    
    // 会话类型：0-单聊，1-群聊
    private Integer type;
    
    // 会话所有者ID（每个用户都有自己的会话列表）
    private Long ownerId;
    
    // 目标ID（单聊时是对方用户ID，群聊时是群组ID）
    private Long targetId;
    
    // 未读消息数
    private Integer unreadCount;
    
    // ========== 关键：连接到 MongoDB ==========
    // 最后一条消息ID（MongoDB 的 _id，ObjectId 字符串）
    private String lastMessageId;
    
    // 最后一条消息内容（冗余存储，方便显示会话列表）
    private String lastMessageContent;
    
    // 最后一条消息时间（用于排序）
    @TableField("last_message_time")
    private Date lastMessageTime;
    
    // 最后一条消息发送者ID
    private Long lastSenderId;
    
    // 最后一条消息发送者昵称（冗余存储）
    private String lastSenderName;
    // ==========================================
    
    // 是否置顶：0-否，1-是
    private Integer isTop;
    
    // 是否免打扰：0-否，1-是
    private Integer isMute;
    
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField("update_time")
    private Date updateTime;
}
