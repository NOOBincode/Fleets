package org.example.fleets.user.service;

import org.example.fleets.common.util.PageResult;
import org.example.fleets.user.model.dto.*;
import org.example.fleets.user.model.vo.UserLoginVO;
import org.example.fleets.user.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    // 用户注册
    UserVO register(UserRegisterDTO registerDTO);

    // 用户登录
    UserLoginVO login(UserLoginDTO loginDTO);

    // 用户登出
    boolean logout(Long userId);

    // 获取用户信息
    UserVO getUserInfo(Long userId);

    // 更新用户信息
    boolean updateUserInfo(UserUpdateDTO updateDTO);

    // 修改密码
    boolean updatePassword(PasswordUpdateDTO passwordDTO);

    // 重置密码
    boolean resetPassword(String username, String verifyCode, String newPassword);

    // 更新用户状态
    boolean updateStatus(Long userId, Integer status);

    // 软删除用户
    boolean deleteUser(Long userId);

    // 检查用户名是否存在
    boolean checkUsernameExist(String username);

    // 检查手机号是否存在
    boolean checkPhoneExist(String phone);

    // 检查邮箱是否存在
    boolean checkEmailExist(String email);

    // 上传头像
    String uploadAvatar(Long userId, MultipartFile file);

    // 发送验证码
    boolean sendVerifyCode(String target, Integer type);

    // 分页查询用户列表
    PageResult<UserVO> getUserList(UserQueryDTO queryDTO, Integer pageNum, Integer pageSize);
}