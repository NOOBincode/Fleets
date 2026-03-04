package org.example.fleets.file.service.impl;

import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.file.mapper.FileMapper;
import org.example.fleets.file.model.entity.FileMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 文件服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("文件服务单元测试")
class FileServiceImplTest {

    @Mock
    private FileMapper fileMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    private static final Long USER_ID = 1L;
    private static final Long FILE_ID = 100L;
    private FileMetadata fileMetadata;

    @BeforeEach
    void setUp() {
        fileMetadata = new FileMetadata();
        fileMetadata.setId(FILE_ID);
        fileMetadata.setFileName("test.txt");
        fileMetadata.setFileType(".txt");
        fileMetadata.setFileSize(1024L);
        fileMetadata.setFilePath("/tmp/upload/file/test.txt");
        fileMetadata.setFileUrl("http://localhost:8080/files/file/test.txt");
        fileMetadata.setUploaderId(USER_ID);
        fileMetadata.setCreateTime(new Date());
    }

    @Test
    @DisplayName("获取文件URL - 成功")
    void testGetFileUrl_Success() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(fileMetadata);

        String url = fileService.getFileUrl(FILE_ID);

        assertThat(url).isEqualTo("http://localhost:8080/files/file/test.txt");
        verify(fileMapper, times(1)).selectById(FILE_ID);
    }

    @Test
    @DisplayName("获取文件URL - 文件不存在抛出异常")
    void testGetFileUrl_NotFound_Throws() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(null);

        assertThatThrownBy(() -> fileService.getFileUrl(FILE_ID))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("获取文件URL - fileId为空抛出异常")
    void testGetFileUrl_NullId_Throws() {
        assertThatThrownBy(() -> fileService.getFileUrl(null))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("删除文件 - 成功")
    void testDeleteFile_Success() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(fileMetadata);
        doNothing().when(fileMapper).deleteById(FILE_ID);

        boolean result = fileService.deleteFile(FILE_ID, USER_ID);

        assertThat(result).isTrue();
        verify(fileMapper, times(1)).deleteById(FILE_ID);
    }

    @Test
    @DisplayName("删除文件 - 非上传者无权限")
    void testDeleteFile_NotOwner_Throws() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(fileMetadata);
        fileMetadata.setUploaderId(999L);

        assertThatThrownBy(() -> fileService.deleteFile(FILE_ID, USER_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("只能删除自己上传的文件");
        verify(fileMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除文件 - 文件不存在抛出异常")
    void testDeleteFile_NotFound_Throws() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(null);

        assertThatThrownBy(() -> fileService.deleteFile(FILE_ID, USER_ID))
            .isInstanceOf(BusinessException.class);
        verify(fileMapper, never()).deleteById(anyLong());
    }
}
