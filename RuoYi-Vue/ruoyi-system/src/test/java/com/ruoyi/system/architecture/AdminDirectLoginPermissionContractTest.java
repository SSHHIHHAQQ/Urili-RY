package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class AdminDirectLoginPermissionContractTest
{
    private static final Pattern HANDLER_MAPPING = Pattern.compile("@(?:GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\b");
    private static final Pattern PUBLIC_METHOD = Pattern.compile("public\\s+[^\\(]+\\s+(\\w+)\\s*\\(");

    @Test
    public void adminDirectLoginAndTicketAuditMustUseDedicatedPermissions() throws IOException
    {
        Path workspaceRoot = findWorkspaceRoot();
        Path backendRoot = workspaceRoot.resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();

        assertTerminalControllerDirectLoginContract(backendRoot, "seller", "Seller", "sellerId", "accountId", violations);
        assertTerminalControllerDirectLoginContract(backendRoot, "buyer", "Buyer", "buyerId", "accountId", violations);
        assertTicketAuditTerminalScope(backendRoot, "seller", "Seller", violations);
        assertTicketAuditTerminalScope(backendRoot, "buyer", "Buyer", violations);
        assertTicketMapperCanFilterByTerminal(backendRoot, violations);
        assertSeedPermissions(backendRoot, violations);
        assertFrontendPermissionGates(workspaceRoot, violations);

        if (!violations.isEmpty())
        {
            fail("admin direct-login permissions must stay separated from generic admin access:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertTerminalControllerDirectLoginContract(Path backendRoot, String terminal, String classPrefix,
            String subjectIdPathVariable, String accountIdPathVariable, List<String> violations) throws IOException
    {
        Path controller = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal + "/controller/Admin"
                + classPrefix + "Controller.java");
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        String directLoginPerm = terminal + ":admin:directLogin";
        String ticketListPerm = terminal + ":admin:ticket:list";
        String subjectRoute = "@PostMapping(\"/{" + subjectIdPathVariable + "}/directLogin\")";
        String accountRoute = "@PostMapping(\"/{" + subjectIdPathVariable + "}/accounts/{" + accountIdPathVariable
                + "}/directLogin\")";

        assertHandlerAnnotations(controller, source, "directLogin", subjectRoute, directLoginPerm, true, violations);
        assertHandlerAnnotations(controller, source, "accountDirectLogin", accountRoute, directLoginPerm, true,
                violations);
        assertHandlerAnnotations(controller, source, "directLoginTickets",
                "@GetMapping(\"/directLoginTickets/list\")", ticketListPerm, false, violations);
    }

    private void assertTicketAuditTerminalScope(Path backendRoot, String terminal, String classPrefix,
            List<String> violations) throws IOException
    {
        Path service = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal + "/service/impl/"
                + classPrefix + "ServiceImpl.java");
        String source = Files.readString(service, StandardCharsets.UTF_8);
        String methodName = "select" + classPrefix + "DirectLoginTicketList";
        String methodBody = extractMethodBody(source, methodName);
        String prefix = service.getFileName() + "#" + methodName;
        if (methodBody.isEmpty())
        {
            violations.add(prefix + " must exist");
            return;
        }
        String normalizerName = "normalize" + classPrefix + "DirectLoginTicketScope";
        assertContains(methodBody, normalizerName + "(query)", service, violations);
        assertContains(methodBody, "query.setTerminal(\"" + terminal + "\")", service, violations);
        assertContains(methodBody, "ticketMapper.selectPortalDirectLoginTicketList(query)", service, violations);
        assertNotContains(methodBody, "ticketMapper.selectPortalDirectLoginTicketList(ticket)", service, violations);
        assertBefore(methodBody, normalizerName + "(query)", "query.setTerminal(\"" + terminal + "\")", prefix,
                violations);
        assertBefore(methodBody, "query.setTerminal(\"" + terminal + "\")",
                "ticketMapper.selectPortalDirectLoginTicketList(query)", prefix, violations);

        String normalizerBody = extractMethodBody(source, normalizerName);
        if (normalizerBody.isEmpty())
        {
            violations.add(service.getFileName() + "#" + normalizerName
                    + " must validate direct-login ticket account filters");
            return;
        }
        String subjectIdVariable = terminal + "Id";
        String accountIdVariable = terminal + "AccountId";
        String mapperName = terminal + "Mapper";
        assertContains(normalizerBody, "Long " + subjectIdVariable + " = ticket.getTargetSubjectId()", service,
                violations);
        assertContains(normalizerBody, "Long " + accountIdVariable + " = ticket.getTargetAccountId()", service,
                violations);
        assertContains(normalizerBody, "if (" + accountIdVariable + " != null)", service, violations);
        assertContains(normalizerBody, "if (" + subjectIdVariable + " == null)", service, violations);
        assertContains(normalizerBody, mapperName + ".select" + classPrefix + "AccountByIdAnd" + classPrefix
                + "Id(" + subjectIdVariable + ", " + accountIdVariable + ")", service, violations);
        assertContains(normalizerBody, "select" + classPrefix + "ById(" + subjectIdVariable + ")", service,
                violations);
        assertNotContains(normalizerBody, mapperName + ".select" + classPrefix + "AccountById(", service,
                violations);
    }

    private void assertTicketMapperCanFilterByTerminal(Path backendRoot, List<String> violations) throws IOException
    {
        Path mapper = backendRoot.resolve(
                "ruoyi-system/src/main/resources/mapper/system/PortalDirectLoginTicketMapper.xml");
        String source = Files.readString(mapper, StandardCharsets.UTF_8);
        assertContains(source, "<if test=\"terminal != null and terminal != ''\">", mapper, violations);
        assertContains(source, "and terminal = #{terminal}", mapper, violations);
    }

    private void assertHandlerAnnotations(Path controller, String source, String methodName, String expectedMapping,
            String expectedPermission, boolean mustLog, List<String> violations)
    {
        String annotations = extractHandlerAnnotations(source, methodName);
        String prefix = controller.getFileName() + "#" + methodName;
        if (annotations.isEmpty())
        {
            violations.add(prefix + " must be a mapped admin handler");
            return;
        }
        if (!annotations.contains(expectedMapping))
        {
            violations.add(prefix + " must keep route " + expectedMapping);
        }
        if (!annotations.contains("@PreAuthorize(\"@ss.hasPermi('" + expectedPermission + "')\")"))
        {
            violations.add(prefix + " must require " + expectedPermission);
        }
        if (mustLog && !annotations.contains("@Log"))
        {
            violations.add(prefix + " must write admin operation log");
        }
    }

    private void assertSeedPermissions(Path backendRoot, List<String> violations) throws IOException
    {
        Path seed = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String sql = Files.readString(seed, StandardCharsets.UTF_8);
        assertContains(sql, "seller:admin:directLogin", seed, violations);
        assertContains(sql, "seller:admin:ticket:list", seed, violations);
        assertContains(sql, "buyer:admin:directLogin", seed, violations);
        assertContains(sql, "buyer:admin:ticket:list", seed, violations);
        assertManagementSeedDoesNotGrantAdminRoles(seed, sql, violations);

        Path standaloneSeed = backendRoot.resolve("sql/20260606_admin_partner_page_direct_login_seed.sql");
        String standaloneSql = Files.readString(standaloneSeed, StandardCharsets.UTF_8);
        assertContains(standaloneSql, "PartnerManagement", standaloneSeed, violations);
        assertContains(standaloneSql, "seller:admin:list", standaloneSeed, violations);
        assertContains(standaloneSql, "buyer:admin:list", standaloneSeed, violations);
        assertContains(standaloneSql, "seller:admin:directLogin", standaloneSeed, violations);
        assertContains(standaloneSql, "buyer:admin:directLogin", standaloneSeed, violations);

        Path grantSeed = backendRoot.resolve("sql/20260606_admin_partner_role_menu_grant.sql");
        String grantSql = Files.readString(grantSeed, StandardCharsets.UTF_8);
        assertContains(grantSql, "set @confirm_admin_partner_role_menu_grant", grantSeed, violations);
        assertContains(grantSql, "call assert_admin_partner_role_menu_grant_confirmed();", grantSeed, violations);
        assertContains(grantSql, "call assert_admin_partner_menu_signature();", grantSeed, violations);
        assertAdminPartnerPageSignature(grantSeed, grantSql, violations);
        assertAdminPartnerGrantsStayInsideSignedMenuTree(grantSeed, grantSql, violations);
        assertAdminButtonGrantsStayAdminOnly(grantSeed, grantSql, violations);
        assertAdminPartnerButtonWhitelist(grantSeed, grantSql, violations);
        assertSubjectLevelOwnerResetPermissionsAreNotGranted(grantSeed, grantSql, violations);

        Path cleanupSeed = backendRoot.resolve("sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql");
        String cleanupSql = Files.readString(cleanupSeed, StandardCharsets.UTF_8);
        assertContains(cleanupSql, "set @confirm_admin_partner_non_admin_button_cleanup", cleanupSeed, violations);
        assertContains(cleanupSql, "set @admin_partner_button_cleanup_role_keys", cleanupSeed, violations);
        assertContains(cleanupSql, "find_in_set(r.role_key collate utf8mb4_unicode_ci",
                cleanupSeed, violations);
        assertContains(cleanupSql,
                "convert(@admin_partner_button_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0",
                cleanupSeed, violations);
        assertContains(cleanupSql, "delete child_grant", cleanupSeed, violations);
        assertContains(cleanupSql, "and r.role_key <> 'admin'", cleanupSeed, violations);
        assertAdminPartnerPageSignature(cleanupSeed, cleanupSql, violations);
        assertAdminPartnerButtonWhitelist(cleanupSeed, cleanupSql, violations);
        assertSubjectLevelOwnerResetPermissionsAreNotGranted(cleanupSeed, cleanupSql, violations);
    }

    private void assertAdminPartnerPageSignature(Path sqlFile, String sql, List<String> violations)
    {
        for (String expected : new String[] {
                "coalesce(path, '') = 'partner'",
                "coalesce(component, '') = ''",
                "coalesce(route_name, '') = 'PartnerManagement'",
                "coalesce(perms, '') = ''",
                "coalesce(path, '') = 'seller'",
                "coalesce(component, '') = 'Seller/index'",
                "coalesce(route_name, '') = 'Seller'",
                "coalesce(path, '') = 'buyer'",
                "coalesce(component, '') = 'Buyer/index'",
                "coalesce(route_name, '') = 'Buyer'",
                "partner sys_menu path/component/route signature does not match expected seller/buyer admin pages"
        })
        {
            assertContains(sql, expected, sqlFile, violations);
        }
    }

    private void assertAdminButtonGrantsStayAdminOnly(Path sqlFile, String sql, List<String> violations)
    {
        assertContains(sql, "select distinct r.role_id, child.menu_id", sqlFile, violations);
        assertContains(sql, "join sys_menu page_menu on page_menu.menu_id in (2011, 2012)", sqlFile, violations);
        assertContains(sql, "where r.role_key = 'admin'", sqlFile, violations);
        assertContains(sql, "and r.status = '0'", sqlFile, violations);
        assertContains(sql, "and r.del_flag = '0'", sqlFile, violations);
        assertContains(sql,
                "and substring_index(child.perms, ':', 1) = substring_index(page_menu.perms, ':', 1)",
                sqlFile, violations);
        if (sql.contains("join sys_role_menu page_grant on page_grant.role_id = r.role_id"))
        {
            violations.add(sqlFile.getFileName()
                    + " must calculate child grants from final signed partner pages, not existing page grants");
        }
        if (sql.contains("select distinct page_grant.role_id, child.menu_id"))
        {
            violations.add(sqlFile.getFileName()
                    + " must not inherit child button grants for every role with page access");
        }
    }

    private void assertAdminPartnerGrantsStayInsideSignedMenuTree(Path sqlFile, String sql, List<String> violations)
    {
        assertContains(sql, "join sys_menu m on m.menu_id in (2010, 2011, 2012)", sqlFile, violations);
        String initialGrant = extractSqlBetween(sql, "insert into sys_role_menu (role_id, menu_id)",
                "insert into sys_role_menu (role_id, menu_id)", 2);
        if (initialGrant.isEmpty())
        {
            violations.add(sqlFile.getFileName() + " must keep a dedicated admin page grant statement");
            return;
        }
        if (initialGrant.contains("m.perms like 'seller:admin:%'")
                || initialGrant.contains("m.perms like 'buyer:admin:%'"))
        {
            violations.add(sqlFile.getFileName()
                    + " must not grant every seller/buyer admin permission by prefix; grant the signed menu tree first");
        }
    }

    private void assertAdminPartnerButtonWhitelist(Path sqlFile, String sql, List<String> violations)
    {
        if (sql.contains("child.perms like 'seller:admin:%'")
                || sql.contains("child.perms like 'buyer:admin:%'"))
        {
            violations.add(sqlFile.getFileName()
                    + " must not select admin partner child buttons by terminal permission wildcard");
        }

        for (String expected : new String[] {
                "and substring_index(child.perms, ':', 1) = substring_index(page_menu.perms, ':', 1)",
                "and coalesce(child.path, '') = '#'",
                "and coalesce(child.component, '') = ''",
                "and coalesce(child.route_name, '') = ''",
                "child.menu_id in (",
                "child.perms in (",
                "2205",
                "2215",
                "2315",
                "2321",
                "2322",
                "2323",
                "seller:admin:directLogin",
                "buyer:admin:directLogin",
                "seller:admin:account:lock",
                "buyer:admin:account:lock"
        })
        {
            assertContains(sql, expected, sqlFile, violations);
        }
    }

    private void assertSubjectLevelOwnerResetPermissionsAreNotGranted(Path sqlFile, String sql, List<String> violations)
    {
        assertNotContains(sql, "seller:admin:resetPwd", sqlFile, violations);
        assertNotContains(sql, "buyer:admin:resetPwd", sqlFile, violations);
        assertNotContains(sql, "2204", sqlFile, violations);
        assertNotContains(sql, "2214", sqlFile, violations);
    }

    private void assertManagementSeedDoesNotGrantAdminRoles(Path sqlFile, String sql, List<String> violations)
    {
        if (sql.contains("insert into sys_role_menu"))
        {
            violations.add(sqlFile.getFileName()
                    + " must not grant sys_role_menu; use 20260606_admin_partner_role_menu_grant.sql");
        }
        if (sql.contains("where r.role_key = 'admin'"))
        {
            violations.add(sqlFile.getFileName()
                    + " must not grant admin role menus inside the base seller/buyer management seed");
        }
    }

    private void assertFrontendPermissionGates(Path workspaceRoot, List<String> violations) throws IOException
    {
        Path[] pages = new Path[] {
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx")
        };
        Path[] accountModals = new Path[] {
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx")
        };
        Path[] auditModals = new Path[] {
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx")
        };

        for (Path page : pages)
        {
            String pageSource = readRequired(page, violations);
            if (pageSource.isEmpty())
            {
                continue;
            }
            assertContains(pageSource, "const permPrefix = `${config.moduleKey}:admin`", page, violations);
            assertContains(pageSource, "access.hasPerms(`${permPrefix}:directLogin`)", page, violations);
            assertContains(pageSource, "access.hasPerms(`${permPrefix}:ticket:list`)", page, violations);
            assertContains(pageSource, "hasAuditPermission", page, violations);
        }
        for (Path accountModal : accountModals)
        {
            String accountModalSource = readRequired(accountModal, violations);
            if (accountModalSource.isEmpty())
            {
                continue;
            }
            assertContains(accountModalSource,
                    "access.hasPerms(`${permPrefix}:directLogin`) && config.services.directLoginAccount",
                    accountModal, violations);
        }
        for (Path auditModal : auditModals)
        {
            String auditModalSource = readRequired(auditModal, violations);
            if (auditModalSource.isEmpty())
            {
                continue;
            }
            assertContains(auditModalSource, "access.hasPerms(`${permPrefix}:ticket:list`)", auditModal, violations);
        }

        assertPureDefaultReExport(
                readRequired(workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.js"),
                        violations),
                "./PartnerManagementPage.tsx",
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.js"),
                violations);
        assertPureDefaultReExport(
                readRequired(workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAccountModal.js"),
                        violations),
                "./PartnerAccountModal.tsx",
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAccountModal.js"),
                violations);
        assertExactSource(
                readRequired(workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAuditModal.js"),
                        violations),
                "export { default, buildAuditParams } from './PartnerAuditModal.tsx';",
                workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAuditModal.js"),
                violations);
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

    private void assertPureDefaultReExport(String source, String target, Path path, List<String> violations)
    {
        assertExactSource(source, "export { default } from '" + target + "';", path, violations);
    }

    private void assertExactSource(String source, String expected, Path path, List<String> violations)
    {
        String normalized = source.replace("\r\n", "\n").trim();
        if (!expected.equals(normalized))
        {
            violations.add(path.getFileName() + " must equal " + expected);
        }
    }

    private void assertNotContains(String source, String forbidden, Path path, List<String> violations)
    {
        if (source.contains(forbidden))
        {
            violations.add(path.getFileName() + " must not contain " + forbidden);
        }
    }

    private void assertBefore(String source, String first, String second, String sourceName, List<String> violations)
    {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        if (firstIndex < 0 || secondIndex < 0)
        {
            return;
        }
        if (firstIndex > secondIndex)
        {
            violations.add(sourceName + " must check " + first + " before " + second);
        }
    }

    private String extractHandlerAnnotations(String source, String methodName)
    {
        String[] lines = source.split("\\R", -1);
        List<String> annotations = new ArrayList<>();

        for (int i = 0; i < lines.length; i++)
        {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("@") || annotationsNeedContinuation(annotations))
            {
                annotations.add(lines[i]);
                continue;
            }

            if (trimmed.startsWith("public ") && trimmed.contains("("))
            {
                String annotationText = String.join("\n", annotations);
                if (methodName.equals(extractMethodName(lines[i]))
                        && HANDLER_MAPPING.matcher(annotationText).find())
                {
                    return annotationText;
                }
                annotations.clear();
                continue;
            }

            if (!trimmed.isEmpty())
            {
                annotations.clear();
            }
        }

        return "";
    }

    private boolean annotationsNeedContinuation(List<String> annotations)
    {
        if (annotations.isEmpty())
        {
            return false;
        }
        int balance = 0;
        for (String annotation : annotations)
        {
            for (int i = 0; i < annotation.length(); i++)
            {
                char c = annotation.charAt(i);
                if (c == '(')
                {
                    balance++;
                }
                else if (c == ')')
                {
                    balance--;
                }
            }
        }
        return balance > 0;
    }

    private String extractMethodName(String methodLine)
    {
        Matcher matcher = PUBLIC_METHOD.matcher(methodLine);
        return matcher.find() ? matcher.group(1) : methodLine.trim();
    }

    private String extractMethodBody(String source, String methodName)
    {
        String signature = methodName + "(";
        int signatureIndex = source.indexOf(signature);
        if (signatureIndex < 0)
        {
            return "";
        }
        int bodyStart = source.indexOf('{', signatureIndex);
        if (bodyStart < 0)
        {
            return "";
        }
        int balance = 0;
        for (int i = bodyStart; i < source.length(); i++)
        {
            char c = source.charAt(i);
            if (c == '{')
            {
                balance++;
            }
            else if (c == '}')
            {
                balance--;
                if (balance == 0)
                {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        return "";
    }

    private String extractSqlBetween(String sql, String startNeedle, String endNeedle, int endOccurrence)
    {
        int start = sql.indexOf(startNeedle);
        if (start < 0)
        {
            return "";
        }

        int end = -1;
        int fromIndex = start + startNeedle.length();
        for (int i = 1; i < endOccurrence; i++)
        {
            end = sql.indexOf(endNeedle, fromIndex);
            if (end < 0)
            {
                return sql.substring(start);
            }
            fromIndex = end + endNeedle.length();
        }
        return sql.substring(start, end);
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
