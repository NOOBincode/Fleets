package org.example.fleets.message.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 在线消息推送消费者
 * 
 * 依赖：
 * - WebSocketService: 推送消息给在线用户
 * - UserOnlineService: 检查用户在线状态
 * - GroupService: 获取群成员列表
 * 
 * TODO: 在实现此消费者前，请先完成：
 * 1. WebSocketService 实现
 * 2. UserOnlineService 实现
 * 3. GroupService.getGroupMemberIds() 实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "im-message-topic",
    consumerGroup = "online-push-consumer-group"
)
public class MessageConsumer implements RocketMQListener<String> {
    
    private final ObjectMapper objectMapper;
    // private final WebSocketService webSocketService;
    // private final UserOnlineService userOnlineService;
    // private final GroupService groupService;
    
    @Override
    public void onMessage(String messageJson) {
        log.info("收到消息推送任务: {}", messageJson);
        
        // TODO: 实现消息推送逻辑
        // 1. 解析消息
        // 2. 确定接收者（单聊/群聊）
        // 3. 过滤在线用户
        // 4. 通过 WebSocket 推送
        
        log.warn("消息推送功能未实现");
    }
}
