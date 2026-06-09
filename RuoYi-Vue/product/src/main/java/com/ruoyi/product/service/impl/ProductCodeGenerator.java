package com.ruoyi.product.service.impl;

import java.math.BigInteger;

/**
 * 商品系统编码生成器。
 */
final class ProductCodeGenerator
{
    static final String SPU_PREFIX = "UP";
    static final String SKU_PREFIX = "UK";
    private static final int BODY_LENGTH = 10;
    private static final BigInteger MODULUS = BigInteger.valueOf(36).pow(BODY_LENGTH);
    private static final BigInteger MIX_MULTIPLIER = BigInteger.valueOf(25214903917L);
    private static final BigInteger MIX_ADDEND = BigInteger.valueOf(1442695040888963407L);

    private ProductCodeGenerator()
    {
    }

    static String spuCode(long id)
    {
        return code(SPU_PREFIX, id);
    }

    static String skuCode(long id)
    {
        return code(SKU_PREFIX, id);
    }

    private static String code(String prefix, long id)
    {
        if (id <= 0)
        {
            throw new IllegalArgumentException("code id must be positive");
        }
        BigInteger raw = BigInteger.valueOf(id);
        if (raw.compareTo(MODULUS) >= 0)
        {
            throw new IllegalArgumentException("code id exceeds 10-char base36 space");
        }
        BigInteger mixed = raw.multiply(MIX_MULTIPLIER).add(MIX_ADDEND).mod(MODULUS);
        String body = mixed.toString(36).toUpperCase();
        if (body.length() > BODY_LENGTH)
        {
            throw new IllegalStateException("code body exceeds fixed length");
        }
        return prefix + "0".repeat(BODY_LENGTH - body.length()) + body;
    }
}
