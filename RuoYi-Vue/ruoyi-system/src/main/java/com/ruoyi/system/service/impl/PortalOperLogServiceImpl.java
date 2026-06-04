package com.ruoyi.system.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.mapper.PortalOperLogMapper;
import com.ruoyi.system.service.IPortalOperLogService;

/**
 * Seller/buyer portal operation log service implementation.
 */
@Service
public class PortalOperLogServiceImpl implements IPortalOperLogService
{
    @Autowired
    private PortalOperLogMapper portalOperLogMapper;

    @Override
    public void insertOperLog(String terminal, PortalOperLog operLog)
    {
        if (StringUtils.equals("seller", terminal))
        {
            portalOperLogMapper.insertSellerOperLog(operLog);
            return;
        }
        if (StringUtils.equals("buyer", terminal))
        {
            portalOperLogMapper.insertBuyerOperLog(operLog);
            return;
        }
        throw new ServiceException("不支持的端内操作日志类型");
    }
}
