package com.ruoyi.integration.lingxing;

import java.util.ArrayList;
import java.util.List;

public class LingxingProductPage
{
    private int current;
    private int size;
    private int total;
    private List<LingxingProductSku> records = new ArrayList<>();

    public int getCurrent() { return current; }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public List<LingxingProductSku> getRecords() { return records; }
    public void setRecords(List<LingxingProductSku> records) { this.records = records; }
}
