# WebSocket 模块

## 概述

基于 Spring WebSocket + STOMP 协议的实时通信模块。

## 模块结构

```
websocket/
├── config/
│   └── WebSocketConfig.java           # WebSocket 配置
├── controller/
│   └── WebSocketController.java       # 消息处理控制器
├── handler/
│   ├── WebSocketHandshakeInterceptor.java  # 握手拦截器
│   └── WebSocketEventListener.java    # 事件监听器
└── service/
    ├── UserOnlineService.java         # 在线状态服务
    └── WebSocketService.java          # 消息推送服务
```

## 实现任务

### 1. WebSocketConfig（配置）
- [ ] 配置消息代理路径（/topic, /queue）
- [ ] 注册 STOMP 端点（/ws）
- [ ] 配置跨域和 SockJS 支持
- [ ] 添加认证拦截器（可选）

### 2. WebSocketHandshakeInterceptor（握手拦截器）
- [ ] 从请求中获取 token
- [ ] 验证 token（使用 SaToken）
- [ ] 将 userId 存入会话属性
- [ ] 返回握手结果

### 3. WebSocketEventListener（事件监听器）
- [ ] 处理连接建立事件（用户上线）
- [ ] 处理连接断开事件（用户离线）
- [ ] 处理订阅事件（可选）

### 4. UserOnlineService（在线状态服务）
- [ ] 实现用户上线逻辑（Redis 存储）
- [ ] 实现用户离线逻辑
- [ ] 支持多端登录（一个用户多个会话）
- [ ] 实现在线状态查询
- [ ] 实现心跳刷新

### 5. WebSocketService（消息推送服务）
- [ ] 实现点对点消息推送
- [ ] 实现多端消息推送
- [ ] 实现群组消息广播
- [ ] 实现系统通知推送
- [ ] 实现在线状态广播

### 6. WebSocketController（消息处理控制器）
- [ ] 实现心跳处理
- [ ] 实现客户端消息处理（可选）
- [ ] 实现输入状态广播（可选）

## 客户端订阅路径

### 点对点消息
- 订阅：`/user/queue/messages`
- 接收：个人消息

### 群组消息
- 订阅：`/topic/group/{groupId}`
- 接收：群组消息

### 系统通知
- 订阅：`/user/queue/notifications`
- 接收：系统通知

### 在线状态
- 订阅：`/topic/online-status`
- 接收：好友在线状态变更

## 客户端发送路径

### 心跳
- 发送：`/app/heartbeat`
- 用途：保持连接活跃

### 发送消息（可选）
- 发送：`/app/send`
- 用途：通过 WebSocket 发送消息（建议使用 HTTP API）

### 输入状态
- 发送：`/app/typing`
- 用途：通知对方正在输入

## Redis 数据结构

### 在线状态
```
Key: user:online:{userId}
Type: String
Value: "1"
TTL: 300秒
```

### 会话信息
```
Key: user:session:{sessionId}
Type: String
Value: userId
TTL: 300秒
```

### 用户会话集合
```
Key: user:sessions:{userId}
Type: Set
Members: [sessionId1, sessionId2, ...]
TTL: 300秒
```

## 集成到 MessageConsumer

在 `MessageConsumer` 中使用 `WebSocketService` 推送消息：

```java
@RocketMQMessageListener(topic = "im-message-topic")
public class MessageConsumer implements RocketMQListener<Message> {
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Override
    public void onMessage(Message message) {
        // 推送给在线用户
        if (message.getMessageType() == 1) {
            // 单聊
            webSocketService.sendMessageToUser(message.getReceiverId(), message);
        } else {
            // 群聊
            webSocketService.sendMessageToGroup(message.getGroupId(), message);
        }
    }
}
```

## 客户端示例（JavaScript）

```javascript
// 连接 WebSocket
const socket = new SockJS('/ws?token=YOUR_TOKEN');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // 订阅个人消息
    stompClient.subscribe('/user/queue/messages', function(message) {
        const msg = JSON.parse(message.body);
        console.log('收到消息:', msg);
    });
    
    // 订阅群组消息
    stompClient.subscribe('/topic/group/123', function(message) {
        const msg = JSON.parse(message.body);
        console.log('收到群组消息:', msg);
    });
    
    // 发送心跳
    setInterval(() => {
        stompClient.send('/app/heartbeat', {}, '');
    }, 30000);
});
```

## 注意事项

1. **认证**：在握手拦截器中验证 token
2. **多端登录**：支持一个用户多个会话
3. **心跳**：定期发送心跳保持连接
4. **异常处理**：处理连接断开和重连
5. **性能**：使用 Redis 存储在线状态
6. **集群部署**：考虑使用 Redis Pub/Sub 或 RabbitMQ 作为消息代理

## 下一步

1. 实现所有 TODO 标记的方法
2. 测试 WebSocket 连接和消息推送
3. 集成到 MessageConsumer
4. 编写客户端代码
5. 进行压力测试
