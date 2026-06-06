package com.ruoyi.integration.lingxing;

import java.util.List;

/**
 * 领星库存分页响应。
 */
public class LingxingInventoryProductPage
{
    private int current;
    private int size;
    private int total;
    private List<LingxingInventoryProductStock> records;

    public int getCurrent() { return current; }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public List<LingxingInventoryProductStock> getRecords() { return records; }
    public void setRecords(List<LingxingInventoryProductStock> records) { this.records = records; }
}
