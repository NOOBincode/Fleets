package org.example.fleets.message.outbox.service.impl;

import org.example.fleets.message.outbox.repository.MqOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mongo Outbox 入队单元测试")
class MqOutboxServiceImplTest {

    @Mock
    private MqOutboxRepository mqOutboxRepository;

    @InjectMocks
    private MqOutboxServiceImpl mqOutboxService;

    @Test
    @DisplayName("bizKey 为空：不入队")
    void enqueueIfAbsent_BlankBizKey_NoSave() {
        mqOutboxService.enqueueIfAbsent(" ", "im-message-topic", "{}");
        verify(mqOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("重复 bizKey：捕获 DuplicateKeyException 并忽略")
    void enqueueIfAbsent_Duplicate_Ignored() {
        when(mqOutboxRepository.save(any())).thenThrow(new DuplicateKeyException("dup"));
        mqOutboxService.enqueueIfAbsent("msg_1", "im-message-topic", "{\"id\":\"msg_1\"}");
        verify(mqOutboxRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("保存异常：吞掉异常不影响主流程")
    void enqueueIfAbsent_SaveError_Swallowed() {
        when(mqOutboxRepository.save(any())).thenThrow(new RuntimeException("boom"));
        mqOutboxService.enqueueIfAbsent("msg_2", "im-message-topic", "{\"id\":\"msg_2\"}");
        verify(mqOutboxRepository, times(1)).save(any());
    }
}

