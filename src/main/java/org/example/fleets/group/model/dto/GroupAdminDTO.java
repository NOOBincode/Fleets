package org.example.fleets.group.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class GroupAdminDTO {
    
    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;
    
    @NotNull(message = "是否设为管理员不能为空")
    private Boolean isAdmin;
}
