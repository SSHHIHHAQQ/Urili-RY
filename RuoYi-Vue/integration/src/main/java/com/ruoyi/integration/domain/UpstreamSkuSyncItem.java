package com.ruoyi.integration.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 领星 SKU 同步清单。
 */
public class UpstreamSkuSyncItem
{
    private String connectionCode;
    private String masterSku;
    private String masterProductName;
    private String productAliasName;
    private String approveStatus;
    private Integer productType;
    private String productDescription;
    private String imageUrl;
    private String mainCode;
    private String otherCode;
    private String fnsku;
    private String countryOfOriginName;
    private String currencyCode;
    private String customhouseCode;
    private Integer dangerousCargo;
    private String declareNameCn;
    private String declareNameEn;
    private BigDecimal declarePrice;
    private BigDecimal height;
    private BigDecimal heightBs;
    private BigDecimal length;
    private BigDecimal lengthBs;
    private BigDecimal weight;
    private BigDecimal weightBs;
    private BigDecimal width;
    private BigDecimal widthBs;
    private BigDecimal wmsHeight;
    private BigDecimal wmsHeightBs;
    private BigDecimal wmsLength;
    private BigDecimal wmsLengthBs;
    private BigDecimal wmsWeight;
    private BigDecimal wmsWeightBs;
    private BigDecimal wmsWidth;
    private BigDecimal wmsWidthBs;
    private String cat1Name;
    private String cat2Name;
    private String cat3Name;
    private String platformSkuInfoJson;
    private String brazilTaxInfoJson;
    private String sourcePayloadJson;
    private String sourcePayloadHash;
    private String status;
    private String searchText;
    private String syncBatchId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private String pairingStatus;
    private Long skuPairingId;
    private String systemSku;
    private String systemSkuName;
    private String customerName;

    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getMasterSku() { return masterSku; }
    public void setMasterSku(String masterSku) { this.masterSku = masterSku; }
    public String getMasterProductName() { return masterProductName; }
    public void setMasterProductName(String masterProductName) { this.masterProductName = masterProductName; }
    public String getProductAliasName() { return productAliasName; }
    public void setProductAliasName(String productAliasName) { this.productAliasName = productAliasName; }
    public String getApproveStatus() { return approveStatus; }
    public void setApproveStatus(String approveStatus) { this.approveStatus = approveStatus; }
    public Integer getProductType() { return productType; }
    public void setProductType(Integer productType) { this.productType = productType; }
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getMainCode() { return mainCode; }
    public void setMainCode(String mainCode) { this.mainCode = mainCode; }
    public String getOtherCode() { return otherCode; }
    public void setOtherCode(String otherCode) { this.otherCode = otherCode; }
    public String getFnsku() { return fnsku; }
    public void setFnsku(String fnsku) { this.fnsku = fnsku; }
    public String getCountryOfOriginName() { return countryOfOriginName; }
    public void setCountryOfOriginName(String countryOfOriginName) { this.countryOfOriginName = countryOfOriginName; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getCustomhouseCode() { return customhouseCode; }
    public void setCustomhouseCode(String customhouseCode) { this.customhouseCode = customhouseCode; }
    public Integer getDangerousCargo() { return dangerousCargo; }
    public void setDangerousCargo(Integer dangerousCargo) { this.dangerousCargo = dangerousCargo; }
    public String getDeclareNameCn() { return declareNameCn; }
    public void setDeclareNameCn(String declareNameCn) { this.declareNameCn = declareNameCn; }
    public String getDeclareNameEn() { return declareNameEn; }
    public void setDeclareNameEn(String declareNameEn) { this.declareNameEn = declareNameEn; }
    public BigDecimal getDeclarePrice() { return declarePrice; }
    public void setDeclarePrice(BigDecimal declarePrice) { this.declarePrice = declarePrice; }
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }
    public BigDecimal getHeightBs() { return heightBs; }
    public void setHeightBs(BigDecimal heightBs) { this.heightBs = heightBs; }
    public BigDecimal getLength() { return length; }
    public void setLength(BigDecimal length) { this.length = length; }
    public BigDecimal getLengthBs() { return lengthBs; }
    public void setLengthBs(BigDecimal lengthBs) { this.lengthBs = lengthBs; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getWeightBs() { return weightBs; }
    public void setWeightBs(BigDecimal weightBs) { this.weightBs = weightBs; }
    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }
    public BigDecimal getWidthBs() { return widthBs; }
    public void setWidthBs(BigDecimal widthBs) { this.widthBs = widthBs; }
    public BigDecimal getWmsHeight() { return wmsHeight; }
    public void setWmsHeight(BigDecimal wmsHeight) { this.wmsHeight = wmsHeight; }
    public BigDecimal getWmsHeightBs() { return wmsHeightBs; }
    public void setWmsHeightBs(BigDecimal wmsHeightBs) { this.wmsHeightBs = wmsHeightBs; }
    public BigDecimal getWmsLength() { return wmsLength; }
    public void setWmsLength(BigDecimal wmsLength) { this.wmsLength = wmsLength; }
    public BigDecimal getWmsLengthBs() { return wmsLengthBs; }
    public void setWmsLengthBs(BigDecimal wmsLengthBs) { this.wmsLengthBs = wmsLengthBs; }
    public BigDecimal getWmsWeight() { return wmsWeight; }
    public void setWmsWeight(BigDecimal wmsWeight) { this.wmsWeight = wmsWeight; }
    public BigDecimal getWmsWeightBs() { return wmsWeightBs; }
    public void setWmsWeightBs(BigDecimal wmsWeightBs) { this.wmsWeightBs = wmsWeightBs; }
    public BigDecimal getWmsWidth() { return wmsWidth; }
    public void setWmsWidth(BigDecimal wmsWidth) { this.wmsWidth = wmsWidth; }
    public BigDecimal getWmsWidthBs() { return wmsWidthBs; }
    public void setWmsWidthBs(BigDecimal wmsWidthBs) { this.wmsWidthBs = wmsWidthBs; }
    public String getCat1Name() { return cat1Name; }
    public void setCat1Name(String cat1Name) { this.cat1Name = cat1Name; }
    public String getCat2Name() { return cat2Name; }
    public void setCat2Name(String cat2Name) { this.cat2Name = cat2Name; }
    public String getCat3Name() { return cat3Name; }
    public void setCat3Name(String cat3Name) { this.cat3Name = cat3Name; }
    public String getPlatformSkuInfoJson() { return platformSkuInfoJson; }
    public void setPlatformSkuInfoJson(String platformSkuInfoJson) { this.platformSkuInfoJson = platformSkuInfoJson; }
    public String getBrazilTaxInfoJson() { return brazilTaxInfoJson; }
    public void setBrazilTaxInfoJson(String brazilTaxInfoJson) { this.brazilTaxInfoJson = brazilTaxInfoJson; }
    public String getSourcePayloadJson() { return sourcePayloadJson; }
    public void setSourcePayloadJson(String sourcePayloadJson) { this.sourcePayloadJson = sourcePayloadJson; }
    public String getSourcePayloadHash() { return sourcePayloadHash; }
    public void setSourcePayloadHash(String sourcePayloadHash) { this.sourcePayloadHash = sourcePayloadHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
    public Date getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(Date firstSeenTime) { this.firstSeenTime = firstSeenTime; }
    public Date getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Date lastSeenTime) { this.lastSeenTime = lastSeenTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getPairingStatus() { return pairingStatus; }
    public void setPairingStatus(String pairingStatus) { this.pairingStatus = pairingStatus; }
    public Long getSkuPairingId() { return skuPairingId; }
    public void setSkuPairingId(Long skuPairingId) { this.skuPairingId = skuPairingId; }
    public String getSystemSku() { return systemSku; }
    public void setSystemSku(String systemSku) { this.systemSku = systemSku; }
    public String getSystemSkuName() { return systemSkuName; }
    public void setSystemSkuName(String systemSkuName) { this.systemSkuName = systemSkuName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
}
