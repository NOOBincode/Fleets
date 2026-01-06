package org.example.fleets.user.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 好友关系缓存服务
 */
@Slf4j
@Service
public class FriendshipCacheService {
    
    @Autowired
    private RedisService redisService;
    
    private static final String FRIEND_LIST_KEY = "friend:list:";
    private static final String FRIEND_RELATION_KEY = "friend:relation:";
    private static final long CACHE_EXPIRE_TIME = 30; // 30分钟
    
    /**
     * 缓存好友列表
     */
    public void cacheFriendList(Long userId, List<Long> friendIds) {
        // TODO: 实现缓存好友列表
        String key = FRIEND_LIST_KEY + userId;
        redisService.set(key, friendIds, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 获取缓存的好友列表
     */
    public List<Long> getCachedFriendList(Long userId) {
        // TODO: 实现获取缓存的好友列表
        String key = FRIEND_LIST_KEY + userId;
        return (List<Long>) redisService.get(key);
    }
    
    /**
     * 删除好友列表缓存
     */
    public void deleteFriendListCache(Long userId) {
        // TODO: 实现删除好友列表缓存
        String key = FRIEND_LIST_KEY + userId;
        redisService.delete(key);
    }
    
    /**
     * 缓存好友关系
     */
    public void cacheFriendRelation(Long userId, Long friendId, boolean isFriend) {
        // TODO: 实现缓存好友关系
        String key = FRIEND_RELATION_KEY + userId + ":" + friendId;
        redisService.set(key, isFriend, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 获取好友关系
     */
    public Boolean getFriendRelation(Long userId, Long friendId) {
        // TODO: 实现获取好友关系
        String key = FRIEND_RELATION_KEY + userId + ":" + friendId;
        return (Boolean) redisService.get(key);
    }
}
