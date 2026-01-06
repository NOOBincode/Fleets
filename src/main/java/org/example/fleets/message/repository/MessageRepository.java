package org.example.fleets.message.repository;

import org.example.fleets.message.model.entity.Message;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * 消息Repository
 */
@Repository
public interface MessageRepository extends ReactiveMongoRepository<Message, String> {
    
    /**
     * 根据发送者和接收者查询消息
     */
    Flux<Message> findBySenderIdAndReceiverIdOrderBySendTimeDesc(Long senderId, Long receiverId);
    
    /**
     * 根据群组ID查询消息
     */
    Flux<Message> findByGroupIdOrderBySendTimeDesc(Long groupId);
    
    /**
     * 根据接收者ID和状态查询消息
     */
    Flux<Message> findByReceiverIdAndStatus(Long receiverId, Integer status);
}
