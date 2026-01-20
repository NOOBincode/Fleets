# 代码迁移检查清单

## 🎯 迁移目标

将项目中的自定义工具类替换为成熟的开源库，提升代码质量和可维护性。

---

## ✅ 已完成的工作

### 1. 依赖管理
- [x] 添加 Hutool 依赖
- [x] 添加 MapStruct 依赖
- [x] 移除 JWT 相关依赖（保留 Sa-Token）

### 2. 文件删除
- [x] 删除 `OfflineMessageService.java`
- [x] 删除 `SyncService.java`
- [x] 删除 `Assert.java`
- [x] 删除 `SnowflakeIdGenerator.java`
- [x] 删除 `SnowflakeIdGeneratorTest.java`
- [x] 删除 `JwtUtils.java`
- [x] 删除 `JwtAuthenticationFilter.java`

### 3. 新增文件
- [x] 创建 `IdGeneratorConfig.java` - Hutool雪花算法配置
- [x] 创建 `SaTokenConfig.java` - Sa-Token配置
- [x] 更新 `UserConverter.java` - 改用MapStruct
- [x] 创建 `FriendshipConverter.java` - MapStruct转换器
- [x] 创建 `MessageConverter.java` - MapStruct转换器

### 4. 文件简化
- [x] 简化 `PageResult.java` - 使用Lombok

---

## 📝 需要手动修改的代码

### 1. Assert 替换（高优先级）🔥

**搜索关键字**：`import org.example.fleets.common.util.Assert`

**需要修改的文件**：
```bash
# 使用以下命令查找所有使用Assert的文件
grep -r "org.example.fleets.common.util.Assert" src/main/java/
```

**修改示例**：
```java
// 旧代码
import org.example.fleets.common.util.Assert;
Assert.notNull(user, ErrorCode.USER_NOT_FOUND);

// 新代码
import org.springframework.util.Assert;
Assert.notNull(user, "用户不存在");
```

---

### 2. SnowflakeIdGenerator 替换（高优先级）🔥

**搜索关键字**：`SnowflakeIdGenerator`

**需要修改的文件**：
```bash
# 查找所有使用SnowflakeIdGenerator的文件
grep -r "SnowflakeIdGenerator" src/main/java/
```

**修改示例**：
```java
// 旧代码
private SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);
long id = idGenerator.nextId();

// 新代码
@Autowired
private Snowflake snowflake;

long id = snowflake.nextId();
```

---

### 3. JWT 替换为 Sa-Token（高优先级）🔥

**搜索关键字**：`JwtUtils`, `jwtUtils`

**需要修改的文件**：
```bash
# 查找所有使用JwtUtils的文件
grep -r "JwtUtils\|jwtUtils" src/main/java/
```

**修改示例**：

#### 用户登录
```java
// 旧代码
String token = jwtUtils.generateToken(user.getId());
Long expireTime = System.currentTimeMillis() + jwtExpiration * 1000;

// 新代码
import cn.dev33.satoken.stp.StpUtil;

StpUtil.login(user.getId());
String token = StpUtil.getTokenValue();
Long expireTime = System.currentTimeMillis() + StpUtil.getTokenTimeout() * 1000;
```

#### 获取当前用户
```java
// 旧代码
String token = request.getHeader("Authorization");
Long userId = jwtUtils.getUserIdFromToken(token);

// 新代码
import cn.dev33.satoken.stp.StpUtil;

Long userId = StpUtil.getLoginIdAsLong();
```

#### 用户登出
```java
// 旧代码
// JWT通常不需要登出，只是删除客户端token

// 新代码
import cn.dev33.satoken.stp.StpUtil;

StpUtil.logout();
```

---

### 4. 对象转换改用 MapStruct（中优先级）⚠️

**搜索关键字**：`BeanUtils.copyProperties`, `UserConverter.toVO`

**需要修改的文件**：
```bash
# 查找所有使用BeanUtils的文件
grep -r "BeanUtils.copyProperties" src/main/java/
```

**修改示例**：
```java
// 旧代码
UserVO vo = new UserVO();
BeanUtils.copyProperties(user, vo);

// 新代码
@Autowired
private UserConverter userConverter;

UserVO vo = userConverter.toVO(user);
```

