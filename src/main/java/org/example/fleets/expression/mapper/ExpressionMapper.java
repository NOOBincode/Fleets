package org.example.fleets.expression.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.fleets.expression.model.entity.Expression;

/**
 * 表情包Mapper
 */
@Mapper
public interface ExpressionMapper extends BaseMapper<Expression> {
}
