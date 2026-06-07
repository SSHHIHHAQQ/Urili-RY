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

public class IntegrationAdminRouteContractTest
{
    @Test
    public void integrationAdminControllersAndFrontendServicesMustUseAdminRoutes() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path repoRoot = backendRoot.getParent();
        List<String> violations = new ArrayList<>();

        assertAdminController(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/controller/AdminSourceProductController.java",
                "AdminSourceProductController",
                "@RequestMapping(\"/integration/admin/source-products\")",
                new String[] {
                        "product:list:list"
                },
                violations);
        assertAdminController(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/controller/AdminSourceWarehouseStockController.java",
                "AdminSourceWarehouseStockController",
                "@RequestMapping(\"/integration/admin/source-warehouse-stocks\")",
                new String[] {
                        "inventory:sourceWarehouse:list"
                },
                violations);
        assertAdminController(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/controller/AdminUpstreamSystemController.java",
                "AdminUpstreamSystemController",
                "@RequestMapping(\"/integration/admin/upstream-systems\")",
                new String[] {
                        "integration:upstream:list",
                        "integration:upstream:query",
                        "integration:upstream:add",
                        "integration:upstream:edit",
                        "integration:upstream:sync",
                        "integration:upstream:dimensionSync",
                        "integration:upstream:inventorySync",
                        "integration:upstream:inventoryQuery",
                        "integration:upstream:pair",
                        "integration:upstream:log"
                },
                violations);

        assertServiceBaseUrl(repoRoot, "react-ui/src/services/integration/sourceProduct.ts",
                "const baseUrl = '/api/integration/admin/source-products';", violations);
        assertServiceBaseUrl(repoRoot, "react-ui/src/services/integration/sourceProduct.js",
                "const baseUrl = '/api/integration/admin/source-products';", violations);
        assertServiceBaseUrl(repoRoot, "react-ui/src/services/integration/sourceWarehouseStock.ts",
                "const baseUrl = '/api/integration/admin/source-warehouse-stocks';", violations);
        assertServiceBaseUrl(repoRoot, "react-ui/src/services/integration/upstreamSystem.ts",
                "const baseUrl = '/api/integration/admin/upstream-systems';", violations);
        assertServiceBaseUrl(repoRoot, "react-ui/src/services/integration/upstreamSystem.js",
                "const baseUrl = '/api/integration/admin/upstream-systems';", violations);
        assertFrontendPermissionGate(repoRoot,
                "react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx",
                new String[] {
                        "const canQueryUpstream = access.hasPerms('integration:upstream:query')",
                        "const canQueryInventory = access.hasPerms('integration:upstream:inventoryQuery')",
                        "const canViewLogs = access.hasPerms('integration:upstream:log')",
                        "if (!canQueryUpstream)",
                        "if (!canViewLogs)"
                },
                violations);
        assertFrontendPermissionGate(repoRoot,
                "react-ui/src/pages/UpstreamSystem/components/SkuSyncPanel.tsx",
                new String[] {
                        "const canQueryUpstream = access.hasPerms('integration:upstream:query')",
                        "if (!selectedCode || !canQueryUpstream)",
                        "if (!canQueryUpstream)"
                },
                violations);
        assertFrontendPermissionGate(repoRoot,
                "react-ui/src/pages/UpstreamSystem/components/SkuDimensionPanel.tsx",
                new String[] {
                        "const canQueryUpstream = access.hasPerms('integration:upstream:query')",
                        "if (!selectedCode || !canQueryUpstream)",
                        "if (!canQueryUpstream)"
                },
                violations);
        assertFrontendPermissionGate(repoRoot,
                "react-ui/src/pages/UpstreamSystem/components/SkuInventoryPanel.tsx",
                new String[] {
                        "const canQueryInventory = access.hasPerms('integration:upstream:inventoryQuery')",
                        "if (!selectedCode || !canQueryInventory)",
                        "if (!canQueryInventory)"
                },
                violations);

        if (!violations.isEmpty())
        {
            fail("integration admin routes must stay on the admin namespace:\n" + String.join("\n", violations));
        }
    }

    private void assertAdminController(Path backendRoot, String relativePath, String controllerName,
            String expectedRoute, String[] expectedPermissions, List<String> violations) throws IOException
    {
        String controller = Files.readString(backendRoot.resolve(relativePath), StandardCharsets.UTF_8);
        assertContains(controller, expectedRoute, controllerName + " must stay under its admin route", violations);
        assertNotContains(controller, "@RequestMapping(\"/integration\")",
                controllerName + " must not expose the admin surface under /integration", violations);
        assertNotContains(controller, "@Anonymous", controllerName + " must not expose anonymous handlers", violations);
        assertNotContains(controller, "@PortalPreAuthorize",
                controllerName + " must not use portal authorization", violations);
        assertNotContains(controller, "@PortalLog", controllerName + " must not use portal logging", violations);
        assertNotContains(controller, "seller:", controllerName + " must not use seller terminal permissions", violations);
        assertNotContains(controller, "buyer:", controllerName + " must not use buyer terminal permissions", violations);

        for (String expectedPermission : expectedPermissions)
        {
            assertContains(controller, expectedPermission,
                    controllerName + " must guard " + expectedPermission, violations);
        }
    }

    private void assertServiceBaseUrl(Path repoRoot, String relativePath, String expectedBaseUrl,
            List<String> violations) throws IOException
    {
        String service = Files.readString(repoRoot.resolve(relativePath), StandardCharsets.UTF_8);
        assertContains(service, expectedBaseUrl, relativePath + " must call the admin route", violations);
        assertNotContains(service, "const baseUrl = '/api/integration/source",
                relativePath + " must not call a non-admin integration route", violations);
        assertNotContains(service, "const baseUrl = '/api/integration/upstream",
                relativePath + " must not call a non-admin integration route", violations);
    }

    private void assertFrontendPermissionGate(Path repoRoot, String relativePath, String[] expectedSnippets,
            List<String> violations) throws IOException
    {
        String source = Files.readString(repoRoot.resolve(relativePath), StandardCharsets.UTF_8);
        for (String expectedSnippet : expectedSnippets)
        {
            assertContains(source, expectedSnippet,
                    relativePath + " must keep permission guard " + expectedSnippet, violations);
        }
    }

    private void assertContains(String source, String expected, String message, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(message + ": missing " + expected);
        }
    }

    private void assertNotContains(String source, String forbidden, String message, List<String> violations)
    {
        if (source.contains(forbidden))
        {
            violations.add(message + ": found " + forbidden);
        }
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
            if (Files.isDirectory(candidate.resolve("integration/src/main/java"))
                    && Files.isDirectory(candidate.resolve("ruoyi-system/src/test/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
