package org.example.fleets.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.fleets.common.util.JwtUtils;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.common.util.PasswordUtils;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.dto.*;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.model.vo.UserLoginVO;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Value("${file.upload.path:/uploads/avatars/}")
    private String uploadPath;
    
    @Value("${file.access.path:/api/file/avatar/}")
    private String accessPath;
    
    // 验证码过期时间（分钟）
    private static final long VERIFY_CODE_EXPIRE = 5;
    
    // 验证码前缀
    private static final String VERIFY_CODE_PREFIX = "verify_code:";
    
    // 用户token前缀
    private static final String USER_TOKEN_PREFIX = "user_token:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(UserRegisterDTO registerDTO) {
        // 1. 检查用户名是否已存在
        if (checkUsernameExist(registerDTO.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 2. 检查手机号是否已存在
        if (StringUtils.hasText(registerDTO.getPhone()) && checkPhoneExist(registerDTO.getPhone())) {
            throw new RuntimeException("手机号已被注册");
        }
        
        // 3. 检查邮箱是否已存在
        if (StringUtils.hasText(registerDTO.getEmail()) && checkEmailExist(registerDTO.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 4. 创建用户实体
        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        
        // 5. 密码加密
        user.setPassword(PasswordUtils.encode(registerDTO.getPassword()));
        
        // 6. 设置默认值
        user.setStatus(1); // 正常状态
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDeleted(0); // 未删除
        
        // 7. 保存用户
        save(user);
        
        // 8. 转换为VO并返回
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
        // 1. 根据用户名查询用户
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername())
                .eq(User::getIsDeleted, 0));
        
        // 2. 用户不存在或已被删除
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 3. 用户被禁用
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        
        // 4. 验证密码
        if (!PasswordUtils.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 5. 更新登录信息
        user.setLastLoginTime(new Date());
        // 这里可以获取请求的IP地址并设置，但需要在Controller层传入
        // user.setLastLoginIp(ip);
        updateById(user);
        
        // 6. 生成JWT令牌
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        
        // 7. 将token存入Redis，设置过期时间与JWT一致
        redisTemplate.opsForValue().set(
                USER_TOKEN_PREFIX + user.getId(), 
                token, 
                jwtUtils.getExpiration(), 
                TimeUnit.SECONDS);
        
        // 8. 构建登录返回VO
        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setToken(token);
        loginVO.setTokenHead(jwtUtils.getTokenHead());
        
        return loginVO;
    }

    @Override
    public boolean logout(Long userId) {
        // 从Redis中删除token
        return redisTemplate.delete(USER_TOKEN_PREFIX + userId);
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        // 1. 查询用户
        User user = getById(userId);
        
        // 2. 用户不存在或已被删除
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        // 3. 转换为VO并返回
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserInfo(UserUpdateDTO updateDTO) {
        // 1. 查询用户是否存在
        User user = getById(updateDTO.getId());
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 检查手机号是否被其他用户使用
        if (StringUtils.hasText(updateDTO.getPhone()) && !updateDTO.getPhone().equals(user.getPhone())) {
            boolean phoneExists = count(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, updateDTO.getPhone())
                    .ne(User::getId, updateDTO.getId())
                    .eq(User::getIsDeleted, 0)) > 0;
            if (phoneExists) {
                throw new RuntimeException("手机号已被其他用户使用");
            }
        }
        
        // 3. 检查邮箱是否被其他用户使用
        if (StringUtils.hasText(updateDTO.getEmail()) && !updateDTO.getEmail().equals(user.getEmail())) {
            boolean emailExists = count(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, updateDTO.getEmail())
                    .ne(User::getId, updateDTO.getId())
                    .eq(User::getIsDeleted, 0)) > 0;
            if (emailExists) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
        }
        
        // 4. 更新用户信息
        User updateUser = new User();
        BeanUtils.copyProperties(updateDTO, updateUser);
        updateUser.setUpdateTime(new Date());
        
        return updateById(updateUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(PasswordUpdateDTO passwordDTO) {
        // 1. 查询用户
        User user = getById(passwordDTO.getUserId());
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 验证旧密码
        if (!PasswordUtils.matches(passwordDTO.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        // 3. 验证新密码与确认密码是否一致
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            throw new RuntimeException("新密码与确认密码不一致");
        }
        
        // 4. 更新密码
        User updateUser = new User();
        updateUser.setId(passwordDTO.getUserId());
        updateUser.setPassword(PasswordUtils.encode(passwordDTO.getNewPassword()));
        updateUser.setUpdateTime(new Date());
        
        // 5. 更新成功后，使当前token失效，强制重新登录
        boolean result = updateById(updateUser);
        if (result) {
            redisTemplate.delete(USER_TOKEN_PREFIX + passwordDTO.getUserId());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String username, String verifyCode, String newPassword) {
        // 1. 查询用户
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0));
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 验证验证码
        String cacheCode = (String) redisTemplate.opsForValue().get(VERIFY_CODE_PREFIX + username);
        if (cacheCode == null || !cacheCode.equals(verifyCode)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        
        // 3. 更新密码
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(PasswordUtils.encode(newPassword));
        updateUser.setUpdateTime(new Date());
        
        // 4. 删除验证码和token
        boolean result = updateById(updateUser);
        if (result) {
            redisTemplate.delete(VERIFY_CODE_PREFIX + username);
            redisTemplate.delete(USER_TOKEN_PREFIX + user.getId());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long userId, Integer status) {
        // 1. 查询用户
        User user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 更新状态
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setStatus(status);
        updateUser.setUpdateTime(new Date());
        
        // 3. 如果禁用用户，则使其token失效
        boolean result = updateById(updateUser);
        if (result && status == 0) {
            redisTemplate.delete(USER_TOKEN_PREFIX + userId);
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        // 1. 查询用户
        User user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 软删除用户
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setIsDeleted(1);
        updateUser.setUpdateTime(new Date());
        
        // 3. 删除token
        boolean result = updateById(updateUser);
        if (result) {
            redisTemplate.delete(USER_TOKEN_PREFIX + userId);
        }
        
        return result;
    }

    @Override
    public boolean checkUsernameExist(String username) {
        return count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0)) > 0;
    }

    @Override
    public boolean checkPhoneExist(String phone) {
        return count(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .eq(User::getIsDeleted, 0)) > 0;
    }

    @Override
    public boolean checkEmailExist(String email) {
        return count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .eq(User::getIsDeleted, 0)) > 0;
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        // 1. 查询用户
        User user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (!fileExtension.equalsIgnoreCase(".jpg") && 
            !fileExtension.equalsIgnoreCase(".jpeg") && 
            !fileExtension.equalsIgnoreCase(".png") && 
            !fileExtension.equalsIgnoreCase(".gif")) {
            throw new RuntimeException("只支持jpg、jpeg、png、gif格式的图片");
        }
        
        // 3. 生成文件名
        String fileName = UUID.randomUUID().toString().replace("-", "") + fileExtension;
        
        // 4. 确保上传目录存在
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // 5. 保存文件
        try {
            File destFile = new File(uploadPath + fileName);
            file.transferTo(destFile);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
        
        // 6. 更新用户头像
        String avatarUrl = accessPath + fileName;
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setAvatar(avatarUrl);
        updateUser.setUpdateTime(new Date());
        updateById(updateUser);
        
        return avatarUrl;
    }

    @Override
    public boolean sendVerifyCode(String target, Integer type) {
        // 1. 根据类型确定发送方式和查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>().eq(User::getIsDeleted, 0);
        
        // 类型：1-手机号，2-邮箱
        if (type == 1) {
            queryWrapper.eq(User::getPhone, target);
        } else if (type == 2) {
            queryWrapper.eq(User::getEmail, target);
        } else {
            throw new RuntimeException("不支持的验证码类型");
        }
        
        // 2. 查询用户是否存在
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 3. 生成6位随机验证码
        String verifyCode = String.format("%06d", (int)(Math.random() * 1000000));
        
        // 4. 存入Redis，设置过期时间
        redisTemplate.opsForValue().set(
                VERIFY_CODE_PREFIX + user.getUsername(), 
                verifyCode, 
                VERIFY_CODE_EXPIRE, 
                TimeUnit.MINUTES);
        
        // 5. 发送验证码
        // 这里应该调用短信或邮件服务发送验证码
        // 由于是示例代码，这里省略实际发送逻辑
        
        return true;
    }

    @Override
    public PageResult<UserVO> getUserList(UserQueryDTO queryDTO, Integer pageNum, Integer pageSize) {
        // 1. 构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getIsDeleted, 0);
        
        // 2. 添加查询条件
        if (StringUtils.hasText(queryDTO.getUsername())) {
            queryWrapper.like(User::getUsername, queryDTO.getUsername());
        }
        
        if (StringUtils.hasText(queryDTO.getNickname())) {
            queryWrapper.like(User::getNickname, queryDTO.getNickname());
        }
        
        if (StringUtils.hasText(queryDTO.getPhone())) {
            queryWrapper.like(User::getPhone, queryDTO.getPhone());
        }
        
        if (StringUtils.hasText(queryDTO.getEmail())) {
            queryWrapper.like(User::getEmail, queryDTO.getEmail());
        }
        
        if (queryDTO.getGender() != null) {
            queryWrapper.eq(User::getGender, queryDTO.getGender());
        }
        
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(User::getStatus, queryDTO.getStatus());
        }
        
        if (queryDTO.getStartTime() != null) {
            queryWrapper.ge(User::getCreateTime, queryDTO.getStartTime());
        }
        
        if (queryDTO.getEndTime() != null) {
            queryWrapper.le(User::getCreateTime, queryDTO.getEndTime());
        }
        
        // 3. 分页查询
        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<User> userPage = page(page, queryWrapper);
        
        // 4. 转换为VO
        List<UserVO> userVOList = new ArrayList<>();
        for (User user : userPage.getRecords()) {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            userVOList.add(userVO);
        }
        
        // 5. 构建分页结果
        return new PageResult<>(
                userPage.getTotal(), 
                userVOList, 
                pageNum, 
                pageSize);
    }

    @Override
    public boolean save(User entity) {
        return super.save(entity);
    }
}