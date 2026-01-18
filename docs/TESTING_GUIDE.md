# 测试方案完整指南

## 一、测试体系概览

```
测试金字塔
    ┌─────────────┐
    │  E2E测试    │  5%  - 端到端测试
    ├─────────────┤
    │  集成测试    │  15% - 模块间协作
    ├─────────────┤
    │  单元测试    │  80% - 单个方法
    └─────────────┘
```

### 测试类型与占比

| 测试类型 | 占比 | 工具 | 目的 |
|---------|------|------|------|
| 单元测试 | 80% | JUnit 5 + Mockito | 测试单个方法逻辑 |
| 集成测试 | 15% | Spring Boot Test | 测试模块协作 |
| 接口测试 | 3% | Postman + Newman | 测试API接口 |
| E2E测试 | 2% | Selenium（可选） | 测试完整流程 |

---

## 二、单元测试（Unit Test）

### 2.1 技术栈

```xml
<!-- pom.xml -->
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ（更好的断言） -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2.2 单元测试示例

创建文件：`src/test/java/org/example/fleets/user/service/impl/UserServiceImplTest.java`

```java
package org.example.fleets.user.service.impl;

import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.cache.UserCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceImplTest {
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private UserCacheService userCacheService;
    
    @Mock
    private RedisService redisService;
    
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private UserRegisterDTO registerDTO;
    
    @BeforeEach
    void setUp() {
        registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
        registerDTO.setNickname("Test User");
    }
    
    @Test
    @DisplayName("用户注册 - 成功场景")
    void testRegister_Success() {
        // Given
        when(redisService.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        
        // When
        UserVO result = userService.register(registerDTO);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userMapper, times(1)).insert(any(User.class));
        verify(redisService, times(1)).delete(anyString());
    }
    
    @Test
    @DisplayName("用户注册 - 用户名已存在")
    void testRegister_UsernameExists() {
        // Given
        when(redisService.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
        when(userMapper.selectCount(any())).thenReturn(1L);
        
        // When & Then
        assertThatThrownBy(() -> userService.register(registerDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("用户名已存在");
        
        verify(userMapper, never()).insert(any(User.class));
    }
    
    @Test
    @DisplayName("用户注册 - 获取分布式锁失败")
    void testRegister_LockFailed() {
        // Given
        when(redisService.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> userService.register(registerDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("操作过于频繁");
    }
}
```

### 2.3 测试覆盖率目标

| 模块 | 目标覆盖率 | 说明 |
|-----|-----------|------|
| Service层 | 80%+ | 核心业务逻辑 |
| Controller层 | 60%+ | 接口层 |
| Util工具类 | 90%+ | 工具方法 |
| Entity实体类 | 不要求 | 简单POJO |

---

## 三、集成测试（Integration Test）

### 3.1 Spring Boot集成测试

创建文件：`src/test/java/org/example/fleets/user/integration/UserIntegrationTest.java`

```java
package org.example.fleets.user.integration;

import org.example.fleets.user.model.dto.UserLoginDTO;
import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.vo.UserLoginVO;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("用户模块集成测试")
class UserIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    private static String testUsername;
    private static Long testUserId;
    
    @Test
    @Order(1)
    @DisplayName("集成测试 - 用户注册")
    void testUserRegistration() {
        // Given
        testUsername = "integrationtest_" + System.currentTimeMillis();
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername(testUsername);
        registerDTO.setPassword("Test@123456");
        registerDTO.setNickname("Integration Test User");
        registerDTO.setPhone("13800138000");
        
        // When
        UserVO userVO = userService.register(registerDTO);
        
        // Then
        assertThat(userVO).isNotNull();
        assertThat(userVO.getUsername()).isEqualTo(testUsername);
        assertThat(userVO.getId()).isNotNull();
        
        testUserId = userVO.getId();
    }
    
    @Test
    @Order(2)
    @DisplayName("集成测试 - 用户登录")
    void testUserLogin() {
        // Given
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername(testUsername);
        loginDTO.setPassword("Test@123456");
        
        // When
        UserLoginVO loginVO = userService.login(loginDTO);
        
        // Then
        assertThat(loginVO).isNotNull();
        assertThat(loginVO.getToken()).isNotBlank();
        assertThat(loginVO.getUserInfo().getUsername()).isEqualTo(testUsername);
    }
    
    @Test
    @Order(3)
    @DisplayName("集成测试 - 获取用户信息")
    void testGetUserInfo() {
        // When
        UserVO userVO = userService.getUserInfo(testUserId);
        
        // Then
        assertThat(userVO).isNotNull();
        assertThat(userVO.getUsername()).isEqualTo(testUsername);
    }
    
    @Test
    @Order(4)
    @DisplayName("集成测试 - 用户登出")
    @Transactional
    void testUserLogout() {
        // When
        boolean result = userService.logout(testUserId);
        
        // Then
        assertThat(result).isTrue();
    }
}
```

### 3.2 测试配置文件

创建文件：`src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fleets_im_test?useUnicode=true&characterEncoding=utf8
    username: root
    password: root
    
  data:
    mongodb:
      uri: mongodb://localhost:27017/fleets_im_test
      
  redis:
    host: localhost
    port: 6379
    database: 1  # 使用不同的数据库避免污染
    
logging:
  level:
    org.example.fleets: DEBUG
```

---

## 四、接口测试（API Test）

### 4.1 Postman测试集

已完成：`Fleets_User_API_Tests.postman_collection.json`

### 4.2 Newman自动化测试

```bash
# 安装Newman
npm install -g newman

# 运行测试集
newman run Fleets_User_API_Tests.postman_collection.json \
  --environment test-env.json \
  --reporters cli,html \
  --reporter-html-export test-report.html
```

### 4.3 创建环境变量文件

创建文件：`test-env.json`

```json
{
  "name": "Test Environment",
  "values": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080",
      "enabled": true
    },
    {
      "key": "token",
      "value": "",
      "enabled": true
    }
  ]
}
```

---

## 五、性能测试（Performance Test）

### 5.1 JMeter测试计划

创建文件：`performance-test/user-login-test.jmx`

**测试场景**：
1. 并发用户登录测试
2. 消息发送吞吐量测试
3. WebSocket连接压力测试

### 5.2 JMeter测试脚本示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.comments">用户登录性能测试</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.user_defined_variables">
        <collectionProp name="Arguments.arguments"/>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup>
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <longProp name="ThreadGroup.duration">60</longProp>
      </ThreadGroup>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### 5.3 性能指标

| 指标 | 目标值 | 说明 |
|-----|--------|------|
| 响应时间 | <200ms | 95%请求 |
| 吞吐量 | >1000 TPS | 每秒事务数 |
| 并发连接 | >5000 | WebSocket连接 |
| 错误率 | <1% | 请求失败率 |

---

## 六、测试数据管理

### 6.1 测试数据准备

创建文件：`src/test/resources/test-data.sql`

```sql
-- 测试用户数据
INSERT INTO user (id, username, password, nickname, status) VALUES
(1001, 'testuser1', '$2a$10$...', 'Test User 1', 1),
(1002, 'testuser2', '$2a$10$...', 'Test User 2', 1);

-- 测试好友关系
INSERT INTO friendship (user_id, friend_id, status) VALUES
(1001, 1002, 1),
(1002, 1001, 1);
```

### 6.2 测试数据清理

```java
@AfterEach
void tearDown() {
    // 清理测试数据
    userMapper.deleteById(testUserId);
    redisService.delete("test:*");
}
```

---

## 七、持续集成（CI）

### 7.1 GitHub Actions配置

创建文件：`.github/workflows/test.yml`

```yaml
name: Run Tests

on:
  push:
    branches: [ main, dev ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: fleets_im_test
        ports:
          - 3306:3306
          
      redis:
        image: redis:7
        ports:
          - 6379:6379
          
      mongodb:
        image: mongo:6
        ports:
          - 27017:27017
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Run Unit Tests
      run: mvn test
      
    - name: Run Integration Tests
      run: mvn verify -P integration-test
      
    - name: Generate Test Report
      run: mvn jacoco:report
      
    - name: Upload Coverage
      uses: codecov/codecov-action@v3
```

---

## 八、测试报告

### 8.1 JaCoCo代码覆盖率

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

生成报告：
```bash
mvn clean test jacoco:report
# 报告位置：target/site/jacoco/index.html
```

---

## 九、测试最佳实践

### 9.1 命名规范

```java
// 测试类命名
UserServiceImplTest.java

// 测试方法命名
@Test
@DisplayName("用户注册 - 成功场景")
void testRegister_Success() { }

@Test
@DisplayName("用户注册 - 用户名已存在")
void testRegister_UsernameExists() { }
```

### 9.2 AAA模式

```java
@Test
void testExample() {
    // Arrange（准备）
    UserRegisterDTO dto = new UserRegisterDTO();
    dto.setUsername("test");
    
    // Act（执行）
    UserVO result = userService.register(dto);
    
    // Assert（断言）
    assertThat(result).isNotNull();
}
```

### 9.3 测试隔离

- 每个测试方法独立运行
- 使用@BeforeEach准备数据
- 使用@AfterEach清理数据
- 避免测试间依赖

---

## 十、毕设测试要求

### 10.1 最低要求

✅ **必须完成**：
1. 核心Service的单元测试（用户、好友、消息）
2. 主要接口的集成测试
3. Postman接口测试集
4. 基本的性能测试（JMeter）

### 10.2 加分项

⭐ **可选完成**：
1. 高代码覆盖率（>70%）
2. 自动化测试（CI/CD）
3. 详细的测试报告
4. 性能对比实验

---

**文档版本**: v1.0  
**最后更新**: 2025-01-09  
**作者**: Kiro AI
