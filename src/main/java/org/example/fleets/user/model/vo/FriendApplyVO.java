package org.example.fleets.user.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 好友申请VO
 */
@Data
public class FriendApplyVO {
    
    /**
     * 申请ID
     */
    private Long id;
    
    /**
     * 申请人ID
     */
    private Long userId;
    
    /**
     * 好友ID
     */
    private Long friendId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 验证消息
     */
    private String verifyMessage;
    
    /**
     * 状态：0-待确认 1-已同意 2-已拒绝
     */
    private Integer status;
    
    /**
     * 申请时间
     */
    private Date createTime;
}
