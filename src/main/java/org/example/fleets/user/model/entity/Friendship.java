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
    
    // 好友分组
    @TableField("group_name")
    private String groupName;
    
    // 关系状态：0-待确认 1-已确认 2-已拒绝 3-已拉黑
    private Integer status;
    
    // 添加时间
    private Date createTime;
    
    @TableField("update_time")
    private Date updateTime;
    
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
