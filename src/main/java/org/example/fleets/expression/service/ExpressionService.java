package org.example.fleets.expression.service;

import org.example.fleets.expression.model.vo.ExpressionCategoryVO;
import org.example.fleets.expression.model.vo.ExpressionVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 表情包服务接口
 */
public interface ExpressionService {
    
    /**
     * 获取表情包列表（按分类）
     */
    List<ExpressionCategoryVO> getExpressionList(Long userId);
    
    /**
     * 上传自定义表情包
     */
    ExpressionVO uploadExpression(Long userId, MultipartFile file);
    
    /**
     * 删除自定义表情包
     */
    boolean deleteExpression(Long userId, Long expressionId);
}
