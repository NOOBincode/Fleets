package org.example.fleets.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.fleets.user.model.entity.Friendship;

/**
 * 好友关系Mapper
 */
@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {
    // 可以在这里添加自定义SQL方法
}
