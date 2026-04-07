package org.example.fleets.websocket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在线状态变更通知 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineStatusDTO {
    private Long userId;
    private boolean online;
}

