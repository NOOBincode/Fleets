# MySQL 与 MongoDB 数据库设计指南

## 🎯 核心设计原则

### 数据库职责划分原则

```
MySQL (关系型数据库)
├─ 存储：结构化、关系型、需要事务的数据
├─ 特点：强一致性、ACID 事务、复杂查询
└─ 适用：用户、好友、群组等关系数据

MongoDB (文档型数据库)
├─ 存储：半结构化、海量、高写入的数据
├─ 特点：高性能、灵活 Schema、水平扩展
└─ 适用：消息内容、日志、时序数据
```

---

## 📊 数据存储职责划分

### MySQL 负责的数据（关系型、低频变更）

#### 1. 用户相关数据
```sql
✅ user 表
- 用户基本信息（id, username, password, nickname, avatar）
- 用户状态（status, last_login_time）
- 用户设置（gender, birth_date, signature）

原因：
- 用户信息变更频率低
- 需要唯一性约束（username, phone, email）
- 需要复杂查询（按条件搜索用户）
- 需要事务保证（注册时的数据一致性）
```

#### 2. 好友关系数据
```sql
✅ friendship 表
- 好友关系（user_id, friend_id）
- 关系状态（status: 待确认/已确认/已拒绝/已拉黑）
- 好友备注（remark）

原因：
- 典型的多对多关系，适合关系型数据库
- 需要双向查询（A的好友列表、谁加了A为好友）
- 需要唯一性约束（防止重复添加）
- 需要事务保证（添加好友的原子性）
```

#### 3. 群组相关数据
```sql
✅ group 表
- 群组信息（id, name, avatar, description）
- 群主信息（owner_id）
- 成员统计（max_member_count, current_member_count）

✅ group_member 表
- 群成员关系（group_id, user_id）
- 成员角色（role: 普通成员/管理员/群主）
- 禁言状态（mute_status, mute_end_time）

原因：
- 群组和成员是典型的一对多关系
- 需要复杂查询（查询用户的所有群组、群组的所有成员）
- 需要事务保证（创建群组时同时添加群主为成员）
- 需要统计功能（成员数量）
```

#### 4. 会话列表数据
```sql
✅ conversation 表
- 会话信息（conversation_id, type, owner_id, target_id）
- 未读数（unread_count）
- 最后消息（last_message_id, last_message_time）
- 会话设置（is_top, is_mute）

原因：
- 会话列表需要频繁查询和更新
- 需要排序（按最后消息时间）
- 需要统计（未读消息数）
- 适合用索引优化查询
```

#### 5. 文件元数据
```sql
✅ file 表
- 文件信息（id, file_name, file_type, file_size）
- 存储路径（file_path, file_url）
- 上传者（uploader_id）

原因：
- 文件元数据结构固定
- 需要关联查询（查询用户上传的所有文件）
- 需要统计（文件大小、数量）
```

---

### MongoDB 负责的数据（文档型、高频写入）

#### 1. 消息内容（核心！）
```javascript
✅ message 集合
{
  _id: ObjectId("..."),
  messageType: 1,           // 1-单聊, 2-群聊
  contentType: 1,           // 1-文本, 2-图片, 3-语音, 4-视频, 5-文件
  senderId: 123,
  receiverId: 456,          // 单聊时使用
  groupId: 789,             // 群聊时使用
  content: "消息内容",
  sequence: 1001,           // 消息序列号
  status: 1,                // 0-发送中, 1-已发送, 2-已送达, 3-已读, 4-撤回
  sendTime: ISODate("..."),
  extra: {                  // 扩展字段（灵活）
    fileUrl: "...",
    duration: 60,
    thumbnail: "..."
  }
}

原因：
- 消息量巨大，写入频率极高
- 消息内容结构灵活（文本、图片、语音等格式不同）
- 主要是追加写入，很少修改
- 需要按时间范围查询（分页加载历史消息）
- MongoDB 的文档模型完美适配
- 易于水平扩展（分片）
```

