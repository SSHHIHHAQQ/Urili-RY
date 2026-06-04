package com.ruoyi.buyer.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.buyer.mapper.BuyerPortalDeptMapper;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalDirectLoginToken;
import com.ruoyi.system.domain.PortalLoginIssue;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginResult;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.support.PartnerSupport;
import com.ruoyi.system.service.support.PortalDirectLoginSupport;
import com.ruoyi.system.service.support.PortalTokenSupport;

/**
 * Buyer service.
 */
@Service
public class BuyerServiceImpl implements IBuyerService
{
    private static final String BUYER_NO_PREFIX = "B";

    @Autowired
    private BuyerMapper buyerMapper;

    @Autowired
    private BuyerPortalDeptMapper deptMapper;

    @Autowired
    private PortalDirectLoginSupport directLoginSupport;

    @Autowired
    private PortalTokenSupport portalTokenSupport;

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
        normalizeBuyer(buyer, current.getAttachmentFileUrl());
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
        if (owner != null)
        {
            owner.setStatus(buyer.getStatus());
            owner.setUpdateBy(SecurityUtils.getUsername());
            buyerMapper.updateBuyerAccount(owner);
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(buyer.getStatus()))
        {
            forceLogoutBuyerSessionScope(buyer.getBuyerId(), null);
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
        selectBuyerById(buyerId);
        normalizeBuyerAccount(account, true);
        account.setBuyerId(buyerId);
        account.setCreateBy(SecurityUtils.getUsername());
        validateBuyerAccountDept(buyerId, account.getDeptId());
        if (buyerMapper.selectBuyerAccountByUserName(account.getUserName()) != null)
        {
            throw new ServiceException("登录账号已存在");
        }
        account.setPassword(SecurityUtils.encryptPassword(account.getPassword()));
        return buyerMapper.insertBuyerAccount(account);
    }

