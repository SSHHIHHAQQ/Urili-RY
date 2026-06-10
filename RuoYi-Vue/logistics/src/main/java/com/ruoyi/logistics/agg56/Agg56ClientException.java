package com.ruoyi.logistics.agg56;

/**
 * AGG56 客户端异常。
 */
public class Agg56ClientException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public Agg56ClientException(String message)
    {
        super(message);
    }

    public Agg56ClientException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
