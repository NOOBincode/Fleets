package org.example.fleets.file.controller;

import lombok.RequiredArgsConstructor;
import cn.dev33.satoken.stp.StpUtil;
import org.example.fleets.common.api.CommonResult;  
import org.example.fleets.file.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
public class FileController {
    
    private final FileService fileService;
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public CommonResult<String> uploadFile(@RequestParam("file") MultipartFile file) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String fileUrl = fileService.uploadFile(userId, file);
        return CommonResult.success(fileUrl);
    }
    
    /**
     * 上传图片
     */
    @PostMapping("/upload/image")
    public CommonResult<String> uploadImage(@RequestParam("file") MultipartFile file) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String imageUrl = fileService.uploadImage(userId, file);
        return CommonResult.success(imageUrl);
    }
    
    /**
     * 上传语音
     */
    @PostMapping("/upload/voice")
    public CommonResult<String> uploadVoice(@RequestParam("file") MultipartFile file) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String voiceUrl = fileService.uploadVoice(userId, file);
        return CommonResult.success(voiceUrl);
    }
    
    /**
     * 上传视频
     */
    @PostMapping("/upload/video")
    public CommonResult<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String videoUrl = fileService.uploadVideo(userId, file);
        return CommonResult.success(videoUrl);
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public CommonResult<Boolean> deleteFile(@PathVariable Long fileId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = fileService.deleteFile(fileId, userId);
        return CommonResult.success(result);
    }
}
