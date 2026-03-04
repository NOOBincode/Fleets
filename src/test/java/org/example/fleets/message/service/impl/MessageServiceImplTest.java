package org.example.fleets.message.service.impl;

import lombok.var;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.common.service.ConversationService;
import org.example.fleets.group.model.vo.GroupVO;
import org.example.fleets.group.service.GroupService;
import org.example.fleets.mailbox.service.MailboxService;
import org.example.fleets.message.converter.MessageConverter;
import org.example.fleets.message.model.dto.MessageSendDTO;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.message.model.enums.MessageStatus;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.producer.MessageProducer;
import org.example.fleets.message.repository.MessageRepository;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.service.FriendshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 消息服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("消息服务单元测试")
class MessageServiceImplTest {

    private static final Long SENDER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long GROUP_ID = 10L;
    private static final String MESSAGE_ID = "msg_001";

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MailboxService mailboxService;
    @Mock
    private ConversationService conversationService;
    @Mock
    private MessageProducer messageProducer;
    @Mock
    private MessageConverter messageConverter;
    @Mock
    private GroupService groupService;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MessageServiceImpl messageService;

    private MessageSendDTO singleChatDTO;
    private MessageSendDTO groupChatDTO;
    private Message savedMessage;

    @BeforeEach
    void setUp() {
        singleChatDTO = new MessageSendDTO();
        singleChatDTO.setMessageType(1);
        singleChatDTO.setContentType(1);
        singleChatDTO.setReceiverId(RECEIVER_ID);
        singleChatDTO.setContent("hello");

        groupChatDTO = new MessageSendDTO();
        groupChatDTO.setMessageType(2);
        groupChatDTO.setContentType(1);
        groupChatDTO.setGroupId(GROUP_ID);
        groupChatDTO.setContent("hi group");

        savedMessage = new Message();
        savedMessage.setId(MESSAGE_ID);
        savedMessage.setMessageType(1);
        savedMessage.setSenderId(SENDER_ID);
        savedMessage.setReceiverId(RECEIVER_ID);
        savedMessage.setContent("hello");
        savedMessage.setStatus(MessageStatus.SENT.getCode());
        savedMessage.setSendTime(new Date());
    }

    @Test
    @DisplayName("发送单聊消息 - 成功：保存、信箱、会话、MQ 均被调用")
    void sendMessage_SingleChat_Success() {
        when(friendshipService.isFriend(SENDER_ID, RECEIVER_ID)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        MessageVO vo = new MessageVO();
        vo.setId(MESSAGE_ID);
        when(messageConverter.toVO(any(Message.class))).thenReturn(vo);

        MessageVO result = messageService.sendMessage(SENDER_ID, singleChatDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(MESSAGE_ID);

        verify(messageRepository, times(1)).save(any(Message.class));
        verify(mailboxService, times(2)).writeMessage(anyLong(), anyString(), any(Message.class), anyBoolean());
        verify(conversationService, times(2)).updateConversation(anyLong(), anyLong(), eq(0), eq(MESSAGE_ID), anyString(), any(Date.class), anyBoolean());

        ArgumentCaptor<Object> mqPayload = ArgumentCaptor.forClass(Object.class);
        verify(messageProducer, times(1)).sendMessage(eq("im-message-topic"), mqPayload.capture());
        assertThat(mqPayload.getValue()).isSameAs(savedMessage);
    }

    @Test
    @DisplayName("发送单聊消息 - 非好友抛出 NOT_FRIEND_CANNOT_SEND")
    void sendMessage_SingleChat_NotFriend_Throws() {
        when(friendshipService.isFriend(SENDER_ID, RECEIVER_ID)).thenReturn(false);

        assertThatThrownBy(() -> messageService.sendMessage(SENDER_ID, singleChatDTO))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FRIEND_CANNOT_SEND));

        verify(messageRepository, never()).save(any());
        verify(messageProducer, never()).sendMessage(anyString(), any());
    }

    @Test
    @DisplayName("发送群聊消息 - 成功：保存、信箱、会话、MQ 均被调用")
    void sendMessage_GroupChat_Success() {
        GroupVO groupVO = new GroupVO();
        groupVO.setId(GROUP_ID);
        when(groupService.getGroupInfo(GROUP_ID)).thenReturn(groupVO);
        when(groupService.getGroupMemberIds(GROUP_ID)).thenReturn(Arrays.asList(SENDER_ID, 2L, 3L));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        savedMessage.setMessageType(2);
        savedMessage.setGroupId(GROUP_ID);
        savedMessage.setReceiverId(null);
        MessageVO vo = new MessageVO();
        vo.setId(MESSAGE_ID);
        when(messageConverter.toVO(any(Message.class))).thenReturn(vo);

        MessageVO result = messageService.sendMessage(SENDER_ID, groupChatDTO);

        assertThat(result).isNotNull();
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(mailboxService, atLeastOnce()).writeMessage(anyLong(), anyString(), any(Message.class), anyBoolean());
        verify(conversationService, times(3)).updateConversation(anyLong(), eq(GROUP_ID), eq(1), eq(MESSAGE_ID), anyString(), any(Date.class), anyBoolean());
        verify(messageProducer, times(1)).sendMessage(eq("im-message-topic"), any(Message.class));
    }

