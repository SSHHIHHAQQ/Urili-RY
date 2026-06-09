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
        assertAdminController(backendRoot,
                "product/src/main/java/com/ruoyi/product/controller/AdminProductCenterController.java",
                "AdminProductCenterController",
                "@RequestMapping(\"/product/admin/product-center\")",
                new String[] {
                        "product:center:list",
                        "product:center:query"
                },
                violations);
        assertAdminController(backendRoot,
                "product/src/main/java/com/ruoyi/product/controller/AdminProductReviewController.java",
                "AdminProductReviewController",
                "@RequestMapping(\"/product/admin/reviews\")",
                new String[] {
                        "review:productDistribution:list",
                        "review:productDistribution:query",
                        "review:productDistribution:approve",
                        "review:productDistribution:reject",
                        "review:productDistribution:log"
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
        String productCenterServiceTs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/product/productCenter.ts"), StandardCharsets.UTF_8);
        String productCenterServiceJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/product/productCenter.js"), StandardCharsets.UTF_8);
        String reviewServiceTs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/product/productReview.ts"), StandardCharsets.UTF_8);
        String reviewServiceJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/product/productReview.js"), StandardCharsets.UTF_8);
        String categoryPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Product/Category/index.tsx"), StandardCharsets.UTF_8);
        String attributeLibrary = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Product/Attribute/components/AttributeLibrary.tsx"), StandardCharsets.UTF_8);
        String distributionPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Product/Distribution/index.tsx"), StandardCharsets.UTF_8);
        String productCenterPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Product/ProductCenter/index.tsx"), StandardCharsets.UTF_8);
        String reviewPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Product/Review/index.tsx"), StandardCharsets.UTF_8);

        assertContains(productServiceTs, "const baseUrl = '/api/product/admin';",
                "product TS service must call the admin route", violations);
        assertEqualsTrimmed("export * from './product.ts';", productServiceJs,
                "product JS service mirror must delegate to TS service", violations);
        assertContains(distributionServiceTs, "const baseUrl = '/api/product/admin/distribution-products';",
                "distribution product TS service must call the admin route", violations);
        assertEqualsTrimmed("export * from './distributionProduct.ts';", distributionServiceJs,
                "distribution product JS service mirror must delegate to TS service", violations);
        assertContains(distributionServiceTs, "submitDistributionProductReview",
                "distribution product TS service must expose submit review action", violations);
        assertContains(productCenterServiceTs, "const baseUrl = '/api/product/admin/product-center';",
                "product center TS service must call the admin route", violations);
        assertEqualsTrimmed("export * from './productCenter.ts';", productCenterServiceJs,
                "product center JS service mirror must delegate to TS service", violations);
        assertContains(productCenterPage, "access.hasPerms('product:center:list')",
                "product center page must derive list permission", violations);
        assertContains(productCenterPage, "access.hasPerms('product:center:query')",
                "product center page must derive query permission", violations);
        assertContains(reviewServiceTs, "const baseUrl = '/api/product/admin/reviews';",
                "product review TS service must call the admin route", violations);
        assertEqualsTrimmed("export * from './productReview.ts';", reviewServiceJs,
                "product review JS service mirror must delegate to TS service", violations);
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
        assertContains(distributionPage, "submitDistributionProductReview",
                "product distribution page must submit draft products to review", violations);
        assertContains(reviewPage, "const canListProductReview = access.hasPerms('review:productDistribution:list')",
                "product review page must derive list permission", violations);
        assertContains(reviewPage, "const canQueryProductReview = access.hasPerms('review:productDistribution:query')",
                "product review page must derive query permission", violations);
        assertContains(reviewPage, "const canApproveProductReview = access.hasPerms('review:productDistribution:approve')",
                "product review page must derive approve permission", violations);
        assertContains(reviewPage, "const canRejectProductReview = access.hasPerms('review:productDistribution:reject')",
                "product review page must derive reject permission", violations);
        assertContains(reviewPage, "const canViewProductReviewLog = access.hasPerms('review:productDistribution:log')",
                "product review page must derive log permission", violations);

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

    private void assertEqualsTrimmed(String expected, String actual, String message, List<String> violations)
    {
        if (!expected.equals(actual.trim()))
        {
            violations.add(message + ": expected " + expected + " but found " + actual.trim());
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
