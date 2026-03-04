package org.example.fleets.mailbox.repository.custom;

import java.util.Date;

/**
 * 信箱消息 Repository 自定义方法（条件更新 / 轻量乐观锁）
 */
public interface MailboxMessageRepositoryCustom {

    /**
     * 仅当消息当前为未读（status=0）时更新为已读，并设置阅读时间。
     * 用于轻量乐观锁：避免重复标记已读导致未读数多减。
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @param sequence       序列号
     * @param readTime       阅读时间
     * @return 实际更新的文档数（1 表示从未读改为已读，0 表示已读或不存在，幂等）
     */
    long markAsReadIfUnread(Long userId, String conversationId, Long sequence, Date readTime);
}
