package org.example.fleets.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.config.properties.FleetsProperties;
import org.example.fleets.common.service.ConversationService;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.group.service.GroupService;
import org.example.fleets.mailbox.service.MailboxService;
import org.example.fleets.message.converter.MessageConverter;
import org.example.fleets.message.model.dto.MessageSendDTO;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.producer.MessageProducer;
import org.example.fleets.message.repository.MessageRepository;
import org.example.fleets.message.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息服务实现类
 * 
 * 依赖模块：
 * - GroupService: 获取群成员列表
 * - MailboxService: 写入信箱
 * - WebSocketService: 实时推送（通过 RocketMQ Consumer）
 * 
 * TODO: 在实现此模块前，请先完成：
 * 1. GroupService.getGroupMemberIds() 实现
 * 2. WebSocket 模块实现
 * 3. MessageConsumer 集成 WebSocketService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    
    private final MessageRepository messageRepository;
    private final MailboxService mailboxService;
    private final ConversationService conversationService;
    private final MessageProducer messageProducer;
    private final MessageConverter messageConverter;
    private final FleetsProperties fleetsProperties;
    private final GroupService groupService;
    
    @Override
    public MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO) {
        // TODO: 实现消息发送
        // 1. 参数校验
        // 2. 保存到 MongoDB
        // 3. 写入 Mailbox（需要 GroupService.getGroupMemberIds()）
        // 4. 更新会话表
        // 5. 发送到 RocketMQ
        
        log.warn("消息发送功能未实现");
        return null;
    }

    @Override
    public boolean recallMessage(String messageId, Long userId) {
        // TODO: 实现撤回消息
        return false;
    }

    @Override
    public boolean deleteMessage(String messageId, Long userId) {
        // TODO: 实现删除消息
        return false;
    }

    @Override
    public boolean markAsRead(String messageId, Long userId) {
        // TODO: 实现标记已读
        return false;
    }

    @Override
    public boolean batchMarkAsRead(List<String> messageIds, Long userId) {
        // TODO: 实现批量标记已读
        return false;
    }

    @Override
    public PageResult<MessageVO> getChatHistory(Long userId, Long targetUserId, Integer pageNum, Integer pageSize) {
        // TODO: 实现获取单聊历史
        return null;
    }

    @Override
    public PageResult<MessageVO> getGroupChatHistory(Long groupId, Integer pageNum, Integer pageSize) {
        // TODO: 实现获取群聊历史
        return null;
    }

    @Override
    public PageResult<MessageVO> searchMessage(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        // TODO: 实现搜索消息
        return null;
    }
}
