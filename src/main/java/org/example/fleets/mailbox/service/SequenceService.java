package org.example.fleets.mailbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 序列号生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SequenceService {
    
    private final RedisService redisService;
    
    private static final String SEQUENCE_KEY_PREFIX = "mailbox:seq:";
    private static final long SEQUENCE_EXPIRE_DAYS = 7;
    
    /**
     * 生成单个序列号
     */
    public Long generateSequence(Long userId, String conversationId) {
        String key = SEQUENCE_KEY_PREFIX + userId + ":" + conversationId;
        Long sequence = redisService.increment(key);
        
        // 设置过期时间（首次生成时）
        if (sequence == 1) {
            redisService.expire(key, SEQUENCE_EXPIRE_DAYS, TimeUnit.DAYS);
        }
        
        log.debug("生成序列号，userId: {}, conversationId: {}, sequence: {}", 
            userId, conversationId, sequence);
        
        return sequence;
    }
    
    /**
     * 批量生成序列号（群聊场景）
     */
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
    
    /**
     * 获取当前序列号（不递增）
     */
    public Long getCurrentSequence(Long userId, String conversationId) {
        String key = SEQUENCE_KEY_PREFIX + userId + ":" + conversationId;
        Object value = redisService.get(key);
        
        if (value == null) {
            return 0L;
        }
        
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        
        return 0L;
    }
}
