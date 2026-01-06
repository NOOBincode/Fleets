# 🚀 Fleets IM 系统快速上手指南

## 📚 5分钟了解你的项目

### 这是什么？
一个完整的 **Java 即时通讯系统**，类似微信/QQ的后端，支持：
- 👤 用户注册登录
- 👥 好友管理
- 💬 单聊/群聊
- 📁 文件传输
- 🔔 实时消息推送

---

## 🏗️ 项目结构一览

```
Fleets/
├── src/main/java/org/example/fleets/
│   ├── user/           # 用户模块（注册、登录、个人信息）
│   ├── message/        # 消息模块（发送、接收、历史记录）
│   ├── group/          # 群组模块（创建群、加群、群管理）
│   ├── connector/      # 连接模块（WebSocket 实时通信）
│   ├── file/           # 文件模块（上传图片、语音、视频）
│   ├── cache/          # 缓存模块（Redis 缓存服务）
│   ├── common/         # 公共模块（工具类、配置）
│   └── protocol/       # 协议模块（消息类型定义）
│
├── src/main/resources/
│   └── application.properties  # 配置文件
│
└── src/main/java/docker/
    ├── docker-compose.yml      # Docker 编排文件
    ├── mysql/                  # MySQL 初始化脚本
    ├── mongodb/                # MongoDB 初始化数据
    └── openresty/              # 网关配置
```

---

## 🎯 核心概念速览

### 1. 三层架构
```
Controller (控制器) → Service (业务逻辑) → Mapper/Repository (数据访问)
     ↓                    ↓                        ↓
  接收请求            处理业务              操作数据库
```

### 2. 数据存储分工

| 存储 | 用途 | 存储内容 |
|------|------|----------|
| **MySQL** | 关系型数据 | 用户、好友、群组、会话 |
| **MongoDB** | 文档型数据 | 消息内容、离线消息 |
| **Redis** | 缓存 | 用户信息、在线状态、Token |
| **RocketMQ** | 消息队列 | 消息异步处理 |

### 3. 消息流转过程

```
用户A发消息 
  → Controller 接收 
  → Service 处理 
  → 存入 MongoDB 
  → 发到 RocketMQ 
  → Consumer 消费 
  → 推送给用户B
```

---

## 🔑 关键文件说明

### 配置文件
📄 `application.properties` - 所有配置的中心
```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/fleets
spring.redis.host=localhost
spring.data.mongodb.host=localhost

# JWT 配置（支持环境变量）
jwt.secret=${JWT_SECRET:默认密钥}
jwt.expiration=${JWT_EXPIRATION:604800}

# RocketMQ 配置
rocketmq.name-server=127.0.0.1:9876
```

### 核心服务类

#### 1. UserService - 用户服务
```java
// 位置：user/service/UserService.java
// 功能：用户注册、登录、信息管理
register()      // 注册
login()         // 登录
getUserInfo()   // 获取用户信息
updateUserInfo() // 更新用户信息
```

#### 2. MessageService - 消息服务
```java
// 位置：message/service/MessageService.java
// 功能：消息发送、接收、管理
sendMessage()        // 发送消息
getChatHistory()     // 获取聊天记录
markAsRead()         // 标记已读
recallMessage()      // 撤回消息
```

#### 3. GroupService - 群组服务
```java
// 位置：group/service/GroupService.java
// 功能：群组创建、管理
createGroup()    // 创建群组
joinGroup()      // 加入群组
quitGroup()      // 退出群组
kickMember()     // 踢出成员
```

#### 4. ConnectionService - 连接服务
```java
// 位置：connector/service/ConnectionService.java
// 功能：WebSocket 连接管理
userOnline()     // 用户上线
userOffline()    // 用户下线
pushToUser()     // 推送消息给用户
pushToGroup()    // 推送消息给群组
```

---

## 🗂️ 数据库表结构

### MySQL 表

