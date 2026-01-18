package org.example.fleets.mailbox.repository;

import org.example.fleets.mailbox.model.entity.UserMailbox;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 用户信箱Repository
 */
@Repository
public interface UserMailboxRepository extends ReactiveMongoRepository<UserMailbox, String> {
    
    /**
     * 根据用户ID和会话ID查询信箱
     */
    Mono<UserMailbox> findByUserIdAndConversationId(Long userId, String conversationId);
    
    /**
     * 根据用户ID查询所有信箱
     */
    Flux<UserMailbox> findByUserId(Long userId);
    
    /**
     * 根据用户ID和会话类型查询信箱
     */
    Flux<UserMailbox> findByUserIdAndConversationType(Long userId, Integer conversationType);
}
