package com.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * Shared buyer/seller account binding checks.
 */
public interface PortalAccountMapper
{
    public int countSellerAccountByUserId(@Param("userId") Long userId);

    public int countBuyerAccountByUserId(@Param("userId") Long userId);
}
