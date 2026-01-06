# 数据库设计问题分析与优化建议

## 🔍 当前设计问题分析

### ❌ 问题1：conversation 表的 last_message_id 字段类型错误

**当前设计：**
```sql
`last_message_id` bigint(20) DEFAULT NULL COMMENT '最后一条消息ID'
```

**问题：**
- MongoDB 的 `_id` 是 ObjectId 类型（24位十六进制字符串）
- 例如：`507f1f77bcf86cd799439011`
- 但你的 MySQL 字段定义为 `bigint(20)`，无法存储 ObjectId！

**影响：**
- 无法正确关联 MySQL 和 MongoDB 的数据
- 会话表无法引用到具体的消息

**解决方案：**
```sql
-- 修改为 varchar 类型
`last_message_id` varchar(64) DEFAULT NULL COMMENT '最后一条消息ID（MongoDB ObjectId）'
```

---

### ❌ 问题2：conversation 表缺少 last_message_content 字段

**当前设计：**
```sql
-- 只有 last_message_id 和 last_message_time
-- 缺少消息内容字段
```

**问题：**
- 查询会话列表时，需要显示最后一条消息的内容
- 如果不冗余存储，就需要去 MongoDB 查询
- 会导致性能问题（N+1 查询）

**影响：**
- 会话列表加载慢
- 需要多次查询数据库

**解决方案：**
```sql
-- 添加冗余字段
`last_message_content` varchar(500) DEFAULT NULL COMMENT '最后一条消息内容（冗余）',
`last_sender_id` bigint(20) DEFAULT NULL COMMENT '最后一条消息发送者ID',
`last_sender_name` varchar(64) DEFAULT NULL COMMENT '最后一条消息发送者昵称（冗余）'
```

---

### ❌ 问题3：MongoDB message 集合字段设计不一致

**当前设计（message.json）：**
```json
{
  "message_id": "NumberLong()",      // ❌ 冗余字段
  "conversation_id": "String",       // ❌ 应该由应用层计算
  "sender_type": "Number",           // ❌ 不必要的字段
  "mentions": ["NumberLong()"],      // ✅ 好的设计
  "reply_to": "NumberLong()",        // ✅ 好的设计
  "is_recalled": "Boolean"           // ❌ 应该用 status 字段
}
```

**问题：**
- `message_id`：MongoDB 已经有 `_id`，不需要额外的 message_id
- `conversation_id`：应该由应用层根据 senderId 和 receiverId 计算
- `sender_type`：不清楚用途，可能不需要
- `is_recalled`：与 status 字段重复

**解决方案：**
```json
{
  "_id": "ObjectId()",               // MongoDB 自动生成
  "messageType": 1,                  // 1-单聊, 2-群聊, 3-系统消息
  "contentType": 1,                  // 1-文本, 2-图片, 3-语音, 4-视频, 5-文件
  "senderId": 123,
  "receiverId": 456,                 // 单聊时使用
  "groupId": 789,                    // 群聊时使用
  "content": "消息内容",
  "sequence": 1001,                  // 消息序列号
  "status": 1,                       // 0-发送中, 1-已发送, 2-已送达, 3-已读, 4-撤回
  "sendTime": "ISODate()",
  "extra": {                         // 扩展字段
    "replyTo": "ObjectId()",         // 回复的消息ID
    "mentions": [123, 456],          // @的用户ID列表
    "fileUrl": "http://...",         // 文件URL
    "duration": 60                   // 语音/视频时长
  }
}
```

---

### ❌ 问题4：friendship 表缺少 group_name 字段

**当前设计：**
```sql
`remark` varchar(64) DEFAULT NULL COMMENT '好友备注'
-- 缺少分组字段
```

**问题：**
- 你的实体类 `Friendship.java` 有 `groupName` 字段
- 但数据库表没有对应字段
- 会导致运行时错误

**解决方案：**
```sql
`remark` varchar(64) DEFAULT NULL COMMENT '好友备注',
`group_name` varchar(64) DEFAULT '我的好友' COMMENT '好友分组',
```

