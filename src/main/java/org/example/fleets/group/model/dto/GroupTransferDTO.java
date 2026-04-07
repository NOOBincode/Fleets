package org.example.fleets.group.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class GroupTransferDTO {
    
    @NotNull(message = "新群主ID不能为空")
    private Long newOwnerId;
}
