package com.ruoyi.inventory.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class InventoryOverviewRefreshContractTest
{
    @Test
    public void inventoryOverviewRuntimeRefreshMustRebuildWarehouseRowsAndReadModels() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String serviceInterface = read(backendRoot,
                "inventory/src/main/java/com/ruoyi/inventory/service/IInventoryOverviewService.java");
        String serviceImpl = read(backendRoot,
                "inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java");
        String syncPolicyServiceInterface = read(backendRoot,
                "inventory/src/main/java/com/ruoyi/inventory/service/IInventoryStockSyncPolicyService.java");
        String syncPolicyServiceImpl = read(backendRoot,
                "inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryStockSyncPolicyServiceImpl.java");
        String mapper = read(backendRoot,
                "inventory/src/main/java/com/ruoyi/inventory/mapper/InventoryOverviewMapper.java");
        String mapperXml = read(backendRoot,
                "inventory/src/main/resources/mapper/inventory/InventoryOverviewMapper.xml");
        String syncPolicyMapperXml = read(backendRoot,
                "inventory/src/main/resources/mapper/inventory/InventoryStockSyncPolicyMapper.xml");
        String productService = read(backendRoot,
                "product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java");
        String upstreamSystemService = read(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java");
        String upstreamSyncService = read(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSyncServiceImpl.java");
        String upstreamInventorySyncComponent = read(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/sync/UpstreamInventorySyncComponent.java");
        String sourceReadModelRefreshService = read(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/service/ISourceReadModelRefreshService.java");
        String sourceReadModelRefreshServiceImpl = read(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/service/impl/SourceReadModelRefreshServiceImpl.java");
        String upstreamMapper = read(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/mapper/UpstreamSystemMapper.java");
        String upstreamMapperXml = read(backendRoot,
                "integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml");
        String productRefreshMethod = sourceBetween(serviceImpl,
                "public void refreshProductInventoryOverview(Long spuId)",
                "public List<Long> selectSourceInventoryOverviewSpuIdsByConnection");
        List<String> violations = new ArrayList<>();

        assertContains(serviceInterface, "void refreshProductInventoryOverview(Long spuId);",
                "inventory overview service must expose product refresh", violations);
        assertContains(serviceInterface, "List<Long> selectSourceInventoryOverviewSpuIdsByConnection(String connectionCode);",
                "inventory overview service must expose pre-rebuild source SPU capture", violations);
        assertContains(serviceImpl, "ObjectProvider<InventorySourceWarehouseStockLookupService> sourceWarehouseStockLookupService",
                "inventory overview service must discover source warehouse stock lookup through the inventory-owned port",
                violations);
        assertContains(serviceImpl, "ObjectProvider<InventoryProductLookupService> productLookupService",
                "inventory overview service must discover product SKU/SPU lookups through the inventory-owned port",
                violations);
        assertContains(serviceImpl, "productLookup.selectSkuSnapshotsBySpuId(spuId)",
                "inventory overview service must refresh SKU read models through the product snapshot port",
                violations);
        assertContains(serviceImpl, "productLookup.selectSourceBindingSnapshotsBySpuId(spuId)",
                "inventory overview service must receive source bindings as product-owned snapshots",
                violations);
        assertContains(serviceImpl, "productLookup.selectWarehouseSnapshotsBySpuId(spuId)",
                "inventory overview service must receive product warehouse bindings as product-owned snapshots",
                violations);
        assertContains(serviceImpl, "productLookup.selectSourceSkuKeysBySpuId(spuId)",
                "inventory overview service must ask product for active source keys before loading source stock slices",
                violations);
        assertContains(serviceImpl, "selectOfficialMasterStocksBySourceSkuKeys(sourceKeys)",
                "inventory overview service must ask integration for official source stock slices through the port",
                violations);
        assertContains(serviceImpl,
                "deleteObsoleteSkuWarehouseStocksBySpuId(spuId, skuSnapshots, sourceBindings,",
                "inventory overview service must pass product snapshots into obsolete-stock deletion", violations);
        assertContains(serviceImpl,
                "upsertOfficialMasterSkuWarehouseStocksBySpuId(spuId, operator, skuSnapshots,",
                "inventory overview service must pass product snapshots into official warehouse upsert", violations);
        assertContains(serviceImpl,
                "upsertUnmatchedOfficialSkuWarehouseStocksBySpuId(spuId, operator, skuSnapshots,",
                "inventory overview service must pass product snapshots into unmatched official warehouse upsert",
                violations);
        assertContains(serviceImpl, "requireProductLookupService().selectSpuIdsBySourceSkuKeys(sourceKeys)",
                "inventory overview service must convert source keys to SPUs through the product lookup port",
                violations);
        assertContains(serviceImpl, "selectAffectedOfficialMasterSkuKeysByConnection(connectionCode.trim())",
                "inventory overview service must ask the source warehouse lookup port for affected source SKU keys",
                violations);
        assertContains(serviceInterface, "void refreshSourceInventoryOverviewByConnection(String connectionCode);",
                "inventory overview service must expose source-connection refresh", violations);
        assertContains(serviceInterface,
                "void refreshSourceInventoryOverviewByConnection(String connectionCode, List<Long> preRebuildSpuIds);",
                "inventory overview service must accept pre-rebuild source SPUs", violations);
        for (String mapperMethod : new String[] {
                "deleteObsoleteSkuWarehouseStocksBySpuId",
                "upsertOfficialMasterSkuWarehouseStocksBySpuId",
                "upsertSourceUnboundSkuWarehouseStocksBySpuId",
                "upsertUnmatchedOfficialSkuWarehouseStocksBySpuId",
                "upsertThirdPartySkuWarehouseStocksBySpuId",
                "upsertNoWarehouseSkuWarehouseStocksBySpuId"
        })
        {
            assertContains(mapper, mapperMethod, "inventory overview mapper must declare " + mapperMethod, violations);
            assertContains(mapperXml, "id=\"" + mapperMethod + "\"",
                    "inventory overview mapper XML must implement " + mapperMethod, violations);
            assertContains(serviceImpl, mapperMethod,
                    "inventory overview service must call " + mapperMethod, violations);
        }
        assertContains(mapperXml, "<sql id=\"currentSkuWarehouseStockKeysBySpu\">",
                "inventory overview mapper must derive current stock keys before deleting obsolete rows", violations);
        assertContains(mapperXml, "stock_key not in",
                "inventory overview mapper must delete stale stock rows by current stock keys", violations);
        assertContains(mapperXml, "on duplicate key update",
                "inventory overview mapper must preserve matching stock rows through upsert", violations);
        assertNotContains(mapperXml, "source_stock_group_key = b.source_sku_group_key",
                "source stock read-model keys and source product binding keys are different domains", violations);
        assertNotContains(mapperXml, "source_warehouse_stock_detail",
                "inventory overview mapper must not read integration source stock detail directly", violations);
        assertNotContains(mapperXml, "product_sku",
                "inventory overview mapper must not read product SKU tables directly", violations);
        assertNotContains(mapperXml, "product_spu",
                "inventory overview mapper must not read product SPU tables directly", violations);
        assertNotContains(mapperXml, "product_spu_warehouse",
                "inventory overview mapper must not read product warehouse bindings directly", violations);
        assertNotContains(mapperXml, "product_sku_source_binding",
                "inventory overview mapper must not read product source bindings directly", violations);
        assertContains(mapperXml, "src.master_sku = b.master_sku",
                "source inventory overview must match stock rows by source SKU identity", violations);
        assertContains(mapperXml, "b.master_product_name",
                "source inventory overview must match stock rows by source product name snapshot", violations);
        assertAppearsBefore(mapperXml, "deleteObsoleteSkuWarehouseStocksBySpuId",
                "upsertOfficialMasterSkuWarehouseStocksBySpuId",
                "inventory overview mapper must delete obsolete rows before upserting current rows", violations);
        assertAppearsBefore(productRefreshMethod,
                "deleteObsoleteSkuWarehouseStocksBySpuId(spuId, skuSnapshots, sourceBindings,",
                "refreshSkuReadModel(skuSnapshot.getSkuId(), skuSnapshots)",
                "inventory overview service must rebuild stock rows before refreshing SKU read model", violations);
        assertContains(syncPolicyServiceInterface, "InventoryStockSyncPolicyPreviewResult previewSyncPolicy",
                "inventory sync policy service must expose preview before saving", violations);
        assertContains(syncPolicyServiceInterface, "InventoryStockSyncPolicyPreviewResult confirmSyncPolicy",
                "inventory sync policy service must expose confirmed policy save", violations);
        assertContains(syncPolicyServiceInterface, "void applyAutoSyncForSpu(Long spuId, String operator);",
                "inventory sync policy service must expose source-refresh auto sync hook", violations);
        assertContains(serviceImpl, "IInventoryStockSyncPolicyService inventoryStockSyncPolicyService",
                "inventory overview service must depend on sync policy service through the public interface",
                violations);
        assertContains(productRefreshMethod, "inventoryStockSyncPolicyService.applyAutoSyncForSpu(spuId, operator)",
                "inventory overview refresh must apply stock sync policy after rebuilding warehouse rows", violations);
        assertAppearsBefore(productRefreshMethod, "inventoryStockSyncPolicyService.applyAutoSyncForSpu(spuId, operator)",
                "refreshSkuReadModel(skuSnapshot.getSkuId(), skuSnapshots)",
                "inventory overview refresh must apply auto WMS sync before refreshing permanent read models",
                violations);
        assertContains(syncPolicyServiceImpl, "effectiveSourceAvailable(row)",
                "inventory sync policy must calculate auto-sync stock from source available minus pending source deduction",
                violations);
        assertContains(syncPolicyServiceImpl, "targetTotal = Math.max(effectiveSourceAvailable, qty(row.getPlatformReservedQty()))",
                "inventory sync policy must not reduce platform total below reserved stock", violations);
        assertContains(syncPolicyServiceImpl, "SYNC_MODE_AUTO_SOURCE_AVAILABLE",
                "inventory sync policy must use a source-neutral auto available mode code", violations);
        assertContains(mapperXml, "sync_mode_summary",
                "inventory overview read models must persist sync mode summary for aggregate views", violations);
        assertContains(mapperXml, "sync_policy_scope_summary",
                "inventory overview read models must persist sync policy scope summary for aggregate views", violations);
        assertContains(syncPolicyMapperXml, "inventory_stock_sync_policy",
                "inventory sync policy mapper must read and write policy table", violations);
        assertContains(serviceImpl, "Set<Long> spuIds = new LinkedHashSet<>();",
                "inventory overview source refresh must de-duplicate pre and post rebuild SPUs", violations);
        assertContains(serviceImpl, "preRebuildSpuIds",
                "inventory overview source refresh must include SPUs captured before source stock rebuild", violations);
        assertContains(serviceImpl, "spuIds.add(spuId);",
                "inventory overview source refresh must merge affected SPUs before refreshing products", violations);

        assertContains(productService, "ObjectProvider<IInventoryOverviewService> inventoryOverviewService",
                "product service must use inventory overview public service as optional dependency", violations);
        if (count(productService, "refreshInventoryOverviewAfterCommit(product.getSpuId())") < 2)
        {
            violations.add("product insert and update flows must schedule inventory overview refresh after saving SKU/warehouse data");
        }
        assertContains(productService, "TransactionSynchronizationManager.registerSynchronization",
                "product save read-model refresh must be registered after transaction commit", violations);
        assertContains(productService, "readModelRefreshExecutor.execute",
                "product save read-model refresh must run asynchronously after commit", violations);
        assertContains(productService, "isSourcePairingProjectionChanged(currentBinding, binding)",
                "product source pairing refresh must skip unchanged bindings", violations);
        assertContains(productService, "refreshSourcePairingProjectionReadModels(affectedConnectionCodes)",
                "product pairing projection writes must refresh source-side read models by affected connection", violations);
        assertContains(productService, "refreshOfficialMasterSkuPairingByConnection(connectionCode)",
                "product service must call the integration facade for SKU pairing projection refreshes", violations);

        assertContains(upstreamMapper, "refreshInventorySnapshotWarehousePairingByConnection",
                "integration mapper must expose warehouse pairing snapshot refresh", violations);
        assertContains(upstreamMapper, "refreshInventorySnapshotSkuPairingByConnection",
                "integration mapper must expose SKU pairing snapshot refresh", violations);
        assertContains(upstreamMapperXml, "id=\"refreshInventorySnapshotWarehousePairingByConnection\"",
                "integration mapper XML must update warehouse pairing fields in inventory snapshots", violations);
        assertContains(upstreamMapperXml, "p.pairing_role = case when c.settlement_type = 'self-operated-receivable'",
                "inventory snapshot warehouse pairing refresh must honor QUOTE settlement role", violations);
        assertNotContains(upstreamMapperXml, "p.pairing_role = 'FULFILLMENT'",
                "inventory/source projections must not hard-code only FULFILLMENT pairing role", violations);
        assertContains(upstreamMapperXml, "id=\"refreshInventorySnapshotSkuPairingByConnection\"",
                "integration mapper XML must update SKU pairing fields in inventory snapshots", violations);
        assertContains(upstreamInventorySyncComponent, "String expectedPairingRole = pairingRoleForConnection(connectionCode)",
                "inventory sync must derive warehouse pairing role from connection settlement type", violations);
        assertContains(upstreamInventorySyncComponent,
                "UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE.equals(connection.getSettlementType())",
                "inventory sync must support QUOTE pairing role for self-operated receivable connections", violations);
        assertContains(upstreamInventorySyncComponent, "return UpstreamSystemConstants.PAIRING_ROLE_QUOTE",
                "inventory sync must map self-operated receivable connections to QUOTE", violations);
        assertContains(upstreamSystemService, "refreshInventorySnapshotWarehousePairingByConnection(connectionCode)",
                "warehouse pairing changes must refresh inventory snapshots", violations);
        assertContains(upstreamSystemService, "refreshInventorySnapshotSkuPairingByConnection(connectionCode)",
                "SKU pairing changes must refresh inventory snapshots", violations);
        assertContains(upstreamSystemService, "sourceWarehouseStockReadModelService.rebuildOfficialMasterByConnection",
                "pairing changes must rebuild source warehouse stock read models", violations);
        if (count(upstreamSystemService, "List<Long> affectedSpuIds = selectSourceInventoryOverviewSpuIds(connectionCode)") < 4)
        {
            violations.add("connection info, warehouse pairing, and SKU pairing changes must capture source SPUs before rebuild");
        }
        assertContains(upstreamSystemService, "refreshSourceInventoryOverview(connectionCode, affectedSpuIds)",
                "upstream system service must refresh source inventory with pre-rebuild SPUs", violations);
        if (count(upstreamSystemService, "refreshSourceInventoryOverview(connectionCode, affectedSpuIds)") < 4)
        {
            violations.add("connection info, warehouse pairing, and SKU pairing changes must refresh inventory overview");
        }
        assertContains(upstreamSystemService, "refreshSourceInventoryOverview(current.getConnectionCode(), affectedSpuIds)",
                "SKU pairing delete must refresh inventory overview with the deleted pairing connection", violations);
        assertContains(upstreamSyncService, "refreshSourceInventoryOverview(connectionCode, affectedSpuIds)",
                "inventory sync must refresh inventory overview after rebuilding source stock read model", violations);
        assertContains(upstreamSyncService, "List<Long> affectedSpuIds = selectSourceInventoryOverviewSpuIds(connectionCode)",
                "inventory sync must capture source SPUs before source stock rebuild", violations);
        assertContains(upstreamSyncService, "refreshSourceInventoryOverview(connectionCode, affectedSpuIds)",
                "inventory sync must refresh source inventory with pre-rebuild SPUs", violations);
        assertContains(sourceReadModelRefreshService,
                "int refreshOfficialMasterSkuPairingByConnection(String connectionCode);",
                "integration source read-model facade must expose SKU pairing projection refresh", violations);
        assertContains(sourceReadModelRefreshServiceImpl,
                "refreshInventorySnapshotSkuPairingByConnection(connectionCode)",
                "integration facade must refresh inventory SKU pairing snapshots", violations);
        assertContains(sourceReadModelRefreshServiceImpl,
                "sourceWarehouseStockReadModelService.rebuildOfficialMasterByConnection(connectionCode)",
                "integration facade must rebuild source warehouse stock read models", violations);
        assertContains(sourceReadModelRefreshServiceImpl,
                "List<Long> affectedSpuIds = selectSourceInventoryOverviewSpuIds(connectionCode)",
                "integration facade must capture source SPUs before source stock rebuild", violations);
        assertContains(sourceReadModelRefreshServiceImpl,
                "refreshSourceInventoryOverviewByConnection(connectionCode, affectedSpuIds)",
                "integration facade must refresh source inventory overview after pairing projection writes", violations);

        if (!violations.isEmpty())
        {
            fail("inventory overview runtime refresh contract failed:\n" + String.join("\n", violations));
        }
    }

    private String read(Path backendRoot, String relativePath) throws IOException
    {
        return Files.readString(backendRoot.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private void assertContains(String source, String expected, String message, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(message + ": missing " + expected);
        }
    }

    private void assertNotContains(String source, String unexpected, String message, List<String> violations)
    {
        if (source.contains(unexpected))
        {
            violations.add(message + ": unexpected " + unexpected);
        }
    }

    private void assertAppearsBefore(String source, String first, String second, String message, List<String> violations)
    {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        if (firstIndex < 0 || secondIndex < 0 || firstIndex >= secondIndex)
        {
            violations.add(message + ": expected " + first + " before " + second);
        }
    }

    private String sourceBetween(String source, String start, String end)
    {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex)
        {
            throw new AssertionError("Cannot locate source block from " + start + " to " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private int count(String source, String expected)
    {
        int count = 0;
        int index = source.indexOf(expected);
        while (index >= 0)
        {
            count++;
            index = source.indexOf(expected, index + expected.length());
        }
        return count;
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
                    && Files.isDirectory(candidate.resolve("product/src/main/java"))
                    && Files.isDirectory(candidate.resolve("integration/src/main/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
