package com.ruoyi.seller.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.service.support.PortalDirectLoginSupport;
import com.ruoyi.system.service.support.PartnerSupport;
import com.ruoyi.system.service.support.PortalAccountSupport;

/**
 * Seller service.
 */
@Service
public class SellerServiceImpl implements ISellerService
{
    private static final String SELLER_NO_PREFIX = "S";

    private static final String SELLER_ROLE_KEY = "seller";

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private PortalAccountSupport accountSupport;

    @Autowired
    private PortalDirectLoginSupport directLoginSupport;

    @Override
    public List<Seller> selectSellerList(Seller seller)
    {
        return sellerMapper.selectSellerList(seller);
    }

    @Override
    public Seller selectSellerById(Long sellerId)
    {
        Seller seller = sellerMapper.selectSellerById(sellerId);
        if (seller == null)
        {
            throw new ServiceException("卖家不存在");
        }
        return seller;
    }

    @Override
    @Transactional
    public int insertSeller(Seller seller)
    {
        normalizeSeller(seller);
        seller.setSellerNo(PartnerSupport.generateNo(SELLER_NO_PREFIX, sellerMapper::selectMaxSellerNoByPrefix));
        seller.setCreateBy(SecurityUtils.getUsername());
        checkSellerCodeUnique(seller);

        int rows = sellerMapper.insertSeller(seller);
        createOwnerAccountIfNeeded(seller);
        return rows;
    }

    @Override
    @Transactional
    public int updateSeller(Seller seller)
    {
        Seller current = selectSellerById(seller.getSellerId());
        seller.setSellerNo(current.getSellerNo());
        normalizeSeller(seller, current.getAttachmentFileUrl());
        seller.setUpdateBy(SecurityUtils.getUsername());
        checkSellerCodeUnique(seller);

        int rows = sellerMapper.updateSeller(seller);
        syncOwnerAccountAfterSellerUpdate(seller);
        return rows;
    }

    @Override
    @Transactional
    public int updateSellerStatus(Seller seller)
    {
        selectSellerById(seller.getSellerId());
        PartnerSupport.assertStatus(seller.getStatus());
        int rows = sellerMapper.updateSellerStatus(seller.getSellerId(), seller.getStatus(), SecurityUtils.getUsername());
        SellerAccount owner = sellerMapper.selectOwnerSellerAccountBySellerId(seller.getSellerId());
        if (owner != null && owner.getUserId() != null)
        {
            accountSupport.updateUserStatus(owner.getUserId(), seller.getStatus());
        }
        return rows;
    }

    @Override
    public List<SellerAccount> selectSellerAccountList(Long sellerId)
    {
        selectSellerById(sellerId);
        return sellerMapper.selectSellerAccountList(sellerId);
    }

    @Override
    @Transactional
    public int insertSellerAccount(Long sellerId, SellerAccount account)
    {
        Seller seller = selectSellerById(sellerId);
        if (StringUtils.isBlank(account.getPassword()))
        {
            throw new ServiceException("初始密码不能为空");
        }
        if (StringUtils.isBlank(account.getAccountRole()))
        {
            account.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        }
        account.setAccountRole(account.getAccountRole().toUpperCase());
        account.setSellerId(sellerId);
        account.setStatus(StringUtils.defaultIfBlank(account.getStatus(), PartnerSupport.STATUS_NORMAL));
        account.setCreateBy(SecurityUtils.getUsername());

        SysUser user = accountSupport.createPortalUser(account, SELLER_ROLE_KEY, "卖家端账号：" + seller.getSellerNo());
        account.setUserId(user.getUserId());
        accountSupport.assertUserNotBoundToAnyPortal(user.getUserId());
        return sellerMapper.insertSellerAccount(account);
    }

    @Override
    public int resetSellerAccountPassword(SellerAccount account)
    {
        if (account.getUserId() == null)
        {
            throw new ServiceException("用户ID不能为空");
        }
        if (StringUtils.isBlank(account.getPassword()))
        {
            throw new ServiceException("新密码不能为空");
        }
        SellerAccount current = sellerMapper.selectSellerAccountByUserId(account.getUserId());
        if (current == null)
        {
            throw new ServiceException("卖家账号不存在");
        }
        selectSellerById(current.getSellerId());
        return accountSupport.resetPassword(account.getUserId(), account.getPassword());
    }

