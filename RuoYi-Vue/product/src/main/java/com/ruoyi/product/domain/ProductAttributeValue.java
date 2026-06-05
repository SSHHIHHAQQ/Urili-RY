package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商城商品类目属性值，backed by product_attribute_value.
 */
public class ProductAttributeValue extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long valueId;
    private String ownerType;
    private Long ownerId;
    private Long spuId;
    private Long categoryId;
    private Integer categorySchemaVersion;
    private Long attributeId;
    private String attributeCode;
    private String attributeName;
    private String attributeType;
    private String valueCode;
    private String valueText;
    private BigDecimal valueNumber;
    private Date valueDate;
    private String valueJson;

    public Long getValueId() { return valueId; }
    public void setValueId(Long valueId) { this.valueId = valueId; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getCategorySchemaVersion() { return categorySchemaVersion; }
    public void setCategorySchemaVersion(Integer categorySchemaVersion) { this.categorySchemaVersion = categorySchemaVersion; }
    public Long getAttributeId() { return attributeId; }
    public void setAttributeId(Long attributeId) { this.attributeId = attributeId; }
    public String getAttributeCode() { return attributeCode; }
    public void setAttributeCode(String attributeCode) { this.attributeCode = attributeCode; }
    public String getAttributeName() { return attributeName; }
    public void setAttributeName(String attributeName) { this.attributeName = attributeName; }
    public String getAttributeType() { return attributeType; }
    public void setAttributeType(String attributeType) { this.attributeType = attributeType; }
    public String getValueCode() { return valueCode; }
    public void setValueCode(String valueCode) { this.valueCode = valueCode; }
    public String getValueText() { return valueText; }
    public void setValueText(String valueText) { this.valueText = valueText; }
    public BigDecimal getValueNumber() { return valueNumber; }
    public void setValueNumber(BigDecimal valueNumber) { this.valueNumber = valueNumber; }
    public Date getValueDate() { return valueDate; }
    public void setValueDate(Date valueDate) { this.valueDate = valueDate; }
    public String getValueJson() { return valueJson; }
    public void setValueJson(String valueJson) { this.valueJson = valueJson; }
}
