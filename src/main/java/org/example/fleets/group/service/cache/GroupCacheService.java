package org.example.fleets.group.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.group.model.entity.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 群组缓存服务
 */
@Slf4j
@Service
public class GroupCacheService {
    
    @Autowired
    private RedisService redisService;
    
    private static final String GROUP_INFO_KEY = "group:info:";
    private static final String GROUP_MEMBER_KEY = "group:members:";
    private static final String USER_GROUP_KEY = "user:groups:";
    private static final long CACHE_EXPIRE_TIME = 30; // 30分钟
    
    /**
     * 缓存群组信息
     */
    public void cacheGroupInfo(Group group) {
        // TODO: 实现缓存群组信息
        String key = GROUP_INFO_KEY + group.getId();
        redisService.set(key, group, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 获取缓存的群组信息
     */
    public Group getCachedGroupInfo(Long groupId) {
        // TODO: 实现获取缓存的群组信息
        String key = GROUP_INFO_KEY + groupId;
        return (Group) redisService.get(key);
    }
    
    /**
     * 删除群组信息缓存
     */
    public void deleteGroupInfoCache(Long groupId) {
        // TODO: 实现删除群组信息缓存
        String key = GROUP_INFO_KEY + groupId;
        redisService.delete(key);
    }
    
    /**
     * 缓存群成员列表
     */
    public void cacheGroupMembers(Long groupId, List<Long> memberIds) {
        // TODO: 实现缓存群成员列表
        String key = GROUP_MEMBER_KEY + groupId;
        redisService.set(key, memberIds, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 获取缓存的群成员列表
     */
    public List<Long> getCachedGroupMembers(Long groupId) {
        // TODO: 实现获取缓存的群成员列表
        String key = GROUP_MEMBER_KEY + groupId;
        return (List<Long>) redisService.get(key);
    }
    
    /**
     * 删除群成员列表缓存
     */
    public void deleteGroupMembersCache(Long groupId) {
        // TODO: 实现删除群成员列表缓存
        String key = GROUP_MEMBER_KEY + groupId;
        redisService.delete(key);
    }
    
    /**
     * 缓存用户的群组列表
     */
    public void cacheUserGroups(Long userId, List<Long> groupIds) {
        // TODO: 实现缓存用户的群组列表
        String key = USER_GROUP_KEY + userId;
        redisService.set(key, groupIds, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 获取用户的群组列表
     */
    public List<Long> getCachedUserGroups(Long userId) {
        // TODO: 实现获取用户的群组列表
        String key = USER_GROUP_KEY + userId;
        return (List<Long>) redisService.get(key);
    }
}
