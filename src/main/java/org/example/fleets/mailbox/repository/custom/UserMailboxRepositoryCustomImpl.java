package org.example.fleets.mailbox.repository.custom;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.example.fleets.mailbox.model.entity.UserMailbox;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;
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

    @Override
    public long decrementUnreadCountByIfEnough(Long userId, String conversationId, long delta) {
        if (delta <= 0) {
            return 0;
        }
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("conversationId").is(conversationId)
                        .and("unreadCount").gte(delta)
        );
        Update update = new Update()
                .inc("unreadCount", -delta)
                .set("updateTime", new Date());
        return mongoTemplate.updateFirst(query, update, UserMailbox.class).getModifiedCount();
    }

    @Override
    public long resetUnreadCountToZero(Long userId, String conversationId) {
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("conversationId").is(conversationId)
                        .and("unreadCount").gt(0)
        );
        Update update = new Update()
                .set("unreadCount", 0)
                .set("updateTime", new Date());
        return mongoTemplate.updateFirst(query, update, UserMailbox.class).getModifiedCount();
    }

    @Override
    public long decrementOrResetUnreadCount(Long userId, String conversationId, long delta) {
        if (delta <= 0) {
            return 0;
        }
        
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("conversationId").is(conversationId)
                        .and("unreadCount").gt(0)
        );
        
        Document maxExpr = new Document("$max", Arrays.asList(
                new Document("$subtract", Arrays.asList("$unreadCount", delta)),
                0
        ));
        
        Update update = new Update()
                .set("unreadCount", maxExpr)
                .set("updateTime", new Date());
        
        UpdateResult result = mongoTemplate.updateFirst(query, update, UserMailbox.class);
        return result.getModifiedCount();
    }
}
