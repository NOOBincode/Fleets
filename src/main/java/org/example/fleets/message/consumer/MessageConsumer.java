package org.example.fleets.message.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消息消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "im-message-topic",
    consumerGroup = "im-message-consumer-group"
)
public class MessageConsumer implements RocketMQListener<String> {
    
    @Override
    public void onMessage(String message) {
        log.info("收到消息: {}", message);
        // TODO: 实现消息处理逻辑
        // 1. 解析消息
        // 2. 推送给在线用户
        // 3. 存储离线消息
    }
}
