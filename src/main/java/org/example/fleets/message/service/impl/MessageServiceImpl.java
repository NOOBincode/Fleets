package org.example.fleets.message.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.service.ConversationService;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.message.model.dto.MessageSendDTO;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.producer.MessageProducer;
import org.example.fleets.message.repository.MessageRepository;
import org.example.fleets.message.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 消息服务实现类
 * 
 * 核心流程：
 * 1. 保存消息到 MongoDB（主存储）
 * 2. 更新会话表到 MySQL（摘要信息）
 * 3. 发送到 RocketMQ（异步推送）
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {
    
    @Autowired
    private MessageRepository messageRepository;  // MongoDB
    
    @Autowired
    private ConversationService conversationService;  // MySQL 会话表
    
    @Autowired
    private MessageProducer messageProducer;  // RocketMQ
    
    @Override
    public MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO) {
        log.info("发送消息: senderId={}, messageType={}, receiverId={}, groupId={}", 
            senderId, sendDTO.getMessageType(), sendDTO.getReceiverId(), sendDTO.getGroupId());
        
        // 1. 构建消息对象
        Message message = new Message();
        message.setMessageType(sendDTO.getMessageType());
        message.setContentType(sendDTO.getContentType());
        message.setSenderId(senderId);
        message.setReceiverId(sendDTO.getReceiverId());
        message.setGroupId(sendDTO.getGroupId());
        message.setContent(sendDTO.getContent());
        message.setSendTime(new Date());
        message.setStatus(1);  // 1-已发送
        message.setExtra(sendDTO.getExtra());
        
        // 2. 保存到 MongoDB（主存储）
        Message savedMessage = messageRepository.save(message).block();
        String messageId = savedMessage.getId();
        log.info("消息保存到MongoDB成功: messageId={}", messageId);
        
        try {
            // 3. 更新会话表（MySQL）
            if (sendDTO.getMessageType() == 1) {
                // 单聊：更新双方的会话表
                updateSingleChatConversation(senderId, sendDTO.getReceiverId(), 
                    messageId, sendDTO.getContent(), savedMessage.getSendTime());
            } else if (sendDTO.getMessageType() == 2) {
                // 群聊：更新发送者的会话表（接收者的会话表由消费者更新）
                conversationService.updateConversation(
                    senderId, sendDTO.getGroupId(), 1,
                    messageId, sendDTO.getContent(), savedMessage.getSendTime(),
                    false  // 发送者不增加未读数
                );
            }
            
            // 4. 发送到消息队列（异步推送）
            messageProducer.sendMessage("im-message-topic", savedMessage);
            log.info("消息发送到RocketMQ成功: messageId={}", messageId);
            
        } catch (Exception e) {
            // 如果会话表更新失败，记录日志但不影响消息发送
            // 可以通过定时任务或补偿机制修复
            log.error("更新会话表失败，messageId: {}", messageId, e);
        }
        
        // 5. 返回消息VO
        return convertToVO(savedMessage);
    }
    
    /**
     * 更新单聊会话表
     */
    private void updateSingleChatConversation(Long senderId, Long receiverId, 
                                             String messageId, String content, Date sendTime) {
        // 更新发送者的会话表（不增加未读数）
        conversationService.updateConversation(
            senderId, receiverId, 0,
            messageId, content, sendTime,
            false
        );
        
        // 更新接收者的会话表（增加未读数）
        conversationService.updateConversation(
            receiverId, senderId, 0,
            messageId, content, sendTime,
            true
        );
    }
    
    /**
     * 转换为VO
     */
    private MessageVO convertToVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setMessageType(message.getMessageType());
        vo.setContentType(message.getContentType());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setGroupId(message.getGroupId());
        vo.setContent(message.getContent());
        vo.setSequence(message.getSequence());
        vo.setStatus(message.getStatus());
        vo.setSendTime(message.getSendTime());
        vo.setExtra(message.getExtra());
        return vo;
    }

    @Override
    public boolean recallMessage(String messageId, Long userId) {
        // TODO: 实现撤回消息逻辑
        return false;
    }

    @Override
    public boolean deleteMessage(String messageId, Long userId) {
        // TODO: 实现删除消息逻辑
        return false;
    }

    @Override
    public boolean markAsRead(String messageId, Long userId) {
        // TODO: 实现标记消息已读逻辑
        return false;
    }

    @Override
    public boolean batchMarkAsRead(List<String> messageIds, Long userId) {
        // TODO: 实现批量标记已读逻辑
        return false;
    }

    @Override
    public PageResult<MessageVO> getChatHistory(Long userId, Long targetUserId, Integer pageNum, Integer pageSize) {
        // TODO: 实现获取单聊消息历史逻辑
        return null;
    }

    @Override
    public PageResult<MessageVO> getGroupChatHistory(Long groupId, Integer pageNum, Integer pageSize) {
        // TODO: 实现获取群聊消息历史逻辑
        return null;
    }

    @Override
    public PageResult<MessageVO> searchMessage(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        // TODO: 实现搜索消息逻辑
        return null;
    }
}
