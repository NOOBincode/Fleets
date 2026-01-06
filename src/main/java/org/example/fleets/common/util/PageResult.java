package org.example.fleets.common.util;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.List;

public class PageResult<T> {
        private long total;       // 总记录数
        private List<T> records; // 数据列表
        private int pageNum;     // 当前页码
        private int pageSize;    // 每页记录数
        private int totalPages;  // 总页数

        public PageResult() {
        }

        public PageResult(long total, List<T> records, int pageNum, int pageSize) {
            this.total = total;
            this.records = records;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) total / pageSize);
        }

        // Getters and Setters
        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public List<T> getRecords() {
            return records;
        }

        public void setRecords(List<T> records) {
            this.records = records;
        }

        public int getPageNum() {
            return pageNum;
        }

        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        // 静态工厂方法，方便创建
        public static <T> PageResult<T> of(long total, List<T> records, int pageNum, int pageSize) {
            return new PageResult<>(total, records, pageNum, pageSize);
        }

        // 从MyBatis-Plus的Page对象转换
        public static <T> PageResult<T> fromPage(Page<T> page) {
            return new PageResult<>(page.getTotal(), page.getRecords(), (int)page.getCurrent(), (int)page.getSize());
        }

        // 空结果
        public static <T> PageResult<T> empty(int pageNum, int pageSize) {
            return new PageResult<>(0, Collections.emptyList(), pageNum, pageSize);
        }
    }
