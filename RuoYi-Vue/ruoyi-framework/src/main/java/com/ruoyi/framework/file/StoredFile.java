package com.ruoyi.framework.file;

/**
 * Stored file metadata returned by the active storage backend.
 */
public class StoredFile
{
    private final String resourcePath;

    private final String url;

    private final String newFileName;

    private final String originalFilename;

    public StoredFile(String resourcePath, String url, String newFileName, String originalFilename)
    {
        this.resourcePath = resourcePath;
        this.url = url;
        this.newFileName = newFileName;
        this.originalFilename = originalFilename;
    }

    public String getResourcePath()
    {
        return resourcePath;
    }

    public String getUrl()
    {
        return url;
    }

    public String getNewFileName()
    {
        return newFileName;
    }

    public String getOriginalFilename()
    {
        return originalFilename;
    }
}
