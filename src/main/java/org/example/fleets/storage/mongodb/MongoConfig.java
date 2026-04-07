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
    // Spring Boot 会自动创建 MongoClient、MongoTemplate（同步）
}
