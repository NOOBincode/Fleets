package org.example.fleets.mailbox.repository.custom;

import lombok.RequiredArgsConstructor;
import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;

/**
 * 信箱消息 Repository 自定义实现（MongoTemplate 条件更新）
 * 由 Spring Data 按约定 [Fragment接口名]Impl 在 repository 包及子包下自动发现。
 */
@RequiredArgsConstructor
public class MailboxMessageRepositoryCustomImpl implements MailboxMessageRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public long markAsReadIfUnread(Long userId, String conversationId, Long sequence, Date readTime) {
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("conversationId").is(conversationId)
                        .and("sequence").is(sequence)
                        .and("status").is(0)
        );
        Update update = new Update()
                .set("status", 1)
                .set("readTime", readTime);
        return mongoTemplate.updateFirst(query, update, MailboxMessage.class).getModifiedCount();
    }
}
