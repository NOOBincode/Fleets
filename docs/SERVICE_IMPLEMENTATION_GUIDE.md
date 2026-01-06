# Service 实现指南

## ✅ 已创建的骨架代码

### 1. Service 实现类 (impl)

#### 用户模块
- ✅ `UserServiceImpl` - 用户服务实现
- ✅ `FriendshipServiceImpl` - 好友关系服务实现

#### 群组模块
- ✅ `GroupServiceImpl` - 群组服务实现

#### 消息模块
- ✅ `MessageServiceImpl` - 消息服务实现

#### 文件模块
- ✅ `FileServiceImpl` - 文件服务实现

#### 连接模块
- ✅ `ConnectionServiceImpl` - 连接管理服务实现

### 2. Cache 缓存服务类

#### 用户模块
- ✅ `UserCacheService` - 用户缓存服务
  - 缓存用户信息
  - 缓存用户Token
  - 管理用户会话

- ✅ `FriendshipCacheService` - 好友关系缓存服务
  - 缓存好友列表
  - 缓存好友关系

#### 群组模块
- ✅ `GroupCacheService` - 群组缓存服务
  - 缓存群组信息
  - 缓存群成员列表
  - 缓存用户的群组列表

#### 消息模块
- ✅ `MessageCacheService` - 消息缓存服务
  - 缓存消息
  - 缓存未读消息数
  - 缓存会话最新消息

#### 文件模块
- ✅ `FileCacheService` - 文件缓存服务
  - 缓存文件元数据
  - 缓存文件URL

#### 连接模块
- ✅ `ConnectionCacheService` - 连接缓存服务
  - 管理用户在线状态
  - 管理用户会话

### 3. Mapper 接口

- ✅ `UserMapper` - 用户数据访问
- ✅ `FriendshipMapper` - 好友关系数据访问
- ✅ `GroupMapper` - 群组数据访问
- ✅ `GroupMemberMapper` - 群成员数据访问
- ✅ `FileMetadataMapper` - 文件元数据访问

### 4. DTO/VO 类

#### 用户相关
- ✅ `UserRegisterDTO` - 用户注册
- ✅ `UserLoginDTO` - 用户登录
- ✅ `UserUpdateDTO` - 用户更新
- ✅ `UserQueryDTO` - 用户查询
- ✅ `PasswordUpdateDTO` - 密码修改
- ✅ `UserVO` - 用户视图对象
- ✅ `UserLoginVO` - 登录返回对象

#### 好友相关
- ✅ `FriendAddDTO` - 添加好友
- ✅ `FriendVO` - 好友视图对象

#### 群组相关
- ✅ `GroupCreateDTO` - 创建群组
- ✅ `GroupVO` - 群组视图对象

#### 消息相关
- ✅ `MessageSendDTO` - 发送消息
- ✅ `MessageVO` - 消息视图对象

## 📋 实现建议

### 1. UserServiceImpl 实现要点

```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserCacheService userCacheService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private PasswordUtils passwordUtils;
    
    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
        // 1. 查询用户
        // 2. 验证密码
        // 3. 生成Token
        // 4. 缓存用户信息和Token
        // 5. 返回登录信息
    }
}
```

### 2. MessageServiceImpl 实现要点

```java
@Service
public class MessageServiceImpl implements MessageService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private MessageCacheService messageCacheService;
    
    @Autowired
    private MessageProducer messageProducer;
    
    @Autowired
    private ConnectionService connectionService;
    
    @Override
    public MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO) {
        // 1. 构建消息对象
        // 2. 保存到MongoDB
        // 3. 发送到RocketMQ
        // 4. 推送给在线用户
        // 5. 缓存消息
        // 6. 返回消息VO
    }
}
```

### 3. GroupServiceImpl 实现要点

```java
@Service
public class GroupServiceImpl implements GroupService {
    
    @Autowired
    private GroupMapper groupMapper;
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    
    @Autowired
    private GroupCacheService groupCacheService;
    
    @Override
    public GroupVO createGroup(Long userId, GroupCreateDTO createDTO) {
        // 1. 创建群组
        // 2. 添加群主为成员
        // 3. 添加初始成员
        // 4. 缓存群组信息
        // 5. 返回群组VO
    }
}
```

### 4. ConnectionServiceImpl 实现要点

