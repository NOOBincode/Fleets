# 好友关系验证流程设计

## 一、好友关系状态说明

好友关系有以下4种状态：

| 状态码 | 状态名称 | 说明 |
|-------|---------|------|
| 0 | 待确认 | 好友请求已发送，等待对方处理 |
| 1 | 已确认 | 双方已成为好友 |
| 2 | 已拒绝 | 对方拒绝了好友请求 |
| 3 | 已拉黑 | 单方面拉黑对方 |

## 二、添加好友流程

### 2.1 发送好友请求

**接口**: `POST /api/friend/add`

**流程**:
1. 用户A发送好友请求给用户B
2. 系统创建双向好友关系记录，状态均为`0`（待确认）
3. 系统发送通知给用户B（需要消息模块支持）

**数据库操作**:
```sql
-- 用户A -> 用户B
INSERT INTO friendship (user_id, friend_id, remark, group_name, status) 
VALUES (A, B, '备注', '分组', 0);

-- 用户B -> 用户A
INSERT INTO friendship (user_id, friend_id, remark, group_name, status) 
VALUES (B, A, NULL, '我的好友', 0);
```

**并发安全**:
- 使用分布式锁防止重复添加
- 锁Key: `friend:add:lock:{min(userId,friendId)}:{max(userId,friendId)}`

### 2.2 接受好友请求

**接口**: `POST /api/friend/accept/{friendId}`

**流程**:
1. 用户B接受用户A的好友请求
2. 系统将双方的好友关系状态改为`1`（已确认）
3. 清理相关缓存
4. 发送通知给用户A（需要消息模块支持）

**数据库操作**:
```sql
-- 更新双方状态
UPDATE friendship SET status = 1 WHERE user_id = A AND friend_id = B;
UPDATE friendship SET status = 1 WHERE user_id = B AND friend_id = A;
```

### 2.3 拒绝好友请求

**接口**: `POST /api/friend/reject/{friendId}`

**流程**:
1. 用户B拒绝用户A的好友请求
2. 系统将双方的好友关系状态改为`2`（已拒绝）
3. 清理相关缓存

**数据库操作**:
```sql
-- 更新双方状态
UPDATE friendship SET status = 2 WHERE user_id = A AND friend_id = B;
UPDATE friendship SET status = 2 WHERE user_id = B AND friend_id = A;
```

### 2.4 获取待处理的好友请求

**接口**: `GET /api/friend/requests`

**流程**:
1. 查询当前用户收到的所有待确认好友请求（status=0）
2. 返回发起人的用户信息

## 三、删除好友流程

**接口**: `DELETE /api/friend/{friendId}`

**流程**:
1. 用户A删除好友B
2. 系统删除双向好友关系记录（逻辑删除）
3. 清理相关缓存

**数据库操作**:
```sql
-- 逻辑删除双方关系
UPDATE friendship SET is_deleted = 1 WHERE user_id = A AND friend_id = B;
UPDATE friendship SET is_deleted = 1 WHERE user_id = B AND friend_id = A;
```

## 四、拉黑好友流程

### 4.1 拉黑好友

**接口**: `POST /api/friend/block/{friendId}`

**流程**:
1. 用户A拉黑用户B
2. 系统将A->B的关系状态改为`3`（已拉黑）
3. B->A的关系保持不变（单向拉黑）
4. 清理相关缓存

**特点**:
- 单向操作，只影响拉黑方
- 被拉黑方无法向拉黑方发送消息
- 被拉黑方看不到拉黑方的在线状态

### 4.2 取消拉黑

**接口**: `POST /api/friend/unblock/{friendId}`

**流程**:
1. 用户A取消拉黑用户B
2. 系统将A->B的关系状态改为`1`（已确认）
3. 清理相关缓存

## 五、其他操作

### 5.1 更新好友备注

**接口**: `PUT /api/friend/{friendId}/remark?remark=xxx`

**特点**: 单向操作，只修改当前用户对好友的备注

