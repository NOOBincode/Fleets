package org.example.fleets.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.cache.GenericCacheService;
import org.example.fleets.common.config.properties.FleetsProperties;
import org.redisson.api.RSet;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOnlineService {
    
    private final GenericCacheService genericCacheService;
    private final FleetsProperties fleetsProperties;
    
    public void userOnline(Long userId, String sessionId) {
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        FleetsProperties.WebSocketConfig webSocketConfig = fleetsProperties.getWebsocket();
        String onlineKey = redisConfig.getOnlineKeyPrefix() + userId;
        genericCacheService.set(onlineKey, "1", webSocketConfig.getOnlineExpireSeconds(), TimeUnit.SECONDS);
        String sessionKey = redisConfig.getSessionKeyPrefix() + sessionId;
        genericCacheService.set(sessionKey, userId);
        String sessionsKey = redisConfig.getUserSessionsKeyPrefix() + userId;
        RSet<String> sessionsSet = genericCacheService.getSet(sessionsKey);
        sessionsSet.add(sessionId);
        genericCacheService.expire(sessionsKey, webSocketConfig.getOnlineExpireSeconds(), TimeUnit.SECONDS);
        
        log.debug("用户上线: userId={}, sessionId={}", userId, sessionId);
    }
    
    public void userOffline(Long userId, String sessionId) {
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        FleetsProperties.WebSocketConfig webSocketConfig = fleetsProperties.getWebsocket();
        String onlineKey = redisConfig.getOnlineKeyPrefix() + userId;
        String sessionKey = redisConfig.getSessionKeyPrefix() + sessionId;
        String sessionsKey = redisConfig.getUserSessionsKeyPrefix() + userId;

        genericCacheService.delete(sessionKey);
        RSet<String> sessionsSet = genericCacheService.getSet(sessionsKey);
        sessionsSet.remove(sessionId);

        if (sessionsSet.isEmpty()) {
            genericCacheService.delete(sessionsKey);
            genericCacheService.delete(onlineKey);
        } else {
            genericCacheService.expire(sessionsKey, webSocketConfig.getOnlineExpireSeconds(), TimeUnit.SECONDS);
        }
        log.info("用户离线: userId={}, sessionId={}", userId, sessionId);
    }
    
    public boolean isOnline(Long userId) {
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        String onlineKey = redisConfig.getOnlineKeyPrefix() + userId;
        return genericCacheService.exists(onlineKey);
    }
    
    public Set<String> getUserSessions(Long userId) {
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        String sessionsKey = redisConfig.getUserSessionsKeyPrefix() + userId;
        RSet<String> sessionsSet = genericCacheService.getSet(sessionsKey);
        return sessionsSet.readAll();
    }
    
    public Long getUserIdBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        String sessionKey = fleetsProperties.getRedis().getSessionKeyPrefix() + sessionId;
        Object val = genericCacheService.get(sessionKey);
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

    public void refreshOnlineStatus(Long userId) {
        FleetsProperties.RedisConfig redisConfig = fleetsProperties.getRedis();
        FleetsProperties.WebSocketConfig webSocketConfig = fleetsProperties.getWebsocket();
        String onlineKey = redisConfig.getOnlineKeyPrefix() + userId;
        genericCacheService.expire(onlineKey, webSocketConfig.getOnlineExpireSeconds(), TimeUnit.SECONDS);
    }
}
