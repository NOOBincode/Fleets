package org.example.fleets.file.controller;

import org.example.fleets.common.api.CommonResult;
import org.example.fleets.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/api/file")
public class FileController {
    
    @Autowired
    private FileService fileService;
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public CommonResult<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        String fileUrl = fileService.uploadFile(userId, file);
        return CommonResult.success(fileUrl);
    }
    
    /**
     * 上传图片
     */
    @PostMapping("/upload/image")
    public CommonResult<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        String imageUrl = fileService.uploadImage(userId, file);
        return CommonResult.success(imageUrl);
    }
    
    /**
     * 上传语音
     */
    @PostMapping("/upload/voice")
    public CommonResult<String> uploadVoice(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        String voiceUrl = fileService.uploadVoice(userId, file);
        return CommonResult.success(voiceUrl);
    }
    
    /**
     * 上传视频
     */
    @PostMapping("/upload/video")
    public CommonResult<String> uploadVideo(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        String videoUrl = fileService.uploadVideo(userId, file);
        return CommonResult.success(videoUrl);
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public CommonResult<Boolean> deleteFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        boolean result = fileService.deleteFile(fileId, userId);
        return CommonResult.success(result);
    }
}
