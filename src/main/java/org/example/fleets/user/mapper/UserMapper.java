package org.example.fleets.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.fleets.user.model.entity.User;

public interface UserMapper extends BaseMapper<User> {
    // 可以为空，基本的CRUD操作已由BaseMapper提供
    // 如果有复杂查询，可以在这里添加自定义方法
}
