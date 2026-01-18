# Fleets 开发快速参考卡

## 开发顺序

```
1. Mailbox模块（2-3天）⭐⭐⭐
   ├─ saveMessage()         - 保存消息
   ├─ syncMessages()        - 增量同步
   ├─ getUnreadCount()      - 未读数统计
   ├─ markAsRead()          - 标记已读
   └─ getOfflineMessages()  - 离线消息

2. 消息模块（3-4天）⭐⭐⭐
   ├─ sendMessage()         - 发送消息
   ├─ receiveMessage()      - 接收消息
   ├─ getMessageHistory()   - 消息历史
   └─ recallMessage()       - 消息撤回

3. WebSocket模块（2-3天）⭐⭐⭐
   ├─ 连接管理              - 上线/下线
   ├─ 消息推送              - 实时推送
   └─ 在线状态              - 状态同步
```

---

## Reactive编程速查

### Mono（单个结果）
```java
// 查询单个对象
Mono<User> userMono = userRepository.findById(1L);

// 转同步
User user = userMono.block();

// 异步处理
userMono.subscribe(user -> {
    System.out.println(user.getName());
});

// 默认值
User user = userMono.defaultIfEmpty(new User()).block();

// 异常处理
userMono.onErrorReturn(new User()).block();
```

### Flux（多个结果）
```java
// 查询多个对象
Flux<Message> messageFlux = messageRepository.findAll();

// 转List
List<Message> messages = messageFlux.collectList().block();

// 限制数量
List<Message> top10 = messageFlux.take(10).collectList().block();

// 过滤
Flux<Message> unread = messageFlux.filter(m -> m.getStatus() == 0);

// 映射
Flux<Long> ids = messageFlux.map(Message::getId);
```

---

## MongoDB查询速查

### Repository方法命名
```java
// 基础查询
findByUserId(Long userId)
findByUserIdAndStatus(Long userId, Integer status)

// 比较查询
findBySequenceGreaterThan(Long sequence)
findBySequenceBetween(Long start, Long end)

// 排序
findByUserIdOrderBySequenceDesc(Long userId)

// 分页
findByUserId(Long userId, Pageable pageable)

// 统计
countByUserIdAndStatus(Long userId, Integer status)

// 删除
deleteByUserId(Long userId)
```

### 自定义查询
```java
public interface MailboxMessageRepository 
    extends ReactiveMongoRepository<MailboxMessage, String> {
    
    @Query("{'userId': ?0, 'sequence': {$gt: ?1}}")
    Flux<MailboxMessage> findIncrementalMessages(Long userId, Long sequence);
}
```

---

## 常用代码片段

### 1. 保存消息到Mailbox
```java
public Long saveMessage(Long userId, MessageDTO dto) {
    // 生成序列号
    Long sequence = sequenceService.generateSequence(userId);
    
    // 创建消息
    MailboxMessage message = new MailboxMessage();
    message.setUserId(userId);
    message.setSequence(sequence);
    message.setMessageId(dto.getMessageId());
    message.setFromUserId(dto.getFromUserId());
    message.setContent(dto.getContent());
    message.setStatus(0);
    message.setCreateTime(new Date());
    
    // 保存
    mailboxMessageRepository.save(message).block();
    
    // 更新lastSequence
    updateLastSequence(userId, sequence);
    
    return sequence;
}
```

### 2. 增量同步消息
```java
public SyncResult syncMessages(Long userId, Long lastSequence) {
    // 查询增量消息
    List<MailboxMessage> messages = mailboxMessageRepository
        .findByUserIdAndSequenceGreaterThan(userId, lastSequence)
        .take(100)  // 限制100条
        .collectList()
        .block();
    
    // 获取最新sequence
    Long latestSequence = userMailboxRepository
        .findByUserId(userId)
        .map(UserMailbox::getLastSequence)
        .defaultIfEmpty(0L)
        .block();
    
    // 构造结果
    SyncResult result = new SyncResult();
    result.setMessages(messages);
    result.setLatestSequence(latestSequence);
    result.setHasMore(messages.size() >= 100);
    
    return result;
}
```

### 3. 统计未读消息
```java
public UnreadCountVO getUnreadCount(Long userId) {
    // 获取readSequence
    Long readSequence = userMailboxRepository
        .findByUserId(userId)
        .map(UserMailbox::getReadSequence)
        .defaultIfEmpty(0L)
        .block();
    
    // 查询未读消息
    List<MailboxMessage> unreadMessages = mailboxMessageRepository
        .findByUserIdAndSequenceGreaterThan(userId, readSequence)
        .collectList()
        .block();
    
    // 按会话分组
    Map<Long, Long> unreadByConversation = unreadMessages.stream()
        .collect(Collectors.groupingBy(
            MailboxMessage::getFromUserId,
            Collectors.counting()
        ));
    
    // 构造结果
    UnreadCountVO vo = new UnreadCountVO();
    vo.setTotalUnread(unreadMessages.size());
    vo.setUnreadByConversation(unreadByConversation);
    
    return vo;
}
```

