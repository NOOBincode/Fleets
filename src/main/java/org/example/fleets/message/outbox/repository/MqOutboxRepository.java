package org.example.fleets.message.outbox.repository;

import org.example.fleets.message.outbox.model.entity.MqOutboxEvent;
import org.example.fleets.message.outbox.repository.custom.MqOutboxRepositoryCustom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MqOutboxRepository extends MongoRepository<MqOutboxEvent, String>, MqOutboxRepositoryCustom {

    Optional<MqOutboxEvent> findByBizKey(String bizKey);
}

