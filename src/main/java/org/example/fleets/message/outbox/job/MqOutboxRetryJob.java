package org.example.fleets.message.outbox.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.message.outbox.model.entity.MqOutboxEvent;
import org.example.fleets.message.outbox.repository.MqOutboxRepository;
import org.example.fleets.message.producer.MessageProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 定时投递/重试
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqOutboxRetryJob {

    private final MqOutboxRepository mqOutboxRepository;
    private final MessageProducer messageProducer;

    // 简化版配置：后续可放到 FleetsProperties
    private static final int BATCH_SIZE = 50;
    private static final int LOCK_SECONDS = 30;
    private static final int BASE_BACKOFF_MS = 1000;
    private static final int MAX_BACKOFF_MS = 60_000;

    @Scheduled(fixedDelay = 3000)
    public void retry() {
        List<MqOutboxEvent> batch = mqOutboxRepository.claimRetryBatch(BATCH_SIZE, LOCK_SECONDS);
        if (batch.isEmpty()) {
            return;
        }

        for (MqOutboxEvent e : batch) {
            try {
                // payload 是 JSON 字符串；consumer 侧消费 String
                messageProducer.sendMessage(e.getTopic(), e.getPayload());
                mqOutboxRepository.markSent(e.getId());
            } catch (Exception ex) {
                int nextRetry = (e.getRetryCount() == null ? 0 : e.getRetryCount()) + 1;
                int maxRetry = e.getMaxRetryCount() == null ? 10 : e.getMaxRetryCount();
                boolean dead = nextRetry >= maxRetry;
                long delay = dead ? MAX_BACKOFF_MS : calcBackoff(nextRetry);
                mqOutboxRepository.markFailed(e.getId(), nextRetry, dead, delay, shrink(ex));
            }
        }
    }

    private long calcBackoff(int retryCount) {
        // 指数退避：base * 2^(retryCount-1)，并限制上限
        long factor = 1L << Math.max(0, Math.min(retryCount - 1, 20));
        long delay = (long) BASE_BACKOFF_MS * factor;
        return Math.min(delay, MAX_BACKOFF_MS);
    }

    private String shrink(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            msg = ex.getClass().getSimpleName();
        }
        if (msg.length() > 500) {
            return msg.substring(0, 500);
        }
        return msg;
    }
}

