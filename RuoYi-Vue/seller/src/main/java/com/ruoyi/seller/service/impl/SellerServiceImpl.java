package com.ruoyi.seller.service.impl;

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
import com.ruoyi.system.domain.PortalPasswordChangeRequest;
import com.ruoyi.system.domain.PortalSessionProfile;
import com.ruoyi.system.mapper.PortalDirectLoginTicketMapper;
import com.ruoyi.system.service.ISysConfigService;
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

    private static final String LOGIN_BLACK_IP_CONFIG_KEY = "sys.login.blackIPList";

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

    @Autowired
    private ISysConfigService configService;

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
    public SellerAccount selectSellerAccountById(Long sellerId, Long sellerAccountId)
    {
        selectSellerById(sellerId);
        SellerAccount account = sellerMapper.selectSellerAccountById(sellerAccountId);
        if (account == null || !Objects.equals(account.getSellerId(), sellerId))
        {
            throw new ServiceException("卖家账号不存在");
        }
        return account;
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
        assertSingleSellerOwner(sellerId, account);
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
        account.setAccountRole(current.getAccountRole());
        account.setLockStatus(current.getLockStatus());
        account.setLockReason(current.getLockReason());
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
    @Transactional
    public int lockSellerAccount(Long sellerId, Long sellerAccountId, String lockReason)
    {
        SellerAccount account = selectSellerAccountById(sellerId, sellerAccountId);
        String reason = StringUtils.trimToEmpty(lockReason);
        if (StringUtils.isBlank(reason))
        {
            throw new ServiceException("锁定原因不能为空");
        }
        if (reason.length() > 500)
        {
            throw new ServiceException("锁定原因不能超过500个字符");
        }
        int rows = sellerMapper.updateSellerAccountLockStatus(sellerId, account.getSellerAccountId(),
            PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED, reason, SecurityUtils.getUsername());
        forceLogoutSellerSessionScope(sellerId, account.getSellerAccountId());
        return rows;
    }

    @Override
    @Transactional
    public int unlockSellerAccount(Long sellerId, Long sellerAccountId)
    {
        SellerAccount account = selectSellerAccountById(sellerId, sellerAccountId);
        return sellerMapper.updateSellerAccountLockStatus(sellerId, account.getSellerAccountId(),
            PartnerSupport.ACCOUNT_LOCK_STATUS_UNLOCKED, "", SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int resetSellerAccountPassword(Long sellerId, Long sellerAccountId, String password)
    {
        if (StringUtils.isBlank(password))
        {
            throw new ServiceException("新密码不能为空");
        }
        SellerAccount current = selectSellerAccountById(sellerId, sellerAccountId);
        int rows = sellerMapper.resetSellerAccountPassword(current.getSellerId(), current.getSellerAccountId(),
            SecurityUtils.encryptPassword(password), SecurityUtils.getUsername());
        forceLogoutSellerAccountSessionsAfterPasswordReset(rows, current.getSellerId(), current.getSellerAccountId());
        return rows;
    }

    @Override
    @Transactional
    public int resetSellerAccountDefaultPassword(Long sellerId, Long sellerAccountId)
    {
        SellerAccount current = selectSellerAccountById(sellerId, sellerAccountId);
        int rows = sellerMapper.resetSellerAccountPassword(current.getSellerId(), current.getSellerAccountId(),
            SecurityUtils.encryptPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD), SecurityUtils.getUsername());
        forceLogoutSellerAccountSessionsAfterPasswordReset(rows, current.getSellerId(), current.getSellerAccountId());
        return rows;
    }

    @Override
    @Transactional
    public int resetSellerOwnerPassword(Long sellerId)
    {
        selectSellerById(sellerId);
        SellerAccount owner = sellerMapper.selectOwnerSellerAccountBySellerId(sellerId);
        if (owner == null || owner.getSellerAccountId() == null)
        {
            throw new ServiceException("卖家主账号不存在");
        }
        int rows = sellerMapper.resetSellerAccountPassword(sellerId, owner.getSellerAccountId(),
            SecurityUtils.encryptPassword(PartnerSupport.DEFAULT_OWNER_PASSWORD), SecurityUtils.getUsername());
        forceLogoutSellerAccountSessionsAfterPasswordReset(rows, sellerId, owner.getSellerAccountId());
        return rows;
    }

    @Override
    public List<PortalSessionProfile> selectSellerSessionList(Long sellerId)
    {
        selectSellerById(sellerId);
        return sellerMapper.selectSellerSessionProfileList(sellerId, null);
    }

    @Override
    public List<PortalSessionProfile> selectSellerAccountSessionList(Long sellerId, Long sellerAccountId)
    {
        selectSellerAccountById(sellerId, sellerAccountId);
        return sellerMapper.selectSellerSessionProfileList(sellerId, sellerAccountId);
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
        assertSellerAccountCanDirectLogin(owner);
        return directLoginSupport.createToken("seller", sellerId, seller.getSellerNo(), owner, reason,
            PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, null);
    }

    @Override
    public PortalDirectLoginResult createSellerAccountDirectLogin(Long sellerId, Long sellerAccountId, String reason)
    {
        Seller seller = selectSellerById(sellerId);
        SellerAccount account = sellerMapper.selectSellerAccountById(sellerAccountId);
        if (account == null || !sellerId.equals(account.getSellerId()))
        {
            throw new ServiceException("卖家账号不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(seller.getStatus()))
        {
            throw new ServiceException("卖家已停用，不能免密登录");
        }
        assertSellerAccountCanDirectLogin(account);
        return directLoginSupport.createToken("seller", sellerId, seller.getSellerNo(), account, reason,
            PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, null);
    }

    @Override
    public List<PortalLoginLog> selectSellerLoginLogList(PortalLoginLog log)
    {
        normalizeSellerAdminLoginLogScope(log);
        return sellerMapper.selectSellerLoginLogList(log);
    }

    @Override
    public List<PortalOperLog> selectSellerOperLogList(PortalOperLog log)
    {
        normalizeSellerAdminOperLogScope(log);
        return sellerMapper.selectSellerOperLogList(log);
    }

    @Override
    public List<PortalLoginLog> selectSellerOwnLoginLogList(PortalLoginSession session, PortalLoginLog log)
    {
        assertSellerSessionAccount(session);
        return sellerMapper.selectSellerLoginLogList(buildSellerOwnLoginLogQuery(session, log));
    }

    @Override
    public List<PortalOperLog> selectSellerOwnOperLogList(PortalLoginSession session, PortalOperLog log)
    {
        assertSellerSessionAccount(session);
        return sellerMapper.selectSellerOperLogList(buildSellerOwnOperLogQuery(session, log));
    }

    @Override
    public List<PortalSessionProfile> selectSellerOwnSessionList(PortalLoginSession session)
    {
        assertSellerSessionAccount(session);
        List<PortalSessionProfile> sessions = sellerMapper.selectSellerSessionProfileList(
            session.getSubjectId(), session.getAccountId());
        for (PortalSessionProfile profile : sessions)
        {
            profile.setCurrent(Objects.equals(session.getTokenId(), profile.getTokenId()));
        }
        return sessions;
    }

    private void normalizeSellerAdminLoginLogScope(PortalLoginLog log)
    {
        if (log == null)
        {
            return;
        }
        log.setSubjectId(resolveSellerAdminLogSubjectId(log.getSubjectId(), log.getAccountId()));
    }

    private void normalizeSellerAdminOperLogScope(PortalOperLog log)
    {
        if (log == null)
        {
            return;
        }
        log.setSubjectId(resolveSellerAdminLogSubjectId(log.getSubjectId(), log.getAccountId()));
    }

    private Long resolveSellerAdminLogSubjectId(Long sellerId, Long sellerAccountId)
    {
        if (sellerAccountId != null)
        {
            SellerAccount account = sellerMapper.selectSellerAccountById(sellerAccountId);
            if (account == null)
            {
                throw new ServiceException("Seller account does not exist");
            }
            if (sellerId != null && !sellerId.equals(account.getSellerId()))
            {
                throw new ServiceException("Seller log account does not belong to this seller");
            }
            return account.getSellerId();
        }
        if (sellerId != null)
        {
            selectSellerById(sellerId);
        }
        return sellerId;
    }

    private void assertSellerSessionAccount(PortalLoginSession session)
    {
        if (session == null || !"seller".equals(session.getTerminal()) || session.getSubjectId() == null
                || session.getAccountId() == null || StringUtils.isBlank(session.getTokenId()))
        {
            throw new ServiceException("登录状态已失效");
        }
        selectSellerAccountById(session.getSubjectId(), session.getAccountId());
    }

    private PortalLoginLog buildSellerOwnLoginLogQuery(PortalLoginSession session, PortalLoginLog log)
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

    private PortalOperLog buildSellerOwnOperLogQuery(PortalLoginSession session, PortalOperLog log)
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
    public List<PortalDirectLoginTicket> selectSellerDirectLoginTicketList(PortalDirectLoginTicket ticket)
    {
        PortalDirectLoginTicket query = ticket == null ? new PortalDirectLoginTicket() : ticket;
        query.setTerminal("seller");
        return ticketMapper.selectPortalDirectLoginTicketList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
    public PortalLoginResult loginSeller(LoginBody loginBody)
    {
        String username = loginBody == null ? null : StringUtils.trimToEmpty(loginBody.getUsername());
        String password = loginBody == null ? null : loginBody.getPassword();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password))
        {
            recordSellerLoginFailure(null, null, username, "账号或密码不能为空");
            throw new ServiceException("账号或密码不能为空");
        }
        assertSellerLoginPreCheck(username, password);

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
        try
        {
            recordSellerLoginSuccess(account, issue, "登录成功");
        }
        catch (RuntimeException e)
        {
            portalTokenSupport.deleteLoginToken(issue.getSession());
            throw e;
        }
        return issue.getResult();
    }

    private void assertSellerLoginPreCheck(String username, String password)
    {
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH
                || username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            recordSellerLoginFailure(null, null, username, "账号或密码格式不正确");
            throw new ServiceException("账号或密码错误");
        }
        String blackList = configService == null ? null : configService.selectConfigByKey(LOGIN_BLACK_IP_CONFIG_KEY);
        String clientIp = resolveClientIp();
        if (clientIp != null && IpUtils.isMatchedIp(blackList, clientIp))
        {
            recordSellerLoginFailure(null, null, username, "登录IP已被列入黑名单");
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
    public PortalLoginResult directLoginSeller(String directLoginToken)
    {
        PortalDirectLoginToken token = consumeSellerDirectLoginToken(directLoginToken);
        Seller seller = sellerMapper.selectSellerById(token.getPartnerId());
        SellerAccount account = sellerMapper.selectSellerAccountById(token.getAccountId());
        if (account == null || !token.getPartnerId().equals(account.getSellerId()))
        {
            recordSellerDirectLoginFailure(token, token.getPartnerId(), null, token.getUsername(), "免密登录目标账号不存在");
            throw new ServiceException("免密登录目标账号不存在");
        }

        assertSellerCanLoginForDirectLogin(seller, account, token);
        PortalLoginIssue issue = portalTokenSupport.createLogin("seller", seller.getSellerId(), seller.getSellerNo(),
            account, token);
        try
        {
            recordSellerLoginSuccess(account, issue, buildDirectLoginSuccessMessage(token));
        }
        catch (RuntimeException e)
        {
            portalTokenSupport.deleteLoginToken(issue.getSession());
            throw e;
        }
        return issue.getResult();
    }

    private PortalDirectLoginToken consumeSellerDirectLoginToken(String directLoginToken)
    {
        try
        {
            return directLoginSupport.consumeToken("seller", directLoginToken,
                this::assertSellerDirectLoginTokenCanLogin, this::recordSellerDirectLoginTokenFailure);
        }
        catch (ServiceException e)
        {
            if (isDirectLoginTokenFailureWithoutAccountContext(e))
            {
                recordSellerLoginFailure(null, null, null, e.getMessage());
            }
            throw e;
        }
    }

    private void assertSellerDirectLoginTokenCanLogin(PortalDirectLoginToken token)
    {
        Seller seller = sellerMapper.selectSellerById(token.getPartnerId());
        SellerAccount account = selectSellerDirectLoginAccount(token);
        if (account == null)
        {
            throw new ServiceException("免密登录目标账号不存在");
        }
        validateSellerCanLogin(seller, account, token.getUsername());
    }

    private SellerAccount selectSellerDirectLoginAccount(PortalDirectLoginToken token)
    {
        SellerAccount account = sellerMapper.selectSellerAccountById(token.getAccountId());
        return account != null && token.getPartnerId().equals(account.getSellerId()) ? account : null;
    }

    private void recordSellerDirectLoginTokenFailure(PortalDirectLoginToken token, ServiceException e)
    {
        SellerAccount account = selectSellerDirectLoginAccount(token);
        recordSellerDirectLoginFailure(token, token.getPartnerId(), account, token.getUsername(), e.getMessage());
    }

    private boolean isDirectLoginTokenFailureWithoutAccountContext(ServiceException e)
    {
        String message = e.getMessage();
        return message != null && message.startsWith("免密登录")
            && !StringUtils.equals(message, "免密登录目标账号不存在");
    }

    @Override
    @Transactional
    public int logoutSeller(PortalLoginSession session)
    {
        assertSellerSessionAccount(session);
        int rows = sellerMapper.logoutSellerSession(session.getSubjectId(), session.getAccountId(), session.getTokenId());
        sellerMapper.insertSellerLoginLog(portalTokenSupport.buildLoginLog(
            session.getSubjectId(), session.getAccountId(), session.getUserName(), Constants.SUCCESS, "退出成功"));
        portalTokenSupport.deleteLoginToken(session);
        return rows;
    }

    @Override
    @Transactional
    public int updateSellerOwnPassword(PortalLoginSession session, PortalPasswordChangeRequest request)
    {
        assertSellerSessionAccount(session);
        String oldPassword = request == null ? null : request.getOldPassword();
        String newPassword = PartnerSupport.normalizePasswordChange(oldPassword,
            request == null ? null : request.getNewPassword(),
            request == null ? null : request.getConfirmPassword());

        Seller seller = sellerMapper.selectSellerById(session.getSubjectId());
        SellerAccount account = sellerMapper.selectSellerAccountById(session.getAccountId());
        if (account == null || !session.getSubjectId().equals(account.getSellerId()))
        {
            throw new ServiceException("卖家账号不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(seller.getStatus()))
        {
            throw new ServiceException("卖家已停用");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("卖家账号已停用");
        }
        if (PartnerSupport.isAccountLocked(account.getLockStatus()))
        {
            throw new ServiceException("卖家账号已锁定");
        }
        if (StringUtils.isBlank(account.getPassword()) || !SecurityUtils.matchesPassword(oldPassword, account.getPassword()))
        {
            throw new ServiceException("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, account.getPassword()))
        {
            throw new ServiceException("新密码不能与旧密码相同");
        }
        int rows = sellerMapper.resetSellerAccountPassword(account.getSellerId(), account.getSellerAccountId(),
            SecurityUtils.encryptPassword(newPassword), session.getUserName());
        forceLogoutSellerAccountSessionsAfterPasswordReset(rows, account.getSellerId(), account.getSellerAccountId());
        return rows;
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
        account.setAccountRole(PartnerSupport.normalizeAccountRole(account.getAccountRole()));
        account.setStatus(StringUtils.defaultIfBlank(account.getStatus(), PartnerSupport.STATUS_NORMAL));
        PartnerSupport.assertStatus(account.getStatus());
        account.setLockStatus(PartnerSupport.normalizeAccountLockStatus(account.getLockStatus()));
        account.setLockReason(StringUtils.trimToEmpty(account.getLockReason()));
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

    private void assertSingleSellerOwner(Long sellerId, SellerAccount account)
    {
        if (!PartnerSupport.ACCOUNT_ROLE_OWNER.equals(account.getAccountRole()))
        {
            return;
        }
        SellerAccount owner = sellerMapper.selectOwnerSellerAccountBySellerId(sellerId);
        if (owner != null && owner.getSellerAccountId() != null)
        {
            throw new ServiceException("卖家主账号已存在");
        }
    }

    private int forceLogoutSellerSessionScope(Long sellerId, Long sellerAccountId)
    {
        return forceLogoutSellerSessionScope(sellerId, sellerAccountId, "FORCE_LOGOUT");
    }

    private int forceLogoutSellerSessionScope(Long sellerId, Long sellerAccountId, String reason)
    {
        List<String> tokenIds = sellerMapper.selectOnlineSellerSessionTokenIds(sellerId, sellerAccountId);
        int rows = sellerMapper.forceLogoutSellerSessions(sellerId, sellerAccountId);
        if (rows > 0)
        {
            recordSellerForceLogoutAudit(sellerId, sellerAccountId, reason);
        }
        portalTokenSupport.deleteLoginTokens("seller", tokenIds);
        return rows;
    }

    private void forceLogoutSellerAccountSessionsAfterPasswordReset(int resetRows, Long sellerId, Long sellerAccountId)
    {
        if (resetRows > 0)
        {
            forceLogoutSellerSessionScope(sellerId, sellerAccountId, "PASSWORD_RESET_FORCE_LOGOUT");
        }
    }

    private void recordSellerForceLogoutAudit(Long sellerId, Long sellerAccountId, String reason)
    {
        SellerAccount account = sellerAccountId == null ? null : sellerMapper.selectSellerAccountById(sellerAccountId);
        String userName = account == null ? null : account.getUserName();
        sellerMapper.insertSellerLoginLog(portalTokenSupport.buildLoginLog(
            sellerId, sellerAccountId, userName, Constants.SUCCESS, reason));
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
        try
        {
            validateSellerCanLogin(seller, account, username);
        }
        catch (ServiceException e)
        {
            recordSellerLoginFailure(seller == null ? (account == null ? null : account.getSellerId()) : seller.getSellerId(),
                account, username, e.getMessage());
            throw e;
        }
    }

    private void assertSellerCanLoginForDirectLogin(Seller seller, SellerAccount account, PortalDirectLoginToken token)
    {
        try
        {
            validateSellerCanLogin(seller, account, token.getUsername());
        }
        catch (ServiceException e)
        {
            recordSellerDirectLoginFailure(token,
                seller == null ? (account == null ? token.getPartnerId() : account.getSellerId()) : seller.getSellerId(),
                account, token.getUsername(), e.getMessage());
            throw e;
        }
    }

    private void validateSellerCanLogin(Seller seller, SellerAccount account, String username)
    {
        if (seller == null)
        {
            throw new ServiceException("卖家主体不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(seller.getStatus()))
        {
            throw new ServiceException("卖家已停用");
        }
        if (account == null || !PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("卖家账号已停用");
        }
        if (PartnerSupport.isAccountLocked(account.getLockStatus()))
        {
            throw new ServiceException("卖家账号已锁定");
        }
    }

    private void assertSellerAccountCanDirectLogin(SellerAccount account)
    {
        if (account == null)
        {
            throw new ServiceException("卖家主账号不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("卖家账号已停用，不能免密登录");
        }
        if (PartnerSupport.isAccountLocked(account.getLockStatus()))
        {
            throw new ServiceException("卖家账号已锁定，不能免密登录");
        }
    }

    private void recordSellerLoginSuccess(SellerAccount account, PortalLoginIssue issue, String message)
    {
        PortalLoginSession session = issue.getSession();
        sellerMapper.updateSellerAccountLoginInfo(account.getSellerId(), account.getSellerAccountId(),
            session.getLoginIp(), session.getLoginTime());
        PortalLoginLog log = Boolean.TRUE.equals(session.getDirectLogin())
            ? portalTokenSupport.buildDirectLoginLog(account.getSellerId(), account.getSellerAccountId(),
                account.getUserName(), Constants.SUCCESS, message, session)
            : portalTokenSupport.buildLoginLog(account.getSellerId(), account.getSellerAccountId(),
                account.getUserName(), Constants.SUCCESS, message);
        sellerMapper.insertSellerLoginLog(log);
        sellerMapper.insertSellerSession(session);
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

    private void recordSellerLoginFailure(Long sellerId, SellerAccount account, String username, String message)
    {
        Long accountId = account == null ? null : account.getSellerAccountId();
        Long subjectId = sellerId != null ? sellerId : (account == null ? null : account.getSellerId());
        PortalLoginLog log = portalTokenSupport.buildLoginLog(subjectId, accountId, username, Constants.FAIL, message);
        sellerMapper.insertSellerLoginLog(log);
    }

    private void recordSellerDirectLoginFailure(PortalDirectLoginToken token, Long sellerId, SellerAccount account,
            String username, String message)
    {
        Long accountId = account == null ? null : account.getSellerAccountId();
        PortalLoginLog log = portalTokenSupport.buildDirectLoginLog(sellerId, accountId, username, Constants.FAIL,
            message, token);
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
