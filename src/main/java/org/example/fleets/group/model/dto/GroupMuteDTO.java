package org.example.fleets.group.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class GroupMuteDTO {
    
    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;
    
    @NotNull(message = "禁言时长不能为空")
    @Positive(message = "禁言时长必须大于0")
    private Long muteMinutes;
}
