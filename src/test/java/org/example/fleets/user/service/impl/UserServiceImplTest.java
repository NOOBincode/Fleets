package org.example.fleets.user.service.impl;

import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.dto.UserLoginDTO;
import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.model.vo.UserLoginVO;
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

/**
 * 用户服务单元测试
 * 
 * 测试策略：
 * 1. 使用Mockito模拟依赖
 * 2. 测试正常场景和异常场景
 * 3. 验证方法调用次数和参数
 */
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
    private User mockUser;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
        registerDTO.setNickname("Test User");
        registerDTO.setPhone("13800138000");
        
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPassword("encoded_password");
        mockUser.setNickname("Test User");
        mockUser.setStatus(1);
    }
    
    // ==================== 用户注册测试 ====================
    
    @Test
    @DisplayName("用户注册 - 成功场景")
    void testRegister_Success() {
        // Given
        when(redisService.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });
        
        // When
        UserVO result = userService.register(registerDTO);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getNickname()).isEqualTo("Test User");
        
        // 验证方法调用
        verify(userMapper, times(1)).insert(any(User.class));
        verify(redisService, times(1)).delete(anyString());
        verify(passwordEncoder, times(1)).encode("password123");
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
        
        // 验证不应该插入数据
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
        
        // 验证不应该查询数据库
        verify(userMapper, never()).selectCount(any());
    }
    
    @Test
    @DisplayName("用户注册 - 手机号已存在")
    void testRegister_PhoneExists() {
        // Given
        when(redisService.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
        when(userMapper.selectCount(any()))
            .thenReturn(0L)  // 用户名不存在
            .thenReturn(1L); // 手机号已存在
        
        // When & Then
        assertThatThrownBy(() -> userService.register(registerDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("手机号已被注册");
    }
    
    // ==================== 用户登录测试 ====================
    
    @Test
    @DisplayName("用户登录 - 成功场景")
    void testLogin_Success() {
        // Given
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");
        
        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        
        // When
        UserLoginVO result = userService.login(loginDTO);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getUsername()).isEqualTo("testuser");
        
        // 验证更新了登录时间
        verify(userMapper, times(1)).updateById(any(User.class));
    }
    
    @Test
    @DisplayName("用户登录 - 用户不存在")
    void testLogin_UserNotFound() {
        // Given
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("nonexistent");
        loginDTO.setPassword("password123");
        
        when(userMapper.selectOne(any())).thenReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> userService.login(loginDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("用户名或密码错误");
    }
    
    @Test
    @DisplayName("用户登录 - 密码错误")
    void testLogin_WrongPassword() {
        // Given
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("wrongpassword");
        
        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> userService.login(loginDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("用户名或密码错误");
    }
    
    @Test
    @DisplayName("用户登录 - 账号已禁用")
    void testLogin_UserDisabled() {
        // Given
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");
        
        mockUser.setStatus(0); // 禁用状态
        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.login(loginDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("账号已被禁用");
    }
    
    // ==================== 获取用户信息测试 ====================
    
    @Test
    @DisplayName("获取用户信息 - 从缓存获取")
    void testGetUserInfo_FromCache() {
        // Given
        UserVO cachedUser = new UserVO();
        cachedUser.setId(1L);
        cachedUser.setUsername("testuser");
        
        when(userCacheService.getUserFromCache(1L)).thenReturn(cachedUser);
        
        // When
        UserVO result = userService.getUserInfo(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        
        // 验证没有查询数据库
        verify(userMapper, never()).selectById(any());
    }
    
    @Test
    @DisplayName("获取用户信息 - 从数据库获取")
    void testGetUserInfo_FromDatabase() {
        // Given
        when(userCacheService.getUserFromCache(1L)).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(mockUser);
        
        // When
        UserVO result = userService.getUserInfo(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        
        // 验证写入了缓存
        verify(userCacheService, times(1)).cacheUser(any(UserVO.class));
    }
    
    @Test
    @DisplayName("获取用户信息 - 用户不存在")
    void testGetUserInfo_NotFound() {
        // Given
        when(userCacheService.getUserFromCache(1L)).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> userService.getUserInfo(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("用户不存在");
    }
    
    // ==================== 检查用户名是否存在测试 ====================
    
    @Test
    @DisplayName("检查用户名 - 已存在")
    void testCheckUsernameExist_Exists() {
        // Given
        when(userMapper.selectCount(any())).thenReturn(1L);
        
        // When
        boolean result = userService.checkUsernameExist("testuser");
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("检查用户名 - 不存在")
    void testCheckUsernameExist_NotExists() {
        // Given
        when(userMapper.selectCount(any())).thenReturn(0L);
        
        // When
        boolean result = userService.checkUsernameExist("testuser");
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("检查用户名 - 空字符串")
    void testCheckUsernameExist_EmptyString() {
        // When
        boolean result = userService.checkUsernameExist("");
        
        // Then
        assertThat(result).isFalse();
        
        // 验证没有查询数据库
        verify(userMapper, never()).selectCount(any());
    }
}
