# Mailbox Repository 使用指南

## 概述

Mailbox 模块提供了多种 Repository 实现，分别用于不同的数据存储和访问场景。

## Repository 分类

### 1. 主 Repository（同步版本）

位于 `org.example.fleets.mailbox.repository` 包下，用于同步操作。

#### UserMailboxRepository
- **类型**：MongoRepository（同步）
- **用途**：用户信箱的同步操作
- **适用场景**：
  - 简单的 CRUD 操作
  - 不需要响应式编程的场景
  - 与传统 Spring MVC 配合使用

#### MailboxMessageRepository
- **类型**：MongoRepository（同步）
- **用途**：信箱消息的同步操作
- **适用场景**：
  - 消息查询和更新
  - 批量操作
  - 统计查询

### 2. MongoDB Repository（响应式版本）

位于 `org.example.fleets.mailbox.repository.mongodb` 包下，用于异步操作。

#### UserMailboxRepository（响应式）
- **类型**：ReactiveMongoRepository
- **用途**：用户信箱的异步操作
- **返回类型**：Mono/Flux
- **适用场景**：
  - 高并发场景
  - 需要非阻塞 I/O
  - WebFlux 应用

**主要方法**：
```java
// 查询单个信箱
Mono<UserMailbox> findByUserIdAndConversationId(Long userId, String conversationId);

// 查询用户所有信箱
Flux<UserMailbox> findByUserId(Long userId);

// 按会话类型查询
Flux<UserMailbox> findByUserIdAndConversationType(Long userId, Integer conversationType);

// 删除操作
Mono<Long> deleteByUserId(Long userId);
Mono<Long> deleteByUserIdAndConversationId(Long userId, String conversationId);

// 统计
Mono<Long> countByUserId(Long userId);
```

#### OfflineMessageRepository（响应式）
- **类型**：ReactiveMongoRepository
- **用途**：离线消息的异步操作
- **返回类型**：Mono/Flux
- **适用场景**：
  - 消息拉取
  - 增量同步
  - 批量查询

**主要方法**：
```java
// 增量查询（序列号大于指定值）
Flux<MailboxMessage> findByUserIdAndConversationIdAndSequenceGreaterThan(
    Long userId, String conversationId, Long sequence, Pageable pageable);

// 按状态查询
Flux<MailboxMessage> findByUserIdAndStatus(Long userId, Integer status, Pageable pageable);

// 精确查询
Mono<MailboxMessage> findByUserIdAndConversationIdAndSequence(
    Long userId, String conversationId, Long sequence);

// 统计未读数
Mono<Long> countByUserIdAndStatus(Long userId, Integer status);
Mono<Long> countByUserIdAndConversationIdAndStatus(
    Long userId, String conversationId, Integer status);

// 删除操作
Mono<Long> deleteByStatusAndCreateTimeBefore(Integer status, Date createTime);
Mono<Long> deleteByUserIdAndConversationId(Long userId, String conversationId);
```

### 3. MySQL Repository（元数据管理）

位于 `org.example.fleets.mailbox.repository.mysql` 包下，用于元数据持久化。

#### SequenceRepository
- **类型**：MyBatis Mapper
- **用途**：序列号持久化备份（可选）
- **说明**：主要序列号生成使用 Redis，这里仅用于备份和恢复

**主要方法**：
```java
// 获取序列号
Long getSequence(Long userId, String conversationId);

// 更新序列号
int updateSequence(Long userId, String conversationId, Long sequence);

// 批量更新
int batchUpdateSequence(List<SequenceEntity> sequences);
```

#### MailboxRepository
- **类型**：MyBatis Mapper
- **用途**：信箱元数据管理（可选）
- **说明**：主要数据在 MongoDB，这里用于统计和查询优化

**主要方法**：
```java
// 获取会话列表
List<MailboxMetadata> getUserConversations(Long userId, int limit);

// 统计未读
int getUnreadConversationCount(Long userId);
int getTotalUnreadCount(Long userId);

// 更新元数据
int updateMailboxMetadata(MailboxMetadata metadata);

// 未读数管理
int incrementUnreadCount(Long userId, String conversationId, int increment);
int clearUnreadCount(Long userId, String conversationId);

// 删除
int deleteConversation(Long userId, String conversationId);
```

