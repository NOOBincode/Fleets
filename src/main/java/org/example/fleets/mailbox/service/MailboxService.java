package org.example.fleets.mailbox.service;

import org.example.fleets.mailbox.model.dto.MarkReadDTO;
import org.example.fleets.mailbox.model.dto.SyncMessageDTO;
import org.example.fleets.mailbox.model.vo.SyncResult;
import org.example.fleets.mailbox.model.vo.UnreadCountVO;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.message.model.vo.MessageVO;

import java.util.List;

/**
 * Mailbox服务接口
 */
public interface MailboxService {
    
    /**
     * 写入消息到信箱
     */
    boolean writeMessage(Long userId, String conversationId, Message message);
    
    /**
     * 批量写入消息（群聊场景）
     */
    boolean batchWriteMessage(List<Long> userIds, String conversationId, Message message);
    
    /**
     * 拉取离线消息
     */
    List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence);
    
    /**
     * 增量同步消息
     */
    SyncResult syncMessages(Long userId, SyncMessageDTO syncDTO);
    
    /**
     * 标记消息已读
     */
    boolean markAsRead(Long userId, MarkReadDTO markReadDTO);
    
    /**
     * 批量标记已读（到指定序列号）
     */
    boolean batchMarkAsRead(Long userId, String conversationId, Long toSequence);
    
    /**
     * 获取未读消息数
     */
    UnreadCountVO getUnreadCount(Long userId);
    
    /**
     * 获取会话未读数
     */
    Integer getConversationUnreadCount(Long userId, String conversationId);
    
    /**
     * 清空会话消息
     */
    boolean clearConversation(Long userId, String conversationId);
    
    /**
     * 删除消息
     */
    boolean deleteMessage(Long userId, String conversationId, Long sequence);
    
    /**
     * 生成序列号
     */
    Long generateSequence(Long userId, String conversationId);
}
