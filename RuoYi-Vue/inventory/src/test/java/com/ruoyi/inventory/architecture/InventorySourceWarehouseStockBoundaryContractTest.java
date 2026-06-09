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

public class InventorySourceWarehouseStockBoundaryContractTest
{
    @Test
    public void sellerOptionsMustUseSellerManagementTableInsteadOfProductOrStockRows() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String inventoryMapperXml = read(backendRoot,
                "inventory/src/main/resources/mapper/inventory/InventoryOverviewMapper.xml");
        String sellerOptionsStatement = statement(inventoryMapperXml, "selectSellerOptions");
        List<String> violations = new ArrayList<>();

        assertContains(sellerOptionsStatement, "from seller s",
                "inventory seller options must use the seller management table", violations);
        assertContains(sellerOptionsStatement, "s.seller_no",
                "inventory seller options must read seller_no from the seller management table", violations);
        assertContains(sellerOptionsStatement, "s.seller_name",
                "inventory seller options must read seller_name from the seller management table", violations);
        assertContains(sellerOptionsStatement, "s.status = '0'",
                "inventory seller options must only include enabled seller subjects", violations);
        assertNotContains(sellerOptionsStatement, "product_spu",
                "inventory seller options must not read product SPU directly", violations);
        assertNotContains(sellerOptionsStatement, "inventory_sku_warehouse_stock st",
                "inventory seller options must not join stock rows for the seller selector",
                violations);

