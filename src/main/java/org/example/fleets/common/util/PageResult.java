package org.example.fleets.common.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果封装类
 * 简化版本，使用Lombok减少样板代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    
    private long total;       // 总记录数
    private List<T> records;  // 数据列表
    private int pageNum;      // 当前页码
    private int pageSize;     // 每页记录数
    
    /**
     * 获取总页数
     */
    public int getTotalPages() {
        return pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
    }
    
    /**
     * 静态工厂方法
     */
    public static <T> PageResult<T> of(long total, List<T> records, int pageNum, int pageSize) {
        return new PageResult<>(total, records, pageNum, pageSize);
    }
    
    /**
     * 从MyBatis-Plus的Page对象转换
     */
    public static <T> PageResult<T> fromPage(Page<T> page) {
        return new PageResult<>(page.getTotal(), page.getRecords(), 
            (int) page.getCurrent(), (int) page.getSize());
    }
    
    /**
     * 空结果
     */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return new PageResult<>(0, Collections.emptyList(), pageNum, pageSize);
    }
}
