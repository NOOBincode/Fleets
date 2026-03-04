package org.example.fleets.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.fleets.file.model.entity.FileMetadata;

/**
 * 文件Mapper
 */
@Mapper
public interface FileMapper extends BaseMapper<FileMetadata> {
}
