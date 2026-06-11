package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class BuyerAdminPermissionContractTest
{
    private static final Pattern BUYER_ADMIN_CLASS_MAPPING = Pattern.compile(
            "@RequestMapping\\s*\\(\\s*(?:(?:value|path)\\s*=\\s*)?\"/buyer/admin(?:/|\")",
            Pattern.DOTALL);
    private static final Pattern HANDLER_MAPPING = Pattern.compile("@(?:GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\b");
    private static final Pattern SENSITIVE_MAPPING = Pattern.compile("@(?:PostMapping|PutMapping|DeleteMapping|PatchMapping)\\b");
    private static final Pattern HAS_PERMI = Pattern.compile("@ss\\.hasPermi\\('([^']+)'\\)");
    private static final Pattern PUBLIC_METHOD = Pattern.compile("public\\s+[^\\(]+\\s+(\\w+)\\s*\\(");
    private static final Pattern BUYER_ADMIN_SEED_PERM = Pattern.compile("'(buyer:admin:[^']+)'");

    @Test
    public void buyerAdminControllersMustUseBuyerAdminControlTemplate() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Set<String> seededPerms = readSeededBuyerAdminPerms(backendRoot);
        List<String> violations = new ArrayList<>();

        for (Path controller : findBuyerAdminControllers(backendRoot))
        {
            assertBuyerAdminControllerTemplate(backendRoot, controller, seededPerms, violations);
        }

        if (!violations.isEmpty())
        {
            fail("buyer admin controllers must follow the buyer management template:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void buyerAccountAdminHandlersMustUseAccountScopedPermissions() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path controller = backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java");
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        List<HandlerMethod> handlers = extractHandlerMethods(source);
        List<String> violations = new ArrayList<>();

        assertHandlerPermission(handlers, "list", "buyer:admin:list", violations);
        assertHandlerPermission(handlers, "get", "buyer:admin:query", violations);
        assertHandlerPermission(handlers, "add", "buyer:admin:add", violations);
        assertHandlerPermission(handlers, "edit", "buyer:admin:edit", violations);
        assertHandlerPermission(handlers, "changeStatus", "buyer:admin:changeStatus", violations);
        assertHandlerPermission(handlers, "accounts", "buyer:admin:account:list", violations);
        assertHandlerPermission(handlers, "accountRoles", "buyer:admin:account:role:query", violations);
        assertHandlerPermission(handlers, "accountRoles", "buyer:admin:role:query", violations);
        assertHandlerPermission(handlers, "assignAccountRoles", "buyer:admin:account:role:edit", violations);
        assertHandlerPermission(handlers, "assignAccountRoles", "buyer:admin:account:role:query", violations);
        assertHandlerPermission(handlers, "assignAccountRoles", "buyer:admin:role:query", violations);
        assertHandlerPermission(handlers, "addAccount", "buyer:admin:account:add", violations);
        assertHandlerPermission(handlers, "editAccount", "buyer:admin:account:edit", violations);
        assertHandlerPermission(handlers, "resetPassword", "buyer:admin:account:resetPwd", violations);
        assertHandlerPermission(handlers, "lockAccount", "buyer:admin:account:lock", violations);
        assertHandlerPermission(handlers, "unlockAccount", "buyer:admin:account:lock", violations);
        assertHandlerPermission(handlers, "sessions", "buyer:admin:session:list", violations);
        assertHandlerPermission(handlers, "accountSessions", "buyer:admin:session:list", violations);
        assertHandlerPermission(handlers, "forceLogoutBuyer", "buyer:admin:forceLogout", violations);
        assertHandlerPermission(handlers, "forceLogoutBuyerAccount", "buyer:admin:forceLogout", violations);
        assertHandlerPermission(handlers, "directLogin", "buyer:admin:directLogin", violations);
        assertHandlerPermission(handlers, "accountDirectLogin", "buyer:admin:directLogin", violations);
        assertHandlerPermission(handlers, "loginLogs", "buyer:admin:loginLog:list", violations);
        assertHandlerPermission(handlers, "operLogs", "buyer:admin:operLog:list", violations);
        assertHandlerPermission(handlers, "directLoginTickets", "buyer:admin:ticket:list", violations);
        assertSensitiveReadLog(handlers, "sessions", violations);
        assertSensitiveReadLog(handlers, "accountSessions", violations);
        assertSensitiveReadLog(handlers, "loginLogs", violations);
        assertSensitiveReadLog(handlers, "operLogs", violations);
        assertSensitiveReadLog(handlers, "directLoginTickets", violations);
        assertHandlerRouteContains(handlers, "accounts", "@GetMapping(\"/{buyerId}/accounts\")", violations);
        assertHandlerRouteContains(handlers, "accountRoles",
                "@GetMapping(\"/{buyerId}/accounts/{accountId}/roles\")", violations);
        assertHandlerRouteContains(handlers, "assignAccountRoles",
                "@PutMapping(\"/{buyerId}/accounts/{accountId}/roles\")", violations);
        assertHandlerRouteContains(handlers, "addAccount", "@PostMapping(\"/{buyerId}/accounts\")", violations);
        assertHandlerRouteContains(handlers, "editAccount", "@PutMapping(\"/{buyerId}/accounts\")", violations);
        assertSourceContains(source, "@Validated @RequestBody BuyerAccount account",
                "AdminBuyerController#editAccount must validate request body", violations);
        assertHandlerRouteContains(handlers, "lockAccount",
                "@PutMapping(\"/{buyerId}/accounts/{accountId}/lock\")", violations);
        assertHandlerRouteContains(handlers, "unlockAccount",
                "@PutMapping(\"/{buyerId}/accounts/{accountId}/unlock\")", violations);
        assertHandlerRouteContains(handlers, "resetPassword", "@PutMapping(\"/{buyerId}/accounts/{accountId}/resetPwd\")", violations);
        assertHandlerMissing(handlers, "resetDefaultPassword",
                "AdminBuyerController must not expose account default password reset", violations);
        assertSourceNotContains(source, "resetDefaultPwd",
                "AdminBuyerController must not expose /{buyerId}/accounts/{accountId}/resetDefaultPwd", violations);
        assertHandlerMissing(handlers, "resetOwnerPassword",
                "AdminBuyerController must not expose subject-level default owner password reset", violations);
        assertSourceNotContains(source, "resetOwnerPwd",
                "AdminBuyerController must not expose /{buyerId}/resetOwnerPwd", violations);
        assertSourceNotContains(source, "buyer:admin:resetPwd",
                "AdminBuyerController must use account-scoped resetPwd permission only", violations);
        assertHandlerRouteContains(handlers, "sessions", "@GetMapping(\"/{buyerId}/sessions/list\")", violations);
        assertHandlerRouteContains(handlers, "accountSessions",
                "@GetMapping(\"/{buyerId}/accounts/{accountId}/sessions/list\")", violations);
        assertHandlerRouteContains(handlers, "forceLogoutBuyer",
                "@DeleteMapping(\"/{buyerId}/sessions\")", violations);
        assertHandlerRouteContains(handlers, "forceLogoutBuyerAccount",
                "@DeleteMapping(\"/{buyerId}/accounts/{accountId}/sessions\")", violations);
        assertHandlerRouteContains(handlers, "directLogin", "@PostMapping(\"/{buyerId}/directLogin\")",
                violations);
        assertHandlerRouteContains(handlers, "accountDirectLogin",
                "@PostMapping(\"/{buyerId}/accounts/{accountId}/directLogin\")", violations);
        assertHandlerRouteContains(handlers, "loginLogs", "@GetMapping(\"/loginLogs/list\")", violations);
        assertHandlerRouteContains(handlers, "operLogs", "@GetMapping(\"/operLogs/list\")", violations);
        assertHandlerRouteContains(handlers, "directLoginTickets",
                "@GetMapping(\"/directLoginTickets/list\")", violations);

        if (!violations.isEmpty())
        {
            fail("buyer account admin handlers must not reuse subject or role permissions:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void buyerRoleMenuTreeMustRequireRoleAndMenuQuery() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path controller = backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerMenuController.java");
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        List<HandlerMethod> handlers = extractHandlerMethods(source);
        List<String> violations = new ArrayList<>();

        assertHandlerPermission(handlers, "roleMenuTreeselect", "buyer:admin:role:query", violations);
        assertHandlerPermission(handlers, "roleMenuTreeselect", "buyer:admin:menu:query", violations);
        assertSourceContains(source, "permissionService.selectSelfManagementMenuIdsByRoleId(buyerId, roleId)",
                "AdminBuyerMenuController#roleMenuTreeselect must filter checked menu IDs to self-management",
                violations);
        assertSourceContains(source, "permissionService.buildSelfManagementMenuTreeSelect()",
                "AdminBuyerMenuController#roleMenuTreeselect must expose only self-management menu templates",
                violations);
        assertSourceNotContains(source, "permissionService.selectMenuIdsByRoleId(buyerId, roleId)",
                "AdminBuyerMenuController#roleMenuTreeselect must not return all checked menu IDs", violations);
        assertSourceNotContains(source, "permissionService.buildMenuTreeSelect(new PortalMenu())",
                "AdminBuyerMenuController#roleMenuTreeselect must not return all terminal menus", violations);

        if (!violations.isEmpty())
        {
            fail("buyer role menu tree must require both role query and menu query permissions and only expose self-management templates:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void buyerRoleDeptMenuHandlersMustUseSpecificPermissionsAndRoutes() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        List<HandlerMethod> roleHandlers = extractHandlerMethods(Files.readString(
                backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerRoleController.java"),
                StandardCharsets.UTF_8));
        assertControllerHandlerPermission(roleHandlers, "AdminBuyerRoleController", "list",
                "buyer:admin:role:list", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminBuyerRoleController", "getInfo",
                "buyer:admin:role:query", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminBuyerRoleController", "add",
                "buyer:admin:role:add", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminBuyerRoleController", "edit",
                "buyer:admin:role:edit", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminBuyerRoleController", "changeStatus",
                "buyer:admin:role:edit", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminBuyerRoleController", "remove",
                "buyer:admin:role:remove", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminBuyerRoleController", "optionselect",
                "buyer:admin:role:query", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminBuyerRoleController", "list",
                "@GetMapping(\"/list\")", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminBuyerRoleController", "getInfo",
                "@GetMapping(\"/{roleId}\")", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminBuyerRoleController", "add",
                "@PostMapping", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminBuyerRoleController", "edit",
                "@PutMapping", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminBuyerRoleController", "changeStatus",
                "@PutMapping(\"/changeStatus\")", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminBuyerRoleController", "remove",
                "@DeleteMapping(\"/{roleIds}\")", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminBuyerRoleController", "optionselect",
                "@GetMapping(\"/optionselect\")", violations);

        List<HandlerMethod> deptHandlers = extractHandlerMethods(Files.readString(
                backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerDeptController.java"),
                StandardCharsets.UTF_8));
        assertControllerHandlerPermission(deptHandlers, "AdminBuyerDeptController", "list",
                "buyer:admin:dept:list", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminBuyerDeptController", "getInfo",
                "buyer:admin:dept:query", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminBuyerDeptController", "treeselect",
                "buyer:admin:dept:query", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminBuyerDeptController", "add",
                "buyer:admin:dept:add", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminBuyerDeptController", "edit",
                "buyer:admin:dept:edit", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminBuyerDeptController", "remove",
                "buyer:admin:dept:remove", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminBuyerDeptController", "list",
                "@GetMapping(\"/list\")", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminBuyerDeptController", "getInfo",
                "@GetMapping(\"/{deptId}\")", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminBuyerDeptController", "treeselect",
                "@GetMapping(\"/treeselect\")", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminBuyerDeptController", "add",
                "@PostMapping", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminBuyerDeptController", "edit",
                "@PutMapping", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminBuyerDeptController", "remove",
                "@DeleteMapping(\"/{deptId}\")", violations);

        String menuSource = Files.readString(
                backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerMenuController.java"),
                StandardCharsets.UTF_8);
        List<HandlerMethod> menuHandlers = extractHandlerMethods(menuSource);
        assertControllerHandlerPermission(menuHandlers, "AdminBuyerMenuController", "list",
                "buyer:admin:menu:list", violations);
        assertControllerHandlerPermission(menuHandlers, "AdminBuyerMenuController", "getInfo",
                "buyer:admin:menu:query", violations);
        assertControllerHandlerPermission(menuHandlers, "AdminBuyerMenuController", "treeselect",
                "buyer:admin:menu:query", violations);
        assertControllerHandlerPermission(menuHandlers, "AdminBuyerMenuController", "roleMenuTreeselect",
                "buyer:admin:role:query", violations);
        assertControllerHandlerPermission(menuHandlers, "AdminBuyerMenuController", "roleMenuTreeselect",
                "buyer:admin:menu:query", violations);
        assertControllerHandlerRouteContains(menuHandlers, "AdminBuyerMenuController", "list",
                "@GetMapping(\"/list\")", violations);
        assertControllerHandlerRouteContains(menuHandlers, "AdminBuyerMenuController", "getInfo",
                "@GetMapping(\"/{menuId}\")", violations);
        assertControllerHandlerRouteContains(menuHandlers, "AdminBuyerMenuController", "treeselect",
                "@GetMapping(\"/treeselect\")", violations);
        assertControllerHandlerRouteContains(menuHandlers, "AdminBuyerMenuController", "roleMenuTreeselect",
                "@GetMapping(\"/roleMenuTreeselect/{buyerId}/{roleId}\")", violations);
        assertHandlerMissing(menuHandlers, "add",
                "AdminBuyerMenuController must not expose menu template add in this phase", violations);
        assertHandlerMissing(menuHandlers, "edit",
                "AdminBuyerMenuController must not expose menu template edit in this phase", violations);
        assertHandlerMissing(menuHandlers, "remove",
                "AdminBuyerMenuController must not expose menu template remove in this phase", violations);
        assertSourceNotContains(menuSource, "buyer:admin:menu:add",
                "AdminBuyerMenuController must not require stale menu add permission", violations);
        assertSourceNotContains(menuSource, "buyer:admin:menu:edit",
                "AdminBuyerMenuController must not require stale menu edit permission", violations);
        assertSourceNotContains(menuSource, "buyer:admin:menu:remove",
                "AdminBuyerMenuController must not require stale menu remove permission", violations);
        assertSourceNotContains(menuSource, "@PostMapping",
                "AdminBuyerMenuController must keep buyer menu templates readonly", violations);
        assertSourceNotContains(menuSource, "@PutMapping",
                "AdminBuyerMenuController must keep buyer menu templates readonly", violations);
        assertSourceNotContains(menuSource, "@DeleteMapping",
                "AdminBuyerMenuController must keep buyer menu templates readonly", violations);

        if (!violations.isEmpty())
        {
            fail("buyer role/dept/menu admin handlers must use their precise permissions:\n"
                    + String.join("\n", violations));
        }
    }

    private List<Path> findBuyerAdminControllers(Path backendRoot) throws IOException
    {
        Path controllerRoot = backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/controller");
        try (Stream<Path> paths = Files.walk(controllerRoot))
        {
            return paths.filter(path -> path.getFileName().toString().startsWith("AdminBuyer"))
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private Set<String> readSeededBuyerAdminPerms(Path backendRoot) throws IOException
    {
        Path seed = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String sql = Files.readString(seed, StandardCharsets.UTF_8);
        Matcher matcher = BUYER_ADMIN_SEED_PERM.matcher(sql);
        Set<String> perms = new HashSet<>();
        while (matcher.find())
        {
            perms.add(matcher.group(1));
        }
        return perms;
    }

    private void assertBuyerAdminControllerTemplate(Path backendRoot, Path controller, Set<String> seededPerms,
            List<String> violations) throws IOException
    {
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        String relativePath = backendRoot.relativize(controller).toString();

        if (!BUYER_ADMIN_CLASS_MAPPING.matcher(source).find())
        {
            violations.add(relativePath + " must use a /buyer/admin class-level route");
        }
        if (source.contains("@Anonymous") || source.contains("@PortalPreAuthorize") || source.contains("@PortalLog"))
        {
            violations.add(relativePath + " must use admin security/log annotations, not portal annotations");
        }

        List<HandlerMethod> handlers = extractHandlerMethods(source);
        if (handlers.isEmpty())
        {
            violations.add(relativePath + " has no admin handler methods");
        }
        for (HandlerMethod handler : handlers)
        {
            assertBuyerAdminHandlerTemplate(relativePath, handler, seededPerms, violations);
        }
    }

    private void assertBuyerAdminHandlerTemplate(String relativePath, HandlerMethod handler, Set<String> seededPerms,
            List<String> violations)
    {
        String prefix = relativePath + "#" + handler.name;
        if (!handler.annotations.contains("@PreAuthorize"))
        {
            violations.add(prefix + " must declare @PreAuthorize(\"@ss.hasPermi('buyer:admin:...')\")");
            return;
        }

        Matcher matcher = HAS_PERMI.matcher(handler.annotations);
        boolean hasPerm = false;
        while (matcher.find())
        {
            hasPerm = true;
            String perm = matcher.group(1);
            if (!perm.startsWith("buyer:admin:"))
            {
                violations.add(prefix + " permission must use buyer:admin:* but was " + perm);
            }
            if (!seededPerms.contains(perm))
            {
                violations.add(prefix + " permission " + perm + " must exist in seller_buyer_management_seed.sql");
            }
        }
        if (!hasPerm)
        {
            violations.add(prefix + " must declare @ss.hasPermi('buyer:admin:...')");
        }
        if (SENSITIVE_MAPPING.matcher(handler.annotations).find() && !handler.annotations.contains("@Log"))
        {
            violations.add(prefix + " must declare @Log for mutating or sensitive admin operations");
        }
    }

    private void assertHandlerPermission(List<HandlerMethod> handlers, String methodName, String expectedPermission,
            List<String> violations)
    {
        assertControllerHandlerPermission(handlers, "AdminBuyerController", methodName, expectedPermission,
                violations);
    }

    private void assertControllerHandlerPermission(List<HandlerMethod> handlers, String controllerName,
            String methodName, String expectedPermission, List<String> violations)
    {
        HandlerMethod target = handlers.stream()
                .filter(handler -> methodName.equals(handler.name))
                .findFirst()
                .orElse(null);
        if (target == null)
        {
            violations.add(controllerName + "#" + methodName + " must exist");
            return;
        }
        if (!target.annotations.contains("@ss.hasPermi('" + expectedPermission + "')"))
        {
            violations.add(controllerName + "#" + methodName + " must require " + expectedPermission);
        }
    }

    private void assertHandlerMissing(List<HandlerMethod> handlers, String methodName, String message,
            List<String> violations)
    {
        boolean exists = handlers.stream().anyMatch(handler -> methodName.equals(handler.name));
        if (exists)
        {
            violations.add(message);
        }
    }

    private void assertHandlerRouteContains(List<HandlerMethod> handlers, String methodName, String expectedRoute,
            List<String> violations)
    {
        assertControllerHandlerRouteContains(handlers, "AdminBuyerController", methodName, expectedRoute,
                violations);
    }

    private void assertControllerHandlerRouteContains(List<HandlerMethod> handlers, String controllerName,
            String methodName, String expectedRoute, List<String> violations)
    {
        HandlerMethod target = handlers.stream()
                .filter(handler -> methodName.equals(handler.name))
                .findFirst()
                .orElse(null);
        if (target == null)
        {
            violations.add(controllerName + "#" + methodName + " must exist");
            return;
        }
        if (!target.annotations.contains(expectedRoute))
        {
            violations.add(controllerName + "#" + methodName + " must keep admin route " + expectedRoute);
        }
    }

    private void assertSourceContains(String source, String expected, String message, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(message + ": missing " + expected);
        }
    }

    private void assertSourceNotContains(String source, String forbidden, String message, List<String> violations)
    {
        if (source.contains(forbidden))
        {
            violations.add(message + ": found " + forbidden);
        }
    }

    private void assertSensitiveReadLog(List<HandlerMethod> handlers, String methodName, List<String> violations)
    {
        HandlerMethod target = handlers.stream()
                .filter(handler -> methodName.equals(handler.name))
                .findFirst()
                .orElse(null);
        if (target == null)
        {
            violations.add("AdminBuyerController#" + methodName + " must exist");
            return;
        }
        if (!target.annotations.contains("@Log"))
        {
            violations.add("AdminBuyerController#" + methodName + " must write admin operation log");
        }
        if (!target.annotations.contains("isSaveResponseData = false"))
        {
            violations.add("AdminBuyerController#" + methodName + " must not save sensitive response data");
        }
    }

    private List<HandlerMethod> extractHandlerMethods(String source)
    {
        String[] lines = source.split("\\R", -1);
        List<HandlerMethod> handlers = new ArrayList<>();
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
                if (HANDLER_MAPPING.matcher(annotationText).find())
                {
                    handlers.add(new HandlerMethod(extractMethodName(lines[i]), annotationText));
                }
                annotations.clear();
                continue;
            }

            if (!trimmed.isEmpty())
            {
                annotations.clear();
            }
        }

        return handlers;
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
            if (Files.isDirectory(candidate.resolve("buyer/src/main/java"))
                    && Files.isDirectory(candidate.resolve("sql")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }

    private static class HandlerMethod
    {
        private final String name;
        private final String annotations;

        private HandlerMethod(String name, String annotations)
        {
            this.name = name;
            this.annotations = annotations;
        }
    }
}
