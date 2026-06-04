package com.ruoyi.seller.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.seller.mapper.SellerPortalDeptMapper;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalDirectLoginTicket;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalDirectLoginToken;
import com.ruoyi.system.domain.PortalLoginIssue;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginResult;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.mapper.PortalDirectLoginTicketMapper;
import com.ruoyi.system.service.support.PortalDirectLoginSupport;
import com.ruoyi.system.service.support.PortalTokenSupport;
import com.ruoyi.system.service.support.PartnerSupport;

/**
 * Seller service.
 */
@Service
public class SellerServiceImpl implements ISellerService
{
    private static final String SELLER_NO_PREFIX = "S";

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private SellerPortalDeptMapper deptMapper;

    @Autowired
    private PortalDirectLoginTicketMapper ticketMapper;

    @Autowired
    private PortalDirectLoginSupport directLoginSupport;

    @Autowired
    private PortalTokenSupport portalTokenSupport;

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
        if (owner != null)
        {
            owner.setStatus(seller.getStatus());
            owner.setUpdateBy(SecurityUtils.getUsername());
            sellerMapper.updateSellerAccount(owner);
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(seller.getStatus()))
        {
            forceLogoutSellerSessionScope(seller.getSellerId(), null);
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
        selectSellerById(sellerId);
        normalizeSellerAccount(account, true);
        account.setSellerId(sellerId);
        account.setCreateBy(SecurityUtils.getUsername());
        validateSellerAccountDept(sellerId, account.getDeptId());
        if (sellerMapper.selectSellerAccountByUserName(account.getUserName()) != null)
        {
            throw new ServiceException("登录账号已存在");
        }
        account.setPassword(SecurityUtils.encryptPassword(account.getPassword()));
        return sellerMapper.insertSellerAccount(account);
    }

