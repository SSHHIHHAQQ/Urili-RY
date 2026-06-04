package com.ruoyi.seller.service;

import java.util.List;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalLoginResult;

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

    public int updateSellerAccount(Long sellerId, SellerAccount account);

    public int resetSellerAccountPassword(SellerAccount account);

    public int resetSellerAccountDefaultPassword(SellerAccount account);

    public int resetSellerOwnerPassword(Long sellerId);

    public int forceLogoutSellerSessions(Long sellerId);

    public int forceLogoutSellerAccountSessions(Long sellerId, Long sellerAccountId);

    public PortalDirectLoginResult createSellerDirectLogin(Long sellerId);

    public PortalLoginResult loginSeller(LoginBody loginBody);

    public PortalLoginResult directLoginSeller(String directLoginToken);
}
