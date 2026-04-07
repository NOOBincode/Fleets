package org.example.fleets.user.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.cache.GenericCacheService;
import org.redisson.api.RSet;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipCacheService {
    
    private final GenericCacheService genericCacheService;
    
    private static final String FRIEND_LIST_KEY = "friend:list:";
    private static final String FRIEND_RELATION_KEY = "friend:relation:";
    private static final long CACHE_EXPIRE_TIME = 30;
    
    public void cacheFriendList(Long userId, List<Long> friendIds) {
        if (userId == null || friendIds == null) {
            return;
        }
        try {
            String key = FRIEND_LIST_KEY + userId;
            if (!friendIds.isEmpty()) {
                RSet<Long> set = genericCacheService.getSet(key);
                set.addAll(friendIds);
                set.expire(java.time.Duration.ofMinutes(CACHE_EXPIRE_TIME));
            }
            log.debug("缓存好友列表成功，userId: {}, count: {}", userId, friendIds.size());
        } catch (Exception e) {
            log.error("缓存好友列表失败，userId: {}", userId, e);
        }
    }
    
    public List<Long> getCachedFriendList(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            String key = FRIEND_LIST_KEY + userId;
            RSet<Long> set = genericCacheService.getSet(key);
            if (set.isEmpty()) {
                return null;
            }
            List<Long> friendIds = new ArrayList<>(set);
            log.debug("获取缓存的好友列表成功，userId: {}, count: {}", userId, friendIds.size());
            return friendIds;
        } catch (Exception e) {
            log.error("获取缓存的好友列表失败，userId: {}", userId, e);
            return null;
        }
    }
    
    public void deleteFriendListCache(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            String key = FRIEND_LIST_KEY + userId;
            genericCacheService.delete(key);
            log.debug("删除好友列表缓存成功，userId: {}", userId);
        } catch (Exception e) {
            log.error("删除好友列表缓存失败，userId: {}", userId, e);
        }
    }
    
    public void cacheFriendRelation(Long userId, Long friendId, boolean isFriend) {
        if (userId == null || friendId == null) {
            return;
        }
        try {
            String key = FRIEND_RELATION_KEY + userId + ":" + friendId;
            genericCacheService.set(key, isFriend, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            log.debug("缓存好友关系成功，userId: {}, friendId: {}, isFriend: {}", 
                userId, friendId, isFriend);
        } catch (Exception e) {
            log.error("缓存好友关系失败，userId: {}, friendId: {}", userId, friendId, e);
        }
    }
    
    public Boolean getFriendRelation(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return null;
        }
        try {
            String key = FRIEND_RELATION_KEY + userId + ":" + friendId;
            Boolean value = genericCacheService.get(key);
            return value;
        } catch (Exception e) {
            log.error("获取好友关系失败，userId: {}, friendId: {}", userId, friendId, e);
            return null;
        }
    }
    
    public void deleteFriendRelationCache(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return;
        }
        try {
            String key = FRIEND_RELATION_KEY + userId + ":" + friendId;
            genericCacheService.delete(key);
            log.debug("删除好友关系缓存成功，userId: {}, friendId: {}", userId, friendId);
        } catch (Exception e) {
            log.error("删除好友关系缓存失败，userId: {}, friendId: {}", userId, friendId, e);
        }
    }
    
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
