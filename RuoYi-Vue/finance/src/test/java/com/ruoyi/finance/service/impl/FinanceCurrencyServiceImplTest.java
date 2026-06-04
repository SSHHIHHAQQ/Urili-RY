package com.ruoyi.finance.service.impl;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import org.junit.Test;
import com.ruoyi.finance.domain.FinanceCurrency;

public class FinanceCurrencyServiceImplTest
{
    @Test
    public void percentAdjustmentUsesHumanPercentValue() throws Exception
    {
        FinanceCurrency currency = currency("PERCENT_UP", "1");

        applyEffectiveRate(currency);

        assertEquals(new BigDecimal("101.0000"), currency.getEffectiveRate());
    }

    @Test
    public void percentDownAdjustmentUsesHumanPercentValue() throws Exception
    {
        FinanceCurrency currency = currency("PERCENT_DOWN", "1.5");

        applyEffectiveRate(currency);

        assertEquals(new BigDecimal("98.5000"), currency.getEffectiveRate());
    }

    @Test
    public void fixedDeltaAddsRawRateDelta() throws Exception
    {
        FinanceCurrency currency = currency("FIXED_DELTA", "0.1234");

        applyEffectiveRate(currency);

        assertEquals(new BigDecimal("100.1234"), currency.getEffectiveRate());
    }

    @Test
    public void roundingDownUsesConfiguredRatePrecision() throws Exception
    {
        FinanceCurrency currency = currency("FIXED_DELTA", "0.23456");
        currency.setOfficialRate(BigDecimal.ONE);
        currency.setRoundingMode("DOWN");

        applyEffectiveRate(currency);

        assertEquals(new BigDecimal("1.2345"), currency.getEffectiveRate());
    }

    @Test
    public void roundingUpUsesConfiguredRatePrecision() throws Exception
    {
        FinanceCurrency currency = currency("FIXED_DELTA", "0.23451");
        currency.setOfficialRate(BigDecimal.ONE);
        currency.setRoundingMode("UP");

        applyEffectiveRate(currency);

        assertEquals(new BigDecimal("1.2346"), currency.getEffectiveRate());
    }

    private FinanceCurrency currency(String adjustmentMode, String adjustmentValue)
    {
        FinanceCurrency currency = new FinanceCurrency();
        currency.setOfficialRate(new BigDecimal("100"));
        currency.setRatePrecision(4);
        currency.setRoundingMode("HALF_UP");
        currency.setAdjustmentMode(adjustmentMode);
        currency.setAdjustmentValue(new BigDecimal(adjustmentValue));
        return currency;
    }

    private void applyEffectiveRate(FinanceCurrency currency) throws Exception
    {
        FinanceCurrencyServiceImpl service = new FinanceCurrencyServiceImpl();
        Method method = FinanceCurrencyServiceImpl.class.getDeclaredMethod("applyEffectiveRate", FinanceCurrency.class);
        method.setAccessible(true);
        method.invoke(service, currency);
    }
}
