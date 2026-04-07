package org.example.fleets.user.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateFriendRemarkRequest {
    @NotBlank(message = "备注不能为空")
    private String remark;
}

