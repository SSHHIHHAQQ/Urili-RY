package com.ruoyi.product.domain.importdata;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品配置导入校验与执行结果。
 */
public class ProductImportResult
{
    private Integer totalCount = 0;
    private Integer createCount = 0;
    private Integer updateCount = 0;
    private Integer skipCount = 0;
    private Integer errorCount = 0;
    private List<ProductImportMessage> messages = new ArrayList<>();

    public boolean isPassed()
    {
        return errorCount == 0;
    }

    public void addCreate(Integer rowNum, String message)
    {
        createCount++;
        messages.add(new ProductImportMessage(rowNum, "CREATE", "SUCCESS", message));
    }

    public void addUpdate(Integer rowNum, String message)
    {
        updateCount++;
        messages.add(new ProductImportMessage(rowNum, "UPDATE", "SUCCESS", message));
    }

    public void addSkip(Integer rowNum, String message)
    {
        skipCount++;
        messages.add(new ProductImportMessage(rowNum, "SKIP", "WARN", message));
    }

    public void addError(Integer rowNum, String message)
    {
        errorCount++;
        messages.add(new ProductImportMessage(rowNum, "ERROR", "ERROR", message));
    }

    public Integer getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount)
    {
        this.totalCount = totalCount;
    }

    public Integer getCreateCount()
    {
        return createCount;
    }

    public void setCreateCount(Integer createCount)
    {
        this.createCount = createCount;
    }

    public Integer getUpdateCount()
    {
        return updateCount;
    }

    public void setUpdateCount(Integer updateCount)
    {
        this.updateCount = updateCount;
    }

    public Integer getSkipCount()
    {
        return skipCount;
    }

    public void setSkipCount(Integer skipCount)
    {
        this.skipCount = skipCount;
    }

    public Integer getErrorCount()
    {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount)
    {
        this.errorCount = errorCount;
    }

    public List<ProductImportMessage> getMessages()
    {
        return messages;
    }

    public void setMessages(List<ProductImportMessage> messages)
    {
        this.messages = messages;
    }
}
