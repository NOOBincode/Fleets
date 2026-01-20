package org.example.fleets.expression.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 表情包分类VO
 */
@Data
public class ExpressionCategoryVO {
    
    /**
     * 分类名称
     */
    private String category;
    
    /**
     * 该分类下的表情列表
     */
    private List<ExpressionVO> expressions;
}
