# Mailbox（信箱）模块设计方案

## 一、为什么需要Mailbox？

### 1.1 当前架构的问题

**现有流程**：
```
发送者 → MessageService → MongoDB → RocketMQ → 在线接收者
                                            ↓
                                      离线？消息丢失
```

**问题**：
1. ❌ 离线消息可能丢失
2. ❌ 无法保证消息顺序
3. ❌ 不支持消息同步
4. ❌ 无法实现多端同步
5. ❌ 消息可靠性无法保证

### 1.2 引入Mailbox后的架构

**改进流程**：
```
发送者 → MessageService → MongoDB（消息内容）
                       ↓
                    Mailbox（每个接收者的信箱）
                       ↓
                    RocketMQ（异步推送）
                       ↓
                    在线接收者（实时推送）
                       ↓
                    离线接收者（上线后主动拉取）
```

**优势**：
1. ✅ 消息持久化到Mailbox，不会丢失
2. ✅ 每个用户有独立的消息序列号
3. ✅ 支持增量同步和多端同步
4. ✅ 消息可靠性有保证
5. ✅ 支持消息重传和补偿

---

## 二、Mailbox数据模型

### 2.1 MongoDB - UserMailbox（用户信箱）

```javascript
{
  _id: ObjectId("..."),
  userId: 123,                    // 用户ID
  conversationId: "conv_123_456", // 会话ID（单聊：user_A_B，群聊：group_789）
  conversationType: 1,            // 会话类型：1-单聊，2-群聊
  sequence: 1523,                 // 当前会话的最大序列号
  lastMessageId: "msg_xxx",       // 最后一条消息ID
  lastMessageTime: ISODate("..."),// 最后一条消息时间
  unreadCount: 5,                 // 未读消息数
  createTime: ISODate("..."),
  updateTime: ISODate("...")
}

// 索引
db.user_mailbox.createIndex({ userId: 1, conversationId: 1 }, { unique: true });
db.user_mailbox.createIndex({ userId: 1, sequence: -1 });
```

### 2.2 MongoDB - MailboxMessage（信箱消息）

```javascript
{
  _id: ObjectId("..."),
  mailboxId: ObjectId("..."),     // 关联的信箱ID
  userId: 123,                    // 接收者用户ID
  conversationId: "conv_123_456", // 会话ID
  sequence: 1523,                 // 该用户在该会话中的序列号
  messageId: "msg_xxx",           // 消息ID（关联message表）
  senderId: 456,                  // 发送者ID
  messageType: 1,                 // 消息类型
  contentType: 1,                 // 内容类型
  content: "Hello",               // 消息内容（冗余存储，提高查询效率）
  status: 0,                      // 状态：0-未读，1-已读，2-已删除
  sendTime: ISODate("..."),       // 发送时间
  readTime: ISODate("..."),       // 阅读时间
  createTime: ISODate("..."),
  expireTime: ISODate("...")      // 过期时间（7天后自动删除）
}

// 索引
db.mailbox_message.createIndex({ userId: 1, conversationId: 1, sequence: -1 });
db.mailbox_message.createIndex({ userId: 1, status: 1, createTime: -1 });
db.mailbox_message.createIndex({ expireTime: 1 }, { expireAfterSeconds: 0 });
```

### 2.3 Redis - 序列号缓存

```
Key: mailbox:seq:{userId}:{conversationId}
Type: String
Value: 当前序列号
TTL: 永久（或7天）

示例：
mailbox:seq:123:conv_123_456 = "1523"
```

---

## 三、核心流程设计

### 3.1 消息发送流程

```java
/**
 * 消息发送流程（改进版）
 */
public MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO) {
    // 1. 保存消息到MongoDB（message集合）
    Message message = saveMessage(sendDTO);
    
    // 2. 确定接收者列表
    List<Long> receiverIds = getReceiverIds(sendDTO);
    
    // 3. 为每个接收者写入Mailbox
    for (Long receiverId : receiverIds) {
        // 3.1 获取或创建用户信箱
        UserMailbox mailbox = getOrCreateMailbox(receiverId, conversationId);
        
        // 3.2 生成序列号（原子递增）
        Long sequence = incrementSequence(receiverId, conversationId);
        
        // 3.3 写入信箱消息
        MailboxMessage mailboxMsg = new MailboxMessage();
        mailboxMsg.setUserId(receiverId);
        mailboxMsg.setConversationId(conversationId);
        mailboxMsg.setSequence(sequence);
        mailboxMsg.setMessageId(message.getId());
        mailboxMsg.setStatus(0); // 未读
        mailboxMessageRepository.save(mailboxMsg);
        
        // 3.4 更新信箱元数据
        mailbox.setSequence(sequence);
        mailbox.setLastMessageId(message.getId());
        mailbox.setUnreadCount(mailbox.getUnreadCount() + 1);
        userMailboxRepository.save(mailbox);
    }
    
    // 4. 发送到RocketMQ（异步推送）
    messageProducer.sendMessage(message);
    
    // 5. 更新会话表
    updateConversation(senderId, receiverIds, message);
    
    return convertToVO(message);
}
```

### 3.2 消息推送流程

