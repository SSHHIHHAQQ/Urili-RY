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
        String overviewServiceJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/inventory/overview.js"), StandardCharsets.UTF_8);
        String overviewPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/index.tsx"), StandardCharsets.UTF_8);
        String overviewPageJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/index.js"), StandardCharsets.UTF_8);
        String warehouseViewTable = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/WarehouseViewTable.tsx"), StandardCharsets.UTF_8);
        String warehouseViewTableJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/WarehouseViewTable.js"), StandardCharsets.UTF_8);
        String spuSkuWarehouseTable = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/SpuSkuWarehouseTable.tsx"), StandardCharsets.UTF_8);
        String spuSkuWarehouseTableJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/SpuSkuWarehouseTable.js"), StandardCharsets.UTF_8);
        String skuWarehouseTable = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/SkuWarehouseTable.tsx"), StandardCharsets.UTF_8);
        String skuWarehouseTableJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/SkuWarehouseTable.js"), StandardCharsets.UTF_8);
        String quantityCell = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/QuantityCell.tsx"), StandardCharsets.UTF_8);
        String quantityCellJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/QuantityCell.js"), StandardCharsets.UTF_8);
        String inventoryAdjustButton = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/InventoryAdjustButton.tsx"), StandardCharsets.UTF_8);
        String inventoryAdjustButtonJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/components/InventoryAdjustButton.js"), StandardCharsets.UTF_8);
        String sharedInventoryAdjustButton = Files.readString(repoRoot.resolve(
                "react-ui/src/components/InventoryAdjust/InventoryAdjustButton.tsx"), StandardCharsets.UTF_8);
        String sharedInventoryAdjustButtonJs = Files.readString(repoRoot.resolve(
                "react-ui/src/components/InventoryAdjust/InventoryAdjustButton.js"), StandardCharsets.UTF_8);
        String sharedInventorySyncPolicyButton = Files.readString(repoRoot.resolve(
                "react-ui/src/components/InventorySyncPolicy/InventorySyncPolicyButton.tsx"), StandardCharsets.UTF_8);
        String sharedInventorySyncPolicyButtonJs = Files.readString(repoRoot.resolve(
                "react-ui/src/components/InventorySyncPolicy/InventorySyncPolicyButton.js"), StandardCharsets.UTF_8);
        String helpers = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/helpers.tsx"), StandardCharsets.UTF_8);
        String helpersJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/Overview/helpers.js"), StandardCharsets.UTF_8);
        String inventoryService = Files.readString(backendRoot.resolve(
                "inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java"),
                StandardCharsets.UTF_8);
        String inventoryMapper = Files.readString(backendRoot.resolve(
                "inventory/src/main/resources/mapper/inventory/InventoryOverviewMapper.xml"),
                StandardCharsets.UTF_8);
        String normalizedController = controller.replace("\r\n", "\n");
        String normalizedInventoryMapper = inventoryMapper.replace("\r\n", "\n");
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
                "inventory:overview:ledger",
                "inventory:overview:syncPolicy"
        })
        {
            assertContains(controller, "@ss.hasPermi('" + expectedPermission + "')",
                    "AdminInventoryOverviewController must guard " + expectedPermission, violations);
        }
        for (String expectedRoute : new String[] {
                "@GetMapping(\"/spu/list\")",
                "@GetMapping(\"/sku/list\")",
                "@GetMapping(\"/warehouse/list\")",
                "@GetMapping(\"/warehouse/options\")",
                "@GetMapping(\"/official-warehouse/options\")",
                "@GetMapping(\"/seller/options\")",
                "@GetMapping(\"/sku/{skuId}/warehouses\")",
                "@GetMapping(\"/spu/{spuId}/sku-warehouses\")",
                "@PostMapping(\"/adjust/preview\")",
                "@PostMapping(\"/adjust/batch-preview\")",
                "@PostMapping(\"/sync-policy/preview\")",
                "@PostMapping(\"/adjust/confirm\")",
                "@PostMapping(\"/adjust/batch-confirm\")",
                "@PostMapping(\"/sync-policy/confirm\")",
                "@GetMapping(\"/ledger/list\")"
        })
        {
            assertContains(controller, expectedRoute,
                    "AdminInventoryOverviewController must expose " + expectedRoute, violations);
        }
        assertContains(controller, "inventoryOverviewService.selectWarehouseStockList(query)",
                "AdminInventoryOverviewController must use warehouse flat stock query", violations);
        assertContains(controller, "inventoryOverviewService.selectWarehouseOptions()",
                "AdminInventoryOverviewController must expose warehouse options for overview filters", violations);
        assertContains(controller, "inventoryOverviewService.selectOfficialWarehouseOptions()",
                "AdminInventoryOverviewController must expose official warehouse options for sync policy modal", violations);
        assertContains(controller, "inventoryOverviewService.selectSellerOptions()",
                "AdminInventoryOverviewController must expose seller options for overview filters", violations);
        assertContains(controller, "inventoryStockSyncPolicyService.previewSyncPolicy(request)",
                "AdminInventoryOverviewController must delegate sync policy preview", violations);
        assertContains(controller, "inventoryStockSyncPolicyService.confirmSyncPolicy(request)",
                "AdminInventoryOverviewController must delegate sync policy confirm", violations);
        assertContains(normalizedController,
                "@PreAuthorize(\"@ss.hasPermi('inventory:overview:query')\")\n    @GetMapping(\"/warehouse/list\")",
                "AdminInventoryOverviewController warehouse flat stock query must require query permission",
                violations);
        assertContains(controller, "inventoryOverviewService.selectWarehouseStockListBySkuId(skuId)",
                "AdminInventoryOverviewController must use sku-scoped warehouse query", violations);
        assertContains(controller, "inventoryOverviewService.selectSkuWarehouseGroupsBySpuId(spuId)",
                "AdminInventoryOverviewController must use spu-scoped sku warehouse groups query", violations);
        assertContains(controller, "inventoryOverviewService.previewAdjust(request)",
                "AdminInventoryOverviewController must delegate adjust preview", violations);
        assertContains(controller, "inventoryOverviewService.confirmAdjust(request)",
                "AdminInventoryOverviewController must delegate adjust confirm", violations);
        assertContains(controller, "inventoryOverviewService.previewBatchAdjust(request)",
                "AdminInventoryOverviewController must delegate batch adjust preview", violations);
        assertContains(controller, "inventoryOverviewService.confirmBatchAdjust(request)",
                "AdminInventoryOverviewController must delegate batch adjust confirm", violations);
        assertContains(controller, "inventoryOverviewService.selectLedgerList(query)",
                "AdminInventoryOverviewController must delegate ledger query", violations);
        assertContains(normalizedInventoryMapper, "where stock_id = #{stockId}\n          and version = #{version}",
                "inventory stock adjustment update must use stock_id + version optimistic lock", violations);
        assertContains(inventoryService, "int updatedRows = inventoryOverviewMapper.updateWarehouseStock(after)",
                "inventory adjustment service must capture optimistic-lock update rows", violations);
        assertContains(inventoryService, "if (updatedRows != 1)",
                "inventory adjustment service must fail when optimistic lock update misses", violations);
        assertContains(inventoryService, "库存数据已变更，请刷新后重试",
                "inventory adjustment service must tell operator to refresh after concurrent stock changes",
                violations);

        assertContains(overviewService, "const baseUrl = '/api/inventory/admin/overview';",
                "inventory overview TS service must call the admin route", violations);
        assertContains(overviewService, "`${baseUrl}/warehouse/list`",
                "inventory overview service must keep warehouse flat list route", violations);
        assertContains(overviewService, "`${baseUrl}/warehouse/options`",
                "inventory overview service must keep warehouse options route", violations);
        assertContains(overviewService, "`${baseUrl}/official-warehouse/options`",
                "inventory overview service must keep official warehouse options route", violations);
        assertContains(overviewService, "`${baseUrl}/seller/options`",
                "inventory overview service must keep seller options route", violations);
        assertContains(overviewService, "`${baseUrl}/sku/${skuId}/warehouses`",
                "inventory overview service must keep sku warehouse route", violations);
        assertContains(overviewService, "`${baseUrl}/spu/${spuId}/sku-warehouses`",
                "inventory overview service must keep spu sku warehouse route", violations);
        assertContains(overviewService, "`${baseUrl}/sync-policy/preview`",
                "inventory overview service must keep sync policy preview route", violations);
        assertContains(overviewService, "`${baseUrl}/sync-policy/confirm`",
                "inventory overview service must keep sync policy confirm route", violations);
        assertContains(overviewService, "`${baseUrl}/adjust/preview`",
                "inventory overview service must keep adjust preview route", violations);
        assertContains(overviewService, "`${baseUrl}/adjust/confirm`",
                "inventory overview service must keep adjust confirm route", violations);
        assertContains(overviewService, "`${baseUrl}/adjust/batch-preview`",
                "inventory overview service must keep batch adjust preview route", violations);
        assertContains(overviewService, "`${baseUrl}/adjust/batch-confirm`",
                "inventory overview service must keep batch adjust confirm route", violations);
        assertContains(overviewService, "`${baseUrl}/ledger/list`",
                "inventory overview service must keep ledger route", violations);
        assertContains(overviewPage, "access.hasPerms('inventory:overview:adjust')",
                "inventory overview adjustment UI must use the backend adjustment permission", violations);
        assertContains(overviewPage,
                "const canAdjustInventoryOverview = access.hasPerms('inventory:overview:adjust') && canQueryInventoryOverview",
                "inventory overview adjustment UI must require query permission before loading warehouse rows",
                violations);
        assertContains(overviewPage,
                "const canSyncInventoryOverview = access.hasPerms('inventory:overview:syncPolicy') && canQueryInventoryOverview",
                "inventory overview sync policy UI must require query permission before applying policy",
                violations);
        assertContains(overviewPage, "const canListInventoryOverview = access.hasPerms('inventory:overview:list')",
                "inventory overview SPU/SKU lists must derive list permission", violations);
        assertContains(overviewPage, "const canQueryInventoryOverview = access.hasPerms('inventory:overview:query')",
                "inventory overview warehouse detail expansion must derive query permission", violations);
        assertContains(overviewPage, "item.value === 'WAREHOUSE' ? canQueryInventoryOverview : canListInventoryOverview",
                "inventory overview view switch must split list and query permissions", violations);
        assertContains(overviewPage, "if (!canListInventoryOverview)",
                "inventory overview SPU/SKU requests must fail closed without list permission", violations);
        assertContains(overviewPage, "expandable={canQueryInventoryOverview",
                "inventory overview warehouse detail expansion must be gated by query permission", violations);
        assertContains(overviewPage, "SkuWarehouseTable",
                "inventory overview page must render sku warehouse details", violations);
        assertContains(overviewPage, "SpuSkuWarehouseTable",
                "inventory overview page must render spu sku warehouse details", violations);
        assertContains(overviewPage, "WarehouseViewTable",
                "inventory overview page must render warehouse flat view", violations);
        assertContains(overviewPage, "InventoryAdjustButton",
                "inventory overview page must expose aggregate adjustment actions", violations);
        assertContains(overviewPage, "getInventoryOverviewWarehouseOptions",
                "inventory overview page must load warehouse options for search", violations);
        assertContains(overviewPage, "getInventoryOverviewOfficialWarehouseOptions",
                "inventory overview page must load official warehouse options for sync policy modal", violations);
        assertContains(overviewPage, "getInventoryOverviewSellerOptions",
                "inventory overview page must load seller options for search and sync policy scope", violations);
        assertContains(overviewPage, "buildInventoryOverviewListParams(params, current, pageSize)",
                "inventory overview page must use shared search range request conversion", violations);
        assertContains(overviewPage, "canAdjust={canAdjustInventoryOverview}",
                "inventory overview page must pass fail-closed adjust permission into warehouse details", violations);
        assertContains(overviewPage, "canSync={canSyncInventoryOverview}",
                "inventory overview page must pass fail-closed sync policy permission into child views", violations);
        assertContains(overviewPage, "canQuery={canQueryInventoryOverview}",
                "inventory overview page must pass query permission into warehouse flat view", violations);
        assertContains(overviewPage, "InventorySyncPolicyButton",
                "inventory overview page must expose sync policy toolbar and row actions", violations);
        assertContains(warehouseViewTable, "getInventoryOverviewWarehouseList",
                "WarehouseViewTable must load warehouse flat rows from the inventory overview service", violations);
        assertContains(warehouseViewTable, "if (!canQuery)",
                "WarehouseViewTable must fail closed without query permission", violations);
        assertContains(warehouseViewTable, "return { data: [], total: 0, success: true };",
                "WarehouseViewTable must not request admin API without query permission", violations);
        assertContains(overviewPage, "dataIndex: 'warehouseKey'",
                "inventory overview SPU/SKU views must expose warehouse search", violations);
        assertContains(warehouseViewTable, "dataIndex: 'warehouseKey'",
                "inventory overview warehouse view must expose warehouse search", violations);
        assertContains(overviewPage, "dataIndex: 'pairingStatus'",
                "inventory overview SPU/SKU views must expose pairing status search", violations);
        assertContains(warehouseViewTable, "dataIndex: 'pairingStatus'",
                "inventory overview warehouse view must expose pairing status search", violations);
        assertContains(overviewPage, "dataIndex: 'sellerId'",
                "inventory overview SPU/SKU views must expose seller search", violations);
        assertContains(warehouseViewTable, "dataIndex: 'sellerId'",
                "inventory overview warehouse view must expose seller search", violations);
        assertContains(overviewPage, "dataIndex: 'syncModeSummary'",
                "inventory overview SPU/SKU views must expose sync mode search and column", violations);
        assertContains(warehouseViewTable, "dataIndex: 'syncMode'",
                "inventory overview warehouse view must expose sync mode search and column", violations);
        assertContains(normalizedInventoryMapper, "pairingStatus == 'PAIRED'",
                "inventory overview mapper must support paired inventory filtering", violations);
        assertContains(normalizedInventoryMapper, "pairingStatus == 'UNASSIGNED'",
                "inventory overview mapper must support unpaired inventory filtering", violations);
        assertContains(normalizedInventoryMapper, "inventory_status in ('NO_WAREHOUSE', 'SOURCE_UNBOUND')",
                "inventory overview aggregate unpaired filtering must use no-warehouse and source-unbound statuses",
                violations);
        assertContains(normalizedInventoryMapper, "sws.effective_status in ('NO_WAREHOUSE', 'SOURCE_UNBOUND')",
                "inventory overview warehouse unpaired filtering must use no-warehouse and source-unbound statuses",
                violations);
        assertContains(normalizedInventoryMapper, "selectWarehouseOptions",
                "inventory overview mapper must provide warehouse options for filters", violations);
        assertContains(normalizedInventoryMapper, "selectOfficialWarehouseOptions",
                "inventory overview mapper must provide official warehouse options for sync policy modal", violations);
        assertContains(normalizedInventoryMapper, "from seller s",
                "inventory overview seller options must come from seller management table", violations);
        assertContains(normalizedInventoryMapper, "from warehouse w",
                "inventory overview official warehouse options must come from warehouse master data", violations);
        assertContains(normalizedInventoryMapper, "selectSellerOptions",
                "inventory overview mapper must provide seller options for filters", violations);
        assertContains(normalizedInventoryMapper, "warehouseKey != null and warehouseKey != ''",
                "inventory overview mapper must support warehouse search", violations);
        assertContains(normalizedInventoryMapper, "syncModeSummary != null and syncModeSummary != ''",
                "inventory overview aggregate mapper must support sync mode filtering", violations);
        assertContains(normalizedInventoryMapper, "syncMode != null and syncMode != ''",
                "inventory overview warehouse mapper must support sync mode filtering", violations);
        assertContains(normalizedInventoryMapper, "platform_total_qty &gt;= #{platformTotalQtyMin}",
                "inventory overview mapper must support platform total quantity range filtering", violations);
        assertContains(normalizedInventoryMapper, "platform_available_qty &gt;= #{platformAvailableQtyMin}",
                "inventory overview mapper must support platform available quantity range filtering", violations);
        assertContains(normalizedInventoryMapper, "latest_stock_update_time &gt;= #{latestStockUpdateTimeStart}",
                "inventory overview aggregate mapper must support stock update time range filtering", violations);
        assertContains(normalizedInventoryMapper, "sws.source_snapshot_time &gt;= #{latestSourceSnapshotTimeStart}",
                "inventory overview warehouse mapper must support source snapshot time range filtering", violations);
        assertContains(helpers, "platformTotalQtyRange",
                "inventory overview helpers must define platform total quantity range search", violations);
        assertContains(helpers, "latestStockUpdateTimeRange",
                "inventory overview helpers must define stock update time range search", violations);
        assertContains(helpers, "buildInventoryOverviewListParams",
                "inventory overview helpers must convert search ranges into backend params", violations);
        assertContains(warehouseViewTable, "field=\"PLATFORM_TOTAL\"",
                "WarehouseViewTable must allow platform total adjustment through QuantityCell", violations);
        assertContains(warehouseViewTable, "field=\"PLATFORM_IN_TRANSIT\"",
                "WarehouseViewTable must allow platform in-transit adjustment through QuantityCell", violations);
        assertContains(spuSkuWarehouseTable, "getInventoryOverviewSpuSkuWarehouses(spuId)",
                "SpuSkuWarehouseTable must load sku warehouse groups from the inventory overview service", violations);
        assertContains(skuWarehouseTable, "getInventoryOverviewWarehouses(skuId)",
                "SkuWarehouseTable must load warehouses from the inventory overview service", violations);
        assertContains(skuWarehouseTable, "record.warehouseRefType === 'NO_WAREHOUSE'",
                "SkuWarehouseTable must gate platform total adjustment for rows without a warehouse", violations);
        assertContains(skuWarehouseTable, "record.syncMode === 'AUTO_SOURCE_AVAILABLE'",
                "SkuWarehouseTable must gate platform total adjustment for auto-synced rows", violations);
        assertContains(skuWarehouseTable, "disabled={!canAdjust || record.warehouseKind !== 'official'}",
                "SkuWarehouseTable must gate in-transit adjustment by canAdjust", violations);
        assertContains(quantityCell, "previewInventoryOverviewAdjust({",
                "QuantityCell must call adjust preview before saving", violations);
        assertContains(quantityCell, "confirmInventoryOverviewAdjust({",
                "QuantityCell must call adjust confirm through the service", violations);
        assertNotContains(quantityCell, "message.warning('请填写库存调整原因')",
                "QuantityCell must treat inventory adjustment reason as optional", violations);
        assertContains(quantityCell, "placeholder=\"调整原因（选填）\"",
                "QuantityCell must label inventory adjustment reason as optional", violations);
        assertContains(quantityCell, "reason: normalizedReason",
                "QuantityCell must persist the optional human adjustment reason, not the preview message", violations);
        assertContains(sharedInventoryAdjustButton, "previewInventoryOverviewBatchAdjust",
                "InventoryAdjustButton must preview batch adjustment before saving", violations);
        assertContains(sharedInventoryAdjustButton, "confirmInventoryOverviewBatchAdjust",
                "InventoryAdjustButton must confirm batch adjustment through the service", violations);
        assertNotContains(sharedInventoryAdjustButton, "message.warning('请填写库存调整原因')",
                "InventoryAdjustButton must treat batch adjustment reason as optional",
                violations);
        assertContains(sharedInventoryAdjustButton, "placeholder=\"调整原因（选填）\"",
                "InventoryAdjustButton must label batch adjustment reason as optional", violations);
        assertContains(sharedInventoryAdjustButton, "getInventoryOverviewSpuSkuWarehouses",
                "InventoryAdjustButton must load SPU-scoped warehouse rows", violations);
        assertContains(sharedInventoryAdjustButton, "getInventoryOverviewWarehouses",
                "InventoryAdjustButton must load SKU-scoped warehouse rows", violations);
        assertContains(sharedInventoryAdjustButton, "effectiveSourceAvailable",
                "InventoryAdjustButton must surface official warehouse source-available upper bound", violations);
        assertContains(sharedInventoryAdjustButton, "record.syncMode !== 'AUTO_SOURCE_AVAILABLE'",
                "InventoryAdjustButton must reject platform total adjustment when auto sync is active",
                violations);
        assertContains(warehouseViewTable, "InventoryAdjustButton",
                "WarehouseViewTable must expose row adjustment action", violations);
        assertContains(warehouseViewTable, "InventorySyncPolicyButton",
                "WarehouseViewTable must expose row sync policy action", violations);
        assertContains(spuSkuWarehouseTable, "showSkuAdjust",
                "SpuSkuWarehouseTable must expose sku adjustment through the flattened warehouse table", violations);
        assertContains(skuWarehouseTable, "InventoryAdjustButton",
                "SkuWarehouseTable must expose warehouse adjustment action", violations);
        assertExactSource(overviewServiceJs, "export * from './overview.ts';",
                "inventory overview JS service must be a pure TS mirror", violations);
        assertExactSource(overviewPageJs, "export { default } from './index.tsx';",
                "inventory overview JS page must be a pure TSX mirror", violations);
        assertExactSource(helpersJs, "export * from './helpers.tsx';",
                "inventory overview JS helpers must be a pure TSX mirror", violations);
        assertExactSource(quantityCellJs,
                "export { default } from './QuantityCell.tsx';",
                "QuantityCell JS mirror must forward to TSX only", violations);
        assertExactSource(inventoryAdjustButton,
                "export { default } from '@/components/InventoryAdjust/InventoryAdjustButton';",
                "InventoryAdjustButton TSX wrapper must forward to the shared inventory adjust component", violations);
        assertExactSource(inventoryAdjustButtonJs,
                "export { default } from './InventoryAdjustButton.tsx';",
                "InventoryAdjustButton JS wrapper must forward to the shared inventory adjust component", violations);
        assertExactSource(sharedInventoryAdjustButtonJs,
                "export { default, InventoryAdjustModal } from './InventoryAdjustButton.tsx';",
                "shared InventoryAdjustButton JS mirror must forward to TSX only", violations);
        assertContains(sharedInventorySyncPolicyButton, "previewInventoryOverviewSyncPolicy",
                "InventorySyncPolicyButton must preview sync policy before saving", violations);
        assertContains(sharedInventorySyncPolicyButton, "confirmInventoryOverviewSyncPolicy",
                "InventorySyncPolicyButton must confirm sync policy through the service", violations);
        assertContains(sharedInventorySyncPolicyButton, "自动同步WMS库存设置",
                "InventorySyncPolicyButton must use the confirmed seller-facing action label", violations);
        assertContains(sharedInventorySyncPolicyButton, "卖家维度",
                "InventorySyncPolicyButton must use seller dimension wording", violations);
        assertContains(sharedInventorySyncPolicyButton, "mode=\"multiple\"",
                "InventorySyncPolicyButton warehouse scope must support selecting multiple official warehouses",
                violations);
        assertContains(sharedInventorySyncPolicyButton, "getInventoryOverviewSpuList",
                "InventorySyncPolicyButton must use searchable SPU selector instead of raw id input", violations);
        assertContains(sharedInventorySyncPolicyButton, "getInventoryOverviewSkuList",
                "InventorySyncPolicyButton must use searchable SKU selector instead of raw id input", violations);
        assertContains(sharedInventorySyncPolicyButton, "getInventoryOverviewWarehouseList",
                "InventorySyncPolicyButton must use searchable stock-row selector instead of raw id input", violations);
        assertExactSource(sharedInventorySyncPolicyButtonJs,
                "export { default, InventorySyncPolicyModal } from './InventorySyncPolicyButton.tsx';",
                "shared InventorySyncPolicyButton JS mirror must forward to TSX only", violations);
        assertExactSource(skuWarehouseTableJs,
                "export { default, WarehouseStockTable } from './SkuWarehouseTable.tsx';",
                "SkuWarehouseTable JS mirror must forward to TSX only", violations);
        assertExactSource(spuSkuWarehouseTableJs,
                "export { default } from './SpuSkuWarehouseTable.tsx';",
                "SpuSkuWarehouseTable JS mirror must forward to TSX only", violations);
        assertExactSource(warehouseViewTableJs,
                "export { default } from './WarehouseViewTable.tsx';",
                "WarehouseViewTable JS mirror must forward to TSX only", violations);

        if (!violations.isEmpty())
        {
            fail("inventory overview admin route must stay on the admin namespace:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryAdjustmentReviewMustUseAdminRouteAndReturnQuantityContract() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path repoRoot = backendRoot.getParent();
        String controller = Files.readString(backendRoot.resolve(
                "inventory/src/main/java/com/ruoyi/inventory/controller/AdminInventoryAdjustmentReviewController.java"),
                StandardCharsets.UTF_8);
        String service = Files.readString(backendRoot.resolve(
                "inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryAdjustmentReviewServiceImpl.java"),
                StandardCharsets.UTF_8);
        String task = Files.readString(backendRoot.resolve(
                "inventory/src/main/java/com/ruoyi/inventory/task/InventoryAdjustmentReviewTask.java"),
                StandardCharsets.UTF_8);
        String mapper = Files.readString(backendRoot.resolve(
                "inventory/src/main/resources/mapper/inventory/InventoryAdjustmentReviewMapper.xml"),
                StandardCharsets.UTF_8);
        String overviewService = Files.readString(backendRoot.resolve(
                "inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java"),
                StandardCharsets.UTF_8);
        String reviewServiceTs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/inventory/adjustmentReview.ts"), StandardCharsets.UTF_8);
        String reviewServiceJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/inventory/adjustmentReview.js"), StandardCharsets.UTF_8);
        String reviewPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/AdjustmentReview/index.tsx"), StandardCharsets.UTF_8);
        String reviewPageJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Inventory/AdjustmentReview/index.js"), StandardCharsets.UTF_8);
        String sql = Files.readString(backendRoot.resolve(
                "sql/20260609_inventory_adjustment_review.sql"), StandardCharsets.UTF_8);
        String businessMenuSeed = Files.readString(backendRoot.resolve(
                "sql/business_menu_seed.sql"), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertContains(controller, "@RequestMapping(\"/inventory/admin/adjustment-reviews\")",
                "inventory adjustment review controller must stay under admin inventory route", violations);
        assertNotContains(controller, "@Anonymous",
                "inventory adjustment review controller must not expose anonymous handlers", violations);
        assertNotContains(controller, "@PortalPreAuthorize",
                "inventory adjustment review controller must not use portal authorization", violations);
        for (String expectedPermission : new String[] {
                "review:inventoryAdjustment:list",
                "review:inventoryAdjustment:query",
                "review:inventoryAdjustment:log",
                "review:inventoryAdjustment:effect",
                "review:inventoryAdjustment:edit",
                "review:inventoryAdjustment:reject",
                "review:inventoryAdjustment:config"
        })
        {
            assertContains(controller, "@ss.hasPermi('" + expectedPermission + "')",
                    "inventory adjustment review controller must guard " + expectedPermission, violations);
        }
        for (String expectedRoute : new String[] {
                "@GetMapping(\"/list\")",
                "@GetMapping(\"/{reviewId}\")",
                "@GetMapping(\"/{reviewId}/logs\")",
                "@PostMapping(\"/{reviewId}/effect-now\")",
                "@PostMapping(\"/{reviewId}/effective-time\")",
                "@PostMapping(\"/{reviewId}/reject\")",
                "@GetMapping(\"/policies/list\")",
                "@GetMapping(\"/policy-bindings/list\")"
        })
        {
            assertContains(controller, expectedRoute,
                    "inventory adjustment review controller must expose " + expectedRoute, violations);
        }

        assertContains(service, "requestedQty > qty(decision.getImmediateReturnableQty())",
                "review trigger must compare requested return quantity with immediate returnable quantity",
                violations);
        assertContains(service, "Math.min(requestedQty, Math.max(0, qty(before.getPlatformTotalQty())",
                "review effect must cap actual return by current platform stock minus reserved stock", violations);
        assertContains(service, "selectDueWaitingReviews(new Date(), 100)",
                "review service must process due waiting reviews in batches", violations);
        assertContains(task, "@Component(\"inventoryAdjustmentReviewTask\")",
                "inventory adjustment review Quartz task bean name must stay stable", violations);
        assertContains(mapper, "from inventory_sku_sales_daily",
                "review threshold must read SKU daily sales aggregate", violations);
        assertContains(mapper, "planned_effective_time &lt;= #{now}",
                "due review mapper must select reviews whose plan time has arrived", violations);
        assertContains(overviewService, "submitAdjustmentReview",
                "inventory overview adjustment must submit a review instead of directly updating stock when required",
                violations);

        assertContains(reviewServiceTs, "const baseUrl = '/api/inventory/admin/adjustment-reviews';",
                "inventory adjustment review TS service must call admin route", violations);
        assertContains(reviewPage, "access.hasPerms('review:inventoryAdjustment:list')",
                "inventory adjustment review page must derive list permission", violations);
        assertContains(reviewPage, "申请退回/调整",
                "inventory adjustment review page must use requested return quantity wording", violations);
        assertContains(reviewPage, "可立即退回",
                "inventory adjustment review page must show immediate returnable quantity", violations);
        assertContains(reviewPage, "保护保留",
                "inventory adjustment review page must show protected retained quantity", violations);
        assertExactSource(reviewServiceJs, "export * from './adjustmentReview.ts';",
                "inventory adjustment review JS service must be a pure TS mirror", violations);
        assertExactSource(reviewPageJs, "export { default } from './index.tsx';",
                "inventory adjustment review JS page must be a pure TSX mirror", violations);

        assertContains(sql, "@confirm_inventory_adjustment_review",
                "inventory adjustment review SQL must require explicit confirm token", violations);
        assertContains(sql, "inventory_adjustment_review_request",
                "inventory adjustment review SQL must create review request table", violations);
        assertContains(sql, "inventoryAdjustmentReviewTask.effectDueReviews",
                "inventory adjustment review SQL must seed due-effect Quartz job", violations);
        assertContains(sql, "Inventory/AdjustmentReview/index",
                "inventory adjustment review SQL must wire menu 2452 to the real page", violations);
        assertContains(businessMenuSeed, "Inventory/AdjustmentReview/index",
                "business menu seed must keep inventory adjustment review on the real page", violations);

        if (!violations.isEmpty())
        {
            fail("inventory adjustment review admin contract failed:\n" + String.join("\n", violations));
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

    private void assertExactSource(String source, String expected, String message, List<String> violations)
    {
        String normalizedSource = source.replace("\r\n", "\n").trim();
        if (!normalizedSource.equals(expected))
        {
            violations.add(message + ": expected " + expected);
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
