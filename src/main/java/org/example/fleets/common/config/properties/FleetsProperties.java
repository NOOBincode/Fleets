package org.example.fleets.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Fleets 应用配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "fleets")
public class FleetsProperties {
    
    /**
     * WebSocket 配置
     */
    private WebSocketConfig websocket = new WebSocketConfig();
    
    /**
     * Mailbox 配置
     */
    private MailboxConfig mailbox = new MailboxConfig();
    
    /**
     * 消息配置
     */
    private MessageConfig message = new MessageConfig();
    
    /**
     * Redis 配置
     */
    private RedisConfig redis = new RedisConfig();
    
    /**
     * WebSocket 配置
     */
    @Data
    public static class WebSocketConfig {
        /**
         * 在线状态过期时间（秒）
         */
        private long onlineExpireSeconds = 300;
        
        /**
         * 心跳间隔（秒）
         */
        private long heartbeatInterval = 30;
        
        /**
         * 允许的跨域源
         */
        private String allowedOrigins = "*";
        
        /**
         * WebSocket 端点路径
         */
        private String endpoint = "/ws";
        
        /**
         * 应用目标前缀
         */
        private String applicationDestinationPrefix = "/app";
        
        /**
         * 用户目标前缀
         */
        private String userDestinationPrefix = "/user";
    }
    
    /**
     * Mailbox 配置
     */
    @Data
    public static class MailboxConfig {
        /**
         * 消息过期天数
         */
        private int messageExpireDays = 7;
        
        /**
         * 最大未读数
         */
        private int maxUnreadCount = 1000;
        
        /**
         * 单次拉取消息数量
         */
        private int pullMessageLimit = 100;
        
        /**
         * 未读数缓存时间（分钟）
         */
        private int unreadCountCacheMinutes = 5;
        
        /**
         * 是否启用消息过期自动清理
         */
        private boolean enableAutoCleanup = true;
    }
    
    /**
     * 消息配置
     */
    @Data
    public static class MessageConfig {
        /**
         * 消息内容最大长度
         */
        private int maxContentLength = 5000;
        
        /**
         * 是否启用内容过滤
         */
        private boolean enableContentFilter = true;
        
        /**
         * 发送频率限制（每分钟）
         */
        private int sendRateLimit = 60;
        
        /**
         * 批量发送最大数量
         */
        private int batchSendLimit = 500;
    }
    
    /**
     * Redis 配置
     */
    @Data
    public static class RedisConfig {
        /**
         * 在线状态 Key 前缀
         */
        private String onlineKeyPrefix = "user:online:";
        
        /**
         * 会话 Key 前缀
         */
        private String sessionKeyPrefix = "user:session:";
        
        /**
         * 用户会话集合 Key 前缀
         */
        private String userSessionsKeyPrefix = "user:sessions:";
        
        /**
         * 序列号 Key 前缀
         */
        private String sequenceKeyPrefix = "mailbox:seq:";
        
        /**
         * 未读数缓存 Key 前缀
         */
        private String unreadCountKeyPrefix = "mailbox:unread:";
        
        /**
         * 序列号过期天数
         */
        private int sequenceExpireDays = 7;
    }
}
