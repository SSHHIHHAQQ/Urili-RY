package com.ruoyi.buyer.service;

import java.util.List;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.system.domain.PortalDirectLoginTicket;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginResult;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.domain.PortalOwnLoginLogProfile;
import com.ruoyi.system.domain.PortalOwnOperLogProfile;
import com.ruoyi.system.domain.PortalOwnSessionProfile;
import com.ruoyi.system.domain.PortalPasswordChangeRequest;
import com.ruoyi.system.domain.PortalSessionProfile;

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

    public BuyerAccount selectBuyerAccountById(Long buyerId, Long buyerAccountId);

    public int insertBuyerAccount(Long buyerId, BuyerAccount account);

    public int updateBuyerAccount(Long buyerId, BuyerAccount account);

    public int lockBuyerAccount(Long buyerId, Long buyerAccountId, String lockReason);

    public int unlockBuyerAccount(Long buyerId, Long buyerAccountId);

    public int resetBuyerAccountPassword(Long buyerId, Long buyerAccountId, String password);

    public List<PortalSessionProfile> selectBuyerSessionList(Long buyerId);

    public List<PortalSessionProfile> selectBuyerAccountSessionList(Long buyerId, Long buyerAccountId);

    public int forceLogoutBuyerSessions(Long buyerId);

    public int forceLogoutBuyerAccountSessions(Long buyerId, Long buyerAccountId);

    public PortalDirectLoginResult createBuyerDirectLogin(Long buyerId, String reason);

    public PortalDirectLoginResult createBuyerAccountDirectLogin(Long buyerId, Long buyerAccountId, String reason);

    public List<PortalLoginLog> selectBuyerLoginLogList(PortalLoginLog log);

    public List<PortalOperLog> selectBuyerOperLogList(PortalOperLog log);

    public List<PortalOwnLoginLogProfile> selectBuyerOwnLoginLogList(PortalLoginSession session, PortalLoginLog log);

    public List<PortalOwnOperLogProfile> selectBuyerOwnOperLogList(PortalLoginSession session, PortalOperLog log);

    public List<PortalOwnSessionProfile> selectBuyerOwnSessionList(PortalLoginSession session);

    public List<PortalDirectLoginTicket> selectBuyerDirectLoginTicketList(PortalDirectLoginTicket ticket);

    public PortalLoginResult loginBuyer(LoginBody loginBody);

    public PortalLoginResult directLoginBuyer(String directLoginToken);

    public int logoutBuyer(PortalLoginSession session);

    public int updateBuyerOwnPassword(PortalLoginSession session, PortalPasswordChangeRequest request);
}
