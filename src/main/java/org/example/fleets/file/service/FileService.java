package org.example.fleets.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface FileService {
    
    /**
     * 上传文件
     */
    String uploadFile(Long userId, MultipartFile file);
    
    /**
     * 上传图片
     */
    String uploadImage(Long userId, MultipartFile file);
    
    /**
     * 上传语音
     */
    String uploadVoice(Long userId, MultipartFile file);
    
    /**
     * 上传视频
     */
    String uploadVideo(Long userId, MultipartFile file);
    
    /**
     * 删除文件
     */
    boolean deleteFile(Long fileId, Long userId);
    
    /**
     * 获取文件URL
     */
    String getFileUrl(Long fileId);
}
