package org.example.fleets.user.converter;

import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.dto.UserUpdateDTO;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.model.vo.UserLoginVO;
import org.example.fleets.user.model.vo.UserVO;
import org.springframework.beans.BeanUtils;

import java.util.Date;

/**
 * 用户对象转换器
 * 负责 DTO/Entity/VO 之间的转换
 */
public class UserConverter {
    
    /**
     * 注册DTO转Entity
     */
    public static User toEntity(UserRegisterDTO dto, String encodedPassword) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encodedPassword);
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        
        // 设置默认值
        user.setStatus(1);
        user.setAvatar("");
        user.setGender(0);
        user.setSignature("");
        
        Date now = new Date();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        
        return user;
    }
    
    /**
     * Entity转VO（不包含敏感信息）
     */
    public static UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
    
    /**
     * Entity转LoginVO
     */
    public static UserLoginVO toLoginVO(User user, String token, Long expireTime) {
        if (user == null) {
            return null;
        }
        
        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setToken(token);
        vo.setExpireTime(expireTime);
        
        return vo;
    }
    
    /**
     * 更新DTO应用到Entity
     */
    public static void applyUpdate(UserUpdateDTO dto, User user) {
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getBirthDate() != null) {
            user.setBirthDate(dto.getBirthDate());
        }
        if (dto.getSignature() != null) {
            user.setSignature(dto.getSignature());
        }
        
        user.setUpdateTime(new Date());
    }
}
