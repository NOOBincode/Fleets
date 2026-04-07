package org.example.fleets.mailbox.repository.custom;

import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    /**
     * 批量标记已读：仅将 status=0 且 sequence <= toSequence 的消息更新为已读，并设置阅读时间。
     * 返回实际从未读变为已读的文档数，用于后续安全递减未读数（避免幂等导致多减）。
     */
    long markAsReadUpToSequenceIfUnread(Long userId, String conversationId, Long toSequence, Date readTime);

    /**
     * 批量软删除会话消息：将该会话下 status != 2 的消息置为 status=2（已删除）。
     *
     * @return 实际更新的文档数
     */
    long markAsDeletedByConversation(Long userId, String conversationId, Date updateTime);

    /**
     * 在当前用户信箱内按正文关键字搜索（忽略大小写，keyword 按字面量匹配，已转义正则元字符）。
     */
    Page<MailboxMessage> searchByUserIdAndKeyword(Long userId, String keywordLiteral, Pageable pageable);
}
