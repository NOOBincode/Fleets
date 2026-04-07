package org.example.fleets.user.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateFriendGroupRequest {
    @NotBlank(message = "分组名不能为空")
    private String groupName;
}

