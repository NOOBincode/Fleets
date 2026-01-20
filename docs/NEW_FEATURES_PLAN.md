# Fleets 新增功能实现计划

## 概述

根据前端需求，需要在后端添加以下三个功能模块：
1. **好友申请审核流程** - 完善现有好友系统
2. **好友分组管理** - 增强好友列表组织能力
3. **表情包系统** - 丰富消息表达方式

## 一、好友申请审核流程

### 1.1 现状分析

后端已有基础接口设计（见 `FriendshipService.java`），但部分接口未实现：
- ✅ `addFriend()` - 发送好友请求
- ✅ `acceptFriendRequest()` - 接受好友请求
- ✅ `rejectFriendRequest()` - 拒绝好友请求
- ❌ `getPendingFriendRequests()` - 获取待处理请求列表
- ❌ 待审核数量统计接口

### 1.2 需要实现的接口

| 接口 | 方法 | 说明 | 优先级 |
|-----|------|------|--------|
| `/friendship/requests` | GET | 获取待审核的好友申请列表 | 高 |
| `/friendship/requests/count` | GET | 获取待审核数量（用于红点提示） | 高 |
| `/friendship/add` | POST | 发送好友申请（支持验证消息） | 中 |

### 1.3 数据库设计

现有 `friendship` 表已支持，需确保包含以下字段：
```sql
- status: 0-待确认, 1-已确认, 2-已拒绝, 3-已拉黑
- verify_message: 验证消息（可选）
- create_time: 申请时间
```

### 1.4 实现步骤

1. **Controller层** (`FriendshipController.java`)
   ```java
   @GetMapping("/requests")
   public CommonResult<List<FriendVO>> getPendingRequests() {
       Long userId = StpUtil.getLoginIdAsLong();
       return CommonResult.success(friendshipService.getPendingFriendRequests(userId));
   }
   
   @GetMapping("/requests/count")
   public CommonResult<Integer> getPendingCount() {
       Long userId = StpUtil.getLoginIdAsLong();
       return CommonResult.success(friendshipService.getPendingRequestCount(userId));
   }
   ```

2. **Service层** (`FriendshipServiceImpl.java`)
   - 实现 `getPendingFriendRequests()` 方法
   - 新增 `getPendingRequestCount()` 方法
   - 查询 status=0 的记录
   - 批量查询发起人用户信息

3. **缓存优化**
   - Key: `friend:requests:{userId}`
   - 过期时间: 5分钟
   - 修改操作时清除缓存

## 二、好友分组管理

### 2.1 现状分析

后端已有基础支持：
- ✅ `updateGroup()` - 更新好友分组
- ❌ 按分组获取好友列表
- ❌ 获取所有分组列表

### 2.2 需要实现的接口

| 接口 | 方法 | 说明 | 优先级 |
|-----|------|------|--------|
| `/friendship/list/grouped` | GET | 按分组返回好友列表 | 高 |
| `/friendship/groups` | GET | 获取当前用户的所有分组 | 中 |

### 2.3 数据结构设计

**分组好友列表响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "groupName": "我的好友",
      "friends": [
        {
          "userId": 1,
          "username": "user1",
          "nickname": "张三",
          "avatar": "...",
          "remark": "同事",
          "groupName": "我的好友",
          "status": 1
        }
      ]
    },
    {
      "groupName": "同事",
      "friends": [...]
    }
  ]
}
```

**分组列表响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "groupName": "我的好友",
      "count": 10
    },
    {
      "groupName": "同事",
      "count": 5
    }
  ]
}
```

### 2.4 实现步骤

1. **Controller层**
   ```java
   @GetMapping("/list/grouped")
   public CommonResult<List<GroupingFriendVO>> getGroupedFriendList() {
       Long userId = StpUtil.getLoginIdAsLong();
       return CommonResult.success(friendshipService.getGroupedFriendList(userId));
   }
   
   @GetMapping("/groups")
   public CommonResult<List<GroupingVO>> getGroups() {
       Long userId = StpUtil.getLoginIdAsLong();
       return CommonResult.success(friendshipService.getUserGroups(userId));
   }
   ```

2. **Service层**
   - 新增 `getGroupedFriendList()` 方法
   - 新增 `getUserGroups()` 方法
   - 使用 Stream API 按 groupName 分组
   - 统计每个分组的好友数量

3. **VO类**
   - 创建 `GroupingFriendVO.java`
   - 创建 `GroupingVO.java`

## 三、表情包系统

### 3.1 功能设计

表情包系统包含：
- 系统预置表情包（emoji、常用表情）
- 用户自定义表情包（上传图片）
- 表情包分类管理

### 3.2 需要实现的接口

| 接口 | 方法 | 说明 | 优先级 |
|-----|------|------|--------|
| `/expression/list` | GET | 获取表情包列表（按分类） | 高 |
| `/expression/upload` | POST | 上传自定义表情包 | 中 |
| `/expression/{id}` | DELETE | 删除自定义表情包 | 低 |