### 4. 标记已读
```java
public void markAsRead(Long userId, Long sequence) {
    UserMailbox mailbox = userMailboxRepository
        .findByUserId(userId)
        .block();
    
    if (mailbox != null && sequence > mailbox.getReadSequence()) {
        mailbox.setReadSequence(sequence);
        mailbox.setUpdateTime(new Date());
        userMailboxRepository.save(mailbox).block();
    }
}
```

### 5. 发送消息（完整流程）
```java
@Transactional
public MessageVO sendMessage(SendMessageDTO dto) {
    // 1. 校验好友关系
    if (!friendshipService.isFriend(dto.getFromUserId(), dto.getToUserId())) {
        throw new BusinessException("不是好友，无法发送消息");
    }
    
    // 2. 保存到MySQL
    Message message = new Message();
    message.setFromUserId(dto.getFromUserId());
    message.setToUserId(dto.getToUserId());
    message.setContent(dto.getContent());
    message.setMessageType(dto.getMessageType());
    message.setStatus(0);
    message.setCreateTime(new Date());
    messageMapper.insert(message);
    
    // 3. 保存到发送方Mailbox
    Long senderSeq = mailboxService.saveMessage(
        dto.getFromUserId(), 
        convertToDTO(message)
    );
    
    // 4. 保存到接收方Mailbox
    Long receiverSeq = mailboxService.saveMessage(
        dto.getToUserId(), 
        convertToDTO(message)
    );
    
    // 5. 发送到MQ（触发推送）
    rocketMQTemplate.convertAndSend("fleets-message", message);
    
    // 6. 返回结果
    MessageVO vo = new MessageVO();
    vo.setMessageId(message.getId());
    vo.setSequence(senderSeq);
    vo.setCreateTime(message.getCreateTime());
    
    return vo;
}
```

---

## 异常处理模板

```java
public class MailboxServiceImpl implements MailboxService {
    
    @Override
    public Long saveMessage(Long userId, MessageDTO dto) {
        try {
            // 业务逻辑
            Long sequence = sequenceService.generateSequence(userId);
            // ...
            return sequence;
            
        } catch (BusinessException e) {
            // 业务异常，直接抛出
            log.warn("业务异常: {}", e.getMessage());
            throw e;
            
        } catch (Exception e) {
            // 系统异常，记录日志并包装
            log.error("保存消息失败，userId: {}, messageId: {}", 
                userId, dto.getMessageId(), e);
            throw new BusinessException("消息保存失败，请稍后重试");
        }
    }
}
```

---

## 日志规范

```java
// INFO - 关键业务操作
log.info("用户登录成功，userId: {}, ip: {}", userId, ip);
log.info("消息发送成功，from: {}, to: {}, messageId: {}", from, to, msgId);

// DEBUG - 详细调试信息
log.debug("查询消息，userId: {}, lastSequence: {}", userId, lastSeq);
log.debug("生成序列号，userId: {}, sequence: {}", userId, seq);

// WARN - 业务警告
log.warn("好友关系不存在，无法发送消息，from: {}, to: {}", from, to);
log.warn("消息重复，messageId: {}", messageId);

// ERROR - 系统错误
log.error("数据库操作失败，userId: {}", userId, e);
log.error("Redis连接失败", e);
```

---

## 测试模板

### 单元测试
```java
@ExtendWith(MockitoExtension.class)
class MailboxServiceImplTest {
    
    @Mock
    private MailboxMessageRepository messageRepository;
    
    @Mock
    private SequenceService sequenceService;
    
    @InjectMocks
    private MailboxServiceImpl mailboxService;
    
    @Test
    @DisplayName("保存消息 - 成功场景")
    void testSaveMessage_Success() {
        // Given
        Long userId = 1L;
        MessageDTO dto = new MessageDTO();
        dto.setContent("Hello");
        
        when(sequenceService.generateSequence(userId)).thenReturn(100L);
        when(messageRepository.save(any())).thenReturn(Mono.just(new MailboxMessage()));
        
        // When
        Long sequence = mailboxService.saveMessage(userId, dto);
        
        // Then
        assertThat(sequence).isEqualTo(100L);
        verify(sequenceService, times(1)).generateSequence(userId);
        verify(messageRepository, times(1)).save(any());
    }
}
```