---

### 5. 静态方法调用改为注入（中优先级）⚠️

**搜索关键字**：`UserConverter.toVO`, `UserConverter.toEntity`

**修改示例**：
```java
// 旧代码
UserVO vo = UserConverter.toVO(user);

// 新代码
@Autowired
private UserConverter userConverter;

UserVO vo = userConverter.toVO(user);
```

---

## 🔍 查找命令

### Windows PowerShell
```powershell
# 查找Assert使用
Select-String -Path "src\main\java\**\*.java" -Pattern "org.example.fleets.common.util.Assert"

# 查找SnowflakeIdGenerator使用
Select-String -Path "src\main\java\**\*.java" -Pattern "SnowflakeIdGenerator"

# 查找JwtUtils使用
Select-String -Path "src\main\java\**\*.java" -Pattern "JwtUtils|jwtUtils"

# 查找BeanUtils使用
Select-String -Path "src\main\java\**\*.java" -Pattern "BeanUtils.copyProperties"
```

### Linux/Mac
```bash
# 查找Assert使用
grep -r "org.example.fleets.common.util.Assert" src/main/java/

# 查找SnowflakeIdGenerator使用
grep -r "SnowflakeIdGenerator" src/main/java/

# 查找JwtUtils使用
grep -r "JwtUtils\|jwtUtils" src/main/java/

# 查找BeanUtils使用
grep -r "BeanUtils.copyProperties" src/main/java/
```

---

## 📋 模块迁移清单

### User 模块
- [ ] UserService - 替换JWT为Sa-Token
- [ ] UserController - 替换JWT为Sa-Token
- [ ] UserConverter - 已改为MapStruct接口
- [ ] 其他使用Assert的地方

### Friendship 模块
- [ ] FriendshipService - 检查Assert使用
- [ ] FriendshipController - 检查JWT使用
- [ ] FriendshipConverter - 已创建MapStruct接口

### Message 模块
- [ ] MessageService - 检查ID生成、Assert使用
- [ ] MessageController - 检查JWT使用
- [ ] MessageConverter - 已创建MapStruct接口

### Group 模块
- [ ] GroupService - 检查Assert使用
- [ ] GroupController - 检查JWT使用

### File 模块
- [ ] FileService - 检查ID生成
- [ ] FileController - 检查JWT使用

### Mailbox 模块
- [ ] MailboxService - 检查Assert使用
- [ ] MailboxController - 检查JWT使用

---

## 🧪 测试清单

### 单元测试
- [ ] 测试 Snowflake ID生成
- [ ] 测试 Sa-Token 登录/登出
- [ ] 测试 MapStruct 对象转换
- [ ] 测试 Spring Assert 异常抛出

### 集成测试
- [ ] 测试用户注册登录流程
- [ ] 测试好友添加流程
- [ ] 测试消息发送流程
- [ ] 测试文件上传流程

---

## 📦 编译和部署

### 1. 清理旧的编译文件
```bash
mvn clean
```

### 2. 重新编译（MapStruct会生成代码）
```bash
mvn compile
```

### 3. 运行测试
```bash
mvn test
```

### 4. 打包
```bash
mvn package
```

---

## ⚠️ 常见问题

### Q1: MapStruct 转换器找不到实现类
**A**: 需要重新编译项目，MapStruct 会在编译时生成实现类。
```bash
mvn clean compile
```

### Q2: Sa-Token 配置不生效
**A**: 检查 `application.yml` 中是否正确配置了 `sa-token` 相关参数。

### Q3: Snowflake ID 重复
**A**: 检查 `workerId` 和 `datacenterId` 配置，确保不同实例使用不同的ID。

### Q4: 编译时 Lombok 和 MapStruct 冲突
**A**: 确保 `mapstruct-processor` 的 scope 是 `provided`，并且在 `maven-compiler-plugin` 中正确配置。

---

## 📞 需要帮助？

如果在迁移过程中遇到问题，可以：
1. 查看 `docs/REFACTORING_GUIDE.md` 详细使用指南
2. 查看各个库的官方文档
3. 检查编译错误信息

---

**最后更新**：2025-01-18
