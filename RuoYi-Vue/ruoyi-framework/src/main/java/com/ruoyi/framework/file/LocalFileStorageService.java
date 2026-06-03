package com.ruoyi.framework.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.config.ServerConfig;

/**
 * Local disk implementation backed by the existing RuoYi upload directory.
 */
@Service
@ConditionalOnProperty(prefix = "ruoyi.file-storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService
{
    @Autowired
    private ServerConfig serverConfig;

    @Override
    public StoredFile uploadFile(MultipartFile file) throws Exception
    {
        String resourcePath = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file);
        return buildStoredFile(resourcePath, file);
    }

    @Override
    public StoredFile uploadAvatar(MultipartFile file) throws Exception
    {
        String resourcePath = FileUploadUtils.upload(RuoYiConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION,
                true);
        return buildStoredFile(resourcePath, file);
    }

    @Override
    public boolean delete(String resourcePath)
    {
        if (StringUtils.isEmpty(resourcePath))
        {
            return false;
        }
        return FileUtils.deleteFile(RuoYiConfig.getProfile() + FileUtils.stripPrefix(resourcePath));
    }

    private StoredFile buildStoredFile(String resourcePath, MultipartFile file)
    {
        return new StoredFile(resourcePath, serverConfig.getUrl() + resourcePath, FileUtils.getName(resourcePath),
                file.getOriginalFilename());
    }
}
