# 消息队列使用指南

## 概述

Fleets 使用 RocketMQ 作为消息队列，实现消息的异步推送和解耦。消息队列在整个消息流转中扮演关键角色。

## 消息流转架构

```
┌─────────────┐
│  发送者      │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────────────┐
│              MessageService.sendMessage()                │
│  1. 保存消息到 MongoDB (Message 集合)                     │
│  2. 写入 Mailbox (每个接收者的信箱)                       │
│  3. 更新会话表 MySQL (Conversation)                       │
│  4. 发送到 RocketMQ ✓                                    │
└──────┬──────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────┐
│                    RocketMQ Topic                        │
│              im-message-topic                            │
└──────┬──────────────────────────────────────────────────┘
       │
       ├──────────────────┬──────────────────┐
       ▼                  ▼                  ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│ Consumer 1  │   │ Consumer 2  │   │ Consumer 3  │
│ 在线推送     │   │ 离线存储     │   │ 统计分析     │
└─────────────┘   └─────────────┘   └─────────────┘
```

## 使用场景

### 1. 实时消息推送（在线用户）

**场景**：用户在线时，实时推送消息

**流程**：
```
发送者 → MessageService → RocketMQ → OnlinePushConsumer → WebSocket → 在线接收者
```

**实现**：
- Consumer 监听消息
- 检查接收者是否在线
- 通过 WebSocket 推送给在线用户

### 2. 离线消息存储

**场景**：用户离线时，消息已经存储在 Mailbox

**流程**：
```
发送者 → MessageService → Mailbox (已存储) → 用户上线 → 主动拉取
```

**说明**：
- Mailbox 在发送时已经写入
- 消息队列主要用于实时推送
- 离线用户上线后通过 API 拉取

### 3. 群聊消息分发

**场景**：群聊消息需要分发给多个成员

**流程**：
```
发送者 → MessageService → 批量写入 Mailbox → RocketMQ → 批量推送
```

**实现**：
- 获取群成员列表
- 批量写入每个成员的 Mailbox
- 发送到消息队列
- Consumer 批量推送给在线成员

### 4. 消息通知

**场景**：系统通知、好友申请、群邀请等

**流程**：
```
系统事件 → NotificationService → RocketMQ → NotificationConsumer → 推送
```

### 5. 消息统计分析

**场景**：消息量统计、用户活跃度分析

**流程**：
```
消息发送 → RocketMQ → AnalyticsConsumer → 统计数据库
```

## Topic 设计

### 主要 Topic

| Topic | 用途 | 消费者 |
|-------|------|--------|
| im-message-topic | 即时消息 | OnlinePushConsumer |
| im-notification-topic | 系统通知 | NotificationConsumer |
| im-group-message-topic | 群聊消息 | GroupMessageConsumer |
| im-analytics-topic | 消息统计 | AnalyticsConsumer |

### Tag 设计

```java
// im-message-topic 的 Tag
- SINGLE_CHAT: 单聊消息
- GROUP_CHAT: 群聊消息
- SYSTEM: 系统消息

// im-notification-topic 的 Tag
- FRIEND_REQUEST: 好友申请
- GROUP_INVITE: 群邀请
- SYSTEM_NOTICE: 系统公告
```

## 生产者使用

### 1. 基本发送

```java
@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageProducer messageProducer;
    
    public void sendMessage(Message message) {
        // 发送到默认 topic
        messageProducer.sendMessage("im-message-topic", message);
    }
}
```

### 2. 带 Tag 发送

```java
// 单聊消息
messageProducer.sendMessageWithTag("im-message-topic", "SINGLE_CHAT", message);

// 群聊消息
messageProducer.sendMessageWithTag("im-message-topic", "GROUP_CHAT", message);
```

### 3. 同步发送（需要确认）

```java
// 重要消息，需要确认发送成功
messageProducer.sendSyncMessage("im-message-topic", message);
```

### 4. 异步发送（高性能）

```java
// 普通消息，异步发送
messageProducer.sendAsyncMessage("im-message-topic", message);
```

## 消费者实现

### 1. 在线推送消费者

```java
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "im-message-topic",
    consumerGroup = "online-push-consumer-group",
    selectorExpression = "SINGLE_CHAT || GROUP_CHAT"
)
public class OnlinePushConsumer implements RocketMQListener<Message> {
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private UserOnlineService userOnlineService;
    
    @Override
    public void onMessage(Message message) {
        log.info("收到消息推送任务: messageId={}", message.getId());
        
        try {
            // 1. 确定接收者
            List<Long> receiverIds = getReceiverIds(message);
            
            // 2. 过滤在线用户
            List<Long> onlineUsers = receiverIds.stream()
                .filter(userOnlineService::isOnline)
                .collect(Collectors.toList());
            
            // 3. 推送给在线用户
            for (Long userId : onlineUsers) {
                webSocketService.sendMessage(userId, message);
                log.info("消息推送成功: userId={}, messageId={}", userId, message.getId());
            }
            
        } catch (Exception e) {
            log.error("消息推送失败: messageId={}", message.getId(), e);
            throw e; // 抛出异常，触发重试
        }
    }
    
    private List<Long> getReceiverIds(Message message) {
        if (message.getMessageType() == 1) {
            // 单聊
            return List.of(message.getReceiverId());
        } else {
            // 群聊：需要查询群成员
            return groupService.getGroupMemberIds(message.getGroupId());
        }
    }
}
```

