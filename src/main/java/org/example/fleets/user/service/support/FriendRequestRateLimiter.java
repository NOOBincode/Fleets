package org.example.fleets.user.service.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.config.properties.FleetsProperties;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 好友申请频控模板（固定窗口计数 + 成功申请最小间隔）。
 *  
 *    {@link #check*} 只读 Redis，不在失败请求上消耗额度。 
 *    {@link #record*} 在业务写库<strong>成功之后</strong>调用，才真正 increment。 
 *    申请与撤销共用同一窗口计数（符合「pair 维度刷量」）。 
 *  
 * 开关：{@code fleets.friendship.rate-limit-enabled=true}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestRateLimiter {

    public static final String KEY_PREFIX_PAIR_ACTION = "friend:req:pair:";

    private final RedissonClient redissonClient;
    private final FleetsProperties fleetsProperties;

    /**
     * 写库前调用：最小申请间隔 + 当前窗口内已成功动作次数是否已达上限（不含本次）。
     */
    public void checkApplyAllowed(long userId, long targetUserId) {
        if (!rateLimitEnabled()) {
            return;
        }
        FleetsProperties.FriendshipConfig cfg = fleetsProperties.getFriendship();
        String base = pairKey(userId, targetUserId);

        assertMinIntervalSinceLastApply(base, cfg);

        long window = currentWindowId(cfg.getPairActionWindowSeconds());
        String windowKey = windowCounterKey(base, window);
        long count = redissonClient.getAtomicLong(windowKey).get();
        if (count >= cfg.getPairActionMaxCount()) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_RATE_LIMITED);
        }
    }

    /**
     * addFriend 成功（含仅更新 pending）后调用：窗口 +1，并刷新「上次成功申请」时间。
     */
    public void recordApplyAction(long userId, long targetUserId) {
        if (!rateLimitEnabled()) {
            return;
        }
        FleetsProperties.FriendshipConfig cfg = fleetsProperties.getFriendship();
        String base = pairKey(userId, targetUserId);

        bumpWindowCounter(base, cfg.getPairActionWindowSeconds());
        touchLastApplyMillis(base, System.currentTimeMillis());
    }

    /**
     * cancel 写库前：与申请共用窗口计数，不校验 minInterval（可按产品改为共用）。
     */
    public void checkCancelAllowed(long userId, long targetUserId) {
        if (!rateLimitEnabled()) {
            return;
        }
        FleetsProperties.FriendshipConfig cfg = fleetsProperties.getFriendship();
        String base = pairKey(userId, targetUserId);
        long window = currentWindowId(cfg.getPairActionWindowSeconds());
        String windowKey = windowCounterKey(base, window);
        long count = redissonClient.getAtomicLong(windowKey).get();
        if (count >= cfg.getPairActionMaxCount()) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_RATE_LIMITED);
        }
    }

    /**
     * cancel 成功后：与申请共用窗口计数。
     */
    public void recordCancelAction(long userId, long targetUserId) {
        if (!rateLimitEnabled()) {
            return;
        }
        FleetsProperties.FriendshipConfig cfg = fleetsProperties.getFriendship();
        String base = pairKey(userId, targetUserId);
        bumpWindowCounter(base, cfg.getPairActionWindowSeconds());
    }

    public String pairKey(long userId, long targetUserId) {
        long a = Math.min(userId, targetUserId);
        long b = Math.max(userId, targetUserId);
        return KEY_PREFIX_PAIR_ACTION + a + ":" + b;
    }

    private boolean rateLimitEnabled() {
        FleetsProperties.FriendshipConfig cfg = fleetsProperties.getFriendship();
        return cfg != null && cfg.isRateLimitEnabled();
    }

    private static long currentWindowId(int windowSeconds) {
        long sec = Math.max(1, windowSeconds);
        return System.currentTimeMillis() / (sec * 1000L);
    }

    private static String windowCounterKey(String pairBase, long windowId) {
        return pairBase + ":win:" + windowId;
    }

    private static String lastApplyKey(String pairBase) {
        return pairBase + ":lastApplyMs";
    }

    private void assertMinIntervalSinceLastApply(String pairBase, FleetsProperties.FriendshipConfig cfg) {
        int minSec = cfg.getMinIntervalSecondsBetweenApplies();
        if (minSec <= 0) {
            return;
        }
        RBucket<String> bucket = redissonClient.getBucket(lastApplyKey(pairBase));
        String raw = bucket.get();
        if (raw == null) {
            return;
        }
        try {
            long last = Long.parseLong(raw);
            long elapsed = System.currentTimeMillis() - last;
            if (elapsed < minSec * 1000L) {
                throw new BusinessException(ErrorCode.FRIEND_REQUEST_RATE_LIMITED);
            }
        } catch (NumberFormatException e) {
            log.warn("FriendRequestRateLimiter: ignore corrupt lastApplyMs for {}", pairBase);
        }
    }

    private void touchLastApplyMillis(String pairBase, long nowMillis) {
        RBucket<String> bucket = redissonClient.getBucket(lastApplyKey(pairBase));
        bucket.set(Long.toString(nowMillis));
    }

    private void bumpWindowCounter(String pairBase, int windowSeconds) {
        long window = currentWindowId(windowSeconds);
        String windowKey = windowCounterKey(pairBase, window);
        RAtomicLong atomic = redissonClient.getAtomicLong(windowKey);
        long v = atomic.incrementAndGet();
        if (v == 1L) {
            long sec = Math.max(1, windowSeconds);
            // 略长于窗口，避免边界时刻 TTL 先到期导致计数异常；可按需改为 sec 或 sec+1
            atomic.expire(Duration.ofSeconds(sec + 5));
        }
    }
}
