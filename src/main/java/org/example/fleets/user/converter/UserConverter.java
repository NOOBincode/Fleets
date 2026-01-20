package org.example.fleets.user.converter;

import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.model.vo.UserLoginVO;
import org.example.fleets.user.model.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * 用户对象转换器 - 使用MapStruct
 * 负责 DTO/Entity/VO 之间的转换
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserConverter {
    
    /**
     * 注册DTO转Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)  // 密码需要单独加密处理
    @Mapping(target = "status", constant = "1")
    @Mapping(target = "avatar", constant = "")
    @Mapping(target = "gender", constant = "0")
    @Mapping(target = "signature", constant = "")
    @Mapping(target = "createTime", expression = "java(new java.util.Date())")
    @Mapping(target = "updateTime", expression = "java(new java.util.Date())")
    User toEntity(UserRegisterDTO dto);
    
    /**
     * Entity转VO（不包含敏感信息）
     */
    UserVO toVO(User user);
    
    /**
     * 批量转换
     */
    List<UserVO> toVOList(List<User> users);
    
    /**
     * Entity转LoginVO
     */
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "token", target = "token")
    @Mapping(source = "expireTime", target = "expireTime")
    UserLoginVO toLoginVO(User user, String token, Long expireTime);
    
    /**
     * 更新Entity（只更新非null字段）
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", expression = "java(new java.util.Date())")
    void updateEntity(org.example.fleets.user.model.dto.UserUpdateDTO dto, @MappingTarget User user);
}

