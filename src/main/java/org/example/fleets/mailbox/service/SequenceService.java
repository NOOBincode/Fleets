package org.example.fleets.mailbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.cache.GenericCacheService;
import org.example.fleets.common.config.properties.FleetsProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SequenceService {
    
    private final GenericCacheService genericCacheService;
    private final FleetsProperties fleetsProperties;
    
    public Long generateSequence(Long userId, String conversationId) {
        String keyPrefix = fleetsProperties.getRedis().getSequenceKeyPrefix();
        String key = keyPrefix + userId + ":" + conversationId;
        Long sequence = genericCacheService.increment(key);
        
        if (sequence == 1) {
            int expireDays = fleetsProperties.getRedis().getSequenceExpireDays();
            genericCacheService.expire(key, expireDays, TimeUnit.DAYS);
        }
        
        log.debug("生成序列号，userId: {}, conversationId: {}, sequence: {}", 
            userId, conversationId, sequence);
        
        return sequence;
    }
    
    public Map<Long, Long> batchGenerateSequence(List<Long> userIds, String conversationId) {
        Map<Long, Long> sequenceMap = new HashMap<>();
        
        for (Long userId : userIds) {
            Long sequence = generateSequence(userId, conversationId);
            sequenceMap.put(userId, sequence);
        }
        
        log.debug("批量生成序列号，userCount: {}, conversationId: {}", 
            userIds.size(), conversationId);
        
        return sequenceMap;
    }
    
    public Long getCurrentSequence(Long userId, String conversationId) {
        String keyPrefix = fleetsProperties.getRedis().getSequenceKeyPrefix();
        String key = keyPrefix + userId + ":" + conversationId;
        Long value = genericCacheService.getAtomicLong(key);
        
        return value != null ? value : 0L;
    }
}
