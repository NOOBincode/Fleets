package org.example.fleets.user.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 分组好友列表VO
 */
@Data
public class GroupingFriendVO {
    
    /**
     * 分组名称
     */
    private String groupName;
    
    /**
     * 该分组的好友列表
     */
    private List<FriendVO> friends;
}
