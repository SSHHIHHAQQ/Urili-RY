package com.ruoyi.integration.service;

/**
 * 来源商品库和来源库存读模型刷新服务。
 */
public interface ISourceReadModelRefreshService
{
    int refreshOfficialMasterByConnection(String connectionCode);

    int refreshOfficialMasterSkuPairingByConnection(String connectionCode);
}
