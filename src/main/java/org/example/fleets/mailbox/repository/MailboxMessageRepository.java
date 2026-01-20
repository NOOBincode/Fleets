package org.example.fleets.mailbox.repository;

import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 信箱消息Repository - 同步版本
 */
@Repository
public interface MailboxMessageRepository extends MongoRepository<MailboxMessage, String> {
    
    /**
     * 根据用户ID和会话ID查询消息（序列号大于指定值）
     */
    List<MailboxMessage> findByUserIdAndConversationIdAndSequenceGreaterThan(
        Long userId, 
        String conversationId, 
        Long sequence,
        Pageable pageable
    );
    
    /**
     * 根据用户ID和状态查询消息
     */
    List<MailboxMessage> findByUserIdAndStatus(Long userId, Integer status);
    
    /**
     * 根据用户ID、会话ID和序列号查询消息
     */
    Optional<MailboxMessage> findByUserIdAndConversationIdAndSequence(
        Long userId, 
        String conversationId, 
        Long sequence
    );
    
    /**
     * 删除过期消息
     */
    void deleteByStatusAndCreateTimeBefore(Integer status, Date createTime);
    
    /**
     * 统计未读消息数
     */
    long countByUserIdAndStatus(Long userId, Integer status);
    
    /**
     * 统计会话未读数
     */
    long countByUserIdAndConversationIdAndStatus(Long userId, String conversationId, Integer status);
}
