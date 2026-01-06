package org.example.fleets.group.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.fleets.group.model.entity.Group;

/**
 * 群组Mapper
 */
@Mapper
public interface GroupMapper extends BaseMapper<Group> {
    // 可以在这里添加自定义SQL方法
}