---

### ❌ 问题5：group 表字段命名不一致

**当前设计：**
```sql
`name` varchar(64) NOT NULL COMMENT '群组名称'
```

**问题：**
- 你的实体类 `Group.java` 使用 `groupName`
- 但数据库表使用 `name`
- 需要在实体类中使用 `@TableField("name")` 注解

**建议：**
```sql
-- 统一命名，避免混淆
`group_name` varchar(64) NOT NULL COMMENT '群组名称',
`announcement` varchar(500) DEFAULT NULL COMMENT '群公告',
```

---

### ⚠️ 问题6：缺少索引优化

**当前设计：**
```sql
-- conversation 表只有基础索引
KEY `idx_owner_id` (`owner_id`)
```

**问题：**
- 查询会话列表时需要按 `last_message_time` 排序
- 缺少复合索引会导致性能问题

**解决方案：**
```sql
-- 添加复合索引
KEY `idx_owner_time` (`owner_id`, `last_message_time` DESC),
KEY `idx_owner_top_time` (`owner_id`, `is_top` DESC, `last_message_time` DESC)
```

---

### ⚠️ 问题7：MongoDB 缺少索引定义

**当前设计：**
- 只有 JSON 示例，没有索引定义

**问题：**
- 查询消息历史时会全表扫描
- 性能极差

**解决方案：**
```javascript
// 创建索引脚本
db.message.createIndex({ senderId: 1, receiverId: 1, sendTime: -1 });
db.message.createIndex({ groupId: 1, sendTime: -1 });
db.message.createIndex({ sendTime: -1 });
db.message.createIndex({ status: 1, sendTime: -1 });

// 离线消息索引
db.offline_messages.createIndex({ user_id: 1, created_at: -1 });
db.offline_messages.createIndex({ expire_at: 1 }, { expireAfterSeconds: 0 });  // TTL索引

// 邮箱索引
db.mailboxes.createIndex({ user_id: 1, sequence_id: -1 });
db.mailboxes.createIndex({ conversation_id: 1, sequence_id: -1 });
```

---

### ⚠️ 问题8：sequence 表设计不合理

**当前设计：**
```sql
CREATE TABLE `sequence` (
  `name` varchar(64) NOT NULL,
  `current_value` bigint(20) NOT NULL DEFAULT '0'
)
```

**问题：**
- 用 MySQL 生成消息序列号会成为性能瓶颈
- 高并发下会有锁竞争

**建议：**
- 使用 MongoDB 的原子操作生成序列号
- 或者使用 Redis 的 INCR 命令
- 或者使用雪花算法（Snowflake）

**解决方案：**
```java
// 方案1：使用 MongoDB 原子操作
db.mailboxes.findAndModify({
  query: { user_id: 123, conversation_id: "conv_123_456" },
  update: { $inc: { sequence_id: 1 } },
  new: true,
  upsert: true
});

// 方案2：使用 Redis
Long sequence = redisTemplate.opsForValue().increment("seq:conv_123_456");

// 方案3：雪花算法（推荐）
@Component
public class SnowflakeIdGenerator {
    private final Snowflake snowflake = new Snowflake(1, 1);
    
    public long nextId() {
        return snowflake.nextId();
    }
}
```

---

## ✅ 优化后的数据库设计

### MySQL 表结构优化

#### 1. conversation 表（优化版）