```java
/**
 * 消息消费者（RocketMQ Consumer）
 */
@RocketMQMessageListener(topic = "im-message-topic")
public class MessageConsumer {
    
    @Override
    public void onMessage(Message message) {
        // 1. 解析消息
        MessageDTO dto = parseMessage(message);
        
        // 2. 获取接收者列表
        List<Long> receiverIds = getReceiverIds(dto);
        
        // 3. 推送给在线用户
        for (Long receiverId : receiverIds) {
            // 3.1 检查用户是否在线
            if (connectionService.isOnline(receiverId)) {
                // 3.2 实时推送
                connectionService.sendToUser(receiverId, dto);
                
                // 3.3 等待ACK确认
                // 如果收到ACK，标记Mailbox消息为已送达
            } else {
                // 3.4 用户离线，消息已在Mailbox中，无需处理
                log.info("用户离线，消息已存入Mailbox: userId={}", receiverId);
            }
        }
    }
}
```

### 3.3 用户上线拉取离线消息

```java
/**
 * 用户上线后拉取离线消息
 */
public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence) {
    // 1. 查询所有会话的离线消息
    List<UserMailbox> mailboxes = userMailboxRepository.findByUserId(userId);
    
    List<MessageVO> offlineMessages = new ArrayList<>();
    
    for (UserMailbox mailbox : mailboxes) {
        // 2. 查询该会话中序列号大于lastSequence的消息
        List<MailboxMessage> messages = mailboxMessageRepository
            .findByUserIdAndConversationIdAndSequenceGreaterThan(
                userId, 
                mailbox.getConversationId(), 
                lastSequence
            );
        
        // 3. 转换为VO并返回
        for (MailboxMessage msg : messages) {
            MessageVO vo = convertToVO(msg);
            offlineMessages.add(vo);
        }
    }
    
    return offlineMessages;
}
```

### 3.4 消息同步流程（多端同步）

```java
/**
 * 增量同步消息（支持多端）
 */
public SyncResult syncMessages(Long userId, String conversationId, Long fromSequence) {
    // 1. 查询该会话的信箱
    UserMailbox mailbox = userMailboxRepository
        .findByUserIdAndConversationId(userId, conversationId);
    
    if (mailbox == null) {
        return SyncResult.empty();
    }
    
    // 2. 查询fromSequence之后的所有消息
    List<MailboxMessage> messages = mailboxMessageRepository
        .findByUserIdAndConversationIdAndSequenceGreaterThan(
            userId, 
            conversationId, 
            fromSequence
        );
    
    // 3. 返回同步结果
    SyncResult result = new SyncResult();
    result.setCurrentSequence(mailbox.getSequence());
    result.setMessages(messages);
    result.setHasMore(false);
    
    return result;
}
```

### 3.5 消息已读流程

```java
/**
 * 标记消息已读
 */
public boolean markAsRead(Long userId, String conversationId, Long sequence) {
    // 1. 更新Mailbox消息状态
    mailboxMessageRepository.updateStatus(
        userId, 
        conversationId, 
        sequence, 
        1  // 已读
    );
    
    // 2. 更新信箱未读数
    UserMailbox mailbox = userMailboxRepository
        .findByUserIdAndConversationId(userId, conversationId);
    
    if (mailbox != null && mailbox.getUnreadCount() > 0) {
        mailbox.setUnreadCount(mailbox.getUnreadCount() - 1);
        userMailboxRepository.save(mailbox);
    }
    
    // 3. 清理Redis缓存
    redisService.delete("mailbox:unread:" + userId);
    
    // 4. 发送已读回执（可选）
    sendReadReceipt(userId, conversationId, sequence);
    
    return true;
}
```

---

## 四、序列号生成策略

### 4.1 Redis原子递增（推荐）

```java
/**
 * 使用Redis原子递增生成序列号
 */
public Long incrementSequence(Long userId, String conversationId) {
    String key = "mailbox:seq:" + userId + ":" + conversationId;
    
    // Redis INCR命令，原子性递增
    Long sequence = redisService.increment(key);
    
    // 设置过期时间（可选，防止Redis内存占用过大）
    if (sequence == 1) {
        redisService.expire(key, 7, TimeUnit.DAYS);
    }
    
    return sequence;
}
```

### 4.2 MongoDB原子操作（备选）

```java
/**
 * 使用MongoDB的findAndModify原子操作
 */
public Long incrementSequence(Long userId, String conversationId) {
    Query query = Query.query(
        Criteria.where("userId").is(userId)
            .and("conversationId").is(conversationId)
    );
    
    Update update = new Update().inc("sequence", 1);
    
    FindAndModifyOptions options = new FindAndModifyOptions()
        .returnNew(true)
        .upsert(true);
    
    UserMailbox mailbox = mongoTemplate.findAndModify(
        query, 
        update, 
        options, 
        UserMailbox.class
    );
    
    return mailbox.getSequence();
}
```

---

## 五、Mailbox服务接口设计

