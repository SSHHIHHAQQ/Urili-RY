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
    private static final Pattern PRE_AUTHORIZE_PERM = Pattern.compile(
            "@PreAuthorize\\s*\\(\\s*\"@ss\\.hasPermi\\('([^']+)'\\)\"\\s*\\)",
            Pattern.DOTALL);
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

        assertHandlerPermission(handlers, "get", "seller:admin:query", violations);
        assertHandlerPermission(handlers, "accounts", "seller:admin:account:list", violations);
        assertHandlerPermission(handlers, "accountRoles", "seller:admin:account:role:query", violations);
        assertHandlerPermission(handlers, "assignAccountRoles", "seller:admin:account:role:edit", violations);
        assertHandlerPermission(handlers, "addAccount", "seller:admin:account:add", violations);
        assertHandlerPermission(handlers, "editAccount", "seller:admin:account:edit", violations);
        assertHandlerPermission(handlers, "resetPassword", "seller:admin:account:resetPwd", violations);
        assertHandlerPermission(handlers, "resetDefaultPassword", "seller:admin:account:resetPwd", violations);

        if (!violations.isEmpty())
        {
            fail("seller account admin handlers must not reuse subject or role permissions:\n"
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
        Matcher matcher = PRE_AUTHORIZE_PERM.matcher(handler.annotations);
        if (!matcher.find())
        {
            violations.add(prefix + " must declare @PreAuthorize(\"@ss.hasPermi('seller:admin:...')\")");
            return;
        }

        String perm = matcher.group(1);
        if (!perm.startsWith("seller:admin:"))
        {
            violations.add(prefix + " permission must use seller:admin:* but was " + perm);
        }
        if (!seededPerms.contains(perm))
        {
            violations.add(prefix + " permission " + perm + " must exist in seller_buyer_management_seed.sql");
        }
        if (SENSITIVE_MAPPING.matcher(handler.annotations).find() && !handler.annotations.contains("@Log"))
        {
            violations.add(prefix + " must declare @Log for mutating or sensitive admin operations");
        }
    }

    private void assertHandlerPermission(List<HandlerMethod> handlers, String methodName, String expectedPermission,
            List<String> violations)
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
        if (!target.annotations.contains("@PreAuthorize(\"@ss.hasPermi('" + expectedPermission + "')\")"))
        {
            violations.add("AdminSellerController#" + methodName + " must require " + expectedPermission);
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