### 集成测试
```java
@SpringBootTest
@ActiveProfiles("test")
class MailboxIntegrationTest {
    
    @Autowired
    private MailboxService mailboxService;
    
    @Test
    @DisplayName("消息流程测试")
    void testMessageFlow() {
        // 1. 保存消息
        Long seq1 = mailboxService.saveMessage(1L, createMessage("msg1"));
        Long seq2 = mailboxService.saveMessage(1L, createMessage("msg2"));
        
        // 2. 同步消息
        SyncResult result = mailboxService.syncMessages(1L, 0L);
        assertThat(result.getMessages()).hasSize(2);
        
        // 3. 标记已读
        mailboxService.markAsRead(1L, seq2);
        
        // 4. 检查未读数
        UnreadCountVO unread = mailboxService.getUnreadCount(1L);
        assertThat(unread.getTotalUnread()).isEqualTo(0);
    }
}
```

---

## 常见错误及解决

### 错误1：Reactive没有触发
```java
// ❌ 错误
repository.save(message);

// ✅ 正确
repository.save(message).block();  // 同步
repository.save(message).subscribe();  // 异步
```

### 错误2：空指针异常
```java
// ❌ 错误
UserMailbox mailbox = repository.findByUserId(userId).block();
Long sequence = mailbox.getLastSequence();  // NPE

// ✅ 正确
Long sequence = repository.findByUserId(userId)
    .map(UserMailbox::getLastSequence)
    .defaultIfEmpty(0L)
    .block();
```

### 错误3：事务不生效
```java
// ❌ 错误：Reactive不支持@Transactional
@Transactional
public Mono<Void> saveMessage() { ... }

// ✅ 正确：使用TransactionalOperator
public Mono<Void> saveMessage() {
    return transactionalOperator.transactional(
        // 操作
    );
}
```

### 错误4：内存溢出
```java
// ❌ 错误：查询所有数据
List<Message> all = repository.findAll().collectList().block();

// ✅ 正确：分页或限制数量
List<Message> limited = repository.findAll()
    .take(100)
    .collectList()
    .block();
```

---

## 性能优化技巧

### 1. 批量操作
```java
// 批量保存
List<MailboxMessage> messages = ...;
messageRepository.saveAll(messages).collectList().block();
```

### 2. 缓存未读数
```java
// Redis缓存未读数
String key = "unread:" + userId;
Long unread = redisTemplate.opsForValue().get(key);
if (unread == null) {
    unread = calculateUnread(userId);
    redisTemplate.opsForValue().set(key, unread, 5, TimeUnit.MINUTES);
}
```

### 3. 异步处理
```java
// 异步保存到Mailbox
CompletableFuture.runAsync(() -> {
    mailboxService.saveMessage(userId, message);
});
```

### 4. 索引优化
```javascript
// MongoDB索引
db.mailbox_message.createIndex({userId: 1, sequence: 1});
db.mailbox_message.createIndex({userId: 1, status: 1, createTime: -1});
```

---

## 调试技巧

### 1. 打印Reactive流
```java
messageRepository.findByUserId(userId)
    .doOnNext(msg -> log.debug("查询到消息: {}", msg))
    .doOnError(e -> log.error("查询失败", e))
    .collectList()
    .block();
```

### 2. 使用Postman测试
```
POST http://localhost:8080/mailbox/save
Content-Type: application/json

{
  "userId": 1,
  "messageId": "msg123",
  "content": "Hello"
}
```

### 3. 查看MongoDB数据
```javascript
// 查询Mailbox
db.mailbox_message.find({userId: 1}).sort({sequence: -1}).limit(10);

// 查询UserMailbox
db.user_mailbox.find({userId: 1});
```

---

## 开发检查清单

### 每个方法完成后
- [ ] 添加日志（INFO/ERROR）
- [ ] 异常处理
- [ ] 参数校验
- [ ] 编写单元测试
- [ ] 用Postman测试

### 每个模块完成后
- [ ] 编写集成测试
- [ ] 检查性能（响应时间）
- [ ] 检查内存占用
- [ ] 更新文档

---

## 求助清单

遇到问题时，提供以下信息：

1. **问题描述**：具体什么错误
2. **错误日志**：完整的堆栈信息
3. **代码片段**：相关代码
4. **已尝试方案**：试过什么方法
5. **环境信息**：JDK版本、依赖版本

---

## 下一步

1. ✅ 收藏本文档
2. ⏳ 开始实现`saveMessage()`
3. ⏳ 遇到问题查阅本文档
4. ⏳ 解决不了再问我

加油！🚀
