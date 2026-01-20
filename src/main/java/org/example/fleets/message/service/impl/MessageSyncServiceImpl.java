package org.example.fleets.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.repository.MessageRepository;
import org.example.fleets.message.service.MessageSyncService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 消息同步服务实现类
 * 
 * TODO: 实现消息同步相关功能
 * 注意：此功能可能与 Mailbox 模块重复，建议使用 Mailbox 的同步接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSyncServiceImpl implements MessageSyncService {
    
    private final MessageRepository messageRepository;
    private final RedisService redisService;
    
    @Override
    public void syncMessagesOnLogin(Long userId) {
        // TODO: 实现用户上线消息同步逻辑
        // 建议：使用 Mailbox 的 syncMessages 接口
        log.warn("消息同步功能未实现: userId={}", userId);
    }
    
    @Override
    public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence, Integer limit) {
        // TODO: 实现离线消息拉取逻辑
        // 建议：使用 Mailbox 的 syncMessages 接口
        log.warn("离线消息拉取功能未实现: userId={}, lastSequence={}", userId, lastSequence);
        return Collections.emptyList();
    }
    
    @Override
    public Long getLastSequence(Long userId) {
        // TODO: 实现获取最后序列号
        // 建议：使用 Mailbox 的序列号管理
        return 0L;
    }
    
    @Override
    public void updateLastSequence(Long userId, Long sequence) {
        // TODO: 实现更新最后序列号
        log.debug("更新序列号功能未实现: userId={}, sequence={}", userId, sequence);
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        // TODO: 实现获取未读消息数
        // 建议：使用 Mailbox 的 getUnreadCount 接口
        return 0L;
    }
}