    @Test
    @DisplayName("发送群聊消息 - 非群成员抛出 NOT_GROUP_MEMBER")
    void sendMessage_GroupChat_NotMember_Throws() {
        when(groupService.getGroupMemberIds(GROUP_ID)).thenReturn(Arrays.asList(2L, 3L));

        assertThatThrownBy(() -> messageService.sendMessage(SENDER_ID, groupChatDTO))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_GROUP_MEMBER));

        verify(messageRepository, never()).save(any());
        verify(messageProducer, never()).sendMessage(anyString(), any());
    }

    @Test
    @DisplayName("撤回消息 - 成功")
    void recallMessage_Success() {
        Message message = new Message();
        message.setId(MESSAGE_ID);
        message.setSenderId(SENDER_ID);
        message.setStatus(MessageStatus.SENT.getCode());
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        boolean result = messageService.recallMessage(MESSAGE_ID, SENDER_ID);

        assertThat(result).isTrue();
        verify(messageRepository).save(argThat(m -> MessageStatus.RECALLED.getCode().equals(m.getStatus())));
        verify(mailboxService).recallMessageByMessageId(MESSAGE_ID);
    }

    @Test
    @DisplayName("撤回消息 - 非发送者抛出 MESSAGE_CANNOT_RECALL")
    void recallMessage_NotSender_Throws() {
        Message message = new Message();
        message.setId(MESSAGE_ID);
        message.setSenderId(SENDER_ID);
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.recallMessage(MESSAGE_ID, RECEIVER_ID))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.MESSAGE_CANNOT_RECALL));

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("撤回消息 - 消息不存在抛出 MESSAGE_NOT_FOUND")
    void recallMessage_NotFound_Throws() {
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.recallMessage(MESSAGE_ID, SENDER_ID))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.MESSAGE_NOT_FOUND));
    }

    @Test
    @DisplayName("删除消息 - 委托给 MailboxService")
    void deleteMessage_DelegatesToMailbox() {
        when(mailboxService.deleteMessageByMessageId(SENDER_ID, MESSAGE_ID)).thenReturn(true);

        boolean result = messageService.deleteMessage(MESSAGE_ID, SENDER_ID);

        assertThat(result).isTrue();
        verify(mailboxService).deleteMessageByMessageId(SENDER_ID, MESSAGE_ID);
    }

    @Test
    @DisplayName("标记已读 - 委托给 MailboxService")
    void markAsRead_DelegatesToMailbox() {
        when(mailboxService.markAsReadByMessageId(SENDER_ID, MESSAGE_ID)).thenReturn(true);

        boolean result = messageService.markAsRead(MESSAGE_ID, SENDER_ID);

        assertThat(result).isTrue();
        verify(mailboxService).markAsReadByMessageId(SENDER_ID, MESSAGE_ID);
    }

    @Test
    @DisplayName("批量标记已读 - 委托给 MailboxService")
    void batchMarkAsRead_DelegatesToMailbox() {
        List<String> ids = Arrays.asList("id1", "id2");
        when(mailboxService.markAsReadByMessageId(eq(SENDER_ID), anyString())).thenReturn(true);

        boolean result = messageService.batchMarkAsRead(ids, SENDER_ID);

        assertThat(result).isTrue();
        verify(mailboxService, times(2)).markAsReadByMessageId(eq(SENDER_ID), anyString());
    }

    @Test
    @DisplayName("获取单聊历史 - 委托给 MailboxService")
    void getChatHistory_DelegatesToMailbox() {
        org.example.fleets.common.util.PageResult<MessageVO> page = org.example.fleets.common.util.PageResult.empty(1, 10);
        when(mailboxService.getConversationMessages(eq(SENDER_ID), anyString(), eq(1), eq(10))).thenReturn(page);

        var result = messageService.getChatHistory(SENDER_ID, RECEIVER_ID, 1, 10);

        assertThat(result).isNotNull();
        verify(mailboxService).getConversationMessages(SENDER_ID, "conv_1_2", 1, 10);
    }

    @Test
    @DisplayName("获取群聊历史 - 委托给 MailboxService")
    void getGroupChatHistory_DelegatesToMailbox() {
        org.example.fleets.common.util.PageResult<MessageVO> page = org.example.fleets.common.util.PageResult.empty(1, 10);
        when(mailboxService.getConversationMessages(eq(SENDER_ID), eq("conv_group_" + GROUP_ID), eq(1), eq(10))).thenReturn(page);

        var result = messageService.getGroupChatHistory(SENDER_ID, GROUP_ID, 1, 10);

        assertThat(result).isNotNull();
        verify(mailboxService).getConversationMessages(SENDER_ID, "conv_group_10", 1, 10);
    }
}
