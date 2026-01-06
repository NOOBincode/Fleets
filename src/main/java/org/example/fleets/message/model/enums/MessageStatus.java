package org.example.fleets.message.model.enums;

import lombok.Getter;

/**
 * 消息状态枚举
 */
@Getter
public enum MessageStatus {
    
    SENDING(0, "发送中"),
    SENT(1, "已发送"),
    DELIVERED(2, "已送达"),
    READ(3, "已读"),
    RECALLED(4, "已撤回"),
    FAILED(5, "发送失败");
    
    private final Integer code;
    private final String desc;
    
    MessageStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
