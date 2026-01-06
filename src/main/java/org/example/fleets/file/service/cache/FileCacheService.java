package org.example.fleets.file.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.file.model.entity.FileMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 文件缓存服务
 */
@Slf4j
@Service
public class FileCacheService {
    
    @Autowired
    private RedisService redisService;
    
    private static final String FILE_METADATA_KEY = "file:metadata:";
    private static final String FILE_URL_KEY = "file:url:";
    private static final long CACHE_EXPIRE_TIME = 60; // 60分钟
    
    /**
     * 缓存文件元数据
     */
    public void cacheFileMetadata(FileMetadata fileMetadata) {
        // TODO: 实现缓存文件元数据
        String key = FILE_METADATA_KEY + fileMetadata.getId();
        redisService.set(key, fileMetadata, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 获取缓存的文件元数据
     */
    public FileMetadata getCachedFileMetadata(Long fileId) {
        // TODO: 实现获取缓存的文件元数据
        String key = FILE_METADATA_KEY + fileId;
        return (FileMetadata) redisService.get(key);
    }
    
    /**
     * 缓存文件URL
     */
    public void cacheFileUrl(Long fileId, String url) {
        // TODO: 实现缓存文件URL
        String key = FILE_URL_KEY + fileId;
        redisService.set(key, url, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 获取缓存的文件URL
     */
    public String getCachedFileUrl(Long fileId) {
        // TODO: 实现获取缓存的文件URL
        String key = FILE_URL_KEY + fileId;
        return (String) redisService.get(key);
    }
    
    /**
     * 删除文件缓存
     */
    public void deleteFileCache(Long fileId) {
        // TODO: 实现删除文件缓存
        String metadataKey = FILE_METADATA_KEY + fileId;
        String urlKey = FILE_URL_KEY + fileId;
        redisService.delete(metadataKey);
        redisService.delete(urlKey);
    }
}