## 使用建议

### 1. 选择同步 vs 响应式

**使用同步版本**（主 Repository）：
- 简单的业务逻辑
- 传统 Spring MVC 应用
- 不需要高并发处理
- 团队对响应式编程不熟悉

**使用响应式版本**（MongoDB Repository）：
- 高并发场景
- 需要非阻塞 I/O
- WebFlux 应用
- 需要流式处理大量数据

### 2. MongoDB vs MySQL

**MongoDB**（主存储）：
- 消息内容存储
- 信箱数据存储
- 高频读写操作
- 需要灵活的数据结构

**MySQL**（辅助存储）：
- 元数据备份
- 统计查询优化
- 序列号持久化备份
- 需要事务保证的场景

### 3. 性能优化建议

1. **使用分页查询**：
```java
Pageable pageable = PageRequest.of(0, 100, Sort.by("sequence").descending());
Flux<MailboxMessage> messages = repository.findByUserId(userId, pageable);
```

2. **批量操作**：
```java
// 批量保存
List<MailboxMessage> messages = ...;
mailboxMessageRepository.saveAll(messages);
```

3. **使用索引**：
确保 MongoDB 集合已创建必要的索引（参考 `mailbox_indexes.js`）

4. **缓存热点数据**：
```java
// 未读数缓存在 Redis
String key = "mailbox:unread:" + userId;
redisService.set(key, unreadCount, 5, TimeUnit.MINUTES);
```

## 示例代码

### 同步操作示例
```java
@Service
@RequiredArgsConstructor
public class MailboxService {
    private final UserMailboxRepository userMailboxRepository;
    private final MailboxMessageRepository mailboxMessageRepository;
    
    public void writeMessage(Long userId, String conversationId, Message message) {
        // 获取或创建信箱
        UserMailbox mailbox = userMailboxRepository
            .findByUserIdAndConversationId(userId, conversationId)
            .orElseGet(() -> createNewMailbox(userId, conversationId));
        
        // 保存消息
        MailboxMessage mailboxMsg = new MailboxMessage();
        // ... 设置字段
        mailboxMessageRepository.save(mailboxMsg);
        
        // 更新信箱
        mailbox.setUnreadCount(mailbox.getUnreadCount() + 1);
        userMailboxRepository.save(mailbox);
    }
}
```

### 响应式操作示例
```java
@Service
@RequiredArgsConstructor
public class ReactiveMailboxService {
    private final org.example.fleets.mailbox.repository.mongodb.UserMailboxRepository 
        reactiveUserMailboxRepository;
    private final OfflineMessageRepository offlineMessageRepository;
    
    public Mono<Void> writeMessageAsync(Long userId, String conversationId, Message message) {
        return reactiveUserMailboxRepository
            .findByUserIdAndConversationId(userId, conversationId)
            .switchIfEmpty(Mono.defer(() -> createNewMailboxAsync(userId, conversationId)))
            .flatMap(mailbox -> {
                // 创建消息
                MailboxMessage mailboxMsg = new MailboxMessage();
                // ... 设置字段
                
                return offlineMessageRepository.save(mailboxMsg)
                    .then(updateMailboxAsync(mailbox));
            });
    }
}
```

## 注意事项

1. **包名冲突**：
   - 主 Repository 和 MongoDB Repository 有同名接口
   - 使用时需要指定完整包名或使用别名

2. **事务处理**：
   - MongoDB 的事务支持有限
   - 跨 MongoDB 和 MySQL 的操作无法使用分布式事务
   - 建议使用最终一致性方案

3. **数据一致性**：
   - MongoDB 为主存储
   - MySQL 为辅助存储，允许短暂不一致
   - 通过定时任务同步数据

4. **性能监控**：
   - 监控慢查询
   - 关注索引使用情况
   - 定期清理过期数据
