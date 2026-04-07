package org.example.fleets.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.example.fleets.common.api.CommonResult;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.user.model.dto.*;
import org.example.fleets.user.model.vo.UserLoginVO;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * 用户控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Validated
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public CommonResult<UserVO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        UserVO userVO = userService.register(registerDTO);
        return CommonResult.success(userVO);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public CommonResult<UserLoginVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        UserLoginVO loginVO = userService.login(loginDTO);
        return CommonResult.success(loginVO);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public CommonResult<Boolean> logout() {
        // 幂等：未登录也视为"已登出"，直接成功返回，避免前端在 token 已失效时登出触发异常链
        if (!StpUtil.isLogin()) {
            return CommonResult.success(true, "已登出");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = userService.logout(userId);
        return CommonResult.success(result, "已登出");
    }

    /**
     * 刷新Token过期时间
     */
    @PostMapping("/refresh-token")
    public CommonResult<Long> refreshToken() {
        Long expireTime = userService.refreshToken();
        return CommonResult.success(expireTime);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public CommonResult<UserVO> getUserInfo(@PathVariable Long userId) {
        UserVO userVO = userService.getUserInfo(userId);
        return CommonResult.success(userVO);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public CommonResult<Boolean> updateUserInfo(@Valid @RequestBody UserUpdateDTO updateDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        updateDTO.setId(userId);
        boolean result = userService.updateUserInfo(updateDTO);
        return CommonResult.success(result);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public CommonResult<Boolean> updatePassword(@Valid @RequestBody PasswordUpdateDTO passwordDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        passwordDTO.setUserId(userId);
        boolean result = userService.updatePassword(passwordDTO);
        return CommonResult.success(result);
    }

    /**
     * 重置密码
     */
    @PostMapping("/password/reset")
    public CommonResult<Boolean> resetPassword(
            @RequestParam String username,
            @RequestParam String verifyCode,
            @RequestParam String newPassword) {
        boolean result = userService.resetPassword(username, verifyCode, newPassword);
        return CommonResult.success(result);
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{userId}/status/{status}")
    public CommonResult<Boolean> updateStatus(
            @PathVariable Long userId,
            @PathVariable Integer status) {
        boolean result = userService.updateStatus(userId, status);
        return CommonResult.success(result);
    }

    /**
     * 删除用户（软删除）
     */
    @DeleteMapping("/{userId}")
    public CommonResult<Boolean> deleteUser(@PathVariable Long userId) {
        boolean result = userService.deleteUser(userId);
        return CommonResult.success(result);
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check/username/{username}")
    public CommonResult<Boolean> checkUsernameExist(@PathVariable String username) {
        boolean exists = userService.checkUsernameExist(username);
        return CommonResult.success(exists);
    }

    /**
     * 检查手机号是否存在
     */
    @GetMapping("/check/phone/{phone}")
    public CommonResult<Boolean> checkPhoneExist(@PathVariable String phone) {
        boolean exists = userService.checkPhoneExist(phone);
        return CommonResult.success(exists);
    }

    /**
     * 检查邮箱是否存在
     */
    @GetMapping("/check/email/{email}")
    public CommonResult<Boolean> checkEmailExist(@PathVariable String email) {
        boolean exists = userService.checkEmailExist(email);
        return CommonResult.success(exists);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public CommonResult<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        String avatarUrl = userService.uploadAvatar(userId, file);
        return CommonResult.success(avatarUrl);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/verify-code")
    public CommonResult<Boolean> sendVerifyCode(
            @RequestParam String target,
            @RequestParam Integer type) {
        boolean result = userService.sendVerifyCode(target, type);
        return CommonResult.success(result);
    }

    /**
     * 分页查询用户列表
     */
    @PostMapping("/list")
    public CommonResult<PageResult<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestBody(required = false) UserQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new UserQueryDTO();
        }
        PageResult<UserVO> pageResult = userService.getUserList(queryDTO, pageNum, pageSize);
        return CommonResult.success(pageResult);
    }
}
