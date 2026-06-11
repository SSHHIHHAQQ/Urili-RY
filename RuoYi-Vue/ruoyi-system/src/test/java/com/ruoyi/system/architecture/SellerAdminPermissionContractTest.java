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

public class SellerAdminPermissionContractTest
{
    private static final Pattern SELLER_ADMIN_CLASS_MAPPING = Pattern.compile(
            "@RequestMapping\\s*\\(\\s*(?:(?:value|path)\\s*=\\s*)?\"/seller/admin(?:/|\")",
            Pattern.DOTALL);
    private static final Pattern HANDLER_MAPPING = Pattern.compile("@(?:GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\b");
    private static final Pattern SENSITIVE_MAPPING = Pattern.compile("@(?:PostMapping|PutMapping|DeleteMapping|PatchMapping)\\b");
    private static final Pattern HAS_PERMI = Pattern.compile("@ss\\.hasPermi\\('([^']+)'\\)");
    private static final Pattern PUBLIC_METHOD = Pattern.compile("public\\s+[^\\(]+\\s+(\\w+)\\s*\\(");
    private static final Pattern SELLER_ADMIN_SEED_PERM = Pattern.compile("'(seller:admin:[^']+)'");

    @Test
    public void sellerAdminControllersMustUseSellerAdminControlTemplate() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Set<String> seededPerms = readSeededSellerAdminPerms(backendRoot);
        List<String> violations = new ArrayList<>();

        for (Path controller : findSellerAdminControllers(backendRoot))
        {
            assertSellerAdminControllerTemplate(backendRoot, controller, seededPerms, violations);
        }

