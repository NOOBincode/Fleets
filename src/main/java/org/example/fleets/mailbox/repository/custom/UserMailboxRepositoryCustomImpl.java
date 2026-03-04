package org.example.fleets.mailbox.repository.custom;

import lombok.RequiredArgsConstructor;
import org.example.fleets.mailbox.model.entity.UserMailbox;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;

/**
 * 用户信箱 Repository 自定义实现（原子 $inc 未读数）
 * 由 Spring Data 按约定 [Fragment接口名]Impl 在 repository 包及子包下自动发现。
 */
@RequiredArgsConstructor
public class UserMailboxRepositoryCustomImpl implements UserMailboxRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public long decrementUnreadCountIfPositive(Long userId, String conversationId) {
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("conversationId").is(conversationId)
                        .and("unreadCount").gt(0)
        );
        Update update = new Update()
                .inc("unreadCount", -1)
                .set("updateTime", new Date());
        return mongoTemplate.updateFirst(query, update, UserMailbox.class).getModifiedCount();
    }
}
