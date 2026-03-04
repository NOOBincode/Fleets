package org.example.fleets.file.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.constant.LogConstants;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.common.util.Assert;
import org.example.fleets.file.mapper.FileMapper;
import org.example.fleets.file.model.entity.FileMetadata;
import org.springframework.util.DigestUtils;
import org.example.fleets.file.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 文件服务实现类 - 本地存储方案
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    
    // 文件上传根目录
    @Value("${file.upload.path:upload}")
    private String uploadPath;
    
    // 文件访问URL前缀
    @Value("${file.access.url:http://localhost:8080/files}")
    private String accessUrl;
    
    // 文件大小限制(MB)
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VOICE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    
    // 允许的文件类型
    private static final String[] IMAGE_TYPES = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
    private static final String[] VOICE_TYPES = {".mp3", ".wav", ".amr", ".m4a"};
    private static final String[] VIDEO_TYPES = {".mp4", ".avi", ".mov", ".wmv", ".flv"};

    @Override
    public String uploadFile(Long userId, MultipartFile file) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_FILE, "上传", 
            "上传文件, userId: " + userId + ", fileName: " + file.getOriginalFilename()));
        
        return doUpload(userId, file, "file", MAX_FILE_SIZE, null);
    }

    @Override
    public String uploadImage(Long userId, MultipartFile file) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_FILE, "上传", 
            "上传图片, userId: " + userId + ", fileName: " + file.getOriginalFilename()));
        
        return doUpload(userId, file, "image", MAX_IMAGE_SIZE, IMAGE_TYPES);
    }

    @Override
    public String uploadVoice(Long userId, MultipartFile file) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_FILE, "上传", 
            "上传语音, userId: " + userId + ", fileName: " + file.getOriginalFilename()));
        
        return doUpload(userId, file, "voice", MAX_VOICE_SIZE, VOICE_TYPES);
    }

    @Override
    public String uploadVideo(Long userId, MultipartFile file) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_FILE, "上传", 
            "上传视频, userId: " + userId + ", fileName: " + file.getOriginalFilename()));
        
        return doUpload(userId, file, "video", MAX_VIDEO_SIZE, VIDEO_TYPES);
    }

    @Override
    public boolean deleteFile(Long fileId, Long userId) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_FILE, LogConstants.OP_DELETE, 
            "删除文件, fileId: " + fileId + ", userId: " + userId));
        
        // 1. 参数校验
        Assert.notNull(fileId, "文件ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        
        // 2. 查询文件
        FileMetadata fileMetadata = fileMapper.selectById(fileId);
        Assert.notNull(fileMetadata, ErrorCode.FILE_NOT_FOUND);
        
        // 3. 权限校验 - 只能删除自己上传的文件
        if (!fileMetadata.getUploaderId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只能删除自己上传的文件");
        }
        
        // 4. 删除物理文件
        try {
            File physicalFile = new File(fileMetadata.getFilePath());
            if (physicalFile.exists()) {
                physicalFile.delete();
                log.info("删除物理文件成功: " + fileMetadata.getFilePath());
            }
        } catch (Exception e) {
            log.error("删除物理文件失败", e);
        }
        
        // 5. 逻辑删除数据库记录
        fileMapper.deleteById(fileId);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_FILE, LogConstants.OP_DELETE, 
            LogConstants.STATUS_SUCCESS, "文件删除成功"));
        
        return true;
    }

    @Override
    public String getFileUrl(Long fileId) {
        Assert.notNull(fileId, "文件ID不能为空");
        
        FileMetadata fileMetadata = fileMapper.selectById(fileId);
        Assert.notNull(fileMetadata, ErrorCode.FILE_NOT_FOUND);
        
        return fileMetadata.getFileUrl();
    }
    
    /**
     * 通用上传方法
     */
    private String doUpload(Long userId, MultipartFile file, String category, 
                           long maxSize, String[] allowedTypes) {
        try {
            // 1. 参数校验
            Assert.notNull(userId, "用户ID不能为空");
            Assert.notNull(file, "文件不能为空");
            
            if (file.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容为空");
            }
            
            // 2. 文件大小校验
            if (file.getSize() > maxSize) {
                throw new BusinessException(ErrorCode.FILE_TOO_LARGE, 
                    "文件大小超过限制: " + (maxSize / 1024 / 1024) + "MB");
            }
            
            // 3. 文件类型校验
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            
            if (allowedTypes != null && !isAllowedType(extension, allowedTypes)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    "不支持的文件类型: " + extension);
            }
            
            // 4. 生成文件路径 (按日期分目录: upload/image/2025/01/30/uuid.jpg)
            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String fileName = UUID.randomUUID().toString() + extension;
            String relativePath = category + "/" + datePath + "/" + fileName;
            String fullPath = uploadPath + "/" + relativePath;
            
            // 5. 创建目录
            File dest = new File(fullPath);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            
            // 6. 保存文件
            file.transferTo(dest);
            
            // 7. 生成访问URL
            String fileUrl = accessUrl + "/" + relativePath;
            
            // 7.5 计算文件 MD5（用于去重与完整性校验）
            String fileMd5 = DigestUtils.md5DigestAsHex(file.getBytes());
            
            // 8. 保存文件元数据到数据库
            FileMetadata metadata = new FileMetadata();
            metadata.setOriginalName(originalFilename);
            metadata.setFileName(fileName);
            metadata.setFileType(extension);
            metadata.setFileSize(file.getSize());
            metadata.setFilePath(fullPath);
            metadata.setFileUrl(fileUrl);
            metadata.setUploaderId(userId);
            metadata.setCreateTime(new Date());
            metadata.setFileMd5(fileMd5);
            
            fileMapper.insert(metadata);
            
            log.info(LogConstants.buildLog(LogConstants.MODULE_FILE, "上传", 
                LogConstants.STATUS_SUCCESS, "文件上传成功, fileId: " + metadata.getId()));
            
            return fileUrl;
            
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
    
    /**
     * 检查文件类型是否允许
     */
    private boolean isAllowedType(String extension, String[] allowedTypes) {
        for (String type : allowedTypes) {
            if (type.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}
