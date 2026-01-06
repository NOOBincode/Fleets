package org.example.fleets.common.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 * 
 * 雪花算法生成的ID是一个64位的Long类型数字，结构如下：
 * 0 - 41位时间戳 - 10位机器ID - 12位序列号
 * 
 * 优点：
 * 1. 高性能：每秒可生成400万个ID
 * 2. 趋势递增：ID按时间递增，有利于数据库索引
 * 3. 分布式：不同机器生成的ID不会重复
 * 4. 无需数据库：不依赖数据库，性能更好
 */
@Component
public class SnowflakeIdGenerator {
    
    // ==============================常量==============================
    
    /** 开始时间戳 (2024-01-01 00:00:00) */
    private final long twepoch = 1704067200000L;
    
    /** 机器ID所占的位数 */
    private final long workerIdBits = 5L;
    
    /** 数据中心ID所占的位数 */
    private final long datacenterIdBits = 5L;
    
    /** 支持的最大机器ID，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数) */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    
    /** 支持的最大数据中心ID，结果是31 */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    
    /** 序列在ID中占的位数 */
    private final long sequenceBits = 12L;
    
    /** 机器ID向左移12位 */
    private final long workerIdShift = sequenceBits;
    
    /** 数据中心ID向左移17位(12+5) */
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    
    /** 时间戳向左移22位(5+5+12) */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    
    /** 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095) */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);
    
    // ==============================字段==============================
    
    /** 工作机器ID(0~31) */
    private long workerId;
    
    /** 数据中心ID(0~31) */
    private long datacenterId;
    
    /** 毫秒内序列(0~4095) */
    private long sequence = 0L;
    
    /** 上次生成ID的时间戳 */
    private long lastTimestamp = -1L;
    
    // ==============================构造函数==============================
    
    /**
     * 构造函数
     * 默认使用机器ID=1, 数据中心ID=1
     */
    public SnowflakeIdGenerator() {
        this(1L, 1L);
    }
    
    /**
     * 构造函数
     * 
     * @param workerId 工作ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(
                String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    
    // ==============================方法==============================
    
    /**
     * 获得下一个ID (线程安全)
     * 
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                String.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
                    lastTimestamp - timestamp));
        }
        
        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }
        
        // 上次生成ID的时间戳
        lastTimestamp = timestamp;
        
        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift)
            | (datacenterId << datacenterIdShift)
            | (workerId << workerIdShift)
            | sequence;
    }
    
    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * 
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    
    /**
     * 返回以毫秒为单位的当前时间
     * 
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }
    
    /**
     * 解析雪花ID
     * 
     * @param id 雪花ID
     * @return 包含时间戳、数据中心ID、机器ID、序列号的字符串
     */
    public String parseId(long id) {
        long timestamp = (id >> timestampLeftShift) + twepoch;
        long datacenterId = (id >> datacenterIdShift) & maxDatacenterId;
        long workerId = (id >> workerIdShift) & maxWorkerId;
        long sequence = id & sequenceMask;
        
        return String.format(
            "ID: %d, Timestamp: %d, DatacenterId: %d, WorkerId: %d, Sequence: %d",
            id, timestamp, datacenterId, workerId, sequence);
    }
}