```java
/**
 * Mailbox服务接口
 */
public interface MailboxService {
    
    /**
     * 写入消息到信箱
     */
    boolean writeMessage(Long userId, String conversationId, Message message);
    
    /**
     * 批量写入消息（群聊场景）
     */
    boolean batchWriteMessage(List<Long> userIds, String conversationId, Message message);
    
    /**
     * 拉取离线消息
     */
    List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence);
    
    /**
     * 增量同步消息
     */
    SyncResult syncMessages(Long userId, String conversationId, Long fromSequence);
    
    /**
     * 标记消息已读
     */
    boolean markAsRead(Long userId, String conversationId, Long sequence);
    
    /**
     * 批量标记已读
     */
    boolean batchMarkAsRead(Long userId, String conversationId, Long toSequence);
    
    /**
     * 获取未读消息数
     */
    Integer getUnreadCount(Long userId);
    
    /**
     * 获取会话未读数
     */
    Integer getConversationUnreadCount(Long userId, String conversationId);
    
    /**
     * 清空会话消息
     */
    boolean clearConversation(Long userId, String conversationId);
    
    /**
     * 删除消息
     */
    boolean deleteMessage(Long userId, String conversationId, Long sequence);
}
```

---

## 六、API接口设计

```
POST   /api/mailbox/pull              - 拉取离线消息
POST   /api/mailbox/sync              - 增量同步消息
POST   /api/mailbox/read              - 标记消息已读
GET    /api/mailbox/unread            - 获取未读消息数
DELETE /api/mailbox/clear/{convId}    - 清空会话消息
```

---

## 七、性能优化

### 7.1 批量写入优化

```java
/**
 * 群聊消息批量写入Mailbox
 */
public boolean batchWriteMessage(List<Long> userIds, String conversationId, Message message) {
    // 1. 批量生成序列号
    Map<Long, Long> sequenceMap = batchIncrementSequence(userIds, conversationId);
    
    // 2. 批量构建MailboxMessage
    List<MailboxMessage> mailboxMessages = new ArrayList<>();
    for (Long userId : userIds) {
        MailboxMessage msg = buildMailboxMessage(
            userId, 
            conversationId, 
            sequenceMap.get(userId), 
            message
        );
        mailboxMessages.add(msg);
    }
    
    // 3. 批量插入MongoDB
    mailboxMessageRepository.saveAll(mailboxMessages);
    
    return true;
}
```

### 7.2 分页拉取优化

```java
/**
 * 分页拉取离线消息
 */
public PageResult<MessageVO> pullOfflineMessages(
    Long userId, 
    String conversationId,
    Long fromSequence,
    Integer pageSize
) {
    // 限制每次拉取数量，避免一次性加载过多数据
    if (pageSize == null || pageSize > 100) {
        pageSize = 100;
    }
    
    List<MailboxMessage> messages = mailboxMessageRepository
        .findByUserIdAndConversationIdAndSequenceGreaterThan(
            userId, 
            conversationId, 
            fromSequence,
            PageRequest.of(0, pageSize, Sort.by("sequence").ascending())
        );
    
    return PageResult.of(messages);
}
```

### 7.3 消息过期清理

```java
/**
 * 定时清理过期消息（7天前的已读消息）
 */
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
public void cleanExpiredMessages() {
    Date expireTime = DateUtils.addDays(new Date(), -7);
    
    // 删除7天前的已读消息
    mailboxMessageRepository.deleteByStatusAndCreateTimeBefore(1, expireTime);
    
    log.info("清理过期消息完成");
}
```

---

## 八、实施步骤

### 阶段一：数据模型和基础服务（1天）
1. 创建MongoDB集合和索引
2. 创建Entity和Repository
3. 实现MailboxService基础方法

### 阶段二：消息流程改造（1天）
1. 修改MessageService.sendMessage()，增加写入Mailbox逻辑
2. 修改MessageConsumer，增加在线推送逻辑
3. 实现序列号生成服务

### 阶段三：离线消息和同步（1天）
1. 实现pullOfflineMessages()
2. 实现syncMessages()
3. 实现markAsRead()

### 阶段四：测试和优化（1天）
1. 单元测试
2. 集成测试
3. 性能测试和优化

---

## 九、总结

### 9.1 Mailbox的核心价值

1. **消息可靠性**：消息持久化到Mailbox，不会丢失
2. **顺序保证**：基于序列号保证消息顺序
3. **离线支持**：用户离线时消息存入Mailbox，上线后拉取
4. **多端同步**：基于序列号实现增量同步
5. **性能优化**：批量写入、分页拉取、过期清理

### 9.2 与现有架构的对比

| 特性 | 无Mailbox | 有Mailbox |
|-----|----------|----------|
| 消息可靠性 | ❌ 推送失败可能丢失 | ✅ 持久化保证不丢失 |
| 离线消息 | ❌ 需要单独的离线表 | ✅ 统一存储在Mailbox |
| 消息顺序 | ❌ 无法保证 | ✅ 序列号保证顺序 |
| 多端同步 | ❌ 不支持 | ✅ 基于序列号同步 |
| 未读数管理 | ⚠️ 需要额外维护 | ✅ Mailbox自带 |

---

**文档版本**: v1.0  
**最后更新**: 2025-01-09  
**作者**: Kiro AI
