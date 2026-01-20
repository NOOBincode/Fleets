# Mailbox 数据访问层设计指南

## 📋 架构概述

Mailbox模块采用 **MongoDB + MySQL 混合架构**：

- **MongoDB**：存储消息内容和信箱数据（高频读写）
- **MySQL**：存储用户、好友、群组等关系数据（复杂查询）

---

## 🔄 Repository vs Mapper

### 当前使用情况

| 模块 | 数据库 | 访问方式 | 原因 |
|-----|--------|---------|------|
| User/Friendship/Group | MySQL | MyBatis-Plus Mapper | 关系型数据，复杂SQL |
| Message/Mailbox | MongoDB | Spring Data Repository | 文档型数据，简单CRUD |

### 是否需要统一？

**答案：不需要统一，保持现状即可** ✅

**理由**：
1. **技术栈匹配**：
   - MyBatis-Plus 是 MySQL 的最佳实践
   - Spring Data MongoDB 是 MongoDB 的标准方案

2. **功能特性**：
   - Mapper 支持复杂SQL、动态SQL
   - Repository 支持方法名查询、响应式编程

3. **社区标准**：
   - 大部分项目都是这样混用的
   - 符合Spring生态的最佳实践

---

## ⚠️ 重要问题：响应式 vs 同步

### 当前问题

你的Repository使用了 `ReactiveMongoRepository`（响应式），但Service可能是同步的：

```java
// Repository - 响应式
public interface UserMailboxRepository extends ReactiveMongoRepository<UserMailbox, String> {
    Mono<UserMailbox> findByUserIdAndConversationId(Long userId, String conversationId);
    Flux<UserMailbox> findByUserId(Long userId);
}

// Service - 同步？
public class MailboxServiceImpl implements MailboxService {
    public boolean writeMessage(Long userId, String conversationId, Message message) {
        // 如何调用响应式的Repository？
    }
}
```

### 解决方案

#### 方案1：改为同步Repository（推荐）✅

**适用场景**：
- 你的项目不需要响应式编程
- 并发量不是特别高（< 10万QPS）
- 团队对响应式编程不熟悉

**实现**：

```java
// 1. 改用同步Repository
import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface UserMailboxRepository extends MongoRepository<UserMailbox, String> {
    
    // 返回类型改为同步
    UserMailbox findByUserIdAndConversationId(Long userId, String conversationId);
    List<UserMailbox> findByUserId(Long userId);
}

@Repository
public interface MailboxMessageRepository extends MongoRepository<MailboxMessage, String> {
    
    List<MailboxMessage> findByUserIdAndConversationIdAndSequenceGreaterThan(
        Long userId, 
        String conversationId, 
        Long sequence,
        Pageable pageable
    );
    
    MailboxMessage findByUserIdAndConversationIdAndSequence(
        Long userId, 
        String conversationId, 
        Long sequence
    );
}

// 2. Service中直接使用
@Service
@RequiredArgsConstructor
public class MailboxServiceImpl implements MailboxService {
    
    private final UserMailboxRepository userMailboxRepository;
    private final MailboxMessageRepository mailboxMessageRepository;
    
    @Override
    public boolean writeMessage(Long userId, String conversationId, Message message) {
        // 同步调用，简单直接
        UserMailbox mailbox = userMailboxRepository
            .findByUserIdAndConversationId(userId, conversationId);
        
        if (mailbox == null) {
            mailbox = createNewMailbox(userId, conversationId);
            userMailboxRepository.save(mailbox);
        }
        
        // 生成序列号
        Long sequence = generateSequence(userId, conversationId);
        
        // 创建信箱消息
        MailboxMessage mailboxMsg = new MailboxMessage();
        mailboxMsg.setUserId(userId);
        mailboxMsg.setConversationId(conversationId);
        mailboxMsg.setSequence(sequence);
        mailboxMsg.setMessageId(message.getId());
        mailboxMsg.setStatus(0);
        
        mailboxMessageRepository.save(mailboxMsg);
        
        return true;
    }
}
```

---

#### 方案2：保持响应式（高级）

**适用场景**：
- 需要高并发支持
- 团队熟悉响应式编程
- 愿意投入学习成本

**实现**：

