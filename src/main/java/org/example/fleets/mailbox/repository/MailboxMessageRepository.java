package org.example.fleets.mailbox.repository;

import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.example.fleets.mailbox.repository.custom.MailboxMessageRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 信箱消息 Repository（Spring Data 方法 + custom 条件更新）
 */
@Repository
public interface MailboxMessageRepository extends MongoRepository<MailboxMessage, String>, MailboxMessageRepositoryCustom {
    
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
     * 根据用户ID和消息ID查询信箱消息（用于 messageId 转 conversationId+sequence）
     */
    Optional<MailboxMessage> findByUserIdAndMessageId(Long userId, String messageId);

    /**
     * 根据消息ID查询所有信箱消息（撤回时批量更新）
     */
    List<MailboxMessage> findByMessageId(String messageId);

    /**
     * 分页查询会话消息（按序列号倒序，最新在前）
     */
    Page<MailboxMessage> findByUserIdAndConversationIdOrderBySequenceDesc(
        Long userId, String conversationId, Pageable pageable);
    
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