### 3.3 数据库设计

**表名**: `expression`

```sql
CREATE TABLE expression (
    id BIGINT PRIMARY KEY,
    user_id BIGINT COMMENT '用户ID，NULL表示系统表情',
    name VARCHAR(50) NOT NULL COMMENT '表情名称',
    url VARCHAR(500) NOT NULL COMMENT '表情图片URL',
    category VARCHAR(50) DEFAULT 'default' COMMENT '分类：emoji/custom/system',
    sort INT DEFAULT 0 COMMENT '排序',
    is_deleted TINYINT DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_user_id (user_id),
    INDEX idx_category (category)
) COMMENT '表情包表';
```

### 3.4 实现步骤

1. **创建模块结构**
   ```
   org.example.fleets.expression/
   ├── controller/
   │   └── ExpressionController.java
   ├── service/
   │   ├── ExpressionService.java
   │   └── impl/
   │       └── ExpressionServiceImpl.java
   ├── model/
   │   ├── entity/
   │   │   └── Expression.java
   │   └── vo/
   │       ├── ExpressionVO.java
   │       └── ExpressionCategoryVO.java
   └── mapper/
       └── ExpressionMapper.java
   ```

2. **Controller层**
   ```java
   @RestController
   @RequestMapping("/expression")
   public class ExpressionController {
       
       @GetMapping("/list")
       public CommonResult<List<ExpressionCategoryVO>> getExpressionList() {
           Long userId = StpUtil.getLoginIdAsLong();
           return CommonResult.success(expressionService.getExpressionList(userId));
       }
       
       @PostMapping("/upload")
       public CommonResult<ExpressionVO> uploadExpression(@RequestParam("file") MultipartFile file) {
           Long userId = StpUtil.getLoginIdAsLong();
           return CommonResult.success(expressionService.uploadExpression(userId, file));
       }
       
       @DeleteMapping("/{id}")
       public CommonResult<Boolean> deleteExpression(@PathVariable Long id) {
           Long userId = StpUtil.getLoginIdAsLong();
           return CommonResult.success(expressionService.deleteExpression(userId, id));
       }
   }
   ```

3. **Service层**
   - 实现表情包列表查询（系统+用户自定义）
   - 实现文件上传（复用现有文件上传服务）
   - 实现表情包删除（仅允许删除自己的）
   - 按分类分组返回

4. **初始化系统表情包**
   - 创建 `ExpressionInitializer.java`
   - 使用 `@PostConstruct` 初始化系统表情
   - 预置常用emoji和表情图片

### 3.5 文件存储

- 复用现有的文件上传服务 (`FileService`)
- 表情包图片存储路径: `/uploads/expressions/{userId}/{filename}`
- 支持的格式: jpg, png, gif (限制大小 < 500KB)

## 四、实现优先级

### Phase 1 - 核心功能（本周完成）
1. ✅ 好友申请审核接口
   - `/friendship/requests` - 获取待审核列表
   - `/friendship/requests/count` - 获取待审核数量

2. ✅ 好友分组接口
   - `/friendship/list/grouped` - 按分组获取好友
   - `/friendship/groups` - 获取分组列表

### Phase 2 - 增强功能（下周完成）
3. ⏳ 表情包基础功能
   - `/expression/list` - 获取表情包列表
   - 初始化系统表情包

### Phase 3 - 扩展功能（可选）
4. ⏳ 表情包高级功能
   - `/expression/upload` - 上传自定义表情
   - `/expression/{id}` - 删除表情包

## 五、测试计划

### 5.1 单元测试
- `FriendshipServiceTest` - 测试好友申请和分组功能
- `ExpressionServiceTest` - 测试表情包CRUD

### 5.2 集成测试
- 使用 Postman 测试所有新增接口
- 验证前后端联调

### 5.3 性能测试
- 好友列表查询性能（1000+好友）
- 表情包列表加载性能

## 六、前端适配

前端已创建对应的TypeScript API文件：
- ✅ `src/api/apply/index.ts` - 好友申请API
- ✅ `src/api/grouping/index.ts` - 好友分组API
- ✅ `src/api/expression/index.ts` - 表情包API

## 七、文档更新

需要更新的文档：
- [ ] `FRIENDSHIP_WORKFLOW.md` - 添加新增接口说明
- [ ] `API_REFERENCE.md` - 添加API文档
- [ ] `README.md` - 更新功能列表

## 八、注意事项

1. **向后兼容**: 新增接口不影响现有功能
2. **权限控制**: 所有接口需要登录认证
3. **数据安全**: 用户只能操作自己的数据
4. **性能优化**: 合理使用缓存，避免N+1查询
5. **错误处理**: 统一使用 `BusinessException` 和 `ErrorCode`

---

**文档版本**: v1.0  
**创建时间**: 2025-01-20  
**作者**: Kiro AI  
**状态**: 待实现
