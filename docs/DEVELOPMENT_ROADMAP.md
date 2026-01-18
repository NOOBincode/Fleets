# Fleets 核心功能开发路线图

## 当前状态分析

### 已完成模块 ✅
- **用户模块**：注册、登录、信息管理（100%）
- **好友模块**：好友关系管理、验证流程（90%）
- **基础设施**：Redis缓存、数据库配置、日志配置

### 待完成模块 ⏳
- **Mailbox模块**：骨架完成，10个业务方法待实现
- **消息模块**：仅有骨架代码
- **WebSocket模块**：连接管理、消息推送
- **群组模块**：群聊功能（可选）

---

## 推荐开发顺序

### 阶段1：Mailbox模块（优先级：最高）⭐⭐⭐

**为什么先做Mailbox？**
- 消息模块依赖Mailbox（序列号管理、离线存储）
- WebSocket推送依赖Mailbox（获取待推送消息）
- 是整个消息系统的核心基础设施

**工作量**：2-3天  
**难度**：中等

---

### 阶段2：消息模块（优先级：高）⭐⭐⭐

**为什么第二做消息？**
- 有了Mailbox后，消息模块可以完整实现
- 是IM系统的核心业务逻辑
- 可以先用HTTP接口测试，再接入WebSocket

**工作量**：3-4天  
**难度**：中等

---

### 阶段3：WebSocket模块（优先级：高）⭐⭐⭐

**为什么第三做WebSocket？**
- 前两个模块完成后，WebSocket只需要做连接管理和推送
- 可以复用消息模块的业务逻辑
- 是实时通讯的关键

**工作量**：2-3天  
**难度**：较高

---

### 阶段4：群组模块（优先级：中）⭐⭐

**可选模块**
- 如果时间充裕再做
- 逻辑类似单聊，但需要处理群成员管理

**工作量**：3-4天  
**难度**：中等

---

## 阶段1：Mailbox模块实现指南

### 📋 任务清单

#### 1.1 SequenceService（已完成）✅
- 序列号生成逻辑已实现
- 基于Redis的分布式序列号

#### 1.2 MailboxServiceImpl - 10个方法待实现

**优先级排序**：

1. **saveMessage()** - 最高优先级 ⭐⭐⭐
   - 保存消息到Mailbox
   - 生成序列号
   - 更新UserMailbox的lastSequence

2. **syncMessages()** - 最高优先级 ⭐⭐⭐
   - 客户端拉取消息
   - 根据lastSequence增量同步

3. **getUnreadCount()** - 高优先级 ⭐⭐
   - 获取未读消息数
   - 用于消息提醒

4. **markAsRead()** - 高优先级 ⭐⭐
   - 标记消息已读
   - 更新readSequence

5. **getOfflineMessages()** - 中优先级 ⭐
   - 获取离线消息
   - 用户上线时调用

6. **deleteMessage()** - 中优先级 ⭐
   - 删除消息（软删除）
   - 只删除自己的Mailbox记录

7. **clearMailbox()** - 低优先级
   - 清空Mailbox
   - 管理功能

8. **getMailboxInfo()** - 低优先级
   - 获取Mailbox统计信息
   - 调试用

9. **batchSaveMessages()** - 低优先级
   - 批量保存消息
   - 性能优化用

10. **cleanExpiredMessages()** - 低优先级
    - 清理过期消息
    - 定时任务调用

---

### 🎯 实现步骤

#### Step 1: saveMessage() - 核心方法

**业务流程**：
```
1. 生成序列号（调用SequenceService）
2. 创建MailboxMessage对象
3. 保存到MongoDB
4. 更新UserMailbox的lastSequence
5. 返回序列号
```

**关键点**：
```java
// 伪代码
public Long saveMessage(Long userId, MessageDTO message) {
    // 1. 生成序列号
    Long sequence = sequenceService.generateSequence(userId);
    
    // 2. 创建MailboxMessage
    MailboxMessage mailboxMessage = new MailboxMessage();
    mailboxMessage.setUserId(userId);
    mailboxMessage.setSequence(sequence);
    mailboxMessage.setMessageId(message.getMessageId());
    mailboxMessage.setFromUserId(message.getFromUserId());
    mailboxMessage.setToUserId(message.getToUserId());
    mailboxMessage.setContent(message.getContent());
    mailboxMessage.setMessageType(message.getMessageType());
    mailboxMessage.setStatus(0); // 0-未读
    mailboxMessage.setCreateTime(new Date());
    
    // 3. 保存到MongoDB（Reactive）
    mailboxMessageRepository.save(mailboxMessage).block();
    
    // 4. 更新UserMailbox的lastSequence
    updateLastSequence(userId, sequence);
    
    return sequence;
}
```

