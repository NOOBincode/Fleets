package org.example.fleets.user.model.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 用户更新DTO
 */
@Data
public class UserUpdateDTO {
    
    private Long id;
    
    @Size(max = 20, message = "昵称长度不能超过20")
    private String nickname;
    
    private String avatar;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private Integer gender;  // 0-未知，1-男，2-女
    
    private Date birthDate;
    
    @Size(max = 100, message = "个性签名长度不能超过100")
    private String signature;
}
