# 数据库设计问题总结

## 🔴 严重问题（必须修复）

### 1. conversation.last_message_id 类型错误 ⚠️

**问题：**
```sql
-- 当前（错误）
`last_message_id` bigint(20) DEFAULT NULL

-- MongoDB 的 _id 是这样的：
ObjectId("507f1f77bcf86cd799439011")  // 24位十六进制字符串
```

**影响：** 无法存储 MongoDB 的 ObjectId，导致无法关联数据！

**修复：**
```sql
`last_message_id` varchar(64) DEFAULT NULL
```

---

### 2. conversation 表缺少冗余字段 ⚠️

**问题：** 查询会话列表时，需要显示最后一条消息内容，但没有冗余字段

**影响：** 需要去 MongoDB 查询，导致 N+1 查询问题，性能差

**修复：**
```sql
`last_message_content` varchar(500) DEFAULT NULL,
`last_sender_id` bigint(20) DEFAULT NULL,
`last_sender_name` varchar(64) DEFAULT NULL
```

---

### 3. friendship 表缺少 group_name 字段 ⚠️

**问题：** 实体类有 `groupName` 字段，但数据库表没有

**影响：** 运行时会报错！

**修复：**
```sql
`group_name` varchar(64) DEFAULT '我的好友'
```

---

## 🟡 重要问题（建议修复）

### 4. 缺少索引优化

**问题：** 查询会话列表时需要排序，但没有复合索引

**影响：** 查询性能差

**修复：**
```sql
KEY `idx_owner_time` (`owner_id`, `last_message_time` DESC),
KEY `idx_owner_top_time` (`owner_id`, `is_top` DESC, `last_message_time` DESC)
```

---

### 5. MongoDB 没有索引

**问题：** 查询消息历史时会全表扫描

**影响：** 性能极差！

**修复：**
```javascript
db.message.createIndex({ senderId: 1, receiverId: 1, sendTime: -1 });
db.message.createIndex({ groupId: 1, sendTime: -1 });
```

---

### 6. group 表字段命名不一致

**问题：** 数据库用 `name`，实体类用 `groupName`

**影响：** 需要手动映射，容易出错

**修复：**
```sql
-- 统一命名
`group_name` varchar(64) NOT NULL
```

---

## 🔵 可选优化

### 7. 添加业务字段

```sql
-- group 表
`announcement` varchar(500) DEFAULT NULL COMMENT '群公告',
`join_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '加群方式',

-- group_member 表
`mute_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '禁言状态'
```

---

## 🚀 快速修复方案

### 步骤1：执行 MySQL 迁移脚本

```bash
mysql -u root -p fleets < src/main/java/docker/mysql/migration_v1_to_v2.sql
```

### 步骤2：创建 MongoDB 索引

```bash
mongo fleets src/main/java/docker/mongodb/create_indexes.js
```

### 步骤3：更新实体类

已自动更新：
- ✅ `Conversation.java` - 添加了 lastSenderId 和 lastSenderName
- ✅ `Group.java` - 添加了 @TableField("group_name")
- ✅ `GroupMember.java` - 添加了 @TableField 注解
- ✅ `Friendship.java` - 添加了 @TableField("group_name")

---

## 📊 修复前后对比

### 会话列表查询

**修复前：**
```java
// 1. 查询会话列表（MySQL）
List<Conversation> conversations = conversationMapper.selectList(...);

// 2. 循环查询每条消息内容（MongoDB）- N+1 问题！
for (Conversation conv : conversations) {
    Message message = messageRepository.findById(conv.getLastMessageId()).block();
    conv.setLastMessageContent(message.getContent());
}
```

**修复后：**
```java
// 一次查询搞定（MySQL）
List<Conversation> conversations = conversationMapper.selectList(...);
// lastMessageContent 已经冗余存储，无需再查 MongoDB！
```

---

## ✅ 总结

### 必须修复的问题（3个）
1. ✅ conversation.last_message_id 类型改为 varchar(64)
2. ✅ conversation 表添加冗余字段
3. ✅ friendship 表添加 group_name 字段

### 建议修复的问题（3个）
4. ✅ 添加 MySQL 复合索引
5. ✅ 创建 MongoDB 索引
6. ✅ 统一 group 表字段命名

### 可选优化（1个）
7. ✅ 添加业务字段（announcement, join_type, mute_status）

---

## 📝 执行清单

- [ ] 备份数据库
- [ ] 执行 MySQL 迁移脚本
- [ ] 创建 MongoDB 索引
- [ ] 重启应用
- [ ] 测试功能是否正常

修复后，你的数据库设计会更加合理和高效！🎉