**注意事项**：
- ⚠️ Reactive编程：Repository返回Mono/Flux，需要`.block()`转同步
- ⚠️ 事务问题：MongoDB不支持跨集合事务，考虑最终一致性
- ⚠️ 并发问题：序列号生成已经用Redis保证唯一性

---

#### Step 2: syncMessages() - 增量同步

**业务流程**：
```
1. 获取客户端的lastSequence
2. 查询大于lastSequence的消息
3. 返回消息列表和最新的sequence
```

**关键点**：
```java
public SyncResult syncMessages(Long userId, Long lastSequence) {
    // 1. 查询增量消息
    List<MailboxMessage> messages = mailboxMessageRepository
        .findByUserIdAndSequenceGreaterThan(userId, lastSequence)
        .collectList()
        .block();
    
    // 2. 获取最新的sequence
    Long latestSequence = userMailboxRepository
        .findByUserId(userId)
        .map(UserMailbox::getLastSequence)
        .block();
    
    // 3. 构造返回结果
    SyncResult result = new SyncResult();
    result.setMessages(messages);
    result.setLatestSequence(latestSequence);
    result.setHasMore(messages.size() >= pageSize);
    
    return result;
}
```

**注意事项**：
- ⚠️ 分页处理：一次不要返回太多消息（建议100条）
- ⚠️ 性能优化：添加索引 `{userId: 1, sequence: 1}`
- ⚠️ 边界情况：lastSequence=0表示首次同步

---

#### Step 3: getUnreadCount() - 未读数统计

**业务流程**：
```
1. 获取UserMailbox的readSequence
2. 统计sequence > readSequence的消息数
3. 按会话分组统计
```

**关键点**：
```java
public UnreadCountVO getUnreadCount(Long userId) {
    // 1. 获取readSequence
    Long readSequence = userMailboxRepository
        .findByUserId(userId)
        .map(UserMailbox::getReadSequence)
        .block();
    
    // 2. 统计未读消息
    List<MailboxMessage> unreadMessages = mailboxMessageRepository
        .findByUserIdAndSequenceGreaterThan(userId, readSequence)
        .collectList()
        .block();
    
    // 3. 按会话分组
    Map<Long, Long> unreadByConversation = unreadMessages.stream()
        .collect(Collectors.groupingBy(
            MailboxMessage::getFromUserId,
            Collectors.counting()
        ));
    
    // 4. 构造返回结果
    UnreadCountVO vo = new UnreadCountVO();
    vo.setTotalUnread(unreadMessages.size());
    vo.setUnreadByConversation(unreadByConversation);
    
    return vo;
}
```

**注意事项**：
- ⚠️ 性能问题：未读数可以缓存到Redis
- ⚠️ 实时性：WebSocket推送时更新未读数

---

#### Step 4: markAsRead() - 标记已读

**业务流程**：
```
1. 更新UserMailbox的readSequence
2. 可选：更新MailboxMessage的status
```

**关键点**：
```java
public void markAsRead(Long userId, Long sequence) {
    // 1. 更新readSequence
    UserMailbox mailbox = userMailboxRepository
        .findByUserId(userId)
        .block();
    
    if (mailbox != null && sequence > mailbox.getReadSequence()) {
        mailbox.setReadSequence(sequence);
        mailbox.setUpdateTime(new Date());
        userMailboxRepository.save(mailbox).block();
    }
    
    // 2. 可选：批量更新消息状态
    // mailboxMessageRepository.updateStatusBySequence(userId, sequence, 1);
}
```

**注意事项**：
- ⚠️ 幂等性：重复标记已读不应该报错
- ⚠️ 性能优化：不需要更新每条消息的status，只更新readSequence即可

---

### 💡 关键技巧

#### 技巧1：Reactive转同步
```java
// Mono转同步
Mono<User> userMono = userRepository.findById(1L);
User user = userMono.block(); // 阻塞等待结果

// Flux转List
Flux<Message> messageFlux = messageRepository.findAll();
List<Message> messages = messageFlux.collectList().block();
```

#### 技巧2：MongoDB索引
```javascript
// 在MongoDB中创建索引
db.mailbox_message.createIndex({userId: 1, sequence: 1});
db.mailbox_message.createIndex({userId: 1, status: 1});
db.user_mailbox.createIndex({userId: 1}, {unique: true});
```

