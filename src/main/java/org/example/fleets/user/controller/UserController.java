package org.example.fleets.user.controller;

import org.example.fleets.common.util.PageResult;
import org.example.fleets.user.model.dto.*;
import org.example.fleets.user.model.vo.UserLoginVO;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<UserVO> register(@RequestBody UserRegisterDTO registerDTO) {
        UserVO userVO = userService.register(registerDTO);
        return ResponseEntity.ok(userVO);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<UserLoginVO> login(@RequestBody UserLoginDTO loginDTO) {
        UserLoginVO loginVO = userService.login(loginDTO);
        return ResponseEntity.ok(loginVO);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(@RequestParam Long userId) {
        boolean result = userService.logout(userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserVO> getUserInfo(@PathVariable Long userId) {
        UserVO userVO = userService.getUserInfo(userId);
        return ResponseEntity.ok(userVO);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Boolean>> updateUserInfo(@RequestBody UserUpdateDTO updateDTO) {
        boolean result = userService.updateUserInfo(updateDTO);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, Boolean>> updatePassword(@RequestBody PasswordUpdateDTO passwordDTO) {
        boolean result = userService.updatePassword(passwordDTO);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 重置密码
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, Boolean>> resetPassword(
            @RequestParam String username,
            @RequestParam String verifyCode,
            @RequestParam String newPassword) {
        boolean result = userService.resetPassword(username, verifyCode, newPassword);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{userId}/status/{status}")
    public ResponseEntity<Map<String, Boolean>> updateStatus(
            @PathVariable Long userId,
            @PathVariable Integer status) {
        boolean result = userService.updateStatus(userId, status);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除用户（软删除）
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Boolean>> deleteUser(@PathVariable Long userId) {
        boolean result = userService.deleteUser(userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check/username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsernameExist(@PathVariable String username) {
        boolean exists = userService.checkUsernameExist(username);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * 检查手机号是否存在
     */
    @GetMapping("/check/phone/{phone}")
    public ResponseEntity<Map<String, Boolean>> checkPhoneExist(@PathVariable String phone) {
        boolean exists = userService.checkPhoneExist(phone);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * 检查邮箱是否存在
     */
    @GetMapping("/check/email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailExist(@PathVariable String email) {
        boolean exists = userService.checkEmailExist(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam Long userId,
            @RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(userId, file);
        Map<String, String> response = new HashMap<>();
        response.put("avatarUrl", avatarUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Boolean>> sendVerifyCode(
            @RequestParam String target,
            @RequestParam Integer type) {
        boolean result = userService.sendVerifyCode(target, type);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 分页查询用户列表
     */
    @PostMapping("/list")
    public ResponseEntity<PageResult<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestBody(required = false) UserQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new UserQueryDTO();
        }
        PageResult<UserVO> pageResult = userService.getUserList(queryDTO ,pageSize, pageNum);
        return ResponseEntity.ok(pageResult);
    }
}
