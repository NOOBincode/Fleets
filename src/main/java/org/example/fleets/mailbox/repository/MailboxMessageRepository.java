package org.example.fleets.mailbox.repository;

import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

/**
 * 信箱消息Repository
 */
@Repository
public interface MailboxMessageRepository extends ReactiveMongoRepository<MailboxMessage, String> {
    
    /**
     * 根据用户ID和会话ID查询消息（序列号大于指定值）
     */
    Flux<MailboxMessage> findByUserIdAndConversationIdAndSequenceGreaterThan(
        Long userId, 
        String conversationId, 
        Long sequence,
        Pageable pageable
    );
    
    /**
     * 根据用户ID和状态查询消息
     */
    Flux<MailboxMessage> findByUserIdAndStatus(Long userId, Integer status);
    
    /**
     * 根据用户ID、会话ID和序列号查询消息
     */
    Mono<MailboxMessage> findByUserIdAndConversationIdAndSequence(
        Long userId, 
        String conversationId, 
        Long sequence
    );
    
    /**
     * 删除过期消息
     */
    Flux<MailboxMessage> deleteByStatusAndCreateTimeBefore(Integer status, Date createTime);
}
