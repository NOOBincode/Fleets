package org.example.fleets.expression.controller;

import org.example.fleets.common.api.CommonResult;
import org.example.fleets.expression.model.vo.ExpressionCategoryVO;
import org.example.fleets.expression.model.vo.ExpressionVO;
import org.example.fleets.expression.service.ExpressionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 表情包控制器
 */
@RestController
@RequestMapping("/api/expression")
public class ExpressionController {
    
    @Autowired
    private ExpressionService expressionService;
    
    /**
     * 获取表情包列表
     */
    @GetMapping("/list")
    public CommonResult<List<ExpressionCategoryVO>> getExpressionList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ExpressionCategoryVO> result = expressionService.getExpressionList(userId);
        return CommonResult.success(result);
    }
    
    /**
     * 上传自定义表情包
     */
    @PostMapping("/upload")
    public CommonResult<ExpressionVO> uploadExpression(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ExpressionVO result = expressionService.uploadExpression(userId, file);
        return CommonResult.success(result, "上传成功");
    }
    
    /**
     * 删除自定义表情包
     */
    @DeleteMapping("/{id}")
    public CommonResult<Boolean> deleteExpression(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = expressionService.deleteExpression(userId, id);
        return CommonResult.success(result, "删除成功");
    }
}
