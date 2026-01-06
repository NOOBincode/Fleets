package org.example.fleets.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.fleets.file.model.entity.FileMetadata;

/**
 * 文件元数据Mapper
 */
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
    // 可以在这里添加自定义SQL方法
}
