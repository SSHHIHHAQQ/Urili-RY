package com.ruoyi.buyer.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.service.support.PartnerSupport;
import com.ruoyi.system.service.support.PortalDirectLoginSupport;
import com.ruoyi.system.service.support.PortalAccountSupport;

/**
 * Buyer service.
 */
@Service
public class BuyerServiceImpl implements IBuyerService
{
    private static final String BUYER_NO_PREFIX = "B";

    private static final String BUYER_ROLE_KEY = "buyer";

    @Autowired
    private BuyerMapper buyerMapper;

    @Autowired
    private PortalAccountSupport accountSupport;

    @Autowired
    private PortalDirectLoginSupport directLoginSupport;

    @Override
    public List<Buyer> selectBuyerList(Buyer buyer)
    {
        return buyerMapper.selectBuyerList(buyer);
    }

    @Override
    public Buyer selectBuyerById(Long buyerId)
    {
        Buyer buyer = buyerMapper.selectBuyerById(buyerId);
        if (buyer == null)
        {
            throw new ServiceException("买家不存在");
        }
        return buyer;
    }

    @Override
    @Transactional
    public int insertBuyer(Buyer buyer)
    {
        normalizeBuyer(buyer);
        buyer.setBuyerNo(PartnerSupport.generateNo(BUYER_NO_PREFIX, buyerMapper::selectMaxBuyerNoByPrefix));
        buyer.setCreateBy(SecurityUtils.getUsername());
        checkBuyerCodeUnique(buyer);

        int rows = buyerMapper.insertBuyer(buyer);
        createOwnerAccountIfNeeded(buyer);
        return rows;
    }

    @Override
    @Transactional
    public int updateBuyer(Buyer buyer)
    {
        Buyer current = selectBuyerById(buyer.getBuyerId());
        buyer.setBuyerNo(current.getBuyerNo());
        normalizeBuyer(buyer);
        buyer.setUpdateBy(SecurityUtils.getUsername());
        checkBuyerCodeUnique(buyer);

        int rows = buyerMapper.updateBuyer(buyer);
        syncOwnerAccountAfterBuyerUpdate(buyer);
        return rows;
    }

    @Override
    @Transactional
    public int updateBuyerStatus(Buyer buyer)
    {
        selectBuyerById(buyer.getBuyerId());
        PartnerSupport.assertStatus(buyer.getStatus());
        int rows = buyerMapper.updateBuyerStatus(buyer.getBuyerId(), buyer.getStatus(), SecurityUtils.getUsername());
        BuyerAccount owner = buyerMapper.selectOwnerBuyerAccountByBuyerId(buyer.getBuyerId());
        if (owner != null && owner.getUserId() != null)
        {
            accountSupport.updateUserStatus(owner.getUserId(), buyer.getStatus());
        }
        return rows;
    }

    @Override
    public List<BuyerAccount> selectBuyerAccountList(Long buyerId)
    {
        selectBuyerById(buyerId);
        return buyerMapper.selectBuyerAccountList(buyerId);
    }

    @Override
    @Transactional
    public int insertBuyerAccount(Long buyerId, BuyerAccount account)
    {
        Buyer buyer = selectBuyerById(buyerId);
        if (StringUtils.isBlank(account.getPassword()))
        {
            throw new ServiceException("初始密码不能为空");
        }
        if (StringUtils.isBlank(account.getAccountRole()))
        {
            account.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        }
        account.setAccountRole(account.getAccountRole().toUpperCase());
        account.setBuyerId(buyerId);
        account.setStatus(StringUtils.defaultIfBlank(account.getStatus(), PartnerSupport.STATUS_NORMAL));
        account.setCreateBy(SecurityUtils.getUsername());

        SysUser user = accountSupport.createPortalUser(account, BUYER_ROLE_KEY, "买家端账号：" + buyer.getBuyerNo());
        account.setUserId(user.getUserId());
        accountSupport.assertUserNotBoundToAnyPortal(user.getUserId());
        return buyerMapper.insertBuyerAccount(account);
    }

    @Override
    public int resetBuyerAccountPassword(BuyerAccount account)
    {
        if (account.getUserId() == null)
        {
            throw new ServiceException("用户ID不能为空");
        }
        if (StringUtils.isBlank(account.getPassword()))
        {
            throw new ServiceException("新密码不能为空");
        }
        BuyerAccount current = buyerMapper.selectBuyerAccountByUserId(account.getUserId());
        if (current == null)
        {
            throw new ServiceException("买家账号不存在");
        }
        selectBuyerById(current.getBuyerId());
        return accountSupport.resetPassword(account.getUserId(), account.getPassword());
    }

