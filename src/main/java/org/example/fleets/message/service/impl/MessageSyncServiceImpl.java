package org.example.fleets.message.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.connector.service.ConnectionService;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.repository.MessageRepository;
import org.example.fleets.message.service.MessageSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息同步服务实现类
 */
@Slf4j
@Service
public class MessageSyncServiceImpl implements MessageSyncService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private ConnectionService connectionService;
    
    private static final String LAST_SEQUENCE_KEY = "user:last_sequence:";
    private static final String UNREAD_COUNT_KEY = "user:unread_count:";
    
    @Override
    public void syncMessagesOnLogin(Long userId) {
        log.info("用户上线，开始同步消息: userId={}", userId);
        
        // TODO: 实现用户上线消息同步逻辑
        // 1. 获取用户最后同步的序列号
        // 2. 查询所有未同步的消息（sequence > lastSequence）
        // 3. 推送给用户
        // 4. 更新最后同步序列号
    }
    
    @Override
    public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence, Integer limit) {
        log.info("拉取离线消息: userId={}, lastSequence={}, limit={}", userId, lastSequence, limit);
        
        // TODO: 实现离线消息拉取逻辑
        // 1. 查询 MongoDB，获取 sequence > lastSequence 的消息
        // 2. 限制返回数量（分页）
        // 3. 转换为 VO
        // 4. 返回消息列表
        
        return null;
    }
    
    @Override
    public Long getLastSequence(Long userId) {
        // TODO: 实现获取最后序列号
        // 1. 从 Redis 查询用户的最后序列号
        // 2. 如果不存在，从 MongoDB 查询最新消息的序列号
        // 3. 返回序列号
        
        String key = LAST_SEQUENCE_KEY + userId;
        Object sequence = redisService.get(key);
        return sequence != null ? (Long) sequence : 0L;
    }
    
    @Override
    public void updateLastSequence(Long userId, Long sequence) {
        log.debug("更新最后序列号: userId={}, sequence={}", userId, sequence);
        
        // TODO: 实现更新最后序列号
        // 1. 更新 Redis 中的最后序列号
        // 2. 可选：持久化到 MongoDB
        
        String key = LAST_SEQUENCE_KEY + userId;
        redisService.set(key, sequence);
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        // TODO: 实现获取未读消息数
        // 1. 从 Redis 查询未读消息数
        // 2. 如果不存在，从 MongoDB 统计
        // 3. 返回未读数
        
        String key = UNREAD_COUNT_KEY + userId;
        Object count = redisService.get(key);
        return count != null ? (Long) count : 0L;
    }
}
