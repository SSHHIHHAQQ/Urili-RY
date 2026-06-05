package com.ruoyi.product.mapper;

import java.util.List;
import com.ruoyi.product.domain.ProductConfigChangeLog;

/**
 * 商品配置修改记录 Mapper。
 */
public interface ProductConfigChangeLogMapper
{
    int insertChangeLog(ProductConfigChangeLog changeLog);

    List<ProductConfigChangeLog> selectChangeLogList(ProductConfigChangeLog query);
}
