package com.ruoyi.buyer.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginSession;

/**
 * 买家Mapper接口
 */
public interface BuyerMapper
{
    public List<Buyer> selectBuyerList(Buyer buyer);

    public Buyer selectBuyerById(Long buyerId);

    public String selectMaxBuyerNoByPrefix(String buyerNoPrefix);

    public Buyer selectBuyerByCode(String buyerCode);

    public int insertBuyer(Buyer buyer);

    public int updateBuyer(Buyer buyer);

    public int updateBuyerStatus(@Param("buyerId") Long buyerId, @Param("status") String status, @Param("updateBy") String updateBy);

    public List<BuyerAccount> selectBuyerAccountList(@Param("buyerId") Long buyerId);

    public BuyerAccount selectBuyerAccountById(Long buyerAccountId);

    public BuyerAccount selectBuyerAccountByUserName(String userName);

    public BuyerAccount selectOwnerBuyerAccountByBuyerId(@Param("buyerId") Long buyerId);

    public int insertBuyerAccount(BuyerAccount account);

    public int updateBuyerAccount(BuyerAccount account);

    public int resetBuyerAccountPassword(@Param("buyerAccountId") Long buyerAccountId, @Param("password") String password,
            @Param("updateBy") String updateBy);

    public int updateBuyerAccountLoginInfo(@Param("buyerAccountId") Long buyerAccountId,
            @Param("lastLoginIp") String lastLoginIp, @Param("lastLoginTime") Date lastLoginTime);

    public int insertBuyerLoginLog(PortalLoginLog log);

    public int insertBuyerSession(PortalLoginSession session);

    public List<String> selectOnlineBuyerSessionTokenIds(@Param("buyerId") Long buyerId,
            @Param("buyerAccountId") Long buyerAccountId);

    public int forceLogoutBuyerSessions(@Param("buyerId") Long buyerId,
            @Param("buyerAccountId") Long buyerAccountId);
}
