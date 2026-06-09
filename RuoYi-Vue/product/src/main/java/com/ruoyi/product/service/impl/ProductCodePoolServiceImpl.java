package com.ruoyi.product.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.service.IProductCodePoolService;

/**
 * 基于 Redis 预生成码池的商城商品系统编码服务。
 */
@Service
public class ProductCodePoolServiceImpl implements IProductCodePoolService
{
    private static final Logger log = LoggerFactory.getLogger(ProductCodePoolServiceImpl.class);

    private static final String SPU_POOL_KEY = "CODE_POOL:SPU";
    private static final String SKU_POOL_KEY = "CODE_POOL:SKU";
    private static final String SPU_SEQUENCE_KEY = "CODE_POOL:SEQ:SPU";
    private static final String SKU_SEQUENCE_KEY = "CODE_POOL:SEQ:SKU";
    private static final String REFILL_LOCK_KEY = "CODE_POOL:REFILL_LOCK";
    private static final int SPU_LOW_WATERMARK = 3000;
    private static final int SPU_TARGET_SIZE = 10000;
    private static final int SKU_LOW_WATERMARK = 20000;
    private static final int SKU_TARGET_SIZE = 100000;
    private static final int REFILL_CHUNK_SIZE = 5000;
    private static final long REFILL_LOCK_TTL_MINUTES = 10L;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String allocateSpuCode()
    {
        List<String> codes = allocateCodes(SPU_POOL_KEY, 1, "SPU");
        return codes.get(0);
    }

    @Override
    public List<String> allocateSkuCodes(int count)
    {
        if (count <= 0)
        {
            return Collections.emptyList();
        }
        return allocateCodes(SKU_POOL_KEY, count, "SKU");
    }

    @Override
    public void maintainPools()
    {
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(REFILL_LOCK_KEY, "1",
            REFILL_LOCK_TTL_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(locked))
        {
            log.info("商城商品编码池补充任务跳过，已有补充任务正在执行");
            return;
        }
        try
        {
            refillIfNeeded("SPU", SPU_POOL_KEY, SPU_SEQUENCE_KEY, SPU_LOW_WATERMARK, SPU_TARGET_SIZE);
            refillIfNeeded("SKU", SKU_POOL_KEY, SKU_SEQUENCE_KEY, SKU_LOW_WATERMARK, SKU_TARGET_SIZE);
        }
        finally
        {
            stringRedisTemplate.delete(REFILL_LOCK_KEY);
        }
    }

    private List<String> allocateCodes(String poolKey, int count, String codeType)
    {
        List<String> codes = stringRedisTemplate.opsForList().leftPop(poolKey, count);
        if (codes == null || codes.size() != count)
        {
            int actual = codes == null ? 0 : codes.size();
            log.error("{}编码池库存不足，期望取{}个，实际取{}个，poolKey={}", codeType, count, actual, poolKey);
            throw new ServiceException(codeType + "编码池库存不足，请联系管理员补充编码池");
        }
        return codes;
    }

    private void refillIfNeeded(String codeType, String poolKey, String sequenceKey, int lowWatermark, int targetSize)
    {
        long currentSize = safeSize(poolKey);
        if (currentSize >= lowWatermark)
        {
            log.info("{}编码池容量充足，当前容量={}，低水位={}", codeType, currentSize, lowWatermark);
            return;
        }
        int appendCount = Math.toIntExact(targetSize - currentSize);
        log.info("{}编码池低于阈值，当前容量={}，目标容量={}，准备补充{}个", codeType, currentSize, targetSize, appendCount);
        int remaining = appendCount;
        while (remaining > 0)
        {
            int batchSize = Math.min(remaining, REFILL_CHUNK_SIZE);
            appendCodes(codeType, poolKey, sequenceKey, batchSize);
            remaining -= batchSize;
        }
        log.info("{}编码池补充完成，当前容量={}", codeType, safeSize(poolKey));
    }

    private long safeSize(String poolKey)
    {
        Long size = stringRedisTemplate.opsForList().size(poolKey);
        return size == null ? 0L : size;
    }

    private void appendCodes(String codeType, String poolKey, String sequenceKey, int batchSize)
    {
        initializeSequenceIfAbsent(sequenceKey);
        Long end = stringRedisTemplate.opsForValue().increment(sequenceKey, batchSize);
        if (end == null)
        {
            throw new ServiceException(codeType + "编码池序列递增失败");
        }
        long start = end - batchSize + 1;
        List<String> codes = new ArrayList<>(batchSize);
        for (long id = start; id <= end; id++)
        {
            codes.add("SPU".equals(codeType) ? ProductCodeGenerator.spuCode(id) : ProductCodeGenerator.skuCode(id));
        }
        stringRedisTemplate.opsForList().rightPushAll(poolKey, codes);
    }

    private void initializeSequenceIfAbsent(String sequenceKey)
    {
        long initialValue = Instant.now().toEpochMilli() * 1000L + Math.floorMod(System.nanoTime(), 1000L);
        stringRedisTemplate.opsForValue().setIfAbsent(sequenceKey, String.valueOf(initialValue));
    }
}
