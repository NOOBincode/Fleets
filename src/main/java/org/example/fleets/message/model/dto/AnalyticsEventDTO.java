package org.example.fleets.message.model.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 统计/行为事件 MQ 消息体（Producer 发到 im-analytics-topic，Consumer 解析后打点或入库）
 */
@Data
public class AnalyticsEventDTO {

    /** 事件类型：如 message_send、login、session_open 等 */
    private String eventType;

    /** 用户 ID（可选） */
    private Long userId;

    /** 事件时间 */
    private Date timestamp;

    /** 扩展维度（如 conversationId、messageType） */
    private Map<String, Object> payload;
}
