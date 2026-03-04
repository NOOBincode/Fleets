package org.example.fleets.message.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.fleets.message.model.dto.NotificationDTO;
import org.example.fleets.websocket.service.WebSocketService;
import org.springframework.stereotype.Component;

/**
 * 系统通知消费者：消费 MQ 后推送到目标用户 WebSocket（/user/queue/notifications）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "im-notification-topic",
    consumerGroup = "notification-consumer-group"
)
public class NotificationConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;

    @Override
    public void onMessage(String notificationJson) {
        NotificationDTO dto;
        try {
            dto = objectMapper.readValue(notificationJson, NotificationDTO.class);
        } catch (JsonProcessingException e) {
            log.error("通知 JSON 解析失败: notificationJson={}", notificationJson, e);
            return;
        }
        if (dto == null || dto.getUserId() == null) {
            log.warn("通知无效（缺少 userId），跳过: dto={}", dto);
            return;
        }

        try {
            webSocketService.sendNotification(dto.getUserId(), dto);
            log.debug("已推送系统通知: userId={}, type={}", dto.getUserId(), dto.getType());
        } catch (Exception e) {
            log.error("推送通知失败: userId={}, type={}", dto.getUserId(), dto.getType(), e);
        }
    }
}
