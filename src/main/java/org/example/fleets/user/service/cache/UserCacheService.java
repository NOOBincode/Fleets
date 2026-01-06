package org.example.fleets.user.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.user.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户缓存服务
 */
@Slf4j
@Service
public class UserCacheService {
    
    @Autowired
    private RedisService redisService;
    
    private static final String USER_CACHE_KEY = "user:info:";
    private static final String USER_TOKEN_KEY = "user:token:";
    private static final long CACHE_EXPIRE_TIME = 1; // 1小时
    
    /**
     * 缓存用户信息
     */
    public void cacheUser(User user) {
        // TODO: 实现缓存用户信息
        String key = USER_CACHE_KEY + user.getId();
        redisService.set(key, user, CACHE_EXPIRE_TIME, TimeUnit.HOURS);
    }
    
    /**
     * 获取缓存的用户信息
     */
    public User getCachedUser(Long userId) {
        // TODO: 实现获取缓存的用户信息
        String key = USER_CACHE_KEY + userId;
        return (User) redisService.get(key);
    }
    
    /**
     * 删除用户缓存
     */
    public void deleteUserCache(Long userId) {
        // TODO: 实现删除用户缓存
        String key = USER_CACHE_KEY + userId;
        redisService.delete(key);
    }
    
    /**
     * 缓存用户Token
     */
    public void cacheUserToken(Long userId, String token) {
        // TODO: 实现缓存用户Token
        String key = USER_TOKEN_KEY + userId;
        redisService.set(key, token, CACHE_EXPIRE_TIME, TimeUnit.HOURS);
    }
    
    /**
     * 获取用户Token
     */
    public String getUserToken(Long userId) {
        // TODO: 实现获取用户Token
        String key = USER_TOKEN_KEY + userId;
        return (String) redisService.get(key);
    }
    
    /**
     * 删除用户Token（登出）
     */
    public void deleteUserToken(Long userId) {
        // TODO: 实现删除用户Token
        String key = USER_TOKEN_KEY + userId;
        redisService.delete(key);
    }
}
