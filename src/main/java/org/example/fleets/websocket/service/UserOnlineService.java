package org.example.fleets.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.common.config.properties.FleetsProperties;
import org.redisson.api.RSet;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户在线状态服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOnlineService {
    
    private final RedisService redisService;
    private final FleetsProperties fleetsProperties;
    
    /**
     * 用户上线
     * TODO: 实现用户上线逻辑
     * 提示：使用 Redis 存储在线状态和会话信息
     */
    public void userOnline(Long userId, String sessionId) {
        // TODO: 标记用户在线
        // TODO: 存储会话信息
        // TODO: 支持多端登录（一个用户多个会话）
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        FleetsProperties.WebSocketConfig webSocketConfig = fleetsProperties.getWebsocket();
        String onlineKey = redisConfig.getOnlineKeyPrefix() + userId;
        redisService.set(onlineKey, "1", webSocketConfig.getOnlineExpireSeconds(),TimeUnit.SECONDS);
        String sessionKey = redisConfig.getSessionKeyPrefix() + sessionId;
        redisService.set(sessionKey, userId);
        String sessionsKey = redisConfig.getUserSessionsKeyPrefix() + userId;
        RSet<String> sessionsSet = redisService.getSet(sessionsKey);
        sessionsSet.add(sessionId);
        redisService.expire(sessionsKey, webSocketConfig.getOnlineExpireSeconds(),TimeUnit.SECONDS);
        
        log.debug("用户上线: userId={}, sessionId={}", userId, sessionId);
    }
    
    /**
     * 用户离线
     * TODO: 实现用户离线逻辑
     */
    public void userOffline(Long userId, String sessionId) {
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        FleetsProperties.WebSocketConfig webSocketConfig = fleetsProperties.getWebsocket();
        String onlineKey = redisConfig.getOnlineKeyPrefix() + userId;
        String sessionKey = redisConfig.getSessionKeyPrefix() + sessionId;
        String sessionsKey = redisConfig.getUserSessionsKeyPrefix() + userId;

        redisService.delete(sessionKey);
        RSet<String> sessionsSet = redisService.getSet(sessionsKey);
        sessionsSet.remove(sessionId);

        if (sessionsSet.isEmpty()) {
            redisService.delete(sessionsKey);
            redisService.delete(onlineKey);
        } else {
            redisService.expire(sessionsKey, webSocketConfig.getOnlineExpireSeconds(), TimeUnit.SECONDS);
        }
        log.info("用户离线: userId={}, sessionId={}", userId, sessionId);
    }
    
    /**
     * 检查用户是否在线
     * TODO: 实现在线状态检查
     */
    public boolean isOnline(Long userId) {
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        String onlineKey = redisConfig.getOnlineKeyPrefix() + userId;
        return redisService.hasKey(onlineKey);
    }
    
    /**
     * 获取用户的所有会话 ID
     */
    public Set<String> getUserSessions(Long userId) {
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        String sessionsKey = redisConfig.getUserSessionsKeyPrefix() + userId;
        RSet<String> sessionsSet = redisService.getSet(sessionsKey);
        // Redisson 的 RSet 实现了 Set 接口，readAll 返回一个常规 Set
        return sessionsSet.readAll();
    }
    
    /**
     * 根据 sessionId 从 Redis 反查 userId（断线时若 Principal 为空可用）
     */
    public Long getUserIdBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        String sessionKey = fleetsProperties.getRedis().getSessionKeyPrefix() + sessionId;
        Object val = redisService.get(sessionKey);
        if (val == null) {
            return null;
        }
        if (val instanceof Long) {
            return (Long) val;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 刷新用户在线状态（心跳）
     */
    public void refreshOnlineStatus(Long userId) {
        // TODO: 刷新 Redis 中的过期时间
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        FleetsProperties.WebSocketConfig webSocketConfig = fleetsProperties.getWebsocket();
        String onlineKey = redisConfig.getOnlineKeyPrefix() + userId;
        redisService.expire(onlineKey, webSocketConfig.getOnlineExpireSeconds(),TimeUnit.SECONDS);
    }
}