        if (!violations.isEmpty())
        {
            fail("inventory seller option boundary contract failed:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void sourceConnectionSpuDiscoveryMustUseInventoryPortAndProductBinding() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String inventoryLookupPort = read(backendRoot,
                "inventory/src/main/java/com/ruoyi/inventory/service/InventorySourceWarehouseStockLookupService.java");
        String productLookupPort = read(backendRoot,
                "inventory/src/main/java/com/ruoyi/inventory/service/InventoryProductLookupService.java");
        String inventoryServiceImpl = read(backendRoot,
                "inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java");
        String inventoryMapper = read(backendRoot,
                "inventory/src/main/java/com/ruoyi/inventory/mapper/InventoryOverviewMapper.java");
        String inventoryMapperXml = read(backendRoot,
                "inventory/src/main/resources/mapper/inventory/InventoryOverviewMapper.xml");
        String productLookupImpl = read(backendRoot,
                "product/src/main/java/com/ruoyi/product/service/impl/ProductInventoryLookupServiceImpl.java");
        String productMapper = read(backendRoot,
                "product/src/main/java/com/ruoyi/product/mapper/ProductDistributionMapper.java");
        String productMapperXml = read(backendRoot,
                "product/src/main/resources/mapper/product/ProductDistributionMapper.xml");
        String integrationLookupImpl = read(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/service/impl/SourceWarehouseStockInventoryLookupServiceImpl.java");
        String upstreamMapper = read(backendRoot,
                "integration/src/main/java/com/ruoyi/integration/mapper/UpstreamSystemMapper.java");
        String upstreamMapperXml = read(backendRoot,
                "integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml");

        String skuDiscoveryStatement = statement(productMapperXml, "selectInventorySkuIdsBySpuId");
        String skuSnapshotBySpuStatement = statement(productMapperXml, "selectInventorySkuSnapshotsBySpuId");
        String skuSnapshotBySkuIdsStatement = statement(productMapperXml, "selectInventorySkuSnapshotsBySkuIds");
        String sourceBindingSnapshotStatement = statement(productMapperXml, "selectInventorySourceBindingSnapshotsBySpuId");
        String warehouseSnapshotStatement = statement(productMapperXml, "selectInventoryWarehouseSnapshotsBySpuId");
        String sourceKeyStatement = statement(productMapperXml, "selectInventorySourceSkuKeysBySpuId");
        String spuDiscoveryStatement = statement(productMapperXml, "selectInventorySpuIdsBySourceSkuKeys");
        String affectedSourceKeyStatement = statement(upstreamMapperXml,
                "selectAffectedOfficialMasterSourceSkuKeysByConnection");
        String officialSourceStockStatement = statement(upstreamMapperXml,
                "selectOfficialMasterSourceStocksBySourceSkuKeys");

        List<String> violations = new ArrayList<>();
        assertContains(inventoryLookupPort,
                "List<InventorySourceSkuKey> selectAffectedOfficialMasterSkuKeysByConnection(String connectionCode);",
                "inventory must own the source warehouse stock lookup port", violations);
        assertContains(inventoryLookupPort,
                "List<InventoryOfficialSourceStock> selectOfficialMasterStocksBySourceSkuKeys",
                "inventory must own the official source-stock slice lookup port", violations);
        assertContains(inventoryServiceImpl,
                "ObjectProvider<InventorySourceWarehouseStockLookupService> sourceWarehouseStockLookupService",
                "inventory overview service must consume the source lookup through ObjectProvider", violations);
        assertContains(inventoryServiceImpl,
                "selectAffectedOfficialMasterSkuKeysByConnection(connectionCode.trim())",
                "inventory overview service must request affected source SKU keys from the port", violations);
        assertContains(inventoryServiceImpl,
                "selectOfficialMasterStocksBySourceSkuKeys(sourceKeys)",
                "inventory overview service must request official source-stock slices from the source lookup port",
                violations);
        assertContains(productLookupPort, "List<Long> selectSkuIdsBySpuId(Long spuId);",
                "inventory must own the product SKU lookup port", violations);
        assertContains(productLookupPort,
                "List<InventoryProductSkuSnapshot> selectSkuSnapshotsBySpuId(Long spuId);",
                "inventory must own the product SKU snapshot lookup port", violations);
        assertContains(productLookupPort,
                "List<InventoryProductSkuSnapshot> selectSkuSnapshotsBySkuIds(List<Long> skuIds);",
                "inventory must own the SKU-id snapshot lookup port", violations);
        assertContains(productLookupPort,
                "List<InventoryProductSourceBindingSnapshot> selectSourceBindingSnapshotsBySpuId(Long spuId);",
                "inventory must own the product source binding snapshot lookup port", violations);
        assertContains(productLookupPort,
                "List<InventoryProductWarehouseSnapshot> selectWarehouseSnapshotsBySpuId(Long spuId);",
                "inventory must own the product warehouse snapshot lookup port", violations);
        assertContains(productLookupPort, "List<InventorySourceSkuKey> selectSourceSkuKeysBySpuId(Long spuId);",
                "inventory must own product source-key lookup for product refresh", violations);
        assertContains(productLookupPort,
                "List<Long> selectSpuIdsBySourceSkuKeys(List<InventorySourceSkuKey> sourceKeys);",
                "inventory must own the product source-key to SPU lookup port", violations);
        assertContains(inventoryServiceImpl,
                "ObjectProvider<InventoryProductLookupService> productLookupService",
                "inventory overview service must consume product lookups through ObjectProvider", violations);
        assertContains(inventoryServiceImpl, "requireProductLookupService().selectSpuIdsBySourceSkuKeys(sourceKeys)",
                "inventory overview service must convert source keys to SPUs through the product lookup port",
                violations);
        assertContains(inventoryServiceImpl, "selectSourceInventoryOverviewSpuIdsByConnection(connectionCode)",
                "source connection refresh must reuse the same source-key discovery path", violations);

        assertNotContains(inventoryMapper, "selectSkuIdsBySpuId",
                "inventory mapper must not keep product SKU discovery", violations);
        assertNotContains(inventoryMapper, "selectSpuIdsBySourceSkuKeys",
                "inventory mapper must not keep product source-key to SPU discovery", violations);
        assertNotContains(inventoryMapperXml, "id=\"selectSkuIdsBySpuId\"",
                "inventory mapper XML must not keep product SKU discovery SQL", violations);
        assertNotContains(inventoryMapperXml, "id=\"selectSpuIdsBySourceSkuKeys\"",
                "inventory mapper XML must not keep product source-key to SPU discovery SQL", violations);
        assertNotContains(inventoryMapper, "selectSpuIdsBySourceConnection",
                "inventory mapper must not keep the old connection-code query", violations);
        assertNotContains(inventoryMapperXml, "id=\"selectSpuIdsBySourceConnection\"",
                "inventory mapper XML must not keep the old direct source detail statement", violations);
        assertContains(productLookupImpl, "implements InventoryProductLookupService",
                "product must implement the inventory-owned product lookup port", violations);
        assertContains(productLookupImpl, "productDistributionMapper.selectInventorySkuIdsBySpuId(spuId)",
                "product lookup implementation must delegate SKU discovery to product mapper", violations);
        assertContains(productLookupImpl, "productDistributionMapper.selectInventorySkuSnapshotsBySpuId(spuId)",
                "product lookup implementation must delegate SKU snapshot discovery to product mapper", violations);
        assertContains(productLookupImpl, "productDistributionMapper.selectInventorySkuSnapshotsBySkuIds(skuIds)",
                "product lookup implementation must delegate SKU-id snapshot discovery to product mapper", violations);
        assertContains(productLookupImpl, "productDistributionMapper.selectInventorySourceBindingSnapshotsBySpuId(spuId)",
                "product lookup implementation must delegate source binding snapshots to product mapper", violations);
        assertContains(productLookupImpl, "productDistributionMapper.selectInventoryWarehouseSnapshotsBySpuId(spuId)",
                "product lookup implementation must delegate warehouse snapshots to product mapper", violations);
        assertContains(productLookupImpl, "productDistributionMapper.selectInventorySourceSkuKeysBySpuId(spuId)",
                "product lookup implementation must delegate source-key discovery to product mapper", violations);
        assertContains(productLookupImpl,
                "productDistributionMapper.selectInventorySpuIdsBySourceSkuKeys(sourceKeys)",
                "product lookup implementation must delegate source-key SPU discovery to product mapper", violations);
        assertContains(productMapper, "selectInventorySkuIdsBySpuId",
                "product mapper must expose inventory SKU discovery", violations);
        assertContains(productMapper, "selectInventorySkuSnapshotsBySpuId",
                "product mapper must expose inventory SKU snapshot discovery", violations);
        assertContains(productMapper, "selectInventorySkuSnapshotsBySkuIds",
                "product mapper must expose inventory SKU-id snapshot discovery", violations);
        assertContains(productMapper, "selectInventorySourceBindingSnapshotsBySpuId",
                "product mapper must expose inventory source binding snapshot discovery", violations);
        assertContains(productMapper, "selectInventoryWarehouseSnapshotsBySpuId",
                "product mapper must expose inventory warehouse snapshot discovery", violations);
        assertContains(productMapper, "selectInventorySourceSkuKeysBySpuId",
                "product mapper must expose inventory source-key discovery", violations);
        assertContains(productMapper, "selectInventorySpuIdsBySourceSkuKeys",
                "product mapper must expose inventory source-key SPU discovery", violations);
        assertContains(skuDiscoveryStatement, "from product_sku",
                "product SKU discovery must read product SKU facts", violations);
        assertContains(skuDiscoveryStatement, "and del_flag = '0'",
                "product SKU discovery must keep the active SKU filter", violations);
        assertContains(skuSnapshotBySpuStatement, "from product_sku sk",
                "product SKU snapshot discovery must stay in product SKU facts", violations);
        assertContains(skuSnapshotBySpuStatement, "join product_spu p",
                "product SKU snapshot discovery must include product SPU facts", violations);
        assertContains(skuSnapshotBySpuStatement, "p.seller_no as sellerNo",
                "product SKU snapshot must carry seller number for inventory read models", violations);
        assertContains(skuSnapshotBySpuStatement, "p.main_image_url as mainImageUrl",
                "product SKU snapshot must carry SPU image for inventory aggregate read models", violations);
        assertContains(skuSnapshotBySpuStatement, "where sk.spu_id = #{spuId}",
                "product SKU snapshot by SPU must stay scoped to the requested SPU", violations);
        assertContains(skuSnapshotBySkuIdsStatement, "sk.sku_id in",
                "product SKU snapshot by SKU IDs must down-push SKU ID filtering", violations);
        assertContains(sourceBindingSnapshotStatement, "join product_sku_source_binding b",
                "product source binding snapshot discovery must use product SKU bindings", violations);
        assertContains(sourceBindingSnapshotStatement, "b.master_product_name_snapshot as masterProductName",
                "product source binding snapshot must carry master product name snapshot", violations);
        assertContains(warehouseSnapshotStatement, "from product_spu_warehouse pw",
                "product warehouse snapshot discovery must use product warehouse bindings", violations);
        assertContains(warehouseSnapshotStatement, "pw.warehouse_kind as warehouseKind",
                "product warehouse snapshot must carry warehouse kind for inventory row generation", violations);
        assertContains(sourceKeyStatement, "from product_sku sk",
                "product source-key discovery must stay in the product SKU side", violations);
        assertContains(sourceKeyStatement, "join product_sku_source_binding b",
                "product source-key discovery must use product SKU source bindings", violations);
        assertContains(sourceKeyStatement, "b.binding_status = 'ACTIVE'",
                "product source-key discovery must only return active source bindings", violations);
        assertContains(sourceKeyStatement, "b.source_scope as sourceScope",
                "product source-key discovery must project source scope into the inventory-owned key DTO", violations);
        assertContains(sourceKeyStatement, "b.master_sku as masterSku",
                "product source-key discovery must project master SKU into the inventory-owned key DTO", violations);
        assertContains(sourceKeyStatement, "b.master_product_name_snapshot as masterProductName",
                "product source-key discovery must project master product name snapshot into the inventory-owned key DTO",
                violations);
        assertNotContains(sourceKeyStatement, "source_warehouse_stock_detail",
                "product source-key discovery must not directly read integration source stock detail", violations);
        assertNotContains(sourceKeyStatement, "source_warehouse_stock_group",
                "product source-key discovery must not directly read integration source stock group", violations);
        assertNotContains(sourceKeyStatement, "upstream_system_sku_inventory_snapshot",
                "product source-key discovery must not directly read upstream inventory snapshots", violations);
        assertContains(spuDiscoveryStatement, "from product_sku sk",
                "product SPU discovery must stay in the product binding side", violations);
        assertContains(spuDiscoveryStatement, "join product_sku_source_binding b",
                "product SPU discovery must use product SKU source bindings", violations);
        assertContains(spuDiscoveryStatement, "b.source_scope = #{sourceKey.sourceScope}",
                "product SPU discovery must match source scope from the integration-provided key", violations);
        assertContains(spuDiscoveryStatement, "b.master_sku = #{sourceKey.masterSku}",
                "product SPU discovery must match source SKU from the integration-provided key", violations);
        assertContains(spuDiscoveryStatement, "b.master_product_name_snapshot",
                "product SPU discovery must preserve the current product-name snapshot match", violations);
        assertNotContains(spuDiscoveryStatement, "source_warehouse_stock_detail",
                "product SPU discovery must not directly read integration source stock detail", violations);
        assertNotContains(spuDiscoveryStatement, "source_warehouse_stock_group",
                "product SPU discovery must not directly read integration source stock group", violations);
        assertNotContains(spuDiscoveryStatement, "upstream_system_sku_inventory_snapshot",
                "product SPU discovery must not directly read upstream inventory snapshots", violations);

        assertContains(integrationLookupImpl, "implements InventorySourceWarehouseStockLookupService",
                "integration must implement the inventory-owned lookup port", violations);
        assertContains(integrationLookupImpl,
                "upstreamSystemMapper.selectAffectedOfficialMasterSourceSkuKeysByConnection(connectionCode.trim())",
                "integration lookup implementation must delegate to its own mapper", violations);
        assertContains(integrationLookupImpl,
                "upstreamSystemMapper.selectOfficialMasterSourceStocksBySourceSkuKeys(sourceKeys)",
                "integration lookup implementation must delegate official source-stock slices to its own mapper",
                violations);
        assertContains(upstreamMapper, "selectAffectedOfficialMasterSourceSkuKeysByConnection",
                "integration mapper must expose affected source SKU key lookup", violations);
        assertContains(upstreamMapper, "selectOfficialMasterSourceStocksBySourceSkuKeys",
                "integration mapper must expose official source-stock slice lookup", violations);
        assertContains(affectedSourceKeyStatement, "from upstream_system_sku_inventory_snapshot s",
                "integration lookup must include current connection snapshots for pre-rebuild discovery", violations);
        assertContains(affectedSourceKeyStatement, "from source_warehouse_stock_group g",
                "integration lookup must include existing stock groups for deleted or old-source discovery", violations);
        assertContains(affectedSourceKeyStatement, "<include refid=\"SourceWarehouseStockAffectedGroupKeys\" />",
                "integration lookup must reuse the affected group key helper", violations);
        assertNotContains(affectedSourceKeyStatement, "product_sku",
                "integration source lookup must not read product SKU tables", violations);
        assertNotContains(affectedSourceKeyStatement, "product_sku_source_binding",
                "integration source lookup must not read product binding tables", violations);
        assertNotContains(affectedSourceKeyStatement, "source_stock_group_key = b.source_sku_group_key",
                "source stock keys and product source SKU group keys must not be equated", violations);
        assertContains(officialSourceStockStatement, "from source_warehouse_stock_detail d",
                "integration source-stock slice lookup must own source detail table reads", violations);
        assertContains(officialSourceStockStatement, "d.repository_scope = 'OFFICIAL_MASTER'",
                "integration source-stock slice lookup must keep official master scope", violations);
        assertContains(officialSourceStockStatement, "d.inventory_scope = 'COMPREHENSIVE'",
                "integration source-stock slice lookup must keep comprehensive inventory scope", violations);
        assertContains(officialSourceStockStatement, "d.inventory_attribute = '0'",
                "integration source-stock slice lookup must keep sellable inventory attribute", violations);
        assertContains(officialSourceStockStatement, "nullif(d.master_warehouse_name, '') is not null",
                "integration source-stock slice lookup must keep source warehouse granularity", violations);
        assertContains(officialSourceStockStatement,
                "group by d.repository_scope, d.master_sku, d.master_product_name, d.master_warehouse_name",
                "integration source-stock slice lookup must aggregate by source SKU and warehouse", violations);
        assertNotContains(officialSourceStockStatement, "product_sku",
                "integration source-stock slice lookup must not read product SKU tables", violations);
        assertNotContains(officialSourceStockStatement, "product_sku_source_binding",
                "integration source-stock slice lookup must not read product binding tables", violations);
        assertNotContains(inventoryMapperXml, "source_warehouse_stock_detail",
                "inventory mapper XML must not directly read integration source stock detail", violations);
        assertNotContains(inventoryMapperXml, "source_warehouse_stock_group",
                "inventory mapper XML must not directly read integration source stock group", violations);
        assertNotContains(inventoryMapperXml, "upstream_system_",
                "inventory mapper XML must not directly read upstream system tables", violations);
        assertNotContains(inventoryMapperXml, "product_sku",
                "inventory mapper XML must not directly read product SKU tables", violations);
        assertNotContains(inventoryMapperXml, "product_spu",
                "inventory mapper XML must not directly read product SPU tables", violations);
        assertNotContains(inventoryMapperXml, "product_spu_warehouse",
                "inventory mapper XML must not directly read product warehouse binding tables", violations);
        assertNotContains(inventoryMapperXml, "product_sku_source_binding",
                "inventory mapper XML must not directly read product source binding tables", violations);

        if (!violations.isEmpty())
        {
            fail("inventory source warehouse stock boundary contract failed:\n" + String.join("\n", violations));
        }
    }

    private String read(Path backendRoot, String relativePath) throws IOException
    {
        return Files.readString(backendRoot.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private String statement(String mapperXml, String id)
    {
        String startToken = "id=\"" + id + "\"";
        int idIndex = mapperXml.indexOf(startToken);
        if (idIndex < 0)
        {
            throw new AssertionError("Cannot locate statement id " + id);
        }
        int statementStart = mapperXml.lastIndexOf('<', idIndex);
        int statementEnd = mapperXml.indexOf("</select>", idIndex);
        if (statementStart < 0 || statementEnd < 0)
        {
            throw new AssertionError("Cannot locate select statement for " + id);
        }
        return mapperXml.substring(statementStart, statementEnd + "</select>".length());
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

    private Path findBackendRoot()
    {
        Path current = Paths.get("").toAbsolutePath();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent())
        {
            if (Files.exists(candidate.resolve("inventory/pom.xml"))
                    && Files.exists(candidate.resolve("integration/pom.xml")))
            {
                return candidate;
            }
            if (Files.exists(candidate.resolve("RuoYi-Vue/inventory/pom.xml"))
                    && Files.exists(candidate.resolve("RuoYi-Vue/integration/pom.xml")))
            {
                return candidate.resolve("RuoYi-Vue");
            }
        }
        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + current);
    }
}
