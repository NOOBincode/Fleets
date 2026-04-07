package org.example.fleets.file.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.cache.GenericCacheService;
import org.example.fleets.file.model.entity.FileMetadata;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCacheService {
    
    private final GenericCacheService genericCacheService;
    
    private static final String FILE_METADATA_KEY = "file:metadata:";
    private static final String FILE_URL_KEY = "file:url:";
    private static final long CACHE_EXPIRE_TIME = 60;
    
    public void cacheFileMetadata(FileMetadata fileMetadata) {
        String key = FILE_METADATA_KEY + fileMetadata.getId();
        genericCacheService.set(key, fileMetadata, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    public FileMetadata getCachedFileMetadata(Long fileId) {
        String key = FILE_METADATA_KEY + fileId;
        return genericCacheService.get(key);
    }
    
    public void cacheFileUrl(Long fileId, String url) {
        String key = FILE_URL_KEY + fileId;
        genericCacheService.set(key, url, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    public String getCachedFileUrl(Long fileId) {
        String key = FILE_URL_KEY + fileId;
        return genericCacheService.getString(key);
    }
    
    public void deleteFileCache(Long fileId) {
        String metadataKey = FILE_METADATA_KEY + fileId;
        String urlKey = FILE_URL_KEY + fileId;
        genericCacheService.delete(metadataKey);
        genericCacheService.delete(urlKey);
    }
}
