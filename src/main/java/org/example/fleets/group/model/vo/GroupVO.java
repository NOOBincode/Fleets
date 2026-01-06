package org.example.fleets.group.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 群组VO
 */
@Data
public class GroupVO {
    
    private Long id;
    
    private String groupName;
    
    private String avatar;
    
    private Long ownerId;
    
    private String ownerNickname;
    
    private String announcement;
    
    private String description;
    
    private Integer maxMembers;
    
    private Integer memberCount;
    
    private Integer status;  // 0-禁用，1-正常
    
    private Integer joinType;  // 0-无需验证，1-需要验证，2-禁止加群
    
    private Date createTime;
}
