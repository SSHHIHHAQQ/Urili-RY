package com.ruoyi.seller.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.domain.PortalSessionProfile;

/**
 * 卖家Mapper接口
 */
public interface SellerMapper
{
    public List<Seller> selectSellerList(Seller seller);

    public Seller selectSellerById(Long sellerId);

    public String selectMaxSellerNoByPrefix(String sellerNoPrefix);

    public Seller selectSellerByCode(String sellerCode);

    public int insertSeller(Seller seller);

    public int updateSeller(Seller seller);

    public int updateSellerStatus(@Param("sellerId") Long sellerId, @Param("status") String status, @Param("updateBy") String updateBy);

    public List<SellerAccount> selectSellerAccountList(@Param("sellerId") Long sellerId);

    public SellerAccount selectSellerAccountById(Long sellerAccountId);

    public SellerAccount selectSellerAccountByUserName(String userName);

    public SellerAccount selectOwnerSellerAccountBySellerId(@Param("sellerId") Long sellerId);

    public int insertSellerAccount(SellerAccount account);

    public int updateSellerAccount(SellerAccount account);

    public int updateSellerAccountLockStatus(@Param("sellerId") Long sellerId, @Param("sellerAccountId") Long sellerAccountId,
            @Param("lockStatus") String lockStatus, @Param("lockReason") String lockReason,
            @Param("updateBy") String updateBy);

    public int resetSellerAccountPassword(@Param("sellerId") Long sellerId,
            @Param("sellerAccountId") Long sellerAccountId, @Param("password") String password,
            @Param("updateBy") String updateBy);

    public int updateSellerAccountLoginInfo(@Param("sellerId") Long sellerId,
            @Param("sellerAccountId") Long sellerAccountId,
            @Param("lastLoginIp") String lastLoginIp, @Param("lastLoginTime") Date lastLoginTime);

    public int insertSellerLoginLog(PortalLoginLog log);

    public List<PortalLoginLog> selectSellerLoginLogList(PortalLoginLog log);

    public List<PortalOperLog> selectSellerOperLogList(PortalOperLog log);

    public int insertSellerSession(PortalLoginSession session);

    public List<PortalSessionProfile> selectSellerSessionProfileList(@Param("sellerId") Long sellerId,
            @Param("sellerAccountId") Long sellerAccountId);

    public List<PortalLoginSession> selectOnlineSellerSessionList(@Param("sellerId") Long sellerId,
            @Param("sellerAccountId") Long sellerAccountId);

    public List<String> selectOnlineSellerSessionTokenIds(@Param("sellerId") Long sellerId,
            @Param("sellerAccountId") Long sellerAccountId);

    public int countOnlineSellerSession(@Param("sellerId") Long sellerId,
            @Param("sellerAccountId") Long sellerAccountId, @Param("tokenId") String tokenId);

    public int forceLogoutSellerSessions(@Param("sellerId") Long sellerId,
            @Param("sellerAccountId") Long sellerAccountId);

    public int logoutSellerSession(@Param("sellerId") Long sellerId,
            @Param("sellerAccountId") Long sellerAccountId, @Param("tokenId") String tokenId);
}
