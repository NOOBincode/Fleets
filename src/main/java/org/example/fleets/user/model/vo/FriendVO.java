package org.example.fleets.user.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 好友VO
 */
@Data
public class FriendVO {
    
    private Long id;
    
    private Long userId;
    
    private Long friendId;
    
    private String friendUsername;
    
    private String friendNickname;
    
    private String friendAvatar;
    
    private String remark;
    
    private String groupName;
    
    private Integer status;  // 0-待确认，1-已确认，2-已拒绝，3-已拉黑
    
    private Boolean isOnline;  // 是否在线
    
    private Date createTime;
}