    @Override
    @Transactional
    public int updateBuyerAccount(Long buyerId, BuyerAccount account)
    {
        selectBuyerById(buyerId);
        BuyerAccount current = selectBuyerAccountByPayload(account);
        if (current == null || !buyerId.equals(current.getBuyerId()))
        {
            throw new ServiceException("Buyer account does not belong to this buyer");
        }
        account.setBuyerAccountId(current.getBuyerAccountId());
        account.setAccountId(current.getBuyerAccountId());
        account.setBuyerId(buyerId);
        account.setUserName(current.getUserName());
        account.setAccountRole(StringUtils.defaultIfBlank(account.getAccountRole(), current.getAccountRole()));
        normalizeBuyerAccount(account, false);
        validateBuyerAccountDept(buyerId, account.getDeptId());
        account.setUpdateBy(SecurityUtils.getUsername());
        int rows = buyerMapper.updateBuyerAccount(account);
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            forceLogoutBuyerSessionScope(buyerId, account.getBuyerAccountId());
        }
        return rows;
    }

    @Override
    public int resetBuyerAccountPassword(BuyerAccount account)
    {
        if (StringUtils.isBlank(account.getPassword()))
        {
            throw new ServiceException("新密码不能为空");
        }
        BuyerAccount current = selectBuyerAccountByPayload(account);
        if (current == null)
        {
            throw new ServiceException("买家账号不存在");
        }
        selectBuyerById(current.getBuyerId());
        return buyerMapper.resetBuyerAccountPassword(current.getBuyerAccountId(),
            SecurityUtils.encryptPassword(account.getPassword()), SecurityUtils.getUsername());
    }

    @Override
    public int resetBuyerAccountDefaultPassword(BuyerAccount account)
    {
        BuyerAccount current = selectBuyerAccountByPayload(account);
        if (current == null)
        {
            throw new ServiceException("买家账号不存在");
        }
        selectBuyerById(current.getBuyerId());
        return buyerMapper.resetBuyerAccountPassword(current.getBuyerAccountId(),
            SecurityUtils.encryptPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD), SecurityUtils.getUsername());
    }

    @Override
    public int resetBuyerOwnerPassword(Long buyerId)
    {
        selectBuyerById(buyerId);
        BuyerAccount owner = buyerMapper.selectOwnerBuyerAccountByBuyerId(buyerId);
        if (owner == null || owner.getBuyerAccountId() == null)
        {
            throw new ServiceException("买家主账号不存在");
        }
        return buyerMapper.resetBuyerAccountPassword(owner.getBuyerAccountId(),
            SecurityUtils.encryptPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD), SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int forceLogoutBuyerSessions(Long buyerId)
    {
        selectBuyerById(buyerId);
        return forceLogoutBuyerSessionScope(buyerId, null);
    }

    @Override
    @Transactional
    public int forceLogoutBuyerAccountSessions(Long buyerId, Long buyerAccountId)
    {
        BuyerAccount account = buyerMapper.selectBuyerAccountById(buyerAccountId);
        if (account == null || !buyerId.equals(account.getBuyerId()))
        {
            throw new ServiceException("买家端账号不存在");
        }
        return forceLogoutBuyerSessionScope(buyerId, buyerAccountId);
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

    @Override
    public PortalLoginResult loginBuyer(LoginBody loginBody)
    {
        String username = loginBody == null ? null : StringUtils.trimToEmpty(loginBody.getUsername());
        String password = loginBody == null ? null : loginBody.getPassword();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password))
        {
            recordBuyerLoginFailure(null, null, username, "账号或密码不能为空");
            throw new ServiceException("账号或密码不能为空");
        }

        BuyerAccount account = buyerMapper.selectBuyerAccountByUserName(username);
        if (account == null || StringUtils.isBlank(account.getPassword())
                || !SecurityUtils.matchesPassword(password, account.getPassword()))
        {
            recordBuyerLoginFailure(null, account, username, "账号或密码错误");
            throw new ServiceException("账号或密码错误");
        }

        Buyer buyer = buyerMapper.selectBuyerById(account.getBuyerId());
        assertBuyerCanLogin(buyer, account, username);
        PortalLoginIssue issue = portalTokenSupport.createLogin("buyer", buyer.getBuyerId(), buyer.getBuyerNo(), account);
        recordBuyerLoginSuccess(account, issue, "登录成功");
        return issue.getResult();
    }

    @Override
    public PortalLoginResult directLoginBuyer(String directLoginToken)
    {
        PortalDirectLoginToken token = directLoginSupport.consumeToken("buyer", directLoginToken);
        Buyer buyer = buyerMapper.selectBuyerById(token.getPartnerId());
        BuyerAccount account = buyerMapper.selectBuyerAccountById(token.getAccountId());
        if (account == null || !token.getPartnerId().equals(account.getBuyerId()))
        {
            recordBuyerLoginFailure(token.getPartnerId(), null, token.getUsername(), "免密登录目标账号不存在");
            throw new ServiceException("免密登录目标账号不存在");
        }

        assertBuyerCanLogin(buyer, account, token.getUsername());
        PortalLoginIssue issue = portalTokenSupport.createLogin("buyer", buyer.getBuyerId(), buyer.getBuyerNo(), account);
        recordBuyerLoginSuccess(account, issue, "免密登录成功");
        return issue.getResult();
    }

    private void normalizeBuyer(Buyer buyer)
    {
        normalizeBuyer(buyer, null);
    }

    private void normalizeBuyer(Buyer buyer, String unchangedLegacyAttachmentUrl)
    {
        buyer.setBuyerCode(PartnerSupport.trimRequired(buyer.getBuyerCode(), "买家代码不能为空"));
        buyer.setBuyerName(PartnerSupport.trimRequired(buyer.getBuyerName(), "买家全称不能为空"));
        buyer.setBuyerShortName(PartnerSupport.trimRequired(buyer.getBuyerShortName(), "买家简称不能为空"));
        buyer.setBuyerType(PartnerSupport.normalizeSubjectType(buyer.getBuyerType()));
        buyer.setBuyerLevel(PartnerSupport.normalizeLevel(buyer.getBuyerLevel(), "买家等级不能为空"));
        PartnerSupport.normalizeCommonProfile(buyer, unchangedLegacyAttachmentUrl);
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
        account.setRemark("买家主账号");
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

        owner.setNickName(PartnerSupport.buildOwnerNickName(buyer.getBuyerName(), buyer.getBuyerShortName(), buyer.getContactName()));
        owner.setStatus(buyer.getStatus());
        owner.setUpdateBy(SecurityUtils.getUsername());
        owner.setRemark("买家主账号");
        buyerMapper.updateBuyerAccount(owner);
    }

    private void normalizeBuyerAccount(BuyerAccount account, boolean requirePassword)
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

    private void validateBuyerAccountDept(Long buyerId, Long deptId)
    {
        if (deptId == null)
        {
            return;
        }
        PortalDept dept = deptMapper.selectBuyerDeptById(buyerId, deptId);
        if (dept == null)
        {
            throw new ServiceException("Buyer department does not belong to this buyer");
        }
    }

    private int forceLogoutBuyerSessionScope(Long buyerId, Long buyerAccountId)
    {
        List<String> tokenIds = buyerMapper.selectOnlineBuyerSessionTokenIds(buyerId, buyerAccountId);
        int rows = buyerMapper.forceLogoutBuyerSessions(buyerId, buyerAccountId);
        portalTokenSupport.deleteLoginTokens("buyer", tokenIds);
        return rows;
    }

    private BuyerAccount selectBuyerAccountByPayload(BuyerAccount account)
    {
        Long accountId = account.getBuyerAccountId() != null ? account.getBuyerAccountId() : account.getAccountId();
        if (accountId == null)
        {
            throw new ServiceException("买家账号ID不能为空");
        }
        return buyerMapper.selectBuyerAccountById(accountId);
    }

    private void assertBuyerCanLogin(Buyer buyer, BuyerAccount account, String username)
    {
        if (buyer == null)
        {
            recordBuyerLoginFailure(account == null ? null : account.getBuyerId(), account, username, "买家主体不存在");
            throw new ServiceException("买家主体不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(buyer.getStatus()))
        {
            recordBuyerLoginFailure(buyer.getBuyerId(), account, username, "买家已停用");
            throw new ServiceException("买家已停用");
        }
        if (account == null || !PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            recordBuyerLoginFailure(buyer.getBuyerId(), account, username, "买家账号已停用");
            throw new ServiceException("买家账号已停用");
        }
    }

    private void recordBuyerLoginSuccess(BuyerAccount account, PortalLoginIssue issue, String message)
    {
        PortalLoginSession session = issue.getSession();
        buyerMapper.updateBuyerAccountLoginInfo(account.getBuyerAccountId(), session.getLoginIp(), session.getLoginTime());
        buyerMapper.insertBuyerLoginLog(portalTokenSupport.buildLoginLog(
            account.getBuyerId(), account.getBuyerAccountId(), account.getUserName(), Constants.SUCCESS, message));
        buyerMapper.insertBuyerSession(session);
    }

    private void recordBuyerLoginFailure(Long buyerId, BuyerAccount account, String username, String message)
    {
        Long accountId = account == null ? null : account.getBuyerAccountId();
        Long subjectId = buyerId != null ? buyerId : (account == null ? null : account.getBuyerId());
        PortalLoginLog log = portalTokenSupport.buildLoginLog(subjectId, accountId, username, Constants.FAIL, message);
        buyerMapper.insertBuyerLoginLog(log);
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
