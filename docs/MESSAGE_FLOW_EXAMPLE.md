# 消息流转完整示例

## 场景：用户 A 给用户 B 发送一条消息

### 1. 发送阶段（MessageService.sendMessage）

```java
// 用户 A (userId=123) 给用户 B (userId=456) 发送消息
MessageSendDTO sendDTO = new MessageSendDTO();
sendDTO.setMessageType(1);  // 单聊
sendDTO.setContentType(1);  // 文本
sendDTO.setReceiverId(456L);
sendDTO.setContent("Hello!");

MessageVO result = messageService.sendMessage(123L, sendDTO);
```

**执行步骤**：

#### Step 1: 保存消息到 MongoDB
```javascript
// message 集合
{
  _id: "msg_001",
  messageType: 1,
  contentType: 1,
  senderId: 123,
  receiverId: 456,
  content: "Hello!",
  sendTime: ISODate("2024-01-19T10:00:00Z"),
  status: 1
}
```

#### Step 2: 写入 Mailbox（接收者的信箱）
```java
// MailboxService.writeMessage(456, "conv_123_456", message)

// 2.1 获取或创建信箱
UserMailbox mailbox = userMailboxRepository
    .findByUserIdAndConversationId(456, "conv_123_456")
    .orElseGet(() -> createNewMailbox(456, "conv_123_456"));

// 2.2 生成序列号
Long sequence = sequenceService.generateSequence(456, "conv_123_456");
// Redis: mailbox:seq:456:conv_123_456 = 1001

// 2.3 创建信箱消息
MailboxMessage mailboxMsg = new MailboxMessage();
mailboxMsg.setUserId(456);
mailboxMsg.setConversationId("conv_123_456");
mailboxMsg.setSequence(1001);
mailboxMsg.setMessageId("msg_001");
mailboxMsg.setSenderId(123);
mailboxMsg.setContent("Hello!");
mailboxMsg.setStatus(0);  // 未读
mailboxMessageRepository.save(mailboxMsg);

// 2.4 更新信箱元数据
mailbox.setSequence(1001);
mailbox.setLastMessageId("msg_001");
mailbox.setUnreadCount(mailbox.getUnreadCount() + 1);
userMailboxRepository.save(mailbox);
```

**MongoDB 数据**：
```javascript
// user_mailbox 集合
{
  _id: ObjectId("..."),
  userId: 456,
  conversationId: "conv_123_456",
  sequence: 1001,
  lastMessageId: "msg_001",
  lastMessageTime: ISODate("2024-01-19T10:00:00Z"),
  unreadCount: 1
}

// mailbox_message 集合
{
  _id: ObjectId("..."),
  userId: 456,
  conversationId: "conv_123_456",
  sequence: 1001,
  messageId: "msg_001",
  senderId: 123,
  content: "Hello!",
  status: 0,  // 未读
  sendTime: ISODate("2024-01-19T10:00:00Z")
}
```

#### Step 3: 更新会话表（MySQL）
```sql
-- 发送者的会话表（不增加未读数）
INSERT INTO conversation (user_id, target_id, conversation_type, last_message_id, 
                         last_message_content, last_message_time, unread_count)
VALUES (123, 456, 0, 'msg_001', 'Hello!', NOW(), 0)
ON DUPLICATE KEY UPDATE 
  last_message_id = 'msg_001',
  last_message_content = 'Hello!',
  last_message_time = NOW();

-- 接收者的会话表（增加未读数）
INSERT INTO conversation (user_id, target_id, conversation_type, last_message_id, 
                         last_message_content, last_message_time, unread_count)
VALUES (456, 123, 0, 'msg_001', 'Hello!', NOW(), 1)
ON DUPLICATE KEY UPDATE 
  last_message_id = 'msg_001',
  last_message_content = 'Hello!',
  last_message_time = NOW(),
  unread_count = unread_count + 1;
```

#### Step 4: 发送到 RocketMQ
```java
messageProducer.sendMessage("im-message-topic", savedMessage);
```

**RocketMQ 消息**：
```json
{
  "id": "msg_001",
  "messageType": 1,
  "contentType": 1,
  "senderId": 123,
  "receiverId": 456,
  "content": "Hello!",
  "sendTime": "2024-01-19T10:00:00Z",
  "status": 1
}
```

### 2. 推送阶段（MessageConsumer）

#### 场景 A：用户 B 在线

```java
@Override
public void onMessage(String messageJson) {
    // 1. 解析消息
    Message message = objectMapper.readValue(messageJson, Message.class);
    
    // 2. 检查用户是否在线
    if (userOnlineService.isOnline(456)) {
        // 3. 通过 WebSocket 推送
        webSocketService.sendMessage(456, message);
        log.info("消息推送成功: userId=456, messageId=msg_001");
    }
}
```

**WebSocket 推送**：
```javascript
// 客户端收到 WebSocket 消息
{
  "type": "NEW_MESSAGE",
  "data": {
    "id": "msg_001",
    "senderId": 123,
    "content": "Hello!",
    "sendTime": "2024-01-19T10:00:00Z"
  }
}
```

#### 场景 B：用户 B 离线

```java
@Override
public void onMessage(String messageJson) {
    // 1. 解析消息
    Message message = objectMapper.readValue(messageJson, Message.class);
    
    // 2. 检查用户是否在线
    if (!userOnlineService.isOnline(456)) {
        log.info("用户离线，消息已存储在 Mailbox: userId=456");
        // 消息已经在 Step 2 写入 Mailbox，无需额外处理
    }
}
```