### 2. 通知消费者

```java
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "im-notification-topic",
    consumerGroup = "notification-consumer-group"
)
public class NotificationConsumer implements RocketMQListener<Notification> {
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public void onMessage(Notification notification) {
        log.info("收到通知: type={}, userId={}", 
            notification.getType(), notification.getUserId());
        
        try {
            // 1. 保存通知
            notificationService.saveNotification(notification);
            
            // 2. 推送通知
            notificationService.pushNotification(notification);
            
        } catch (Exception e) {
            log.error("通知处理失败", e);
            throw e;
        }
    }
}
```

### 3. 统计分析消费者

```java
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "im-analytics-topic",
    consumerGroup = "analytics-consumer-group"
)
public class AnalyticsConsumer implements RocketMQListener<Message> {
    
    @Autowired
    private AnalyticsService analyticsService;
    
    @Override
    public void onMessage(Message message) {
        try {
            // 统计消息量
            analyticsService.recordMessage(message);
            
            // 更新用户活跃度
            analyticsService.updateUserActivity(message.getSenderId());
            
        } catch (Exception e) {
            log.error("统计分析失败", e);
            // 统计失败不影响主流程，不抛出异常
        }
    }
}
```

## 消息可靠性保证

### 1. 生产者端

```java
// 使用同步发送，确保消息发送成功
SendResult result = messageProducer.sendSyncMessage("im-message-topic", message);
if (result.getSendStatus() != SendStatus.SEND_OK) {
    // 发送失败，记录日志或重试
    log.error("消息发送失败: messageId={}", message.getId());
}
```

### 2. 消费者端

```java
@Override
public void onMessage(Message message) {
    try {
        // 处理消息
        processMessage(message);
    } catch (Exception e) {
        log.error("消息处理失败，将重试", e);
        throw e; // 抛出异常，RocketMQ 会自动重试
    }
}
```

### 3. 消息持久化

- 消息在发送前已经保存到 MongoDB
- Mailbox 已经写入
- 即使消息队列失败，数据也不会丢失
- 用户可以通过拉取接口获取消息

## 性能优化

### 1. 批量发送

```java
// 群聊场景，批量发送消息
List<Message> messages = ...;
for (Message msg : messages) {
    messageProducer.sendAsyncMessage("im-message-topic", msg);
}
```

### 2. 消费者并发

```java
@RocketMQMessageListener(
    topic = "im-message-topic",
    consumerGroup = "online-push-consumer-group",
    consumeThreadMax = 20  // 增加消费线程数
)
```

### 3. 消息过滤

```java
// 使用 Tag 过滤，减少不必要的消费
@RocketMQMessageListener(
    topic = "im-message-topic",
    consumerGroup = "single-chat-consumer-group",
    selectorExpression = "SINGLE_CHAT"  // 只消费单聊消息
)
```

## 监控和告警

### 1. 消息堆积监控

```java
// 定期检查消息堆积情况
@Scheduled(fixedRate = 60000)
public void checkMessageBacklog() {
    long backlog = rocketMQTemplate.getConsumerBacklog("im-message-topic", "consumer-group");
    if (backlog > 10000) {
        // 告警：消息堆积严重
        alertService.sendAlert("消息堆积: " + backlog);
    }
}
```

### 2. 消费失败监控

```java
@Override
public void onMessage(Message message) {
    try {
        processMessage(message);
        // 记录成功
        metricsService.recordSuccess("message.consume");
    } catch (Exception e) {
        // 记录失败
        metricsService.recordFailure("message.consume");
        throw e;
    }
}
```

## 最佳实践

### 1. 消息幂等性

```java
@Override
public void onMessage(Message message) {
    // 检查消息是否已处理
    if (messageProcessedCache.contains(message.getId())) {
        log.info("消息已处理，跳过: messageId={}", message.getId());
        return;
    }
    
    // 处理消息
    processMessage(message);
    
    // 标记已处理
    messageProcessedCache.add(message.getId());
}
```

### 2. 消息顺序性

```java
// 使用顺序消息，保证同一会话的消息顺序
messageProducer.sendOrderlyMessage(
    "im-message-topic", 
    message, 
    conversationId  // 使用会话ID作为顺序键
);
```

### 3. 错误处理

```java
@Override
public void onMessage(Message message) {
    try {
        processMessage(message);
    } catch (BusinessException e) {
        // 业务异常，不重试
        log.error("业务处理失败: {}", e.getMessage());
    } catch (Exception e) {
        // 系统异常，重试
        log.error("系统异常，将重试", e);
        throw e;
    }
}
```

## 总结

消息队列在 Fleets 中的主要作用：

1. **解耦**：消息发送和推送解耦
2. **异步**：提高消息发送性能
3. **削峰**：应对高并发场景
4. **可靠**：配合 Mailbox 保证消息不丢失
5. **扩展**：支持多种消费场景（推送、统计、通知等）

关键点：
- Mailbox 保证消息持久化
- 消息队列负责实时推送
- 离线消息通过拉取接口获取
- 消息可靠性由多层保障
