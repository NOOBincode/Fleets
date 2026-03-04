package org.example.fleets.storage.mongodb;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB配置类（同步 Repository）
 */
@Configuration
@EnableMongoRepositories(basePackages = "org.example.fleets")
@EnableMongoAuditing
public class MongoConfig {
    // Spring Boot 会自动创建以下 Bean:
    // - MongoClient (同步客户端)
    // - MongoTemplate (同步模板)
    // - ReactiveMongoClient (响应式客户端)
    // - ReactiveMongoTemplate (响应式模板)
}
