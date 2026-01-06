package org.example.fleets.user.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 添加好友DTO
 */
@Data
public class FriendAddDTO {
    
    @NotNull(message = "好友ID不能为空")
    private Long friendId;
    
    @Size(max = 50, message = "备注长度不能超过50")
    private String remark;
    
    @Size(max = 20, message = "分组名称长度不能超过20")
    private String groupName;
    
    @Size(max = 100, message = "验证消息长度不能超过100")
    private String verifyMessage;
}
