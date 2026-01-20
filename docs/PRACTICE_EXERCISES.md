# 实战练习：从零实现Mailbox模块

## 🎯 目标

不依赖AI，独立完成Mailbox模块的核心功能，提升业务代码能力。

---

## 📝 练习1：实现writeMessage方法（基础）

### 需求分析

**功能**：将消息写入用户的信箱

**输入**：
- userId: 接收者ID
- conversationId: 会话ID
- message: 消息对象

**输出**：
- true: 写入成功
- false: 写入失败

### 思考题（先思考再写代码）

1. 需要做哪些参数校验？
2. 如果信箱不存在，怎么办？
3. 序列号如何生成？
4. 需要更新哪些数据？
5. 如何保证数据一致性？

### 实现步骤

```java
@Override
public boolean writeMessage(Long userId, String conversationId, Message message) {
    // TODO: 第1步 - 参数校验
    // 提示：userId、conversationId、message都不能为空
    
    
    // TODO: 第2步 - 获取或创建信箱
    // 提示：使用userMailboxRepository.findByUserIdAndConversationId()
    // 如果不存在，调用createNewMailbox()创建
    
    
    // TODO: 第3步 - 生成序列号
    // 提示：使用sequenceService.generateSequence()
    
    
    // TODO: 第4步 - 创建MailboxMessage对象
    // 提示：设置所有必要字段
    
    
    // TODO: 第5步 - 保存到MongoDB
    // 提示：使用mailboxMessageRepository.save()
    
    
    // TODO: 第6步 - 更新信箱元数据
    // 提示：更新sequence、lastMessageId、unreadCount等
    
    
    // TODO: 第7步 - 清理缓存
    // 提示：清理未读数缓存
    
    
    return true;
}
```

### 参考答案（先自己写，再看答案）

<details>
<summary>点击查看答案</summary>

```java
@Override
public boolean writeMessage(Long userId, String conversationId, Message message) {
    log.info("写入消息到信箱，userId: {}, conversationId: {}", userId, conversationId);
    
    try {
        // 1. 参数校验
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        if (message == null || !StringUtils.hasText(message.getId())) {
            throw new IllegalArgumentException("消息不能为空");
        }
        
        // 2. 获取或创建信箱
        UserMailbox mailbox = userMailboxRepository
            .findByUserIdAndConversationId(userId, conversationId)
            .orElseGet(() -> createNewMailbox(userId, conversationId));
        
        // 3. 生成序列号
        Long sequence = sequenceService.generateSequence(userId, conversationId);
        
        // 4. 创建MailboxMessage
        MailboxMessage mailboxMsg = new MailboxMessage();
        mailboxMsg.setUserId(userId);
        mailboxMsg.setConversationId(conversationId);
        mailboxMsg.setSequence(sequence);
        mailboxMsg.setMessageId(message.getId());
        mailboxMsg.setSenderId(message.getSenderId());
        mailboxMsg.setContent(message.getContent());
        mailboxMsg.setStatus(0); // 未读
        mailboxMsg.setCreateTime(new Date());
        
        // 5. 保存到MongoDB
        mailboxMessageRepository.save(mailboxMsg);
        
        // 6. 更新信箱元数据
        mailbox.setSequence(sequence);
        mailbox.setLastMessageId(message.getId());
        mailbox.setUnreadCount(mailbox.getUnreadCount() + 1);
        mailbox.setUpdateTime(new Date());
        userMailboxRepository.save(mailbox);
        
        // 7. 清理缓存
        redisService.delete("mailbox:unread:" + userId);
        
        log.info("写入消息成功");
        return true;
        
    } catch (Exception e) {
        log.error("写入消息失败", e);
        return false;
    }
}
```

</details>

---

## 📝 练习2：实现pullOfflineMessages方法（进阶）

### 需求分析

**功能**：拉取用户的所有离线消息

**输入**：
- userId: 用户ID
- lastSequence: 上次同步的序列号

**输出**：
- List<MessageVO>: 离线消息列表

### 思考题

1. 需要查询哪些数据？
2. 如何关联查询消息内容？
3. 如何关联查询发送者信息？
4. 如何优化性能（避免N+1查询）？
5. 如果消息很多，如何分页？

### 实现步骤

