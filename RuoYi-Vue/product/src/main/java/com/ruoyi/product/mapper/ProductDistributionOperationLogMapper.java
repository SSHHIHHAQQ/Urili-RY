package com.ruoyi.product.mapper;

import java.util.List;
import com.ruoyi.product.domain.ProductDistributionOperationLog;

/**
 * 商城商品业务操作日志 Mapper。
 */
public interface ProductDistributionOperationLogMapper
{
    int insertOperationLog(ProductDistributionOperationLog log);

    List<ProductDistributionOperationLog> selectOperationLogList(ProductDistributionOperationLog query);
}
