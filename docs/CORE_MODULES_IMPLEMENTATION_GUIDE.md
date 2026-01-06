## 核心模块实现指南

## 📚 已创建的骨架代码

### 1. 异常处理体系（已完整实现 ✅）

#### 核心类
- ✅ `GlobalExceptionHandler` - 全局异常处理器
- ✅ `ErrorCode` - 错误码枚举
- ✅ `BusinessException` - 业务异常类
- ✅ `Assert` - 断言工具类

#### 使用示例

```java
// 方式1：直接抛出异常
throw new BusinessException(ErrorCode.USER_NOT_FOUND);

// 方式2：使用断言工具
Assert.notNull(user, ErrorCode.USER_NOT_FOUND);
Assert.isTrue(user.getStatus() == 1, "用户已被禁用");

// 方式3：在 Service 中使用
@Service
public class UserServiceImpl implements UserService {
    
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        
        // 如果用户不存在，自动抛出异常并被全局处理器捕获
        Assert.notNull(user, ErrorCode.USER_NOT_FOUND);
        
        return convertToVO(user);
    }
}
```

---

### 2. 消息ACK机制（骨架 ⚙️）

#### 核心类
- ⚙️ `MessageAckService` - 消息确认服务接口
- ⚙️ `MessageAckServiceImpl` - 消息确认服务实现（待完善）
- ⚙️ `MessageAckController` - 消息确认控制器
- ✅ `MessageStatus` - 消息状态枚举
- ✅ `MessageAckDTO` - 消息确认DTO

#### 实现要点

**步骤1：实现送达确认**
```java
@Override
public void handleDeliveredAck(Long userId, String messageId) {
    // 1. 查询消息
    Message message = messageRepository.findById(messageId).block();
    Assert.notNull(message, ErrorCode.MESSAGE_NOT_FOUND);
    
    // 2. 验证接收者
    Assert.isTrue(message.getReceiverId().equals(userId), "不是消息接收者");
    
    // 3. 更新消息状态为"已送达"
    message.setStatus(MessageStatus.DELIVERED.getCode());
    messageRepository.save(message).block();
    
    // 4. 通知发送者（可选）
    connectionService.pushToUser(message.getSenderId(), 
        new MessageAck(messageId, MessageStatus.DELIVERED));
}
```

**步骤2：实现已读确认**
```java
@Override
public void handleReadAck(Long userId, String messageId) {
    // 1. 查询消息
    Message message = messageRepository.findById(messageId).block();
    Assert.notNull(message, ErrorCode.MESSAGE_NOT_FOUND);
    
    // 2. 验证接收者
    Assert.isTrue(message.getReceiverId().equals(userId), "不是消息接收者");
    
    // 3. 更新消息状态为"已读"
    message.setStatus(MessageStatus.READ.getCode());
    messageRepository.save(message).block();
    
    // 4. 清空未读数
    conversationService.clearUnreadCount(
        generateConversationId(userId, message.getSenderId()), userId);
    
    // 5. 通知发送者
    connectionService.pushToUser(message.getSenderId(), 
        new MessageAck(messageId, MessageStatus.READ));
}
```

**步骤3：实现消息重试**
```java
@Override
@Scheduled(fixedDelay = 60000)
public void retryFailedMessages() {
    // 1. 查询状态为"发送中"且超过5分钟的消息
    Date fiveMinutesAgo = new Date(System.currentTimeMillis() - 300000);
    
    List<Message> failedMessages = messageRepository
        .findByStatusAndSendTimeBefore(MessageStatus.SENDING.getCode(), fiveMinutesAgo)
        .collectList()
        .block();
    
    // 2. 重新发送
    for (Message message : failedMessages) {
        try {
            // 重新推送
            messageProducer.sendMessage("im-message-topic", message);
            
            // 更新状态
            message.setStatus(MessageStatus.SENT.getCode());
            messageRepository.save(message).block();
            
        } catch (Exception e) {
            log.error("消息重试失败: messageId={}", message.getId(), e);
            
            // 标记为失败
            message.setStatus(MessageStatus.FAILED.getCode());
            messageRepository.save(message).block();
        }
    }
}
```

#### API 接口

