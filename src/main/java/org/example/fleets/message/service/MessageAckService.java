package org.example.fleets.message.service;

import org.example.fleets.message.model.dto.MessageAckDTO;

import java.util.List;

/**
 * 消息确认服务接口
 */
public interface MessageAckService {
    
    /**
     * 处理送达确认
     * 
     * @param userId 用户ID
     * @param messageId 消息ID
     */
    void handleDeliveredAck(Long userId, String messageId);
    
    /**
     * 处理已读确认
     * 
     * @param userId 用户ID
     * @param messageId 消息ID
     */
    void handleReadAck(Long userId, String messageId);
    
    /**
     * 批量处理已读确认
     * 
     * @param userId 用户ID
     * @param messageIds 消息ID列表
     */
    void batchHandleReadAck(Long userId, List<String> messageIds);
    
    /**
     * 检查并重试失败的消息
     * 定时任务调用
     */
    void retryFailedMessages();
    
    /**
     * 检查超时未确认的消息
     * 定时任务调用
     */
    void checkTimeoutMessages();
}
