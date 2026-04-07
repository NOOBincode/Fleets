package org.example.fleets.common.service;

import org.example.fleets.common.model.Conversation;

import java.util.Date;
import java.util.List;

/**
 * 会话服务接口
 */
public interface ConversationService {
    
    /**
     * 更新或创建会话（发送消息时调用）
     * 
     * @param ownerId 会话所有者ID
     * @param targetId 目标ID（对方用户ID或群组ID）
     * @param type 会话类型（0-单聊，1-群聊）
     * @param messageId MongoDB 消息ID
     * @param content 消息内容
     * @param messageTime 消息时间
     * @param incrementUnread 是否增加未读数（发送者不增加，接收者增加）
     */
    void updateConversation(Long ownerId, Long targetId, Integer type,
                          String messageId, String content, Date messageTime,
                          boolean incrementUnread);

    /**
     * 确保会话存在（查不到就创建）
     *
     * 设计要点 / TODO（需要你实现）：
     * 1. 会话维度：
     *    - ownerId：当前登录用户 ID，即会话“所属者”
     *    - targetId：单聊时为对方用户 ID，群聊时为群组 ID
     *    - type：0=单聊，1=群聊（建议和前端 ConversationType 保持一致）
     *
     * 2. 行为：
     *    - 根据 ownerId + targetId + type 生成唯一的 conversationId
     *      （可以复用当前类中的 generateConversationId 逻辑）
     *    - 在 DB 中按 conversationId + ownerId 查询：
     *      - 若已存在则直接返回
     *      - 若不存在则插入一条“空会话”记录（无 lastMessage）
     *
     * 3. 注意事项：
     *    - 不要在这里修改未读数（unreadCount 初始为 0）
     *    - 避免和 updateConversation 里的“更新最后一条消息”逻辑耦合
     *    - 需要考虑并发下的幂等性（可以先简单实现，后续再根据需要优化）
     */
    Conversation ensureConversation(Long ownerId, Long targetId, Integer type);
    
    /**
     * 获取用户的会话列表
     */
    List<Conversation> getUserConversations(Long userId);
    
    /**
     * 清空会话未读数（用户点击会话时调用）
     */
    void clearUnreadCount(String conversationId, Long userId);
    
    /**
     * 删除会话
     */
    boolean deleteConversation(String conversationId, Long userId);
    
    /**
     * 置顶/取消置顶会话
     */
    boolean toggleTop(String conversationId, Long userId, boolean isTop);
    
    /**
     * 免打扰/取消免打扰
     */
    boolean toggleMute(String conversationId, Long userId, boolean isMute);
}