#### 2. 离线消息
```javascript
✅ offline_messages 集合
{
  _id: ObjectId("..."),
  userId: 456,              // 接收者ID
  messages: [               // 离线消息数组
    {
      messageId: "msg_001",
      senderId: 123,
      content: "你好",
      sendTime: ISODate("...")
    },
    ...
  ],
  createTime: ISODate("..."),
  expireTime: ISODate("...")  // 过期时间（7天后自动删除）
}

原因：
- 临时存储，用户上线后即可删除
- 数组结构方便批量操作
- 可以设置 TTL 索引自动过期
```

#### 3. 消息序列号（Mailbox）
```javascript
✅ mailboxes 集合
{
  _id: ObjectId("..."),
  userId: 123,
  conversationId: "conv_001",
  sequence: 1001,           // 当前序列号
  lastUpdateTime: ISODate("...")
}

原因：
- 用于保证消息顺序
- 高并发写入场景
- MongoDB 的原子操作（$inc）性能好
```

#### 4. 群消息已读记录（可选）
```javascript
✅ group_message_read 集合
{
  _id: ObjectId("..."),
  messageId: "msg_001",
  groupId: 789,
  readUsers: [              // 已读用户列表
    { userId: 123, readTime: ISODate("...") },
    { userId: 456, readTime: ISODate("...") }
  ]
}

原因：
- 群消息已读状态复杂（多人）
- 数组结构方便存储
- 查询灵活
```

---

## 🔄 数据同步策略

### 核心原则：**不需要完全同步！**

> ⚠️ 重要：MySQL 和 MongoDB 存储的是**不同维度**的数据，不是主从关系，所以**不需要双向同步**！

### 数据流向设计

```
┌─────────────────────────────────────────────────────────┐
│                    数据流向图                             │
└─────────────────────────────────────────────────────────┘

用户注册/登录
    ↓
  MySQL (user 表)
    ↓
  Redis (缓存用户信息)

添加好友
    ↓
  MySQL (friendship 表)
    ↓
  Redis (缓存好友列表)

创建群组
    ↓
  MySQL (group, group_member 表)
    ↓
  Redis (缓存群组信息)

发送消息
    ↓
  MongoDB (message 集合) ← 主存储
    ↓
  MySQL (conversation 表) ← 只存储摘要信息
    ↓
  Redis (缓存最新消息)
```

---

## 🔗 关键：会话表的设计（连接两个数据库）

### conversation 表是桥梁

```sql
CREATE TABLE `conversation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `conversation_id` varchar(64) NOT NULL COMMENT '会话唯一标识',
  `type` tinyint(1) NOT NULL COMMENT '0-单聊，1-群聊',
  `owner_id` bigint(20) NOT NULL COMMENT '会话所有者ID',
  `target_id` bigint(20) NOT NULL COMMENT '目标ID（用户ID或群组ID）',
  `unread_count` int(11) NOT NULL DEFAULT '0' COMMENT '未读消息数',
  
  -- 关键字段：连接到 MongoDB
  `last_message_id` varchar(64) DEFAULT NULL COMMENT '最后一条消息ID（MongoDB _id）',
  `last_message_content` varchar(500) DEFAULT NULL COMMENT '最后一条消息内容（冗余）',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后一条消息时间',
  
  `is_top` tinyint(1) NOT NULL DEFAULT '0',
  `is_mute` tinyint(1) NOT NULL DEFAULT '0',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_conversation_owner` (`conversation_id`,`owner_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';
```

### 工作原理

```
1. 用户A发送消息给用户B
   ↓
2. 保存消息到 MongoDB message 集合
   返回 messageId = "507f1f77bcf86cd799439011"
   ↓
3. 更新 MySQL conversation 表
   UPDATE conversation SET
     last_message_id = "507f1f77bcf86cd799439011",
     last_message_content = "你好",  -- 冗余存储，方便显示
     last_message_time = NOW(),
     unread_count = unread_count + 1
   WHERE owner_id = B AND target_id = A
   ↓
4. 用户B查询会话列表
   SELECT * FROM conversation WHERE owner_id = B
   ORDER BY last_message_time DESC
   ↓
