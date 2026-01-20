package org.example.fleets.user.model.vo;

import lombok.Data;

/**
 * 好友分组VO
 */
@Data
public class GroupingVO {
    
    /**
     * 分组名称
     */
    private String groupName;
    
    /**
     * 该分组的好友数量
     */
    private Integer count;
}
