package org.example.fleets.file.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 文件元数据实体类
 */
@Data
@TableName("file")
public class FileMetadata {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String filePath;
    private String fileUrl;
    private Long uploaderId;
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
