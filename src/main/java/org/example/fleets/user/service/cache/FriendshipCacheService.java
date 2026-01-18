package org.example.fleets.user.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 好友关系缓存服务
 * 
 * 缓存策略：
 * 1. 好友列表缓存：存储用户的好友ID列表，30分钟过期
 * 2. 好友关系缓存：存储两个用户之间的好友关系，30分钟过期
 * 3. 缓存一致性：增删改操作时主动删除缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipCacheService {
    
    private final RedisService redisService;
    
    private static final String FRIEND_LIST_KEY = "friend:list:";
    private static final String FRIEND_RELATION_KEY = "friend:relation:";
    private static final long CACHE_EXPIRE_TIME = 30; // 30分钟
    
    /**
     * 缓存好友列表
     * 存储格式：Set<Long> 好友ID集合
     */
    public void cacheFriendList(Long userId, List<Long> friendIds) {
        if (userId == null || friendIds == null) {
            return;
        }
        
        try {
            String key = FRIEND_LIST_KEY + userId;
            
            // 使用Redisson的RSet存储好友ID列表
            if (!friendIds.isEmpty()) {
                org.redisson.api.RSet<Long> set = redisService.getSet(key);
                set.addAll(friendIds);
                set.expire(java.time.Duration.ofMinutes(CACHE_EXPIRE_TIME));
            }
            
            log.debug("缓存好友列表成功，userId: {}, count: {}", userId, friendIds.size());
            
        } catch (Exception e) {
            log.error("缓存好友列表失败，userId: {}", userId, e);
        }
    }
    
    /**
     * 获取缓存的好友列表
     * 返回好友ID集合
     */
    public List<Long> getCachedFriendList(Long userId) {
        if (userId == null) {
            return null;
        }
        
        try {
            String key = FRIEND_LIST_KEY + userId;
            
            // 从Redisson的RSet获取好友ID列表
            org.redisson.api.RSet<Long> set = redisService.getSet(key);
            
            if (set.isEmpty()) {
                return null;
            }
            
            List<Long> friendIds = new java.util.ArrayList<>(set);
            
            log.debug("获取缓存的好友列表成功，userId: {}, count: {}", userId, friendIds.size());
            return friendIds;
            
        } catch (Exception e) {
            log.error("获取缓存的好友列表失败，userId: {}", userId, e);
            return null;
        }
    }
    
    /**
     * 删除好友列表缓存
     * 在添加、删除好友时调用
     */
    public void deleteFriendListCache(Long userId) {
        if (userId == null) {
            return;
        }
        
        try {
            String key = FRIEND_LIST_KEY + userId;
            redisService.delete(key);
            
            log.debug("删除好友列表缓存成功，userId: {}", userId);
            
        } catch (Exception e) {
            log.error("删除好友列表缓存失败，userId: {}", userId, e);
        }
    }
    
    /**
     * 缓存好友关系
     * 存储格式：String "true" 或 "false"
     */
    public void cacheFriendRelation(Long userId, Long friendId, boolean isFriend) {
        if (userId == null || friendId == null) {
            return;
        }
        
        try {
            String key = FRIEND_RELATION_KEY + userId + ":" + friendId;
            redisService.set(key, isFriend, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            
            log.debug("缓存好友关系成功，userId: {}, friendId: {}, isFriend: {}", 
                userId, friendId, isFriend);
            
        } catch (Exception e) {
            log.error("缓存好友关系失败，userId: {}, friendId: {}", userId, friendId, e);
        }
    }
    
    /**
     * 获取好友关系
     * 返回是否是好友
     */
    public Boolean getFriendRelation(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return null;
        }
        
        try {
            String key = FRIEND_RELATION_KEY + userId + ":" + friendId;
            Object value = redisService.get(key);
            
            if (value == null) {
                return null;
            }
            
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("获取好友关系失败，userId: {}, friendId: {}", userId, friendId, e);
            return null;
        }
    }
    
    /**
     * 删除好友关系缓存
     * 在添加、删除、拉黑好友时调用
     */
    public void deleteFriendRelationCache(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return;
        }
        
        try {
            String key = FRIEND_RELATION_KEY + userId + ":" + friendId;
            redisService.delete(key);
            
            log.debug("删除好友关系缓存成功，userId: {}, friendId: {}", userId, friendId);
            
        } catch (Exception e) {
            log.error("删除好友关系缓存失败，userId: {}, friendId: {}", userId, friendId, e);
        }
    }
    
    /**
     * 批量删除好友关系缓存
     * 用于批量操作场景
     */
    public void batchDeleteFriendRelationCache(Long userId, List<Long> friendIds) {
        if (userId == null || friendIds == null || friendIds.isEmpty()) {
            return;
        }
        
        try {
            for (Long friendId : friendIds) {
                deleteFriendRelationCache(userId, friendId);
            }
            
            log.debug("批量删除好友关系缓存成功，userId: {}, count: {}", userId, friendIds.size());
            
        } catch (Exception e) {
            log.error("批量删除好友关系缓存失败，userId: {}", userId, e);
        }
    }
}
