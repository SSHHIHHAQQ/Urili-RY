package com.ruoyi.seller.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.github.pagehelper.Page;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.seller.mapper.SellerPortalDeptMapper;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalAccount;
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
import com.ruoyi.system.domain.PortalSessionProfile;
import com.ruoyi.system.mapper.PortalDirectLoginTicketMapper;
import com.ruoyi.system.service.ISysDictTypeService;
import com.ruoyi.system.service.support.PartnerSupport;
import com.ruoyi.system.service.support.PortalDirectLoginSupport;
import com.ruoyi.system.service.support.PortalTokenSupport;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class SellerServiceImplTest
{
    @Test
    public void insertSellerRejectsCountryCodeOutsideCountryRegionDictionaryBeforeMapper()
    {
        authenticateAdmin();
        Seller seller = sellerProfile("ZZ");
        RecordingSellerMapper mapper = recordingMapper(null);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            new RecordingPortalTokenSupport());

        try
        {
            service.insertSeller(seller);
        }
        catch (ServiceException e)
        {
            assertEquals("国家/地区代码不在字典范围内", e.getMessage());
            assertEquals(null, mapper.insertedSeller);
            assertEquals(null, mapper.insertedAccount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void insertSellerAccountHashesPasswordDefaultsStaffRoleAndValidatesDept()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(null, 11L, " seller-staff ", PartnerSupport.STATUS_NORMAL);
        account.setNickName("Seller Staff");
        account.setPassword("U12346");
        account.setDeptId(33L);
        RecordingSellerMapper mapper = recordingMapper(seller);
        RecordingSellerDeptMapper deptMapper = deptMapper(dept(11L, 33L));
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper.proxy(),
            new RecordingPortalTokenSupport());

        int rows = service.insertSellerAccount(11L, account);

        assertEquals(1, rows);
        assertSame(account, mapper.insertedAccount);
        assertEquals(Long.valueOf(11L), mapper.insertedAccount.getSellerId());
        assertEquals("seller-staff", mapper.insertedAccount.getUserName());
        assertEquals(PartnerSupport.ACCOUNT_ROLE_STAFF, mapper.insertedAccount.getAccountRole());
        assertEquals(PartnerSupport.STATUS_NORMAL, mapper.insertedAccount.getStatus());
        assertFalse("U12346".equals(mapper.insertedAccount.getPassword()));
        assertTrue(SecurityUtils.matchesPassword("U12346", mapper.insertedAccount.getPassword()));
        assertEquals(Long.valueOf(11L), deptMapper.lastSelectSellerId);
        assertEquals(Long.valueOf(33L), deptMapper.lastSelectDeptId);
    }

    @Test
    public void insertSellerAccountRejectsDeptOutsideSeller()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(null, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        account.setNickName("Seller Staff");
        account.setPassword("U12346");
        account.setDeptId(99L);
        RecordingSellerMapper mapper = recordingMapper(seller);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            new RecordingPortalTokenSupport());

        try
        {
            service.insertSellerAccount(11L, account);
        }
        catch (ServiceException e)
        {
            assertEquals("Seller department does not belong to this seller", e.getMessage());
            assertEquals(null, mapper.insertedAccount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void insertSellerAccountRejectsSecondOwnerAccount()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount owner = account(21L, 11L, "seller-owner", PartnerSupport.STATUS_NORMAL);
        owner.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        SellerAccount account = account(null, 11L, "seller-owner-2", PartnerSupport.STATUS_NORMAL);
        account.setNickName("Second Owner");
        account.setPassword("U12346");
        account.setAccountRole("owner");
        RecordingSellerMapper mapper = recordingMapper(seller, owner);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            new RecordingPortalTokenSupport());

        try
        {
            service.insertSellerAccount(11L, account);
        }
        catch (ServiceException e)
        {
            assertEquals("卖家主账号已存在", e.getMessage());
            assertEquals(null, mapper.insertedAccount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void insertSellerAccountRejectsInvalidAccountRole()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(null, 11L, "seller-root", PartnerSupport.STATUS_NORMAL);
        account.setNickName("Seller Root");
        account.setPassword("U12346");
        account.setAccountRole("root");
        RecordingSellerMapper mapper = recordingMapper(seller);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            new RecordingPortalTokenSupport());

        try
        {
            service.insertSellerAccount(11L, account);
        }
        catch (ServiceException e)
        {
            assertEquals("账号角色不正确", e.getMessage());
            assertEquals(null, mapper.insertedAccount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void updateSellerAccountPreservesCurrentOwnerRole()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount owner = account(21L, 11L, "seller-owner", PartnerSupport.STATUS_NORMAL);
        owner.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        RecordingSellerMapper mapper = recordingMapper(seller, owner);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            new RecordingPortalTokenSupport());
        SellerAccount update = new SellerAccount();
        update.setSellerAccountId(21L);
        update.setNickName("Seller Owner");
        update.setAccountRole(PartnerSupport.ACCOUNT_ROLE_STAFF);
        update.setStatus(PartnerSupport.STATUS_NORMAL);

        int rows = service.updateSellerAccount(11L, update);

        assertEquals(1, rows);
        assertEquals(PartnerSupport.ACCOUNT_ROLE_OWNER, mapper.updatedAccount.getAccountRole());
    }

    @Test
    public void resetSellerAccountPasswordForcesOnlyThatAccountSessionsOut()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        mapper.onlineSessions.add(onlineSession(11L, 22L, "seller-staff", "token-a", false));
        PortalLoginSession directLoginSession = onlineSession(11L, 22L, "seller-staff", "token-b", true);
        directLoginSession.setActingAdminId(99L);
        directLoginSession.setActingAdminName("issuer-admin");
        mapper.onlineSessions.add(directLoginSession);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);
        int rows = service.resetSellerAccountPassword(11L, 22L, "new-secret");

        assertEquals(1, rows);
        assertEquals(Long.valueOf(11L), mapper.resetPasswordSellerId);
        assertEquals(Long.valueOf(22L), mapper.resetPasswordAccountId);
        assertFalse("new-secret".equals(mapper.resetPasswordValue));
        assertTrue(SecurityUtils.matchesPassword("new-secret", mapper.resetPasswordValue));
        assertEquals(1, mapper.forceLogoutCallCount);
        assertEquals(Long.valueOf(11L), mapper.forceLogoutSellerId);
        assertEquals(Long.valueOf(22L), mapper.forceLogoutAccountId);
        assertEquals(Constants.SUCCESS, mapper.insertedLoginLog.getStatus());
        assertEquals("PASSWORD_RESET_FORCE_LOGOUT", mapper.insertedLoginLog.getMsg());
        assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
        assertEquals("seller-staff", mapper.insertedLoginLog.getUserName());
        assertEquals(2, mapper.insertedLoginLogs.size());
        assertEquals(Boolean.FALSE, mapper.insertedLoginLogs.get(0).getDirectLogin());
        assertCurrentAdminAudit(mapper.insertedLoginLogs.get(0));
        assertEquals(Boolean.TRUE, mapper.insertedLoginLogs.get(1).getDirectLogin());
        assertEquals(Long.valueOf(100L), mapper.insertedLoginLogs.get(1).getDirectLoginTicketId());
        assertEquals("support check", mapper.insertedLoginLogs.get(1).getDirectLoginReason());
        assertCurrentAdminAudit(mapper.insertedLoginLogs.get(1));
        assertEquals("seller", tokenSupport.deletedTerminal);
        assertEquals(List.of("token-a", "token-b"), tokenSupport.deletedTokenIds);
    }

    @Test
    public void resetSellerAccountPasswordRejectsInvalidTemporaryPasswordBeforeMapper()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            new RecordingPortalTokenSupport());

        try
        {
            service.resetSellerAccountPassword(11L, 22L, "1234");
        }
        catch (ServiceException e)
        {
            assertEquals("密码长度必须在5到20个字符之间", e.getMessage());
            assertEquals(null, mapper.resetPasswordAccountId);
            assertEquals(0, mapper.forceLogoutCallCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void resetSellerAccountPasswordRejectsAccountOutsideSeller()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 12L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            new RecordingPortalTokenSupport());

        try
        {
            service.resetSellerAccountPassword(11L, 22L, "new-secret");
        }
        catch (ServiceException e)
        {
            assertEquals(null, mapper.resetPasswordAccountId);
            assertEquals(0, mapper.forceLogoutCallCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void updateSellerAccountDisablingAccountForcesOnlyThatAccountSessionsOut()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount current = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        current.setNickName("Seller Staff");
        RecordingSellerMapper mapper = recordingMapper(seller, current);
        mapper.onlineTokenIds.add("token-a");
        mapper.onlineTokenIds.add("token-b");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);
        SellerAccount update = new SellerAccount();
        update.setSellerAccountId(22L);
        update.setNickName("Seller Staff");
        update.setStatus("1");

        int rows = service.updateSellerAccount(11L, update);

        assertEquals(1, rows);
        assertEquals(Long.valueOf(22L), mapper.updatedAccount.getSellerAccountId());
        assertEquals("seller-staff", mapper.updatedAccount.getUserName());
        assertEquals("1", mapper.updatedAccount.getStatus());
        assertEquals(Long.valueOf(11L), mapper.forceLogoutSellerId);
        assertEquals(Long.valueOf(22L), mapper.forceLogoutAccountId);
        assertEquals("seller", tokenSupport.deletedTerminal);
        assertEquals(mapper.onlineTokenIds, tokenSupport.deletedTokenIds);
    }

    @Test
    public void updateSellerAccountRejectsAccountOutsideSeller()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount otherSellerAccount = account(22L, 12L, "other-seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, otherSellerAccount);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            new RecordingPortalTokenSupport());
        SellerAccount update = new SellerAccount();
        update.setSellerAccountId(22L);
        update.setNickName("Other Seller Staff");
        update.setStatus("1");

        try
        {
            service.updateSellerAccount(11L, update);
        }
        catch (ServiceException e)
        {
            assertEquals(null, mapper.updatedAccount);
            assertEquals(0, mapper.forceLogoutCallCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void lockSellerAccountLocksAccountAndForcesOnlyThatAccountSessionsOut()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount current = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, current);
        mapper.onlineTokenIds.add("token-a");
        mapper.onlineTokenIds.add("token-b");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);

        int rows = service.lockSellerAccount(11L, 22L, " risk review ");

        assertEquals(1, rows);
        assertEquals(1, mapper.updateLockStatusCallCount);
        assertEquals(Long.valueOf(11L), mapper.lockStatusSellerId);
        assertEquals(Long.valueOf(22L), mapper.lockStatusAccountId);
        assertEquals(PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED, mapper.lockStatusValue);
        assertEquals("risk review", mapper.lockReasonValue);
        assertEquals(Long.valueOf(11L), mapper.forceLogoutSellerId);
        assertEquals(Long.valueOf(22L), mapper.forceLogoutAccountId);
        assertEquals(Constants.SUCCESS, mapper.insertedLoginLog.getStatus());
        assertEquals("FORCE_LOGOUT", mapper.insertedLoginLog.getMsg());
        assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
        assertEquals("seller-staff", mapper.insertedLoginLog.getUserName());
        assertCurrentAdminAudit(mapper.insertedLoginLog);
        assertEquals("seller", tokenSupport.deletedTerminal);
        assertEquals(mapper.onlineTokenIds, tokenSupport.deletedTokenIds);
        assertEquals(1, tokenSupport.deleteLoginTokensCallCount);
    }

    @Test
    public void lockSellerAccountRejectsBlankReasonBeforeMutatingOrKicking()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount current = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, current);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);

        try
        {
            service.lockSellerAccount(11L, 22L, " ");
        }
        catch (ServiceException e)
        {
            assertEquals("锁定原因不能为空", e.getMessage());
            assertEquals(0, mapper.updateLockStatusCallCount);
            assertEquals(0, mapper.forceLogoutCallCount);
            assertEquals(0, tokenSupport.deleteLoginTokensCallCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void lockSellerAccountRejectsAccountOutsideSellerBeforeMutatingOrKicking()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount otherSellerAccount = account(22L, 12L, "other-seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, otherSellerAccount);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);

        try
        {
            service.lockSellerAccount(11L, 22L, "risk review");
        }
        catch (ServiceException e)
        {
            assertEquals(0, mapper.updateLockStatusCallCount);
            assertEquals(0, mapper.forceLogoutCallCount);
            assertEquals(0, tokenSupport.deleteLoginTokensCallCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void unlockSellerAccountUnlocksWithoutForceLogout()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount current = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        current.setLockStatus(PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED);
        current.setLockReason("risk review");
        RecordingSellerMapper mapper = recordingMapper(seller, current);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);

        int rows = service.unlockSellerAccount(11L, 22L);

        assertEquals(1, rows);
        assertEquals(1, mapper.updateLockStatusCallCount);
        assertEquals(Long.valueOf(11L), mapper.lockStatusSellerId);
        assertEquals(Long.valueOf(22L), mapper.lockStatusAccountId);
        assertEquals(PartnerSupport.ACCOUNT_LOCK_STATUS_UNLOCKED, mapper.lockStatusValue);
        assertEquals("", mapper.lockReasonValue);
        assertEquals(0, mapper.forceLogoutCallCount);
        assertEquals(0, tokenSupport.deleteLoginTokensCallCount);
    }

    @Test
    public void forceLogoutSellerSessionsAuditsDirectLoginSessionsWithCurrentAdminAndDeletesSellerTokens()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        mapper.onlineSessions.add(onlineSession(11L, 22L, "seller-staff", "token-a", false));
        PortalLoginSession directLoginSession = onlineSession(11L, 22L, "seller-staff", "token-b", true);
        directLoginSession.setActingAdminId(99L);
        directLoginSession.setActingAdminName("issuer-admin");
        mapper.onlineSessions.add(directLoginSession);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);

        int rows = service.forceLogoutSellerSessions(11L);

        assertEquals(2, rows);
        assertEquals(1, mapper.forceLogoutCallCount);
        assertEquals(Long.valueOf(11L), mapper.forceLogoutSellerId);
        assertEquals(null, mapper.forceLogoutAccountId);
        assertEquals(2, mapper.insertedLoginLogs.size());
        assertEquals(Boolean.FALSE, mapper.insertedLoginLogs.get(0).getDirectLogin());
        assertEquals(Constants.SUCCESS, mapper.insertedLoginLogs.get(0).getStatus());
        assertEquals("FORCE_LOGOUT", mapper.insertedLoginLogs.get(0).getMsg());
        assertEquals(Long.valueOf(11L), mapper.insertedLoginLogs.get(0).getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.insertedLoginLogs.get(0).getAccountId());
        assertCurrentAdminAudit(mapper.insertedLoginLogs.get(0));
        assertEquals(Boolean.TRUE, mapper.insertedLoginLogs.get(1).getDirectLogin());
        assertEquals(Long.valueOf(100L), mapper.insertedLoginLogs.get(1).getDirectLoginTicketId());
        assertEquals("support check", mapper.insertedLoginLogs.get(1).getDirectLoginReason());
        assertCurrentAdminAudit(mapper.insertedLoginLogs.get(1));
        assertEquals("seller", tokenSupport.deletedTerminal);
        assertEquals(List.of("token-a", "token-b"), tokenSupport.deletedTokenIds);
        assertEquals(1, tokenSupport.deleteLoginTokensCallCount);
    }

    @Test
    public void forceLogoutSellerAccountSessionsAuditsDirectLoginSessionsWithCurrentAdminAndDeletesSellerTokens()
    {
        authenticateAdmin();
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        mapper.onlineSessions.add(onlineSession(11L, 22L, "seller-staff", "token-a", false));
        PortalLoginSession directLoginSession = onlineSession(11L, 22L, "seller-staff", "token-b", true);
        directLoginSession.setActingAdminId(99L);
        directLoginSession.setActingAdminName("issuer-admin");
        mapper.onlineSessions.add(directLoginSession);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);

        int rows = service.forceLogoutSellerAccountSessions(11L, 22L);

        assertEquals(2, rows);
        assertEquals(1, mapper.forceLogoutCallCount);
        assertEquals(Long.valueOf(11L), mapper.forceLogoutSellerId);
        assertEquals(Long.valueOf(22L), mapper.forceLogoutAccountId);
        assertEquals(2, mapper.insertedLoginLogs.size());
        assertEquals(Boolean.FALSE, mapper.insertedLoginLogs.get(0).getDirectLogin());
        assertEquals(Constants.SUCCESS, mapper.insertedLoginLogs.get(0).getStatus());
        assertEquals("FORCE_LOGOUT", mapper.insertedLoginLogs.get(0).getMsg());
        assertEquals(Long.valueOf(11L), mapper.insertedLoginLogs.get(0).getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.insertedLoginLogs.get(0).getAccountId());
        assertCurrentAdminAudit(mapper.insertedLoginLogs.get(0));
        assertEquals(Boolean.TRUE, mapper.insertedLoginLogs.get(1).getDirectLogin());
        assertEquals(Long.valueOf(100L), mapper.insertedLoginLogs.get(1).getDirectLoginTicketId());
        assertEquals("support check", mapper.insertedLoginLogs.get(1).getDirectLoginReason());
        assertCurrentAdminAudit(mapper.insertedLoginLogs.get(1));
        assertEquals("seller", tokenSupport.deletedTerminal);
        assertEquals(List.of("token-a", "token-b"), tokenSupport.deletedTokenIds);
        assertEquals(1, tokenSupport.deleteLoginTokensCallCount);
    }

    @Test
    public void forceLogoutSellerAccountSessionsRejectsAccountOutsideSeller()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount otherSellerAccount = account(22L, 12L, "other-seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, otherSellerAccount);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);

        try
        {
            service.forceLogoutSellerAccountSessions(11L, 22L);
        }
        catch (ServiceException e)
        {
            assertEquals(0, mapper.forceLogoutCallCount);
            assertEquals(0, tokenSupport.deleteLoginTokensCallCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void loginSellerUpdatesLastLoginSessionAndSuccessLog()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        account.setPassword(SecurityUtils.encryptPassword("secret"));
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);
        LoginBody loginBody = new LoginBody();
        loginBody.setUsername("seller-staff");
        loginBody.setPassword("secret");

        PortalLoginResult result = service.loginSeller(loginBody);

        assertEquals("seller", result.getTerminal());
        assertEquals(Long.valueOf(11L), result.getSubjectId());
        assertEquals(Long.valueOf(22L), result.getAccountId());
        assertEquals(Long.valueOf(11L), mapper.loginInfoSellerId);
        assertEquals(Long.valueOf(22L), mapper.loginInfoAccountId);
        assertEquals("127.0.0.1", mapper.loginInfoIp);
        assertSame(tokenSupport.createdSession, mapper.insertedSession);
        assertEquals(Constants.SUCCESS, mapper.insertedLoginLog.getStatus());
        assertEquals("登录成功", mapper.insertedLoginLog.getMsg());
    }

    @Test
    public void loginSellerRejectsDisabledAccountAndWritesFailureLog()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", "1");
        account.setPassword(SecurityUtils.encryptPassword("secret"));
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);
        LoginBody loginBody = new LoginBody();
        loginBody.setUsername("seller-staff");
        loginBody.setPassword("secret");

        try
        {
            service.loginSeller(loginBody);
        }
        catch (ServiceException e)
        {
            assertEquals("卖家账号已停用", e.getMessage());
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals("卖家账号已停用", mapper.insertedLoginLog.getMsg());
            assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
            assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void loginSellerRejectsLockedAccountAndWritesFailureLog()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        account.setLockStatus(PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED);
        account.setLockReason("risk review");
        account.setPassword(SecurityUtils.encryptPassword("secret"));
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);
        LoginBody loginBody = new LoginBody();
        loginBody.setUsername("seller-staff");
        loginBody.setPassword("secret");

        try
        {
            service.loginSeller(loginBody);
        }
        catch (ServiceException e)
        {
            assertEquals("卖家账号已锁定", e.getMessage());
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(null, mapper.loginInfoAccountId);
            assertEquals(null, mapper.insertedSession);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals("卖家账号已锁定", mapper.insertedLoginLog.getMsg());
            assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
            assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerOwnSessionListUsesSessionScopeAndMarksCurrent()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        PortalSessionProfile current = sessionProfile("seller_test_token");
        PortalSessionProfile other = sessionProfile("seller_other_token");
        Page<PortalSessionProfile> page = new Page<>(1, 10, true);
        page.setTotal(2);
        page.setPages(1);
        page.add(current);
        page.add(other);
        mapper.sessionProfiles = page;
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            new RecordingPortalTokenSupport());

        List<PortalOwnSessionProfile> result = service.selectSellerOwnSessionList(session(11L, 22L));

        Page<?> resultPage = (Page<?>) result;
        assertEquals(2, result.size());
        assertEquals(1, resultPage.getPageNum());
        assertEquals(10, resultPage.getPageSize());
        assertEquals(2L, resultPage.getTotal());
        assertEquals(1, resultPage.getPages());
        assertEquals(Long.valueOf(11L), mapper.sessionProfileSellerId);
        assertEquals(Long.valueOf(22L), mapper.sessionProfileAccountId);
        assertEquals(Boolean.TRUE, current.getCurrent());
        assertEquals(Boolean.FALSE, other.getCurrent());
        assertEquals("seller-staff", result.get(0).getUserName());
        assertEquals(Boolean.TRUE, result.get(0).getCurrent());
        assertEquals(Boolean.FALSE, result.get(1).getCurrent());
    }

    @Test
    public void selectSellerOwnSessionListRejectsWrongTerminalBeforeMapper()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalLoginSession session = session(11L, 22L);
        session.setTerminal("buyer");

        try
        {
            service.selectSellerOwnSessionList(session);
        }
        catch (ServiceException e)
        {
            assertEquals(null, mapper.sessionProfileSellerId);
            assertEquals(null, mapper.sessionProfileAccountId);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerSessionListValidatesSellerBeforeQueryingSessions()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller);
        PortalSessionProfile profile = sessionProfile("seller_test_token");
        mapper.sessionProfiles.add(profile);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());

        List<PortalSessionProfile> result = service.selectSellerSessionList(11L);

        assertSame(mapper.sessionProfiles, result);
        assertEquals(Long.valueOf(11L), mapper.sessionProfileSellerId);
        assertEquals(null, mapper.sessionProfileAccountId);
    }

    @Test
    public void selectSellerSessionListRejectsMissingSellerBeforeMapper()
    {
        RecordingSellerMapper mapper = recordingMapper(null);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());

        try
        {
            service.selectSellerSessionList(11L);
        }
        catch (ServiceException e)
        {
            assertEquals("卖家不存在", e.getMessage());
            assertEquals(null, mapper.sessionProfileSellerId);
            assertEquals(null, mapper.sessionProfileAccountId);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerAccountSessionListValidatesAccountScopeBeforeQueryingSessions()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        PortalSessionProfile profile = sessionProfile("seller_test_token");
        mapper.sessionProfiles.add(profile);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());

        List<PortalSessionProfile> result = service.selectSellerAccountSessionList(11L, 22L);

        assertSame(mapper.sessionProfiles, result);
        assertEquals(Long.valueOf(11L), mapper.sessionProfileSellerId);
        assertEquals(Long.valueOf(22L), mapper.sessionProfileAccountId);
    }

    @Test
    public void selectSellerAccountSessionListRejectsAccountFromAnotherSellerBeforeMapper()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 99L, "other-seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());

        try
        {
            service.selectSellerAccountSessionList(11L, 22L);
        }
        catch (ServiceException e)
        {
            assertEquals("卖家账号不存在", e.getMessage());
            assertEquals(null, mapper.sessionProfileSellerId);
            assertEquals(null, mapper.sessionProfileAccountId);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void createSellerAccountDirectLoginUsesSelectedSellerAccount()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        SellerServiceImpl service = service(mapper(seller, account), directLoginSupport);

        PortalDirectLoginResult result = service.createSellerAccountDirectLogin(11L, 22L, "support check");

        assertEquals(Long.valueOf(22L), result.getAccountId());
        assertEquals("seller-staff", result.getUsername());
        assertEquals(1, directLoginSupport.callCount);
        assertEquals("seller", directLoginSupport.portalType);
        assertEquals(Long.valueOf(11L), directLoginSupport.partnerId);
        assertEquals("SAA010001", directLoginSupport.partnerNo);
        assertSame(account, directLoginSupport.account);
        assertEquals("support check", directLoginSupport.reason);
        assertEquals(PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, directLoginSupport.webUrlConfigKey);
        assertEquals(null, directLoginSupport.fallbackWebUrl);
    }

    @Test
    public void createSellerAccountDirectLoginRejectsAccountFromAnotherSeller()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 99L, "other-seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        SellerServiceImpl service = service(mapper(seller, account), directLoginSupport);

        try
        {
            service.createSellerAccountDirectLogin(11L, 22L, "support check");
        }
        catch (ServiceException e)
        {
            assertEquals("卖家账号不存在", e.getMessage());
            assertEquals(0, directLoginSupport.callCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void createSellerAccountDirectLoginRejectsDisabledSeller()
    {
        Seller seller = seller(11L, "SAA010001", "1");
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        SellerServiceImpl service = service(mapper(seller, account), directLoginSupport);

        try
        {
            service.createSellerAccountDirectLogin(11L, 22L, "support check");
        }
        catch (ServiceException e)
        {
            assertEquals("卖家已停用，不能免密登录", e.getMessage());
            assertEquals(0, directLoginSupport.callCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void createSellerAccountDirectLoginRejectsLockedAccountBeforeCreatingToken()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        account.setLockStatus(PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        SellerServiceImpl service = service(mapper(seller, account), directLoginSupport);

        try
        {
            service.createSellerAccountDirectLogin(11L, 22L, "support check");
        }
        catch (ServiceException e)
        {
            assertEquals("卖家账号已锁定，不能免密登录", e.getMessage());
            assertEquals(0, directLoginSupport.callCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void directLoginSellerUsesCurrentSellerAndAccountState()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        directLoginSupport.tokenToConsume = directLoginToken("seller", 11L, "SAA010001", 22L, "seller-staff");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), directLoginSupport, deptMapper().proxy(), tokenSupport);

        PortalLoginResult result = service.directLoginSeller("issued-token");

        assertEquals(1, directLoginSupport.consumeCount);
        assertEquals("seller", directLoginSupport.consumedPortalType);
        assertEquals("issued-token", directLoginSupport.consumedTokenValue);
        assertEquals("seller", result.getTerminal());
        assertEquals(Long.valueOf(11L), result.getSubjectId());
        assertEquals(Long.valueOf(22L), result.getAccountId());
        assertEquals(1, tokenSupport.createLoginCount);
        assertSame(tokenSupport.createdSession, mapper.insertedSession);
        assertEquals(Boolean.TRUE, mapper.insertedSession.getDirectLogin());
        assertEquals(Long.valueOf(100L), mapper.insertedSession.getDirectLoginTicketId());
        assertEquals(Long.valueOf(1L), mapper.insertedSession.getActingAdminId());
        assertEquals("admin", mapper.insertedSession.getActingAdminName());
        assertEquals(Constants.SUCCESS, mapper.insertedLoginLog.getStatus());
        assertDirectLoginAudit(mapper.insertedLoginLog);
        assertEquals("免密登录成功", mapper.insertedLoginLog.getMsg());
        assertFalse(mapper.insertedLoginLog.getMsg().contains("ticketId="));
        assertFalse(mapper.insertedLoginLog.getMsg().contains("actingAdminName="));
    }

    @Test
    public void directLoginSellerWritesFailureLogWhenTicketCannotBeConsumed()
    {
        RecordingSellerMapper mapper = recordingMapper(null);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        directLoginSupport.consumeException = new ServiceException("免密登录票据不存在");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), directLoginSupport, deptMapper().proxy(), tokenSupport);

        try
        {
            service.directLoginSeller("missing-ticket");
        }
        catch (ServiceException e)
        {
            assertEquals("免密登录票据不存在", e.getMessage());
            assertEquals(1, directLoginSupport.consumeCount);
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(null, mapper.insertedSession);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals("免密登录票据不存在", mapper.insertedLoginLog.getMsg());
            assertEquals(null, mapper.insertedLoginLog.getSubjectId());
            assertEquals(null, mapper.insertedLoginLog.getAccountId());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void directLoginSellerWritesDirectLoginFailureLogWhenTicketContextIsAvailable()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        directLoginSupport.tokenToConsume = directLoginToken("seller", 11L, "SAA010001", 22L, "seller-staff");
        directLoginSupport.consumeException = new ServiceException("免密登录 token 不存在或已过期");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), directLoginSupport, deptMapper().proxy(), tokenSupport);

        try
        {
            service.directLoginSeller("expired-token");
        }
        catch (ServiceException e)
        {
            assertEquals("免密登录 token 不存在或已过期", e.getMessage());
            assertEquals(1, directLoginSupport.consumeCount);
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(null, mapper.insertedSession);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
            assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
            assertEquals("seller-staff", mapper.insertedLoginLog.getUserName());
            assertDirectLoginAudit(mapper.insertedLoginLog);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void directLoginSellerDoesNotWriteForeignTicketAuditIntoSellerLog()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        directLoginSupport.tokenToConsume = directLoginToken("buyer", 11L, "BAA010001", 22L, "buyer-staff");
        directLoginSupport.consumeException = new ServiceException("免密登录票据端类型不匹配");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), directLoginSupport, deptMapper().proxy(), tokenSupport);

        try
        {
            service.directLoginSeller("foreign-token");
        }
        catch (ServiceException e)
        {
            assertEquals("免密登录票据端类型不匹配", e.getMessage());
            assertEquals(1, directLoginSupport.consumeCount);
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(null, mapper.insertedSession);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals(null, mapper.insertedLoginLog.getSubjectId());
            assertEquals(null, mapper.insertedLoginLog.getAccountId());
            assertEquals(null, mapper.insertedLoginLog.getUserName());
            assertNoDirectLoginAudit(mapper.insertedLoginLog);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void directLoginSellerRejectsDisabledSellerAfterTicketIssued()
    {
        Seller seller = seller(11L, "SAA010001", "1");
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        directLoginSupport.tokenToConsume = directLoginToken("seller", 11L, "SAA010001", 22L, "seller-staff");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), directLoginSupport, deptMapper().proxy(), tokenSupport);

        try
        {
            service.directLoginSeller("issued-token");
        }
        catch (ServiceException e)
        {
            assertEquals(1, directLoginSupport.consumeCount);
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(null, mapper.insertedSession);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
            assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
            assertDirectLoginAudit(mapper.insertedLoginLog);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void directLoginSellerRejectsDisabledAccountAfterTicketIssued()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", "1");
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        directLoginSupport.tokenToConsume = directLoginToken("seller", 11L, "SAA010001", 22L, "seller-staff");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), directLoginSupport, deptMapper().proxy(), tokenSupport);

        try
        {
            service.directLoginSeller("issued-token");
        }
        catch (ServiceException e)
        {
            assertEquals(1, directLoginSupport.consumeCount);
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(null, mapper.insertedSession);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
            assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
            assertDirectLoginAudit(mapper.insertedLoginLog);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void directLoginSellerRejectsLockedAccountAfterTicketIssued()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        account.setLockStatus(PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        directLoginSupport.tokenToConsume = directLoginToken("seller", 11L, "SAA010001", 22L, "seller-staff");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), directLoginSupport, deptMapper().proxy(), tokenSupport);

        try
        {
            service.directLoginSeller("issued-token");
        }
        catch (ServiceException e)
        {
            assertEquals(1, directLoginSupport.consumeCount);
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(null, mapper.insertedSession);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
            assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
            assertDirectLoginAudit(mapper.insertedLoginLog);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void directLoginSellerRejectsMissingAccountAfterTicketIssued()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        directLoginSupport.tokenToConsume = directLoginToken("seller", 11L, "SAA010001", 22L, "seller-staff");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), directLoginSupport, deptMapper().proxy(), tokenSupport);

        try
        {
            service.directLoginSeller("issued-token");
        }
        catch (ServiceException e)
        {
            assertEquals(1, directLoginSupport.consumeCount);
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(null, mapper.insertedSession);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
            assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
            assertDirectLoginAudit(mapper.insertedLoginLog);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void directLoginSellerRejectsAccountMovedToAnotherSellerAfterTicketIssued()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 99L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        directLoginSupport.tokenToConsume = directLoginToken("seller", 11L, "SAA010001", 22L, "seller-staff");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), directLoginSupport, deptMapper().proxy(), tokenSupport);

        try
        {
            service.directLoginSeller("issued-token");
        }
        catch (ServiceException e)
        {
            assertEquals(1, directLoginSupport.consumeCount);
            assertEquals(0, tokenSupport.createLoginCount);
            assertEquals(null, mapper.insertedSession);
            assertEquals(Constants.FAIL, mapper.insertedLoginLog.getStatus());
            assertEquals(Long.valueOf(11L), mapper.insertedLoginLog.getSubjectId());
            assertEquals(Long.valueOf(22L), mapper.insertedLoginLog.getAccountId());
            assertDirectLoginAudit(mapper.insertedLoginLog);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerOwnLoginLogListUsesSessionScopeAndIgnoresClientScope()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalLoginSession session = session(11L, 22L);
        PortalLoginLog request = new PortalLoginLog();
        request.setSubjectId(99L);
        request.setAccountId(88L);
        request.setUserName("staff");
        request.setIpaddr("127.0.0.1");
        request.setStatus(Constants.SUCCESS);
        request.getParams().put("beginTime", "2026-06-01");
        request.getParams().put("unexpected", "ignored");
        Page<PortalLoginLog> page = new Page<>(2, 10, true);
        page.setTotal(21);
        page.setPages(3);
        PortalLoginLog row = new PortalLoginLog();
        row.setUserName("staff");
        row.setDirectLogin(Boolean.TRUE);
        row.setStatus(Constants.SUCCESS);
        row.setMsg("internal direct login reason");
        page.add(row);
        mapper.loginLogResult = page;

        List<PortalOwnLoginLogProfile> result = service.selectSellerOwnLoginLogList(session, request);

        Page<?> resultPage = (Page<?>) result;
        assertEquals(1, result.size());
        assertEquals(2, resultPage.getPageNum());
        assertEquals(10, resultPage.getPageSize());
        assertEquals(21L, resultPage.getTotal());
        assertEquals(3, resultPage.getPages());
        assertEquals("staff", result.get(0).getUserName());
        assertEquals("免密登录成功", result.get(0).getMsg());
        assertEquals(Long.valueOf(11L), mapper.loginLogQuery.getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.loginLogQuery.getAccountId());
        assertEquals("staff", mapper.loginLogQuery.getUserName());
        assertEquals("127.0.0.1", mapper.loginLogQuery.getIpaddr());
        assertEquals(Constants.SUCCESS, mapper.loginLogQuery.getStatus());
        assertEquals("2026-06-01", mapper.loginLogQuery.getParams().get("beginTime"));
        assertEquals(null, mapper.loginLogQuery.getParams().get("unexpected"));
        assertEquals(Long.valueOf(99L), request.getSubjectId());
        assertEquals(Long.valueOf(88L), request.getAccountId());
    }

    @Test
    public void selectSellerOwnLoginLogListRejectsMissingTokenBeforeMapper()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalLoginSession session = session(11L, 22L);
        session.setTokenId(null);

        try
        {
            service.selectSellerOwnLoginLogList(session, new PortalLoginLog());
        }
        catch (ServiceException e)
        {
            assertEquals(null, mapper.loginLogQuery);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerLoginLogListRequiresSubjectWhenAccountFilterPresent()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalLoginLog request = new PortalLoginLog();
        request.setAccountId(22L);

        try
        {
            service.selectSellerLoginLogList(request);
        }
        catch (ServiceException e)
        {
            assertEquals(null, mapper.loginLogQuery);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerLoginLogListUsesExplicitSubjectAndAccountScope()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalLoginLog request = new PortalLoginLog();
        request.setSubjectId(11L);
        request.setAccountId(22L);

        List<PortalLoginLog> result = service.selectSellerLoginLogList(request);

        assertSame(mapper.loginLogResult, result);
        assertEquals(Long.valueOf(11L), mapper.loginLogQuery.getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.loginLogQuery.getAccountId());
    }

    @Test
    public void selectSellerOperLogListRejectsMismatchedSubjectAndAccount()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalOperLog request = new PortalOperLog();
        request.setSubjectId(99L);
        request.setAccountId(22L);

        try
        {
            service.selectSellerOperLogList(request);
        }
        catch (ServiceException e)
        {
            assertEquals(null, mapper.operLogQuery);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerOperLogListUsesExplicitSubjectAndAccountScope()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalOperLog request = new PortalOperLog();
        request.setSubjectId(11L);
        request.setAccountId(22L);

        List<PortalOperLog> result = service.selectSellerOperLogList(request);

        assertSame(mapper.operLogResult, result);
        assertEquals(Long.valueOf(11L), mapper.operLogQuery.getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.operLogQuery.getAccountId());
    }

    @Test
    public void selectSellerDirectLoginTicketListRequiresSubjectWhenAccountFilterPresent()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingTicketMapper ticketMapper = new RecordingTicketMapper();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), ticketMapper);
        PortalDirectLoginTicket request = new PortalDirectLoginTicket();
        request.setTargetAccountId(22L);

        try
        {
            service.selectSellerDirectLoginTicketList(request);
        }
        catch (ServiceException e)
        {
            assertEquals(null, ticketMapper.ticketQuery);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerDirectLoginTicketListUsesExplicitSubjectAndAccountScope()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingTicketMapper ticketMapper = new RecordingTicketMapper();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), ticketMapper);
        PortalDirectLoginTicket request = new PortalDirectLoginTicket();
        request.setTargetSubjectId(11L);
        request.setTargetAccountId(22L);

        List<PortalDirectLoginTicket> result = service.selectSellerDirectLoginTicketList(request);

        assertSame(ticketMapper.ticketResult, result);
        assertEquals("seller", ticketMapper.ticketQuery.getTerminal());
        assertEquals(Long.valueOf(11L), ticketMapper.ticketQuery.getTargetSubjectId());
        assertEquals(Long.valueOf(22L), ticketMapper.ticketQuery.getTargetAccountId());
    }

    @Test
    public void selectSellerDirectLoginTicketListRejectsMismatchedSubjectAndAccount()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingTicketMapper ticketMapper = new RecordingTicketMapper();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), ticketMapper);
        PortalDirectLoginTicket request = new PortalDirectLoginTicket();
        request.setTargetSubjectId(99L);
        request.setTargetAccountId(22L);

        try
        {
            service.selectSellerDirectLoginTicketList(request);
        }
        catch (ServiceException e)
        {
            assertEquals(null, ticketMapper.ticketQuery);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerOwnOperLogListUsesSessionScopeAndIgnoresClientScope()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalLoginSession session = session(11L, 22L);
        PortalOperLog request = new PortalOperLog();
        request.setSubjectId(99L);
        request.setAccountId(88L);
        request.setTitle("订单");
        request.setOperName("staff");
        request.setStatus(0);
        request.getParams().put("endTime", "2026-06-05");
        request.getParams().put("unexpected", "ignored");
        Page<PortalOperLog> page = new Page<>(3, 20, true);
        page.setTotal(41);
        page.setPages(3);
        PortalOperLog row = new PortalOperLog();
        row.setTitle("订单");
        row.setOperName("staff");
        row.setOperParam("{\"secret\":true}");
        row.setJsonResult("{\"internal\":true}");
        page.add(row);
        mapper.operLogResult = page;

        List<PortalOwnOperLogProfile> result = service.selectSellerOwnOperLogList(session, request);

        Page<?> resultPage = (Page<?>) result;
        assertEquals(1, result.size());
        assertEquals(3, resultPage.getPageNum());
        assertEquals(20, resultPage.getPageSize());
        assertEquals(41L, resultPage.getTotal());
        assertEquals(3, resultPage.getPages());
        assertEquals("订单", result.get(0).getTitle());
        assertEquals("staff", result.get(0).getOperName());
        assertEquals(Long.valueOf(11L), mapper.operLogQuery.getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.operLogQuery.getAccountId());
        assertEquals("订单", mapper.operLogQuery.getTitle());
        assertEquals("staff", mapper.operLogQuery.getOperName());
        assertEquals(Integer.valueOf(0), mapper.operLogQuery.getStatus());
        assertEquals("2026-06-05", mapper.operLogQuery.getParams().get("endTime"));
        assertEquals(null, mapper.operLogQuery.getParams().get("unexpected"));
        assertEquals(Long.valueOf(99L), request.getSubjectId());
        assertEquals(Long.valueOf(88L), request.getAccountId());
    }

    @Test
    public void logoutSellerRejectsWrongTerminalBeforeMapperAndTokenDelete()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);
        PortalLoginSession session = session(11L, 22L);
        session.setTerminal("buyer");

        try
        {
            service.logoutSeller(session);
        }
        catch (ServiceException e)
        {
            assertEquals(0, mapper.logoutSessionCallCount);
            assertEquals(null, mapper.insertedLoginLog);
            assertEquals(0, tokenSupport.deleteLoginTokenCallCount);
            assertEquals(0, tokenSupport.deleteLoginTokensCallCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void logoutSellerPreservesDirectLoginAuditFromSession()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);
        PortalLoginSession session = session(11L, 22L);
        session.setUserName("seller-staff");
        session.setDirectLogin(Boolean.TRUE);
        session.setDirectLoginTicketId(100L);
        session.setActingAdminId(1L);
        session.setActingAdminName("admin");
        session.setDirectLoginReason("support check");

        int rows = service.logoutSeller(session);

        assertEquals(1, rows);
        assertEquals(1, mapper.logoutSessionCallCount);
        assertEquals(Long.valueOf(11L), mapper.logoutSellerId);
        assertEquals(Long.valueOf(22L), mapper.logoutAccountId);
        assertEquals("seller_test_token", mapper.logoutTokenId);
        assertEquals(Constants.SUCCESS, mapper.insertedLoginLog.getStatus());
        assertEquals("退出成功", mapper.insertedLoginLog.getMsg());
        assertEquals("seller-staff", mapper.insertedLoginLog.getUserName());
        assertDirectLoginAudit(mapper.insertedLoginLog);
        assertEquals(1, tokenSupport.deleteLoginTokenCallCount);
        assertSame(session, tokenSupport.deletedSession);
    }

    @Test
    public void updateSellerOwnPasswordRejectsMissingTokenBeforeMapper()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalLoginSession session = session(11L, 22L);
        session.setTokenId(null);

        try
        {
            service.updateSellerOwnPassword(session, new PortalPasswordChangeRequest());
        }
        catch (ServiceException e)
        {
            assertEquals(null, mapper.resetPasswordAccountId);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void updateSellerOwnPasswordForcesAccountSessionsOut()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        account.setPassword(SecurityUtils.encryptPassword("old-secret"));
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        mapper.onlineTokenIds.add("seller-token-a");
        mapper.onlineTokenIds.add("seller-token-b");
        RecordingPortalTokenSupport tokenSupport = new RecordingPortalTokenSupport();
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport(), deptMapper().proxy(),
            tokenSupport);
        PortalPasswordChangeRequest request = new PortalPasswordChangeRequest();
        request.setOldPassword("old-secret");
        request.setNewPassword("new-secret");
        request.setConfirmPassword("new-secret");

        int rows = service.updateSellerOwnPassword(session(11L, 22L), request);

        assertEquals(1, rows);
        assertEquals(Long.valueOf(11L), mapper.resetPasswordSellerId);
        assertEquals(Long.valueOf(22L), mapper.resetPasswordAccountId);
        assertTrue(SecurityUtils.matchesPassword("new-secret", mapper.resetPasswordValue));
        assertEquals(Long.valueOf(11L), mapper.forceLogoutSellerId);
        assertEquals(Long.valueOf(22L), mapper.forceLogoutAccountId);
        assertEquals("seller", tokenSupport.deletedTerminal);
        assertEquals(mapper.onlineTokenIds, tokenSupport.deletedTokenIds);
        assertEquals(1, tokenSupport.deleteLoginTokensCallCount);
        assertEquals(2, mapper.insertedLoginLogs.size());
        assertNoAdminAudit(mapper.insertedLoginLogs.get(0));
        assertNoAdminAudit(mapper.insertedLoginLogs.get(1));
    }

    private SellerServiceImpl service(SellerMapper mapper, PortalDirectLoginSupport directLoginSupport)
    {
        return service(mapper, directLoginSupport, deptMapper().proxy(), new RecordingPortalTokenSupport());
    }

    private SellerServiceImpl service(SellerMapper mapper, PortalDirectLoginSupport directLoginSupport,
            RecordingTicketMapper ticketMapper)
    {
        SellerServiceImpl service = service(mapper, directLoginSupport);
        setField(service, "ticketMapper", ticketMapper.proxy());
        return service;
    }

    private SellerServiceImpl service(SellerMapper mapper, PortalDirectLoginSupport directLoginSupport,
            SellerPortalDeptMapper deptMapper, PortalTokenSupport portalTokenSupport)
    {
        SellerServiceImpl service = new SellerServiceImpl();
        setField(service, "sellerMapper", mapper);
        setField(service, "deptMapper", deptMapper);
        setField(service, "directLoginSupport", directLoginSupport);
        setField(service, "portalTokenSupport", portalTokenSupport);
        setField(service, "ticketMapper", new RecordingTicketMapper().proxy());
        setField(service, "dictTypeService", countryDict("CN", "US").proxy());
        return service;
    }

    private Seller seller(Long sellerId, String sellerNo, String status)
    {
        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setSellerNo(sellerNo);
        seller.setStatus(status);
        return seller;
    }

    private Seller sellerProfile(String countryCode)
    {
        Seller seller = seller(null, null, PartnerSupport.STATUS_NORMAL);
        seller.setSellerCode("S-CODE-001");
        seller.setSellerName(" Seller Full Name ");
        seller.setSellerShortName(" Seller ");
        seller.setSellerType("company");
        seller.setCountryCode(countryCode);
        seller.setCity("Shenzhen");
        seller.setPostalCode("518000");
        seller.setAddressLine1("Address 1");
        seller.setContactName("Contact");
        seller.setContactPhone("13800000000");
        seller.setUsername("seller-owner");
        return seller;
    }

    private SellerAccount account(Long accountId, Long sellerId, String userName, String status)
    {
        SellerAccount account = new SellerAccount();
        account.setSellerAccountId(accountId);
        account.setAccountId(accountId);
        account.setSellerId(sellerId);
        account.setUserName(userName);
        account.setNickName(userName);
        account.setStatus(status);
        return account;
    }

    private PortalDept dept(Long sellerId, Long deptId)
    {
        PortalDept dept = new PortalDept();
        dept.setSubjectId(sellerId);
        dept.setDeptId(deptId);
        dept.setParentId(0L);
        dept.setDeptName("Sales");
        dept.setAncestors("0");
        dept.setStatus(PartnerSupport.STATUS_NORMAL);
        return dept;
    }

    private PortalSessionProfile sessionProfile(String tokenId)
    {
        PortalSessionProfile profile = new PortalSessionProfile();
        profile.setTokenId(tokenId);
        profile.setTerminal("seller");
        profile.setSubjectId(11L);
        profile.setAccountId(22L);
        profile.setUserName("seller-staff");
        return profile;
    }

    private PortalLoginSession session(Long sellerId, Long accountId)
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal("seller");
        session.setSubjectId(sellerId);
        session.setAccountId(accountId);
        session.setTokenId("seller_test_token");
        return session;
    }

    private PortalLoginSession onlineSession(Long sellerId, Long accountId, String username, String tokenId,
            boolean directLogin)
    {
        PortalLoginSession session = session(sellerId, accountId);
        session.setUserName(username);
        session.setTokenId(tokenId);
        session.setDirectLogin(directLogin);
        if (directLogin)
        {
            session.setDirectLoginTicketId(100L);
            session.setActingAdminId(1L);
            session.setActingAdminName("admin");
            session.setDirectLoginReason("support check");
        }
        return session;
    }

    private PortalDirectLoginToken directLoginToken(String terminal, Long sellerId, String sellerNo, Long accountId,
            String username)
    {
        PortalDirectLoginToken token = new PortalDirectLoginToken();
        token.setTicketId(100L);
        token.setPortalType(terminal);
        token.setPartnerId(sellerId);
        token.setPartnerNo(sellerNo);
        token.setAccountId(accountId);
        token.setUsername(username);
        token.setActingAdminId(1L);
        token.setActingAdminName("admin");
        token.setDirectLoginReason("support check");
        return token;
    }

    private void assertDirectLoginAudit(PortalLoginLog log)
    {
        assertEquals(Boolean.TRUE, log.getDirectLogin());
        assertEquals(Long.valueOf(100L), log.getDirectLoginTicketId());
        assertEquals(Long.valueOf(1L), log.getActingAdminId());
        assertEquals("admin", log.getActingAdminName());
        assertEquals("support check", log.getDirectLoginReason());
    }

    private void assertNoDirectLoginAudit(PortalLoginLog log)
    {
        assertEquals(Boolean.FALSE, log.getDirectLogin());
        assertEquals(null, log.getDirectLoginTicketId());
        assertEquals(null, log.getActingAdminId());
        assertEquals(null, log.getActingAdminName());
        assertEquals(null, log.getDirectLoginReason());
    }

    private void assertCurrentAdminAudit(PortalLoginLog log)
    {
        assertEquals(Long.valueOf(1L), log.getActingAdminId());
        assertEquals("admin", log.getActingAdminName());
    }

    private void assertNoAdminAudit(PortalLoginLog log)
    {
        assertEquals(null, log.getActingAdminId());
        assertEquals(null, log.getActingAdminName());
    }

    private SellerMapper mapper(Seller seller, SellerAccount... accounts)
    {
        return recordingMapper(seller, accounts).proxy();
    }

    private RecordingSellerMapper recordingMapper(Seller seller, SellerAccount... accounts)
    {
        return new RecordingSellerMapper(seller, accounts);
    }

    private RecordingSellerDeptMapper deptMapper(PortalDept... depts)
    {
        return new RecordingSellerDeptMapper(depts);
    }

    private void authenticateAdmin()
    {
        SysUser user = new SysUser();
        user.setUserName("admin");
        LoginUser loginUser = new LoginUser(1L, 103L, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private class RecordingSellerMapper implements InvocationHandler
    {
        private final Seller seller;

        private final Map<Long, SellerAccount> accountById = new HashMap<>();

        private List<PortalLoginLog> loginLogResult = new ArrayList<>();

        private List<PortalOperLog> operLogResult = new ArrayList<>();

        private PortalLoginLog loginLogQuery;

        private PortalOperLog operLogQuery;

        private SellerAccount insertedAccount;

        private Seller insertedSeller;

        private SellerAccount updatedAccount;

        private int updateLockStatusCallCount;

        private Long lockStatusSellerId;

        private Long lockStatusAccountId;

        private String lockStatusValue;

        private String lockReasonValue;

        private Long resetPasswordAccountId;

        private Long resetPasswordSellerId;

        private String resetPasswordValue;

        private final List<String> onlineTokenIds = new ArrayList<>();

        private final List<PortalLoginSession> onlineSessions = new ArrayList<>();

        private Long forceLogoutSellerId;

        private Long forceLogoutAccountId;

        private int forceLogoutCallCount;

        private Long logoutSellerId;

        private Long logoutAccountId;

        private String logoutTokenId;

        private int logoutSessionCallCount;

        private Long loginInfoAccountId;

        private Long loginInfoSellerId;

        private String loginInfoIp;

        private PortalLoginLog insertedLoginLog;

        private final List<PortalLoginLog> insertedLoginLogs = new ArrayList<>();

        private PortalLoginSession insertedSession;

        private List<PortalSessionProfile> sessionProfiles = new ArrayList<>();

        private Long sessionProfileSellerId;

        private Long sessionProfileAccountId;

        private RecordingSellerMapper(Seller seller, SellerAccount... accounts)
        {
            this.seller = seller;
            for (SellerAccount account : accounts)
            {
                accountById.put(account.getSellerAccountId(), account);
            }
        }

        private SellerMapper proxy()
        {
            return (SellerMapper) Proxy.newProxyInstance(
                SellerMapper.class.getClassLoader(), new Class<?>[] { SellerMapper.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            String methodName = method.getName();
            if ("selectSellerById".equals(methodName))
            {
                Long sellerId = (Long) args[0];
                return seller != null && sellerId.equals(seller.getSellerId()) ? seller : null;
            }
            if ("selectMaxSellerNoByPrefix".equals(methodName))
            {
                return (String) args[0] + "0000";
            }
            if ("selectSellerByCode".equals(methodName))
            {
                return seller != null && seller.getSellerCode().equals(args[0]) ? seller : null;
            }
            if ("insertSeller".equals(methodName))
            {
                insertedSeller = (Seller) args[0];
                if (insertedSeller.getSellerId() == null)
                {
                    insertedSeller.setSellerId(11L);
                }
                return 1;
            }
            if ("selectSellerAccountByIdAndSellerId".equals(methodName))
            {
                Long sellerId = (Long) args[0];
                SellerAccount account = accountById.get((Long) args[1]);
                return account != null && sellerId.equals(account.getSellerId()) ? account : null;
            }
            if ("selectSellerAccountByUserName".equals(methodName))
            {
                for (SellerAccount account : accountById.values())
                {
                    if (account.getUserName().equals(args[0]))
                    {
                        return account;
                    }
                }
                return null;
            }
            if ("selectOwnerSellerAccountBySellerId".equals(methodName))
            {
                Long sellerId = (Long) args[0];
                SellerAccount selected = null;
                for (SellerAccount account : accountById.values())
                {
                    if (sellerId.equals(account.getSellerId())
                        && PartnerSupport.ACCOUNT_ROLE_OWNER.equals(account.getAccountRole())
                        && (selected == null || account.getSellerAccountId() < selected.getSellerAccountId()))
                    {
                        selected = account;
                    }
                }
                return selected;
            }
            if ("insertSellerAccount".equals(methodName))
            {
                insertedAccount = (SellerAccount) args[0];
                return 1;
            }
            if ("updateSellerAccount".equals(methodName))
            {
                updatedAccount = (SellerAccount) args[0];
                return 1;
            }
            if ("updateSellerAccountLockStatus".equals(methodName))
            {
                updateLockStatusCallCount++;
                lockStatusSellerId = (Long) args[0];
                lockStatusAccountId = (Long) args[1];
                lockStatusValue = (String) args[2];
                lockReasonValue = (String) args[3];
                SellerAccount account = accountById.get(lockStatusAccountId);
                if (account != null && lockStatusSellerId.equals(account.getSellerId()))
                {
                    account.setLockStatus(lockStatusValue);
                    account.setLockReason(lockReasonValue);
                    return 1;
                }
                return 0;
            }
            if ("resetSellerAccountPassword".equals(methodName))
            {
                resetPasswordSellerId = (Long) args[0];
                resetPasswordAccountId = (Long) args[1];
                resetPasswordValue = (String) args[2];
                return 1;
            }
            if ("selectOnlineSellerSessionTokenIds".equals(methodName))
            {
                return onlineTokenIds;
            }
            if ("selectOnlineSellerSessionList".equals(methodName))
            {
                return selectOnlineSessions((Long) args[0], (Long) args[1]);
            }
            if ("forceLogoutSellerSessions".equals(methodName))
            {
                forceLogoutCallCount++;
                forceLogoutSellerId = (Long) args[0];
                forceLogoutAccountId = (Long) args[1];
                return selectOnlineSessions(forceLogoutSellerId, forceLogoutAccountId).size();
            }
            if ("logoutSellerSession".equals(methodName))
            {
                logoutSessionCallCount++;
                logoutSellerId = (Long) args[0];
                logoutAccountId = (Long) args[1];
                logoutTokenId = (String) args[2];
                return 1;
            }
            if ("updateSellerAccountLoginInfo".equals(methodName))
            {
                loginInfoSellerId = (Long) args[0];
                loginInfoAccountId = (Long) args[1];
                loginInfoIp = (String) args[2];
                return 1;
            }
            if ("insertSellerLoginLog".equals(methodName))
            {
                insertedLoginLog = (PortalLoginLog) args[0];
                insertedLoginLogs.add(insertedLoginLog);
                return 1;
            }
            if ("insertSellerSession".equals(methodName))
            {
                insertedSession = (PortalLoginSession) args[0];
                return 1;
            }
            if ("selectSellerSessionProfileList".equals(methodName))
            {
                sessionProfileSellerId = (Long) args[0];
                sessionProfileAccountId = (Long) args[1];
                return sessionProfiles;
            }
            if ("selectSellerLoginLogList".equals(methodName))
            {
                loginLogQuery = (PortalLoginLog) args[0];
                return loginLogResult;
            }
            if ("selectSellerOperLogList".equals(methodName))
            {
                operLogQuery = (PortalOperLog) args[0];
                return operLogResult;
            }
            if ("toString".equals(methodName))
            {
                return "SellerServiceImplTestMapper";
            }
            if ("hashCode".equals(methodName))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName))
            {
                return proxy == args[0];
            }
            return defaultValue(method.getReturnType());
        }

        private List<PortalLoginSession> selectOnlineSessions(Long sellerId, Long sellerAccountId)
        {
            if (!onlineSessions.isEmpty())
            {
                return onlineSessions;
            }
            List<PortalLoginSession> sessions = new ArrayList<>();
            SellerAccount account = sellerAccountId == null ? null : accountById.get(sellerAccountId);
            for (String tokenId : onlineTokenIds)
            {
                PortalLoginSession session = new PortalLoginSession();
                session.setTerminal("seller");
                session.setSubjectId(sellerId);
                session.setAccountId(sellerAccountId);
                session.setTokenId(tokenId);
                session.setUserName(account == null ? null : account.getUserName());
                session.setDirectLogin(Boolean.FALSE);
                sessions.add(session);
            }
            return sessions;
        }
    }

    private class RecordingSellerDeptMapper implements InvocationHandler
    {
        private final Map<Long, PortalDept> deptById = new HashMap<>();

        private Long lastSelectSellerId;

        private Long lastSelectDeptId;

        private RecordingSellerDeptMapper(PortalDept... depts)
        {
            for (PortalDept dept : depts)
            {
                deptById.put(dept.getDeptId(), dept);
            }
        }

        private SellerPortalDeptMapper proxy()
        {
            return (SellerPortalDeptMapper) Proxy.newProxyInstance(
                SellerPortalDeptMapper.class.getClassLoader(), new Class<?>[] { SellerPortalDeptMapper.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            String methodName = method.getName();
            if ("selectSellerDeptById".equals(methodName))
            {
                lastSelectSellerId = (Long) args[0];
                lastSelectDeptId = (Long) args[1];
                PortalDept dept = deptById.get(lastSelectDeptId);
                return dept != null && lastSelectSellerId.equals(dept.getSubjectId()) ? dept : null;
            }
            if ("toString".equals(methodName))
            {
                return "SellerServiceImplTestDeptMapper";
            }
            if ("hashCode".equals(methodName))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName))
            {
                return proxy == args[0];
            }
            return defaultValue(method.getReturnType());
        }
    }

    private class RecordingTicketMapper implements InvocationHandler
    {
        private final List<PortalDirectLoginTicket> ticketResult = new ArrayList<>();

        private PortalDirectLoginTicket ticketQuery;

        private PortalDirectLoginTicketMapper proxy()
        {
            return (PortalDirectLoginTicketMapper) Proxy.newProxyInstance(
                PortalDirectLoginTicketMapper.class.getClassLoader(),
                new Class<?>[] { PortalDirectLoginTicketMapper.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            String methodName = method.getName();
            if ("selectPortalDirectLoginTicketList".equals(methodName))
            {
                ticketQuery = (PortalDirectLoginTicket) args[0];
                return ticketResult;
            }
            if ("toString".equals(methodName))
            {
                return "SellerServiceImplTestTicketMapper";
            }
            if ("hashCode".equals(methodName))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName))
            {
                return proxy == args[0];
            }
            return defaultValue(method.getReturnType());
        }
    }

    private Object defaultValue(Class<?> returnType)
    {
        if (!returnType.isPrimitive())
        {
            return null;
        }
        if (returnType == boolean.class)
        {
            return false;
        }
        if (returnType == byte.class)
        {
            return (byte) 0;
        }
        if (returnType == short.class)
        {
            return (short) 0;
        }
        if (returnType == int.class)
        {
            return 0;
        }
        if (returnType == long.class)
        {
            return 0L;
        }
        if (returnType == float.class)
        {
            return 0F;
        }
        if (returnType == double.class)
        {
            return 0D;
        }
        if (returnType == char.class)
        {
            return '\0';
        }
        return null;
    }

    private RecordingDictTypeService countryDict(String... values)
    {
        return new RecordingDictTypeService(values);
    }

    private class RecordingDictTypeService implements InvocationHandler
    {
        private final List<SysDictData> countryRegionOptions = new ArrayList<>();

        private RecordingDictTypeService(String... values)
        {
            for (String value : values)
            {
                SysDictData data = new SysDictData();
                data.setDictType(PartnerSupport.COUNTRY_REGION_DICT_TYPE);
                data.setDictValue(value);
                data.setStatus(PartnerSupport.STATUS_NORMAL);
                countryRegionOptions.add(data);
            }
        }

        private ISysDictTypeService proxy()
        {
            return (ISysDictTypeService) Proxy.newProxyInstance(
                ISysDictTypeService.class.getClassLoader(), new Class<?>[] { ISysDictTypeService.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            String methodName = method.getName();
            if ("selectDictDataByType".equals(methodName)
                    && PartnerSupport.COUNTRY_REGION_DICT_TYPE.equals(args[0]))
            {
                return countryRegionOptions;
            }
            if ("toString".equals(methodName))
            {
                return "SellerServiceImplTestDictTypeService";
            }
            if ("hashCode".equals(methodName))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName))
            {
                return proxy == args[0];
            }
            return defaultValue(method.getReturnType());
        }
    }

    private void setField(Object target, String fieldName, Object value)
    {
        try
        {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Unable to set field " + fieldName, e);
        }
    }

    private static class RecordingDirectLoginSupport extends PortalDirectLoginSupport
    {
        private int callCount;

        private String portalType;

        private Long partnerId;

        private String partnerNo;

        private PortalAccount account;

        private String reason;

        private String webUrlConfigKey;

        private String fallbackWebUrl;

        private PortalDirectLoginToken tokenToConsume;

        private ServiceException consumeException;

        private int consumeCount;

        private String consumedPortalType;

        private String consumedTokenValue;

        @Override
        public PortalDirectLoginResult createToken(String portalType, Long partnerId, String partnerNo,
                PortalAccount account, String reason, String webUrlConfigKey, String fallbackWebUrl)
        {
            this.callCount++;
            this.portalType = portalType;
            this.partnerId = partnerId;
            this.partnerNo = partnerNo;
            this.account = account;
            this.reason = reason;
            this.webUrlConfigKey = webUrlConfigKey;
            this.fallbackWebUrl = fallbackWebUrl;

            PortalDirectLoginResult result = new PortalDirectLoginResult();
            result.setToken(portalType + "_test_token");
            result.setTicketId(1L);
            result.setLoginUrl(fallbackWebUrl == null ? "about:blank" : fallbackWebUrl);
            result.setExpireMinutes(30);
            result.setExpireTime(new Date());
            result.setAccountId(account.getAccountId());
            result.setUsername(account.getUserName());
            return result;
        }

        @Override
        public PortalDirectLoginToken consumeToken(String portalType, String token,
                java.util.function.Consumer<PortalDirectLoginToken> validator)
        {
            return consumeToken(portalType, token, validator, null);
        }

        @Override
        public PortalDirectLoginToken consumeToken(String portalType, String token,
                java.util.function.Consumer<PortalDirectLoginToken> validator,
                java.util.function.BiConsumer<PortalDirectLoginToken, ServiceException> failureAuditor)
        {
            consumeCount++;
            consumedPortalType = portalType;
            consumedTokenValue = token;
            if (consumeException != null)
            {
                if (failureAuditor != null && tokenToConsume != null)
                {
                    failureAuditor.accept(tokenToConsume, consumeException);
                }
                throw consumeException;
            }
            if (validator != null)
            {
                try
                {
                    validator.accept(tokenToConsume);
                }
                catch (ServiceException e)
                {
                    if (failureAuditor != null && tokenToConsume != null)
                    {
                        failureAuditor.accept(tokenToConsume, e);
                    }
                    throw e;
                }
            }
            return tokenToConsume;
        }
    }

    private static class RecordingPortalTokenSupport extends PortalTokenSupport
    {
        private int createLoginCount;

        private PortalLoginSession createdSession;

        private String deletedTerminal;

        private List<String> deletedTokenIds;

        private int deleteLoginTokensCallCount;

        private PortalLoginSession deletedSession;

        private int deleteLoginTokenCallCount;

        @Override
        public PortalLoginIssue createLogin(String terminal, Long subjectId, String subjectNo, PortalAccount account)
        {
            return createLogin(terminal, subjectId, subjectNo, account, null);
        }

        @Override
        public PortalLoginIssue createLogin(String terminal, Long subjectId, String subjectNo, PortalAccount account,
                PortalDirectLoginToken directLoginToken)
        {
            createLoginCount++;
            Date loginTime = new Date();
            PortalLoginSession session = new PortalLoginSession();
            session.setTokenId(terminal + "_login_token");
            session.setTerminal(terminal);
            session.setSubjectId(subjectId);
            session.setSubjectNo(subjectNo);
            session.setAccountId(account.getAccountId());
            session.setUserName(account.getUserName());
            session.setNickName(account.getNickName());
            session.setLoginIp("127.0.0.1");
            session.setLoginTime(loginTime);
            session.setExpireTime(new Date(loginTime.getTime() + 30L * 60L * 1000L));
            session.setStatus(PartnerSupport.STATUS_NORMAL);
            if (directLoginToken != null)
            {
                session.setDirectLogin(Boolean.TRUE);
                session.setDirectLoginTicketId(directLoginToken.getTicketId());
                session.setActingAdminId(directLoginToken.getActingAdminId());
                session.setActingAdminName(directLoginToken.getActingAdminName());
                session.setDirectLoginReason(directLoginToken.getDirectLoginReason());
            }
            createdSession = session;

            PortalLoginResult result = new PortalLoginResult();
            result.setToken(terminal + "_jwt");
            result.setTerminal(terminal);
            result.setSubjectId(subjectId);
            result.setSubjectNo(subjectNo);
            result.setAccountId(account.getAccountId());
            result.setUsername(account.getUserName());
            result.setNickName(account.getNickName());
            result.setExpireMinutes(30);
            result.setExpireTime(session.getExpireTime());

            PortalLoginIssue issue = new PortalLoginIssue();
            issue.setResult(result);
            issue.setSession(session);
            return issue;
        }

        @Override
        public PortalLoginLog buildLoginLog(Long subjectId, Long accountId, String userName, String status, String msg)
        {
            PortalLoginLog log = new PortalLoginLog();
            log.setSubjectId(subjectId);
            log.setAccountId(accountId);
            log.setUserName(userName);
            log.setIpaddr("127.0.0.1");
            log.setStatus(status);
            log.setMsg(msg);
            log.setDirectLogin(Boolean.FALSE);
            log.setLoginTime(new Date());
            return log;
        }

        @Override
        public PortalLoginLog buildDirectLoginLog(Long subjectId, Long accountId, String userName, String status,
                String msg, PortalDirectLoginToken directLoginToken)
        {
            PortalLoginLog log = buildLoginLog(subjectId, accountId, userName, status, msg);
            copyDirectLoginAudit(log, directLoginToken);
            return log;
        }

        @Override
        public PortalLoginLog buildDirectLoginLog(Long subjectId, Long accountId, String userName, String status,
                String msg, PortalLoginSession session)
        {
            PortalLoginLog log = buildLoginLog(subjectId, accountId, userName, status, msg);
            if (session != null && Boolean.TRUE.equals(session.getDirectLogin()))
            {
                log.setDirectLogin(Boolean.TRUE);
                log.setDirectLoginTicketId(session.getDirectLoginTicketId());
                log.setActingAdminId(session.getActingAdminId());
                log.setActingAdminName(session.getActingAdminName());
                log.setDirectLoginReason(session.getDirectLoginReason());
            }
            return log;
        }

        private void copyDirectLoginAudit(PortalLoginLog log, PortalDirectLoginToken directLoginToken)
        {
            if (directLoginToken == null)
            {
                return;
            }
            log.setDirectLogin(Boolean.TRUE);
            log.setDirectLoginTicketId(directLoginToken.getTicketId());
            log.setActingAdminId(directLoginToken.getActingAdminId());
            log.setActingAdminName(directLoginToken.getActingAdminName());
            log.setDirectLoginReason(directLoginToken.getDirectLoginReason());
        }

        @Override
        public void deleteLoginTokens(String terminal, List<String> tokenIds)
        {
            deleteLoginTokensCallCount++;
            deletedTerminal = terminal;
            deletedTokenIds = tokenIds;
        }

        @Override
        public void deleteLoginToken(PortalLoginSession session)
        {
            deleteLoginTokenCallCount++;
            deletedSession = session;
            super.deleteLoginToken(session);
        }
    }
}
