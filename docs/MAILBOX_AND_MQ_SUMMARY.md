# Mailbox 与消息队列完整总结

## 核心架构

```
                    ┌─────────────────────────────────────┐
                    │         消息发送流程                 │
                    └─────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│   MongoDB        │      │   Mailbox        │      │   MySQL          │
│   (Message)      │      │   (UserMailbox)  │      │   (Conversation) │
│                  │      │   (MailboxMsg)   │      │                  │
│   消息内容存储    │      │   信箱存储        │      │   会话摘要        │
│   持久化         │      │   序列号管理      │      │   快速查询        │
└──────────────────┘      └──────────────────┘      └──────────────────┘
                                    │
                                    ▼
                          ┌──────────────────┐
                          │   RocketMQ       │
                          │   异步推送        │
                          └──────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
            ┌──────────────┐              ┌──────────────┐
            │  在线用户     │              │  离线用户     │
            │  WebSocket   │              │  上线后拉取   │
            │  实时推送     │              │  Mailbox     │
            └──────────────┘              └──────────────┘
```

## 各组件职责

### 1. MongoDB (Message 集合)
**职责**：消息内容的主存储

**存储内容**：
- 消息 ID
- 发送者/接收者
- 消息类型/内容类型
- 消息内容
- 发送时间
- 状态

**使用场景**：
- 消息历史查询
- 消息搜索
- 消息详情获取

### 2. Mailbox (MongoDB)
**职责**：每个用户的消息信箱

**两个集合**：

#### UserMailbox（信箱元数据）
- userId + conversationId（唯一）
- 当前最大序列号
- 最后一条消息
- 未读数

#### MailboxMessage（信箱消息）
- userId + conversationId + sequence（唯一）
- 消息引用（messageId）
- 消息内容（冗余）
- 状态（未读/已读/删除）

**使用场景**：
- 离线消息存储
- 增量同步
- 未读数统计
- 消息拉取

### 3. MySQL (Conversation 表)
**职责**：会话列表的快速查询

**存储内容**：
- 用户的所有会话
- 最后一条消息摘要
- 未读数
- 更新时间

**使用场景**：
- 会话列表展示
- 快速获取未读数
- 会话排序

### 4. RocketMQ
**职责**：消息的异步推送

**Topic 设计**：
- im-message-topic：即时消息
- im-notification-topic：系统通知
- im-analytics-topic：消息统计

**使用场景**：
- 实时推送给在线用户
- 系统通知分发
- 消息统计分析
- 系统解耦

### 5. Redis
**职责**：序列号生成和缓存

**存储内容**：
- 序列号：`mailbox:seq:{userId}:{conversationId}`
- 未读数缓存：`mailbox:unread:{userId}`
- 在线状态：`user:online:{userId}`

**使用场景**：
- 原子递增序列号
- 未读数缓存
- 用户在线状态

## 消息流转完整流程

### 发送阶段

```java
// 1. 保存消息到 MongoDB
Message message = messageRepository.save(buildMessage(sendDTO));

// 2. 写入 Mailbox（每个接收者）
for (Long receiverId : receiverIds) {
    // 2.1 生成序列号（Redis 原子递增）
    Long sequence = sequenceService.generateSequence(receiverId, conversationId);
    
    // 2.2 创建信箱消息
    MailboxMessage mailboxMsg = mailboxConverter.toMailboxMessage(message);
    mailboxMsg.setUserId(receiverId);
    mailboxMsg.setSequence(sequence);
    mailboxMessageRepository.save(mailboxMsg);
    
    // 2.3 更新信箱元数据
    updateMailboxMetadata(receiverId, conversationId, sequence, message);
}

// 3. 更新会话表（MySQL）
conversationService.updateConversation(...);

// 4. 发送到 RocketMQ（异步推送）
messageProducer.sendMessage("im-message-topic", message);
```

### 推送阶段（在线用户）

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
        
        // 3. 通过 WebSocket 推送
        for (Long userId : onlineUsers) {
            webSocketService.sendMessage(userId, message);
        }
    }
}
```

### 拉取阶段（离线用户上线）

```java
// 客户端请求
GET /api/mailbox/sync?conversationId=xxx&fromSequence=1000