```sql
CREATE TABLE `conversation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `conversation_id` varchar(64) NOT NULL COMMENT '会话唯一标识',
  `type` tinyint(1) NOT NULL COMMENT '会话类型：0-单聊，1-群聊',
  `owner_id` bigint(20) NOT NULL COMMENT '会话所有者ID',
  `target_id` bigint(20) NOT NULL COMMENT '目标ID（用户ID或群组ID）',
  `unread_count` int(11) NOT NULL DEFAULT '0' COMMENT '未读消息数',
  
  -- ========== 优化：修改字段类型和添加冗余字段 ==========
  `last_message_id` varchar(64) DEFAULT NULL COMMENT '最后一条消息ID（MongoDB ObjectId）',
  `last_message_content` varchar(500) DEFAULT NULL COMMENT '最后一条消息内容（冗余）',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后一条消息时间',
  `last_sender_id` bigint(20) DEFAULT NULL COMMENT '最后一条消息发送者ID',
  `last_sender_name` varchar(64) DEFAULT NULL COMMENT '最后一条消息发送者昵称（冗余）',
  -- ======================================================
  
  `is_top` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否置顶：0-否，1-是',
  `is_mute` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否免打扰：0-否，1-是',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_conversation_owner` (`conversation_id`, `owner_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_target_id` (`target_id`),
  
  -- ========== 优化：添加复合索引 ==========
  KEY `idx_owner_time` (`owner_id`, `last_message_time` DESC),
  KEY `idx_owner_top_time` (`owner_id`, `is_top` DESC, `last_message_time` DESC),
  -- ======================================
  
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';
```

#### 2. friendship 表（优化版）

```sql
CREATE TABLE `friendship` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `friend_id` bigint(20) NOT NULL COMMENT '好友ID',
  `remark` varchar(64) DEFAULT NULL COMMENT '好友备注',
  
  -- ========== 优化：添加分组字段 ==========
  `group_name` varchar(64) DEFAULT '我的好友' COMMENT '好友分组',
  -- ======================================
  
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：0-待确认，1-已确认，2-已拒绝，3-已拉黑',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_friend` (`user_id`, `friend_id`),
  KEY `idx_friend_id` (`friend_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  
  -- ========== 优化：添加分组索引 ==========
  KEY `idx_user_group` (`user_id`, `group_name`)
  -- ======================================
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';
```

#### 3. group 表（优化版）

```sql
CREATE TABLE `group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '群组ID',
  
  -- ========== 优化：统一命名 ==========
  `group_name` varchar(64) NOT NULL COMMENT '群组名称',
  -- ===================================
  
  `avatar` varchar(255) DEFAULT NULL COMMENT '群头像URL',
  `description` varchar(255) DEFAULT NULL COMMENT '群描述',
  
  -- ========== 优化：添加群公告字段 ==========
  `announcement` varchar(500) DEFAULT NULL COMMENT '群公告',
  -- ========================================
  
  `owner_id` bigint(20) NOT NULL COMMENT '群主ID',
  `max_member_count` int(11) NOT NULL DEFAULT '200' COMMENT '最大成员数',
  `current_member_count` int(11) NOT NULL DEFAULT '1' COMMENT '当前成员数',
  
  -- ========== 优化：添加加群方式字段 ==========
  `join_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '加群方式：0-无需验证，1-需要验证，2-禁止加群',
  -- =========================================
  
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  
  -- ========== 优化：添加群名搜索索引 ==========
  KEY `idx_group_name` (`group_name`)
  -- =========================================
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';
```

#### 4. group_member 表（优化版）

```sql
CREATE TABLE `group_member` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `group_id` bigint(20) NOT NULL COMMENT '群组ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  
  -- ========== 优化：统一命名 ==========
  `group_nickname` varchar(64) DEFAULT NULL COMMENT '群内昵称',
  -- ===================================
  
  `role` tinyint(1) NOT NULL DEFAULT '0' COMMENT '角色：0-普通成员，1-管理员，2-群主',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  
  -- ========== 优化：添加禁言状态字段 ==========
  `mute_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '禁言状态：0-正常，1-禁言',
  -- =========================================
  
  `mute_end_time` datetime DEFAULT NULL COMMENT '禁言结束时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_group_user` (`group_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  
  -- ========== 优化：添加角色索引 ==========
  KEY `idx_group_role` (`group_id`, `role`)
  -- ======================================
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组成员表';
```

---

### MongoDB 集合优化

#### 1. message 集合（优化版）

```javascript
// 集合结构
{
  "_id": ObjectId("507f1f77bcf86cd799439011"),
  "messageType": 1,              // 1-单聊, 2-群聊, 3-系统消息
  "contentType": 1,              // 1-文本, 2-图片, 3-语音, 4-视频, 5-文件
  "senderId": NumberLong(123),
  "receiverId": NumberLong(456), // 单聊时使用
  "groupId": NumberLong(789),    // 群聊时使用
  "content": "消息内容",
  "sequence": NumberLong(1001),  // 消息序列号
  "status": 1,                   // 0-发送中, 1-已发送, 2-已送达, 3-已读, 4-撤回
  "sendTime": ISODate("2024-12-03T10:00:00Z"),
  "extra": {                     // 扩展字段（灵活）
    "replyTo": ObjectId("..."),  // 回复的消息ID
    "mentions": [123, 456],      // @的用户ID列表
    "fileUrl": "http://...",     // 文件URL
    "fileName": "image.jpg",     // 文件名
    "fileSize": 1024000,         // 文件大小
    "duration": 60,              // 语音/视频时长（秒）
    "thumbnail": "http://..."    // 缩略图URL
  }
}

// 索引定义
db.message.createIndex({ senderId: 1, receiverId: 1, sendTime: -1 });
db.message.createIndex({ receiverId: 1, senderId: 1, sendTime: -1 });
db.message.createIndex({ groupId: 1, sendTime: -1 });
db.message.createIndex({ sendTime: -1 });
db.message.createIndex({ status: 1 });
```

#### 2. offline_messages 集合（优化版）

```javascript
// 集合结构
{
  "_id": ObjectId("..."),
  "userId": NumberLong(456),
  "messageId": ObjectId("507f1f77bcf86cd799439011"),  // 引用 message._id
  "senderId": NumberLong(123),
  "contentType": 1,
  "createTime": ISODate("2024-12-03T10:00:00Z"),
  "expireTime": ISODate("2024-12-10T10:00:00Z")  // 7天后过期
}

// 索引定义
db.offline_messages.createIndex({ userId: 1, createTime: -1 });
db.offline_messages.createIndex({ expireTime: 1 }, { expireAfterSeconds: 0 });  // TTL索引
```

#### 3. mailboxes 集合（简化版）

```javascript
// 建议：使用 Redis 或雪花算法代替
// 如果必须使用 MongoDB，简化设计：

{
  "_id": ObjectId("..."),
  "userId": NumberLong(123),
  "conversationId": "conv_123_456",
  "sequence": NumberLong(1001)  // 当前序列号
}

// 索引定义
db.mailboxes.createIndex({ userId: 1, conversationId: 1 }, { unique: true });
```

---

## 🔧 数据库迁移脚本

### MySQL 迁移脚本

```sql
-- 1. 修改 conversation 表
ALTER TABLE `conversation` 
  MODIFY COLUMN `last_message_id` varchar(64) DEFAULT NULL COMMENT '最后一条消息ID（MongoDB ObjectId）',
  ADD COLUMN `last_message_content` varchar(500) DEFAULT NULL COMMENT '最后一条消息内容（冗余）' AFTER `last_message_id`,
  ADD COLUMN `last_sender_id` bigint(20) DEFAULT NULL COMMENT '最后一条消息发送者ID' AFTER `last_message_content`,
  ADD COLUMN `last_sender_name` varchar(64) DEFAULT NULL COMMENT '最后一条消息发送者昵称（冗余）' AFTER `last_sender_id`,
  ADD KEY `idx_owner_time` (`owner_id`, `last_message_time` DESC),
  ADD KEY `idx_owner_top_time` (`owner_id`, `is_top` DESC, `last_message_time` DESC);

-- 2. 修改 friendship 表
ALTER TABLE `friendship`
  ADD COLUMN `group_name` varchar(64) DEFAULT '我的好友' COMMENT '好友分组' AFTER `remark`,
  ADD KEY `idx_user_group` (`user_id`, `group_name`);

-- 3. 修改 group 表
ALTER TABLE `group`
  CHANGE COLUMN `name` `group_name` varchar(64) NOT NULL COMMENT '群组名称',
  ADD COLUMN `announcement` varchar(500) DEFAULT NULL COMMENT '群公告' AFTER `description`,
  ADD COLUMN `join_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '加群方式：0-无需验证，1-需要验证，2-禁止加群' AFTER `current_member_count`,
  ADD KEY `idx_group_name` (`group_name`);

-- 4. 修改 group_member 表
ALTER TABLE `group_member`
  CHANGE COLUMN `nickname` `group_nickname` varchar(64) DEFAULT NULL COMMENT '群内昵称',
  ADD COLUMN `mute_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '禁言状态：0-正常，1-禁言' AFTER `role`,
  ADD KEY `idx_group_role` (`group_id`, `role`);
```

### MongoDB 索引创建脚本

```javascript
// 连接到 MongoDB
use fleets;

// 创建 message 集合索引
db.message.createIndex({ senderId: 1, receiverId: 1, sendTime: -1 });
db.message.createIndex({ receiverId: 1, senderId: 1, sendTime: -1 });
db.message.createIndex({ groupId: 1, sendTime: -1 });
db.message.createIndex({ sendTime: -1 });
db.message.createIndex({ status: 1 });

// 创建 offline_messages 集合索引
db.offline_messages.createIndex({ userId: 1, createTime: -1 });
db.offline_messages.createIndex({ expireTime: 1 }, { expireAfterSeconds: 0 });

// 创建 mailboxes 集合索引
db.mailboxes.createIndex({ userId: 1, conversationId: 1 }, { unique: true });

// 查看索引
db.message.getIndexes();
db.offline_messages.getIndexes();
db.mailboxes.getIndexes();
```

---

## 📊 性能优化建议

### 1. 查询优化

```sql
-- ❌ 不好的查询（没有使用索引）
SELECT * FROM conversation WHERE owner_id = 123 ORDER BY create_time DESC;

-- ✅ 好的查询（使用复合索引）
SELECT * FROM conversation 
WHERE owner_id = 123 AND is_deleted = 0
ORDER BY is_top DESC, last_message_time DESC
LIMIT 20;
```

### 2. MongoDB 查询优化

```javascript
// ❌ 不好的查询（全表扫描）
db.message.find({ senderId: 123 }).sort({ sendTime: -1 });

// ✅ 好的查询（使用索引）
db.message.find({ 
  senderId: 123, 
  receiverId: 456 
}).sort({ sendTime: -1 }).limit(20);

// 使用 explain 查看执行计划
db.message.find({ 
  senderId: 123, 
  receiverId: 456 
}).sort({ sendTime: -1 }).explain("executionStats");
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

### 主要问题

1. ❌ **conversation.last_message_id 类型错误** - 应该是 varchar(64)
2. ❌ **缺少冗余字段** - 需要 last_message_content 等
3. ❌ **MongoDB 字段设计不合理** - 有冗余和不必要的字段
4. ❌ **friendship 表缺少 group_name** - 与实体类不匹配
5. ❌ **group 表命名不一致** - name vs groupName
6. ⚠️ **缺少复合索引** - 影响查询性能
7. ⚠️ **MongoDB 没有索引** - 严重影响性能
8. ⚠️ **sequence 表设计不合理** - 会成为性能瓶颈

### 优化建议优先级

**高优先级（必须修改）：**
1. ✅ 修改 conversation.last_message_id 为 varchar(64)
2. ✅ 添加 conversation.last_message_content 等冗余字段
3. ✅ 添加 friendship.group_name 字段
4. ✅ 创建 MongoDB 索引

**中优先级（建议修改）：**
5. ✅ 统一 group 表字段命名
6. ✅ 添加 MySQL 复合索引
7. ✅ 优化 MongoDB message 集合结构

**低优先级（可选）：**
8. ✅ 使用雪花算法替代 sequence 表
9. ✅ 添加更多业务字段（announcement, join_type 等）

按照这个优化方案，你的数据库设计会更加合理和高效！
