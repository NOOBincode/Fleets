# 什么时候使用消息队列？

## 快速回答

**消息队列（RocketMQ）在 Fleets 中的使用时机**：

✅ **使用消息队列的场景**：
1. 实时推送给在线用户
2. 异步处理耗时任务
3. 系统解耦
4. 削峰填谷
5. 消息通知和广播

❌ **不使用消息队列的场景**：
1. 离线消息存储（使用 Mailbox）
2. 消息持久化（使用 MongoDB）
3. 同步查询操作
4. 需要立即返回结果的操作

## 详细说明

### 1. 实时推送（主要用途）

**场景**：用户在线时，需要实时收到消息

```
发送者 → MessageService → Mailbox (存储) → RocketMQ → Consumer → WebSocket → 在线接收者
```

**为什么用 MQ**：
- 解耦消息发送和推送逻辑
- 支持多个消费者（推送、统计、通知等）
- 异步处理，提高发送性能
- 支持重试和失败处理

**代码示例**：
```java
// 发送端
messageProducer.sendMessage("im-message-topic", message);

// 消费端
@RocketMQMessageListener(topic = "im-message-topic")
public class MessageConsumer implements RocketMQListener<Message> {
    public void onMessage(Message message) {
        // 推送给在线用户
        if (userOnlineService.isOnline(receiverId)) {
            webSocketService.sendMessage(receiverId, message);
        }
    }
}
```

### 2. 离线消息（不用 MQ）

**场景**：用户离线时，消息如何处理？

```
发送者 → MessageService → Mailbox (存储) ✓
                       → RocketMQ (推送失败，但消息已存储) ✓
                       
用户上线 → 主动拉取 Mailbox ✓
```

**为什么不用 MQ 存储离线消息**：
- Mailbox 已经持久化存储
- 消息队列不适合长期存储
- 拉取接口更可靠
- 支持增量同步

**代码示例**：
```java
// 用户上线后拉取
GET /api/mailbox/sync?conversationId=xxx&fromSequence=1000

// 服务端从 Mailbox 查询
List<MailboxMessage> messages = mailboxMessageRepository
    .findByUserIdAndConversationIdAndSequenceGreaterThan(
        userId, conversationId, fromSequence, pageable);
```

### 3. 群聊消息分发

**场景**：群聊消息需要分发给多个成员

```
发送者 → MessageService → 批量写入 Mailbox (每个成员) → RocketMQ → 批量推送
```

**为什么用 MQ**：
- 异步批量推送
- 避免阻塞发送流程
- 支持部分失败重试

**代码示例**：
```java
// 发送端：批量写入 Mailbox
List<Long> memberIds = groupService.getGroupMemberIds(groupId);
for (Long memberId : memberIds) {
    mailboxService.writeMessage(memberId, conversationId, message);
}

// 发送到 MQ
messageProducer.sendMessageWithTag("im-message-topic", "GROUP_CHAT", message);

// 消费端：批量推送
public void onMessage(Message message) {
    List<Long> onlineMembers = getOnlineMembers(message.getGroupId());
    for (Long memberId : onlineMembers) {
        webSocketService.sendMessage(memberId, message);
    }
}
```

### 4. 系统通知

**场景**：好友申请、群邀请、系统公告

```
系统事件 → NotificationService → RocketMQ → NotificationConsumer → 推送
```

**为什么用 MQ**：
- 解耦业务逻辑和通知逻辑
- 支持多种通知方式（站内信、推送、邮件）
- 异步处理，不影响主流程

**代码示例**：
```java
// 发送通知
messageProducer.sendMessage("im-notification-topic", notification);

// 消费通知
@RocketMQMessageListener(topic = "im-notification-topic")
public class NotificationConsumer implements RocketMQListener<Notification> {
    public void onMessage(Notification notification) {
        // 保存通知
        notificationService.save(notification);
        // 推送通知
        pushService.push(notification);
    }
}
```

### 5. 消息统计分析

**场景**：消息量统计、用户活跃度分析

```
消息发送 → RocketMQ → AnalyticsConsumer → 统计数据库
```

**为什么用 MQ**：
- 不影响主流程性能
- 异步统计
- 统计失败不影响消息发送

**代码示例**：
```java
// 发送到统计 topic
messageProducer.sendMessage("im-analytics-topic", message);

// 统计消费者
@RocketMQMessageListener(topic = "im-analytics-topic")
public class AnalyticsConsumer implements RocketMQListener<Message> {
    public void onMessage(Message message) {
        // 统计消息量
        analyticsService.recordMessage(message);
        // 更新用户活跃度
        analyticsService.updateUserActivity(message.getSenderId());
    }
}
```

