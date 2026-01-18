# Mailbox模块快速参考

## 已完成的骨架

### 1. 实体类（Entity）
- ✅ `UserMailbox` - 用户信箱元数据
- ✅ `MailboxMessage` - 信箱消息

### 2. Repository
- ✅ `UserMailboxRepository` - 信箱Repository（Reactive）
- ✅ `MailboxMessageRepository` - 消息Repository（Reactive）

### 3. Service
- ✅ `MailboxService` - 服务接口（10个方法）
- ✅ `MailboxServiceImpl` - 服务实现骨架（含TODO）
- ✅ `SequenceService` - 序列号生成服务（已完整实现）

### 4. Controller
- ✅ `MailboxController` - 8个REST接口

### 5. DTO/VO
- ✅ `SyncMessageDTO` - 同步消息请求
- ✅ `MarkReadDTO` - 标记已读请求
- ✅ `SyncResult` - 同步结果响应
- ✅ `UnreadCountVO` - 未读数响应

### 6. 数据库
- ✅ MongoDB索引脚本（`mailbox_indexes.js`）

---

## 待实现的方法（TODO）

### MailboxServiceImpl

1. **writeMessage()** - 写入消息到信箱
   - 获取或创建用户信箱
   - 生成序列号
   - 创建MailboxMessage
   - 更新UserMailbox元数据

2. **batchWriteMessage()** - 批量写入（群聊）
   - 批量生成序列号
   - 批量创建MailboxMessage
   - 批量更新UserMailbox

3. **pullOfflineMessages()** - 拉取离线消息
   - 查询所有会话的信箱
   - 查询序列号大于lastSequence的消息
   - 转换为MessageVO

4. **syncMessages()** - 增量同步
   - 查询该会话的信箱
   - 查询fromSequence之后的消息
   - 返回同步结果

5. **markAsRead()** - 标记已读
   - 更新MailboxMessage状态
   - 更新UserMailbox未读数
   - 清理缓存
   - 发送已读回执

6. **batchMarkAsRead()** - 批量标记已读

7. **getUnreadCount()** - 获取未读数
   - 查询所有信箱
   - 统计总未读数和各会话未读数

8. **getConversationUnreadCount()** - 获取会话未读数

9. **clearConversation()** - 清空会话
   - 删除该会话的所有MailboxMessage
   - 重置UserMailbox元数据

10. **deleteMessage()** - 删除消息
    - 更新MailboxMessage状态为已删除
    - 更新UserMailbox未读数

---

## API接口列表

| 接口 | 方法 | 说明 |
|-----|------|------|
| /api/mailbox/pull | GET | 拉取离线消息 |
| /api/mailbox/sync | POST | 增量同步消息 |
| /api/mailbox/read | POST | 标记消息已读 |
| /api/mailbox/unread | GET | 获取未读消息数 |
| /api/mailbox/unread/{conversationId} | GET | 获取会话未读数 |
| /api/mailbox/clear/{conversationId} | DELETE | 清空会话消息 |
| /api/mailbox/{conversationId}/{sequence} | DELETE | 删除消息 |

---

## 核心流程

### 消息发送流程（需要集成）

```java
// 在MessageServiceImpl.sendMessage()中
public MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO) {
    // 1. 保存消息到MongoDB
    Message message = saveMessage(sendDTO);
    
    // 2. 确定接收者列表
    List<Long> receiverIds = getReceiverIds(sendDTO);
    
    // 3. 为每个接收者写入Mailbox（新增）
    if (sendDTO.getMessageType() == 1) {
        // 单聊
        mailboxService.writeMessage(receiverIds.get(0), conversationId, message);
    } else {
        // 群聊
        mailboxService.batchWriteMessage(receiverIds, conversationId, message);
    }
    
    // 4. 发送到RocketMQ
    messageProducer.sendMessage(message);
    
    return convertToVO(message);
}
```

### 用户上线流程（需要实现）

```java
// 在ConnectionServiceImpl.handleConnect()中
public void handleConnect(Long userId, WebSocketSession session) {
    // 1. 保存连接
    saveConnection(userId, session);
    
    // 2. 拉取离线消息（新增）
    List<MessageVO> offlineMessages = mailboxService.pullOfflineMessages(userId, 0L);
    
    // 3. 推送离线消息
    for (MessageVO msg : offlineMessages) {
        sendToUser(userId, msg);
    }
}
```

---

## 数据模型

### UserMailbox（用户信箱）

```javascript
{
  userId: 123,
  conversationId: "conv_123_456",
  conversationType: 1,
  sequence: 1523,              // 当前最大序列号
  lastMessageId: "msg_xxx",
  lastMessageTime: ISODate(),
  unreadCount: 5,
  createTime: ISODate(),
  updateTime: ISODate()
}
```

### MailboxMessage（信箱消息）

```javascript
{
  userId: 123,
  conversationId: "conv_123_456",
  sequence: 1523,              // 该用户的序列号
  messageId: "msg_xxx",
  senderId: 456,
  messageType: 1,
  contentType: 1,
  content: "Hello",
  status: 0,                   // 0-未读，1-已读，2-已删除
  sendTime: ISODate(),
  readTime: ISODate(),
  createTime: ISODate(),
  expireTime: ISODate()        // 7天后过期
}
```

---

## 实现建议

### 优先级

1. **高优先级**（核心功能）
   - writeMessage()
   - pullOfflineMessages()
   - markAsRead()

2. **中优先级**（常用功能）
   - syncMessages()
   - getUnreadCount()
   - batchWriteMessage()

3. **低优先级**（辅助功能）
   - clearConversation()
   - deleteMessage()
   - batchMarkAsRead()

### 实现顺序

1. 先实现单聊场景（writeMessage + pullOfflineMessages）
2. 测试单聊消息流转
3. 实现群聊场景（batchWriteMessage）
4. 实现已读功能（markAsRead）
5. 实现同步功能（syncMessages）
6. 实现其他辅助功能

---

## 注意事项

1. **序列号生成**
   - 使用SequenceService.generateSequence()
   - 已实现Redis原子递增
   - 保证并发安全

2. **Reactive编程**
   - Repository使用Reactive接口
   - 需要使用block()转换为同步
   - 或者改造Service为Reactive

3. **消息过期**
   - MailboxMessage有expireTime字段
   - MongoDB TTL索引自动删除
   - 默认7天过期

4. **缓存策略**
   - 未读数可以缓存到Redis
   - 序列号已缓存在Redis
   - 注意缓存一致性

---

**文档版本**: v1.0  
**最后更新**: 2025-01-09  
**作者**: Kiro AI
