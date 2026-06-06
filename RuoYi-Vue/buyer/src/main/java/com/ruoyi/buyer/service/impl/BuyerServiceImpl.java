package com.ruoyi.buyer.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
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
import com.ruoyi.system.service.ISysConfigService;
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

    private static final String LOGIN_BLACK_IP_CONFIG_KEY = "sys.login.blackIPList";

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

    @Autowired
    private ISysConfigService configService;

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
        assertSingleBuyerOwner(buyerId, account);
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
        account.setAccountRole(current.getAccountRole());
        account.setLockStatus(current.getLockStatus());
        account.setLockReason(current.getLockReason());
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
    @Transactional
    public int lockBuyerAccount(Long buyerId, Long buyerAccountId, String lockReason)
    {
        BuyerAccount account = selectBuyerAccountById(buyerId, buyerAccountId);
        String reason = StringUtils.trimToEmpty(lockReason);
        if (StringUtils.isBlank(reason))
        {
            throw new ServiceException("锁定原因不能为空");
        }
        if (reason.length() > 500)
        {
            throw new ServiceException("锁定原因不能超过500个字符");
        }
        int rows = buyerMapper.updateBuyerAccountLockStatus(buyerId, account.getBuyerAccountId(),
            PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED, reason, SecurityUtils.getUsername());
        forceLogoutBuyerSessionScope(buyerId, account.getBuyerAccountId());
        return rows;
    }

    @Override
    @Transactional
    public int unlockBuyerAccount(Long buyerId, Long buyerAccountId)
    {
        BuyerAccount account = selectBuyerAccountById(buyerId, buyerAccountId);
        return buyerMapper.updateBuyerAccountLockStatus(buyerId, account.getBuyerAccountId(),
            PartnerSupport.ACCOUNT_LOCK_STATUS_UNLOCKED, "", SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int resetBuyerAccountPassword(Long buyerId, Long buyerAccountId, String password)
    {
        if (StringUtils.isBlank(password))
        {
            throw new ServiceException("新密码不能为空");
        }
        BuyerAccount current = selectBuyerAccountById(buyerId, buyerAccountId);
        int rows = buyerMapper.resetBuyerAccountPassword(current.getBuyerId(), current.getBuyerAccountId(),
            SecurityUtils.encryptPassword(password), SecurityUtils.getUsername());
        forceLogoutBuyerAccountSessionsAfterPasswordReset(rows, current.getBuyerId(), current.getBuyerAccountId());
        return rows;
    }

    @Override
    @Transactional
    public int resetBuyerAccountDefaultPassword(Long buyerId, Long buyerAccountId)
    {
        BuyerAccount current = selectBuyerAccountById(buyerId, buyerAccountId);
        int rows = buyerMapper.resetBuyerAccountPassword(current.getBuyerId(), current.getBuyerAccountId(),
            SecurityUtils.encryptPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD), SecurityUtils.getUsername());
        forceLogoutBuyerAccountSessionsAfterPasswordReset(rows, current.getBuyerId(), current.getBuyerAccountId());
        return rows;
    }

    @Override
    @Transactional
    public int resetBuyerOwnerPassword(Long buyerId)
    {
        selectBuyerById(buyerId);
        BuyerAccount owner = buyerMapper.selectOwnerBuyerAccountByBuyerId(buyerId);
        if (owner == null || owner.getBuyerAccountId() == null)
        {
            throw new ServiceException("买家主账号不存在");
        }
        int rows = buyerMapper.resetBuyerAccountPassword(buyerId, owner.getBuyerAccountId(),
            SecurityUtils.encryptPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD), SecurityUtils.getUsername());
        forceLogoutBuyerAccountSessionsAfterPasswordReset(rows, buyerId, owner.getBuyerAccountId());
        return rows;
    }

    @Override
    public List<PortalSessionProfile> selectBuyerSessionList(Long buyerId)
    {
        selectBuyerById(buyerId);
        return buyerMapper.selectBuyerSessionProfileList(buyerId, null);
    }

    @Override
    public List<PortalSessionProfile> selectBuyerAccountSessionList(Long buyerId, Long buyerAccountId)
    {
        selectBuyerAccountById(buyerId, buyerAccountId);
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
        assertBuyerAccountCanDirectLogin(owner);
        return directLoginSupport.createToken("buyer", buyerId, buyer.getBuyerNo(), owner, reason,
            PortalDirectLoginSupport.BUYER_WEB_URL_CONFIG_KEY, null);
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
        assertBuyerAccountCanDirectLogin(account);
        return directLoginSupport.createToken("buyer", buyerId, buyer.getBuyerNo(), account, reason,
            PortalDirectLoginSupport.BUYER_WEB_URL_CONFIG_KEY, null);
    }

    @Override
    public List<PortalLoginLog> selectBuyerLoginLogList(PortalLoginLog log)
    {
        normalizeBuyerAdminLoginLogScope(log);
        return buyerMapper.selectBuyerLoginLogList(log);
    }

    @Override
    public List<PortalOperLog> selectBuyerOperLogList(PortalOperLog log)
    {
        normalizeBuyerAdminOperLogScope(log);
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

    private void normalizeBuyerAdminLoginLogScope(PortalLoginLog log)
    {
        if (log == null)
        {
            return;
        }
        log.setSubjectId(resolveBuyerAdminLogSubjectId(log.getSubjectId(), log.getAccountId()));
    }

    private void normalizeBuyerAdminOperLogScope(PortalOperLog log)
    {
        if (log == null)
        {
            return;
        }
        log.setSubjectId(resolveBuyerAdminLogSubjectId(log.getSubjectId(), log.getAccountId()));
    }

    private Long resolveBuyerAdminLogSubjectId(Long buyerId, Long buyerAccountId)
    {
        if (buyerAccountId != null)
        {
            BuyerAccount account = buyerMapper.selectBuyerAccountById(buyerAccountId);
            if (account == null)
            {
                throw new ServiceException("Buyer account does not exist");
            }
            if (buyerId != null && !buyerId.equals(account.getBuyerId()))
            {
                throw new ServiceException("Buyer log account does not belong to this buyer");
            }
            return account.getBuyerId();
        }
        if (buyerId != null)
        {
            selectBuyerById(buyerId);
        }
        return buyerId;
    }

    private void assertBuyerSessionAccount(PortalLoginSession session)
    {
        if (session == null || !"buyer".equals(session.getTerminal()) || session.getSubjectId() == null
                || session.getAccountId() == null || StringUtils.isBlank(session.getTokenId()))
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
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
    public PortalLoginResult loginBuyer(LoginBody loginBody)
    {
        String username = loginBody == null ? null : StringUtils.trimToEmpty(loginBody.getUsername());
        String password = loginBody == null ? null : loginBody.getPassword();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password))
        {
            recordBuyerLoginFailure(null, null, username, "账号或密码不能为空");
            throw new ServiceException("账号或密码不能为空");
        }
        assertBuyerLoginPreCheck(username, password);

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
        try
        {
            recordBuyerLoginSuccess(account, issue, "登录成功");
        }
        catch (RuntimeException e)
        {
            portalTokenSupport.deleteLoginToken(issue.getSession());
            throw e;
        }
        return issue.getResult();
    }

    private void assertBuyerLoginPreCheck(String username, String password)
    {
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH
                || username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            recordBuyerLoginFailure(null, null, username, "账号或密码格式不正确");
            throw new ServiceException("账号或密码错误");
        }
        String blackList = configService == null ? null : configService.selectConfigByKey(LOGIN_BLACK_IP_CONFIG_KEY);
        String clientIp = resolveClientIp();
        if (clientIp != null && IpUtils.isMatchedIp(blackList, clientIp))
        {
            recordBuyerLoginFailure(null, null, username, "登录IP已被列入黑名单");
            throw new ServiceException("登录IP已被列入黑名单");
        }
    }

    private String resolveClientIp()
    {
        try
        {
            return IpUtils.getIpAddr();
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
    public PortalLoginResult directLoginBuyer(String directLoginToken)
    {
        PortalDirectLoginToken token = consumeBuyerDirectLoginToken(directLoginToken);
        Buyer buyer = buyerMapper.selectBuyerById(token.getPartnerId());
        BuyerAccount account = buyerMapper.selectBuyerAccountById(token.getAccountId());
        if (account == null || !token.getPartnerId().equals(account.getBuyerId()))
        {
            recordBuyerDirectLoginFailure(token, token.getPartnerId(), null, token.getUsername(), "免密登录目标账号不存在");
            throw new ServiceException("免密登录目标账号不存在");
        }

        assertBuyerCanLoginForDirectLogin(buyer, account, token);
        PortalLoginIssue issue = portalTokenSupport.createLogin("buyer", buyer.getBuyerId(), buyer.getBuyerNo(),
            account, token);
        try
        {
            recordBuyerLoginSuccess(account, issue, buildDirectLoginSuccessMessage(token));
        }
        catch (RuntimeException e)
        {
            portalTokenSupport.deleteLoginToken(issue.getSession());
            throw e;
        }
        return issue.getResult();
    }

    private PortalDirectLoginToken consumeBuyerDirectLoginToken(String directLoginToken)
    {
        try
        {
            return directLoginSupport.consumeToken("buyer", directLoginToken,
                this::assertBuyerDirectLoginTokenCanLogin, this::recordBuyerDirectLoginTokenFailure);
        }
        catch (ServiceException e)
        {
            if (isDirectLoginTokenFailureWithoutAccountContext(e))
            {
                recordBuyerLoginFailure(null, null, null, e.getMessage());
            }
            throw e;
        }
    }

    private void assertBuyerDirectLoginTokenCanLogin(PortalDirectLoginToken token)
    {
        Buyer buyer = buyerMapper.selectBuyerById(token.getPartnerId());
        BuyerAccount account = selectBuyerDirectLoginAccount(token);
        if (account == null)
        {
            throw new ServiceException("免密登录目标账号不存在");
        }
        validateBuyerCanLogin(buyer, account, token.getUsername());
    }

    private BuyerAccount selectBuyerDirectLoginAccount(PortalDirectLoginToken token)
    {
        BuyerAccount account = buyerMapper.selectBuyerAccountById(token.getAccountId());
        return account != null && token.getPartnerId().equals(account.getBuyerId()) ? account : null;
    }

    private void recordBuyerDirectLoginTokenFailure(PortalDirectLoginToken token, ServiceException e)
    {
        BuyerAccount account = selectBuyerDirectLoginAccount(token);
        recordBuyerDirectLoginFailure(token, token.getPartnerId(), account, token.getUsername(), e.getMessage());
    }

    private boolean isDirectLoginTokenFailureWithoutAccountContext(ServiceException e)
    {
        String message = e.getMessage();
        return message != null && message.startsWith("免密登录")
            && !StringUtils.equals(message, "免密登录目标账号不存在");
    }

    @Override
    @Transactional
    public int logoutBuyer(PortalLoginSession session)
    {
        assertBuyerSessionAccount(session);
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
        assertBuyerSessionAccount(session);
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
        if (PartnerSupport.isAccountLocked(account.getLockStatus()))
        {
            throw new ServiceException("买家账号已锁定");
        }
        if (StringUtils.isBlank(account.getPassword()) || !SecurityUtils.matchesPassword(oldPassword, account.getPassword()))
        {
            throw new ServiceException("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, account.getPassword()))
        {
            throw new ServiceException("新密码不能与旧密码相同");
        }
        int rows = buyerMapper.resetBuyerAccountPassword(account.getBuyerId(), account.getBuyerAccountId(),
            SecurityUtils.encryptPassword(newPassword), session.getUserName());
        forceLogoutBuyerAccountSessionsAfterPasswordReset(rows, account.getBuyerId(), account.getBuyerAccountId());
        return rows;
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
        account.setAccountRole(PartnerSupport.normalizeAccountRole(account.getAccountRole()));
        account.setStatus(StringUtils.defaultIfBlank(account.getStatus(), PartnerSupport.STATUS_NORMAL));
        PartnerSupport.assertStatus(account.getStatus());
        account.setLockStatus(PartnerSupport.normalizeAccountLockStatus(account.getLockStatus()));
        account.setLockReason(StringUtils.trimToEmpty(account.getLockReason()));
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

    private void assertSingleBuyerOwner(Long buyerId, BuyerAccount account)
    {
        if (!PartnerSupport.ACCOUNT_ROLE_OWNER.equals(account.getAccountRole()))
        {
            return;
        }
        BuyerAccount owner = buyerMapper.selectOwnerBuyerAccountByBuyerId(buyerId);
        if (owner != null && owner.getBuyerAccountId() != null)
        {
            throw new ServiceException("买家主账号已存在");
        }
    }

    private int forceLogoutBuyerSessionScope(Long buyerId, Long buyerAccountId)
    {
        return forceLogoutBuyerSessionScope(buyerId, buyerAccountId, "FORCE_LOGOUT");
    }

    private int forceLogoutBuyerSessionScope(Long buyerId, Long buyerAccountId, String reason)
    {
        List<String> tokenIds = buyerMapper.selectOnlineBuyerSessionTokenIds(buyerId, buyerAccountId);
        int rows = buyerMapper.forceLogoutBuyerSessions(buyerId, buyerAccountId);
        if (rows > 0)
        {
            recordBuyerForceLogoutAudit(buyerId, buyerAccountId, reason);
        }
        portalTokenSupport.deleteLoginTokens("buyer", tokenIds);
        return rows;
    }

    private void forceLogoutBuyerAccountSessionsAfterPasswordReset(int resetRows, Long buyerId, Long buyerAccountId)
    {
        if (resetRows > 0)
        {
            forceLogoutBuyerSessionScope(buyerId, buyerAccountId, "PASSWORD_RESET_FORCE_LOGOUT");
        }
    }

    private void recordBuyerForceLogoutAudit(Long buyerId, Long buyerAccountId, String reason)
    {
        BuyerAccount account = buyerAccountId == null ? null : buyerMapper.selectBuyerAccountById(buyerAccountId);
        String userName = account == null ? null : account.getUserName();
        buyerMapper.insertBuyerLoginLog(portalTokenSupport.buildLoginLog(
            buyerId, buyerAccountId, userName, Constants.SUCCESS, reason));
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
        try
        {
            validateBuyerCanLogin(buyer, account, username);
        }
        catch (ServiceException e)
        {
            recordBuyerLoginFailure(buyer == null ? (account == null ? null : account.getBuyerId()) : buyer.getBuyerId(),
                account, username, e.getMessage());
            throw e;
        }
    }

    private void assertBuyerCanLoginForDirectLogin(Buyer buyer, BuyerAccount account, PortalDirectLoginToken token)
    {
        try
        {
            validateBuyerCanLogin(buyer, account, token.getUsername());
        }
        catch (ServiceException e)
        {
            recordBuyerDirectLoginFailure(token,
                buyer == null ? (account == null ? token.getPartnerId() : account.getBuyerId()) : buyer.getBuyerId(),
                account, token.getUsername(), e.getMessage());
            throw e;
        }
    }

    private void validateBuyerCanLogin(Buyer buyer, BuyerAccount account, String username)
    {
        if (buyer == null)
        {
            throw new ServiceException("买家主体不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(buyer.getStatus()))
        {
            throw new ServiceException("买家已停用");
        }
        if (account == null || !PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("买家账号已停用");
        }
        if (PartnerSupport.isAccountLocked(account.getLockStatus()))
        {
            throw new ServiceException("买家账号已锁定");
        }
    }

    private void assertBuyerAccountCanDirectLogin(BuyerAccount account)
    {
        if (account == null)
        {
            throw new ServiceException("买家主账号不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("买家账号已停用，不能免密登录");
        }
        if (PartnerSupport.isAccountLocked(account.getLockStatus()))
        {
            throw new ServiceException("买家账号已锁定，不能免密登录");
        }
    }

    private void recordBuyerLoginSuccess(BuyerAccount account, PortalLoginIssue issue, String message)
    {
        PortalLoginSession session = issue.getSession();
        buyerMapper.updateBuyerAccountLoginInfo(account.getBuyerId(), account.getBuyerAccountId(),
            session.getLoginIp(), session.getLoginTime());
        PortalLoginLog log = Boolean.TRUE.equals(session.getDirectLogin())
            ? portalTokenSupport.buildDirectLoginLog(account.getBuyerId(), account.getBuyerAccountId(),
                account.getUserName(), Constants.SUCCESS, message, session)
            : portalTokenSupport.buildLoginLog(account.getBuyerId(), account.getBuyerAccountId(),
                account.getUserName(), Constants.SUCCESS, message);
        buyerMapper.insertBuyerLoginLog(log);
        buyerMapper.insertBuyerSession(session);
    }

    private String buildDirectLoginSuccessMessage(PortalDirectLoginToken token)
    {
        return "免密登录成功; ticketId=" + token.getTicketId()
            + "; actingAdminId=" + token.getActingAdminId()
            + "; actingAdminName=" + safeAuditValue(token.getActingAdminName())
            + "; reason=" + safeAuditValue(token.getDirectLoginReason());
    }

    private String safeAuditValue(String value)
    {
        return StringUtils.isBlank(value) ? "" : value;
    }

    private void recordBuyerLoginFailure(Long buyerId, BuyerAccount account, String username, String message)
    {
        Long accountId = account == null ? null : account.getBuyerAccountId();
        Long subjectId = buyerId != null ? buyerId : (account == null ? null : account.getBuyerId());
        PortalLoginLog log = portalTokenSupport.buildLoginLog(subjectId, accountId, username, Constants.FAIL, message);
        buyerMapper.insertBuyerLoginLog(log);
    }

    private void recordBuyerDirectLoginFailure(PortalDirectLoginToken token, Long buyerId, BuyerAccount account,
            String username, String message)
    {
        Long accountId = account == null ? null : account.getBuyerAccountId();
        PortalLoginLog log = portalTokenSupport.buildDirectLoginLog(buyerId, accountId, username, Constants.FAIL,
            message, token);
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
