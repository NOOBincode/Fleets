package org.example.fleets.message.model.dto;

import lombok.Data;

import java.util.Date;

/**
 * 系统通知 MQ 消息体（Producer 发到 im-notification-topic，Consumer 解析后推 WebSocket）
 */
@Data
public class NotificationDTO {

    /** 目标用户 ID（接收通知的人） */
    private Long userId;

    /** 通知类型：如 friend_apply、friend_accept、group_invite、system 等 */
    private String type;

    /** 标题 */
    private String title;

    /** 正文 */
    private String content;

    /** 业务关联 ID（如申请单 ID、群 ID） */
    private String bizId;

    /** 扩展信息（JSON 或键值） */
    private String extra;

    /** 通知时间 */
    private Date timestamp;
}