5. 用户B点击会话，查询历史消息
   从 MongoDB 查询：
   db.message.find({
     $or: [
       { senderId: A, receiverId: B },
       { senderId: B, receiverId: A }
     ]
   }).sort({ sendTime: -1 }).limit(20)
```

---

## 💡 具体实现方案

### 方案1：应用层同步（推荐 ⭐⭐⭐⭐⭐）

**适用场景：** 毕设项目、中小型系统

**实现方式：** 在业务代码中同时操作两个数据库

```java
@Service
public class MessageServiceImpl implements MessageService {
    
    @Autowired
    private MessageRepository messageRepository;  // MongoDB
    
    @Autowired
    private ConversationMapper conversationMapper;  // MySQL
    
    @Autowired
    private MessageProducer messageProducer;  // RocketMQ
    
    @Override
    @Transactional  // 注意：这里的事务只对 MySQL 有效
    public MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO) {
        // 1. 构建消息对象
        Message message = new Message();
        message.setMessageType(sendDTO.getMessageType());
        message.setContentType(sendDTO.getContentType());
        message.setSenderId(senderId);
        message.setReceiverId(sendDTO.getReceiverId());
        message.setContent(sendDTO.getContent());
        message.setSendTime(new Date());
        message.setStatus(1);  // 已发送
        
        // 2. 保存到 MongoDB（主存储）
        Message savedMessage = messageRepository.save(message).block();
        String messageId = savedMessage.getId();
        
        try {
            // 3. 更新发送者的会话表（MySQL）
            updateConversation(senderId, sendDTO.getReceiverId(), 
                messageId, sendDTO.getContent(), false);
            
            // 4. 更新接收者的会话表（MySQL）
            updateConversation(sendDTO.getReceiverId(), senderId, 
                messageId, sendDTO.getContent(), true);
            
            // 5. 发送到消息队列
            messageProducer.sendMessage("im-message-topic", savedMessage);
            
        } catch (Exception e) {
            // 如果 MySQL 更新失败，记录日志，但不影响消息发送
            log.error("更新会话表失败，messageId: {}", messageId, e);
            // 可以通过定时任务或补偿机制修复
        }
        
        return convertToVO(savedMessage);
    }
    
    /**
     * 更新会话表
     */
    private void updateConversation(Long ownerId, Long targetId, 
                                    String messageId, String content, 
                                    boolean incrementUnread) {
        // 生成会话ID（保证双方会话ID一致）
        String conversationId = generateConversationId(ownerId, targetId);
        
        // 查询会话是否存在
        Conversation conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("owner_id", ownerId)
        );
        
        if (conversation == null) {
            // 创建新会话
            conversation = new Conversation();
            conversation.setConversationId(conversationId);
            conversation.setType(0);  // 单聊
            conversation.setOwnerId(ownerId);
            conversation.setTargetId(targetId);
            conversation.setUnreadCount(incrementUnread ? 1 : 0);
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageContent(truncate(content, 100));
            conversation.setLastMessageTime(new Date());
            conversationMapper.insert(conversation);
        } else {
            // 更新会话
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageContent(truncate(content, 100));
            conversation.setLastMessageTime(new Date());
            if (incrementUnread) {
                conversation.setUnreadCount(conversation.getUnreadCount() + 1);
            }
            conversationMapper.updateById(conversation);
        }
    }
    
    /**
     * 生成会话ID（单聊）
     */
    private String generateConversationId(Long userId1, Long userId2) {
        // 保证会话ID唯一且双向一致
        long min = Math.min(userId1, userId2);
        long max = Math.max(userId1, userId2);
        return "conv_" + min + "_" + max;
    }
}
```

**优点：**
- ✅ 实现简单，逻辑清晰
- ✅ 容易理解和维护
- ✅ 适合毕设项目

**缺点：**
- ❌ 无法保证两个数据库的强一致性
- ❌ 需要手动处理失败情况

---

### 方案2：事件驱动同步（进阶 ⭐⭐⭐⭐）

**适用场景：** 对一致性要求较高的场景

**实现方式：** 通过消息队列异步同步

```java
@Service
public class MessageServiceImpl implements MessageService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private MessageProducer messageProducer;
    
    @Override
    public MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO) {
        // 1. 保存到 MongoDB
        Message message = buildMessage(sendDTO);
        Message savedMessage = messageRepository.save(message).block();
        
        // 2. 发送事件到 RocketMQ
        MessageSavedEvent event = new MessageSavedEvent();
        event.setMessageId(savedMessage.getId());
        event.setSenderId(senderId);
        event.setReceiverId(sendDTO.getReceiverId());
        event.setContent(sendDTO.getContent());
        event.setSendTime(savedMessage.getSendTime());
        
        messageProducer.sendMessage("message-saved-topic", event);
        
        return convertToVO(savedMessage);
    }
}

