package org.example.fleets.message.outbox.repository;

import org.example.fleets.message.outbox.model.entity.MqOutboxEvent;
import org.example.fleets.message.outbox.model.enums.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"
})
@DisplayName("Mongo Outbox Repository 集成测试（使用本地 Mongo）")
class MqOutboxRepositoryDataMongoTest {

    @Autowired
    private MqOutboxRepository mqOutboxRepository;

    @Test
    @DisplayName("claimRetryBatch：只抢占可重试事件并置为 SENDING + lockedUntil")
    void claimRetryBatch_Works() {
        mqOutboxRepository.deleteAll();

        MqOutboxEvent a = MqOutboxEvent.newPending("biz_a", "im-message-topic", "{\"id\":\"a\"}", 3);
        a.setNextRetryAt(new Date(System.currentTimeMillis() - 1000));
        a.setStatus(OutboxStatus.PENDING.getCode());

        MqOutboxEvent b = MqOutboxEvent.newPending("biz_b", "im-message-topic", "{\"id\":\"b\"}", 3);
        b.setNextRetryAt(new Date(System.currentTimeMillis() + 60_000)); // 未来，不应被 claim
        b.setStatus(OutboxStatus.PENDING.getCode());

        mqOutboxRepository.save(a);
        mqOutboxRepository.save(b);

        List<MqOutboxEvent> claimed = mqOutboxRepository.claimRetryBatch(10, 30);
        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getBizKey()).isEqualTo("biz_a");
        assertThat(claimed.get(0).getStatus()).isEqualTo(OutboxStatus.SENDING.getCode());
        assertThat(claimed.get(0).getLockedUntil()).isNotNull();

        MqOutboxEvent reloadedA = mqOutboxRepository.findByBizKey("biz_a")
                .orElseThrow(() -> new IllegalStateException("biz_a should exist"));
        assertThat(reloadedA.getStatus()).isEqualTo(OutboxStatus.SENDING.getCode());
        assertThat(reloadedA.getLockedUntil()).isNotNull();

        MqOutboxEvent reloadedB = mqOutboxRepository.findByBizKey("biz_b")
                .orElseThrow(() -> new IllegalStateException("biz_b should exist"));
        assertThat(reloadedB.getStatus()).isEqualTo(OutboxStatus.PENDING.getCode());
    }
}

