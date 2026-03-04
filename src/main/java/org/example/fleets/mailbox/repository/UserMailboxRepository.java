package org.example.fleets.mailbox.repository;

import org.example.fleets.mailbox.model.entity.UserMailbox;
import org.example.fleets.mailbox.repository.custom.UserMailboxRepositoryCustom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户信箱 Repository（Spring Data 方法 + custom 原子更新）
 */
@Repository
public interface UserMailboxRepository extends MongoRepository<UserMailbox, String>, UserMailboxRepositoryCustom {
    
    /**
     * 根据用户ID和会话ID查询信箱
     */
    Optional<UserMailbox> findByUserIdAndConversationId(Long userId, String conversationId);
    
    /**
     * 根据用户ID查询所有信箱
     */
    List<UserMailbox> findByUserId(Long userId);
    
    /**
     * 根据用户ID和会话类型查询信箱
     */
    List<UserMailbox> findByUserIdAndConversationType(Long userId, Integer conversationType);
}
