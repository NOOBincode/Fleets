package org.example.fleets.message.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.message.model.dto.NotificationDTO;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 系统通知：投递到 {@code im-notification-topic}，由 {@link org.example.fleets.message.consumer.NotificationConsumer} 推 WebSocket。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final String TOPIC_NOTIFICATION = "im-notification-topic";

    private final MessageProducer messageProducer;
    private final ObjectMapper objectMapper;

    public void publish(NotificationDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            log.warn("跳过无效系统通知: dto={}", dto);
            return;
        }
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(new Date());
        }
        try {
            String json = objectMapper.writeValueAsString(dto);
            messageProducer.sendMessage(TOPIC_NOTIFICATION, json);
        } catch (Exception e) {
            log.error("系统通知投递 MQ 失败: userId={}, type={}", dto.getUserId(), dto.getType(), e);
        }
    }
}
