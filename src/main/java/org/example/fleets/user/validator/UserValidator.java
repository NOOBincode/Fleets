package org.example.fleets.user.validator;

import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.user.model.dto.PasswordUpdateDTO;
import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.dto.UserUpdateDTO;
import org.springframework.util.StringUtils;

/**
 * 用户业务校验器
 * 负责业务层面的参数校验（DTO注解校验之外的额外校验）
 */
public class UserValidator {
    
    /**
     * 校验注册参数
     */
    public static void validateRegister(UserRegisterDTO dto) {
        String username = dto.getUsername();
        if (username.length() < 3 || username.length() > 20) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED.getCode(), "用户名长度必须在3-20之间");
        }
        
        String password = dto.getPassword();
        if (password.length() < 6) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED.getCode(), "密码长度不能少于6位");
        }
        
        if (!StringUtils.hasText(dto.getNickname())) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED.getCode(), "昵称不能为空");
        }
    }
    
    /**
     * 校验更新参数
     */
    public static void validateUpdate(UserUpdateDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED.getCode(), "用户ID不能为空");
        }
        
        // 至少要更新一个字段
        if (!StringUtils.hasText(dto.getNickname()) 
            && !StringUtils.hasText(dto.getAvatar())
            && !StringUtils.hasText(dto.getPhone())
            && !StringUtils.hasText(dto.getEmail())
            && dto.getGender() == null
            && dto.getBirthDate() == null
            && !StringUtils.hasText(dto.getSignature())) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED.getCode(), "至少需要更新一个字段");
        }
    }
    
    /**
     * 校验密码修改参数
     */
    public static void validatePasswordUpdate(PasswordUpdateDTO dto) {
        if (dto.getUserId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED.getCode(), "用户ID不能为空");
        }
        
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED.getCode(), "新密码不能与旧密码相同");
        }
        
        if (dto.getNewPassword().length() < 6) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED.getCode(), "新密码长度不能少于6位");
        }
    }
}