    @Override
    @Transactional
    public int updateSellerAccount(Long sellerId, SellerAccount account)
    {
        selectSellerById(sellerId);
        SellerAccount current = selectSellerAccountByPayload(account);
        if (current == null || !sellerId.equals(current.getSellerId()))
        {
            throw new ServiceException("Seller account does not belong to this seller");
        }
        account.setSellerAccountId(current.getSellerAccountId());
        account.setAccountId(current.getSellerAccountId());
        account.setSellerId(sellerId);
        account.setUserName(current.getUserName());
        account.setAccountRole(StringUtils.defaultIfBlank(account.getAccountRole(), current.getAccountRole()));
        normalizeSellerAccount(account, false);
        validateSellerAccountDept(sellerId, account.getDeptId());
        account.setUpdateBy(SecurityUtils.getUsername());
        int rows = sellerMapper.updateSellerAccount(account);
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            forceLogoutSellerSessionScope(sellerId, account.getSellerAccountId());
        }
        return rows;
    }

    @Override
    public int resetSellerAccountPassword(SellerAccount account)
    {
        if (StringUtils.isBlank(account.getPassword()))
        {
            throw new ServiceException("新密码不能为空");
        }
        SellerAccount current = selectSellerAccountByPayload(account);
        if (current == null)
        {
            throw new ServiceException("卖家账号不存在");
        }
        selectSellerById(current.getSellerId());
        return sellerMapper.resetSellerAccountPassword(current.getSellerAccountId(),
            SecurityUtils.encryptPassword(account.getPassword()), SecurityUtils.getUsername());
    }

    @Override
    public int resetSellerAccountDefaultPassword(SellerAccount account)
    {
        SellerAccount current = selectSellerAccountByPayload(account);
        if (current == null)
        {
            throw new ServiceException("卖家账号不存在");
        }
        selectSellerById(current.getSellerId());
        return sellerMapper.resetSellerAccountPassword(current.getSellerAccountId(),
            SecurityUtils.encryptPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD), SecurityUtils.getUsername());
    }

    @Override
    public int resetSellerOwnerPassword(Long sellerId)
    {
        selectSellerById(sellerId);
        SellerAccount owner = sellerMapper.selectOwnerSellerAccountBySellerId(sellerId);
        if (owner == null || owner.getSellerAccountId() == null)
        {
            throw new ServiceException("卖家主账号不存在");
        }
        return sellerMapper.resetSellerAccountPassword(owner.getSellerAccountId(),
            SecurityUtils.encryptPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD), SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int forceLogoutSellerSessions(Long sellerId)
    {
        selectSellerById(sellerId);
        return forceLogoutSellerSessionScope(sellerId, null);
    }

    @Override
    @Transactional
    public int forceLogoutSellerAccountSessions(Long sellerId, Long sellerAccountId)
    {
        SellerAccount account = sellerMapper.selectSellerAccountById(sellerAccountId);
        if (account == null || !sellerId.equals(account.getSellerId()))
        {
            throw new ServiceException("卖家端账号不存在");
        }
        return forceLogoutSellerSessionScope(sellerId, sellerAccountId);
    }

    @Override
    public PortalDirectLoginResult createSellerDirectLogin(Long sellerId, String reason)
    {
        Seller seller = selectSellerById(sellerId);
        SellerAccount owner = sellerMapper.selectOwnerSellerAccountBySellerId(sellerId);
        if (!PartnerSupport.STATUS_NORMAL.equals(seller.getStatus()))
        {
            throw new ServiceException("卖家已停用，不能免密登录");
        }
        return directLoginSupport.createToken("seller", sellerId, seller.getSellerNo(), owner, reason,
            PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY,
            "http://127.0.0.1:8001/seller/direct-login");
    }

    @Override
    public List<PortalLoginLog> selectSellerLoginLogList(PortalLoginLog log)
    {
        return sellerMapper.selectSellerLoginLogList(log);
    }

    @Override
    public List<PortalOperLog> selectSellerOperLogList(PortalOperLog log)
    {
        return sellerMapper.selectSellerOperLogList(log);
    }

    @Override
    public List<PortalDirectLoginTicket> selectSellerDirectLoginTicketList(PortalDirectLoginTicket ticket)
    {
        PortalDirectLoginTicket query = ticket == null ? new PortalDirectLoginTicket() : ticket;
        query.setTerminal("seller");
        return ticketMapper.selectPortalDirectLoginTicketList(query);
    }

    @Override
    public PortalLoginResult loginSeller(LoginBody loginBody)
    {
        String username = loginBody == null ? null : StringUtils.trimToEmpty(loginBody.getUsername());
        String password = loginBody == null ? null : loginBody.getPassword();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password))
        {
            recordSellerLoginFailure(null, null, username, "账号或密码不能为空");
            throw new ServiceException("账号或密码不能为空");
        }

        SellerAccount account = sellerMapper.selectSellerAccountByUserName(username);
        if (account == null || StringUtils.isBlank(account.getPassword())
                || !SecurityUtils.matchesPassword(password, account.getPassword()))
        {
            recordSellerLoginFailure(null, account, username, "账号或密码错误");
            throw new ServiceException("账号或密码错误");
        }

        Seller seller = sellerMapper.selectSellerById(account.getSellerId());
        assertSellerCanLogin(seller, account, username);
        PortalLoginIssue issue = portalTokenSupport.createLogin("seller", seller.getSellerId(), seller.getSellerNo(), account);
        recordSellerLoginSuccess(account, issue, "登录成功");
        return issue.getResult();
    }

    @Override
    public PortalLoginResult directLoginSeller(String directLoginToken)
    {
        PortalDirectLoginToken token = directLoginSupport.consumeToken("seller", directLoginToken);
        Seller seller = sellerMapper.selectSellerById(token.getPartnerId());
        SellerAccount account = sellerMapper.selectSellerAccountById(token.getAccountId());
        if (account == null || !token.getPartnerId().equals(account.getSellerId()))
        {
            recordSellerLoginFailure(token.getPartnerId(), null, token.getUsername(), "免密登录目标账号不存在");
            throw new ServiceException("免密登录目标账号不存在");
        }

        assertSellerCanLogin(seller, account, token.getUsername());
        PortalLoginIssue issue = portalTokenSupport.createLogin("seller", seller.getSellerId(), seller.getSellerNo(), account);
        recordSellerLoginSuccess(account, issue, "免密登录成功");
        return issue.getResult();
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
        account.setRemark("卖家主账号");
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

        owner.setNickName(PartnerSupport.buildOwnerNickName(seller.getSellerName(), seller.getSellerShortName(), seller.getContactName()));
        owner.setStatus(seller.getStatus());
        owner.setUpdateBy(SecurityUtils.getUsername());
        owner.setRemark("卖家主账号");
        sellerMapper.updateSellerAccount(owner);
    }

    private void normalizeSellerAccount(SellerAccount account, boolean requirePassword)
    {
        account.setUserName(PartnerSupport.trimRequired(account.getUserName(), "登录账号不能为空"));
        account.setNickName(PartnerSupport.trimRequired(account.getNickName(), "姓名不能为空"));
        if (requirePassword)
        {
            account.setPassword(PartnerSupport.trimRequired(account.getPassword(), "初始密码不能为空"));
        }
        account.setEmail(StringUtils.trimToEmpty(account.getEmail()));
        account.setPhonenumber(StringUtils.trimToEmpty(account.getPhonenumber()));
        if (StringUtils.isBlank(account.getAccountRole()))
        {
            account.setAccountRole(PartnerSupport.ACCOUNT_ROLE_STAFF);
        }
        account.setAccountRole(account.getAccountRole().toUpperCase());
        account.setStatus(StringUtils.defaultIfBlank(account.getStatus(), PartnerSupport.STATUS_NORMAL));
        PartnerSupport.assertStatus(account.getStatus());
    }

    private void validateSellerAccountDept(Long sellerId, Long deptId)
    {
        if (deptId == null)
        {
            return;
        }
        PortalDept dept = deptMapper.selectSellerDeptById(sellerId, deptId);
        if (dept == null)
        {
            throw new ServiceException("Seller department does not belong to this seller");
        }
    }

    private int forceLogoutSellerSessionScope(Long sellerId, Long sellerAccountId)
    {
        List<String> tokenIds = sellerMapper.selectOnlineSellerSessionTokenIds(sellerId, sellerAccountId);
        int rows = sellerMapper.forceLogoutSellerSessions(sellerId, sellerAccountId);
        portalTokenSupport.deleteLoginTokens("seller", tokenIds);
        return rows;
    }

    private SellerAccount selectSellerAccountByPayload(SellerAccount account)
    {
        Long accountId = account.getSellerAccountId() != null ? account.getSellerAccountId() : account.getAccountId();
        if (accountId == null)
        {
            throw new ServiceException("卖家账号ID不能为空");
        }
        return sellerMapper.selectSellerAccountById(accountId);
    }

    private void assertSellerCanLogin(Seller seller, SellerAccount account, String username)
    {
        if (seller == null)
        {
            recordSellerLoginFailure(account == null ? null : account.getSellerId(), account, username, "卖家主体不存在");
            throw new ServiceException("卖家主体不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(seller.getStatus()))
        {
            recordSellerLoginFailure(seller.getSellerId(), account, username, "卖家已停用");
            throw new ServiceException("卖家已停用");
        }
        if (account == null || !PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            recordSellerLoginFailure(seller.getSellerId(), account, username, "卖家账号已停用");
            throw new ServiceException("卖家账号已停用");
        }
    }

    private void recordSellerLoginSuccess(SellerAccount account, PortalLoginIssue issue, String message)
    {
        PortalLoginSession session = issue.getSession();
        sellerMapper.updateSellerAccountLoginInfo(account.getSellerAccountId(), session.getLoginIp(), session.getLoginTime());
        sellerMapper.insertSellerLoginLog(portalTokenSupport.buildLoginLog(
            account.getSellerId(), account.getSellerAccountId(), account.getUserName(), Constants.SUCCESS, message));
        sellerMapper.insertSellerSession(session);
    }

    private void recordSellerLoginFailure(Long sellerId, SellerAccount account, String username, String message)
    {
        Long accountId = account == null ? null : account.getSellerAccountId();
        Long subjectId = sellerId != null ? sellerId : (account == null ? null : account.getSellerId());
        PortalLoginLog log = portalTokenSupport.buildLoginLog(subjectId, accountId, username, Constants.FAIL, message);
        sellerMapper.insertSellerLoginLog(log);
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