        if (!violations.isEmpty())
        {
            fail("seller admin controllers must follow the seller management template:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerAccountAdminHandlersMustUseAccountScopedPermissions() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path controller = backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java");
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        List<HandlerMethod> handlers = extractHandlerMethods(source);
        List<String> violations = new ArrayList<>();

        assertHandlerPermission(handlers, "list", "seller:admin:list", violations);
        assertHandlerPermission(handlers, "get", "seller:admin:query", violations);
        assertHandlerPermission(handlers, "add", "seller:admin:add", violations);
        assertHandlerPermission(handlers, "edit", "seller:admin:edit", violations);
        assertHandlerPermission(handlers, "changeStatus", "seller:admin:changeStatus", violations);
        assertHandlerPermission(handlers, "accounts", "seller:admin:account:list", violations);
        assertHandlerPermission(handlers, "accountRoles", "seller:admin:account:role:query", violations);
        assertHandlerPermission(handlers, "accountRoles", "seller:admin:role:query", violations);
        assertHandlerPermission(handlers, "assignAccountRoles", "seller:admin:account:role:edit", violations);
        assertHandlerPermission(handlers, "assignAccountRoles", "seller:admin:account:role:query", violations);
        assertHandlerPermission(handlers, "assignAccountRoles", "seller:admin:role:query", violations);
        assertHandlerPermission(handlers, "addAccount", "seller:admin:account:add", violations);
        assertHandlerPermission(handlers, "editAccount", "seller:admin:account:edit", violations);
        assertHandlerPermission(handlers, "resetPassword", "seller:admin:account:resetPwd", violations);
        assertHandlerPermission(handlers, "lockAccount", "seller:admin:account:lock", violations);
        assertHandlerPermission(handlers, "unlockAccount", "seller:admin:account:lock", violations);
        assertHandlerPermission(handlers, "sessions", "seller:admin:session:list", violations);
        assertHandlerPermission(handlers, "accountSessions", "seller:admin:session:list", violations);
        assertHandlerPermission(handlers, "forceLogoutSeller", "seller:admin:forceLogout", violations);
        assertHandlerPermission(handlers, "forceLogoutSellerAccount", "seller:admin:forceLogout", violations);
        assertHandlerPermission(handlers, "directLogin", "seller:admin:directLogin", violations);
        assertHandlerPermission(handlers, "accountDirectLogin", "seller:admin:directLogin", violations);
        assertHandlerPermission(handlers, "loginLogs", "seller:admin:loginLog:list", violations);
        assertHandlerPermission(handlers, "operLogs", "seller:admin:operLog:list", violations);
        assertHandlerPermission(handlers, "directLoginTickets", "seller:admin:ticket:list", violations);
        assertSensitiveReadLog(handlers, "sessions", violations);
        assertSensitiveReadLog(handlers, "accountSessions", violations);
        assertSensitiveReadLog(handlers, "loginLogs", violations);
        assertSensitiveReadLog(handlers, "operLogs", violations);
        assertSensitiveReadLog(handlers, "directLoginTickets", violations);
        assertHandlerRouteContains(handlers, "accounts", "@GetMapping(\"/{sellerId}/accounts\")", violations);
        assertHandlerRouteContains(handlers, "accountRoles",
                "@GetMapping(\"/{sellerId}/accounts/{accountId}/roles\")", violations);
        assertHandlerRouteContains(handlers, "assignAccountRoles",
                "@PutMapping(\"/{sellerId}/accounts/{accountId}/roles\")", violations);
        assertHandlerRouteContains(handlers, "addAccount", "@PostMapping(\"/{sellerId}/accounts\")", violations);
        assertHandlerRouteContains(handlers, "editAccount", "@PutMapping(\"/{sellerId}/accounts\")", violations);
        assertSourceContains(source, "@Validated @RequestBody SellerAccount account",
                "AdminSellerController#editAccount must validate request body", violations);
        assertHandlerRouteContains(handlers, "lockAccount",
                "@PutMapping(\"/{sellerId}/accounts/{accountId}/lock\")", violations);
        assertHandlerRouteContains(handlers, "unlockAccount",
                "@PutMapping(\"/{sellerId}/accounts/{accountId}/unlock\")", violations);
        assertHandlerRouteContains(handlers, "resetPassword", "@PutMapping(\"/{sellerId}/accounts/{accountId}/resetPwd\")", violations);
        assertHandlerMissing(handlers, "resetDefaultPassword",
                "AdminSellerController must not expose account default password reset", violations);
        assertSourceNotContains(source, "resetDefaultPwd",
                "AdminSellerController must not expose /{sellerId}/accounts/{accountId}/resetDefaultPwd", violations);
        assertHandlerMissing(handlers, "resetOwnerPassword",
                "AdminSellerController must not expose subject-level default owner password reset", violations);
        assertSourceNotContains(source, "resetOwnerPwd",
                "AdminSellerController must not expose /{sellerId}/resetOwnerPwd", violations);
        assertSourceNotContains(source, "seller:admin:resetPwd",
                "AdminSellerController must use account-scoped resetPwd permission only", violations);
        assertHandlerRouteContains(handlers, "sessions", "@GetMapping(\"/{sellerId}/sessions/list\")", violations);
        assertHandlerRouteContains(handlers, "accountSessions",
                "@GetMapping(\"/{sellerId}/accounts/{accountId}/sessions/list\")", violations);
        assertHandlerRouteContains(handlers, "forceLogoutSeller",
                "@DeleteMapping(\"/{sellerId}/sessions\")", violations);
        assertHandlerRouteContains(handlers, "forceLogoutSellerAccount",
                "@DeleteMapping(\"/{sellerId}/accounts/{accountId}/sessions\")", violations);
        assertHandlerRouteContains(handlers, "directLogin", "@PostMapping(\"/{sellerId}/directLogin\")",
                violations);
        assertHandlerRouteContains(handlers, "accountDirectLogin",
                "@PostMapping(\"/{sellerId}/accounts/{accountId}/directLogin\")", violations);
        assertHandlerRouteContains(handlers, "loginLogs", "@GetMapping(\"/loginLogs/list\")", violations);
        assertHandlerRouteContains(handlers, "operLogs", "@GetMapping(\"/operLogs/list\")", violations);
        assertHandlerRouteContains(handlers, "directLoginTickets",
                "@GetMapping(\"/directLoginTickets/list\")", violations);

        if (!violations.isEmpty())
        {
            fail("seller account admin handlers must not reuse subject or role permissions:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerRoleMenuTreeMustRequireRoleAndMenuQuery() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path controller = backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/controller/AdminSellerMenuController.java");
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        List<HandlerMethod> handlers = extractHandlerMethods(source);
        List<String> violations = new ArrayList<>();

        assertHandlerPermission(handlers, "roleMenuTreeselect", "seller:admin:role:query", violations);
        assertHandlerPermission(handlers, "roleMenuTreeselect", "seller:admin:menu:query", violations);
        assertSourceContains(source, "permissionService.selectSelfManagementMenuIdsByRoleId(sellerId, roleId)",
                "AdminSellerMenuController#roleMenuTreeselect must filter checked menu IDs to self-management",
                violations);
        assertSourceContains(source, "permissionService.buildSelfManagementMenuTreeSelect()",
                "AdminSellerMenuController#roleMenuTreeselect must expose only self-management menu templates",
                violations);
        assertSourceNotContains(source, "permissionService.selectMenuIdsByRoleId(sellerId, roleId)",
                "AdminSellerMenuController#roleMenuTreeselect must not return all checked menu IDs", violations);
        assertSourceNotContains(source, "permissionService.buildMenuTreeSelect(new PortalMenu())",
                "AdminSellerMenuController#roleMenuTreeselect must not return all terminal menus", violations);

        if (!violations.isEmpty())
        {
            fail("seller role menu tree must require both role query and menu query permissions and only expose self-management templates:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerRoleDeptMenuHandlersMustUseSpecificPermissionsAndRoutes() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        List<HandlerMethod> roleHandlers = extractHandlerMethods(Files.readString(
                backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/controller/AdminSellerRoleController.java"),
                StandardCharsets.UTF_8));
        assertControllerHandlerPermission(roleHandlers, "AdminSellerRoleController", "list",
                "seller:admin:role:list", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminSellerRoleController", "getInfo",
                "seller:admin:role:query", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminSellerRoleController", "add",
                "seller:admin:role:add", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminSellerRoleController", "edit",
                "seller:admin:role:edit", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminSellerRoleController", "changeStatus",
                "seller:admin:role:edit", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminSellerRoleController", "remove",
                "seller:admin:role:remove", violations);
        assertControllerHandlerPermission(roleHandlers, "AdminSellerRoleController", "optionselect",
                "seller:admin:role:query", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminSellerRoleController", "list",
                "@GetMapping(\"/list\")", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminSellerRoleController", "getInfo",
                "@GetMapping(\"/{roleId}\")", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminSellerRoleController", "add",
                "@PostMapping", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminSellerRoleController", "edit",
                "@PutMapping", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminSellerRoleController", "changeStatus",
                "@PutMapping(\"/changeStatus\")", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminSellerRoleController", "remove",
                "@DeleteMapping(\"/{roleIds}\")", violations);
        assertControllerHandlerRouteContains(roleHandlers, "AdminSellerRoleController", "optionselect",
                "@GetMapping(\"/optionselect\")", violations);

        List<HandlerMethod> deptHandlers = extractHandlerMethods(Files.readString(
                backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/controller/AdminSellerDeptController.java"),
                StandardCharsets.UTF_8));
        assertControllerHandlerPermission(deptHandlers, "AdminSellerDeptController", "list",
                "seller:admin:dept:list", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminSellerDeptController", "getInfo",
                "seller:admin:dept:query", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminSellerDeptController", "treeselect",
                "seller:admin:dept:query", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminSellerDeptController", "add",
                "seller:admin:dept:add", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminSellerDeptController", "edit",
                "seller:admin:dept:edit", violations);
        assertControllerHandlerPermission(deptHandlers, "AdminSellerDeptController", "remove",
                "seller:admin:dept:remove", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminSellerDeptController", "list",
                "@GetMapping(\"/list\")", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminSellerDeptController", "getInfo",
                "@GetMapping(\"/{deptId}\")", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminSellerDeptController", "treeselect",
                "@GetMapping(\"/treeselect\")", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminSellerDeptController", "add",
                "@PostMapping", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminSellerDeptController", "edit",
                "@PutMapping", violations);
        assertControllerHandlerRouteContains(deptHandlers, "AdminSellerDeptController", "remove",
                "@DeleteMapping(\"/{deptId}\")", violations);

        String menuSource = Files.readString(
                backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/controller/AdminSellerMenuController.java"),
                StandardCharsets.UTF_8);
        List<HandlerMethod> menuHandlers = extractHandlerMethods(menuSource);
        assertControllerHandlerPermission(menuHandlers, "AdminSellerMenuController", "list",
                "seller:admin:menu:list", violations);
        assertControllerHandlerPermission(menuHandlers, "AdminSellerMenuController", "getInfo",
                "seller:admin:menu:query", violations);
        assertControllerHandlerPermission(menuHandlers, "AdminSellerMenuController", "treeselect",
                "seller:admin:menu:query", violations);
        assertControllerHandlerPermission(menuHandlers, "AdminSellerMenuController", "roleMenuTreeselect",
                "seller:admin:role:query", violations);
        assertControllerHandlerPermission(menuHandlers, "AdminSellerMenuController", "roleMenuTreeselect",
                "seller:admin:menu:query", violations);
        assertControllerHandlerRouteContains(menuHandlers, "AdminSellerMenuController", "list",
                "@GetMapping(\"/list\")", violations);
        assertControllerHandlerRouteContains(menuHandlers, "AdminSellerMenuController", "getInfo",
                "@GetMapping(\"/{menuId}\")", violations);
        assertControllerHandlerRouteContains(menuHandlers, "AdminSellerMenuController", "treeselect",
                "@GetMapping(\"/treeselect\")", violations);
        assertControllerHandlerRouteContains(menuHandlers, "AdminSellerMenuController", "roleMenuTreeselect",
                "@GetMapping(\"/roleMenuTreeselect/{sellerId}/{roleId}\")", violations);
        assertHandlerMissing(menuHandlers, "add",
                "AdminSellerMenuController must not expose menu template add in this phase", violations);
        assertHandlerMissing(menuHandlers, "edit",
                "AdminSellerMenuController must not expose menu template edit in this phase", violations);
        assertHandlerMissing(menuHandlers, "remove",
                "AdminSellerMenuController must not expose menu template remove in this phase", violations);
        assertSourceNotContains(menuSource, "seller:admin:menu:add",
                "AdminSellerMenuController must not require stale menu add permission", violations);
        assertSourceNotContains(menuSource, "seller:admin:menu:edit",
                "AdminSellerMenuController must not require stale menu edit permission", violations);
        assertSourceNotContains(menuSource, "seller:admin:menu:remove",
                "AdminSellerMenuController must not require stale menu remove permission", violations);
        assertSourceNotContains(menuSource, "@PostMapping",
                "AdminSellerMenuController must keep seller menu templates readonly", violations);
        assertSourceNotContains(menuSource, "@PutMapping",
                "AdminSellerMenuController must keep seller menu templates readonly", violations);
        assertSourceNotContains(menuSource, "@DeleteMapping",
                "AdminSellerMenuController must keep seller menu templates readonly", violations);

        if (!violations.isEmpty())
        {
            fail("seller role/dept/menu admin handlers must use their precise permissions:\n"
                    + String.join("\n", violations));
        }
    }

    private List<Path> findSellerAdminControllers(Path backendRoot) throws IOException
    {
        Path controllerRoot = backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/controller");
        try (Stream<Path> paths = Files.walk(controllerRoot))
        {
            return paths.filter(path -> path.getFileName().toString().startsWith("AdminSeller"))
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private Set<String> readSeededSellerAdminPerms(Path backendRoot) throws IOException
    {
        Path seed = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String sql = Files.readString(seed, StandardCharsets.UTF_8);
        Matcher matcher = SELLER_ADMIN_SEED_PERM.matcher(sql);
        Set<String> perms = new HashSet<>();
        while (matcher.find())
        {
            perms.add(matcher.group(1));
        }
        return perms;
    }

    private void assertSellerAdminControllerTemplate(Path backendRoot, Path controller, Set<String> seededPerms,
            List<String> violations) throws IOException
    {
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        String relativePath = backendRoot.relativize(controller).toString();

        if (!SELLER_ADMIN_CLASS_MAPPING.matcher(source).find())
        {
            violations.add(relativePath + " must use a /seller/admin class-level route");
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
            assertSellerAdminHandlerTemplate(relativePath, handler, seededPerms, violations);
        }
    }

    private void assertSellerAdminHandlerTemplate(String relativePath, HandlerMethod handler, Set<String> seededPerms,
            List<String> violations)
    {
        String prefix = relativePath + "#" + handler.name;
        if (!handler.annotations.contains("@PreAuthorize"))
        {
            violations.add(prefix + " must declare @PreAuthorize(\"@ss.hasPermi('seller:admin:...')\")");
            return;
        }

        Matcher matcher = HAS_PERMI.matcher(handler.annotations);
        boolean hasPerm = false;
        while (matcher.find())
        {
            hasPerm = true;
            String perm = matcher.group(1);
            if (!perm.startsWith("seller:admin:"))
            {
                violations.add(prefix + " permission must use seller:admin:* but was " + perm);
            }
            if (!seededPerms.contains(perm))
            {
                violations.add(prefix + " permission " + perm + " must exist in seller_buyer_management_seed.sql");
            }
        }
        if (!hasPerm)
        {
            violations.add(prefix + " must declare @ss.hasPermi('seller:admin:...')");
        }
        if (SENSITIVE_MAPPING.matcher(handler.annotations).find() && !handler.annotations.contains("@Log"))
        {
            violations.add(prefix + " must declare @Log for mutating or sensitive admin operations");
        }
    }

    private void assertHandlerPermission(List<HandlerMethod> handlers, String methodName, String expectedPermission,
            List<String> violations)
    {
        assertControllerHandlerPermission(handlers, "AdminSellerController", methodName, expectedPermission,
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
        assertControllerHandlerRouteContains(handlers, "AdminSellerController", methodName, expectedRoute,
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
            violations.add("AdminSellerController#" + methodName + " must exist");
            return;
        }
        if (!target.annotations.contains("@Log"))
        {
            violations.add("AdminSellerController#" + methodName + " must write admin operation log");
        }
        if (!target.annotations.contains("isSaveResponseData = false"))
        {
            violations.add("AdminSellerController#" + methodName + " must not save sensitive response data");
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
            if (Files.isDirectory(candidate.resolve("seller/src/main/java"))
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
