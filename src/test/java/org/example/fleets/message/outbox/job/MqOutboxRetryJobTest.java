package org.example.fleets.message.outbox.job;

import org.example.fleets.message.outbox.model.entity.MqOutboxEvent;
import org.example.fleets.message.outbox.repository.MqOutboxRepository;
import org.example.fleets.message.producer.MessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mongo Outbox 重试 Job 单元测试")
class MqOutboxRetryJobTest {

    @Mock
    private MqOutboxRepository mqOutboxRepository;
    @Mock
    private MessageProducer messageProducer;

    @InjectMocks
    private MqOutboxRetryJob job;

    @Test
    @DisplayName("无可重试事件：不发送、不更新")
    void retry_Empty_NoOps() {
        when(mqOutboxRepository.claimRetryBatch(anyInt(), anyInt())).thenReturn(Collections.emptyList());
        job.retry();
        verify(messageProducer, never()).sendMessage(anyString(), any());
        verify(mqOutboxRepository, never()).markSent(anyString());
        verify(mqOutboxRepository, never()).markFailed(anyString(), anyInt(), anyBoolean(), anyLong(), anyString());
    }

    @Test
    @DisplayName("发送成功：标记 SENT")
    void retry_SendSuccess_MarkSent() {
        MqOutboxEvent e = new MqOutboxEvent();
        e.setId("1");
        e.setTopic("im-message-topic");
        e.setPayload("{\"id\":\"msg_1\"}");
        e.setRetryCount(0);
        e.setMaxRetryCount(3);
        when(mqOutboxRepository.claimRetryBatch(anyInt(), anyInt())).thenReturn(Arrays.asList(e));

        job.retry();

        verify(messageProducer, times(1)).sendMessage(eq("im-message-topic"), eq("{\"id\":\"msg_1\"}"));
        verify(mqOutboxRepository, times(1)).markSent("1");
        verify(mqOutboxRepository, never()).markFailed(anyString(), anyInt(), anyBoolean(), anyLong(), anyString());
    }

    @Test
    @DisplayName("发送失败：标记 FAILED/DEAD 并设置退避")
    void retry_SendFail_MarkFailed() {
        MqOutboxEvent e = new MqOutboxEvent();
        e.setId("2");
        e.setTopic("im-message-topic");
        e.setPayload("{\"id\":\"msg_2\"}");
        e.setRetryCount(0);
        e.setMaxRetryCount(2);
        when(mqOutboxRepository.claimRetryBatch(anyInt(), anyInt())).thenReturn(Arrays.asList(e));
        doThrow(new RuntimeException("mq down")).when(messageProducer).sendMessage(anyString(), any());

        job.retry();

        verify(mqOutboxRepository, times(1))
                .markFailed(eq("2"), eq(1), eq(false), anyLong(), anyString());
    }
}

