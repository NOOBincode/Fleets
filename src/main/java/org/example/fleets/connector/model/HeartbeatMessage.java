package org.example.fleets.connector.model;

import lombok.Data;

/**
 * 心跳消息
 */
@Data
public class HeartbeatMessage {
    
    // 消息类型：PING
    private String type = "PING";
    
    // 时间戳
    private Long timestamp;
    
    // 设备ID
    private String deviceId;
}
