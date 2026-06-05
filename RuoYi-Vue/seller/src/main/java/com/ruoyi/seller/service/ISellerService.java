package com.ruoyi.seller.service;

import java.util.List;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.system.domain.PortalDirectLoginTicket;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginResult;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.domain.PortalPasswordChangeRequest;
import com.ruoyi.system.domain.PortalSessionProfile;

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

    public SellerAccount selectSellerAccountById(Long sellerId, Long sellerAccountId);

    public int insertSellerAccount(Long sellerId, SellerAccount account);

    public int updateSellerAccount(Long sellerId, SellerAccount account);

    public int lockSellerAccount(Long sellerId, Long sellerAccountId, String lockReason);

    public int unlockSellerAccount(Long sellerId, Long sellerAccountId);

    public int resetSellerAccountPassword(SellerAccount account);

    public int resetSellerAccountDefaultPassword(SellerAccount account);

    public int resetSellerOwnerPassword(Long sellerId);

    public List<PortalSessionProfile> selectSellerSessionList(Long sellerId);

    public List<PortalSessionProfile> selectSellerAccountSessionList(Long sellerId, Long sellerAccountId);

    public int forceLogoutSellerSessions(Long sellerId);

    public int forceLogoutSellerAccountSessions(Long sellerId, Long sellerAccountId);

    public PortalDirectLoginResult createSellerDirectLogin(Long sellerId, String reason);

    public PortalDirectLoginResult createSellerAccountDirectLogin(Long sellerId, Long sellerAccountId, String reason);

    public List<PortalLoginLog> selectSellerLoginLogList(PortalLoginLog log);

    public List<PortalOperLog> selectSellerOperLogList(PortalOperLog log);

    public List<PortalLoginLog> selectSellerOwnLoginLogList(PortalLoginSession session, PortalLoginLog log);

    public List<PortalOperLog> selectSellerOwnOperLogList(PortalLoginSession session, PortalOperLog log);

    public List<PortalSessionProfile> selectSellerOwnSessionList(PortalLoginSession session);

    public List<PortalDirectLoginTicket> selectSellerDirectLoginTicketList(PortalDirectLoginTicket ticket);

    public PortalLoginResult loginSeller(LoginBody loginBody);

    public PortalLoginResult directLoginSeller(String directLoginToken);

    public int logoutSeller(PortalLoginSession session);

    public int updateSellerOwnPassword(PortalLoginSession session, PortalPasswordChangeRequest request);
}
