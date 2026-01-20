# 代码重构指南

## 📋 重构内容总结

本次重构主要优化了项目中重复造轮子的部分，引入了成熟的开源库，提升代码质量和开发效率。

---

## 🔄 主要变更

### 1. 删除的组件

#### ❌ 空的Service类
- `OfflineMessageService.java` - 功能已被 `MailboxService` 覆盖
- `SyncService.java` - 功能已被 `MailboxService` 覆盖

#### ❌ 自定义工具类
- `Assert.java` - 改用 Spring 自带的 `org.springframework.util.Assert`
- `SnowflakeIdGenerator.java` - 改用 Hutool 的 `IdUtil.getSnowflake()`
- `JwtUtils.java` - 改用 Sa-Token
- `JwtAuthenticationFilter.java` - 改用 Sa-Token 拦截器

---

### 2. 新增的依赖

#### ✅ Hutool 工具库
```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.25</version>
</dependency>
```

**用途**：
- ID生成：`IdUtil.getSnowflake()`
- 字符串工具：`StrUtil`
- 日期工具：`DateUtil`
- 集合工具：`CollUtil`
- JSON工具：`JSONUtil`

#### ✅ MapStruct 对象转换
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
```

**用途**：自动生成对象转换代码，替代手动的 BeanUtils.copyProperties()

---

### 3. 简化的组件

#### ✅ PageResult 简化
使用 Lombok 注解简化代码：
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private List<T> records;
    private int pageNum;
    private int pageSize;
    
    public int getTotalPages() {
        return pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
    }
}
```

---

## 📖 使用指南

### 1. 使用 Spring Assert 替代自定义 Assert

**之前**：
```java
import org.example.fleets.common.util.Assert;

Assert.notNull(user, ErrorCode.USER_NOT_FOUND);
Assert.isTrue(condition, ErrorCode.VALIDATE_FAILED);
```

**现在**：
```java
import org.springframework.util.Assert;

Assert.notNull(user, "用户不存在");
Assert.isTrue(condition, "验证失败");
```

---

### 2. 使用 Hutool 生成雪花ID

**之前**：
```java
SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
long id = generator.nextId();
```

**现在**：
```java
// 方式1：注入Bean（推荐）
@Autowired
private Snowflake snowflake;

long id = snowflake.nextId();

// 方式2：直接使用
import cn.hutool.core.util.IdUtil;

long id = IdUtil.getSnowflake(1, 1).nextId();
```

**配置**（application.yml）：
```yaml
snowflake:
  workerId: 1
  datacenterId: 1
```

---

### 3. 使用 Sa-Token 替代 JWT

**之前**：
```java
// 登录
String token = jwtUtils.generateToken(userId);

// 验证
Long userId = jwtUtils.getUserIdFromToken(token);
```

**现在**：
```java
import cn.dev33.satoken.stp.StpUtil;

// 登录
StpUtil.login(userId);
String token = StpUtil.getTokenValue();

// 获取当前登录用户ID
Long userId = StpUtil.getLoginIdAsLong();

// 登出
StpUtil.logout();

// 检查是否登录
boolean isLogin = StpUtil.isLogin();
```

**配置**（application.yml）：
```yaml
sa-token:
  # token名称（同时也是cookie名称）
  token-name: satoken
  # token有效期，单位秒，-1代表永不过期
  timeout: 604800
  # token临时有效期（指定时间内无操作就视为token过期），单位秒
  activity-timeout: -1
  # 是否允许同一账号并发登录（为false时新登录挤掉旧登录）
  is-concurrent: true
  # 在多人登录同一账号时，是否共用一个token（为false时每次登录新建一个token）
  is-share: false
  # token风格
  token-style: uuid
  # 是否输出操作日志
  is-log: false
```

---

### 4. 使用 MapStruct 做对象转换

**之前**：
```java
public static UserVO toVO(User user) {
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(user, vo);
    return vo;
}
```