#### 技巧3：序列号连续性检查
```java
// 客户端检查消息是否连续
public boolean checkContinuity(List<MailboxMessage> messages) {
    for (int i = 1; i < messages.size(); i++) {
        if (messages.get(i).getSequence() != messages.get(i-1).getSequence() + 1) {
            // 发现断层，需要重新同步
            return false;
        }
    }
    return true;
}
```

#### 技巧4：错误处理
```java
try {
    mailboxMessageRepository.save(message).block();
} catch (Exception e) {
    log.error("保存消息失败，userId: {}, messageId: {}", 
        userId, message.getMessageId(), e);
    throw new BusinessException("消息保存失败");
}
```

---

### 🧪 测试建议

#### 单元测试
```java
@Test
void testSaveMessage() {
    // Given
    MessageDTO dto = new MessageDTO();
    dto.setFromUserId(1L);
    dto.setToUserId(2L);
    dto.setContent("Hello");
    
    // When
    Long sequence = mailboxService.saveMessage(2L, dto);
    
    // Then
    assertThat(sequence).isGreaterThan(0L);
}
```

#### 集成测试
```java
@Test
void testMessageFlow() {
    // 1. 保存消息
    Long seq1 = mailboxService.saveMessage(userId, msg1);
    Long seq2 = mailboxService.saveMessage(userId, msg2);
    
    // 2. 同步消息
    SyncResult result = mailboxService.syncMessages(userId, 0L);
    assertThat(result.getMessages()).hasSize(2);
    
    // 3. 标记已读
    mailboxService.markAsRead(userId, seq2);
    
    // 4. 检查未读数
    UnreadCountVO unread = mailboxService.getUnreadCount(userId);
    assertThat(unread.getTotalUnread()).isEqualTo(0);
}
```

---

### ⚠️ 常见坑点

#### 坑1：Reactive编程忘记subscribe/block
```java
// ❌ 错误：没有触发执行
mailboxMessageRepository.save(message);

// ✅ 正确：阻塞等待结果
mailboxMessageRepository.save(message).block();

// ✅ 或者异步执行
mailboxMessageRepository.save(message).subscribe();
```

#### 坑2：序列号不连续
```java
// 原因：并发保存消息时，MongoDB写入顺序不确定
// 解决：序列号生成用Redis保证原子性（已实现）
```

#### 坑3：MongoDB事务问题
```java
// MongoDB单文档操作是原子的，但跨文档不是
// 解决：接受最终一致性，或者使用MongoDB 4.0+的事务
```

#### 坑4：内存溢出
```java
// ❌ 错误：一次查询所有消息
List<Message> all = repository.findByUserId(userId).collectList().block();

// ✅ 正确：分页查询
Flux<Message> messages = repository.findByUserId(userId)
    .take(100); // 限制数量
```

---

## 阶段2：消息模块实现指南

### 📋 核心功能

1. **发送单聊消息**
   - 保存消息到MySQL（持久化）
   - 保存到双方Mailbox（发件箱+收件箱）
   - 发送到RocketMQ（异步处理）

2. **接收消息**
   - 从Mailbox拉取消息
   - 标记已读

3. **消息撤回**
   - 更新消息状态
   - 通知对方

4. **消息历史**
   - 分页查询
   - 按会话查询

### 🎯 实现步骤

#### Step 1: 发送消息流程

```
1. 校验好友关系（是否是好友）
2. 保存消息到MySQL（message表）
3. 保存到发送方Mailbox
4. 保存到接收方Mailbox
5. 发送到RocketMQ（触发推送）
6. 返回消息ID和序列号
```

**关键代码结构**：
```java
@Transactional
public MessageVO sendMessage(SendMessageDTO dto) {
    // 1. 校验好友关系
    checkFriendship(dto.getFromUserId(), dto.getToUserId());
    
    // 2. 保存消息到MySQL
    Message message = saveToDatabase(dto);
    
    // 3. 保存到双方Mailbox
    Long senderSeq = mailboxService.saveMessage(dto.getFromUserId(), message);
    Long receiverSeq = mailboxService.saveMessage(dto.getToUserId(), message);
    
    // 4. 发送到MQ
    sendToMQ(message);
    
    // 5. 返回结果
    return buildMessageVO(message, senderSeq);
}
```

#### Step 2: 消息持久化设计

**MySQL表结构**（已有）：
```sql
CREATE TABLE message (
    id BIGINT PRIMARY KEY,
    from_user_id BIGINT,
    to_user_id BIGINT,
    content TEXT,
    message_type TINYINT,  -- 1-文本 2-图片 3-语音 4-视频
    status TINYINT,        -- 0-正常 1-撤回 2-删除
    create_time DATETIME,
    INDEX idx_from_user (from_user_id, create_time),
    INDEX idx_to_user (to_user_id, create_time)
);
```

