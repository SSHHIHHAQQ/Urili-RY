package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.PortalOperLog;

/**
 * Seller/buyer portal operation log mapper.
 */
public interface PortalOperLogMapper
{
    public void insertSellerOperLog(PortalOperLog operLog);

    public void insertBuyerOperLog(PortalOperLog operLog);
}
