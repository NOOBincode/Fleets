package org.example.fleets.message.outbox.model.entity;

import lombok.Data;
import org.example.fleets.message.outbox.model.enums.OutboxStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Mongo Outbox：用于 MQ 投递补偿与重试
 */
@Data
@Document(collection = "mq_outbox")
@CompoundIndex(name = "idx_status_next_retry", def = "{'status': 1, 'nextRetryAt': 1}")
public class MqOutboxEvent {

    @Id
    private String id;

    /**
     * 业务幂等键：建议使用 messageId 或 topic:messageId
     */
    @Indexed(unique = true, name = "uniq_biz_key")
    private String bizKey;

    private String topic;

    /**
     * 发送到 MQ 的原始 payload（JSON 字符串）
     */
    private String payload;

    /**
     * PENDING/SENDING/SENT/FAILED/DEAD
     */
    private String status;

    private Integer retryCount;

    private Integer maxRetryCount;

    private Date nextRetryAt;

    /**
     * 抢占锁：避免多实例重复发送
     */
    private Date lockedUntil;

    private String lastError;

    private Date createdAt;

    private Date updatedAt;

    private Date sentAt;

    /**
     * 用于 TTL 清理：仅在 SENT 时设置 expireAt，然后对 expireAt 建 TTL 索引（可选，后续再加）。
     */
    private Date expireAt;

    public static MqOutboxEvent newPending(String bizKey, String topic, String payload, int maxRetryCount) {
        Date now = new Date();
        MqOutboxEvent e = new MqOutboxEvent();
        e.setBizKey(bizKey);
        e.setTopic(topic);
        e.setPayload(payload);
        e.setStatus(OutboxStatus.PENDING.getCode());
        e.setRetryCount(0);
        e.setMaxRetryCount(maxRetryCount);
        e.setNextRetryAt(now);
        e.setLockedUntil(null);
        e.setLastError(null);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }
}

