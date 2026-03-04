package org.example.fleets.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.mailbox.model.vo.UnreadCountVO;
import org.example.fleets.mailbox.service.MailboxService;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.service.MessageSyncService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息同步服务实现类
 * 薄封装，委托 Mailbox 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSyncServiceImpl implements MessageSyncService {

    private final MailboxService mailboxService;

    @Override
    public void syncMessagesOnLogin(Long userId) {
        // 用户上线时无额外同步逻辑，客户端按会话调用 Mailbox.syncMessages 即可
        log.debug("用户上线: userId={}", userId);
    }

    @Override
    public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence, Integer limit) {
        return mailboxService.pullOfflineMessages(userId, lastSequence);
    }

    @Override
    public Long getLastSequence(Long userId) {
        // sequence 按 (userId, conversationId) 维度，无全局 lastSequence，返回 0 占位
        return 0L;
    }

    @Override
    public void updateLastSequence(Long userId, Long sequence) {
        // sequence 按会话维度，此处无操作；客户端应按会话维护 lastSequence
        log.debug("updateLastSequence 按会话维度，当前接口无操作: userId={}, sequence={}", userId, sequence);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        UnreadCountVO vo = mailboxService.getUnreadCount(userId);
        return vo != null && vo.getTotalUnread() != null ? vo.getTotalUnread().longValue() : 0L;
    }
}
