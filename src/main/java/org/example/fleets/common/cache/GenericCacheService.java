package org.example.fleets.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 通用缓存服务
 * 基于 Redisson 提供统一的缓存操作接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenericCacheService {

    private final RedissonClient redissonClient;

    public <T> void set(String key, T value) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    public <T> void set(String key, T value, long ttl, TimeUnit unit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, ttl, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public String getString(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    public long delete(String... keys) {
        return redissonClient.getKeys().delete(keys);
    }

    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    public boolean expire(String key, long ttl, TimeUnit unit) {
        return redissonClient.getBucket(key).expire(Duration.ofMillis(unit.toMillis(ttl)));
    }

    public long getExpire(String key) {
        return redissonClient.getBucket(key).remainTimeToLive() / 1000;
    }

    public <T> boolean setIfAbsent(String key, T value, long ttl, TimeUnit unit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.trySet(value, ttl, unit);
    }

    public <T> void cacheEntity(String prefix, Long id, T entity, long ttl, TimeUnit unit) {
        set(prefix + id, entity, ttl, unit);
    }

    public <T> T getEntity(String prefix, Long id) {
        return get(prefix + id);
    }

    public void deleteEntity(String prefix, Long id) {
        delete(prefix + id);
    }

    public long increment(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.incrementAndGet();
    }

    public long increment(String key, long delta) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.addAndGet(delta);
    }

    public long decrement(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.decrementAndGet();
    }

    public Long getAtomicLong(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.isExists() ? atomicLong.get() : null;
    }

    public <T> RSet<T> getSet(String key) {
        return redissonClient.getSet(key);
    }

    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(key);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁被中断: key={}", key, e);
            return false;
        }
    }

    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public void unlock(String key) {
        RLock lock = getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
