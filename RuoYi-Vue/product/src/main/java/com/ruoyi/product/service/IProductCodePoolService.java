package com.ruoyi.product.service;

import java.util.List;

/**
 * 商城商品系统编码池服务。
 */
public interface IProductCodePoolService
{
    /**
     * 从 SPU 编码池取一个系统 SPU 编码。
     *
     * @return 系统 SPU 编码
     */
    String allocateSpuCode();

    /**
     * 从 SKU 编码池批量取系统 SKU 编码。
     *
     * @param count 需要的 SKU 编码数量
     * @return 系统 SKU 编码列表
     */
    List<String> allocateSkuCodes(int count);

    /**
     * 维护 SPU/SKU Redis 编码池容量。
     */
    void maintainPools();
}
