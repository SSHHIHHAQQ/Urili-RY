package com.ruoyi.framework.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * Unified file storage entry for uploads used by business modules.
 */
public interface FileStorageService
{
    StoredFile uploadFile(MultipartFile file) throws Exception;

    StoredFile uploadAvatar(MultipartFile file) throws Exception;

    boolean delete(String resourcePath);
}
