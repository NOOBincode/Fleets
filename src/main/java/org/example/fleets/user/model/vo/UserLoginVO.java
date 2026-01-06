package org.example.fleets.user.model.vo;

import lombok.Data;

/**
 * 用户登录VO
 */
@Data
public class UserLoginVO {
    
    private Long userId;
    
    private String username;
    
    private String nickname;
    
    private String avatar;
    
    private String token;
    
    private Long expireTime;  // Token过期时间（时间戳）
}
