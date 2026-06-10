package com.ruoyi.buyer.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.github.pagehelper.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.buyer.mapper.BuyerPortalPermissionMapper;
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
import com.ruoyi.system.domain.PortalOwnLoginLogProfile;
import com.ruoyi.system.domain.PortalOwnOperLogProfile;
import com.ruoyi.system.domain.PortalOwnSessionProfile;
import com.ruoyi.system.domain.PortalPasswordChangeRequest;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalSessionProfile;
import com.ruoyi.system.mapper.PortalDirectLoginTicketMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysDictTypeService;
import com.ruoyi.system.service.support.PortalActorSupport;
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

    private static final String OWNER_ROLE_KEY = "owner";

    private static final String[] DEFAULT_OWNER_PERMS = {
        "buyer:portal:home",
        "buyer:account:list",
        "buyer:account:add",
        "buyer:account:edit",
        "buyer:account:role:query",
        "buyer:account:role:edit",
        "buyer:account:loginLog:list",
        "buyer:account:operLog:list",
        "buyer:account:session:list",
        "buyer:dept:list",
        "buyer:dept:query",
        "buyer:dept:add",
        "buyer:dept:edit",
        "buyer:dept:remove",
        "buyer:role:list",
        "buyer:role:query",
        "buyer:role:add",
        "buyer:role:edit",
        "buyer:role:remove"
    };

    private static final String LOGIN_BLACK_IP_CONFIG_KEY = "sys.login.blackIPList";

    @Autowired
    private BuyerMapper buyerMapper;

    @Autowired
    private BuyerPortalPermissionMapper permissionMapper;

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

    @Autowired
    private ISysDictTypeService dictTypeService;

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
        buyer.setCreateBy(PortalActorSupport.currentActorName());
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
        buyer.setUpdateBy(PortalActorSupport.currentActorName());
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
        int rows = buyerMapper.updateBuyerStatus(buyer.getBuyerId(), buyer.getStatus(), PortalActorSupport.currentActorName());
        BuyerAccount owner = buyerMapper.selectOwnerBuyerAccountByBuyerId(buyer.getBuyerId());
        if (owner != null)
        {
            owner.setStatus(buyer.getStatus());
            owner.setUpdateBy(PortalActorSupport.currentActorName());
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
        BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(buyerId, buyerAccountId);
        if (account == null)
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
        account.setCreateBy(PortalActorSupport.currentActorName());
        validateBuyerAccountDept(buyerId, account.getDeptId());
        assertSingleBuyerOwner(buyerId, account);
        if (buyerMapper.selectBuyerAccountByUserName(account.getUserName()) != null)
        {
            throw new ServiceException("登录账号已存在");
        }
        account.setPassword(SecurityUtils.encryptPassword(account.getPassword()));
        int rows = buyerMapper.insertBuyerAccount(account);
        bindOwnerRoleIfNeeded(buyerId, account);
        return rows;
    }

    @Override
    @Transactional
    public int updateBuyerAccount(Long buyerId, BuyerAccount account)
    {
        selectBuyerById(buyerId);
        BuyerAccount current = selectBuyerAccountByPayload(buyerId, account);
        if (current == null)
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
        account.setUpdateBy(PortalActorSupport.currentActorName());
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
            PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED, reason, PortalActorSupport.currentActorName());
        forceLogoutBuyerSessionScope(buyerId, account.getBuyerAccountId());
        return rows;
    }

    @Override
    @Transactional
    public int unlockBuyerAccount(Long buyerId, Long buyerAccountId)
    {
        BuyerAccount account = selectBuyerAccountById(buyerId, buyerAccountId);
        return buyerMapper.updateBuyerAccountLockStatus(buyerId, account.getBuyerAccountId(),
            PartnerSupport.ACCOUNT_LOCK_STATUS_UNLOCKED, "", PortalActorSupport.currentActorName());
    }

    @Override
    @Transactional
    public int resetBuyerAccountPassword(Long buyerId, Long buyerAccountId, String password)
    {
        String normalizedPassword = PartnerSupport.normalizeTemporaryPassword(password);
        BuyerAccount current = selectBuyerAccountById(buyerId, buyerAccountId);
        int rows = buyerMapper.resetBuyerAccountPassword(current.getBuyerId(), current.getBuyerAccountId(),
            SecurityUtils.encryptPassword(normalizedPassword), PortalActorSupport.currentActorName());
        forceLogoutBuyerAccountSessionsAfterPasswordReset(rows, current.getBuyerId(), current.getBuyerAccountId());
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
        BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(buyerId, buyerAccountId);
        if (account == null)
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
        BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(buyerId, buyerAccountId);
        if (account == null)
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
    public List<PortalOwnLoginLogProfile> selectBuyerOwnLoginLogList(PortalLoginSession session, PortalLoginLog log)
    {
        assertBuyerSessionAccount(session);
        List<PortalLoginLog> logs = buyerMapper.selectBuyerLoginLogList(buildBuyerOwnLoginLogQuery(session, log));
        List<PortalOwnLoginLogProfile> profiles = newOwnLoginLogProfileList(logs);
        for (PortalLoginLog item : logs)
        {
            profiles.add(buildOwnLoginLogProfile(item));
        }
        return profiles;
    }

    @Override
    public List<PortalOwnOperLogProfile> selectBuyerOwnOperLogList(PortalLoginSession session, PortalOperLog log)
    {
        assertBuyerSessionAccount(session);
        List<PortalOperLog> logs = buyerMapper.selectBuyerOperLogList(buildBuyerOwnOperLogQuery(session, log));
        List<PortalOwnOperLogProfile> profiles = newOwnOperLogProfileList(logs);
        for (PortalOperLog item : logs)
        {
            profiles.add(buildOwnOperLogProfile(item));
        }
        return profiles;
    }

    @Override
    public List<PortalOwnSessionProfile> selectBuyerOwnSessionList(PortalLoginSession session)
    {
        assertBuyerSessionAccount(session);
        List<PortalSessionProfile> sessions = buyerMapper.selectBuyerSessionProfileList(
            session.getSubjectId(), session.getAccountId());
        List<PortalOwnSessionProfile> profiles = newOwnSessionProfileList(sessions);
        for (PortalSessionProfile profile : sessions)
        {
            profile.setCurrent(Objects.equals(session.getTokenId(), profile.getTokenId()));
            profiles.add(buildOwnSessionProfile(profile));
        }
        return profiles;
    }

    private List<PortalOwnLoginLogProfile> newOwnLoginLogProfileList(List<PortalLoginLog> logs)
    {
        if (logs instanceof Page)
        {
            Page<?> source = (Page<?>) logs;
            Page<PortalOwnLoginLogProfile> result = new Page<>(source.getPageNum(), source.getPageSize(),
                source.isCount());
            copyPageMetadata(source, result);
            return result;
        }
        return new ArrayList<>();
    }

    private PortalOwnLoginLogProfile buildOwnLoginLogProfile(PortalLoginLog source)
    {
        PortalOwnLoginLogProfile profile = new PortalOwnLoginLogProfile();
        profile.setUserName(source.getUserName());
        profile.setIpaddr(source.getIpaddr());
        profile.setLoginLocation(source.getLoginLocation());
        profile.setBrowser(source.getBrowser());
        profile.setOs(source.getOs());
        profile.setStatus(source.getStatus());
        profile.setMsg(buildOwnLoginLogMessage(source));
        profile.setLoginTime(source.getLoginTime());
        return profile;
    }

    private String buildOwnLoginLogMessage(PortalLoginLog source)
    {
        if (!Boolean.TRUE.equals(source.getDirectLogin()))
        {
            return source.getMsg();
        }
        return Constants.FAIL.equals(source.getStatus()) ? "免密登录失败" : "免密登录成功";
    }

    private List<PortalOwnOperLogProfile> newOwnOperLogProfileList(List<PortalOperLog> logs)
    {
        if (logs instanceof Page)
        {
            Page<?> source = (Page<?>) logs;
            Page<PortalOwnOperLogProfile> result = new Page<>(source.getPageNum(), source.getPageSize(),
                source.isCount());
            copyPageMetadata(source, result);
            return result;
        }
        return new ArrayList<>();
    }

    private PortalOwnOperLogProfile buildOwnOperLogProfile(PortalOperLog source)
    {
        PortalOwnOperLogProfile profile = new PortalOwnOperLogProfile();
        profile.setTitle(source.getTitle());
        profile.setBusinessType(source.getBusinessType());
        profile.setRequestMethod(source.getRequestMethod());
        profile.setOperName(source.getOperName());
        profile.setOperUrl(source.getOperUrl());
        profile.setOperIp(source.getOperIp());
        profile.setOperLocation(source.getOperLocation());
        profile.setStatus(source.getStatus());
        profile.setErrorMsg(source.getErrorMsg());
        profile.setOperTime(source.getOperTime());
        profile.setCostTime(source.getCostTime());
        return profile;
    }

    private List<PortalOwnSessionProfile> newOwnSessionProfileList(List<PortalSessionProfile> sessions)
    {
        if (sessions instanceof Page)
        {
            Page<?> source = (Page<?>) sessions;
            Page<PortalOwnSessionProfile> result = new Page<>(source.getPageNum(), source.getPageSize(),
                source.isCount());
            copyPageMetadata(source, result);
            return result;
        }
        return new ArrayList<>();
    }

    private PortalOwnSessionProfile buildOwnSessionProfile(PortalSessionProfile source)
    {
        PortalOwnSessionProfile profile = new PortalOwnSessionProfile();
        profile.setUserName(source.getUserName());
        profile.setLoginIp(source.getLoginIp());
        profile.setLoginTime(source.getLoginTime());
        profile.setExpireTime(source.getExpireTime());
        profile.setLogoutTime(source.getLogoutTime());
        profile.setStatus(source.getStatus());
        profile.setCurrent(source.getCurrent());
        return profile;
    }

    private void copyPageMetadata(Page<?> source, Page<?> result)
    {
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setStartRow(source.getStartRow());
        result.setEndRow(source.getEndRow());
        result.setReasonable(source.getReasonable());
        result.setPageSizeZero(source.getPageSizeZero());
        result.setOrderBy(source.getOrderBy());
        result.setOrderByOnly(source.isOrderByOnly());
        result.setCount(source.isCount());
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
            if (buyerId == null)
            {
                throw new ServiceException("查询买家账号日志必须指定买家主体");
            }
            BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(buyerId, buyerAccountId);
            if (account == null)
            {
                throw new ServiceException("Buyer account does not exist");
            }
            return buyerId;
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
            throw portalSessionExpired();
        }
        BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(session.getSubjectId(), session.getAccountId());
        if (account == null)
        {
            throw portalSessionExpired();
        }
    }

    private ServiceException portalSessionExpired()
    {
        return new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
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
        normalizeBuyerDirectLoginTicketScope(query);
        query.setTerminal("buyer");
        return ticketMapper.selectPortalDirectLoginTicketList(query);
    }

    private void normalizeBuyerDirectLoginTicketScope(PortalDirectLoginTicket ticket)
    {
        Long buyerId = ticket.getTargetSubjectId();
        Long buyerAccountId = ticket.getTargetAccountId();
        if (buyerAccountId != null)
        {
            if (buyerId == null)
            {
                throw new ServiceException("查询买家免密票据必须指定买家主体");
            }
            BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(buyerId, buyerAccountId);
            if (account == null)
            {
                throw new ServiceException("Buyer account does not exist");
            }
            return;
        }
        if (buyerId != null)
        {
            selectBuyerById(buyerId);
        }
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
        BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(token.getPartnerId(), token.getAccountId());
        if (account == null)
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
        return buyerMapper.selectBuyerAccountByIdAndBuyerId(token.getPartnerId(), token.getAccountId());
    }

    private void recordBuyerDirectLoginTokenFailure(PortalDirectLoginToken token, ServiceException e)
    {
        if (!StringUtils.equals("buyer", token.getPortalType()))
        {
            recordBuyerLoginFailure(null, null, null, e.getMessage());
            return;
        }
        BuyerAccount account = selectBuyerDirectLoginAccount(token);
        recordBuyerDirectLoginFailure(token, token.getPartnerId(), account, token.getUsername(), e.getMessage());
    }

    private boolean isDirectLoginTokenFailureWithoutAccountContext(ServiceException e)
    {
        String message = e.getMessage();
        return StringUtils.equals(message, "免密登录 token 不能为空")
            || StringUtils.equals(message, "免密登录票据不存在")
            || StringUtils.equals(message, "免密登录票据端类型不匹配")
            || StringUtils.equals(message, "免密登录 token 校验器不能为空");
    }

    @Override
    @Transactional
    public int logoutBuyer(PortalLoginSession session)
    {
        assertBuyerSessionAccount(session);
        int rows = buyerMapper.logoutBuyerSession(session.getSubjectId(), session.getAccountId(), session.getTokenId());
        PortalLoginLog log = Boolean.TRUE.equals(session.getDirectLogin())
            ? portalTokenSupport.buildDirectLoginLog(session.getSubjectId(), session.getAccountId(),
                session.getUserName(), Constants.SUCCESS, "退出成功", session)
            : portalTokenSupport.buildLoginLog(session.getSubjectId(), session.getAccountId(),
                session.getUserName(), Constants.SUCCESS, "退出成功");
        buyerMapper.insertBuyerLoginLog(log);
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
        BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(session.getSubjectId(), session.getAccountId());
        if (buyer == null || account == null)
        {
            throw portalSessionExpired();
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
        forceLogoutBuyerAccountSessionsAfterPasswordReset(rows, account.getBuyerId(), account.getBuyerAccountId(),
            false);
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
        PartnerSupport.assertCountryRegionCode(buyer.getCountryCode(),
            dictTypeService.selectDictDataByType(PartnerSupport.COUNTRY_REGION_DICT_TYPE));
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
        owner.setUpdateBy(PortalActorSupport.currentActorName());
        owner.setRemark("买家主账号");
        buyerMapper.updateBuyerAccount(owner);
        bindOwnerRoleIfNeeded(buyer.getBuyerId(), owner);
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

    private void bindOwnerRoleIfNeeded(Long buyerId, BuyerAccount account)
    {
        if (!PartnerSupport.ACCOUNT_ROLE_OWNER.equals(account.getAccountRole()))
        {
            return;
        }
        Long buyerAccountId = account.getBuyerAccountId() != null ? account.getBuyerAccountId() : account.getAccountId();
        if (buyerAccountId == null)
        {
            throw new ServiceException("买家主账号ID不能为空");
        }
        PortalRole ownerRole = ensureOwnerRoleReady(buyerId);
        permissionMapper.insertBuyerOwnerAccountRoleIfMissing(buyerId, buyerAccountId);
        if (!permissionMapper.selectBuyerAccountRoleIds(buyerId, buyerAccountId).contains(ownerRole.getRoleId()))
        {
            throw new ServiceException("买家主账号绑定 owner 角色失败");
        }
    }

    private PortalRole ensureOwnerRoleReady(Long buyerId)
    {
        permissionMapper.insertBuyerOwnerRoleIfMissing(buyerId, PortalActorSupport.currentActorName());
        PortalRole ownerRole = permissionMapper.checkBuyerRoleKeyUnique(buyerId, OWNER_ROLE_KEY);
        if (ownerRole == null || !PartnerSupport.STATUS_NORMAL.equals(ownerRole.getStatus()))
        {
            throw new ServiceException("买家端 owner 角色不存在或已停用");
        }
        permissionMapper.insertBuyerOwnerRoleMenusIfMissing(buyerId, DEFAULT_OWNER_PERMS);
        if (permissionMapper.countBuyerOwnerRoleMenuGrants(buyerId, DEFAULT_OWNER_PERMS) != DEFAULT_OWNER_PERMS.length)
        {
            throw new ServiceException("买家端 owner 默认菜单权限不完整");
        }
        return ownerRole;
    }

    private int forceLogoutBuyerSessionScope(Long buyerId, Long buyerAccountId)
    {
        return forceLogoutBuyerSessionScope(buyerId, buyerAccountId, "FORCE_LOGOUT", true);
    }

    private int forceLogoutBuyerSessionScope(Long buyerId, Long buyerAccountId, String reason)
    {
        return forceLogoutBuyerSessionScope(buyerId, buyerAccountId, reason, true);
    }

    private int forceLogoutBuyerSessionScope(Long buyerId, Long buyerAccountId, String reason,
            boolean auditCurrentAdmin)
    {
        List<PortalLoginSession> sessions = buyerMapper.selectOnlineBuyerSessionList(buyerId, buyerAccountId);
        int rows = buyerMapper.forceLogoutBuyerSessions(buyerId, buyerAccountId);
        if (rows > 0)
        {
            recordBuyerForceLogoutAudit(buyerId, buyerAccountId, reason, sessions, auditCurrentAdmin);
        }
        List<String> tokenIds = sessions == null ? List.of()
            : sessions.stream().map(PortalLoginSession::getTokenId).filter(StringUtils::isNotEmpty).toList();
        portalTokenSupport.deleteLoginTokens("buyer", tokenIds);
        return rows;
    }

    private void forceLogoutBuyerAccountSessionsAfterPasswordReset(int resetRows, Long buyerId, Long buyerAccountId)
    {
        forceLogoutBuyerAccountSessionsAfterPasswordReset(resetRows, buyerId, buyerAccountId, true);
    }

    private void forceLogoutBuyerAccountSessionsAfterPasswordReset(int resetRows, Long buyerId, Long buyerAccountId,
            boolean auditCurrentAdmin)
    {
        if (resetRows > 0)
        {
            forceLogoutBuyerSessionScope(buyerId, buyerAccountId, "PASSWORD_RESET_FORCE_LOGOUT",
                auditCurrentAdmin);
        }
    }

    private void recordBuyerForceLogoutAudit(Long buyerId, Long buyerAccountId, String reason,
            List<PortalLoginSession> sessions, boolean auditCurrentAdmin)
    {
        if (sessions != null && !sessions.isEmpty())
        {
            for (PortalLoginSession session : sessions)
            {
                recordBuyerForceLogoutSessionAudit(session, reason, auditCurrentAdmin);
            }
            return;
        }
        BuyerAccount account = buyerAccountId == null ? null
            : buyerMapper.selectBuyerAccountByIdAndBuyerId(buyerId, buyerAccountId);
        String userName = account == null ? null : account.getUserName();
        PortalLoginLog log = portalTokenSupport.buildLoginLog(
            buyerId, buyerAccountId, userName, Constants.SUCCESS, reason);
        if (auditCurrentAdmin)
        {
            applyCurrentAdminAudit(log);
        }
        buyerMapper.insertBuyerLoginLog(log);
    }

    private void recordBuyerForceLogoutSessionAudit(PortalLoginSession session, String reason,
            boolean auditCurrentAdmin)
    {
        PortalLoginLog log = Boolean.TRUE.equals(session.getDirectLogin())
            ? portalTokenSupport.buildDirectLoginLog(session.getSubjectId(), session.getAccountId(),
                session.getUserName(), Constants.SUCCESS, reason, session)
            : portalTokenSupport.buildLoginLog(session.getSubjectId(), session.getAccountId(),
                session.getUserName(), Constants.SUCCESS, reason);
        if (auditCurrentAdmin)
        {
            applyCurrentAdminAudit(log);
        }
        buyerMapper.insertBuyerLoginLog(log);
    }

    private void applyCurrentAdminAudit(PortalLoginLog log)
    {
        log.setActingAdminId(SecurityUtils.getUserId());
        log.setActingAdminName(SecurityUtils.getUsername());
    }

    private BuyerAccount selectBuyerAccountByPayload(Long buyerId, BuyerAccount account)
    {
        Long accountId = account.getBuyerAccountId() != null ? account.getBuyerAccountId() : account.getAccountId();
        if (accountId == null)
        {
            throw new ServiceException("买家账号ID不能为空");
        }
        return buyerMapper.selectBuyerAccountByIdAndBuyerId(buyerId, accountId);
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
        return "免密登录成功";
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
        Long accountId = account == null
            ? (StringUtils.equals("buyer", token.getPortalType()) ? token.getAccountId() : null)
            : account.getBuyerAccountId();
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
