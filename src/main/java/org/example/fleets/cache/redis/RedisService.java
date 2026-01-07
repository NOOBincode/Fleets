package org.example.fleets.cache.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis服务类 - 基于Redisson实现
 */
@Service
@RequiredArgsConstructor
public class RedisService {
    
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // ==================== 基础操作 ====================
    
    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }
    
    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value, Duration.ofMillis(unit.toMillis(timeout)));
    }
    
    /**
     * 获取缓存
     */
    public Object get(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }
    
    /**
     * 获取字符串缓存
     */
    public String getString(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }
    
    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }
    
    /**
     * 批量删除
     */
    public long delete(String... keys) {
        return redissonClient.getKeys().delete(keys);
    }
    
    /**
     * 判断key是否存在
     */
    public Boolean hasKey(String key) {
        return redissonClient.getBucket(key).isExists();
    }
    
    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(key).expire(Duration.ofMillis(unit.toMillis(timeout)));
    }
    
    /**
     * 获取过期时间（秒）
     */
    public Long getExpire(String key) {
        return redissonClient.getBucket(key).remainTimeToLive() / 1000;
    }
    
    // ==================== 分布式锁相关 ====================
    
    /**
     * 设置缓存（如果不存在）- 用于分布式锁
     * 
     * @param key 键
     * @param value 值
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true-设置成功，false-key已存在
     */
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent(value, Duration.ofMillis(unit.toMillis(timeout)));
    }
    
    /**
     * 获取分布式锁
     * 
     * @param lockKey 锁的key
     * @return RLock对象
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }
    
    /**
     * 尝试获取锁
     * 
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @return true-获取成功，false-获取失败
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 释放锁
     * 
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
    
    /**
     * 释放锁（安全版本）
     * 
     * @param lock 锁对象
     */
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
    
    // ==================== 计数器相关 ====================
    
    /**
     * 递增
     */
    public Long increment(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.incrementAndGet();
    }
    
    /**
     * 递增指定值
     */
    public Long increment(String key, long delta) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.addAndGet(delta);
    }
    
    /**
     * 递减
     */
    public Long decrement(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.decrementAndGet();
    }
    
    /**
     * 递减指定值
     */
    public Long decrement(String key, long delta) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.addAndGet(-delta);
    }
    
    // ==================== 集合操作 ====================
    
    /**
     * 获取Set集合
     */
    public <T> RSet<T> getSet(String key) {
        return redissonClient.getSet(key);
    }
    
    /**
     * 获取List集合
     */
    public <T> RList<T> getList(String key) {
        return redissonClient.getList(key);
    }
    
    /**
     * 获取Map
     */
    public <K, V> RMap<K, V> getMap(String key) {
        return redissonClient.getMap(key);
    }
    
    // ==================== 布隆过滤器 ====================
    
    /**
     * 获取布隆过滤器
     * 
     * @param name 过滤器名称
     * @return 布隆过滤器
     */
    public <T> RBloomFilter<T> getBloomFilter(String name) {
        return redissonClient.getBloomFilter(name);
    }
    
    /**
     * 初始化布隆过滤器
     * 
     * @param name 过滤器名称
     * @param expectedInsertions 预期插入数量
     * @param falseProbability 误判率
     * @return 布隆过滤器
     */
    public <T> RBloomFilter<T> createBloomFilter(String name, long expectedInsertions, double falseProbability) {
        RBloomFilter<T> bloomFilter = redissonClient.getBloomFilter(name);
        bloomFilter.tryInit(expectedInsertions, falseProbability);
        return bloomFilter;
    }
}
