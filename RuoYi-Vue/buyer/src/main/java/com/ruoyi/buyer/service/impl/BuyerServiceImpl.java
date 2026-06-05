package com.ruoyi.buyer.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.ruoyi.system.domain.PortalDirectLoginTicket;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalDirectLoginToken;
import com.ruoyi.system.domain.PortalLoginIssue;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginResult;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.domain.PortalPasswordChangeRequest;
import com.ruoyi.system.domain.PortalSessionProfile;
import com.ruoyi.system.mapper.PortalDirectLoginTicketMapper;
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
    private PortalDirectLoginTicketMapper ticketMapper;

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
    public BuyerAccount selectBuyerAccountById(Long buyerId, Long buyerAccountId)
    {
        selectBuyerById(buyerId);
        BuyerAccount account = buyerMapper.selectBuyerAccountById(buyerAccountId);
        if (account == null || !Objects.equals(account.getBuyerId(), buyerId))
        {
            throw new ServiceException("买家账号不存在");
        }
        return account;
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
    public List<PortalSessionProfile> selectBuyerSessionList(Long buyerId)
    {
        return buyerMapper.selectBuyerSessionProfileList(buyerId, null);
    }

    @Override
    public List<PortalSessionProfile> selectBuyerAccountSessionList(Long buyerId, Long buyerAccountId)
    {
        return buyerMapper.selectBuyerSessionProfileList(buyerId, buyerAccountId);
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
    public PortalDirectLoginResult createBuyerDirectLogin(Long buyerId, String reason)
    {
        Buyer buyer = selectBuyerById(buyerId);
        BuyerAccount owner = buyerMapper.selectOwnerBuyerAccountByBuyerId(buyerId);
        if (!PartnerSupport.STATUS_NORMAL.equals(buyer.getStatus()))
        {
            throw new ServiceException("买家已停用，不能免密登录");
        }
        return directLoginSupport.createToken("buyer", buyerId, buyer.getBuyerNo(), owner, reason,
            PortalDirectLoginSupport.BUYER_WEB_URL_CONFIG_KEY,
            "http://127.0.0.1:8001/buyer/direct-login");
    }

    @Override
    public PortalDirectLoginResult createBuyerAccountDirectLogin(Long buyerId, Long buyerAccountId, String reason)
    {
        Buyer buyer = selectBuyerById(buyerId);
        BuyerAccount account = buyerMapper.selectBuyerAccountById(buyerAccountId);
        if (account == null || !buyerId.equals(account.getBuyerId()))
        {
            throw new ServiceException("买家账号不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(buyer.getStatus()))
        {
            throw new ServiceException("买家已停用，不能免密登录");
        }
        return directLoginSupport.createToken("buyer", buyerId, buyer.getBuyerNo(), account, reason,
            PortalDirectLoginSupport.BUYER_WEB_URL_CONFIG_KEY,
            "http://127.0.0.1:8001/buyer/direct-login");
    }

    @Override
    public List<PortalLoginLog> selectBuyerLoginLogList(PortalLoginLog log)
    {
        return buyerMapper.selectBuyerLoginLogList(log);
    }

    @Override
    public List<PortalOperLog> selectBuyerOperLogList(PortalOperLog log)
    {
        return buyerMapper.selectBuyerOperLogList(log);
    }

    @Override
    public List<PortalLoginLog> selectBuyerOwnLoginLogList(PortalLoginSession session, PortalLoginLog log)
    {
        assertBuyerSessionAccount(session);
        return buyerMapper.selectBuyerLoginLogList(buildBuyerOwnLoginLogQuery(session, log));
    }

    @Override
    public List<PortalOperLog> selectBuyerOwnOperLogList(PortalLoginSession session, PortalOperLog log)
    {
        assertBuyerSessionAccount(session);
        return buyerMapper.selectBuyerOperLogList(buildBuyerOwnOperLogQuery(session, log));
    }

    @Override
    public List<PortalSessionProfile> selectBuyerOwnSessionList(PortalLoginSession session)
    {
        assertBuyerSessionAccount(session);
        List<PortalSessionProfile> sessions = buyerMapper.selectBuyerSessionProfileList(
            session.getSubjectId(), session.getAccountId());
        for (PortalSessionProfile profile : sessions)
        {
            profile.setCurrent(Objects.equals(session.getTokenId(), profile.getTokenId()));
        }
        return sessions;
    }

    private void assertBuyerSessionAccount(PortalLoginSession session)
    {
        if (session == null || session.getSubjectId() == null || session.getAccountId() == null)
        {
            throw new ServiceException("登录状态已失效");
        }
        selectBuyerAccountById(session.getSubjectId(), session.getAccountId());
    }

    private PortalLoginLog buildBuyerOwnLoginLogQuery(PortalLoginSession session, PortalLoginLog log)
    {
        PortalLoginLog query = new PortalLoginLog();
        if (log != null)
        {
            query.setUserName(log.getUserName());
            query.setIpaddr(log.getIpaddr());
            query.setStatus(log.getStatus());
            query.setParams(copyTimeRangeParams(log.getParams()));
        }
        query.setSubjectId(session.getSubjectId());
        query.setAccountId(session.getAccountId());
        return query;
    }

    private PortalOperLog buildBuyerOwnOperLogQuery(PortalLoginSession session, PortalOperLog log)
    {
        PortalOperLog query = new PortalOperLog();
        if (log != null)
        {
            query.setTitle(log.getTitle());
            query.setOperName(log.getOperName());
            query.setStatus(log.getStatus());
            query.setParams(copyTimeRangeParams(log.getParams()));
        }
        query.setSubjectId(session.getSubjectId());
        query.setAccountId(session.getAccountId());
        return query;
    }

    private HashMap<String, Object> copyTimeRangeParams(Map<String, Object> params)
    {
        HashMap<String, Object> queryParams = new HashMap<>();
        if (params == null)
        {
            return queryParams;
        }
        if (params.containsKey("beginTime"))
        {
            queryParams.put("beginTime", params.get("beginTime"));
        }
        if (params.containsKey("endTime"))
        {
            queryParams.put("endTime", params.get("endTime"));
        }
        return queryParams;
    }

    @Override
    public List<PortalDirectLoginTicket> selectBuyerDirectLoginTicketList(PortalDirectLoginTicket ticket)
    {
        PortalDirectLoginTicket query = ticket == null ? new PortalDirectLoginTicket() : ticket;
        query.setTerminal("buyer");
        return ticketMapper.selectPortalDirectLoginTicketList(query);
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

    @Override
    @Transactional
    public int logoutBuyer(PortalLoginSession session)
    {
        if (session == null)
        {
            throw new ServiceException("登录状态已失效");
        }
        int rows = buyerMapper.logoutBuyerSession(session.getSubjectId(), session.getAccountId(), session.getTokenId());
        buyerMapper.insertBuyerLoginLog(portalTokenSupport.buildLoginLog(
            session.getSubjectId(), session.getAccountId(), session.getUserName(), Constants.SUCCESS, "退出成功"));
        portalTokenSupport.deleteLoginToken(session);
        return rows;
    }

    @Override
    @Transactional
    public int updateBuyerOwnPassword(PortalLoginSession session, PortalPasswordChangeRequest request)
    {
        if (session == null)
        {
            throw new ServiceException("登录状态已失效");
        }
        String oldPassword = request == null ? null : request.getOldPassword();
        String newPassword = PartnerSupport.normalizePasswordChange(oldPassword,
            request == null ? null : request.getNewPassword(),
            request == null ? null : request.getConfirmPassword());

        Buyer buyer = buyerMapper.selectBuyerById(session.getSubjectId());
        BuyerAccount account = buyerMapper.selectBuyerAccountById(session.getAccountId());
        if (account == null || !session.getSubjectId().equals(account.getBuyerId()))
        {
            throw new ServiceException("买家账号不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(buyer.getStatus()))
        {
            throw new ServiceException("买家已停用");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("买家账号已停用");
        }
        if (StringUtils.isBlank(account.getPassword()) || !SecurityUtils.matchesPassword(oldPassword, account.getPassword()))
        {
            throw new ServiceException("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, account.getPassword()))
        {
            throw new ServiceException("新密码不能与旧密码相同");
        }
        return buyerMapper.resetBuyerAccountPassword(account.getBuyerAccountId(),
            SecurityUtils.encryptPassword(newPassword), session.getUserName());
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
