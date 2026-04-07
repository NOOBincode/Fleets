package org.example.fleets.user.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 好友关系实体类
 */
@Data
@TableName("friendship")
public class Friendship {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    // 用户ID
    private Long userId;
    
    // 好友ID
    private Long friendId;
    
    // 好友备注
    private String remark;

    /**
     * 本次好友申请的发起人；双向两行应相同。用于区分「收到」与镜像行、以及 accept/cancel 权限。
     */
    private Long applicantUserId;

    /**
     * 申请附言；双向两行应相同。
     */
    private String verifyMessage;

    /**
     * 最后一次发起或刷新 pending 申请的时间（重复申请时更新）。
     */
    private Date lastApplyTime;

    /**
     * 关系状态：0-待确认 1-已确认 2-已拒绝（接收方） 3-已拉黑 4-已撤销（发起方）
     */
    private Integer status;
    
    // 添加时间
    private Date createTime;
    
    @TableField("update_time")
    private Date updateTime;
    
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
