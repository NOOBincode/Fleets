package org.example.fleets.user.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户VO
 */
@Data
public class UserVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    
    private String username;
    
    private String nickname;
    
    private String avatar;
    
    private String phone;
    
    private String email;
    
    private Integer gender;
    
    private Date birthDate;
    
    private String signature;
    
    private Integer status;
    
    private Date createTime;
    
    private Date lastLoginTime;
}
