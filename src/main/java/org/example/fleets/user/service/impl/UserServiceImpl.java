package org.example.fleets.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.user.converter.UserConverter;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.dto.*;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.model.vo.UserLoginVO;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.UserService;
import org.example.fleets.user.service.cache.UserCacheService;
import org.example.fleets.user.validator.UserValidator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserCacheService userCacheService;
    private final RedisService redisService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserConverter userConverter;  // 注入MapStruct转换器
    
    // Redis Key前缀
    private static final String REGISTER_LOCK_PREFIX = "register:lock:";
    private static final String VERIFY_CODE_PREFIX = "verify:code:";
    private static final int LOCK_EXPIRE_SECONDS = 10;

    /**
     * 用户注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(UserRegisterDTO registerDTO) {
        log.info("用户注册开始，username: {}", registerDTO.getUsername());
        
        UserValidator.validateRegister(registerDTO);
        
        String lockKey = REGISTER_LOCK_PREFIX + registerDTO.getUsername();
        Boolean locked = redisService.setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        
        if (locked == null || !locked) {
            log.warn("用户注册失败，获取分布式锁失败，username: {}", registerDTO.getUsername());
            throw new BusinessException(ErrorCode.FAILED, "注册请求过于频繁，请稍后再试");
        }
        
        try {
            checkUniqueness(registerDTO);
            
            if (StringUtils.hasText(registerDTO.getVerifyCode())) {
                validateVerifyCode(registerDTO);
            }
            
            // 使用MapStruct转换
            User user = userConverter.toEntity(registerDTO);
            // 单独设置密码（需要加密）
            String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());
            user.setPassword(encodedPassword);
            
            int insertResult = userMapper.insert(user);
            if (insertResult <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "用户注册失败");
            }
            
            if (StringUtils.hasText(registerDTO.getVerifyCode())) {
                redisService.delete(getVerifyCodeKey(registerDTO));
            }
            
            log.info("用户注册成功，userId: {}, username: {}", user.getId(), user.getUsername());
            return userConverter.toVO(user);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户注册异常，username: {}", registerDTO.getUsername(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，请稍后重试");
        } finally {
            redisService.delete(lockKey);
        }
    }

    /**
     * 用户登录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO login(UserLoginDTO loginDTO) {
        log.info("用户登录开始，username: {}", loginDTO.getUsername());
        
        try {
            // 1. 查询用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, loginDTO.getUsername());
            User user = userMapper.selectOne(wrapper);
            
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户名或密码错误");
            }
            
            // 2. 校验密码
            if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
                log.warn("用户登录失败，密码错误，username: {}", loginDTO.getUsername());
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户名或密码错误");
            }
            
            // 3. 校验用户状态
            if (user.getStatus() != 1) {
                throw new BusinessException(ErrorCode.USER_DISABLED, "账号已被禁用");
            }
            
            // 4. 使用 Sa-Token 登录
            StpUtil.login(user.getId());
            String token = StpUtil.getTokenValue();
            Long expireTime = System.currentTimeMillis() + (StpUtil.getTokenTimeout() * 1000);
            
            // 5. 更新登录信息
            user.setLastLoginTime(new Date());
            userMapper.updateById(user);
            
            log.info("用户登录成功，userId: {}, username: {}", user.getId(), user.getUsername());
            return userConverter.toLoginVO(user, token, expireTime);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户登录异常，username: {}", loginDTO.getUsername(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，请稍后重试");
        }
    }

    /**
     * 用户登出
     */
    @Override
    public boolean logout(Long userId) {
        log.info("用户登出，userId: {}", userId);
        
        try {
            // 使用 Sa-Token 登出
            StpUtil.logout(userId);
            
            // 清理用户缓存
            userCacheService.deleteUserCache(userId);
            
            log.info("用户登出成功，userId: {}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("用户登出异常，userId: {}", userId, e);
            return false;
        }
    }

    /**
     * 获取用户信息
     */
    @Override
    public UserVO getUserInfo(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "用户ID不能为空");
        }
        
        try {
            // 先查缓存
            UserVO cachedUser = userCacheService.getUserFromCache(userId);
            if (cachedUser != null) {
                return cachedUser;
            }
            
            // 查数据库
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            
            UserVO userVO = userConverter.toVO(user);
            
            // 写入缓存
            userCacheService.cacheUser(userVO);
            
            return userVO;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户信息异常，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取用户信息失败");
        }
    }

    /**
     * 更新用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserInfo(UserUpdateDTO updateDTO) {
        log.info("更新用户信息，userId: {}", updateDTO.getId());
        
        UserValidator.validateUpdate(updateDTO);
        
        try {
            // 查询用户
            User user = userMapper.selectById(updateDTO.getId());
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            
            // 如果更新手机号或邮箱，需要检查唯一性
            if (StringUtils.hasText(updateDTO.getPhone()) 
                && !updateDTO.getPhone().equals(user.getPhone())) {
                if (checkPhoneExist(updateDTO.getPhone())) {
                    throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "手机号已被使用");
                }
            }
            
            if (StringUtils.hasText(updateDTO.getEmail()) 
                && !updateDTO.getEmail().equals(user.getEmail())) {
                if (checkEmailExist(updateDTO.getEmail())) {
                    throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已被使用");
                }
            }
            
            // 使用MapStruct更新（只更新非null字段）
            userConverter.updateEntity(updateDTO, user);
            
            // 更新数据库
            int updateResult = userMapper.updateById(user);
            if (updateResult <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "更新用户信息失败");
            }
            
            // 清理缓存
            userCacheService.deleteUserCache(user.getId());
            
            log.info("更新用户信息成功，userId: {}", user.getId());
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新用户信息异常，userId: {}", updateDTO.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户信息失败");
        }
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(PasswordUpdateDTO passwordDTO) {
        log.info("修改密码，userId: {}", passwordDTO.getUserId());
        
        UserValidator.validatePasswordUpdate(passwordDTO);
        
        try {
            // 查询用户
            User user = userMapper.selectById(passwordDTO.getUserId());
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            
            // 验证旧密码
            if (!passwordEncoder.matches(passwordDTO.getOldPassword(), user.getPassword())) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "旧密码错误");
            }
            
            // 更新密码
            String encodedPassword = passwordEncoder.encode(passwordDTO.getNewPassword());
            user.setPassword(encodedPassword);
            user.setUpdateTime(new Date());
            
            int updateResult = userMapper.updateById(user);
            if (updateResult <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "修改密码失败");
            }
            
            // 清理Token，强制重新登录
            StpUtil.logout(user.getId());
            
            log.info("修改密码成功，userId: {}", user.getId());
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改密码异常，userId: {}", passwordDTO.getUserId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改密码失败");
        }
    }

    /**
     * 重置密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String username, String verifyCode, String newPassword) {
        log.info("重置密码，username: {}", username);
        
        if (!StringUtils.hasText(username) || !StringUtils.hasText(verifyCode) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        if (newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "密码长度不能少于6位");
        }
        
        try {
            // 查询用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(wrapper);
            
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            
            // 验证验证码
            String codeKey = VERIFY_CODE_PREFIX + (StringUtils.hasText(user.getPhone()) ? user.getPhone() : user.getEmail());
            String cachedCode = redisService.getString(codeKey);
            
            if (!StringUtils.hasText(cachedCode)) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "验证码已过期");
            }
            
            if (!cachedCode.equalsIgnoreCase(verifyCode)) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "验证码错误");
            }
            
            // 更新密码
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            user.setUpdateTime(new Date());
            
            int updateResult = userMapper.updateById(user);
            if (updateResult <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "重置密码失败");
            }
            
            // 清理验证码和Token
            redisService.delete(codeKey);
            StpUtil.logout(user.getId());
            
            log.info("重置密码成功，userId: {}", user.getId());
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("重置密码异常，username: {}", username, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "重置密码失败");
        }
    }

    /**
     * 更新用户状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long userId, Integer status) {
        log.info("更新用户状态，userId: {}, status: {}", userId, status);
        
        if (userId == null || status == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "状态值无效");
        }
        
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            
            user.setStatus(status);
            user.setUpdateTime(new Date());
            
            int updateResult = userMapper.updateById(user);
            if (updateResult <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "更新用户状态失败");
            }
            
            // 如果禁用用户，清理Token
            if (status == 0) {
                StpUtil.logout(userId);
            }
            
            userCacheService.deleteUserCache(userId);
            
            log.info("更新用户状态成功，userId: {}", userId);
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新用户状态异常，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户状态失败");
        }
    }

    /**
     * 删除用户（软删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        log.info("删除用户，userId: {}", userId);
        
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "用户ID不能为空");
        }
        
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            
            // MyBatis-Plus的逻辑删除
            int deleteResult = userMapper.deleteById(userId);
            if (deleteResult <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "删除用户失败");
            }
            
            // 清理相关缓存和Token
            StpUtil.logout(userId);
            userCacheService.deleteUserCache(userId);
            
            log.info("删除用户成功，userId: {}", userId);
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除用户异常，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除用户失败");
        }
    }

    /**
     * 检查用户名是否存在
     */
    @Override
    public boolean checkUsernameExist(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        
        try {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            wrapper.select(User::getId);
            wrapper.last("LIMIT 1");
            
            Long count = userMapper.selectCount(wrapper);
            return count != null && count > 0;
            
        } catch (Exception e) {
            log.error("检查用户名是否存在异常，username: {}", username, e);
            return true;
        }
    }

    /**
     * 检查手机号是否存在
     */
    @Override
    public boolean checkPhoneExist(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        
        try {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, phone);
            wrapper.select(User::getId);
            wrapper.last("LIMIT 1");
            
            Long count = userMapper.selectCount(wrapper);
            return count != null && count > 0;
            
        } catch (Exception e) {
            log.error("检查手机号是否存在异常，phone: {}", phone, e);
            return true;
        }
    }

    /**
     * 检查邮箱是否存在
     */
    @Override
    public boolean checkEmailExist(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        
        try {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, email);
            wrapper.select(User::getId);
            wrapper.last("LIMIT 1");
            
            Long count = userMapper.selectCount(wrapper);
            return count != null && count > 0;
            
        } catch (Exception e) {
            log.error("检查邮箱是否存在异常，email: {}", email, e);
            return true;
        }
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        // TODO: 实现上传头像逻辑（需要文件服务支持）
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "功能暂未实现");
    }

    @Override
    public boolean sendVerifyCode(String target, Integer type) {
        // TODO: 实现发送验证码逻辑（需要短信/邮件服务支持）
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "功能暂未实现");
    }

    @Override
    public PageResult<UserVO> getUserList(UserQueryDTO queryDTO, Integer pageNum, Integer pageSize) {
        log.info("查询用户列表，pageNum: {}, pageSize: {}", pageNum, pageSize);
        
        // 参数校验
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100; // 限制最大每页数量
        }
        
        try {
            // 构建查询条件
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            
            // 用户名模糊查询
            if (StringUtils.hasText(queryDTO.getUsername())) {
                wrapper.like(User::getUsername, queryDTO.getUsername());
            }
            
            // 昵称模糊查询
            if (StringUtils.hasText(queryDTO.getNickname())) {
                wrapper.like(User::getNickname, queryDTO.getNickname());
            }
            
            // 手机号精确查询
            if (StringUtils.hasText(queryDTO.getPhone())) {
                wrapper.eq(User::getPhone, queryDTO.getPhone());
            }
            
            // 邮箱精确查询
            if (StringUtils.hasText(queryDTO.getEmail())) {
                wrapper.eq(User::getEmail, queryDTO.getEmail());
            }
            
            // 状态查询
            if (queryDTO.getStatus() != null) {
                wrapper.eq(User::getStatus, queryDTO.getStatus());
            }
            
            // 按创建时间倒序
            wrapper.orderByDesc(User::getCreateTime);
            
            // 分页查询
            Page<User> page = new Page<>(pageNum, pageSize);
            Page<User> resultPage = userMapper.selectPage(page, wrapper);
            
            // 使用MapStruct批量转换
            List<UserVO> userVOList = userConverter.toVOList(resultPage.getRecords());
            
            return PageResult.of(
                resultPage.getTotal(),
                userVOList,
                pageNum,
                pageSize
            );
            
        } catch (Exception e) {
            log.error("查询用户列表异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询用户列表失败");
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 唯一性校验
     */
    private void checkUniqueness(UserRegisterDTO registerDTO) {
        if (checkUsernameExist(registerDTO.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        
        if (StringUtils.hasText(registerDTO.getPhone()) && checkPhoneExist(registerDTO.getPhone())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "手机号已被注册");
        }
        
        if (StringUtils.hasText(registerDTO.getEmail()) && checkEmailExist(registerDTO.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已被注册");
        }
    }
    
    /**
     * 验证码校验
     */
    private void validateVerifyCode(UserRegisterDTO registerDTO) {
        String codeKey = getVerifyCodeKey(registerDTO);
        String cachedCode = redisService.getString(codeKey);
        
        if (!StringUtils.hasText(cachedCode)) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "验证码已过期，请重新获取");
        }
        
        if (!cachedCode.equalsIgnoreCase(registerDTO.getVerifyCode())) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "验证码错误");
        }
    }
    
    /**
     * 获取验证码缓存Key
     */
    private String getVerifyCodeKey(UserRegisterDTO registerDTO) {
        String target = StringUtils.hasText(registerDTO.getPhone()) 
            ? registerDTO.getPhone() 
            : registerDTO.getEmail();
        return VERIFY_CODE_PREFIX + target;
    }
}

