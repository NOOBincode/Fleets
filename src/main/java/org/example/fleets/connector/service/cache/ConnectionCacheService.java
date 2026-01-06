package org.example.fleets.connector.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 连接缓存服务
 */
@Slf4j
@Service
public class ConnectionCacheService {
    
    @Autowired
    private RedisService redisService;
    
    private static final String USER_ONLINE_KEY = "user:online:";
    private static final String USER_SESSION_KEY = "user:session:";
    private static final long ONLINE_EXPIRE_TIME = 5; // 5分钟
    
    /**
     * 设置用户在线状态
     */
    public void setUserOnline(Long userId, String sessionId) {
        // TODO: 实现设置用户在线状态
        String key = USER_ONLINE_KEY + userId;
        redisService.set(key, sessionId, ONLINE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        // TODO: 实现检查用户是否在线
        String key = USER_ONLINE_KEY + userId;
        return redisService.hasKey(key);
    }
    
    /**
     * 设置用户离线
     */
    public void setUserOffline(Long userId) {
        // TODO: 实现设置用户离线
        String key = USER_ONLINE_KEY + userId;
        redisService.delete(key);
    }
}
