package com.ruoyi.framework.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tencent COS file storage settings.
 */
@Component
@ConfigurationProperties(prefix = "ruoyi.file-storage.cos")
public class CosFileStorageProperties
{
    private String secretId;

    private String secretKey;

    private String bucket;

    private String region;

    private String endpoint;

    private String keyPrefix = "profile";

    private boolean publicRead = false;

    private long urlExpirationSeconds = 300;

    public String getSecretId()
    {
        return secretId;
    }

    public void setSecretId(String secretId)
    {
        this.secretId = secretId;
    }

    public String getSecretKey()
    {
        return secretKey;
    }

    public void setSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    public String getBucket()
    {
        return bucket;
    }

    public void setBucket(String bucket)
    {
        this.bucket = bucket;
    }

    public String getRegion()
    {
        return region;
    }

    public void setRegion(String region)
    {
        this.region = region;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public String getKeyPrefix()
    {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix)
    {
        this.keyPrefix = keyPrefix;
    }

    public boolean isPublicRead()
    {
        return publicRead;
    }

    public void setPublicRead(boolean publicRead)
    {
        this.publicRead = publicRead;
    }

    public long getUrlExpirationSeconds()
    {
        return urlExpirationSeconds;
    }

    public void setUrlExpirationSeconds(long urlExpirationSeconds)
    {
        this.urlExpirationSeconds = urlExpirationSeconds;
    }
}