```bash
# 送达确认
POST /api/message/ack/delivered/{messageId}

# 已读确认
POST /api/message/ack/read/{messageId}

# 批量已读确认
POST /api/message/ack/read/batch
Body: ["msg1", "msg2", "msg3"]
```

---

### 3. 在线状态和心跳机制（骨架 ⚙️）

#### 核心类
- ⚙️ `OnlineStatusService` - 在线状态服务接口
- ⚙️ `OnlineStatusServiceImpl` - 在线状态服务实现（待完善）
- ⚙️ `OnlineStatusController` - 在线状态控制器
- ✅ `HeartbeatMessage` - 心跳消息

#### 实现要点

**步骤1：实现用户上线**
```java
@Override
public void userOnline(Long userId, String sessionId, String deviceId) {
    // 1. 设置在线状态到 Redis（TTL=60秒）
    String onlineKey = ONLINE_KEY_PREFIX + userId;
    redisService.set(onlineKey, sessionId, ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
    
    // 2. 保存会话信息（支持多端登录）
    String sessionKey = SESSION_KEY_PREFIX + userId;
    redisService.set(sessionKey + ":" + sessionId, deviceId, 
        ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
    
    // 3. 发布上线事件（通知好友）
    publishOnlineEvent(userId, true);
    
    // 4. 触发离线消息推送
    messageSyncService.syncMessagesOnLogin(userId);
}
```

**步骤2：实现心跳刷新**
```java
@Override
public void heartbeat(Long userId, String sessionId) {
    // 刷新在线状态的过期时间
    String onlineKey = ONLINE_KEY_PREFIX + userId;
    redisService.expire(onlineKey, ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
    
    // 刷新会话的过期时间
    String sessionKey = SESSION_KEY_PREFIX + userId + ":" + sessionId;
    redisService.expire(sessionKey, ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
}
```

**步骤3：WebSocket 心跳处理**
```java
@Component
public class WebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private OnlineStatusService onlineStatusService;
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        
        if ("PING".equals(payload)) {
            // 收到心跳
            Long userId = getUserIdFromSession(session);
            String sessionId = session.getId();
            
            // 刷新在线状态
            onlineStatusService.heartbeat(userId, sessionId);
            
            // 回复 PONG
            session.sendMessage(new TextMessage("PONG"));
        }
    }
}
```

**步骤4：客户端心跳（前端）**
```javascript
// WebSocket 连接
const ws = new WebSocket('ws://localhost/ws');

// 每30秒发送一次心跳
setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send('PING');
    }
}, 30000);

// 接收 PONG
ws.onmessage = (event) => {
    if (event.data === 'PONG') {
        console.log('心跳正常');
    }
};
```

#### API 接口

```bash
# 检查用户是否在线
GET /api/online/check/{userId}

# 批量检查用户是否在线
POST /api/online/check/batch
Body: [123, 456, 789]

# 获取在线用户数量
GET /api/online/count
```

---

### 4. 消息同步（推拉结合）（骨架 ⚙️）

#### 核心类
- ⚙️ `MessageSyncService` - 消息同步服务接口
- ⚙️ `MessageSyncServiceImpl` - 消息同步服务实现（待完善）
- ⚙️ `MessageSyncController` - 消息同步控制器

#### 实现要点

**步骤1：实现离线消息拉取**
```java
@Override
public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence, Integer limit) {
    // 1. 查询 MongoDB，获取 sequence > lastSequence 的消息
    Query query = new Query();
    query.addCriteria(Criteria.where("receiverId").is(userId)
        .and("sequence").gt(lastSequence));
    query.with(Sort.by(Sort.Direction.ASC, "sequence"));
    query.limit(limit);
    
    List<Message> messages = mongoTemplate.find(query, Message.class);
    
    // 2. 转换为 VO
    List<MessageVO> result = messages.stream()
        .map(this::convertToVO)
        .collect(Collectors.toList());
    
    // 3. 更新最后序列号
    if (!messages.isEmpty()) {
        Long maxSequence = messages.get(messages.size() - 1).getSequence();
        updateLastSequence(userId, maxSequence);
    }
    
    return result;
}
```

