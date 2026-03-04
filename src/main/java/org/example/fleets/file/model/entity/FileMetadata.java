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
    
    @TableField("original_name")
    private String originalName;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_path")
    private String filePath;

    @TableField("file_url")
    private String fileUrl;

    // 映射到表中的 uploader_id 列
    @TableField("uploader_id")
    private Long uploaderId;

    @TableField("file_md5")
    private String fileMd5;
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField("update_time")
    private Date updateTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