**现在**：
```java
// 1. 定义转换器接口
@Mapper(componentModel = "spring")
public interface UserConverter {
    UserVO toVO(User user);
    List<UserVO> toVOList(List<User> users);
}

// 2. 在Service中注入使用
@Autowired
private UserConverter userConverter;

public UserVO getUserInfo(Long userId) {
    User user = userMapper.selectById(userId);
    return userConverter.toVO(user);
}
```

**高级用法**：
```java
@Mapper(componentModel = "spring")
public interface UserConverter {
    
    // 自定义映射
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "token", target = "token")
    UserLoginVO toLoginVO(User user, String token, Long expireTime);
    
    // 忽略某些字段
    @Mapping(target = "password", ignore = true)
    User toEntity(UserRegisterDTO dto);
    
    // 更新对象（只更新非null字段）
    @Mapping(target = "id", ignore = true)
    void updateEntity(UserUpdateDTO dto, @MappingTarget User user);
}
```

---

### 5. 使用 Hutool 工具类

#### 字符串工具
```java
import cn.hutool.core.util.StrUtil;

// 判空
boolean isEmpty = StrUtil.isEmpty(str);
boolean isNotEmpty = StrUtil.isNotEmpty(str);

// 格式化
String result = StrUtil.format("Hello {}, age is {}", "Tom", 18);

// 驼峰转下划线
String snake = StrUtil.toUnderlineCase("userName"); // user_name
```

#### 日期工具
```java
import cn.hutool.core.date.DateUtil;

// 格式化
String dateStr = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");

// 解析
Date date = DateUtil.parse("2024-01-01 12:00:00");

// 计算
Date tomorrow = DateUtil.tomorrow();
Date yesterday = DateUtil.yesterday();
```

#### 集合工具
```java
import cn.hutool.core.collection.CollUtil;

// 判空
boolean isEmpty = CollUtil.isEmpty(list);

// 创建
List<String> list = CollUtil.newArrayList("a", "b", "c");
Map<String, String> map = CollUtil.newHashMap();
```

#### JSON工具
```java
import cn.hutool.json.JSONUtil;

// 对象转JSON
String json = JSONUtil.toJsonStr(user);

// JSON转对象
User user = JSONUtil.toBean(json, User.class);

// JSON转List
List<User> users = JSONUtil.toList(json, User.class);
```

---

## 🎯 迁移检查清单

### 必须修改的地方

- [ ] 所有使用 `Assert` 的地方改为 `org.springframework.util.Assert`
- [ ] 所有使用 `SnowflakeIdGenerator` 的地方改为 `Snowflake`（注入Bean）
- [ ] 所有使用 `JwtUtils` 的地方改为 `StpUtil`
- [ ] 所有使用 `JwtAuthenticationFilter` 的地方移除

### 建议修改的地方

- [ ] 手动的对象转换代码改为 MapStruct
- [ ] 字符串、日期、集合操作改用 Hutool 工具类
- [ ] JSON 操作改用 Hutool 的 JSONUtil

---

## 🔧 编译和运行

### 1. 清理并重新编译
```bash
mvn clean compile
```

MapStruct 会在编译时自动生成转换器实现类，位于 `target/generated-sources/annotations/` 目录。

### 2. 运行项目
```bash
mvn spring-boot:run
```

---

## 📚 参考文档

- **Hutool 官方文档**：https://hutool.cn/docs/
- **MapStruct 官方文档**：https://mapstruct.org/
- **Sa-Token 官方文档**：https://sa-token.cc/
- **Spring Assert 文档**：https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/Assert.html

---

## ⚠️ 注意事项

1. **MapStruct 需要重新编译**：修改 Converter 接口后，需要重新编译项目才能生成新的实现类
2. **Sa-Token 配置**：确保 `application.yml` 中配置了 Sa-Token 相关参数
3. **Snowflake 配置**：确保配置了 `workerId` 和 `datacenterId`，避免分布式环境下ID冲突
4. **依赖冲突**：如果遇到依赖冲突，使用 `mvn dependency:tree` 查看依赖树

---

**重构完成时间**：2025-01-18  
**重构人员**：Kiro AI
