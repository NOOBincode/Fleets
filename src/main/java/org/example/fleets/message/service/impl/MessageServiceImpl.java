package org.example.fleets.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.example.fleets.message.producer.MessageProducer;
import org.example.fleets.message.repository.MessageRepository;
import org.example.fleets.message.service.MessageService;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.service.FriendshipService;
import org.springframework.stereotype.Service;

import java.util.Date;
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
        Message saved = messageRepository.save(message);
        if (saved == null) {
            throw new BusinessException(ErrorCode.MESSAGE_SAVE_FAILED);
        }

        // 会话ID生成规则与读取端保持一致：
        // type=0 表示单聊（conv_min_max），type=1 表示群聊（conv_group_<groupId>）
        int conversationType = (msgType == 1) ? 0 : 1;
        String conversationId = generateConversationId(conversationType, senderId,
                msgType == 1 ? sendDTO.getReceiverId() : sendDTO.getGroupId());

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

        // 5. 发送到 RocketMQ（供 MessageConsumer 做 WebSocket 推送）
        messageProducer.sendMessage(TOPIC_IM_MESSAGE, saved);

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
        return mailboxService.getConversationMessages(userId, conversationId, pageNum, pageSize);
    }

    @Override
    public PageResult<MessageVO> getGroupChatHistory(Long userId, Long groupId, Integer pageNum, Integer pageSize) {
        String conversationId = "conv_group_" + groupId;
        return mailboxService.getConversationMessages(userId, conversationId, pageNum, pageSize);
    }

    @Override
    public PageResult<MessageVO> searchMessage(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        // TODO: 实现搜索消息（按关键词搜索 MailboxMessage）
        log.warn("搜索消息功能未实现: userId={}, keyword={}", userId, keyword);
        return PageResult.empty(pageNum, pageSize);
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
