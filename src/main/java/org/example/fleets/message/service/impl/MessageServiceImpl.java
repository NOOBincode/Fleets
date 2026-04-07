package org.example.fleets.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.common.service.ConversationService;
import org.example.fleets.common.util.Assert;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.group.service.GroupService;
import org.example.fleets.mailbox.service.MailboxService;
import org.example.fleets.message.converter.MessageConverter;
import org.example.fleets.message.model.dto.MessageSendDTO;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.message.model.enums.MessageStatus;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.outbox.service.MqOutboxService;
import org.example.fleets.message.producer.MessageProducer;
import org.example.fleets.message.repository.MessageRepository;
import org.example.fleets.message.service.MessageService;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.service.FriendshipService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final String TOPIC_IM_MESSAGE = "im-message-topic";

    private final MessageRepository messageRepository;
    private final MailboxService mailboxService;
    private final ConversationService conversationService;
    private final MessageProducer messageProducer;
    private final MqOutboxService mqOutboxService;
    private final ObjectMapper objectMapper;
    private final MessageConverter messageConverter;
    private final GroupService groupService;
    private final FriendshipService friendshipService;
    private final UserMapper userMapper;

    @Override
    public MessageVO sendMessage(Long senderId, MessageSendDTO sendDTO) {
        // 1. 参数校验
        Assert.notNull(sendDTO.getMessageType(), "消息类型不能为空");
        Assert.notNull(sendDTO.getContentType(), "内容类型不能为空");
        Assert.hasText(sendDTO.getContent(), "消息内容不能为空");

        int msgType = sendDTO.getMessageType();
        if (msgType == 1) {
            // 单聊
            Assert.notNull(sendDTO.getReceiverId(), "单聊时接收者不能为空");
            Assert.isTrue(friendshipService.isFriend(senderId, sendDTO.getReceiverId()),
                    ErrorCode.NOT_FRIEND_CANNOT_SEND);
        } else if (msgType == 2) {
            // 群聊
            Assert.notNull(sendDTO.getGroupId(), "群聊时群组不能为空");
            groupService.getGroupInfo(sendDTO.getGroupId()); // 校验群存在，不存在会抛 GROUP_NOT_FOUND
            List<Long> memberIds = groupService.getGroupMemberIds(sendDTO.getGroupId());
            Assert.isTrue(memberIds.contains(senderId), ErrorCode.NOT_GROUP_MEMBER);
        } else {
            throw new BusinessException(ErrorCode.INVALID_MESSAGE_TYPE);
        }

        // 2. 构建并保存 Message
        Message message = Message.fromSendDTO(senderId, sendDTO);
        
        // 会话ID生成规则与读取端保持一致：
        // type=0 表示单聊（conv_min_max），type=1 表示群聊（conv_group_<groupId>）
        int conversationType = (msgType == 1) ? 0 : 1;
        String conversationId = generateConversationId(conversationType, senderId,
                msgType == 1 ? sendDTO.getReceiverId() : sendDTO.getGroupId());
        message.setConversationId(conversationId);
        
        Message saved = messageRepository.save(message);
        Assert.notNull(saved, ErrorCode.MESSAGE_SAVE_FAILED);
        Assert.hasText(saved.getId(), ErrorCode.MESSAGE_SAVE_FAILED);

        // 3. 写入 Mailbox（发送者不增未读，接收者增未读）
        if (msgType == 1) {
            mailboxService.writeMessage(sendDTO.getReceiverId(), conversationId, saved, true);
            mailboxService.writeMessage(senderId, conversationId, saved, false);
        } else {
            List<Long> memberIds = groupService.getGroupMemberIds(sendDTO.getGroupId());
            List<Long> others = memberIds.stream().filter(id -> !id.equals(senderId)).collect(Collectors.toList());
            if (!others.isEmpty()) {
                mailboxService.batchWriteMessage(others, conversationId, saved, true);
            }
            mailboxService.writeMessage(senderId, conversationId, saved, false);
        }

        // 4. 更新会话表
        if (msgType == 1) {
            conversationService.updateConversation(senderId, sendDTO.getReceiverId(), 0,
                    saved.getId(), saved.getContent(), saved.getSendTime(), false);
            conversationService.updateConversation(sendDTO.getReceiverId(), senderId, 0,
                    saved.getId(), saved.getContent(), saved.getSendTime(), true);
        } else {
            List<Long> memberIds = groupService.getGroupMemberIds(sendDTO.getGroupId());
            for (Long userId : memberIds) {
                boolean incrementUnread = !userId.equals(senderId);
                conversationService.updateConversation(userId, sendDTO.getGroupId(), 1,
                        saved.getId(), saved.getContent(), saved.getSendTime(), incrementUnread);
            }
        }

        // 5. 写入 Outbox（幂等）并尽力投递 MQ（失败则由定时任务补偿重试）
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(saved);
        } catch (Exception e) {
            // 极端情况：序列化失败，直接降级为不推送（消息仍在 Mongo + Mailbox，可由客户端同步拉取）
            log.error("消息序列化失败，跳过 MQ 投递: messageId={}", saved.getId(), e);
            payloadJson = null;
        }

        if (payloadJson != null) {
            mqOutboxService.enqueueIfAbsent(saved.getId(), TOPIC_IM_MESSAGE, payloadJson);
            try {
                messageProducer.sendMessage(TOPIC_IM_MESSAGE, payloadJson);
            } catch (Exception e) {
                log.warn("MQ 投递失败，等待 Outbox 重试: messageId={}", saved.getId(), e);
            }
        }

        // 6. 返回 MessageVO（填充发送者信息）
        MessageVO vo = messageConverter.toVO(saved);
        enrichSenderInfo(vo);
        return vo;
    }

    @Override
    public boolean recallMessage(String messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        Assert.isTrue(message.getSenderId().equals(userId), ErrorCode.MESSAGE_CANNOT_RECALL);
        if (MessageStatus.RECALLED.getCode().equals(message.getStatus())) {
            return true; // 已撤回，幂等
        }
        message.setStatus(MessageStatus.RECALLED.getCode());
        messageRepository.save(message);
        mailboxService.recallMessageByMessageId(messageId);
        return true;
    }

    @Override
    public boolean deleteMessage(String messageId, Long userId) {
        return mailboxService.deleteMessageByMessageId(userId, messageId);
    }

    @Override
    public boolean markAsRead(String messageId, Long userId) {
        return mailboxService.markAsReadByMessageId(userId, messageId);
    }

    @Override
    public boolean batchMarkAsRead(List<String> messageIds, Long userId) {
        Assert.notEmpty(messageIds, "消息ID列表不能为空");
        for (String messageId : messageIds) {
            mailboxService.markAsReadByMessageId(userId, messageId);
        }
        return true;
    }

    @Override
    public PageResult<MessageVO> getChatHistory(Long userId, Long targetUserId, Integer pageNum, Integer pageSize) {
        String conversationId = generateConversationId(0, userId, targetUserId);
        // 首次进入单聊时确保会话存在（无消息也可打开聊天页）
        conversationService.ensureConversation(userId, targetUserId, 0);
        return mailboxService.getConversationMessages(userId, conversationId, pageNum, pageSize);
    }

    @Override
    public PageResult<MessageVO> getGroupChatHistory(Long userId, Long groupId, Integer pageNum, Integer pageSize) {
        // 进入群聊必须校验：群存在且当前用户为群成员
        groupService.getGroupInfo(groupId); // 不存在会抛 GROUP_NOT_FOUND
        List<Long> memberIds = groupService.getGroupMemberIds(groupId);
        Assert.isTrue(memberIds.contains(userId), ErrorCode.NOT_GROUP_MEMBER);

        String conversationId = "conv_group_" + groupId;
        // 首次进入群聊时确保会话存在（无消息也可打开聊天页）
        conversationService.ensureConversation(userId, groupId, 1);
        return mailboxService.getConversationMessages(userId, conversationId, pageNum, pageSize);
    }

    @Override
    public PageResult<MessageVO> searchMessage(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(keyword, "搜索关键词不能为空");
        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? Math.min(pageSize, 100) : 20;
        String kw = keyword.trim();
        if (kw.length() > 200) {
            kw = kw.substring(0, 200);
        }
        return mailboxService.searchMessages(userId, kw, pn, ps);
    }

    private String generateConversationId(Integer type, Long userId1, Long targetId) {
        if (type == 0) {
            long min = Math.min(userId1, targetId);
            long max = Math.max(userId1, targetId);
            return "conv_" + min + "_" + max;
        }
        return "conv_group_" + targetId;
    }

    private void enrichSenderInfo(MessageVO vo) {
        if (vo == null || vo.getSenderId() == null) {
            return;
        }
        User sender = userMapper.selectById(vo.getSenderId());
        if (sender != null) {
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        }
    }
}