```java
// Service也改为响应式
@Service
@RequiredArgsConstructor
public class MailboxServiceImpl implements MailboxService {
    
    private final UserMailboxRepository userMailboxRepository;
    private final MailboxMessageRepository mailboxMessageRepository;
    
    @Override
    public Mono<Boolean> writeMessage(Long userId, String conversationId, Message message) {
        return userMailboxRepository
            .findByUserIdAndConversationId(userId, conversationId)
            .switchIfEmpty(createNewMailbox(userId, conversationId))
            .flatMap(mailbox -> {
                Long sequence = generateSequence(userId, conversationId);
                
                MailboxMessage mailboxMsg = new MailboxMessage();
                mailboxMsg.setUserId(userId);
                mailboxMsg.setConversationId(conversationId);
                mailboxMsg.setSequence(sequence);
                mailboxMsg.setMessageId(message.getId());
                
                return mailboxMessageRepository.save(mailboxMsg);
            })
            .map(saved -> true)
            .onErrorReturn(false);
    }
}
```

---

## 🎯 推荐方案：改为同步Repository

### 修改步骤

#### 1. 修改 UserMailboxRepository

```java
package org.example.fleets.mailbox.repository;

import org.example.fleets.mailbox.model.entity.UserMailbox;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户信箱Repository - 同步版本
 */
@Repository
public interface UserMailboxRepository extends MongoRepository<UserMailbox, String> {
    
    /**
     * 根据用户ID和会话ID查询信箱
     */
    Optional<UserMailbox> findByUserIdAndConversationId(Long userId, String conversationId);
    
    /**
     * 根据用户ID查询所有信箱
     */
    List<UserMailbox> findByUserId(Long userId);
    
    /**
     * 根据用户ID和会话类型查询信箱
     */
    List<UserMailbox> findByUserIdAndConversationType(Long userId, Integer conversationType);
}
```

#### 2. 修改 MailboxMessageRepository

```java
package org.example.fleets.mailbox.repository;

import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 信箱消息Repository - 同步版本
 */
@Repository
public interface MailboxMessageRepository extends MongoRepository<MailboxMessage, String> {
    
    /**
     * 根据用户ID和会话ID查询消息（序列号大于指定值）
     */
    List<MailboxMessage> findByUserIdAndConversationIdAndSequenceGreaterThan(
        Long userId, 
        String conversationId, 
        Long sequence,
        Pageable pageable
    );
    
    /**
     * 根据用户ID和状态查询消息
     */
    List<MailboxMessage> findByUserIdAndStatus(Long userId, Integer status);
    
    /**
     * 根据用户ID、会话ID和序列号查询消息
     */
    Optional<MailboxMessage> findByUserIdAndConversationIdAndSequence(
        Long userId, 
        String conversationId, 
        Long sequence
    );
    
    /**
     * 删除过期消息
     */
    void deleteByStatusAndCreateTimeBefore(Integer status, Date createTime);
    
    /**
     * 统计未读消息数
     */
    long countByUserIdAndStatus(Long userId, Integer status);
    
    /**
     * 统计会话未读数
     */
    long countByUserIdAndConversationIdAndStatus(Long userId, String conversationId, Integer status);
}
```

---

## 💡 MongoDB + MySQL 混合查询场景

### 场景1：拉取离线消息（需要关联查询）

```java
@Override
public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence) {
    // 1. 从MongoDB查询信箱消息
    List<MailboxMessage> mailboxMessages = mailboxMessageRepository
        .findByUserIdAndStatus(userId, 0); // 0-未读
    
    // 2. 提取消息ID列表
    List<String> messageIds = mailboxMessages.stream()
        .map(MailboxMessage::getMessageId)
        .collect(Collectors.toList());
    
    // 3. 从MongoDB批量查询消息内容
    List<Message> messages = messageRepository.findAllById(messageIds);
    
    // 4. 如果需要发送者信息，从MySQL查询
    List<Long> senderIds = messages.stream()
        .map(Message::getSenderId)
        .distinct()
        .collect(Collectors.toList());
    
    List<User> senders = userMapper.selectBatchIds(senderIds);
    Map<Long, User> senderMap = senders.stream()
        .collect(Collectors.toMap(User::getId, u -> u));
    
    // 5. 组装VO
    return messages.stream()
        .map(msg -> {
            MessageVO vo = messageConverter.toVO(msg);
            User sender = senderMap.get(msg.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
            return vo;
        })
        .collect(Collectors.toList());
}
```

### 场景2：发送群聊消息（批量写入）

