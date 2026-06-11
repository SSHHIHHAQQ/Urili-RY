package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class PortalSelfServiceSurfaceContractTest
{
    private static final Pattern METHOD_NAME = Pattern.compile("(?:public|private)\\s+[^\\(]+\\s+(\\w+)\\s*\\(");

    private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"]+)\"");

    private static final List<String> FORBIDDEN_LOGIN_QUERY_SETTERS = Arrays.asList(
            "setInfoId(",
            "setLoginLocation(",
            "setBrowser(",
            "setOs(",
            "setMsg(",
            "setDirectLogin(",
            "setDirectLoginTicketId(",
            "setActingAdminId(",
            "setActingAdminName(",
            "setDirectLoginReason(",
            "setLoginTime("
    );

    private static final List<String> FORBIDDEN_OPER_QUERY_SETTERS = Arrays.asList(
            "setOperId(",
            "setBusinessType(",
            "setMethod(",
            "setRequestMethod(",
            "setOperUrl(",
            "setOperIp(",
            "setOperLocation(",
            "setOperParam(",
            "setJsonResult(",
            "setDirectLogin(",
            "setDirectLoginTicketId(",
            "setActingAdminId(",
            "setActingAdminName(",
            "setDirectLoginReason(",
            "setErrorMsg(",
            "setOperTime(",
            "setCostTime("
    );

    private static final List<String> FORBIDDEN_LOGIN_PROFILE_SETTERS = Arrays.asList(
            "setSubjectId(",
            "setAccountId(",
            "setDirectLoginTicketId(",
            "setActingAdminId(",
            "setActingAdminName(",
            "setDirectLoginReason("
    );

    private static final List<String> FORBIDDEN_OPER_PROFILE_SETTERS = Arrays.asList(
            "setSubjectId(",
            "setAccountId(",
            "setOperParam(",
            "setJsonResult(",
            "setDirectLoginTicketId(",
            "setActingAdminId(",
            "setActingAdminName(",
            "setDirectLoginReason("
    );

    private static final List<String> FORBIDDEN_SESSION_PROFILE_SETTERS = Arrays.asList(
            "setTerminal(",
            "setSubjectId(",
            "setAccountId(",
            "setTokenId(",
            "setDirectLogin(",
            "setDirectLoginTicketId(",
            "setActingAdminId(",
            "setActingAdminName(",
            "setDirectLoginReason("
    );

    @Test
    public void portalSelfServiceMustReturnVisibleProfilesAndWhitelistAuditQueries() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        assertPortalController(backendRoot, "seller", violations);
        assertPortalController(backendRoot, "buyer", violations);
        assertPortalService(backendRoot, "seller", violations);
        assertPortalService(backendRoot, "buyer", violations);
        assertPortalPermissionService(backendRoot, "seller", violations);
        assertPortalPermissionService(backendRoot, "buyer", violations);

        if (!violations.isEmpty())
        {
            fail("portal self-service endpoints must keep visible DTO and audit query contracts:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertPortalController(Path backendRoot, String terminal, List<String> violations) throws IOException
    {
        Path controller = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal
                + "/controller/" + capitalize(terminal) + "PortalController.java");
        List<MethodBody> methods = extractMethods(Files.readString(controller, StandardCharsets.UTF_8));

        requireBodyContains(controller, methods, "profile", "return success(buildProfile(", violations);
        requireBodyContains(controller, methods, "accountProfile", "return success(buildAccountProfile(", violations);
        requireBodyContains(controller, methods, "accounts", "profiles.add(buildAccountProfile(account));", violations);
        requireBodyContains(controller, methods, "depts", "profiles.add(buildDeptProfile(dept));", violations);
        requireBodyContains(controller, methods, "roles", "profiles.add(buildRoleProfile(role));", violations);
        requireBodyContains(controller, methods, "accountLoginLogs",
                "return getDataTable(" + terminal + "Service.select" + capitalize(terminal)
                        + "OwnLoginLogList(session, log));", violations);
        requireBodyContains(controller, methods, "accountOperLogs",
                "return getDataTable(" + terminal + "Service.select" + capitalize(terminal)
                        + "OwnOperLogList(session, log));", violations);
        requireBodyContains(controller, methods, "accountSessions",
                "return getDataTable(" + terminal + "Service.select" + capitalize(terminal)
                        + "OwnSessionList(session));", violations);

        String source = Files.readString(controller, StandardCharsets.UTF_8);
        assertPortalSelfManagementControllerSurface(controller, source, terminal, violations);
        requireSourceAbsent(controller, source, "getOwnLoginLogDataTable(", violations);
        requireSourceAbsent(controller, source, "getOwnOperLogDataTable(", violations);
        requireSourceAbsent(controller, source, "buildOwnLoginLogProfile(", violations);
        requireSourceAbsent(controller, source, "buildOwnOperLogProfile(", violations);
        requireSourceAbsent(controller, source, "buildOwnSessionProfile(", violations);
    }

    private void assertPortalSelfManagementControllerSurface(Path controller, String source, String terminal,
            List<String> violations)
    {
        String terminalCap = capitalize(terminal);
        String serviceName = terminal + "Service";
        String accountIdSetter = "set" + terminalCap + "AccountId(targetAccountId);";

        assertExactStringLiteralBlock(controller, source,
                "private static final Set<String> PORTAL_SELF_MANAGEMENT_PERMS", "));",
                portalSelfManagementPermissions(terminal), violations);

        for (String expected : Arrays.asList(
                "private static final Set<String> PORTAL_SELF_MANAGEMENT_PERMS",
                "\"" + terminal + ":account:add\"",
                "\"" + terminal + ":account:edit\"",
                "\"" + terminal + ":account:role:query\"",
                "\"" + terminal + ":account:role:edit\"",
                "@PortalPreAuthorize(terminal = \"" + terminal + "\", hasPermi = {",
                "\"" + terminal + ":account:role:query\", \"" + terminal + ":role:list\" })",
                "\"" + terminal + ":account:role:edit\", \"" + terminal + ":account:role:query\", \""
                        + terminal + ":role:list\" })",
                "\"" + terminal + ":dept:query\"",
                "\"" + terminal + ":dept:add\"",
                "\"" + terminal + ":dept:edit\"",
                "\"" + terminal + ":dept:remove\"",
                "\"" + terminal + ":role:query\"",
                "\"" + terminal + ":role:add\"",
                "\"" + terminal + ":role:edit\"",
                "\"" + terminal + ":role:remove\"",
                serviceName + ".insert" + terminalCap + "Account(session.getSubjectId(), account)",
                serviceName + ".update" + terminalCap + "Account(session.getSubjectId(), account)",
                "permissionService.selectRoleAll(session.getSubjectId())",
                "permissionService.selectAccountRoleIds(session.getSubjectId(), targetAccountId)",
                "permissionService.assignAccountRoles(session.getSubjectId(), targetAccountId,",
                "deptService.selectDeptById(session.getSubjectId(), deptId)",
                "deptService.buildDeptTreeSelect(session.getSubjectId(), dept)",
                "deptService.insertDept(session.getSubjectId(), dept)",
                "deptService.updateDept(session.getSubjectId(), dept)",
                "deptService.deleteDeptById(session.getSubjectId(), deptId)",
                "permissionService.selectRoleById(session.getSubjectId(), roleId)",
                "selectPortalSelfManagementMenuIds(session.getSubjectId(), roleId, selfManagementMenus)",
                "permissionService.selectMenuIdsByRoleId(" + terminal + "Id, roleId)",
                "permissionService.insertRole(session.getSubjectId(), role)",
                "permissionService.updateRole(session.getSubjectId(), role)",
                "permissionService.deleteRoleByIds(session.getSubjectId(), roleIds)",
                accountIdSetter,
                "if (menuId == null || menuId <= 0)",
                "if (!seenMenuIds.add(menuId))",
                "PortalPermissionSupport.assertReadableTerminalMenu(menu, \"" + terminal + "\");",
                "if (!PORTAL_SELF_MANAGEMENT_PERMS.contains(StringUtils.trimToEmpty(menu.getPerms())))",
                "if (allowedMenuIds.contains(menuId))",
                "String perms = menu == null ? \"\" : StringUtils.trimToEmpty(menu.getPerms());",
                "if (PORTAL_SELF_MANAGEMENT_PERMS.contains(perms))",
                "PortalPermissionSupport.buildMenuTreeSelect(selectPortalSelfManagementMenus())"
        ))
        {
            requireSourceContains(controller, source, expected, violations);
        }

        requireSourceContains(controller, source, "account.set" + terminalCap + "Id(null);", violations);
        requireSourceAbsent(controller, source, "@RequestParam(\"" + terminal + "Id\")", violations);
        requireSourceAbsent(controller, source, "@PathVariable(\"" + terminal + "Id\")", violations);
        requireSourceAbsent(controller, source, "@RequestParam(\"subjectId\")", violations);
        requireSourceAbsent(controller, source, "@PathVariable(\"subjectId\")", violations);
        requireSourceAbsent(controller, source, "@RequestParam(\"accountId\")", violations);
        requireSourceAbsent(controller, source, "@RequestParam(\"" + terminal + "AccountId\")", violations);
    }

    private void assertPortalService(Path backendRoot, String terminal, List<String> violations) throws IOException
    {
        Path service = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal
                + "/service/impl/" + capitalize(terminal) + "ServiceImpl.java");
        String serviceSource = Files.readString(service, StandardCharsets.UTF_8);
        List<MethodBody> methods = extractMethods(serviceSource);
        String terminalCap = capitalize(terminal);
        Path serviceInterface = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal
                + "/service/I" + terminalCap + "Service.java");
        String interfaceSource = Files.readString(serviceInterface, StandardCharsets.UTF_8);

        MethodBody loginQuery = findMethod(methods, "build" + terminalCap + "OwnLoginLogQuery");
        MethodBody operQuery = findMethod(methods, "build" + terminalCap + "OwnOperLogQuery");

        requireSourceContains(serviceInterface, interfaceSource,
                "List<PortalOwnLoginLogProfile> select" + terminalCap + "OwnLoginLogList", violations);
        requireSourceContains(serviceInterface, interfaceSource,
                "List<PortalOwnOperLogProfile> select" + terminalCap + "OwnOperLogList", violations);
        requireSourceContains(serviceInterface, interfaceSource,
                "List<PortalOwnSessionProfile> select" + terminalCap + "OwnSessionList", violations);
        requireSourceContains(service, serviceSource,
                "public List<PortalOwnLoginLogProfile> select" + terminalCap + "OwnLoginLogList", violations);
        requireSourceContains(service, serviceSource,
                "public List<PortalOwnOperLogProfile> select" + terminalCap + "OwnOperLogList", violations);
        requireSourceContains(service, serviceSource,
                "public List<PortalOwnSessionProfile> select" + terminalCap + "OwnSessionList", violations);

        assertOwnerDefaultSelfManagementPerms(service, serviceSource, terminal, violations);

        requireBodyContains(service, methods, "select" + terminalCap + "OwnLoginLogList",
                "newOwnLoginLogProfileList(logs)", violations);
        requireBodyContains(service, methods, "select" + terminalCap + "OwnLoginLogList",
                "profiles.add(buildOwnLoginLogProfile(item));", violations);
        requireBodyContains(service, methods, "select" + terminalCap + "OwnOperLogList",
                "newOwnOperLogProfileList(logs)", violations);
        requireBodyContains(service, methods, "select" + terminalCap + "OwnOperLogList",
                "profiles.add(buildOwnOperLogProfile(item));", violations);
        requireBodyContains(service, methods, "select" + terminalCap + "OwnSessionList",
                "newOwnSessionProfileList(sessions)", violations);
        requireBodyContains(service, methods, "select" + terminalCap + "OwnSessionList",
                "profiles.add(buildOwnSessionProfile(profile));", violations);
        requireBodyContains(service, methods, "newOwnLoginLogProfileList", "copyPageMetadata(source, result);",
                violations);
        requireBodyContains(service, methods, "newOwnOperLogProfileList", "copyPageMetadata(source, result);",
                violations);
        requireBodyContains(service, methods, "newOwnSessionProfileList", "copyPageMetadata(source, result);",
                violations);

        requireBodyContains(service, methods, "buildOwnLoginLogProfile", "new PortalOwnLoginLogProfile()",
                violations);
        requireBodyContains(service, methods, "buildOwnOperLogProfile", "new PortalOwnOperLogProfile()",
                violations);
        requireBodyContains(service, methods, "buildOwnSessionProfile", "new PortalOwnSessionProfile()",
                violations);
        forbid(service, findMethod(methods, "buildOwnLoginLogProfile"), FORBIDDEN_LOGIN_PROFILE_SETTERS,
                violations);
        forbid(service, findMethod(methods, "buildOwnOperLogProfile"), FORBIDDEN_OPER_PROFILE_SETTERS,
                violations);
        forbid(service, findMethod(methods, "buildOwnSessionProfile"), FORBIDDEN_SESSION_PROFILE_SETTERS,
                violations);

        if (loginQuery == null)
        {
            violations.add(relative(service) + "#build" + terminalCap + "OwnLoginLogQuery must exist");
        }
        else
        {
            requireContains(service, loginQuery, "query.setSubjectId(session.getSubjectId());", violations);
            requireContains(service, loginQuery, "query.setAccountId(session.getAccountId());", violations);
            requireContains(service, loginQuery, "copyTimeRangeParams(log.getParams())", violations);
            forbid(service, loginQuery, FORBIDDEN_LOGIN_QUERY_SETTERS, violations);
        }

        if (operQuery == null)
        {
            violations.add(relative(service) + "#build" + terminalCap + "OwnOperLogQuery must exist");
        }
        else
        {
            requireContains(service, operQuery, "query.setSubjectId(session.getSubjectId());", violations);
            requireContains(service, operQuery, "query.setAccountId(session.getAccountId());", violations);
            requireContains(service, operQuery, "copyTimeRangeParams(log.getParams())", violations);
            forbid(service, operQuery, FORBIDDEN_OPER_QUERY_SETTERS, violations);
        }
    }

    private void assertOwnerDefaultSelfManagementPerms(Path service, String serviceSource, String terminal,
            List<String> violations)
    {
        assertExactStringLiteralBlock(service, serviceSource,
                "private static final String[] DEFAULT_OWNER_PERMS", "};",
                portalSelfManagementPermissions(terminal), violations);
    }

    private void assertPortalPermissionService(Path backendRoot, String terminal, List<String> violations)
            throws IOException
    {
        Path service = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal
                + "/service/impl/" + capitalize(terminal) + "PortalPermissionServiceImpl.java");
        String source = Files.readString(service, StandardCharsets.UTF_8);

        assertExactStringLiteralBlock(service, source,
                "private static final Set<String> PORTAL_SELF_MANAGEMENT_PERMS", "));",
                portalSelfManagementPermissions(terminal), violations);
        requireSourceContains(service, source, "splitSelfManagementPermissions(permissionMapper.select"
                + capitalize(terminal) + "AccountPermissions(", violations);
        requireSourceContains(service, source, "if (PORTAL_SELF_MANAGEMENT_PERMS.contains(permission))", violations);
        requireSourceContains(service, source, "permissions.add(permission);", violations);
        requireSourceContains(service, source, "return selectPortalPermissionInfo(session).getPermissions();",
                violations);
        requireSourceContains(service, source, "permission.contains(\"*\")", violations);
        requireSourceContains(service, source, "List<PortalMenu> selfManagementMenus = new ArrayList<>();",
                violations);
        requireSourceContains(service, source,
                "if (PORTAL_SELF_MANAGEMENT_PERMS.contains(StringUtils.trimToEmpty(menu.getPerms())))", violations);
        requireSourceContains(service, source, "selfManagementMenus.add(menu);", violations);
        requireSourceContains(service, source, "return PortalPermissionSupport.buildMenuTree(selfManagementMenus);",
                violations);
        requireSourceContains(service, source, "Long[] ids = normalizeRoleIds(roleIds);", violations);
        requireSourceContains(service, source, "if (roleId == null || roleId <= 0)", violations);
        requireSourceContains(service, source, "if (!values.add(roleId))", violations);
        requireSourceContains(service, source, "permissionMapper.count" + capitalize(terminal)
                + "RolesByIds(" + terminal + "Id, ids) != ids.length", violations);
        requireSourceAbsent(service, source, "PortalPermissionSupport.sanitizeIds(roleIds)", violations);
        requireSourceContains(service, source, "Long[] menuIds = normalizeRoleMenuIds(role.getMenuIds());",
                violations);
        requireSourceContains(service, source, "role.setMenuIds(menuIds);", violations);
        requireSourceContains(service, source, "if (menuId == null || menuId <= 0)", violations);
        requireSourceContains(service, source, "if (!values.add(menuId))", violations);
        requireSourceContains(service, source, "permissionMapper.count" + capitalize(terminal)
                + "MenusByIds(menuIds) != menuIds.length", violations);
        requireSourceContains(service, source, "assertRoleMenuSelfManagement(menu);", violations);
        requireSourceContains(service, source, "private void assertRoleMenuSelfManagement(PortalMenu menu)",
                violations);
        requireSourceContains(service, source,
                "if (!PORTAL_SELF_MANAGEMENT_PERMS.contains(StringUtils.trimToEmpty(menu.getPerms())))",
                violations);
    }

    private List<String> portalSelfManagementPermissions(String terminal)
    {
        return Arrays.asList(
                terminal + ":portal:home",
                terminal + ":account:list",
                terminal + ":account:add",
                terminal + ":account:edit",
                terminal + ":account:role:query",
                terminal + ":account:role:edit",
                terminal + ":account:loginLog:list",
                terminal + ":account:operLog:list",
                terminal + ":account:session:list",
                terminal + ":dept:list",
                terminal + ":dept:query",
                terminal + ":dept:add",
                terminal + ":dept:edit",
                terminal + ":dept:remove",
                terminal + ":role:list",
                terminal + ":role:query",
                terminal + ":role:add",
                terminal + ":role:edit",
                terminal + ":role:remove"
        );
    }

    private void assertExactStringLiteralBlock(Path path, String source, String startMarker, String endMarker,
            List<String> expected, List<String> violations)
    {
        int start = source.indexOf(startMarker);
        if (start < 0)
        {
            violations.add(relative(path) + " must contain " + startMarker);
            return;
        }
        int end = source.indexOf(endMarker, start);
        if (end < 0)
        {
            violations.add(relative(path) + " must close string literal block " + startMarker + " with "
                    + endMarker);
            return;
        }

        String block = source.substring(start, end + endMarker.length());
        List<String> actual = new ArrayList<>();
        Matcher matcher = STRING_LITERAL.matcher(block);
        while (matcher.find())
        {
            actual.add(matcher.group(1));
        }

        if (!expected.equals(actual))
        {
            violations.add(relative(path) + " block " + startMarker
                    + " must equal self-management permissions " + expected + " but was " + actual);
        }
    }

    private List<MethodBody> extractMethods(String source)
    {
        String[] lines = source.split("\\R", -1);
        List<MethodBody> methods = new ArrayList<>();

        for (int i = 0; i < lines.length; i++)
        {
            String trimmed = lines[i].trim();
            if ((trimmed.startsWith("public ") || trimmed.startsWith("private ")) && trimmed.contains("("))
            {
                methods.add(new MethodBody(extractMethodName(lines[i]), collectMethodBody(lines, i)));
            }
        }

        return methods;
    }

    private String collectMethodBody(String[] lines, int methodLine)
    {
        StringBuilder body = new StringBuilder();
        int braceBalance = 0;
        boolean started = false;

        for (int i = methodLine; i < lines.length; i++)
        {
            String line = lines[i];
            body.append(line).append('\n');
            for (int j = 0; j < line.length(); j++)
            {
                char c = line.charAt(j);
                if (c == '{')
                {
                    braceBalance++;
                    started = true;
                }
                else if (c == '}')
                {
                    braceBalance--;
                }
            }
            if (started && braceBalance == 0)
            {
                break;
            }
        }

        return body.toString();
    }

    private String extractMethodName(String methodLine)
    {
        Matcher matcher = METHOD_NAME.matcher(methodLine);
        return matcher.find() ? matcher.group(1) : methodLine.trim();
    }

    private void requireBodyContains(Path path, List<MethodBody> methods, String methodName, String expected,
            List<String> violations)
    {
        MethodBody method = findMethod(methods, methodName);
        if (method == null)
        {
            violations.add(relative(path) + "#" + methodName + " must exist");
            return;
        }
        requireContains(path, method, expected, violations);
    }

    private void requireContains(Path path, MethodBody method, String expected, List<String> violations)
    {
        if (!method.body.contains(expected))
        {
            violations.add(relative(path) + "#" + method.name + " must contain " + expected);
        }
    }

    private void requireSourceContains(Path path, String source, String expected, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(relative(path) + " must contain " + expected);
        }
    }

    private void requireSourceAbsent(Path path, String source, String forbidden, List<String> violations)
    {
        if (source.contains(forbidden))
        {
            violations.add(relative(path) + " must not keep portal self-audit DTO mapping helper "
                    + forbidden + " in controller");
        }
    }

    private void forbid(Path path, MethodBody method, List<String> forbiddenItems, List<String> violations)
    {
        if (method == null)
        {
            violations.add(relative(path) + " must contain profile mapping method before checking forbidden fields");
            return;
        }
        for (String forbidden : forbiddenItems)
        {
            if (method.body.contains(forbidden))
            {
                violations.add(relative(path) + "#" + method.name
                        + " must not copy internal audit field " + forbidden);
            }
        }
    }

    private MethodBody findMethod(List<MethodBody> methods, String methodName)
    {
        return methods.stream().filter(method -> methodName.equals(method.name)).findFirst().orElse(null);
    }

    private String capitalize(String value)
    {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private String relative(Path path)
    {
        return findBackendRoot().relativize(path).toString().replace('\\', '/');
    }

    private Path findBackendRoot()
    {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path[] candidates = new Path[] {
                cwd,
                cwd.resolve("..").normalize(),
                cwd.resolve("RuoYi-Vue").normalize()
        };

        for (Path candidate : candidates)
        {
            if (Files.isDirectory(candidate.resolve("seller/src/main/java"))
                    && Files.isDirectory(candidate.resolve("buyer/src/main/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }

    private static class MethodBody
    {
        private final String name;

        private final String body;

        private MethodBody(String name, String body)
        {
            this.name = name;
            this.body = body;
        }
    }
}