```java
@Override
public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence) {
    // TODO: 第1步 - 参数校验
    
    
    // TODO: 第2步 - 查询所有信箱
    // 提示：使用userMailboxRepository.findByUserId()
    
    
    // TODO: 第3步 - 遍历信箱，查询离线消息
    // 提示：sequence > lastSequence
    
    
    // TODO: 第4步 - 转换为MessageVO
    
    
    // TODO: 第5步 - 批量查询发送者信息（优化性能）
    // 提示：先收集所有senderId，再批量查询
    
    
    // TODO: 第6步 - 填充发送者信息到VO
    
    
    return result;
}
```

### 自己实现（不要看答案）

---

## 📝 练习3：实现batchWriteMessage方法（高级）

### 需求分析

**功能**：批量写入消息（群聊场景）

**输入**：
- userIds: 接收者ID列表
- conversationId: 会话ID
- message: 消息对象

**输出**：
- true: 写入成功
- false: 写入失败

### 思考题

1. 如何批量生成序列号？
2. 如何批量创建MailboxMessage？
3. 如何批量保存到MongoDB？
4. 如何保证原子性？
5. 如何优化性能？

### 挑战

**要求**：
1. 支持1000个用户同时接收消息
2. 性能要求：< 1秒完成
3. 保证数据一致性

### 自己实现

---

## 🧪 单元测试练习

### 练习4：为writeMessage写单元测试

```java
@SpringBootTest
public class MailboxServiceTest {
    
    @Autowired
    private MailboxService mailboxService;
    
    @Autowired
    private UserMailboxRepository userMailboxRepository;
    
    @Autowired
    private MailboxMessageRepository mailboxMessageRepository;
    
    @Test
    public void testWriteMessage_Success() {
        // TODO: 测试正常写入消息
        // 1. 准备测试数据
        
        
        // 2. 执行方法
        
        
        // 3. 验证结果
        
    }
    
    @Test
    public void testWriteMessage_NullUserId() {
        // TODO: 测试userId为null的情况
        
    }
    
    @Test
    public void testWriteMessage_NullConversationId() {
        // TODO: 测试conversationId为null的情况
        
    }
    
    @Test
    public void testWriteMessage_CreateNewMailbox() {
        // TODO: 测试信箱不存在时自动创建
        
    }
}
```

---

## 🎓 学习检查清单

完成以上练习后，检查自己是否掌握：

### 基础能力
- [ ] 能独立分析需求
- [ ] 能设计方法签名
- [ ] 能写参数校验
- [ ] 能处理异常情况
- [ ] 能写日志

### 数据库操作
- [ ] 能使用Repository查询
- [ ] 能使用Repository保存
- [ ] 能使用Optional处理null
- [ ] 能使用Stream处理集合
- [ ] 能批量操作数据

### 业务逻辑
- [ ] 能设计业务流程
- [ ] 能处理边界情况
- [ ] 能保证数据一致性
- [ ] 能优化性能
- [ ] 能写单元测试

---

## 💡 提示

### 遇到问题怎么办？

1. **先思考**：这个问题的本质是什么？
2. **查文档**：Spring Data MongoDB文档
3. **看源码**：MongoRepository的实现
4. **写测试**：用单元测试验证想法
5. **问同学**：讨论不同的实现方案

### 不要做的事

- ❌ 直接复制AI生成的代码
- ❌ 不理解就提交代码
- ❌ 不写单元测试
- ❌ 不处理异常
- ❌ 不写注释和日志

### 要做的事

- ✅ 先画流程图再写代码
- ✅ 每个方法都要理解
- ✅ 写完代码立即测试
- ✅ 遇到问题先调试
- ✅ 写清晰的注释

---

## 🎯 进阶挑战

完成基础练习后，尝试以下挑战：

### 挑战1：性能优化
- 优化pullOfflineMessages，支持10万条消息
- 使用分页、索引、缓存

### 挑战2：并发安全
- 处理多个线程同时写入消息
- 使用分布式锁、乐观锁

### 挑战3：数据一致性
- 保证MongoDB和Redis的数据一致性
- 处理写入失败的回滚

### 挑战4：监控告警
- 添加性能监控
- 添加异常告警
- 添加业务指标统计

---

**记住：编程是一门手艺，需要大量练习才能掌握！**

加油！💪
