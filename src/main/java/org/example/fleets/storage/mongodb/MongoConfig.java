package org.example.fleets.storage.mongodb;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * MongoDB配置类
 */
@Configuration
@EnableReactiveMongoRepositories(basePackages = "org.example.fleets.**.repository")
@EnableMongoAuditing
public class MongoConfig {
    // MongoDB配置
}
