package org.example.fleets.message.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.message.model.dto.AnalyticsEventDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 统计/行为事件消费者：解析事件后打日志并写入 Redis 计数（按事件类型+日期），便于后续报表或活跃度分析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "im-analytics-topic",
    consumerGroup = "analytics-consumer-group"
)
public class AnalyticsConsumer implements RocketMQListener<String> {

    private static final String KEY_PREFIX = "analytics:count:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ObjectMapper objectMapper;
    private final RedisService redisService;

    @Override
    public void onMessage(String messageJson) {
        AnalyticsEventDTO dto;
        try {
            dto = objectMapper.readValue(messageJson, AnalyticsEventDTO.class);
        } catch (JsonProcessingException e) {
            log.error("统计事件 JSON 解析失败: messageJson={}", messageJson, e);
            return;
        }
        if (dto == null || dto.getEventType() == null || dto.getEventType().isEmpty()) {
            log.warn("统计事件无效（缺少 eventType），跳过: dto={}", dto);
            return;
        }

        String date = LocalDate.now().format(DATE_FMT);
        String key = KEY_PREFIX + dto.getEventType() + ":" + date;
        try {
            redisService.increment(key);
            log.debug("统计事件: eventType={}, userId={}, key={}", dto.getEventType(), dto.getUserId(), key);
        } catch (Exception e) {
            log.error("统计事件处理失败: eventType={}, key={}", dto.getEventType(), key, e);
        }
    }
}
