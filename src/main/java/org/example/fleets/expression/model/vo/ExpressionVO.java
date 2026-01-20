package org.example.fleets.expression.model.vo;

import lombok.Data;

/**
 * 表情包VO
 */
@Data
public class ExpressionVO {
    
    /**
     * 表情ID
     */
    private Long id;
    
    /**
     * 表情名称
     */
    private String name;
    
    /**
     * 表情图片URL
     */
    private String url;
    
    /**
     * 分类
     */
    private String category;
    
    /**
     * 排序
     */
    private Integer sort;
}
