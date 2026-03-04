package org.example.fleets.message.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.fleets.group.service.GroupService;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.websocket.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MessageConsumer 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("消息消费者单元测试")
class MessageConsumerTest {

    private static final Long RECEIVER_ID = 2L;
    private static final Long GROUP_ID = 10L;
    private static final String MESSAGE_ID = "msg_001";

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private GroupService groupService;

    @InjectMocks
    private MessageConsumer messageConsumer;

    private Message singleChatMessage;
    private Message groupChatMessage;

    @BeforeEach
    void setUp() {
        singleChatMessage = new Message();
        singleChatMessage.setId(MESSAGE_ID);
        singleChatMessage.setMessageType(1);
        singleChatMessage.setSenderId(1L);
        singleChatMessage.setReceiverId(RECEIVER_ID);
        singleChatMessage.setContent("hello");
        singleChatMessage.setSendTime(new Date());

        groupChatMessage = new Message();
        groupChatMessage.setId("msg_002");
        groupChatMessage.setMessageType(2);
        groupChatMessage.setSenderId(1L);
        groupChatMessage.setGroupId(GROUP_ID);
        groupChatMessage.setContent("hi group");
        groupChatMessage.setSendTime(new Date());
    }

    @Test
    @DisplayName("单聊消息 - 解析成功并推送给接收者")
    void onMessage_SingleChat_CallsSendMessageToUser() throws JsonProcessingException {
        String json = "{\"id\":\"msg_001\",\"messageType\":1,\"receiverId\":2}";
        when(objectMapper.readValue(json, Message.class)).thenReturn(singleChatMessage);

        messageConsumer.onMessage(json);

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(webSocketService, times(1)).sendMessageToUser(userIdCaptor.capture(), messageCaptor.capture());
        assertThat(userIdCaptor.getValue()).isEqualTo(RECEIVER_ID);
        assertThat(messageCaptor.getValue().getId()).isEqualTo(MESSAGE_ID);
    }

    @Test
    @DisplayName("群聊消息 - 解析成功并推送到群")
    void onMessage_GroupChat_CallsSendMessageToGroup() throws JsonProcessingException {
        String json = "{\"id\":\"msg_002\",\"messageType\":2,\"groupId\":10}";
        when(objectMapper.readValue(json, Message.class)).thenReturn(groupChatMessage);

        messageConsumer.onMessage(json);

        ArgumentCaptor<Long> groupIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(webSocketService, times(1)).sendMessageToGroup(groupIdCaptor.capture(), messageCaptor.capture());
        assertThat(groupIdCaptor.getValue()).isEqualTo(GROUP_ID);
        assertThat(messageCaptor.getValue().getMessageType()).isEqualTo(2);
    }

    @Test
    @DisplayName("JSON 解析失败 - 不推送、不抛异常")
    void onMessage_InvalidJson_NoPush() throws JsonProcessingException {
        String invalidJson = "not json";
        when(objectMapper.readValue(invalidJson, Message.class)).thenThrow(new JsonProcessingException("bad") {});

        messageConsumer.onMessage(invalidJson);

        verify(webSocketService, never()).sendMessageToUser(anyLong(), any(Message.class));
        verify(webSocketService, never()).sendMessageToGroup(anyLong(), any(Message.class));
    }

    @Test
    @DisplayName("消息无 id - 不推送")
    void onMessage_NoId_NoPush() throws JsonProcessingException {
        singleChatMessage.setId(null);
        String json = "{}";
        when(objectMapper.readValue(json, Message.class)).thenReturn(singleChatMessage);

        messageConsumer.onMessage(json);

        verify(webSocketService, never()).sendMessageToUser(anyLong(), any(Message.class));
    }

    @Test
    @DisplayName("单聊消息无 receiverId - 不推送")
    void onMessage_SingleChat_NoReceiverId_NoPush() throws JsonProcessingException {
        singleChatMessage.setReceiverId(null);
        String json = "{\"id\":\"msg_001\",\"messageType\":1}";
        when(objectMapper.readValue(json, Message.class)).thenReturn(singleChatMessage);

        messageConsumer.onMessage(json);

        verify(webSocketService, never()).sendMessageToUser(anyLong(), any(Message.class));
    }

    @Test
    @DisplayName("群聊消息无 groupId - 不推送")
    void onMessage_GroupChat_NoGroupId_NoPush() throws JsonProcessingException {
        groupChatMessage.setGroupId(null);
        String json = "{\"id\":\"msg_002\",\"messageType\":2}";
        when(objectMapper.readValue(json, Message.class)).thenReturn(groupChatMessage);

        messageConsumer.onMessage(json);

        verify(webSocketService, never()).sendMessageToGroup(anyLong(), any(Message.class));
    }
}