**注意事项**：
- MySQL存储完整消息（持久化）
- MongoDB存储Mailbox索引（快速查询）
- 两者通过messageId关联

---

## 阶段3：WebSocket模块实现指南

### 📋 核心功能

1. **连接管理**
   - 用户上线/下线
   - 心跳检测
   - 断线重连

2. **消息推送**
   - 实时推送新消息
   - 推送系统通知

3. **在线状态**
   - 维护在线用户列表
   - 好友在线状态通知

### 🎯 实现步骤

#### Step 1: WebSocket连接管理

**关键组件**：
```java
@Component
public class WebSocketSessionManager {
    // userId -> WebSocketSession
    private ConcurrentHashMap<Long, WebSocketSession> sessions;
    
    public void addSession(Long userId, WebSocketSession session);
    public void removeSession(Long userId);
    public WebSocketSession getSession(Long userId);
    public boolean isOnline(Long userId);
}
```

#### Step 2: 消息推送逻辑

```java
@Service
public class MessagePushService {
    
    public void pushMessage(Long userId, Message message) {
        // 1. 检查用户是否在线
        if (sessionManager.isOnline(userId)) {
            // 2. 获取WebSocket连接
            WebSocketSession session = sessionManager.getSession(userId);
            
            // 3. 推送消息
            session.sendMessage(new TextMessage(JSON.toJSONString(message)));
        } else {
            // 4. 用户离线，消息已在Mailbox，等待拉取
            log.info("用户离线，消息存入Mailbox: {}", userId);
        }
    }
}
```

#### Step 3: RocketMQ消费者

```java
@Component
@RocketMQMessageListener(
    topic = "fleets-message",
    consumerGroup = "fleets-push-consumer"
)
public class MessagePushConsumer implements RocketMQListener<Message> {
    
    @Override
    public void onMessage(Message message) {
        // 推送给接收方
        messagePushService.pushMessage(message.getToUserId(), message);
    }
}
```

---

## 开发技巧总结

### 1. 分层开发
```
Controller -> Service -> Repository
先实现Service层逻辑，再暴露Controller接口
```

### 2. 先HTTP后WebSocket
```
先用HTTP接口测试业务逻辑
业务逻辑稳定后再接入WebSocket
```

### 3. 日志很重要
```java
log.info("保存消息，userId: {}, messageId: {}", userId, messageId);
log.error("消息保存失败", e);
```

### 4. 异常处理
```java
try {
    // 业务逻辑
} catch (BusinessException e) {
    // 业务异常，返回错误信息
    throw e;
} catch (Exception e) {
    // 系统异常，记录日志
    log.error("系统异常", e);
    throw new BusinessException("系统繁忙，请稍后重试");
}
```

### 5. 单元测试驱动
```
写一个方法 -> 写一个测试 -> 运行测试 -> 修复bug
```

---

## 时间规划建议

| 阶段 | 工作内容 | 预计时间 | 优先级 |
|-----|---------|---------|--------|
| 阶段1 | Mailbox核心5个方法 | 2天 | 最高 |
| 阶段1 | Mailbox其他5个方法 | 1天 | 中 |
| 阶段2 | 消息发送/接收 | 2天 | 高 |
| 阶段2 | 消息历史/撤回 | 1天 | 中 |
| 阶段3 | WebSocket连接管理 | 1天 | 高 |
| 阶段3 | 消息推送 | 1天 | 高 |
| 阶段3 | 在线状态 | 1天 | 中 |
| **总计** | | **9-10天** | |

---

## 遇到问题怎么办？

### 1. Reactive编程不熟悉
- 参考Spring WebFlux官方文档
- 先用`.block()`转同步，稳定后再优化

### 2. MongoDB查询不会写
- 参考Spring Data MongoDB文档
- 使用方法命名规则：`findByUserIdAndSequenceGreaterThan`

### 3. WebSocket不熟悉
- 参考Spring WebSocket官方文档
- 先实现简单的echo服务器，再加业务逻辑

### 4. 性能问题
- 先实现功能，再优化性能
- 使用Prometheus监控找瓶颈

---

## 下一步行动

1. ✅ 阅读本文档
2. ⏳ 从Mailbox的`saveMessage()`开始
3. ⏳ 写一个方法，写一个测试
4. ⏳ 遇到问题随时问我

加油！💪
