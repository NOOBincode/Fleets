package org.example.fleets.mailbox.repository.custom;

/**
 * 用户信箱 Repository 自定义方法（原子递减未读数）
 */
public interface UserMailboxRepositoryCustom {

    /**
     * 仅当未读数大于 0 时原子减 1，避免减成负数。
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 实际更新的文档数（1 表示减成功，0 表示未读已为 0 或信箱不存在）
     */
    long decrementUnreadCountIfPositive(Long userId, String conversationId);
}
