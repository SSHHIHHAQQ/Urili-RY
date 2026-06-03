package com.ruoyi.seller.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;

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

    public SellerAccount selectSellerAccountByUserId(Long userId);

    public SellerAccount selectOwnerSellerAccountBySellerId(@Param("sellerId") Long sellerId);

    public int insertSellerAccount(SellerAccount account);
}
