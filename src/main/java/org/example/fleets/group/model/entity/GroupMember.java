package org.example.fleets.group.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 群成员实体类
 */
@Data
@TableName("group_member")
public class GroupMember {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    // 群组ID
    private Long groupId;
    
    // 用户ID
    private Long userId;
    
    // 群昵称
    @TableField("group_nickname")
    private String groupNickname;
    
    // 成员角色：0-普通成员 1-管理员 2-群主
    private Integer role;
    
    // 禁言状态：0-正常 1-禁言
    @TableField("mute_status")
    private Integer muteStatus;
    
    // 禁言结束时间
    private Date muteEndTime;
    
    // 加入时间
    private Date joinTime;
    
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
