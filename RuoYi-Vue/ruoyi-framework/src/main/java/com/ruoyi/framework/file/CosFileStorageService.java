package com.ruoyi.framework.file;

import java.io.InputStream;
import java.util.Date;
import java.util.Objects;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.file.FileNameLengthLimitExceededException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;

/**
 * Tencent COS implementation for the unified upload entry.
 */
@Service
@ConditionalOnProperty(prefix = "ruoyi.file-storage", name = "type", havingValue = "cos")
public class CosFileStorageService implements FileStorageService
{
    private static final Logger log = LoggerFactory.getLogger(CosFileStorageService.class);

    private static final String UPLOAD_DIRECTORY = "upload";

    private static final String AVATAR_DIRECTORY = "avatar";

    private final CosFileStorageProperties properties;

    private COSClient cosClient;

    public CosFileStorageService(CosFileStorageProperties properties)
    {
        this.properties = properties;
    }

    @PostConstruct
    public void init()
    {
        validateProperties();
        COSCredentials credentials = new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
        cosClient = new COSClient(credentials, clientConfig);
    }

    @PreDestroy
    public void destroy()
    {
        if (cosClient != null)
        {
            cosClient.shutdown();
        }
    }

    @Override
    public StoredFile uploadFile(MultipartFile file) throws Exception
    {
        return upload(file, UPLOAD_DIRECTORY, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION, false);
    }

    @Override
    public StoredFile uploadAvatar(MultipartFile file) throws Exception
    {
        return upload(file, AVATAR_DIRECTORY, MimeTypeUtils.IMAGE_EXTENSION, true);
    }

    @Override
    public boolean delete(String resourcePath)
    {
        if (StringUtils.isEmpty(resourcePath))
        {
            return false;
        }
        try
        {
            cosClient.deleteObject(new DeleteObjectRequest(properties.getBucket(), toObjectKey(resourcePath)));
            return true;
        }
        catch (Exception e)
        {
            log.warn("删除 COS 文件失败，resourcePath={}", resourcePath, e);
            return false;
        }
    }

    public String resolveUrl(String resourcePath)
    {
        String objectKey = toObjectKey(resourcePath);
        if (properties.isPublicRead())
        {
            return buildPublicUrl(objectKey);
        }
        long expirationMillis = Math.max(properties.getUrlExpirationSeconds(), 60) * 1000;
        Date expiration = new Date(System.currentTimeMillis() + expirationMillis);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(properties.getBucket(), objectKey,
                HttpMethodName.GET);
        request.setExpiration(expiration);
        return cosClient.generatePresignedUrl(request).toString();
    }

    private StoredFile upload(MultipartFile file, String directory, String[] allowedExtension, boolean useCustomNaming)
            throws Exception
    {
        validateFilename(file);
        FileUploadUtils.assertAllowed(file, allowedExtension);

        String fileName = useCustomNaming ? FileUploadUtils.uuidFilename(file) : FileUploadUtils.extractFilename(file);
        String resourcePath = Constants.RESOURCE_PREFIX + "/" + directory + "/" + fileName;
        String objectKey = toObjectKey(resourcePath);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        if (StringUtils.isNotEmpty(file.getContentType()))
        {
            metadata.setContentType(file.getContentType());
        }

        try (InputStream inputStream = file.getInputStream())
        {
            PutObjectRequest request = new PutObjectRequest(properties.getBucket(), objectKey, inputStream, metadata);
            if (properties.isPublicRead())
            {
                request.setCannedAcl(CannedAccessControlList.PublicRead);
            }
            cosClient.putObject(request);
        }

        return new StoredFile(resourcePath, resolveUrl(resourcePath), FileUtils.getName(resourcePath),
                file.getOriginalFilename());
    }

    private void validateFilename(MultipartFile file) throws FileNameLengthLimitExceededException
    {
        int fileNameLength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNameLength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }
    }

    private String toObjectKey(String resourcePath)
    {
        String path = resourcePath;
        if (StringUtils.startsWithAny(path, properties.getEndpoint()))
        {
            path = path.substring(properties.getEndpoint().length());
        }
        if (path.startsWith(Constants.RESOURCE_PREFIX + "/"))
        {
            path = path.substring((Constants.RESOURCE_PREFIX + "/").length());
        }
        path = normalizePath(path);

        String keyPrefix = normalizePath(properties.getKeyPrefix());
        if (StringUtils.isEmpty(keyPrefix))
        {
            return path;
        }
        if (StringUtils.isEmpty(path))
        {
            return keyPrefix;
        }
        return keyPrefix + "/" + path;
    }

    private String buildPublicUrl(String objectKey)
    {
        return trimTrailingSlash(properties.getEndpoint()) + "/" + objectKey;
    }

    private void validateProperties()
    {
        if (StringUtils.isEmpty(properties.getSecretId()) || StringUtils.isEmpty(properties.getSecretKey())
                || StringUtils.isEmpty(properties.getBucket()) || StringUtils.isEmpty(properties.getRegion())
                || StringUtils.isEmpty(properties.getEndpoint()))
        {
            throw new ServiceException("COS 文件存储配置不完整，不能启用 ruoyi.file-storage.type=cos");
        }
    }

    private String normalizePath(String path)
    {
        if (StringUtils.isEmpty(path))
        {
            return "";
        }
        String normalized = FilenameUtils.separatorsToUnix(path.trim());
        while (normalized.startsWith("/"))
        {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/"))
        {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String trimTrailingSlash(String value)
    {
        String trimmed = value.trim();
        while (trimmed.endsWith("/"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
