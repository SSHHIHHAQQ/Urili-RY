package com.ruoyi.product.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class ProductCodeGeneratorTest
{
    @Test
    public void spuCodeUsesFixedPrefixLengthAndUpperBase36Body()
    {
        String code = ProductCodeGenerator.spuCode(1L);

        assertEquals(12, code.length());
        assertTrue(code.startsWith("UP"));
        assertTrue(code.substring(2).matches("[0-9A-Z]{10}"));
    }

    @Test
    public void skuCodeUsesFixedPrefixLengthAndUpperBase36Body()
    {
        String code = ProductCodeGenerator.skuCode(1L);

        assertEquals(12, code.length());
        assertTrue(code.startsWith("UK"));
        assertTrue(code.substring(2).matches("[0-9A-Z]{10}"));
    }

    @Test
    public void mixedCodeBodyDoesNotRepeatForSequentialIds()
    {
        Set<String> codes = new HashSet<>();
        for (long id = 1L; id <= 1000L; id++)
        {
            codes.add(ProductCodeGenerator.skuCode(id));
        }

        assertEquals(1000, codes.size());
    }
}
