package org.example.fleets.message.producer;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 消息生产者
 */
@Component
public class MessageProducer {
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    /**
     * 发送消息到RocketMQ
     */
    public void sendMessage(String topic, Object message) {
        rocketMQTemplate.convertAndSend(topic, message);
    }
    
    /**
     * 发送带标签的消息
     */
    public void sendMessageWithTag(String topic, String tag, Object message) {
        String destination = topic + ":" + tag;
        rocketMQTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送同步消息
     */
    public void sendSyncMessage(String topic, Object message) {
        rocketMQTemplate.syncSend(topic, MessageBuilder.withPayload(message).build());
    }
    
    /**
     * 发送异步消息
     */
    public void sendAsyncMessage(String topic, Object message) {
        rocketMQTemplate.asyncSend(topic, MessageBuilder.withPayload(message).build(), null);
    }
}