    @Override
    public int resetSellerAccountDefaultPassword(SellerAccount account)
    {
        if (account.getUserId() == null)
        {
            throw new ServiceException("用户ID不能为空");
        }
        SellerAccount current = sellerMapper.selectSellerAccountByUserId(account.getUserId());
        if (current == null)
        {
            throw new ServiceException("卖家账号不存在");
        }
        selectSellerById(current.getSellerId());
        return accountSupport.resetPassword(account.getUserId(), PartnerSupport.DEFAULT_OWNER_PASSWORD);
    }

    @Override
    public int resetSellerOwnerPassword(Long sellerId)
    {
        selectSellerById(sellerId);
        SellerAccount owner = sellerMapper.selectOwnerSellerAccountBySellerId(sellerId);
        if (owner == null || owner.getUserId() == null)
        {
            throw new ServiceException("卖家主账号不存在");
        }
        return accountSupport.resetPassword(owner.getUserId(), PartnerSupport.DEFAULT_OWNER_PASSWORD);
    }

    @Override
    public PortalDirectLoginResult createSellerDirectLogin(Long sellerId)
    {
        Seller seller = selectSellerById(sellerId);
        SellerAccount owner = sellerMapper.selectOwnerSellerAccountBySellerId(sellerId);
        if (!PartnerSupport.STATUS_NORMAL.equals(seller.getStatus()))
        {
            throw new ServiceException("卖家已停用，不能免密登录");
        }
        return directLoginSupport.createToken("seller", sellerId, seller.getSellerNo(), owner,
            PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY,
            "http://127.0.0.1:8001/seller/direct-login");
    }

    private void normalizeSeller(Seller seller)
    {
        normalizeSeller(seller, null);
    }

    private void normalizeSeller(Seller seller, String unchangedLegacyAttachmentUrl)
    {
        seller.setSellerCode(PartnerSupport.trimRequired(seller.getSellerCode(), "卖家代码不能为空"));
        seller.setSellerName(PartnerSupport.trimRequired(seller.getSellerName(), "卖家全称不能为空"));
        seller.setSellerShortName(PartnerSupport.trimRequired(seller.getSellerShortName(), "卖家简称不能为空"));
        seller.setSellerType(PartnerSupport.normalizeSubjectType(seller.getSellerType()));
        seller.setSellerLevel(PartnerSupport.normalizeLevel(seller.getSellerLevel(), "卖家等级不能为空"));
        PartnerSupport.normalizeCommonProfile(seller, unchangedLegacyAttachmentUrl);
    }

    private void createOwnerAccountIfNeeded(Seller seller)
    {
        if (StringUtils.isBlank(seller.getUsername()))
        {
            throw new ServiceException("登录用户名不能为空");
        }
        if (sellerMapper.selectOwnerSellerAccountBySellerId(seller.getSellerId()) != null)
        {
            return;
        }

        SellerAccount account = new SellerAccount();
        account.setUserName(seller.getUsername());
        account.setNickName(PartnerSupport.buildOwnerNickName(seller.getSellerName(), seller.getSellerShortName(), seller.getContactName()));
        account.setPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD);
        account.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        account.setStatus(seller.getStatus());
        account.setRemark("卖家主账号，默认密码 " + PartnerSupport.DEFAULT_OWNER_PASSWORD);
        insertSellerAccount(seller.getSellerId(), account);
    }

    private void syncOwnerAccountAfterSellerUpdate(Seller seller)
    {
        SellerAccount owner = sellerMapper.selectOwnerSellerAccountBySellerId(seller.getSellerId());
        if (owner == null)
        {
            if (StringUtils.isNotBlank(seller.getUsername()))
            {
                createOwnerAccountIfNeeded(seller);
            }
            return;
        }
        if (StringUtils.isNotBlank(seller.getUsername()) && !seller.getUsername().equals(owner.getUserName()))
        {
            throw new ServiceException("登录用户名暂不支持在卖家资料中修改");
        }

        accountSupport.syncUserProfile(owner.getUserId(),
            PartnerSupport.buildOwnerNickName(seller.getSellerName(), seller.getSellerShortName(), seller.getContactName()),
            seller.getStatus(),
            "卖家端主账号：" + seller.getSellerNo());
    }

    private void checkSellerCodeUnique(Seller seller)
    {
        Seller existing = sellerMapper.selectSellerByCode(seller.getSellerCode());
        if (existing != null && (seller.getSellerId() == null || !existing.getSellerId().equals(seller.getSellerId())))
        {
            throw new ServiceException("卖家代码已存在");
        }
    }

}
