package org.example.fleets.connector.model;

import lombok.Data;

/**
 * WebSocket连接请求
 */
@Data
public class ConnectRequest {
    private String token;
    private String deviceId;
    private String platform;
}
