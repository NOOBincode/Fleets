package org.example.fleets.message.repository;

import org.example.fleets.message.model.entity.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息Repository（同步）
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    /**
     * 根据发送者和接收者查询消息
     */
    List<Message> findBySenderIdAndReceiverIdOrderBySendTimeDesc(Long senderId, Long receiverId);

    /**
     * 根据群组ID查询消息
     */
    List<Message> findByGroupIdOrderBySendTimeDesc(Long groupId);

    /**
     * 根据接收者ID和状态查询消息
     */
    List<Message> findByReceiverIdAndStatus(Long receiverId, Integer status);
}
