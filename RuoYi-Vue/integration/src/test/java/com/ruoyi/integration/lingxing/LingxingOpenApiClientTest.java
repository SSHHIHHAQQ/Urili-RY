package com.ruoyi.integration.lingxing;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;
import com.alibaba.fastjson2.JSONObject;

public class LingxingOpenApiClientTest
{
    @Test
    public void firstBigDecimalParsesNumberWithoutDoublePrecisionDrift() throws Exception
    {
        JSONObject object = new JSONObject();
        object.put("floatValue", Float.valueOf("0.1"));
        object.put("bigIntegerValue", new BigInteger("9007199254740993"));

        assertEquals(new BigDecimal("0.1"), firstBigDecimal(object, "floatValue"));
        assertEquals(new BigDecimal("9007199254740993"), firstBigDecimal(object, "bigIntegerValue"));
    }

    private BigDecimal firstBigDecimal(JSONObject object, String key) throws Exception
    {
        Method method = LingxingOpenApiClient.class.getDeclaredMethod("firstBigDecimal", JSONObject.class, String[].class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(null, object, new String[] { key });
    }
}
