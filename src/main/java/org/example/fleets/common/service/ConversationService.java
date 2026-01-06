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