```java
@Service
public class ConnectionServiceImpl implements ConnectionService {
    
    @Autowired
    private ConnectionCacheService connectionCacheService;
    
    // 使用ConcurrentHashMap管理WebSocket会话
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    
    @Override
    public void userOnline(Long userId, String sessionId, String deviceId) {
        // 1. 保存会话信息
        // 2. 设置在线状态
        // 3. 推送离线消息
    }
}
```

## 🔧 缓存使用示例

### 用户信息缓存

```java
// 获取用户信息（先查缓存，再查数据库）
public UserVO getUserInfo(Long userId) {
    // 1. 查询缓存
    User cachedUser = userCacheService.getCachedUser(userId);
    if (cachedUser != null) {
        return convertToVO(cachedUser);
    }
    
    // 2. 查询数据库
    User user = userMapper.selectById(userId);
    if (user != null) {
        // 3. 写入缓存
        userCacheService.cacheUser(user);
        return convertToVO(user);
    }
    
    return null;
}
```

### 好友列表缓存

```java
// 获取好友列表（先查缓存，再查数据库）
public List<FriendVO> getFriendList(Long userId) {
    // 1. 查询缓存
    List<Long> cachedFriendIds = friendshipCacheService.getCachedFriendList(userId);
    if (cachedFriendIds != null) {
        return loadFriendDetails(cachedFriendIds);
    }
    
    // 2. 查询数据库
    List<Friendship> friendships = friendshipMapper.selectList(
        new QueryWrapper<Friendship>().eq("user_id", userId)
    );
    
    // 3. 写入缓存
    List<Long> friendIds = friendships.stream()
        .map(Friendship::getFriendId)
        .collect(Collectors.toList());
    friendshipCacheService.cacheFriendList(userId, friendIds);
    
    return convertToVOList(friendships);
}
```

### 消息未读数缓存

```java
// 增加未读消息数
public void incrementUnreadCount(Long userId) {
    messageCacheService.incrementUnreadCount(userId);
}

// 清空未读消息数
public void clearUnreadCount(Long userId) {
    messageCacheService.clearUnreadCount(userId);
}
```

## 📊 数据库操作示例

### MyBatis-Plus 基本操作

```java
// 插入
User user = new User();
user.setUsername("test");
userMapper.insert(user);

// 更新
user.setNickname("新昵称");
userMapper.updateById(user);

// 查询
User user = userMapper.selectById(userId);

// 条件查询
List<User> users = userMapper.selectList(
    new QueryWrapper<User>()
        .eq("status", 0)
        .like("username", "test")
);

// 分页查询
Page<User> page = new Page<>(pageNum, pageSize);
userMapper.selectPage(page, new QueryWrapper<User>());
```

### MongoDB 响应式操作

```java
// 保存消息
messageRepository.save(message).subscribe();

// 查询消息
messageRepository.findById(messageId)
    .subscribe(message -> {
        // 处理消息
    });

// 查询列表
messageRepository.findByGroupIdOrderBySendTimeDesc(groupId)
    .collectList()
    .subscribe(messages -> {
        // 处理消息列表
    });
```

## 🚀 消息队列使用示例

### 发送消息到RocketMQ

```java
@Autowired
private MessageProducer messageProducer;

// 发送消息
messageProducer.sendMessage("im-message-topic", messageDTO);

// 发送同步消息
messageProducer.sendSyncMessage("im-message-topic", messageDTO);

// 发送带标签的消息
messageProducer.sendMessageWithTag("im-message-topic", "CHAT", messageDTO);
```

### 消费消息

```java
@Component
@RocketMQMessageListener(
    topic = "im-message-topic",
    consumerGroup = "im-message-consumer-group"
)
public class MessageConsumer implements RocketMQListener<String> {
    
    @Override
    public void onMessage(String message) {
        // 处理消息
        log.info("收到消息: {}", message);
    }
}
```

## 🔐 安全建议

1. **密码加密** - 使用 BCrypt 加密密码
2. **Token验证** - 每次请求验证JWT Token
3. **权限检查** - 操作前检查用户权限
4. **参数校验** - 使用 @Valid 注解校验参数
5. **SQL注入防护** - 使用参数化查询

## 📝 注意事项

1. 所有 TODO 标记的地方需要实现具体业务逻辑
2. 缓存过期时间根据实际业务调整
3. 异常处理需要完善
4. 日志记录需要添加
5. 事务管理需要考虑
6. 分布式锁在必要时使用

## 🎯 下一步工作

1. 实现各个 Service 的具体业务逻辑
2. 添加参数校验和异常处理
3. 编写单元测试
4. 性能优化和压力测试
5. 前端对接和联调
