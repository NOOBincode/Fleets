package org.example.fleets.group.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.cache.GenericCacheService;
import org.example.fleets.group.model.entity.Group;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupCacheService {
    
    private final GenericCacheService genericCacheService;
    
    private static final String GROUP_INFO_KEY = "group:info:";
    private static final String GROUP_MEMBER_KEY = "group:members:";
    private static final String USER_GROUP_KEY = "user:groups:";
    private static final long CACHE_EXPIRE_TIME = 30;
    
    public void cacheGroupInfo(Group group) {
        String key = GROUP_INFO_KEY + group.getId();
        genericCacheService.set(key, group, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    public Group getCachedGroupInfo(Long groupId) {
        String key = GROUP_INFO_KEY + groupId;
        return genericCacheService.get(key);
    }
    
    public void deleteGroupInfoCache(Long groupId) {
        String key = GROUP_INFO_KEY + groupId;
        genericCacheService.delete(key);
    }
    
    public void cacheGroupMembers(Long groupId, List<Long> memberIds) {
        String key = GROUP_MEMBER_KEY + groupId;
        genericCacheService.set(key, memberIds, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    public List<Long> getCachedGroupMembers(Long groupId) {
        String key = GROUP_MEMBER_KEY + groupId;
        return genericCacheService.get(key);
    }
    
    public void deleteGroupMembersCache(Long groupId) {
        String key = GROUP_MEMBER_KEY + groupId;
        genericCacheService.delete(key);
    }
    
    public void cacheUserGroups(Long userId, List<Long> groupIds) {
        String key = USER_GROUP_KEY + userId;
        genericCacheService.set(key, groupIds, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    public List<Long> getCachedUserGroups(Long userId) {
        String key = USER_GROUP_KEY + userId;
        return genericCacheService.get(key);
    }
}
