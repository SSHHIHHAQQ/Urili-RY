package com.ruoyi.product.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.product.service.IProductCodePoolService;

/**
 * 商城商品系统编码池定时维护入口。
 */
@Component("productCodePoolTask")
public class ProductCodePoolTask
{
    private static final Logger log = LoggerFactory.getLogger(ProductCodePoolTask.class);

    @Autowired
    private IProductCodePoolService productCodePoolService;

    public void maintainPools()
    {
        log.info("商城商品编码池定时维护开始");
        productCodePoolService.maintainPools();
        log.info("商城商品编码池定时维护完成");
    }
}
