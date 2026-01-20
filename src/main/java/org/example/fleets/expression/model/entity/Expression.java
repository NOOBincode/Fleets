package org.example.fleets.expression.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表情包实体
 */
@Data
@TableName("expression")
public class Expression {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 用户ID（NULL表示系统表情）
     */
    private Long userId;
    
    /**
     * 表情名称
     */
    private String name;
    
    /**
     * 表情图片URL
     */
    private String url;
    
    /**
     * 分类：emoji/custom/system
     */
    private String category;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer isDeleted;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
