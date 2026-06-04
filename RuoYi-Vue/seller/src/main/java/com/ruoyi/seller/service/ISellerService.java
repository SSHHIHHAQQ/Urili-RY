package com.ruoyi.seller.service;

import java.util.List;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.system.domain.PortalDirectLoginResult;

/**
 * 卖家Service接口
 */
public interface ISellerService
{
    public List<Seller> selectSellerList(Seller seller);

    public Seller selectSellerById(Long sellerId);

    public int insertSeller(Seller seller);

    public int updateSeller(Seller seller);

    public int updateSellerStatus(Seller seller);

    public List<SellerAccount> selectSellerAccountList(Long sellerId);

    public int insertSellerAccount(Long sellerId, SellerAccount account);

    public int resetSellerAccountPassword(SellerAccount account);

    public int resetSellerAccountDefaultPassword(SellerAccount account);

    public int resetSellerOwnerPassword(Long sellerId);

    public PortalDirectLoginResult createSellerDirectLogin(Long sellerId);
}