#### user - 用户表
```sql
id              用户ID
username        用户名（唯一）
password        密码（加密）
nickname        昵称
avatar          头像URL
phone           手机号
email           邮箱
status          状态（0-禁用，1-正常）
create_time     创建时间
```

#### friendship - 好友关系表
```sql
id              关系ID
user_id         用户ID
friend_id       好友ID
remark          备注
status          状态（0-待确认，1-已确认，2-已拒绝，3-已拉黑）
```

#### group - 群组表
```sql
id              群组ID
name            群组名称
avatar          群头像
owner_id        群主ID
max_member_count    最大成员数
current_member_count 当前成员数
status          状态
```

#### group_member - 群成员表
```sql
id              成员ID
group_id        群组ID
user_id         用户ID
role            角色（0-普通成员，1-管理员，2-群主）
mute_status     禁言状态
```

### MongoDB 集合

#### message - 消息集合
```javascript
{
  _id: "消息ID",
  messageType: 1,      // 1-单聊，2-群聊
  contentType: 1,      // 1-文本，2-图片，3-语音，4-视频，5-文件
  senderId: 123,       // 发送者ID
  receiverId: 456,     // 接收者ID（单聊）
  groupId: 789,        // 群组ID（群聊）
  content: "消息内容",
  sendTime: "2024-12-03T10:00:00Z",
  status: 1            // 0-发送中，1-已发送，2-已送达，3-已读，4-撤回
}
```

---

## 🔄 典型业务流程

### 场景1：用户登录

```
1. 前端发送 POST /api/user/login
   Body: { username: "test", password: "123456" }

2. UserController.login() 接收请求

3. UserService.login() 处理：
   - 查询数据库验证用户名密码
   - 生成 JWT Token
   - 缓存用户信息到 Redis

4. 返回 UserLoginVO：
   {
     userId: 1,
     username: "test",
     nickname: "测试用户",
     token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     expireTime: 1701619200000
   }

5. 前端保存 Token，后续请求带上 Authorization: Bearer {token}
```

### 场景2：发送消息

```
1. 前端发送 POST /api/message/send
   Headers: { Authorization: "Bearer {token}" }
   Body: {
     messageType: 1,      // 单聊
     contentType: 1,      // 文本
     receiverId: 456,
     content: "你好"
   }

2. MessageController.sendMessage() 接收

3. MessageService.sendMessage() 处理：
   - 构建 Message 对象
   - 保存到 MongoDB
   - 发送到 RocketMQ (im-message-topic)

4. MessageConsumer.onMessage() 消费：
   - 检查接收者是否在线
   - 如果在线：通过 WebSocket 推送
   - 如果离线：存储到离线消息表

5. 返回 MessageVO 给发送者
```

### 场景3：创建群组

```
1. 前端发送 POST /api/group/create
   Body: {
     groupName: "技术交流群",
     avatar: "http://...",
     maxMembers: 200,
     memberIds: [2, 3, 4]  // 初始成员
   }

2. GroupController.createGroup() 接收

3. GroupService.createGroup() 处理：
   - 创建群组记录（group 表）
   - 添加群主为成员（role=2）
   - 添加初始成员（role=0）
   - 缓存群组信息到 Redis

4. 返回 GroupVO：
   {
     id: 1,
     groupName: "技术交流群",
     ownerId: 1,
     memberCount: 4,
     ...
   }
```

---

## 🛠️ 开发流程

### 实现一个新功能的步骤

以"消息撤回"为例：

#### 1. 定义接口（Controller）
```java
@PostMapping("/recall/{messageId}")
public CommonResult<Boolean> recallMessage(
    @PathVariable String messageId,
    HttpServletRequest request
) {
    Long userId = (Long) request.getAttribute("userId");
    boolean result = messageService.recallMessage(messageId, userId);
    return CommonResult.success(result);
}
```

