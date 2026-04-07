package org.example.fleets.message.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.cache.GenericCacheService;
import org.example.fleets.message.model.entity.Message;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCacheService {
    
    private final GenericCacheService genericCacheService;
    
    private static final String MESSAGE_CACHE_KEY = "message:";
    private static final String UNREAD_COUNT_KEY = "message:unread:";
    private static final String CONVERSATION_KEY = "conversation:";
    private static final long CACHE_EXPIRE_TIME = 10;
    
    public void cacheMessage(Message message) {
        String key = MESSAGE_CACHE_KEY + message.getId();
        genericCacheService.set(key, message, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    public Message getCachedMessage(String messageId) {
        String key = MESSAGE_CACHE_KEY + messageId;
        return genericCacheService.get(key);
    }
    
    public void cacheUnreadCount(Long userId, Long count) {
        String key = UNREAD_COUNT_KEY + userId;
        genericCacheService.set(key, count, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    public Long getUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY + userId;
        Long count = genericCacheService.get(key);
        return count != null ? count : 0L;
    }
    
    public void incrementUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY + userId;
        genericCacheService.increment(key, 1);
    }
    
    public void clearUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY + userId;
        genericCacheService.delete(key);
    }
    
    public void cacheConversationLastMessage(Long userId, Long targetId, Message message) {
        String key = CONVERSATION_KEY + userId + ":" + targetId;
        genericCacheService.set(key, message, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
}
