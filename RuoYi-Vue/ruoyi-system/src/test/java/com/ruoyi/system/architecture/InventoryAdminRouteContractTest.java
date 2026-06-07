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

public class InventoryAdminRouteContractTest
{
    @Test
    public void inventoryOverviewControllerAndFrontendServiceMustUseAdminRoute() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path repoRoot = backendRoot.getParent();
        String controller = Files.readString(backendRoot.resolve(
                "inventory/src/main/java/com/ruoyi/inventory/controller/AdminInventoryOverviewController.java"),
                StandardCharsets.UTF_8);
        String overviewService = Files.readString(repoRoot.resolve(
                "react-ui/src/services/inventory/overview.ts"), StandardCharsets.UTF_8);
        String overviewPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/index.tsx"), StandardCharsets.UTF_8);
        String skuWarehouseTable = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/SkuWarehouseTable.tsx"), StandardCharsets.UTF_8);
        String quantityCell = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/QuantityCell.tsx"), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertContains(controller, "@RequestMapping(\"/inventory/admin/overview\")",
                "AdminInventoryOverviewController must stay under /inventory/admin/overview", violations);
        assertNotContains(controller, "@RequestMapping(\"/inventory\")",
                "AdminInventoryOverviewController must not expose the admin surface under /inventory", violations);
        assertNotContains(controller, "@Anonymous",
                "AdminInventoryOverviewController must not expose anonymous handlers", violations);
        assertNotContains(controller, "@PortalPreAuthorize",
                "AdminInventoryOverviewController must not use portal authorization", violations);
        assertNotContains(controller, "@PortalLog",
                "AdminInventoryOverviewController must not use portal logging", violations);
        assertNotContains(controller, "seller:",
                "AdminInventoryOverviewController must not use seller terminal permissions", violations);
        assertNotContains(controller, "buyer:",
                "AdminInventoryOverviewController must not use buyer terminal permissions", violations);

        for (String expectedPermission : new String[] {
                "inventory:overview:list",
                "inventory:overview:query",
                "inventory:overview:adjust",
                "inventory:overview:ledger"
        })
        {
            assertContains(controller, "@ss.hasPermi('" + expectedPermission + "')",
                    "AdminInventoryOverviewController must guard " + expectedPermission, violations);
        }
        for (String expectedRoute : new String[] {
                "@GetMapping(\"/spu/list\")",
                "@GetMapping(\"/sku/list\")",
                "@GetMapping(\"/sku/{skuId}/warehouses\")",
                "@PostMapping(\"/adjust/preview\")",
                "@PostMapping(\"/adjust/confirm\")",
                "@GetMapping(\"/ledger/list\")"
        })
        {
            assertContains(controller, expectedRoute,
                    "AdminInventoryOverviewController must expose " + expectedRoute, violations);
        }
        assertContains(controller, "inventoryOverviewService.selectWarehouseStockListBySkuId(skuId)",
                "AdminInventoryOverviewController must use sku-scoped warehouse query", violations);
        assertContains(controller, "inventoryOverviewService.previewAdjust(request)",
                "AdminInventoryOverviewController must delegate adjust preview", violations);
        assertContains(controller, "inventoryOverviewService.confirmAdjust(request)",
                "AdminInventoryOverviewController must delegate adjust confirm", violations);
        assertContains(controller, "inventoryOverviewService.selectLedgerList(query)",
                "AdminInventoryOverviewController must delegate ledger query", violations);

        assertContains(overviewService, "const baseUrl = '/api/inventory/admin/overview';",
                "inventory overview TS service must call the admin route", violations);
        assertContains(overviewService, "`${baseUrl}/sku/${skuId}/warehouses`",
                "inventory overview service must keep sku warehouse route", violations);
        assertContains(overviewService, "`${baseUrl}/adjust/preview`",
                "inventory overview service must keep adjust preview route", violations);
        assertContains(overviewService, "`${baseUrl}/adjust/confirm`",
                "inventory overview service must keep adjust confirm route", violations);
        assertContains(overviewService, "`${baseUrl}/ledger/list`",
                "inventory overview service must keep ledger route", violations);
        assertContains(overviewPage, "access.hasPerms('inventory:overview:adjust')",
                "inventory overview adjustment UI must use the backend adjustment permission", violations);
        assertContains(overviewPage, "const canQueryInventoryOverview = access.hasPerms('inventory:overview:query')",
                "inventory overview warehouse detail expansion must derive query permission", violations);
        assertContains(overviewPage, "expandable={canQueryInventoryOverview",
                "inventory overview warehouse detail expansion must be gated by query permission", violations);
        assertContains(overviewPage, "SkuWarehouseTable",
                "inventory overview page must render sku warehouse details", violations);
        assertContains(overviewPage, "canAdjust={access.hasPerms('inventory:overview:adjust')}",
                "inventory overview page must pass adjust permission into warehouse details", violations);
        assertContains(skuWarehouseTable, "getInventoryOverviewWarehouses(skuId)",
                "SkuWarehouseTable must load warehouses from the inventory overview service", violations);
        assertContains(skuWarehouseTable, "disabled={!canAdjust || record.warehouseRefType === 'NO_WAREHOUSE'}",
                "SkuWarehouseTable must gate platform total adjustment by canAdjust", violations);
        assertContains(skuWarehouseTable, "disabled={!canAdjust || record.warehouseKind !== 'official'}",
                "SkuWarehouseTable must gate in-transit adjustment by canAdjust", violations);
        assertContains(quantityCell, "previewInventoryOverviewAdjust({",
                "QuantityCell must call adjust preview before saving", violations);
        assertContains(quantityCell, "confirmInventoryOverviewAdjust({",
                "QuantityCell must call adjust confirm through the service", violations);

        if (!violations.isEmpty())
        {
            fail("inventory overview admin route must stay on the admin namespace:\n" + String.join("\n", violations));
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
            if (Files.isDirectory(candidate.resolve("inventory/src/main/java"))
                    && Files.isDirectory(candidate.resolve("ruoyi-system/src/test/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
