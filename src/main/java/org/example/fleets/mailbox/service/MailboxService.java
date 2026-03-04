package org.example.fleets.mailbox.service;

import org.example.fleets.common.util.PageResult;
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
     * 写入消息到信箱（可选是否增加未读数）
     *
     * @param incrementUnread true-增加未读；false-不增加未读（典型：发送者自己的信箱）
     */
    boolean writeMessage(Long userId, String conversationId, Message message, boolean incrementUnread);
    
    /**
     * 批量写入消息（群聊场景）
     */
    boolean batchWriteMessage(List<Long> userIds, String conversationId, Message message);

    /**
     * 批量写入消息（群聊场景，可选是否增加未读数）
     *
     * @param incrementUnread true-增加未读；false-不增加未读
     */
    boolean batchWriteMessage(List<Long> userIds, String conversationId, Message message, boolean incrementUnread);
    
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
     * 按消息ID删除（当前用户信箱中的消息）
     */
    boolean deleteMessageByMessageId(Long userId, String messageId);

    /**
     * 按消息ID标记已读（当前用户信箱中的消息）
     */
    boolean markAsReadByMessageId(Long userId, String messageId);

    /**
     * 撤回消息：更新所有信箱中该消息的内容为「已撤回」
     */
    void recallMessageByMessageId(String messageId);
    
    /**
     * 生成序列号
     */
    Long generateSequence(Long userId, String conversationId);

    /**
     * 分页获取会话消息（按序列号倒序，最新在前）
     */
    PageResult<MessageVO> getConversationMessages(Long userId, String conversationId, int pageNum, int pageSize);
}
