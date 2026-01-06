package org.example.fleets.user.model.dto;

import lombok.Data;

/**
 * 用户查询DTO
 */
@Data
public class UserQueryDTO {
    
    private String username;
    
    private String nickname;
    
    private String phone;
    
    private String email;
    
    private Integer status;  // 0-禁用，1-正常
}
