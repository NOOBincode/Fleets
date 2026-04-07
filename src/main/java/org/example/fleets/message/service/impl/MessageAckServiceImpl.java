package org.example.fleets.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.util.Assert;
import org.example.fleets.mailbox.service.MailboxService;
import org.example.fleets.message.outbox.job.MqOutboxRetryJob;
import org.example.fleets.message.service.MessageAckService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息确认服务实现类（送达占位；已读落库信箱；Outbox 重试与补偿）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageAckServiceImpl implements MessageAckService {
    
    private final MailboxService mailboxService;
    private final MqOutboxRetryJob mqOutboxRetryJob;
    
    @Override
    public void handleDeliveredAck(Long userId, String messageId) {
        // P0：当前信箱模型只区分未读/已读/已删除；送达态暂不落库，保持接口幂等且可观测
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(messageId, "messageId不能为空");
        log.debug("收到送达确认: userId={}, messageId={}", userId, messageId);
    }
    
    @Override
    public void handleReadAck(Long userId, String messageId) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(messageId, "messageId不能为空");
        mailboxService.markAsReadByMessageId(userId, messageId);
    }
    
    @Override
    public void batchHandleReadAck(Long userId, List<String> messageIds) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notEmpty(messageIds, "messageIds不能为空");
        // P0：先用循环保证正确性与幂等；后续可优化为 Mongo 批量更新
        for (String messageId : messageIds) {
            if (messageId == null || messageId.trim().isEmpty()) {
                continue;
            }
            mailboxService.markAsReadByMessageId(userId, messageId);
        }
    }
    
    @Override
    public void retryFailedMessages() {
        // 复用 Outbox 重试逻辑：手动触发一次扫描与投递
        mqOutboxRetryJob.retry();
    }
    
    @Override
    public void checkTimeoutMessages() {
        // P0：当前只做 Outbox 级别的超时补偿（已由 retry 负责）。
        // 送达/已读超时需要 per-user delivered 状态模型，后续再补。
        mqOutboxRetryJob.retry();
    }
}
