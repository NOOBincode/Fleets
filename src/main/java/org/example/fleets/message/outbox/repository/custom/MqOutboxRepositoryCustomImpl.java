package org.example.fleets.message.outbox.repository.custom;

import lombok.RequiredArgsConstructor;
import org.example.fleets.message.outbox.model.entity.MqOutboxEvent;
import org.example.fleets.message.outbox.model.enums.OutboxStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 通过 findAndModify 循环 claim，保证多实例下不重复处理同一条事件。
 */
@RequiredArgsConstructor
public class MqOutboxRepositoryCustomImpl implements MqOutboxRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<MqOutboxEvent> claimRetryBatch(int limit, int lockSeconds) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        int safeLockSeconds = Math.max(5, Math.min(lockSeconds, 300));

        Date now = new Date();
        Date lockedUntil = new Date(now.getTime() + safeLockSeconds * 1000L);

        List<MqOutboxEvent> claimed = new ArrayList<>();
        for (int i = 0; i < safeLimit; i++) {
            Query query = Query.query(
                            Criteria.where("status").in(OutboxStatus.PENDING.getCode(), OutboxStatus.FAILED.getCode())
                                    .and("nextRetryAt").lte(now)
                                    .andOperator(new Criteria().orOperator(
                                            Criteria.where("lockedUntil").exists(false),
                                            Criteria.where("lockedUntil").lte(now)
                                    ))
                    )
                    .with(Sort.by(Sort.Direction.ASC, "nextRetryAt", "createdAt"));

            Update update = new Update()
                    .set("status", OutboxStatus.SENDING.getCode())
                    .set("lockedUntil", lockedUntil)
                    .set("updatedAt", now);

            MqOutboxEvent one = mongoTemplate.findAndModify(
                    query,
                    update,
                    FindAndModifyOptions.options().returnNew(true),
                    MqOutboxEvent.class
            );
            if (one == null) {
                break;
            }
            claimed.add(one);
        }
        return claimed;
    }

    @Override
    public void markSent(String id) {
        Date now = new Date();
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("status", OutboxStatus.SENT.getCode())
                .set("sentAt", now)
                .set("lockedUntil", null)
                .set("updatedAt", now);
        mongoTemplate.updateFirst(query, update, MqOutboxEvent.class);
    }

    @Override
    public void markFailed(String id, int retryCount, boolean dead, long nextRetryDelayMs, String lastError) {
        Date now = new Date();
        long delay = Math.max(0L, nextRetryDelayMs);
        Date nextRetryAt = new Date(now.getTime() + delay);

        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("status", dead ? OutboxStatus.DEAD.getCode() : OutboxStatus.FAILED.getCode())
                .set("retryCount", retryCount)
                .set("nextRetryAt", nextRetryAt)
                .set("lockedUntil", null)
                .set("lastError", lastError)
                .set("updatedAt", now);
        mongoTemplate.updateFirst(query, update, MqOutboxEvent.class);
    }
}

