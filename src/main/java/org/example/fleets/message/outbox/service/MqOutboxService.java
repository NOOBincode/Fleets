package org.example.fleets.message.outbox.service;

/**
 * Mongo Outbox 服务：负责入队与投递补偿
 */
public interface MqOutboxService {

    /**
     * 幂等入队（若 bizKey 已存在则忽略）。
     */
    void enqueueIfAbsent(String bizKey, String topic, String payload);
}

