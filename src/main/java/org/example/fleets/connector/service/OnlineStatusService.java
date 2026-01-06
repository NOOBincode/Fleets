package org.example.fleets.connector.service;

import java.util.List;
import java.util.Map;

/**
 * 在线状态服务接口
 */
public interface OnlineStatusService {
    
    /**
     * 用户上线
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param deviceId 设备ID
     */
    void userOnline(Long userId, String sessionId, String deviceId);
    
    /**
     * 用户下线
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    void userOffline(Long userId, String sessionId);
    
    /**
     * 刷新心跳
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    void heartbeat(Long userId, String sessionId);
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return 是否在线
     */
    boolean isOnline(Long userId);
    
    /**
     * 批量检查用户是否在线
     * 
     * @param userIds 用户ID列表
     * @return userId -> 是否在线的映射
     */
    Map<Long, Boolean> batchCheckOnline(List<Long> userIds);
    
    /**
     * 获取用户的所有在线会话
     * 
     * @param userId 用户ID
     * @return 会话ID列表
     */
    List<String> getUserSessions(Long userId);
    
    /**
     * 获取在线用户数量
     * 
     * @return 在线用户数
     */
    long getOnlineUserCount();
}
