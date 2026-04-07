package org.example.fleets.message.outbox.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.message.outbox.model.entity.MqOutboxEvent;
import org.example.fleets.message.outbox.repository.MqOutboxRepository;
import org.example.fleets.message.outbox.service.MqOutboxService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 仅负责入队幂等；发送由 Job 完成（或发送失败后由 Job 补偿）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqOutboxServiceImpl implements MqOutboxService {

    private static final int DEFAULT_MAX_RETRY = 10;

    private final MqOutboxRepository mqOutboxRepository;

    @Override
    public void enqueueIfAbsent(String bizKey, String topic, String payload) {
        if (bizKey == null || bizKey.trim().isEmpty()) {
            return;
        }
        try {
            mqOutboxRepository.save(MqOutboxEvent.newPending(bizKey, topic, payload, DEFAULT_MAX_RETRY));
        } catch (DuplicateKeyException ignore) {
            // 幂等：已存在即忽略
        } catch (Exception e) {
            // outbox 写失败不应影响主链路（否则等于引入强依赖）
            log.error("Outbox 入队失败: bizKey={}, topic={}", bizKey, topic, e);
        }
    }
}

