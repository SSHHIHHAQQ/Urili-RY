package com.ruoyi.finance.domain;

/**
 * 可用币种下拉选项。
 */
public class FinanceCurrencyOption
{
    private String label;

    private String value;

    private String symbol;

    private Integer amountPrecision;

    public FinanceCurrencyOption()
    {
    }

    public FinanceCurrencyOption(String label, String value, String symbol, Integer amountPrecision)
    {
        this.label = label;
        this.value = value;
        this.symbol = symbol;
        this.amountPrecision = amountPrecision;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public Integer getAmountPrecision()
    {
        return amountPrecision;
    }

    public void setAmountPrecision(Integer amountPrecision)
    {
        this.amountPrecision = amountPrecision;
    }
}
