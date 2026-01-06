# 基础设施配置指南

## ✅ 已修复的问题

### 1. Redis 配置
- ✅ 添加了 `RedisConfig` 配置类
- ✅ 创建了 `RedisService` 服务类
- ✅ 配置支持环境变量
- ✅ 配置了连接池

**使用示例：**
```java
@Autowired
private RedisService redisService;

// 缓存用户信息
redisService.set("user:" + userId, userInfo, 1, TimeUnit.HOURS);

// 获取缓存
Object userInfo = redisService.get("user:" + userId);
```

### 2. MongoDB 配置
- ✅ 添加了 `MongoConfig` 配置类
- ✅ 创建了 `MessageRepository` 接口
- ✅ 配置支持环境变量
- ✅ 启用了响应式 MongoDB

**使用示例：**
```java
@Autowired
private MessageRepository messageRepository;

// 保存消息
messageRepository.save(message).subscribe();

// 查询消息
messageRepository.findByGroupIdOrderBySendTimeDesc(groupId)
    .collectList()
    .subscribe(messages -> {
        // 处理消息列表
    });
```

### 3. RocketMQ 配置
- ✅ 添加了 RocketMQ Spring Boot Starter 依赖
- ✅ 创建了 `MessageProducer` 生产者
- ✅ 创建了 `MessageConsumer` 消费者
- ✅ 配置了 NameServer 地址

**使用示例：**
```java
@Autowired
private MessageProducer messageProducer;

// 发送消息
messageProducer.sendMessage("im-message-topic", messageDTO);

// 发送同步消息
messageProducer.sendSyncMessage("im-message-topic", messageDTO);
```

### 4. OpenResty 网关配置
- ✅ 创建了完整的 `nginx.conf`
- ✅ 实现了 JWT 认证（auth.lua）
- ✅ 实现了限流功能（limit.lua）
- ✅ 实现了 WebSocket 处理（websocket_handler.lua）
- ✅ 修复了 Docker Compose 路径映射

## 🔧 配置验证

### 启动顺序
```bash
# 1. 启动基础设施
cd src/main/java/docker
docker-compose up -d mysql redis mongodb rocketmq-namesrv rocketmq-broker

# 2. 等待服务就绪（约30秒）
docker-compose ps

# 3. 启动应用
cd ../../../..
mvn spring-boot:run

# 4. 启动网关（可选）
cd src/main/java/docker
docker-compose up -d im-openresty
```

### 验证连接

#### 1. MySQL 连接测试
```bash
docker exec -it mysql mysql -uroot -proot -e "SHOW DATABASES;"
```

#### 2. Redis 连接测试
```bash
docker exec -it redis redis-cli ping
# 应该返回: PONG
```

#### 3. MongoDB 连接测试
```bash
docker exec -it mongodb mongo -u root -p root123 --authenticationDatabase admin --eval "db.adminCommand('ping')"
# 应该返回: { "ok" : 1 }
```

#### 4. RocketMQ 连接测试
访问 RocketMQ Dashboard: http://localhost:8080

#### 5. OpenResty 测试
```bash
curl http://localhost/health
# 应该返回: OK
```

## 📝 配置说明

### 环境变量配置（推荐）

创建 `.env` 文件：
```bash
# JWT
JWT_SECRET=your-production-secret-key-min-32-chars
JWT_EXPIRATION=604800

# MySQL
DB_HOST=localhost
DB_PORT=3306
DB_NAME=fleets
DB_USERNAME=root
DB_PASSWORD=root

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# MongoDB
MONGO_HOST=localhost
MONGO_PORT=27017
MONGO_USERNAME=root
MONGO_PASSWORD=root123
MONGO_DATABASE=fleets

# RocketMQ
ROCKETMQ_NAME_SERVER=localhost:9876
```

### Docker 环境配置

如果使用 Docker Compose 启动应用，修改 `application.properties`：
```properties
# MySQL
spring.datasource.url=jdbc:mysql://mysql:3306/fleets?...

# Redis
spring.redis.host=redis

# MongoDB
spring.data.mongodb.host=mongodb

# RocketMQ
rocketmq.name-server=rocketmq-namesrv:9876
```

## 🚨 常见问题

### 1. RocketMQ 连接失败
**问题：** `connect to <172.x.x.x:10909> failed`

**解决：**
- 检查 `broker.conf` 中的 `brokerIP1` 配置
- 确保 NameServer 和 Broker 都已启动
- 查看日志：`docker logs rocketmq-broker`

### 2. MongoDB 认证失败
**问题：** `Authentication failed`

**解决：**
```properties
spring.data.mongodb.authentication-database=admin
```

### 3. Redis 连接超时
**问题：** `Connection timeout`

**解决：**
- 检查 Redis 是否启动：`docker ps | grep redis`
- 检查端口映射：`docker port redis`
- 增加超时时间：`spring.redis.timeout=10000ms`

### 4. OpenResty Lua 脚本错误
**问题：** `lua entry thread aborted`

**解决：**
- 检查 Lua 脚本语法
- 查看错误日志：`docker logs im-openresty`
- 确保 Redis 连接正常（auth.lua 依赖 Redis）

## 📊 性能优化建议

### Redis 优化
```properties
# 连接池配置
spring.redis.lettuce.pool.max-active=20
spring.redis.lettuce.pool.max-idle=10
spring.redis.lettuce.pool.min-idle=5
```

### MongoDB 优化
- 为常用查询字段创建索引
- 使用响应式编程避免阻塞

### RocketMQ 优化
```properties
# 批量发送
rocketmq.producer.compress-message-body-threshold=4096
# 异步发送
rocketmq.producer.send-message-timeout=3000
```

## 🔐 安全建议

1. **生产环境必须修改默认密码**
2. **JWT Secret 使用强随机字符串（至少32位）**
3. **Redis 设置密码**
4. **MongoDB 启用认证**
5. **OpenResty 配置 HTTPS**

## 📈 监控建议

1. **RocketMQ Dashboard** - 监控消息队列
2. **Redis Commander** - 监控 Redis 缓存
3. **MongoDB Compass** - 监控 MongoDB 数据
4. **Spring Boot Actuator** - 监控应用健康状态
