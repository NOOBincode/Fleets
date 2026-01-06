package org.example.fleets.message.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.message.model.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消息缓存服务
 */
@Slf4j
@Service
public class MessageCacheService {
    
    @Autowired
    private RedisService redisService;
    
    private static final String MESSAGE_CACHE_KEY = "message:";
    private static final String UNREAD_COUNT_KEY = "message:unread:";
    private static final String CONVERSATION_KEY = "conversation:";
    private static final long CACHE_EXPIRE_TIME = 10; // 10分钟
    
    /**
     * 缓存消息
     */
    public void cacheMessage(Message message) {
        // TODO: 实现缓存消息
        String key = MESSAGE_CACHE_KEY + message.getId();
        redisService.set(key, message, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 获取缓存的消息
     */
    public Message getCachedMessage(String messageId) {
        // TODO: 实现获取缓存的消息
        String key = MESSAGE_CACHE_KEY + messageId;
        return (Message) redisService.get(key);
    }
    
    /**
     * 缓存未读消息数
     */
    public void cacheUnreadCount(Long userId, Long count) {
        // TODO: 实现缓存未读消息数
        String key = UNREAD_COUNT_KEY + userId;
        redisService.set(key, count, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 获取未读消息数
     */
    public Long getUnreadCount(Long userId) {
        // TODO: 实现获取未读消息数
        String key = UNREAD_COUNT_KEY + userId;
        Object count = redisService.get(key);
        return count != null ? (Long) count : 0L;
    }
    
    /**
     * 增加未读消息数
     */
    public void incrementUnreadCount(Long userId) {
        // TODO: 实现增加未读消息数
        String key = UNREAD_COUNT_KEY + userId;
        redisService.increment(key, 1);
    }
    
    /**
     * 清空未读消息数
     */
    public void clearUnreadCount(Long userId) {
        // TODO: 实现清空未读消息数
        String key = UNREAD_COUNT_KEY + userId;
        redisService.delete(key);
    }
    
    /**
     * 缓存会话最新消息
     */
    public void cacheConversationLastMessage(Long userId, Long targetId, Message message) {
        // TODO: 实现缓存会话最新消息
        String key = CONVERSATION_KEY + userId + ":" + targetId;
        redisService.set(key, message, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
}
