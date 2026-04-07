package org.example.fleets.user.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 好友申请 VO：可同时用于「收到的申请」与「发出的申请」（由 {@link #listType} 区分）。
 */
@Data
public class FriendApplyVO {
    
    /**
     * 关系行 ID（friendship.id）
     */
    private Long id;

    /**
     * RECEIVED：我收到的申请；SENT：我发出的申请（Phase 2 前端可按此分 Tab）。
     */
    private String listType;
    
    /**
     * 申请人 userId（与 friendship.applicant_user_id 一致）
     */
    private Long applicantUserId;
    
    /**
     * 行视角：user_id（列表组装时与查询行一致）
     */
    private Long userId;
    
    /**
     * 行视角：friend_id（对端）
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
     * 验证消息 / 申请附言
     */
    private String verifyMessage;

    /**
     * 我对好友的备注（仅发起方行有值；收到列表通常不展示给对方）
     */
    private String remark;
    
    /**
     * 状态：0-待确认 1-已确认 2-已拒绝 4-已撤销（若列表会包含历史可扩展）
     */
    private Integer status;
    
    /**
     * 首次建立该关系行时间
     */
    private Date createTime;

    /**
     * 最后一次发起/刷新申请时间
     */
    private Date lastApplyTime;
}
