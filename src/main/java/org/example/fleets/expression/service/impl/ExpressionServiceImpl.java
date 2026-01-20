package org.example.fleets.expression.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.expression.mapper.ExpressionMapper;
import org.example.fleets.expression.model.entity.Expression;
import org.example.fleets.expression.model.vo.ExpressionCategoryVO;
import org.example.fleets.expression.model.vo.ExpressionVO;
import org.example.fleets.expression.service.ExpressionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表情包服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressionServiceImpl implements ExpressionService {
    
    private final ExpressionMapper expressionMapper;
    
    /**
     * 获取表情包列表（按分类）
     */
    @Override
    public List<ExpressionCategoryVO> getExpressionList(Long userId) {
        log.info("获取表情包列表，userId: {}", userId);
        
        try {
            // 查询系统表情和用户自定义表情
            LambdaQueryWrapper<Expression> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> w.isNull(Expression::getUserId).or().eq(Expression::getUserId, userId))
                   .orderByAsc(Expression::getCategory)
                   .orderByAsc(Expression::getSort);
            
            List<Expression> expressions = expressionMapper.selectList(wrapper);
            
            // 按分类分组
            Map<String, List<Expression>> categoryMap = expressions.stream()
                    .collect(Collectors.groupingBy(Expression::getCategory));
            
            // 转换为VO
            List<ExpressionCategoryVO> result = new ArrayList<>();
            categoryMap.forEach((category, expList) -> {
                ExpressionCategoryVO categoryVO = new ExpressionCategoryVO();
                categoryVO.setCategory(category);
                
                List<ExpressionVO> expressionVOs = expList.stream()
                        .map(this::convertToVO)
                        .collect(Collectors.toList());
                
                categoryVO.setExpressions(expressionVOs);
                result.add(categoryVO);
            });
            
            return result;
            
        } catch (Exception e) {
            log.error("获取表情包列表异常，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取表情包列表失败");
        }
    }
    
    /**
     * 上传自定义表情包
     */
    @Override
    public ExpressionVO uploadExpression(Long userId, MultipartFile file) {
        log.info("上传自定义表情包，userId: {}", userId);
        
        // TODO: 实现文件上传逻辑
        // 1. 验证文件类型和大小
        // 2. 上传文件到存储服务
        // 3. 保存表情包记录
        
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "功能暂未实现");
    }
    
    /**
     * 删除自定义表情包
     */
    @Override
    public boolean deleteExpression(Long userId, Long expressionId) {
        log.info("删除自定义表情包，userId: {}, expressionId: {}", userId, expressionId);
        
        try {
            // 查询表情包
            Expression expression = expressionMapper.selectById(expressionId);
            if (expression == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "表情包不存在");
            }
            
            // 只能删除自己的表情包
            if (!userId.equals(expression.getUserId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "无权删除该表情包");
            }
            
            // 逻辑删除
            int result = expressionMapper.deleteById(expressionId);
            return result > 0;
            
        } catch (Exception e) {
            log.error("删除表情包异常，userId: {}, expressionId: {}", userId, expressionId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除表情包失败");
        }
    }
    
    /**
     * 转换为VO
     */
    private ExpressionVO convertToVO(Expression expression) {
        ExpressionVO vo = new ExpressionVO();
        vo.setId(expression.getId());
        vo.setName(expression.getName());
        vo.setUrl(expression.getUrl());
        vo.setCategory(expression.getCategory());
        vo.setSort(expression.getSort());
        return vo;
    }
}
