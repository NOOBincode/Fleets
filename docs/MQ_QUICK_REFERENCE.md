# 消息队列快速参考

## 一句话总结

> **Mailbox 存储消息，RocketMQ 推送消息**

## 使用时机

| 场景 | 使用 MQ | 使用 Mailbox |
|------|---------|--------------|
| 在线推送 | ✅ | ❌ |
| 离线消息 | ❌ | ✅ |
| 消息存储 | ❌ | ✅ |
| 消息查询 | ❌ | ✅ |
| 系统通知 | ✅ | ❌ |
| 消息统计 | ✅ | ❌ |

## 快速代码

### 发送消息

```java
// 1. 保存到 MongoDB
Message message = messageRepository.save(buildMessage(sendDTO));

// 2. 写入 Mailbox
mailboxService.writeMessage(receiverId, conversationId, message);

// 3. 发送到 MQ
messageProducer.sendMessage("im-message-topic", message);
```

### 消费消息

```java
@RocketMQMessageListener(topic = "im-message-topic")
public class MessageConsumer implements RocketMQListener<Message> {
    public void onMessage(Message message) {
        // 推送给在线用户
        if (userOnlineService.isOnline(receiverId)) {
            webSocketService.sendMessage(receiverId, message);
        }
        // 离线用户无需处理，消息已在 Mailbox
    }
}
```

### 拉取离线消息

```java
// 从 Mailbox 拉取
GET /api/mailbox/sync?conversationId=xxx&fromSequence=1000

// 服务端
List<MailboxMessage> messages = mailboxMessageRepository
    .findByUserIdAndConversationIdAndSequenceGreaterThan(
        userId, conversationId, fromSequence, pageable);
```

## Topic 列表

| Topic | 用途 | Consumer |
|-------|------|----------|
| im-message-topic | 即时消息 | MessageConsumer |
| im-notification-topic | 系统通知 | NotificationConsumer |
| im-analytics-topic | 消息统计 | AnalyticsConsumer |

## 常见问题

### Q: 离线消息怎么处理？
A: 消息已在 Mailbox，用户上线后主动拉取

### Q: 消息会丢失吗？
A: 不会，Mailbox 已持久化存储

### Q: RocketMQ 故障怎么办？
A: 不影响，消息在 Mailbox，可拉取

### Q: 如何保证消息顺序？
A: 使用序列号，客户端按序列号排序

### Q: 未读数如何统计？
A: Mailbox 实时统计 + Redis 缓存

## 数据流向

```
发送 → MongoDB → Mailbox → RocketMQ → 在线用户
                    ↓
                离线用户（上线拉取）
```

## 记住这些

1. **Mailbox 是核心**：保证消息不丢失
2. **RocketMQ 是辅助**：提供实时推送
3. **离线消息**：已在 Mailbox，无需 MQ
4. **在线推送**：MQ + WebSocket
5. **消息可靠**：多层保障
