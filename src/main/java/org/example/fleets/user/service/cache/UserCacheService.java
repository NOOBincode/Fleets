package org.example.fleets.user.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.cache.GenericCacheService;
import org.example.fleets.user.model.vo.UserVO;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {
    
    private final GenericCacheService genericCacheService;
    
    private static final String USER_CACHE_KEY = "user:info:";
    private static final long CACHE_EXPIRE_TIME = 1;
    
    public void cacheUser(UserVO userVO) {
        String key = USER_CACHE_KEY + userVO.getId();
        genericCacheService.set(key, userVO, CACHE_EXPIRE_TIME, TimeUnit.HOURS);
    }
    
    public UserVO getUserFromCache(Long userId) {
        String key = USER_CACHE_KEY + userId;
        return genericCacheService.get(key);
    }
    
    public void deleteUserCache(Long userId) {
        String key = USER_CACHE_KEY + userId;
        genericCacheService.delete(key);
    }
}
