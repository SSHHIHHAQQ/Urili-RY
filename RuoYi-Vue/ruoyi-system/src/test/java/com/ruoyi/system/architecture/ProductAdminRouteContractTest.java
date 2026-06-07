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

public class ProductAdminRouteContractTest
{
    @Test
    public void productAdminControllersAndFrontendServicesMustUseAdminRoutes() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path repoRoot = backendRoot.getParent();
        List<String> violations = new ArrayList<>();

        assertAdminController(backendRoot,
                "product/src/main/java/com/ruoyi/product/controller/AdminProductCategoryController.java",
                "AdminProductCategoryController",
                "@RequestMapping(\"/product/admin/categories\")",
                new String[] {
                        "product:category:list",
                        "product:category:query",
                        "product:category:add",
                        "product:category:edit",
                        "product:category:remove"
                },
                violations);
        assertAdminController(backendRoot,
                "product/src/main/java/com/ruoyi/product/controller/AdminProductAttributeController.java",
                "AdminProductAttributeController",
                "@RequestMapping(\"/product/admin/attributes\")",
                new String[] {
                        "product:attribute:list",
                        "product:attribute:query",
                        "product:attribute:add",
                        "product:attribute:edit",
                        "product:attribute:remove"
                },
                violations);
        assertAdminController(backendRoot,
                "product/src/main/java/com/ruoyi/product/controller/AdminProductCategoryAttributeController.java",
                "AdminProductCategoryAttributeController",
                "@RequestMapping(\"/product/admin/category-attributes\")",
                new String[] {
                        "product:categoryAttribute:list",
                        "product:categoryAttribute:preview",
                        "product:categoryAttribute:edit"
                },
                violations);
        assertAdminController(backendRoot,
                "product/src/main/java/com/ruoyi/product/controller/AdminProductConfigChangeLogController.java",
                "AdminProductConfigChangeLogController",
                "@RequestMapping(\"/product/admin/change-logs\")",
                new String[] {
                        "product:category:list",
                        "product:attribute:list",
                        "product:categoryAttribute:list"
                },
                violations);
        assertAdminController(backendRoot,
                "product/src/main/java/com/ruoyi/product/controller/AdminProductDistributionController.java",
                "AdminProductDistributionController",
                "@RequestMapping(\"/product/admin/distribution-products\")",
                new String[] {
                        "product:distribution:list",
                        "product:distribution:query",
                        "product:distribution:add",
                        "product:distribution:edit",
                        "product:distribution:status",
                        "product:distribution:price",
                        "product:distribution:log"
                },
                violations);

        String productServiceTs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/product/product.ts"), StandardCharsets.UTF_8);
        String productServiceJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/product/product.js"), StandardCharsets.UTF_8);
        String distributionServiceTs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/product/distributionProduct.ts"), StandardCharsets.UTF_8);
        String distributionServiceJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/product/distributionProduct.js"), StandardCharsets.UTF_8);
        String categoryPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Product/Category/index.tsx"), StandardCharsets.UTF_8);
        String attributeLibrary = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Product/Attribute/components/AttributeLibrary.tsx"), StandardCharsets.UTF_8);
        String distributionPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Product/Distribution/index.tsx"), StandardCharsets.UTF_8);

        assertContains(productServiceTs, "const baseUrl = '/api/product/admin';",
                "product TS service must call the admin route", violations);
        assertContains(productServiceJs, "const baseUrl = '/api/product/admin';",
                "product JS service mirror must call the admin route", violations);
        assertContains(distributionServiceTs, "const baseUrl = '/api/product/admin/distribution-products';",
                "distribution product TS service must call the admin route", violations);
        assertContains(distributionServiceJs, "const baseUrl = '/api/product/admin/distribution-products';",
                "distribution product JS service mirror must call the admin route", violations);
        assertContains(categoryPage, "access.hasPerms('product:category:add')",
                "product category page must gate add action", violations);
        assertContains(categoryPage, "access.hasPerms('product:category:remove')",
                "product category page must gate remove action", violations);
        assertContains(attributeLibrary, "access.hasPerms('product:attribute:add')",
                "product attribute page must gate add action", violations);
        assertContains(attributeLibrary, "access.hasPerms('product:attribute:remove')",
                "product attribute page must gate remove action", violations);
        assertContains(distributionPage, "access.hasPerms('product:distribution:add')",
                "product distribution page must gate add action", violations);
        assertContains(distributionPage, "access.hasPerms('product:distribution:status')",
                "product distribution page must gate status action", violations);
        assertContains(distributionPage, "access.hasPerms('product:distribution:price')",
                "product distribution page must gate price action", violations);
        assertContains(distributionPage, "const canViewDistributionDetail = access.hasPerms('product:distribution:query')",
                "product distribution page must derive detail query permission", violations);
        assertContains(distributionPage, "if (!canViewDistributionDetail || record.spuId == null)",
                "product distribution detail action must fail closed without query permission", violations);
        assertContains(distributionPage, "hidden={!canViewDistributionDetail}",
                "product distribution detail buttons must be hidden without query permission", violations);

        if (!violations.isEmpty())
        {
            fail("product admin routes must stay on the admin namespace:\n" + String.join("\n", violations));
        }
    }

    private void assertAdminController(Path backendRoot, String relativePath, String controllerName,
            String expectedRoute, String[] expectedPermissions, List<String> violations) throws IOException
    {
        String controller = Files.readString(backendRoot.resolve(relativePath), StandardCharsets.UTF_8);
        assertContains(controller, expectedRoute, controllerName + " must stay under its admin route", violations);
        assertNotContains(controller, "@RequestMapping(\"/product\")",
                controllerName + " must not expose the admin surface under /product", violations);
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
            if (Files.isDirectory(candidate.resolve("product/src/main/java"))
                    && Files.isDirectory(candidate.resolve("ruoyi-system/src/test/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