```java
@Override
public boolean batchWriteMessage(List<Long> userIds, String conversationId, Message message) {
    // 1. 批量生成序列号
    Map<Long, Long> sequenceMap = new HashMap<>();
    for (Long userId : userIds) {
        Long sequence = sequenceService.generateSequence(userId, conversationId);
        sequenceMap.put(userId, sequence);
    }
    
    // 2. 批量创建信箱消息
    List<MailboxMessage> mailboxMessages = userIds.stream()
        .map(userId -> {
            MailboxMessage msg = new MailboxMessage();
            msg.setUserId(userId);
            msg.setConversationId(conversationId);
            msg.setSequence(sequenceMap.get(userId));
            msg.setMessageId(message.getId());
            msg.setStatus(0);
            msg.setCreateTime(new Date());
            return msg;
        })
        .collect(Collectors.toList());
    
    // 3. MongoDB批量插入
    mailboxMessageRepository.saveAll(mailboxMessages);
    
    // 4. 更新信箱元数据（可以异步）
    for (Long userId : userIds) {
        updateMailboxMetadata(userId, conversationId, sequenceMap.get(userId));
    }
    
    return true;
}
```

### 场景3：获取未读消息数（聚合查询）

```java
@Override
public UnreadCountVO getUnreadCount(Long userId) {
    // 1. 从MongoDB统计未读数
    long totalUnread = mailboxMessageRepository.countByUserIdAndStatus(userId, 0);
    
    // 2. 查询各会话的未读数
    List<UserMailbox> mailboxes = userMailboxRepository.findByUserId(userId);
    
    Map<String, Integer> conversationUnreadMap = mailboxes.stream()
        .collect(Collectors.toMap(
            UserMailbox::getConversationId,
            UserMailbox::getUnreadCount
        ));
    
    // 3. 组装结果
    UnreadCountVO vo = new UnreadCountVO();
    vo.setTotalUnread((int) totalUnread);
    vo.setConversationUnreadMap(conversationUnreadMap);
    
    return vo;
}
```

---

## 📝 最佳实践

### 1. 数据一致性

```java
@Transactional(rollbackFor = Exception.class)
public boolean writeMessage(Long userId, String conversationId, Message message) {
    try {
        // MongoDB操作（不支持事务，需要手动回滚）
        MailboxMessage mailboxMsg = createMailboxMessage(userId, conversationId, message);
        MailboxMessage saved = mailboxMessageRepository.save(mailboxMsg);
        
        // 更新元数据
        updateMailboxMetadata(userId, conversationId, saved.getSequence());
        
        return true;
    } catch (Exception e) {
        // 手动回滚MongoDB操作
        rollbackMailboxMessage(mailboxMsg);
        throw e;
    }
}
```

### 2. 性能优化

```java
// 使用批量操作
List<MailboxMessage> messages = mailboxMessageRepository.saveAll(mailboxMessages);

// 使用分页查询
Pageable pageable = PageRequest.of(0, 100, Sort.by("sequence").ascending());
List<MailboxMessage> messages = mailboxMessageRepository
    .findByUserIdAndConversationIdAndSequenceGreaterThan(userId, conversationId, lastSeq, pageable);

// 使用索引
// 在MongoDB中创建复合索引
db.mailbox_message.createIndex({ userId: 1, conversationId: 1, sequence: -1 });
```

### 3. 缓存策略

```java
@Override
public UnreadCountVO getUnreadCount(Long userId) {
    // 先查Redis缓存
    String cacheKey = "mailbox:unread:" + userId;
    UnreadCountVO cached = (UnreadCountVO) redisService.get(cacheKey);
    if (cached != null) {
        return cached;
    }
    
    // 查询MongoDB
    UnreadCountVO vo = queryUnreadCountFromDB(userId);
    
    // 写入缓存（5分钟过期）
    redisService.set(cacheKey, vo, 5, TimeUnit.MINUTES);
    
    return vo;
}
```

---

## 🎯 总结

### 推荐方案

1. ✅ **保持 Mapper + Repository 混用**
   - MySQL 用 MyBatis-Plus Mapper
   - MongoDB 用 Spring Data Repository

2. ✅ **改为同步Repository**
   - 将 `ReactiveMongoRepository` 改为 `MongoRepository`
   - 返回类型从 `Mono/Flux` 改为 `Optional/List`

3. ✅ **混合查询策略**
   - 先查MongoDB获取消息ID
   - 再查MySQL获取用户信息
   - 最后组装VO返回

### 不需要做的

- ❌ 不需要统一为Mapper
- ❌ 不需要统一为Repository
- ❌ 不需要改为响应式（除非有高并发需求）

---

**文档版本**: v1.0  
**最后更新**: 2025-01-18  
**作者**: Kiro AI