    @Override
    public int resetBuyerAccountDefaultPassword(BuyerAccount account)
    {
        if (account.getUserId() == null)
        {
            throw new ServiceException("用户ID不能为空");
        }
        BuyerAccount current = buyerMapper.selectBuyerAccountByUserId(account.getUserId());
        if (current == null)
        {
            throw new ServiceException("买家账号不存在");
        }
        selectBuyerById(current.getBuyerId());
        return accountSupport.resetPassword(account.getUserId(), PartnerSupport.DEFAULT_OWNER_PASSWORD);
    }

    @Override
    public int resetBuyerOwnerPassword(Long buyerId)
    {
        selectBuyerById(buyerId);
        BuyerAccount owner = buyerMapper.selectOwnerBuyerAccountByBuyerId(buyerId);
        if (owner == null || owner.getUserId() == null)
        {
            throw new ServiceException("买家主账号不存在");
        }
        return accountSupport.resetPassword(owner.getUserId(), PartnerSupport.DEFAULT_OWNER_PASSWORD);
    }

    @Override
    public PortalDirectLoginResult createBuyerDirectLogin(Long buyerId)
    {
        Buyer buyer = selectBuyerById(buyerId);
        BuyerAccount owner = buyerMapper.selectOwnerBuyerAccountByBuyerId(buyerId);
        if (!PartnerSupport.STATUS_NORMAL.equals(buyer.getStatus()))
        {
            throw new ServiceException("买家已停用，不能免密登录");
        }
        return directLoginSupport.createToken("buyer", buyerId, buyer.getBuyerNo(), owner,
            PortalDirectLoginSupport.BUYER_WEB_URL_CONFIG_KEY,
            "http://127.0.0.1:8001/buyer/direct-login");
    }

    private void normalizeBuyer(Buyer buyer)
    {
        buyer.setBuyerCode(PartnerSupport.trimRequired(buyer.getBuyerCode(), "买家代码不能为空"));
        buyer.setBuyerName(PartnerSupport.trimRequired(buyer.getBuyerName(), "买家全称不能为空"));
        buyer.setBuyerShortName(PartnerSupport.trimRequired(buyer.getBuyerShortName(), "买家简称不能为空"));
        buyer.setBuyerType(PartnerSupport.normalizeSubjectType(buyer.getBuyerType()));
        buyer.setBuyerLevel(PartnerSupport.normalizeLevel(buyer.getBuyerLevel(), "买家等级不能为空"));
        PartnerSupport.normalizeCommonProfile(buyer);
    }

    private void createOwnerAccountIfNeeded(Buyer buyer)
    {
        if (StringUtils.isBlank(buyer.getUsername()))
        {
            throw new ServiceException("登录用户名不能为空");
        }
        if (buyerMapper.selectOwnerBuyerAccountByBuyerId(buyer.getBuyerId()) != null)
        {
            return;
        }

        BuyerAccount account = new BuyerAccount();
        account.setUserName(buyer.getUsername());
        account.setNickName(PartnerSupport.buildOwnerNickName(buyer.getBuyerName(), buyer.getBuyerShortName(), buyer.getContactName()));
        account.setPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD);
        account.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        account.setStatus(buyer.getStatus());
        account.setRemark("买家主账号，默认密码 " + PartnerSupport.DEFAULT_OWNER_PASSWORD);
        insertBuyerAccount(buyer.getBuyerId(), account);
    }

    private void syncOwnerAccountAfterBuyerUpdate(Buyer buyer)
    {
        BuyerAccount owner = buyerMapper.selectOwnerBuyerAccountByBuyerId(buyer.getBuyerId());
        if (owner == null)
        {
            if (StringUtils.isNotBlank(buyer.getUsername()))
            {
                createOwnerAccountIfNeeded(buyer);
            }
            return;
        }
        if (StringUtils.isNotBlank(buyer.getUsername()) && !buyer.getUsername().equals(owner.getUserName()))
        {
            throw new ServiceException("登录用户名暂不支持在买家资料中修改");
        }

        accountSupport.syncUserProfile(owner.getUserId(),
            PartnerSupport.buildOwnerNickName(buyer.getBuyerName(), buyer.getBuyerShortName(), buyer.getContactName()),
            buyer.getStatus(),
            "买家端主账号：" + buyer.getBuyerNo());
    }

    private void checkBuyerCodeUnique(Buyer buyer)
    {
        Buyer existing = buyerMapper.selectBuyerByCode(buyer.getBuyerCode());
        if (existing != null && (buyer.getBuyerId() == null || !existing.getBuyerId().equals(buyer.getBuyerId())))
        {
            throw new ServiceException("买家代码已存在");
        }
    }

}
