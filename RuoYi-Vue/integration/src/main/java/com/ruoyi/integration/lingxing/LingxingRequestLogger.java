package com.ruoyi.integration.lingxing;

/**
 * 领星请求日志回调。
 */
@FunctionalInterface
public interface LingxingRequestLogger
{
    void log(LingxingRequestLogEntry entry);
}
