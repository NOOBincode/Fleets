package org.example.fleets.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.cache.GenericCacheService;
import org.example.fleets.common.config.properties.FleetsProperties;
import org.example.fleets.mailbox.model.vo.UnreadCountVO;
import org.example.fleets.mailbox.service.MailboxService;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.service.MessageSyncService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSyncServiceImpl implements MessageSyncService {

    private final MailboxService mailboxService;
    private final GenericCacheService genericCacheService;
    private final FleetsProperties fleetsProperties;

    @Override
    public void syncMessagesOnLogin(Long userId) {
        log.debug("用户上线: userId={}", userId);
    }

    @Override
    public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence, Integer limit) {
        return mailboxService.pullOfflineMessages(userId, lastSequence, limit);
    }

    @Override
    public Long getLastSequence(Long userId) {
        String key = fleetsProperties.getRedis().getMessageSyncLastSeqPrefix() + userId;
        Long v = genericCacheService.get(key);
        if (v != null) {
            return v;
        }
        return mailboxService.getMaxMailboxSequence(userId);
    }

    @Override
    public void updateLastSequence(Long userId, Long sequence) {
        if (userId == null || sequence == null) {
            return;
        }
        String key = fleetsProperties.getRedis().getMessageSyncLastSeqPrefix() + userId;
        genericCacheService.set(key, sequence);
        log.debug("更新同步游标: userId={}, sequence={}", userId, sequence);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        UnreadCountVO vo = mailboxService.getUnreadCount(userId);
        return vo != null && vo.getTotalUnread() != null ? vo.getTotalUnread().longValue() : 0L;
    }
}
