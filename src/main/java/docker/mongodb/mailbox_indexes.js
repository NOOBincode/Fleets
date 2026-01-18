// ========================================
// Mailbox 模块索引创建脚本
// ========================================

print("\n========================================");
print("开始创建 Mailbox 模块索引");
print("========================================\n");

// 切换到数据库
db = db.getSiblingDB('fleets_im');

// ========================================
// 1. user_mailbox 集合索引
// ========================================
print("创建 user_mailbox 集合索引...");

// 用户会话唯一索引
db.user_mailbox.createIndex(
  { userId: 1, conversationId: 1 },
  { 
    unique: true,
    name: "idx_user_conversation_unique"
  }
);

// 用户ID索引（查询用户所有信箱）
db.user_mailbox.createIndex(
  { userId: 1, sequence: -1 },
  { name: "idx_user_sequence" }
);

// 会话类型索引
db.user_mailbox.createIndex(
  { userId: 1, conversationType: 1 },
  { name: "idx_user_conversation_type" }
);

print("user_mailbox 集合索引创建完成");

// ========================================
// 2. mailbox_message 集合索引
// ========================================
print("\n创建 mailbox_message 集合索引...");

// 用户会话序列号索引（核心查询索引）
db.mailbox_message.createIndex(
  { userId: 1, conversationId: 1, sequence: -1 },
  { name: "idx_user_conversation_sequence" }
);

// 用户状态索引（查询未读消息）
db.mailbox_message.createIndex(
  { userId: 1, status: 1, createTime: -1 },
  { name: "idx_user_status_time" }
);

// 消息ID索引（关联查询）
db.mailbox_message.createIndex(
  { messageId: 1 },
  { name: "idx_message_id" }
);

// 过期时间索引（TTL索引，自动删除过期消息）
db.mailbox_message.createIndex(
  { expireTime: 1 },
  { 
    expireAfterSeconds: 0,
    name: "idx_expire_time_ttl"
  }
);

// 信箱ID索引
db.mailbox_message.createIndex(
  { mailboxId: 1, sequence: -1 },
  { name: "idx_mailbox_sequence" }
);

print("mailbox_message 集合索引创建完成");

// ========================================
// 3. 查看创建的索引
// ========================================
print("\n========================================");
print("索引创建完成，查看索引信息：");
print("========================================\n");

print("user_mailbox 集合索引：");
printjson(db.user_mailbox.getIndexes());

print("\nmailbox_message 集合索引：");
printjson(db.mailbox_message.getIndexes());

print("\n========================================");
print("Mailbox 模块索引创建完成！");
print("========================================");
