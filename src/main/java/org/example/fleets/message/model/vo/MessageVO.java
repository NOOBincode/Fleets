package org.example.fleets.message.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 消息VO
 */
@Data
public class MessageVO {
    
    private String id;
    
    private Integer messageType;  // 1-单聊，2-群聊
    
    private Integer contentType;  // 1-文本，2-图片，3-语音，4-视频，5-文件
    
    private Long senderId;
    
    private String senderNickname;
    
    private String senderAvatar;
    
    private Long receiverId;
    
    private Long groupId;
    
    private String content;
    
    private Long sequence;
    
    private Integer status;  // 0-发送中，1-已发送，2-已送达，3-已读，4-撤回
    
    private Date sendTime;
    
    private String extra;
}
