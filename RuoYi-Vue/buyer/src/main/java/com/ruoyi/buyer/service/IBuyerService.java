package com.ruoyi.buyer.service;

import java.util.List;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.system.domain.PortalDirectLoginResult;

/**
 * 买家Service接口
 */
public interface IBuyerService
{
    public List<Buyer> selectBuyerList(Buyer buyer);

    public Buyer selectBuyerById(Long buyerId);

    public int insertBuyer(Buyer buyer);

    public int updateBuyer(Buyer buyer);

    public int updateBuyerStatus(Buyer buyer);

    public List<BuyerAccount> selectBuyerAccountList(Long buyerId);

    public int insertBuyerAccount(Long buyerId, BuyerAccount account);

    public int resetBuyerAccountPassword(BuyerAccount account);

    public int resetBuyerAccountDefaultPassword(BuyerAccount account);

    public int resetBuyerOwnerPassword(Long buyerId);

    public PortalDirectLoginResult createBuyerDirectLogin(Long buyerId);
}
