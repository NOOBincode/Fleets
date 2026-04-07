package org.example.fleets.message.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdateSequenceRequest {
    @NotNull(message = "sequence不能为空")
    private Long sequence;
}

