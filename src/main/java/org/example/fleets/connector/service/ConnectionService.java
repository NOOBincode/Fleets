package org.example.fleets.connector.service;

/**
 * 连接管理服务接口
 */
public interface ConnectionService {
    
    /**
     * 用户上线
     */
    void userOnline(Long userId, String sessionId, String deviceId);
    
    /**
     * 用户下线
     */
    void userOffline(Long userId, String sessionId);
    
    /**
     * 检查用户是否在线
     */
    boolean isUserOnline(Long userId);
    
    /**
     * 获取用户的所有会话ID
     */
    String[] getUserSessions(Long userId);
    
    /**
     * 推送消息到用户
     */
    void pushToUser(Long userId, Object message);
    
    /**
     * 推送消息到群组
     */
    void pushToGroup(Long groupId, Object message);
    
    /**
     * 广播消息
     */
    void broadcast(Object message);
}