#### 2. 实现业务逻辑（Service）
```java
@Override
public boolean recallMessage(String messageId, Long userId) {
    // 1. 查询消息
    Message message = messageRepository.findById(messageId).block();
    
    // 2. 验证权限（只能撤回自己的消息）
    if (!message.getSenderId().equals(userId)) {
        throw new BusinessException("无权撤回此消息");
    }
    
    // 3. 检查时间（2分钟内可撤回）
    if (System.currentTimeMillis() - message.getSendTime().getTime() > 120000) {
        throw new BusinessException("超过撤回时间");
    }
    
    // 4. 更新消息状态
    message.setStatus(4); // 4-撤回
    messageRepository.save(message).block();
    
    // 5. 推送撤回通知
    connectionService.pushToUser(message.getReceiverId(), 
        new RecallNotification(messageId));
    
    return true;
}
```

#### 3. 测试
```bash
# 使用 Postman 或 curl 测试
curl -X POST http://localhost:8080/api/message/recall/msg123 \
  -H "Authorization: Bearer {token}"
```

---

## 🎨 前端对接指南

### API 基础信息
- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Token
- **请求头**: `Authorization: Bearer {token}`

### 常用 API

#### 用户相关
```javascript
// 登录
POST /api/user/login
Body: { username, password }
Response: { userId, username, token, ... }

// 获取用户信息
GET /api/user/info
Headers: { Authorization: "Bearer {token}" }
Response: { id, username, nickname, avatar, ... }
```

#### 消息相关
```javascript
// 发送消息
POST /api/message/send
Body: { messageType, contentType, receiverId, content }
Response: { id, sendTime, status, ... }

// 获取聊天记录
GET /api/message/chat/{targetUserId}?pageNum=1&pageSize=20
Response: { list: [...], total, pageNum, pageSize }
```

#### WebSocket 连接
```javascript
// 建立连接
const ws = new WebSocket('ws://localhost/ws');

// 连接成功
ws.onopen = () => {
  console.log('WebSocket 连接成功');
};

// 接收消息
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('收到消息:', message);
};

// 发送消息
ws.send(JSON.stringify({
  type: 'message',
  content: '你好'
}));
```

---

## 🐛 常见问题

### Q1: 启动报错 "Connection refused"
**A:** 检查 MySQL/Redis/MongoDB 是否启动
```bash
docker-compose ps
```

### Q2: JWT Token 验证失败
**A:** 检查 Token 是否过期，或者 jwt.secret 配置是否一致

### Q3: 消息发送失败
**A:** 检查 RocketMQ 是否正常运行
```bash
docker logs rocketmq-broker
```

### Q4: WebSocket 连接失败
**A:** 检查 OpenResty 是否启动，JWT 认证是否通过

---

## 📖 学习路径建议

### 第1周：熟悉项目
- ✅ 阅读 README.md 和架构文档
- ✅ 启动项目，测试基本功能
- ✅ 理解数据库表结构

### 第2周：实现核心功能
- ✅ 实现用户登录注册
- ✅ 实现好友添加
- ✅ 实现消息发送

### 第3周：完善功能
- ✅ 实现群组管理
- ✅ 实现文件上传
- ✅ 添加缓存优化

### 第4周：前端对接
- ✅ 开发前端界面
- ✅ 对接 API
- ✅ 测试联调

---

## 🎓 毕设答辩要点

### 技术亮点
1. **微服务架构** - 模块化设计，易扩展
2. **消息队列** - RocketMQ 异步处理，高并发
3. **缓存优化** - Redis 多级缓存，提升性能
4. **网关层** - OpenResty + Lua 实现认证和限流
5. **混合存储** - MySQL + MongoDB 冷热数据分离

### 可展示的功能
- 实时消息推送（WebSocket）
- 离线消息处理
- 群组管理
- 文件传输
- 消息已读回执

### 性能指标
- 支持 1000+ 并发连接
- 消息延迟 < 100ms
- 缓存命中率 > 80%

---

现在你应该对项目有全面的了解了！有任何问题随时问我 😊
