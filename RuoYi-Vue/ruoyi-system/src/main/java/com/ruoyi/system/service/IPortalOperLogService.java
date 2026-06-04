package com.ruoyi.system.service;

import com.ruoyi.system.domain.PortalOperLog;

/**
 * Seller/buyer portal operation log service.
 */
public interface IPortalOperLogService
{
    public void insertOperLog(String terminal, PortalOperLog operLog);
}