### 3. 拉取阶段（用户 B 上线后）

#### 用户 B 上线，拉取离线消息

```java
// 客户端请求
GET /api/mailbox/sync?conversationId=conv_123_456&fromSequence=1000

// 服务端处理
@Override
public SyncResult syncMessages(Long userId, SyncMessageDTO syncDTO) {
    // 1. 查询信箱
    UserMailbox mailbox = userMailboxRepository
        .findByUserIdAndConversationId(456, "conv_123_456")
        .orElse(null);
    
    // 2. 查询增量消息（sequence > 1000）
    List<MailboxMessage> messages = mailboxMessageRepository
        .findByUserIdAndConversationIdAndSequenceGreaterThan(
            456, "conv_123_456", 1000, pageable);
    
    // 3. 转换为 MessageVO
    List<MessageVO> messageVOs = mailboxConverter.toMessageVOList(messages);
    
    // 4. 返回同步结果
    return mailboxConverter.toSyncResult(
        mailbox.getSequence(),  // 当前最大序列号：1001
        messageVOs,
        false
    );
}
```

**响应数据**：
```json
{
  "currentSequence": 1001,
  "messages": [
    {
      "id": "msg_001",
      "senderId": 123,
      "content": "Hello!",
      "sequence": 1001,
      "sendTime": "2024-01-19T10:00:00Z"
    }
  ],
  "hasMore": false
}
```

### 4. 已读阶段（用户 B 阅读消息）

```java
// 客户端请求
POST /api/mailbox/read
{
  "conversationId": "conv_123_456",
  "sequence": 1001
}

// 服务端处理
@Override
public boolean markAsRead(Long userId, MarkReadDTO markReadDTO) {
    // 1. 查询消息
    MailboxMessage message = mailboxMessageRepository
        .findByUserIdAndConversationIdAndSequence(
            456, "conv_123_456", 1001)
        .orElse(null);
    
    // 2. 更新状态
    if (message.getStatus() == 0) {
        message.setStatus(1);  // 已读
        message.setReadTime(new Date());
        mailboxMessageRepository.save(message);
        
        // 3. 减少未读数
        decrementUnreadCount(456, "conv_123_456");
        
        // 4. 清理缓存
        clearUnreadCountCache(456);
    }
    
    return true;
}
```

**MongoDB 更新**：
```javascript
// mailbox_message 更新
{
  status: 1,  // 0 -> 1
  readTime: ISODate("2024-01-19T10:05:00Z")
}

// user_mailbox 更新
{
  unreadCount: 0  // 1 -> 0
}
```

## 群聊场景

### 用户 A 在群 G (groupId=789) 发送消息

#### Step 1: 保存消息
```javascript
{
  _id: "msg_002",
  messageType: 2,  // 群聊
  groupId: 789,
  senderId: 123,
  content: "Hello Group!"
}
```

#### Step 2: 批量写入 Mailbox
```java
// 获取群成员：[123, 456, 789]
List<Long> memberIds = groupService.getGroupMemberIds(789);

// 批量生成序列号
Map<Long, Long> sequenceMap = sequenceService.batchGenerateSequence(
    memberIds, "group_789");

// 批量创建 MailboxMessage
for (Long memberId : memberIds) {
    if (memberId.equals(123)) continue;  // 跳过发送者
    
    MailboxMessage msg = new MailboxMessage();
    msg.setUserId(memberId);
    msg.setConversationId("group_789");
    msg.setSequence(sequenceMap.get(memberId));
    msg.setMessageId("msg_002");
    // ... 其他字段
    mailboxMessageRepository.save(msg);
}
```

#### Step 3: 发送到 RocketMQ
```java
messageProducer.sendMessageWithTag("im-message-topic", "GROUP_CHAT", message);
```

#### Step 4: 消费者批量推送
```java
@Override
public void onMessage(Message message) {
    // 获取群成员
    List<Long> memberIds = groupService.getGroupMemberIds(message.getGroupId());
    
    // 推送给在线成员
    for (Long memberId : memberIds) {
        if (memberId.equals(message.getSenderId())) continue;
        
        if (userOnlineService.isOnline(memberId)) {
            webSocketService.sendMessage(memberId, message);
        }
    }
}
```

## 数据流转总结

```
┌─────────────────────────────────────────────────────────────┐
│                    消息发送流程                              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  1. MongoDB (message 集合)         │  ← 消息内容
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  2. MongoDB (mailbox 集合)         │  ← 每个接收者的信箱
        │     - user_mailbox                 │
        │     - mailbox_message              │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  3. MySQL (conversation 表)        │  ← 会话摘要
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  4. RocketMQ                       │  ← 异步推送
        └───────────────────────────────────┘
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
        ┌──────────────┐        ┌──────────────┐
        │  在线用户     │        │  离线用户     │
        │  WebSocket   │        │  Mailbox存储  │
        │  实时推送     │        │  上线后拉取   │
        └──────────────┘        └──────────────┘
```

## 关键点

1. **Mailbox 是核心**：保证消息不丢失
2. **RocketMQ 是辅助**：用于实时推送
3. **离线消息**：已经在 Mailbox，无需额外存储
4. **在线推送**：通过消息队列 + WebSocket
5. **消息可靠性**：多层保障（MongoDB + Mailbox + 拉取接口）