### 5.2 更新好友分组

**接口**: `PUT /api/friend/{friendId}/group?groupName=xxx`

**特点**: 单向操作，只修改当前用户对好友的分组

### 5.3 获取好友列表

**接口**: `GET /api/friend/list`

**特点**: 
- 只返回状态为`1`（已确认）的好友
- 支持缓存优化
- 批量查询好友用户信息

### 5.4 搜索好友

**接口**: `GET /api/friend/search?keyword=xxx&pageNum=1&pageSize=20`

**特点**: 
- 在已确认的好友范围内搜索
- 支持用户名、昵称模糊查询
- 分页返回结果

## 六、缓存策略

### 6.1 好友列表缓存

**Key**: `friend:list:{userId}`  
**类型**: Redis Set  
**过期时间**: 30分钟  
**存储内容**: 好友ID集合

### 6.2 好友关系缓存

**Key**: `friend:relation:{userId}:{friendId}`  
**类型**: String  
**过期时间**: 30分钟  
**存储内容**: true/false（是否是好友）

### 6.3 缓存一致性

所有修改操作（添加、删除、拉黑、接受、拒绝）都会主动清理相关缓存，保证数据一致性。

## 七、并发安全设计

### 7.1 分布式锁

添加好友时使用Redis分布式锁，防止并发重复添加：
- 锁Key使用较小的userId和较大的userId组合，保证双向一致
- 锁过期时间10秒
- 获取锁失败时提示"操作过于频繁"

### 7.2 事务保证

所有涉及双向关系的操作都使用`@Transactional`注解，保证原子性：
- 添加好友：双向插入
- 删除好友：双向删除
- 接受/拒绝请求：双向更新

### 7.3 幂等性

- 重复添加好友：检查是否已存在关系
- 重复接受/拒绝：检查状态是否为待确认
- 重复拉黑：检查状态是否已拉黑

## 八、代码优化

### 8.1 封装重复代码

使用`createFriendship()`方法封装好友关系对象的创建：

```java
private Friendship createFriendship(Long userId, Long friendId, String remark, 
                                   String groupName, Integer status, Date now) {
    Friendship friendship = new Friendship();
    friendship.setUserId(userId);
    friendship.setFriendId(friendId);
    friendship.setRemark(remark);
    friendship.setGroupName(groupName);
    friendship.setStatus(status);
    friendship.setCreateTime(now);
    friendship.setUpdateTime(now);
    return friendship;
}
```

### 8.2 批量查询优化

获取好友列表时使用批量查询，减少数据库访问次数：

```java
// 批量查询好友用户信息
List<Long> friendIds = friendships.stream()
    .map(Friendship::getFriendId)
    .collect(Collectors.toList());

List<User> friendUsers = userMapper.selectBatchIds(friendIds);
```

## 九、待实现功能

以下功能需要消息模块支持：

1. **好友请求通知**: 发送好友请求时通知对方
2. **好友添加成功通知**: 接受好友请求后通知双方
3. **验证消息**: 支持添加好友时附带验证消息

## 十、API接口列表

| 接口 | 方法 | 说明 |
|-----|------|------|
| /api/friend/add | POST | 发送好友请求 |
| /api/friend/accept/{friendId} | POST | 接受好友请求 |
| /api/friend/reject/{friendId} | POST | 拒绝好友请求 |
| /api/friend/requests | GET | 获取待处理的好友请求 |
| /api/friend/{friendId} | DELETE | 删除好友 |
| /api/friend/block/{friendId} | POST | 拉黑好友 |
| /api/friend/unblock/{friendId} | POST | 取消拉黑 |
| /api/friend/{friendId}/remark | PUT | 更新好友备注 |
| /api/friend/{friendId}/group | PUT | 更新好友分组 |
| /api/friend/list | GET | 获取好友列表 |
| /api/friend/search | GET | 搜索好友 |

---

**文档版本**: v1.0  
**最后更新**: 2025-01-09  
**作者**: Kiro AI