**步骤2：实现用户上线同步**
```java
@Override
public void syncMessagesOnLogin(Long userId) {
    // 1. 获取用户最后同步的序列号
    Long lastSequence = getLastSequence(userId);
    
    // 2. 查询所有未同步的消息
    List<MessageVO> messages = pullOfflineMessages(userId, lastSequence, 100);
    
    // 3. 推送给用户
    for (MessageVO message : messages) {
        connectionService.pushToUser(userId, message);
    }
    
    log.info("用户上线消息同步完成: userId={}, count={}", userId, messages.size());
}
```

**步骤3：客户端拉取逻辑（前端）**
```javascript
// 用户登录后拉取离线消息
async function syncMessages() {
    // 1. 获取本地存储的最后序列号
    const lastSequence = localStorage.getItem('lastSequence') || 0;
    
    // 2. 拉取离线消息
    const response = await fetch(`/api/message/sync/pull?lastSequence=${lastSequence}&limit=100`);
    const messages = await response.json();
    
    // 3. 显示消息
    messages.forEach(msg => {
        displayMessage(msg);
    });
    
    // 4. 更新本地序列号
    if (messages.length > 0) {
        const maxSequence = Math.max(...messages.map(m => m.sequence));
        localStorage.setItem('lastSequence', maxSequence);
    }
}

// 登录后调用
syncMessages();
```

#### API 接口

```bash
# 拉取离线消息
GET /api/message/sync/pull?lastSequence=1000&limit=100

# 获取同步信息
GET /api/message/sync/info

# 更新同步序列号
POST /api/message/sync/update-sequence?sequence=1100
```

---

## 🔧 实现顺序建议

### 第1周：异常处理 + 消息ACK
1. ✅ 异常处理已完成，直接使用
2. 实现 `handleDeliveredAck`
3. 实现 `handleReadAck`
4. 实现 `retryFailedMessages`
5. 测试 ACK 功能

### 第2周：在线状态 + 心跳
1. 实现 `userOnline`
2. 实现 `userOffline`
3. 实现 `heartbeat`
4. 修改 WebSocketHandler 处理心跳
5. 测试在线状态

### 第3周：消息同步
1. 实现 `pullOfflineMessages`
2. 实现 `syncMessagesOnLogin`
3. 实现 `getLastSequence` 和 `updateLastSequence`
4. 测试消息同步
5. 前端对接

---

## 📝 测试建议

### 测试ACK机制
```bash
# 1. 发送消息
POST /api/message/send
Body: { "receiverId": 456, "content": "测试消息" }

# 2. 送达确认
POST /api/message/ack/delivered/{messageId}

# 3. 已读确认
POST /api/message/ack/read/{messageId}

# 4. 查询消息状态
GET /api/message/{messageId}
# 应该看到 status=3（已读）
```

### 测试心跳机制
```bash
# 1. 建立 WebSocket 连接
ws://localhost/ws

# 2. 发送心跳
PING

# 3. 应该收到
PONG

# 4. 检查在线状态
GET /api/online/check/{userId}
# 应该返回 true
```

### 测试消息同步
```bash
# 1. 用户A发送10条消息给用户B（用户B离线）

# 2. 用户B上线，拉取离线消息
GET /api/message/sync/pull?lastSequence=0&limit=100

# 3. 应该返回10条消息

# 4. 确认消息
POST /api/message/ack/read/batch
Body: ["msg1", "msg2", ...]
```

---

## ✅ 完成标准

### 消息ACK机制
- [ ] 消息状态正确更新（发送中 → 已发送 → 已送达 → 已读）
- [ ] 超时消息自动重试
- [ ] 发送者能收到 ACK 通知
- [ ] 未读消息数正确统计

### 在线状态管理
- [ ] 用户上线后状态为在线
- [ ] 心跳正常刷新在线状态
- [ ] 60秒无心跳自动离线
- [ ] 好友能看到在线状态

### 消息同步
- [ ] 用户上线自动推送离线消息
- [ ] 拉取接口返回正确的消息
- [ ] 序列号正确更新
- [ ] 支持分页拉取

---

## 🎯 总结

所有骨架代码已创建完成，你只需要：

1. **异常处理** - 已完整实现，直接使用 ✅
2. **消息ACK** - 填充 TODO 部分的业务逻辑
3. **在线状态** - 填充 TODO 部分的业务逻辑
4. **消息同步** - 填充 TODO 部分的业务逻辑

每个 TODO 都有详细的注释说明需要做什么，按照注释实现即可！

祝你实现顺利！🎉
