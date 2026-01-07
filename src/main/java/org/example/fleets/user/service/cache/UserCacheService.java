package org.example.fleets.user.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.model.vo.UserVO;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户缓存服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {
    
    private final RedisService redisService;
    
    private static final String USER_CACHE_KEY = "user:info:";
    private static final String USER_TOKEN_KEY = "user:token:";
    private static final long CACHE_EXPIRE_TIME = 1; // 1小时
    
    /**
     * 缓存用户信息（Entity）
     */
    public void cacheUser(User user) {
        String key = USER_CACHE_KEY + user.getId();
        redisService.set(key, user, CACHE_EXPIRE_TIME, TimeUnit.HOURS);
    }
    
    /**
     * 缓存用户信息（VO）
     */
    public void cacheUser(UserVO userVO) {
        String key = USER_CACHE_KEY + userVO.getId();
        redisService.set(key, userVO, CACHE_EXPIRE_TIME, TimeUnit.HOURS);
    }
    
    /**
     * 获取缓存的用户信息（返回VO）
     */
    public UserVO getUserFromCache(Long userId) {
        String key = USER_CACHE_KEY + userId;
        Object cached = redisService.get(key);
        if (cached instanceof UserVO) {
            return (UserVO) cached;
        }
        return null;
    }
    
    /**
     * 获取缓存的用户信息（返回Entity）
     */
    public User getCachedUser(Long userId) {
        String key = USER_CACHE_KEY + userId;
        Object cached = redisService.get(key);
        if (cached instanceof User) {
            return (User) cached;
        }
        return null;
    }
    
    /**
     * 删除用户缓存
     */
    public void deleteUserCache(Long userId) {
        String key = USER_CACHE_KEY + userId;
        redisService.delete(key);
    }
    
    /**
     * 缓存用户Token
     */
    public void cacheUserToken(Long userId, String token) {
        String key = USER_TOKEN_KEY + userId;
        redisService.set(key, token, CACHE_EXPIRE_TIME, TimeUnit.HOURS);
    }
    
    /**
     * 获取用户Token
     */
    public String getUserToken(Long userId) {
        String key = USER_TOKEN_KEY + userId;
        return (String) redisService.get(key);
    }
    
    /**
     * 删除用户Token（登出）
     */
    public void deleteUserToken(Long userId) {
        String key = USER_TOKEN_KEY + userId;
        redisService.delete(key);
    }
}
