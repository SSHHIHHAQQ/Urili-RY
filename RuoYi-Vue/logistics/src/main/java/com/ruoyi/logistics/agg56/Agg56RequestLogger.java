package com.ruoyi.logistics.agg56;

/**
 * AGG56 请求日志回调。
 */
@FunctionalInterface
public interface Agg56RequestLogger
{
    void log(Agg56RequestLogEntry entry);
}
