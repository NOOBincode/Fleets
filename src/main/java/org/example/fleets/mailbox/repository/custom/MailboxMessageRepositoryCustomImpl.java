package org.example.fleets.mailbox.repository.custom;

import lombok.RequiredArgsConstructor;
import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

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

    @Override
    public long markAsReadUpToSequenceIfUnread(Long userId, String conversationId, Long toSequence, Date readTime) {
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("conversationId").is(conversationId)
                        .and("sequence").lte(toSequence)
                        .and("status").is(0)
        );
        Update update = new Update()
                .set("status", 1)
                .set("readTime", readTime);
        return mongoTemplate.updateMulti(query, update, MailboxMessage.class).getModifiedCount();
    }

    @Override
    public long markAsDeletedByConversation(Long userId, String conversationId, Date updateTime) {
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("conversationId").is(conversationId)
                        .and("status").ne(2)
        );
        // 软删除：仅依赖 status=2，保持最小改动
        Update update = new Update().set("status", 2);
        return mongoTemplate.updateMulti(query, update, MailboxMessage.class).getModifiedCount();
    }

    @Override
    public Page<MailboxMessage> searchByUserIdAndKeyword(Long userId, String keywordLiteral, Pageable pageable) {
        String safe = Pattern.quote(keywordLiteral);
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("status").ne(2)
                        .and("content").regex(safe, "i")
        );
        long total = mongoTemplate.count(query, MailboxMessage.class);
        query.with(pageable);
        List<MailboxMessage> list = mongoTemplate.find(query, MailboxMessage.class);
        return new PageImpl<>(list, pageable, total);
    }
}