// 消费者：监听消息保存事件，更新会话表
@Component
@RocketMQMessageListener(
    topic = "message-saved-topic",
    consumerGroup = "conversation-sync-group"
)
public class ConversationSyncConsumer implements RocketMQListener<MessageSavedEvent> {
    
    @Autowired
    private ConversationMapper conversationMapper;
    
    @Override
    public void onMessage(MessageSavedEvent event) {
        try {
            // 更新发送者会话
            updateConversation(event.getSenderId(), event.getReceiverId(), 
                event.getMessageId(), event.getContent(), false);
            
            // 更新接收者会话
            updateConversation(event.getReceiverId(), event.getSenderId(), 
                event.getMessageId(), event.getContent(), true);
                
        } catch (Exception e) {
            log.error("同步会话表失败", e);
            // 重试或记录到失败表
            throw e;  // 触发 RocketMQ 重试机制
        }
    }
}
```

**优点：**
- ✅ 解耦，MongoDB 和 MySQL 操作独立
- ✅ 失败可以重试
- ✅ 易于扩展（可以添加更多消费者）

**缺点：**
- ❌ 存在延迟（最终一致性）
- ❌ 实现复杂度较高

---

### 方案3：定时同步（补偿机制 ⭐⭐⭐）

**适用场景：** 作为方案1或方案2的补充

**实现方式：** 定时任务检查并修复不一致的数据

```java
@Component
public class ConversationSyncTask {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ConversationMapper conversationMapper;
    
