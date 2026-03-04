package org.example.fleets.message.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.fleets.group.service.GroupService;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.websocket.service.WebSocketService;
import org.springframework.stereotype.Component;

/**
 * 在线消息推送消费者：消费 IM 消息并推送到 WebSocket（单聊/群聊）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "im-message-topic",
    consumerGroup = "online-push-consumer-group"
)
public class MessageConsumer implements RocketMQListener<String> {

    private static final int MESSAGE_TYPE_SINGLE = 1;
    private static final int MESSAGE_TYPE_GROUP = 2;

    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;
    private final GroupService groupService;

    @Override
    public void onMessage(String messageJson) {
        Message message;
        try {
            message = objectMapper.readValue(messageJson, Message.class);
        } catch (JsonProcessingException e) {
            log.error("消息 JSON 解析失败: messageJson={}", messageJson, e);
            return;
        }
        if (message == null || message.getId() == null) {
            log.warn("消息无效，跳过: message={}", message);
            return;
        }

        Integer messageType = message.getMessageType();
        if (messageType == null) {
            log.warn("消息类型为空，跳过: messageId={}", message.getId());
            return;
        }

        try {
            if (messageType == MESSAGE_TYPE_SINGLE) {
                if (message.getReceiverId() == null) {
                    log.warn("单聊消息缺少 receiverId，跳过: messageId={}", message.getId());
                    return;
                }
                webSocketService.sendMessageToUser(message.getReceiverId(), message);
                log.debug("已推送单聊消息: messageId={}, receiverId={}", message.getId(), message.getReceiverId());
            } else if (messageType == MESSAGE_TYPE_GROUP) {
                if (message.getGroupId() == null) {
                    log.warn("群聊消息缺少 groupId，跳过: messageId={}", message.getId());
                    return;
                }
                webSocketService.sendMessageToGroup(message.getGroupId(), message);
                log.debug("已推送群聊消息: messageId={}, groupId={}", message.getId(), message.getGroupId());
            } else {
                log.warn("未知消息类型，跳过: messageId={}, messageType={}", message.getId(), messageType);
            }
        } catch (Exception e) {
            log.error("推送消息失败: messageId={}", message.getId(), e);
        }
    }
}
