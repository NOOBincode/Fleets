package org.example.fleets.user.converter;

import org.example.fleets.user.model.entity.Friendship;
import org.example.fleets.user.model.vo.FriendVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 好友关系转换器 - 使用MapStruct
 */
@Mapper(componentModel = "spring")
public interface FriendshipConverter {
    
    /**
     * Friendship转FriendVO
     */
    @Mapping(source = "friendship.id", target = "id")
    @Mapping(source = "friendship.userId", target = "userId")
    @Mapping(source = "friendship.friendId", target = "friendId")
    @Mapping(source = "friendship.remark", target = "remark")
    @Mapping(source = "friendship.groupName", target = "groupName")
    @Mapping(source = "friendship.status", target = "status")
    @Mapping(source = "friendship.createTime", target = "createTime")
    FriendVO toVO(Friendship friendship);
    
    /**
     * 批量转换
     */
    List<FriendVO> toVOList(List<Friendship> friendships);
}
