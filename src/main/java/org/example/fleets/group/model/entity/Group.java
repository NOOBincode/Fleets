package org.example.fleets.group.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 群组实体类
 */
@Data
@TableName("group")
public class Group {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    // 群组名称
    @TableField("group_name")
    private String groupName;
    
    // 群组头像
    private String avatar;
    
    // 群主ID
    private Long ownerId;
    
    // 群公告
    private String announcement;
    
    // 群简介
    private String description;
    
    // 最大成员数
    private Integer maxMembers;
    
    // 当前成员数
    private Integer memberCount;
    
    // 群状态：0-正常 1-禁言 2-解散
    private Integer status;
    
    // 加群方式：0-无需验证 1-需要验证 2-禁止加群
    private Integer joinType;
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField("update_time")
    private Date updateTime;
    
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
