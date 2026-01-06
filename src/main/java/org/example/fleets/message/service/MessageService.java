package org.example.fleets.message.service;

import org.example.fleets.common.util.PageResult;
import org.example.fleets.message.model.dto.MessageSendDTO;
import org.example.fleets.message.model.vo.MessageVO;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {
    
    /**
     * 发送消息
     */
    MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO);
    
    /**
     * 撤回消息
     */
    boolean recallMessage(String messageId, Long userId);
    
    /**
     * 删除消息
     */
    boolean deleteMessage(String messageId, Long userId);
    
    /**
     * 标记消息已读
     */
    boolean markAsRead(String messageId, Long userId);
    
    /**
     * 批量标记已读
     */
    boolean batchMarkAsRead(List<String> messageIds, Long userId);
    
    /**
     * 获取单聊消息历史
     */
    PageResult<MessageVO> getChatHistory(Long userId, Long targetUserId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取群聊消息历史
     */
    PageResult<MessageVO> getGroupChatHistory(Long groupId, Integer pageNum, Integer pageSize);
    
    /**
     * 搜索消息
     */
    PageResult<MessageVO> searchMessage(Long userId, String keyword, Integer pageNum, Integer pageSize);
}
