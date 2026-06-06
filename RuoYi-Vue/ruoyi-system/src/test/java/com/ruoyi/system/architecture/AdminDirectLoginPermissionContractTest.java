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
        assertContains(methodBody, "query.setTerminal(\"" + terminal + "\")", service, violations);
        assertContains(methodBody, "ticketMapper.selectPortalDirectLoginTicketList(query)", service, violations);
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
        assertAdminButtonGrantsStayAdminOnly(grantSeed, grantSql, violations);

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
    }

    private void assertAdminButtonGrantsStayAdminOnly(Path sqlFile, String sql, List<String> violations)
    {
        assertContains(sql, "select distinct r.role_id, child.menu_id", sqlFile, violations);
        assertContains(sql, "join sys_role_menu page_grant on page_grant.role_id = r.role_id", sqlFile, violations);
        assertContains(sql, "where r.role_key = 'admin'", sqlFile, violations);
        assertContains(sql, "and r.del_flag = '0'", sqlFile, violations);
        if (sql.contains("select distinct page_grant.role_id, child.menu_id"))
        {
            violations.add(sqlFile.getFileName()
                    + " must not inherit child button grants for every role with page access");
        }
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
        Path page = workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx");
        Path accountModal = workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx");
        Path auditModal = workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx");

        String pageSource = Files.readString(page, StandardCharsets.UTF_8);
        String accountModalSource = Files.readString(accountModal, StandardCharsets.UTF_8);
        String auditModalSource = Files.readString(auditModal, StandardCharsets.UTF_8);

        assertContains(pageSource, "const permPrefix = `${config.moduleKey}:admin`", page, violations);
        assertContains(pageSource, "access.hasPerms(`${permPrefix}:directLogin`)", page, violations);
        assertContains(pageSource, "access.hasPerms(`${permPrefix}:ticket:list`)", page, violations);
        assertContains(pageSource, "hidden={!hasAuditPermission}", page, violations);
        assertContains(accountModalSource,
                "access.hasPerms(`${permPrefix}:directLogin`) && config.services.directLoginAccount",
                accountModal, violations);
        assertContains(auditModalSource, "access.hasPerms(`${permPrefix}:ticket:list`)", auditModal, violations);
    }

    private void assertContains(String source, String expected, Path path, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(path.getFileName() + " must contain " + expected);
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
