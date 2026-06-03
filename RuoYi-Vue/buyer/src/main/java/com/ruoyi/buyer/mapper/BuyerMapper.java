package com.ruoyi.buyer.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;

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

    public BuyerAccount selectBuyerAccountByUserId(Long userId);

    public BuyerAccount selectOwnerBuyerAccountByBuyerId(@Param("buyerId") Long buyerId);

    public int insertBuyerAccount(BuyerAccount account);
}
