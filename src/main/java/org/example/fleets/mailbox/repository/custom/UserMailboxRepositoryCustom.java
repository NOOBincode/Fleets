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

    /**
     * 尝试原子递减未读数 delta（仅当 unreadCount >= delta 时才递减成功）。
     *
     * @return 实际更新的文档数（1 表示递减成功，0 表示 unreadCount 不足或信箱不存在）
     */
    long decrementUnreadCountByIfEnough(Long userId, String conversationId, long delta);

    /**
     * 将未读数直接置 0（仅当 unreadCount > 0 时更新）。
     *
     * @return 实际更新的文档数
     */
    long resetUnreadCountToZero(Long userId, String conversationId);

    /**
     * 原子递减未读数，如果不够则置零（单次调用，避免两次数据库操作）。
     * 使用 $max 操作符保证不会变成负数。
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @param delta          递减数量
     * @return 实际更新的文档数
     */
    long decrementOrResetUnreadCount(Long userId, String conversationId, long delta);
}
