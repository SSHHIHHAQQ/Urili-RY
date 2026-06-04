package com.ruoyi.integration.lingxing;

/**
 * 领星客户端异常。
 */
public class LingxingClientException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    private final boolean retryable;

    public LingxingClientException(String errorCode, String message, boolean retryable)
    {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public String getErrorCode()
    {
        return errorCode;
    }

    public boolean isRetryable()
    {
        return retryable;
    }
}