// 服务端处理
public SyncResult syncMessages(Long userId, SyncMessageDTO syncDTO) {
    // 查询 Mailbox 中的增量消息
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

### 已读阶段

```java
// 客户端请求
POST /api/mailbox/read
{
  "conversationId": "xxx",
  "sequence": 1001
}

// 服务端处理
public boolean markAsRead(Long userId, MarkReadDTO markReadDTO) {
    // 1. 更新 Mailbox 消息状态
    MailboxMessage message = mailboxMessageRepository
        .findByUserIdAndConversationIdAndSequence(...)
        .orElse(null);
    
    message.setStatus(1);  // 已读
    mailboxMessageRepository.save(message);
    
    // 2. 减少未读数
    decrementUnreadCount(userId, conversationId);
    
    // 3. 清理缓存
    clearUnreadCountCache(userId);
    
    return true;
}
```

## 数据一致性保证

### 1. 消息不丢失

**多层保障**：
1. MongoDB 持久化存储
2. Mailbox 信箱存储
3. 拉取接口兜底
4. RocketMQ 重试机制

**即使失败**：
- RocketMQ 推送失败 → Mailbox 已存储，可拉取
- Mailbox 写入失败 → MongoDB 有记录，可补偿
- 会话表更新失败 → 不影响消息发送，可异步修复

### 2. 消息顺序

**序列号机制**：
- Redis 原子递增
- 每个用户在每个会话中独立序列号
- 客户端按序列号排序
- 支持增量同步

### 3. 未读数准确性

**多级缓存**：
1. Redis 缓存（5分钟）
2. Mailbox 实时统计
3. MySQL 会话表备份

**更新时机**：
- 收到新消息：+1
- 标记已读：-1
- 清空会话：归零

## 性能优化

### 1. 批量操作

```java
// 群聊场景：批量写入 Mailbox
List<MailboxMessage> messages = memberIds.stream()
    .map(memberId -> createMailboxMessage(memberId, message))
    .collect(Collectors.toList());
mailboxMessageRepository.saveAll(messages);
```

### 2. 异步处理

```java
// 使用异步发送，不阻塞主流程
messageProducer.sendAsyncMessage("im-message-topic", message);
```

### 3. 缓存策略

```java
// 未读数缓存
String cacheKey = "mailbox:unread:" + userId;
UnreadCountVO cached = redisService.get(cacheKey);
if (cached != null) {
    return cached;
}
// 查询并缓存
UnreadCountVO vo = queryUnreadCount(userId);
redisService.set(cacheKey, vo, 5, TimeUnit.MINUTES);
```

### 4. 索引优化

```javascript
// MongoDB 索引
db.mailbox_message.createIndex({ 
    userId: 1, 
    conversationId: 1, 
    sequence: -1 
});

db.mailbox_message.createIndex({ 
    userId: 1, 
    status: 1, 
    createTime: -1 
});
```

## 监控指标

### 1. Mailbox 监控

- 信箱数量
- 消息总量
- 未读消息数
- 写入 QPS
- 查询延迟

### 2. RocketMQ 监控

- 消息堆积量
- 消费速率
- 推送成功率
- 消费延迟

### 3. 业务监控

- 消息发送量
- 在线推送率
- 离线拉取量
- 已读率

## 故障处理

### 1. RocketMQ 故障

**影响**：无法实时推送

**应对**：
- 消息已在 Mailbox，不会丢失
- 用户上线后主动拉取
- 修复后自动恢复推送

### 2. MongoDB 故障

**影响**：无法写入 Mailbox

**应对**：
- 消息已在 Message 集合
- 通过补偿任务修复
- 用户可查询历史消息

### 3. Redis 故障

**影响**：序列号生成失败

**应对**：
- 从 Mailbox 查询最大序列号
- 使用 MySQL 备份序列号
- 恢复后重新初始化

## 最佳实践

### 1. 消息发送

✅ **推荐**：
```java
// 先存储，后推送
message = messageRepository.save(message);
mailboxService.writeMessage(receiverId, conversationId, message);
messageProducer.sendMessage("im-message-topic", message);
```

❌ **不推荐**：
```java
// 先推送，后存储（可能丢失）
messageProducer.sendMessage("im-message-topic", message);
messageRepository.save(message);
```

### 2. 消息消费

✅ **推荐**：
```java
// 幂等性处理
if (messageProcessedCache.contains(messageId)) {
    return;
}
processMessage(message);
messageProcessedCache.add(messageId);
```

❌ **不推荐**：
```java
// 重复处理
processMessage(message);
```

### 3. 错误处理

✅ **推荐**：
```java
try {
    processMessage(message);
} catch (BusinessException e) {
    // 业务异常，不重试
    log.error("业务处理失败", e);
} catch (Exception e) {
    // 系统异常，重试
    throw e;
}
```

## 总结

### 核心原则

1. **Mailbox 是核心**：保证消息不丢失
2. **RocketMQ 是辅助**：提供实时推送
3. **多层保障**：MongoDB + Mailbox + 拉取接口
4. **最终一致性**：允许短暂不一致，保证最终一致

### 关键特性

- ✅ 消息不丢失
- ✅ 支持离线消息
- ✅ 支持增量同步
- ✅ 支持多端同步
- ✅ 高性能推送
- ✅ 可靠性保证

### 适用场景

- 即时通讯
- 单聊/群聊
- 系统通知
- 离线消息
- 消息同步
