package org.example.fleets.message.outbox.repository.custom;

import org.example.fleets.message.outbox.model.entity.MqOutboxEvent;

import java.util.List;

/**
 * Mongo Outbox 自定义原子操作（claim + 状态更新）
 */
public interface MqOutboxRepositoryCustom {

    /**
     * 原子抢占一批可重试事件：将其状态置为 SENDING，并设置 lockedUntil。
     */
    List<MqOutboxEvent> claimRetryBatch(int limit, int lockSeconds);

    void markSent(String id);

    void markFailed(String id, int retryCount, boolean dead, long nextRetryDelayMs, String lastError);
}

