package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class AdminAccountPermissionUiContractTest
{
    @Test
    public void sellerAndBuyerAccountActionsMustUseAccountScopedUiPermissions() throws IOException
    {
        Path workspaceRoot = findWorkspaceRoot();
        List<String> violations = new ArrayList<>();

        assertTerminalPageConfig(workspaceRoot, "Seller", "seller", violations);
        assertTerminalPageConfig(workspaceRoot, "Buyer", "buyer", violations);
        assertBalancePlaceholdersDoNotExposeFinanceSemantics(workspaceRoot, violations);
        assertNoSubjectLevelOwnerPasswordReset(workspaceRoot, violations);
        assertPartnerManagementPageGates(workspaceRoot, violations);
        assertPartnerAccountModalGates(workspaceRoot, violations);
        assertPartnerAuditModalGates(workspaceRoot, violations);

        if (!violations.isEmpty())
        {
            fail("admin account/session UI actions must be gated by terminal-scoped permissions:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertTerminalPageConfig(Path workspaceRoot, String pageName, String terminal, List<String> violations)
            throws IOException
    {
        Path[] pages = new Path[] {
                workspaceRoot.resolve("react-ui/src/pages/" + pageName + "/index.tsx"),
                workspaceRoot.resolve("react-ui/src/pages/" + pageName + "/index.js")
        };
        for (Path page : pages)
        {
            String source = readRequired(page, violations);
            if (source.isEmpty())
            {
                continue;
            }
            assertContains(source, "accountPermissions: {", page, violations);
            assertContains(source, "list: '" + terminal + ":admin:account:list'", page, violations);
            assertContains(source, "add: '" + terminal + ":admin:account:add'", page, violations);
            assertContains(source, "edit: '" + terminal + ":admin:account:edit'", page, violations);
            assertContains(source, "resetPwd: '" + terminal + ":admin:account:resetPwd'", page, violations);
            assertContains(source, "lock: '" + terminal + ":admin:account:lock'", page, violations);
            assertContains(source, "roleQuery: '" + terminal + ":admin:account:role:query'", page, violations);
            assertContains(source, "roleEdit: '" + terminal + ":admin:account:role:edit'", page, violations);
            assertContains(source, "listSubjectSessions: getAdmin" + pageName + "Sessions", page, violations);
            assertContains(source, "listAccountSessions: getAdmin" + pageName + "AccountSessions", page, violations);
            assertContains(source, "forceLogoutSubject: forceLogoutAdmin" + pageName + "Sessions", page, violations);
            assertContains(source, "forceLogoutAccount: forceLogoutAdmin" + pageName + "AccountSessions", page,
                    violations);
            assertContains(source, "resetAccountPassword: resetAdmin" + pageName + "AccountPassword", page,
                    violations);
            assertNotContains(source, "resetAccountDefaultPassword", page, violations);
            assertNotContains(source, "resetAdmin" + pageName + "AccountDefaultPassword", page, violations);
            assertNotContains(source, "resetOwnerPassword", page, violations);
            assertNotContains(source, "resetAdmin" + pageName + "OwnerPassword", page, violations);
            assertContains(source, "directLogin: createAdmin" + pageName + "DirectLogin", page, violations);
            assertContains(source, "directLoginAccount: createAdmin" + pageName + "AccountDirectLogin", page,
                    violations);
            assertContains(source, "listLoginLogs: getAdmin" + pageName + "LoginLogs", page, violations);
            assertContains(source, "listOperLogs: getAdmin" + pageName + "OperLogs", page, violations);
            assertContains(source, "listDirectLoginTickets: getAdmin" + pageName + "DirectLoginTickets", page,
                    violations);
        }
    }

    private void assertNoSubjectLevelOwnerPasswordReset(Path workspaceRoot, List<String> violations)
            throws IOException
    {
        Path[] files = new Path[] {
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx"),
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.js"),
                workspaceRoot.resolve("react-ui/src/pages/Seller/index.tsx"),
                workspaceRoot.resolve("react-ui/src/pages/Seller/index.js"),
                workspaceRoot.resolve("react-ui/src/pages/Buyer/index.tsx"),
                workspaceRoot.resolve("react-ui/src/pages/Buyer/index.js"),
                workspaceRoot.resolve("react-ui/src/services/seller/seller.ts"),
                workspaceRoot.resolve("react-ui/src/services/seller/seller.js"),
                workspaceRoot.resolve("react-ui/src/services/buyer/buyer.ts"),
                workspaceRoot.resolve("react-ui/src/services/buyer/buyer.js")
        };
        for (Path file : files)
        {
            String source = readRequired(file, violations);
            if (source.isEmpty())
            {
                continue;
            }
            assertNotContains(source, "resetOwnerPwd", file, violations);
            assertNotContains(source, "resetOwnerPassword", file, violations);
            assertNotContains(source, "/resetOwnerPwd", file, violations);
            assertNotContains(source, "resetAccountDefaultPassword", file, violations);
            assertNotContains(source, "resetAdminSellerAccountDefaultPassword", file, violations);
            assertNotContains(source, "resetAdminBuyerAccountDefaultPassword", file, violations);
            assertNotContains(source, "resetDefaultPwd", file, violations);
        }
    }

    private void assertPartnerManagementPageGates(Path workspaceRoot, List<String> violations) throws IOException
    {
        Path[] pages = new Path[] {
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx"),
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.js")
        };
        for (Path page : pages)
        {
            String source = readRequired(page, violations);
            if (source.isEmpty())
            {
                continue;
            }
            assertContains(source, "const accountPermissions = config.accountPermissions ??", page, violations);
            assertContains(source, "access.hasPerms(accountPermissions.list)", page, violations);
            assertContains(source, "PartnerSessionModal", page, violations);
            assertContains(source, "config.services.listSubjectSessions", page, violations);
            assertContains(source, "config.services.forceLogoutSubject", page, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:session:list`)", page, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:forceLogout`)", page, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:directLogin`)", page, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:dept:list`)", page, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:role:list`)", page, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:menu:list`)", page, violations);
            assertContains(source, "const hasAuditPermission = access.hasPerms(`${permPrefix}:loginLog:list`)", page,
                    violations);
            assertContains(source, "|| access.hasPerms(`${permPrefix}:operLog:list`)", page, violations);
            assertContains(source, "|| access.hasPerms(`${permPrefix}:ticket:list`)", page, violations);
        }
    }

    private void assertBalancePlaceholdersDoNotExposeFinanceSemantics(Path workspaceRoot, List<String> violations)
            throws IOException
    {
        Path[] partnerPages = new Path[] {
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx"),
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.js")
        };
        for (Path page : partnerPages)
        {
            String source = readRequired(page, violations);
            if (source.isEmpty())
            {
                continue;
            }
            assertContains(source, "renderBalancePlaceholder", page, violations);
            assertNotContains(source, "formatBalance", page, violations);
            assertNotContains(source, "BalanceRangeInput", page, violations);
            assertNotContains(source, "params[balanceMin]", page, violations);
            assertNotContains(source, "params[balanceMax]", page, violations);
            assertContainsOneOf(source, new String[] { "充值能力", "\\u5145\\u503C\\u80FD\\u529B" }, page,
                    violations);
            assertContainsOneOf(source, new String[] { "规划中", "\\u89C4\\u5212\\u4E2D" }, page, violations);
        }

        Path[] terminalPages = new Path[] {
                workspaceRoot.resolve("react-ui/src/pages/Seller/index.tsx"),
                workspaceRoot.resolve("react-ui/src/pages/Seller/index.js"),
                workspaceRoot.resolve("react-ui/src/pages/Buyer/index.tsx"),
                workspaceRoot.resolve("react-ui/src/pages/Buyer/index.js")
        };
        for (Path page : terminalPages)
        {
            String source = readRequired(page, violations);
            if (source.isEmpty())
            {
                continue;
            }
            assertContains(source, "（占位）", page, violations);
            assertNotContains(source, "searchFieldCount: 8", page, violations);
        }

        Path sellerMapper = workspaceRoot.resolve("RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml");
        Path buyerMapper = workspaceRoot.resolve("RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml");
        for (Path mapper : new Path[] { sellerMapper, buyerMapper })
        {
            String source = readRequired(mapper, violations);
            if (source.isEmpty())
            {
                continue;
            }
            assertNotContains(source, "account_balance", mapper, violations);
            assertNotContains(source, "balance_currency", mapper, violations);
            assertNotContains(source, "cast(0.00 as decimal(18,2))", mapper, violations);
            assertNotContains(source, "#{params.balanceMin}", mapper, violations);
            assertNotContains(source, "#{params.balanceMax}", mapper, violations);
        }

        Path partnerProfile = workspaceRoot.resolve(
                "RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PartnerProfile.java");
        String partnerProfileSource = readRequired(partnerProfile, violations);
        if (!partnerProfileSource.isEmpty())
        {
            assertNotContains(partnerProfileSource, "accountBalance", partnerProfile, violations);
            assertNotContains(partnerProfileSource, "balanceCurrency", partnerProfile, violations);
        }

        Path subjectProfile = workspaceRoot.resolve(
                "RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalSubjectProfile.java");
        String profileSource = readRequired(subjectProfile, violations);
        if (!profileSource.isEmpty())
        {
            assertNotContains(profileSource, "accountBalance", subjectProfile, violations);
            assertNotContains(profileSource, "balanceCurrency", subjectProfile, violations);
        }
    }

    private void assertPartnerAccountModalGates(Path workspaceRoot, List<String> violations) throws IOException
    {
        Path[] modals = new Path[] {
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx"),
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAccountModal.js")
        };
        for (Path modal : modals)
        {
            String source = readRequired(modal, violations);
            if (source.isEmpty())
            {
                continue;
            }
            assertContains(source, "const accountPermissions = config.accountPermissions ??", modal, violations);
            assertContains(source, "const canQueryRole = access.hasPerms(`${permPrefix}:role:query`)", modal,
                    violations);
            assertContains(source, "const canAssignAccountRoles = canQueryRole", modal, violations);
            assertContains(source, "access.hasPerms(accountPermissions.roleQuery)", modal, violations);
            assertContains(source, "access.hasPerms(accountPermissions.roleEdit)", modal, violations);
            assertContains(source, "access.hasPerms(accountPermissions.resetPwd)", modal, violations);
            assertContains(source, "config.services.resetAccountPassword(partnerId, accountId", modal, violations);
            assertContains(source, "resetPasswordForm.validateFields()", modal, violations);
            assertNotContains(source, "config.services.resetAccountDefaultPassword(partnerId, accountId)", modal,
                    violations);
            assertContains(source, "access.hasPerms(accountPermissions.lock", modal, violations);
            assertContains(source, "access.hasPerms(accountPermissions.edit)", modal, violations);
            assertContains(source, "access.hasPerms(accountPermissions.add)", modal, violations);
            assertContains(source, "const canQueryDept = access.hasPerms(`${permPrefix}:dept:query`)", modal,
                    violations);
            assertContains(source, "if (!partnerId || !canQueryDept)", modal, violations);
            assertContainsOneOf(source, new String[] { "disabled={!canQueryDept}", "disabled: !canQueryDept" }, modal,
                    violations);
            assertContainsOneOf(source,
                    new String[] { "placeholder={canQueryDept ? '请选择' : '无部门查询权限'}",
                            "placeholder: canQueryDept ? '\\u8BF7\\u9009\\u62E9' : '\\u65E0\\u90E8\\u95E8\\u67E5\\u8BE2\\u6743\\u9650'" },
                    modal, violations);
            assertContains(source, "PartnerSessionModal", modal, violations);
            assertContains(source, "config.services.listAccountSessions", modal, violations);
            assertContains(source, "config.services.forceLogoutAccount", modal, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:session:list`)", modal, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:forceLogout`)", modal, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:directLogin`) && config.services.directLoginAccount",
                    modal, violations);
            assertContains(source, "PartnerAuditModal", modal, violations);
            assertContains(source, "const canViewAccountAudit = access.hasPerms(`${permPrefix}:loginLog:list`)",
                    modal, violations);
            assertContains(source, "|| access.hasPerms(`${permPrefix}:operLog:list`)", modal, violations);
            assertContains(source, "|| access.hasPerms(`${permPrefix}:ticket:list`)", modal, violations);
            assertContainsOneOf(source, new String[] { "key: 'audit', label: '审计'",
                    "key: 'audit', label: '\\u5BA1\\u8BA1'" }, modal, violations);
            assertContains(source, "setAuditAccount(record)", modal, violations);
            assertContains(source, "setAuditModalOpen(true)", modal, violations);
            assertContainsOneOf(source, new String[] { "account={auditAccount}", "account: auditAccount" }, modal,
                    violations);
        }
    }

    private void assertPartnerAuditModalGates(Path workspaceRoot, List<String> violations) throws IOException
    {
        Path[] modals = new Path[] {
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx"),
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAuditModal.js")
        };
        for (Path modal : modals)
        {
            String source = readRequired(modal, violations);
            if (source.isEmpty())
            {
                continue;
            }
            assertContains(source, "access.hasPerms(`${permPrefix}:loginLog:list`)", modal, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:operLog:list`)", modal, violations);
            assertContains(source, "access.hasPerms(`${permPrefix}:ticket:list`)", modal, violations);
            assertContains(source, "next[accountField] = accountId", modal, violations);
            assertContains(source,
                    "request(buildAuditParams(rest, current, pageSize, partnerId, accountId, subjectField, accountField))",
                    modal, violations);
            assertContains(source, "dataIndex: 'accountId'", modal, violations);
            assertContains(source, "render: (_, record) => renderCompactText(record.accountId)", modal, violations);
            assertContains(source, "dataIndex: 'targetAccountId'", modal, violations);
            assertContains(source, "render: (_, record) => renderCompactText(record.targetAccountId)", modal,
                    violations);
            assertContains(source, "dataIndex: 'actingAdminName'", modal, violations);
            assertContains(source, "render: (_, record) => renderCompactText(record.actingAdminName)", modal,
                    violations);
            assertContains(source, "renderDetailText(record.actingAdminId)", modal, violations);
            assertContains(source, "renderDetailText(record.reason)", modal, violations);
            assertContainsOneOf(source, new String[] { "暂无审计权限",
                    "\\u6682\\u65E0\\u5BA1\\u8BA1\\u6743\\u9650" }, modal, violations);
            assertNotContains(source, "tokenHash", modal, violations);
            assertNotContains(source, "directLoginToken", modal, violations);
            assertNotContains(source, "loginUrl", modal, violations);
            assertNotContains(source, "renderDetailText(record.operParam)", modal, violations);
            assertNotContains(source, "renderDetailText(record.jsonResult)", modal, violations);
        }
    }

    private String readRequired(Path path, List<String> violations) throws IOException
    {
        if (!Files.exists(path))
        {
            violations.add(path.getFileName() + " must exist");
            return "";
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private void assertContains(String source, String expected, Path path, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(path.getFileName() + " must contain " + expected);
        }
    }

    private void assertContainsOneOf(String source, String[] expectedValues, Path path, List<String> violations)
    {
        for (String expected : expectedValues)
        {
            if (source.contains(expected))
            {
                return;
            }
        }
        violations.add(path.getFileName() + " must contain one of " + String.join(", ", expectedValues));
    }

    private void assertNotContains(String source, String forbidden, Path path, List<String> violations)
    {
        if (source.contains(forbidden))
        {
            violations.add(path.getFileName() + " must not contain " + forbidden);
        }
    }

    private Path findWorkspaceRoot()
    {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path[] candidates = new Path[] {
                cwd,
                cwd.resolve("..").normalize(),
                cwd.resolve("../..").normalize()
        };

        for (Path candidate : candidates)
        {
            if (Files.isDirectory(candidate.resolve("RuoYi-Vue"))
                    && Files.isDirectory(candidate.resolve("react-ui")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate E:\\Urili-Ruoyi workspace root from " + cwd);
    }
}
