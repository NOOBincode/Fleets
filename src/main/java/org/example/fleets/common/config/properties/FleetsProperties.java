package org.example.fleets.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * 好友申请（防刷、频控）配置；具体阈值在 {@link org.example.fleets.user.service.support.FriendRequestRateLimiter} 中消费。
     */
    private FriendshipConfig friendship = new FriendshipConfig();
    
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
         * 允许的跨域源（已弃用：SockJS 请用 allowedOriginPatterns）
         */
        @Deprecated
        private String allowedOrigins = "*";

        /**
         * SockJS /info 与 STOMP 握手允许的 Origin 模式（如 http://localhost:3000、*）
         */
        private List<String> allowedOriginPatterns = new ArrayList<>(Arrays.asList("*"));
        
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
         * 客户端上报的全局同步游标（兼容旧接口；精准同步请按会话调用 mailbox sync）
         */
        private String messageSyncLastSeqPrefix = "message:sync:lastSeq:";
        
        /**
         * 序列号过期天数
         */
        private int sequenceExpireDays = 7;
    }

    /**
     * 好友申请频控（与 Redis 键设计配合，实现时在 FriendRequestRateLimiter 内读取）。
     */
    @Data
    public static class FriendshipConfig {
        /**
         * 是否启用好友申请 Redis 频控（false 时 FriendRequestRateLimiter 应直接放行）。
         */
        private boolean rateLimitEnabled = true;

        /**
         * 滑动窗口长度（秒），统计「申请 + 撤销」合计次数时使用。
         */
        private int pairActionWindowSeconds = 600;

        /**
         * 上述窗口内允许的最大动作次数（申请成功、重复刷新 pending、撤销各算一次，具体以你实现为准）。
         */
        private int pairActionMaxCount = 20;

        /**
         * 两次「有效申请」之间的最短间隔（秒）；可与窗口策略二选一或叠加。
         */
        private int minIntervalSecondsBetweenApplies = 5;
    }
}
