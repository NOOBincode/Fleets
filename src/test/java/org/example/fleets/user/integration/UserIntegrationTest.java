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

import static org.assertj.core.api.Assertions.*;

/**
 * 用户模块集成测试
 * 
 * 测试策略：
 * 1. 使用真实的Spring容器
 * 2. 连接真实的数据库和Redis
 * 3. 测试完整的业务流程
 * 4. 按顺序执行测试（模拟真实用户操作）
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("用户模块集成测试")
class UserIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    private static String testUsername;
    private static Long testUserId;
    private static String testToken;
    
    @Test
    @Order(1)
    @DisplayName("集成测试 - 用户注册流程")
    void testUserRegistrationFlow() {
        // Given - 准备注册数据
        testUsername = "integrationtest_" + System.currentTimeMillis();
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername(testUsername);
        registerDTO.setPassword("Test@123456");
        registerDTO.setNickname("Integration Test User");
        registerDTO.setPhone("13800138000");
        registerDTO.setEmail("test@example.com");
        
        // When - 执行注册
        UserVO userVO = userService.register(registerDTO);
        
        // Then - 验证注册结果
        assertThat(userVO).isNotNull();
        assertThat(userVO.getUsername()).isEqualTo(testUsername);
        assertThat(userVO.getNickname()).isEqualTo("Integration Test User");
        assertThat(userVO.getPhone()).isEqualTo("13800138000");
        assertThat(userVO.getId()).isNotNull();
        
        // 保存用户ID供后续测试使用
        testUserId = userVO.getId();
        
        System.out.println("✅ 用户注册成功，userId: " + testUserId);
    }
    
    @Test
    @Order(2)
    @DisplayName("集成测试 - 用户登录流程")
    void testUserLoginFlow() {
        // Given - 准备登录数据
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername(testUsername);
        loginDTO.setPassword("Test@123456");
        
        // When - 执行登录
        UserLoginVO loginVO = userService.login(loginDTO);
        
        // Then - 验证登录结果
        assertThat(loginVO).isNotNull();
        assertThat(loginVO.getToken()).isNotBlank();
        assertThat(loginVO.getExpireTime()).isGreaterThan(System.currentTimeMillis());
        assertThat(loginVO.getUsername()).isEqualTo(testUsername);
        assertThat(loginVO.getUserId()).isNotNull();
        
        // 保存Token供后续测试使用
        testToken = loginVO.getToken();
        
        System.out.println("✅ 用户登录成功，token: " + testToken.substring(0, 20) + "...");
    }
    
    @Test
    @Order(3)
    @DisplayName("集成测试 - 获取用户信息")
    void testGetUserInfoFlow() {
        // When - 获取用户信息
        UserVO userVO = userService.getUserInfo(testUserId);
        
        // Then - 验证用户信息
        assertThat(userVO).isNotNull();
        assertThat(userVO.getId()).isEqualTo(testUserId);
        assertThat(userVO.getUsername()).isEqualTo(testUsername);
        assertThat(userVO.getNickname()).isEqualTo("Integration Test User");
        
        System.out.println("✅ 获取用户信息成功");
    }
    
    @Test
    @Order(4)
    @DisplayName("集成测试 - 检查用户名是否存在")
    void testCheckUsernameExistFlow() {
        // When - 检查已存在的用户名
        boolean exists = userService.checkUsernameExist(testUsername);
        
        // Then - 应该返回true
        assertThat(exists).isTrue();
        
        // When - 检查不存在的用户名
        boolean notExists = userService.checkUsernameExist("nonexistent_user_12345");
        
        // Then - 应该返回false
        assertThat(notExists).isFalse();
        
        System.out.println("✅ 用户名检查功能正常");
    }
    
    @Test
    @Order(5)
    @DisplayName("集成测试 - 用户登出流程")
    void testUserLogoutFlow() {
        // When - 执行登出
        boolean result = userService.logout(testUserId);
        
        // Then - 验证登出结果
        assertThat(result).isTrue();
        
        System.out.println("✅ 用户登出成功");
    }
    
    @Test
    @Order(6)
    @DisplayName("集成测试 - 重复注册应该失败")
    void testDuplicateRegistrationShouldFail() {
        // Given - 使用相同的用户名注册
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername(testUsername);
        registerDTO.setPassword("AnotherPassword@123");
        registerDTO.setNickname("Another User");
        
        // When & Then - 应该抛出异常
        assertThatThrownBy(() -> userService.register(registerDTO))
            .hasMessageContaining("用户名已存在");
        
        System.out.println("✅ 重复注册检查正常");
    }
    
    @Test
    @Order(7)
    @DisplayName("集成测试 - 错误密码登录应该失败")
    void testLoginWithWrongPasswordShouldFail() {
        // Given - 使用错误的密码
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername(testUsername);
        loginDTO.setPassword("WrongPassword@123");
        
        // When & Then - 应该抛出异常
        assertThatThrownBy(() -> userService.login(loginDTO))
            .hasMessageContaining("用户名或密码错误");
        
        System.out.println("✅ 错误密码检查正常");
    }
    
    @AfterAll
    static void cleanup(@Autowired UserService userService) {
        // 清理测试数据
        if (testUserId != null) {
            try {
                userService.deleteUser(testUserId);
                System.out.println("✅ 测试数据清理完成");
            } catch (Exception e) {
                System.err.println("⚠️ 清理测试数据失败: " + e.getMessage());
            }
        }
    }
}