    /**
     * 每小时执行一次，同步最近1小时的消息
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncRecentMessages() {
        Date oneHourAgo = new Date(System.currentTimeMillis() - 3600000);
        
        // 1. 从 MongoDB 查询最近1小时的消息
        List<Message> recentMessages = messageRepository
            .findBySendTimeAfter(oneHourAgo)
            .collectList()
            .block();
        
        // 2. 检查每条消息对应的会话是否已更新
        for (Message message : recentMessages) {
            checkAndSyncConversation(message);
        }
    }
    
    private void checkAndSyncConversation(Message message) {
        // 检查会话表的 last_message_id 是否匹配
        // 如果不匹配，说明之前的更新失败了，需要补偿
    }
}
```

---

## 🎯 推荐方案（毕设项目）

### 组合方案：方案1 + 方案3

```
主流程：应用层同步（方案1）
  ├─ 发送消息时同时更新 MongoDB 和 MySQL
  └─ 简单直接，易于理解

补偿机制：定时同步（方案3）
  ├─ 每小时检查一次数据一致性
  └─ 修复失败的同步操作
```

---

## 📋 数据一致性保证

### 1. 最终一致性（推荐）

```
原则：
- MongoDB 是消息的主存储（Source of Truth）
- MySQL 存储的是摘要信息（可以重建）
- 允许短暂的不一致，但最终会一致

实现：
- 主流程：尽力保证同步
- 补偿机制：定时任务修复
- 查询时：优先从 MongoDB 查询完整数据
```

### 2. 幂等性设计

```java
// 更新会话表时，使用幂等操作
UPDATE conversation SET
  last_message_id = ?,
  last_message_time = ?,
  unread_count = unread_count + 1
WHERE owner_id = ? AND target_id = ?
  AND (last_message_time IS NULL OR last_message_time < ?)
  -- 只有当新消息时间更晚时才更新
```

### 3. 失败处理

```java
try {
    // 更新 MySQL
    updateConversation(...);
} catch (Exception e) {
    // 记录失败日志
    log.error("更新会话表失败", e);
    
    // 写入失败队列（可选）
    failedSyncQueue.add(new SyncTask(...));
    
    // 不影响消息发送（MongoDB 已保存成功）
}
```

---

## 🔍 查询策略

### 会话列表查询（从 MySQL）

```java
// 查询用户的会话列表
List<Conversation> conversations = conversationMapper.selectList(
    new QueryWrapper<Conversation>()
        .eq("owner_id", userId)
        .eq("is_deleted", 0)
        .orderByDesc("last_message_time")
        .last("LIMIT 20")
);

// 返回结果包含：
// - 会话ID
// - 对方信息（从 user 表 JOIN）
// - 最后一条消息内容（冗余字段）
// - 未读消息数
```

### 消息历史查询（从 MongoDB）

```java
// 查询单聊历史消息
Flux<Message> messages = messageRepository.findBySenderIdAndReceiverIdOrderBySendTimeDesc(
    userId, targetUserId
);

// 或者使用自定义查询
Query query = new Query();
query.addCriteria(new Criteria().orOperator(
    Criteria.where("senderId").is(userId).and("receiverId").is(targetUserId),
    Criteria.where("senderId").is(targetUserId).and("receiverId").is(userId)
));
query.with(Sort.by(Sort.Direction.DESC, "sendTime"));
query.limit(20);

List<Message> messages = mongoTemplate.find(query, Message.class);
```

---

## 📊 性能优化建议

### 1. MySQL 索引优化

```sql
-- conversation 表索引
CREATE INDEX idx_owner_time ON conversation(owner_id, last_message_time);
CREATE INDEX idx_conversation_owner ON conversation(conversation_id, owner_id);

-- 查询会话列表时使用
EXPLAIN SELECT * FROM conversation 
WHERE owner_id = 123 
ORDER BY last_message_time DESC 
LIMIT 20;
```

### 2. MongoDB 索引优化

```javascript
// message 集合索引
db.message.createIndex({ senderId: 1, receiverId: 1, sendTime: -1 });
db.message.createIndex({ groupId: 1, sendTime: -1 });
db.message.createIndex({ sendTime: -1 });

// 查询单聊消息时使用
db.message.find({
  $or: [
    { senderId: 123, receiverId: 456 },
    { senderId: 456, receiverId: 123 }
  ]
}).sort({ sendTime: -1 }).limit(20);
```

### 3. 缓存策略

```java
// 会话列表缓存（Redis）
String cacheKey = "conversation:list:" + userId;
List<Conversation> conversations = redisService.get(cacheKey);

if (conversations == null) {
    conversations = conversationMapper.selectList(...);
    redisService.set(cacheKey, conversations, 5, TimeUnit.MINUTES);
}
```

---

## ✅ 总结

### 数据存储职责

| 数据类型 | 存储位置 | 原因 |
|---------|---------|------|
| 用户信息 | MySQL | 结构化、需要唯一约束 |
| 好友关系 | MySQL | 关系型数据、需要事务 |
| 群组信息 | MySQL | 关系型数据、需要统计 |
| 会话列表 | MySQL | 需要排序、统计未读数 |
| 消息内容 | MongoDB | 海量数据、高写入、灵活Schema |
| 离线消息 | MongoDB | 临时数据、数组结构 |

### 同步策略

1. **不需要完全同步** - 两个数据库存储不同维度的数据
2. **应用层同步** - 发送消息时同时更新两个数据库
3. **最终一致性** - 允许短暂不一致，通过补偿机制修复
4. **MongoDB 为主** - 消息内容以 MongoDB 为准
5. **MySQL 为辅** - 存储摘要信息，方便查询会话列表

### 实现建议

```
✅ 推荐：方案1（应用层同步）+ 方案3（定时补偿）
✅ 简单：直接在 Service 层同时操作两个数据库
✅ 可靠：添加异常处理和日志记录
✅ 补偿：定时任务检查并修复不一致
```

这样的设计既能满足毕设要求，又具有实际项目的参考价值！
