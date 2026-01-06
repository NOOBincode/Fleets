package org.example.fleets.group.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.fleets.group.model.entity.GroupMember;

/**
 * 群成员Mapper
 */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
    // 可以在这里添加自定义SQL方法
}
