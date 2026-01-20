package org.example.fleets.message.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 系统通知消费者
 * 
 * TODO: 实现系统通知处理
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
    
    @Override
    public void onMessage(String notificationJson) {
        log.info("收到系统通知: {}", notificationJson);
        
        // TODO: 实现通知处理逻辑
        // 1. 解析通知
        // 2. 保存通知到数据库
        // 3. 推送给目标用户
        
        log.warn("系统通知处理功能未实现");
    }
}
