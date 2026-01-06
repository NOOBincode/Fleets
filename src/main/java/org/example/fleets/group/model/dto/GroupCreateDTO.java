package org.example.fleets.group.model.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 创建群组DTO
 */
@Data
public class GroupCreateDTO {
    
    @NotBlank(message = "群组名称不能为空")
    @Size(max = 30, message = "群组名称长度不能超过30")
    private String groupName;
    
    private String avatar;
    
    @Size(max = 200, message = "群简介长度不能超过200")
    private String description;
    
    @Size(max = 500, message = "群公告长度不能超过500")
    private String announcement;
    
    @Min(value = 10, message = "最大成员数不能少于10")
    @Max(value = 500, message = "最大成员数不能超过500")
    private Integer maxMembers = 200;
    
    private Integer joinType = 0;  // 0-无需验证，1-需要验证，2-禁止加群
    
    // 初始成员ID列表
    private List<Long> memberIds;
}
