package org.example.fleets.user.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Long        id;
    private String      username;
    private String      password;
    private String      nickname;
    private String      avatar;
    private String      phone;
    private String      email;
    private Integer     gender;
    @TableField("birth_date")
    private Date        birthDate;
    private String      signature;
    private Integer     status;
    @TableField("create_time")
    private Date        createTime;
    @TableField("update_time")
    private Date        updateTime;
    @TableField("last_login_time")
    private Date        lastLoginTime;
    @TableField("last_login_ip")
    private String      lastLoginIp;
    @TableField("is_deleted")
    @TableLogic
    private Integer     isDeleted;


}
