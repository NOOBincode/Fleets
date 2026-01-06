// ========================================
// MongoDB 索引创建脚本
// 执行方式：mongo fleets create_indexes.js
// ========================================

// 切换到 fleets 数据库
use fleets;

print("开始创建索引...");

// ========================================
// 1. message 集合索引
// ========================================
print("\n创建 message 集合索引...");

// 单聊消息查询索引（用户A和用户B的聊天记录）
db.message.createIndex(
  { senderId: 1, receiverId: 1, sendTime: -1 },
  { name: "idx_sender_receiver_time" }
);

// 反向查询索引
db.message.createIndex(
  { receiverId: 1, senderId: 1, sendTime: -1 },
  { name: "idx_receiver_sender_time" }
);

// 群聊消息查询索引
db.message.createIndex(
  { groupId: 1, sendTime: -1 },
  { name: "idx_group_time" }
);

// 按时间查询索引
db.message.createIndex(
  { sendTime: -1 },
  { name: "idx_send_time" }
);

// 按状态查询索引
db.message.createIndex(
  { status: 1 },
  { name: "idx_status" }
);

// 复合索引：群聊 + 状态 + 时间
db.message.createIndex(
  { groupId: 1, status: 1, sendTime: -1 },
  { name: "idx_group_status_time" }
);

print("message 集合索引创建完成");

// ========================================
// 2. offline_messages 集合索引
// ========================================
print("\n创建 offline_messages 集合索引...");

// 用户离线消息查询索引
db.offline_messages.createIndex(
  { userId: 1, createTime: -1 },
  { name: "idx_user_time" }
);

// TTL 索引（自动删除过期数据）
db.offline_messages.createIndex(
  { expireTime: 1 },
  { 
    name: "idx_expire_time",
    expireAfterSeconds: 0  // 到达 expireTime 时自动删除
  }
);

print("offline_messages 集合索引创建完成");

// ========================================
// 3. mailboxes 集合索引
// ========================================
print("\n创建 mailboxes 集合索引...");

// 用户会话序列号索引（唯一）
db.mailboxes.createIndex(
  { userId: 1, conversationId: 1 },
  { 
    name: "idx_user_conversation",
    unique: true 
  }
);

// 会话序列号查询索引
db.mailboxes.createIndex(
  { conversationId: 1, sequence: -1 },
  { name: "idx_conversation_sequence" }
);

print("mailboxes 集合索引创建完成");

// ========================================
// 4. group_messages 集合索引（如果使用）
// ========================================
print("\n创建 group_messages 集合索引...");

// 群消息已读记录索引
db.group_messages.createIndex(
  { messageId: 1 },
  { name: "idx_message_id" }
);

db.group_messages.createIndex(
  { groupId: 1, messageId: 1 },
  { name: "idx_group_message" }
);

print("group_messages 集合索引创建完成");

// ========================================
// 查看所有索引
// ========================================
print("\n========================================");
print("message 集合索引：");
printjson(db.message.getIndexes());

print("\noffline_messages 集合索引：");
printjson(db.offline_messages.getIndexes());

print("\nmailboxes 集合索引：");
printjson(db.mailboxes.getIndexes());

print("\n========================================");
print("索引创建完成！");
print("========================================");