## 对比表格

| 场景 | 使用 MQ | 使用 Mailbox | 使用 MySQL | 说明 |
|------|---------|--------------|------------|------|
| 消息持久化 | ❌ | ✅ | ❌ | Mailbox 是主存储 |
| 离线消息 | ❌ | ✅ | ❌ | 已在 Mailbox，拉取即可 |
| 在线推送 | ✅ | ❌ | ❌ | MQ + WebSocket |
| 消息查询 | ❌ | ✅ | ❌ | 从 Mailbox 查询 |
| 会话列表 | ❌ | ❌ | ✅ | MySQL 存储摘要 |
| 系统通知 | ✅ | ❌ | ✅ | MQ 推送，MySQL 存储 |
| 消息统计 | ✅ | ❌ | ✅ | MQ 异步，MySQL 存储结果 |
| 群聊分发 | ✅ | ✅ | ❌ | Mailbox 存储，MQ 推送 |

## 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      消息发送                                │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   MongoDB    │    │   Mailbox    │    │    MySQL     │
│  (消息内容)   │    │  (信箱存储)   │    │  (会话摘要)   │
│              │    │              │    │              │
│  持久化存储   │    │  离线消息     │    │  快速查询     │
└──────────────┘    └──────────────┘    └──────────────┘
                            │
                            ▼
                    ┌──────────────┐
                    │   RocketMQ   │
                    │  (异步推送)   │
                    └──────────────┘
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
        ┌──────────────┐        ┌──────────────┐
        │  在线用户     │        │  离线用户     │
        │  实时推送     │        │  上线拉取     │
        └──────────────┘        └──────────────┘
```

## 最佳实践

### 1. 消息发送流程

```java
public MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO) {
    // 1. 保存到 MongoDB（消息内容）
    Message message = messageRepository.save(buildMessage(sendDTO));
    
    // 2. 写入 Mailbox（每个接收者）
    List<Long> receiverIds = getReceiverIds(sendDTO);
    for (Long receiverId : receiverIds) {
        mailboxService.writeMessage(receiverId, conversationId, message);
    }
    
    // 3. 更新会话表（MySQL）
    conversationService.updateConversation(...);
    
    // 4. 发送到 MQ（异步推送）
    messageProducer.sendMessage("im-message-topic", message);
    
    return convertToVO(message);
}
```

### 2. 消息消费流程

```java
@RocketMQMessageListener(topic = "im-message-topic")
public class MessageConsumer implements RocketMQListener<Message> {
    
    public void onMessage(Message message) {
        // 1. 获取接收者
        List<Long> receiverIds = getReceiverIds(message);
        
        // 2. 过滤在线用户
        List<Long> onlineUsers = receiverIds.stream()
            .filter(userOnlineService::isOnline)
            .collect(Collectors.toList());
        
        // 3. 推送给在线用户
        for (Long userId : onlineUsers) {
            webSocketService.sendMessage(userId, message);
        }
        
        // 注意：离线用户无需处理，消息已在 Mailbox
    }
}
```

### 3. 离线消息拉取

```java
public SyncResult syncMessages(Long userId, SyncMessageDTO syncDTO) {
    // 从 Mailbox 查询增量消息
    List<MailboxMessage> messages = mailboxMessageRepository
        .findByUserIdAndConversationIdAndSequenceGreaterThan(
            userId, 
            syncDTO.getConversationId(), 
            syncDTO.getFromSequence(),
            pageable
        );
    
    // 转换并返回
    return mailboxConverter.toSyncResult(...);
}
```

## 总结

### 消息队列的作用

1. **实时推送**：推送给在线用户（主要用途）
2. **系统解耦**：发送和推送逻辑分离
3. **异步处理**：提高发送性能
4. **削峰填谷**：应对高并发
5. **多端消费**：支持多种消费场景

### 消息队列不做的事

1. ❌ 不存储离线消息（Mailbox 负责）
2. ❌ 不做消息持久化（MongoDB 负责）
3. ❌ 不做消息查询（Mailbox 负责）
4. ❌ 不保证消息顺序（序列号保证）

### 关键原则

> **Mailbox 是核心，RocketMQ 是辅助**

- Mailbox 保证消息不丢失
- RocketMQ 提供实时推送
- 离线消息通过拉取接口获取
- 消息可靠性由多层保障
