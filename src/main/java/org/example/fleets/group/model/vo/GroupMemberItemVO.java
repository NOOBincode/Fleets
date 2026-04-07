package org.example.fleets.group.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 群成员列表展示（含用户头像、显示名）
 */
@Data
public class GroupMemberItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    /**
     * 展示名：群昵称优先，否则用户昵称，否则用户名
     */
    private String userName;

    private String userAvatar;

    /**
     * 0-普通成员 1-管理员 2-群主
     */
    private Integer role;

    private String joinTime;
}
