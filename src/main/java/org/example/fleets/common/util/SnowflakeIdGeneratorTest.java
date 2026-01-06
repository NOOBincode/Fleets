package org.example.fleets.common.util;

/**
 * 雪花算法测试类
 * 
 * 运行此类可以测试雪花算法的性能和正确性
 */
public class SnowflakeIdGeneratorTest {
    
    public static void main(String[] args) {
        // 创建雪花算法生成器
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        
        System.out.println("========================================");
        System.out.println("雪花算法ID生成器测试");
        System.out.println("========================================\n");
        
        // 测试1：生成10个ID
        System.out.println("测试1：生成10个ID");
        System.out.println("----------------------------------------");
        for (int i = 0; i < 10; i++) {
            long id = generator.nextId();
            System.out.println("ID " + (i + 1) + ": " + id);
            System.out.println("解析: " + generator.parseId(id));
            System.out.println();
        }
        
        // 测试2：性能测试
        System.out.println("\n测试2：性能测试（生成100万个ID）");
        System.out.println("----------------------------------------");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            generator.nextId();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("生成100万个ID耗时: " + (endTime - startTime) + "ms");
        System.out.println("平均每秒生成: " + (1000000 * 1000 / (endTime - startTime)) + " 个ID");
        
        // 测试3：唯一性测试
        System.out.println("\n测试3：唯一性测试（生成10000个ID）");
        System.out.println("----------------------------------------");
        java.util.Set<Long> idSet = new java.util.HashSet<>();
        for (int i = 0; i < 10000; i++) {
            long id = generator.nextId();
            if (idSet.contains(id)) {
                System.out.println("❌ 发现重复ID: " + id);
                return;
            }
            idSet.add(id);
        }
        System.out.println("✅ 10000个ID全部唯一");
        
        // 测试4：趋势递增测试
        System.out.println("\n测试4：趋势递增测试");
        System.out.println("----------------------------------------");
        long prevId = generator.nextId();
        boolean isIncreasing = true;
        for (int i = 0; i < 1000; i++) {
            long currentId = generator.nextId();
            if (currentId <= prevId) {
                isIncreasing = false;
                System.out.println("❌ ID不是递增的: " + prevId + " -> " + currentId);
                break;
            }
            prevId = currentId;
        }
        if (isIncreasing) {
            System.out.println("✅ 1000个ID全部递增");
        }
        
        System.out.println("\n========================================");
        System.out.println("测试完成！");
        System.out.println("========================================");
    }
}
