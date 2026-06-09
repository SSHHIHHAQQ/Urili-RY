package com.ruoyi.product.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class ProductDistributionMapperContractTest
{
    private static final List<String> SPU_FORBIDDEN_SOURCE_BINDING_PROPERTIES = Arrays.asList(
            "sourceBindingId",
            "sourceScope",
            "sourceSkuGroupKey",
            "sourceDimensionGroupKey",
            "masterSku",
            "masterProductNameSnapshot",
            "sourcePayloadHash",
            "wmsPayloadHash",
            "measureLengthCm",
            "measureWidthCm",
            "measureHeightCm",
            "measureWeightKg",
            "measureSource",
            "sourceWarehouseNames",
            "sourceWarehouseCount",
            "bindingStatus",
            "lockStatus",
            "lockedTime");

    private static final List<String> SOURCE_BINDING_RESULT_PROPERTIES = Arrays.asList(
            "bindingId",
            "sourceScope",
            "sourceSkuGroupKey",
            "sourceDimensionGroupKey",
            "masterSku",
            "masterProductNameSnapshot",
            "sourcePayloadHash",
            "wmsPayloadHash",
            "measureLengthCm",
            "measureWidthCm",
            "measureHeightCm",
            "measureWeightKg",
            "measureSource",
            "sourceWarehouseNames",
            "sourceWarehouseCount",
            "bindingStatus",
            "lockStatus",
            "lockedTime");

    private static final List<String> SKU_SOURCE_BINDING_RESULT_PROPERTIES = Arrays.asList(
            "sourceBindingId",
            "sourceScope",
            "sourceSkuGroupKey",
            "sourceDimensionGroupKey",
            "masterSku",
            "masterProductNameSnapshot",
            "sourcePayloadHash",
            "wmsPayloadHash",
            "measureLengthCm",
            "measureWidthCm",
            "measureHeightCm",
            "measureWeightKg",
            "measureSource",
            "sourceWarehouseNames",
            "sourceWarehouseCount",
            "bindingStatus",
            "lockStatus",
            "lockedTime");

    private static final List<String> PRODUCT_OWNED_TABLES = Arrays.asList(
            "product_spu",
            "product_sku",
            "product_attribute_value",
            "product_image",
            "product_spu_warehouse",
            "product_sku_source_binding");

    private static final Map<String, List<String>> ALLOWED_EXTERNAL_TABLES_BY_STATEMENT =
            createAllowedExternalTablesByStatement();

    private static final List<String> INVENTORY_SUMMARY_ALIASES = Arrays.asList(
            "available_stock",
            "warehouse_count",
            "inventory_status",
            "stock_update_time");

    private static final List<String> FORBIDDEN_INVENTORY_FACT_SOURCE_TOKENS = Arrays.asList(
            "source_warehouse_stock_",
            "upstream_system_sku_inventory_snapshot");

    @Test
    public void systemSpuSkuCodeGenerationMustUseRedisCodePoolWithoutDatabaseDedupLookup() throws IOException
    {
        String mapperXmlSource = readMapperSource();
        String mapperApiSource = Files.readString(findBackendRoot().resolve(
                "product/src/main/java/com/ruoyi/product/mapper/ProductDistributionMapper.java"),
                StandardCharsets.UTF_8);
        String serviceSource = Files.readString(findBackendRoot().resolve(
                "product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java"),
                StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        requireContains(serviceSource, "productCodePoolService.allocateSpuCode()", violations);
        requireContains(serviceSource, "productCodePoolService.allocateSkuCodes", violations);
        requireNotContains(serviceSource, "generateSpuCode", violations);
        requireNotContains(serviceSource, "generateSkuCode", violations);
        requireNotContains(serviceSource, "dateTimeNow(\"yyyyMMdd\")", violations);
        requireNotContains(mapperApiSource, "countSystemSpuCode", violations);
        requireNotContains(mapperApiSource, "countSystemSkuCode", violations);
        requireNotContains(mapperXmlSource, "countSystemSpuCode", violations);
        requireNotContains(mapperXmlSource, "countSystemSkuCode", violations);

        if (!violations.isEmpty())
        {
            fail("system SPU/SKU codes must be allocated from Redis code pool instead of DB lookup loops:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void spuResultMustNotMapSkuSourceBindingFields() throws IOException
    {
        String source = readMapperSource();
        String spuResult = extractResultMap(source, "ProductSpuResult");
        String sourceBindingResult = extractResultMap(source, "ProductSkuSourceBindingResult");
        List<String> violations = new ArrayList<>();

        if (spuResult.isEmpty())
        {
            violations.add("ProductSpuResult must exist");
        }
        if (sourceBindingResult.isEmpty())
        {
            violations.add("ProductSkuSourceBindingResult must exist");
        }

        for (String property : SPU_FORBIDDEN_SOURCE_BINDING_PROPERTIES)
        {
            if (spuResult.contains("property=\"" + property + "\""))
            {
                violations.add("ProductSpuResult must not map SKU binding property " + property);
            }
        }
        for (String property : SOURCE_BINDING_RESULT_PROPERTIES)
        {
            if (!sourceBindingResult.contains("property=\"" + property + "\""))
            {
                violations.add("ProductSkuSourceBindingResult must keep SKU binding property " + property);
            }
        }

        if (!violations.isEmpty())
        {
            fail("product distribution mapper must keep SPU/SKU source binding result maps separated:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void skuResultMustMapActiveSourceBindingFieldsForEditEcho() throws IOException
    {
        String source = readMapperSource();
        String skuResult = extractResultMap(source, "ProductSkuResult");
        String skuBindingSelect = extractSqlFragment(source, "skuSourceBindingSelect");
        String skuBindingJoin = extractSqlFragment(source, "skuSourceBindingJoin");
        List<String> violations = new ArrayList<>();

        if (skuResult.isEmpty())
        {
            violations.add("ProductSkuResult must exist");
        }
        for (String property : SKU_SOURCE_BINDING_RESULT_PROPERTIES)
        {
            if (!skuResult.contains("property=\"" + property + "\""))
            {
                violations.add("ProductSkuResult must map active source binding property " + property);
            }
        }
        requireContains(skuBindingSelect, "b.binding_id as source_binding_id", violations);
        requireContains(skuBindingSelect, "b.source_dimension_group_key", violations);
        requireContains(skuBindingSelect, "b.master_sku", violations);
        requireContains(skuBindingJoin, "left join product_sku_source_binding b", violations);
        requireContains(skuBindingJoin, "and b.binding_status = 'ACTIVE'", violations);

        if (!violations.isEmpty())
        {
            fail("ProductSkuResult must rehydrate active source binding fields after save:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void externalTableUsageMustStayWithinExplicitDebtAllowlist() throws IOException
    {
        Map<String, Set<String>> externalTablesByStatement = findExternalTablesByStatement(readMapperSource());
        List<String> violations = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : externalTablesByStatement.entrySet())
        {
            String statementId = entry.getKey();
            List<String> allowedTables = ALLOWED_EXTERNAL_TABLES_BY_STATEMENT.getOrDefault(statementId,
                    Collections.emptyList());
            for (String table : entry.getValue())
            {
                if (!allowedTables.contains(table))
                {
                    violations.add(statementId + " reads or writes external table " + table
                            + " without an explicit product mapper boundary allowlist entry");
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail("ProductDistributionMapper must not expand product -> source/integration/warehouse table debt:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventorySummaryFieldsMustReadInventoryOverviewModelsOnly()
            throws IOException
    {
        String source = readMapperSource();
        List<String> violations = new ArrayList<>();

        for (String alias : INVENTORY_SUMMARY_ALIASES)
        {
            if (Pattern.compile("\\bnull\\s+as\\s+" + alias + "\\b", Pattern.CASE_INSENSITIVE).matcher(source).find())
            {
                violations.add(alias + " must not stay as a null placeholder after inventory overview read models exist");
            }
        }

        String lowerSource = source.toLowerCase();
        for (String token : FORBIDDEN_INVENTORY_FACT_SOURCE_TOKENS)
        {
            if (lowerSource.contains(token))
            {
                violations.add("ProductDistributionMapper must not assemble inventory summary directly from " + token);
            }
        }

        String spuInventorySelect = extractSqlFragment(source, "spuInventorySummarySelect");
        String spuInventoryJoin = extractSqlFragment(source, "spuInventorySummaryJoin");
        String skuInventorySelect = extractSqlFragment(source, "skuInventorySummarySelect");
        String skuInventoryJoin = extractSqlFragment(source, "skuInventorySummaryJoin");
        String[] spuStatements = { "spuAggregateSelect", "spuOnSaleAggregateSelect" };
        String[] skuStatements = {
                "selectSkuPageList",
                "selectSkuListBySpuId",
                "selectSkuListBySpuIdAndSellerId",
                "selectOnSaleSkuListBySpuId",
                "selectSkuById"
        };

        requireContains(spuInventorySelect, "spu_inventory.platform_available_qty as available_stock", violations);
        requireContains(spuInventorySelect, "spu_inventory.warehouse_count as warehouse_count", violations);
        requireContains(spuInventorySelect, "spu_inventory.inventory_status as inventory_status", violations);
        requireContains(spuInventorySelect, "spu_inventory.latest_stock_update_time as stock_update_time", violations);
        requireContains(spuInventoryJoin, "left join inventory_overview_spu_read_model spu_inventory", violations);
        requireContains(spuInventoryJoin, "on spu_inventory.spu_id = p.spu_id", violations);
        requireContains(skuInventorySelect, "sku_inventory.platform_available_qty as available_stock", violations);
        requireContains(skuInventorySelect, "sku_inventory.warehouse_count as warehouse_count", violations);
        requireContains(skuInventorySelect, "sku_inventory.inventory_status as inventory_status", violations);
        requireContains(skuInventorySelect, "sku_inventory.latest_stock_update_time as stock_update_time", violations);
        requireContains(skuInventoryJoin, "left join inventory_overview_sku_read_model sku_inventory", violations);
        requireContains(skuInventoryJoin, "on sku_inventory.sku_id = sk.sku_id", violations);

        for (String statementId : spuStatements)
        {
            String statement = extractSqlFragment(source, statementId);
            requireContains(statement, "<include refid=\"spuInventorySummarySelect\"/>", violations);
            requireContains(statement, "<include refid=\"spuInventorySummaryJoin\"/>", violations);
        }
        for (String statementId : skuStatements)
        {
            String statement = extractStatement(source, statementId);
            requireContains(statement, "<include refid=\"skuInventorySummarySelect\"/>", violations);
            requireContains(statement, "<include refid=\"skuInventorySummaryJoin\"/>", violations);
        }

        if (!violations.isEmpty())
        {
            fail("ProductDistributionMapper inventory summary fields must come from inventory overview read models:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryLookupStatementsMustPreserveProductIdentitySemantics() throws IOException
    {
        String source = readMapperSource();
        String skuStatement = extractStatement(source, "selectInventorySkuIdsBySpuId");
        String skuSnapshotBySpuStatement = extractStatement(source, "selectInventorySkuSnapshotsBySpuId");
        String skuSnapshotBySkuIdsStatement = extractStatement(source, "selectInventorySkuSnapshotsBySkuIds");
        String sourceBindingSnapshotStatement = extractStatement(source, "selectInventorySourceBindingSnapshotsBySpuId");
        String warehouseSnapshotStatement = extractStatement(source, "selectInventoryWarehouseSnapshotsBySpuId");
        String sourceKeyStatement = extractStatement(source, "selectInventorySourceSkuKeysBySpuId");
        String spuStatement = extractStatement(source, "selectInventorySpuIdsBySourceSkuKeys");
        List<String> violations = new ArrayList<>();

        requireContains(skuStatement, "from product_sku", violations);
        requireContains(skuStatement, "where spu_id = #{spuId}", violations);
        requireContains(skuStatement, "and del_flag = '0'", violations);
        requireContains(skuStatement, "order by sku_id asc", violations);

        requireContains(skuSnapshotBySpuStatement, "from product_sku sk", violations);
        requireContains(skuSnapshotBySpuStatement, "join product_spu p", violations);
        requireContains(skuSnapshotBySpuStatement, "p.seller_no as sellerNo", violations);
        requireContains(skuSnapshotBySpuStatement, "p.seller_name as sellerName", violations);
        requireContains(skuSnapshotBySpuStatement, "p.system_spu_code as systemSpuCode", violations);
        requireContains(skuSnapshotBySpuStatement, "sk.system_sku_code as systemSkuCode", violations);
        requireContains(skuSnapshotBySpuStatement, "where sk.spu_id = #{spuId}", violations);
        requireContains(skuSnapshotBySpuStatement, "and sk.del_flag = '0'", violations);
        requireNotContains(skuSnapshotBySpuStatement, "source_warehouse_stock_", violations);
        requireNotContains(skuSnapshotBySpuStatement, "upstream_system_", violations);

        requireContains(skuSnapshotBySkuIdsStatement, "from product_sku sk", violations);
        requireContains(skuSnapshotBySkuIdsStatement, "sk.sku_id in", violations);
        requireContains(skuSnapshotBySkuIdsStatement, "where sk.del_flag = '0'", violations);
        requireNotContains(skuSnapshotBySkuIdsStatement, "source_warehouse_stock_", violations);
        requireNotContains(skuSnapshotBySkuIdsStatement, "upstream_system_", violations);

        requireContains(sourceBindingSnapshotStatement, "join product_sku_source_binding b", violations);
        requireContains(sourceBindingSnapshotStatement, "and b.binding_status = 'ACTIVE'", violations);
        requireContains(sourceBindingSnapshotStatement, "b.master_product_name_snapshot as masterProductName", violations);
        requireNotContains(sourceBindingSnapshotStatement, "source_warehouse_stock_", violations);
        requireNotContains(sourceBindingSnapshotStatement, "upstream_system_", violations);

        requireContains(warehouseSnapshotStatement, "from product_spu_warehouse pw", violations);
        requireContains(warehouseSnapshotStatement, "join product_spu p", violations);
        requireContains(warehouseSnapshotStatement, "pw.warehouse_kind as warehouseKind", violations);
        requireNotContains(warehouseSnapshotStatement, "source_warehouse_stock_", violations);
        requireNotContains(warehouseSnapshotStatement, "upstream_system_", violations);

        requireContains(sourceKeyStatement, "from product_sku sk", violations);
        requireContains(sourceKeyStatement, "join product_sku_source_binding b", violations);
        requireContains(sourceKeyStatement, "and b.binding_status = 'ACTIVE'", violations);
        requireContains(sourceKeyStatement, "where sk.spu_id = #{spuId}", violations);
        requireContains(sourceKeyStatement, "and sk.del_flag = '0'", violations);
        requireContains(sourceKeyStatement, "b.source_scope as sourceScope", violations);
        requireContains(sourceKeyStatement, "b.master_sku as masterSku", violations);
        requireContains(sourceKeyStatement, "b.master_product_name_snapshot as masterProductName", violations);
        requireNotContains(sourceKeyStatement, "source_warehouse_stock_", violations);
        requireNotContains(sourceKeyStatement, "upstream_system_", violations);

        requireContains(spuStatement, "from product_sku sk", violations);
        requireContains(spuStatement, "join product_sku_source_binding b", violations);
        requireContains(spuStatement, "and b.binding_status = 'ACTIVE'", violations);
        requireContains(spuStatement, "where sk.del_flag = '0'", violations);
        requireContains(spuStatement, "b.source_scope = #{sourceKey.sourceScope}", violations);
        requireContains(spuStatement, "b.master_sku = #{sourceKey.masterSku}", violations);
        requireContains(spuStatement,
                "coalesce(b.master_product_name_snapshot, '') = coalesce(#{sourceKey.masterProductName}, '')",
                violations);
        requireNotContains(spuStatement, "source_warehouse_stock_", violations);
        requireNotContains(spuStatement, "upstream_system_", violations);

        if (!violations.isEmpty())
        {
            fail("inventory product lookup statements must keep product-owned identity semantics:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerPortalSkuListMustDownPushSellerScope() throws IOException
    {
        String source = readMapperSource();
        String statement = extractStatement(source, "selectSkuListBySpuIdAndSellerId");
        List<String> violations = new ArrayList<>();

        if (statement.isEmpty())
        {
            violations.add("selectSkuListBySpuIdAndSellerId must exist for seller portal SKU detail scope");
        }
        requireContains(statement, "where sk.spu_id = #{spuId}", violations);
        requireContains(statement, "and sk.seller_id = #{sellerId}", violations);
        requireContains(statement, "and sk.del_flag = '0'", violations);

        if (!violations.isEmpty())
        {
            fail("seller portal SKU details must keep seller_id scoped at SQL level:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerPortalProductListMustKeepSellerScopeFilter() throws IOException
    {
        String source = readMapperSource();
        String statement = extractStatement(source, "selectProductList");
        List<String> violations = new ArrayList<>();

        if (statement.isEmpty())
        {
            violations.add("selectProductList must exist for seller portal product list scope");
        }
        requireContains(statement, "where p.del_flag = '0'", violations);
        requireContains(statement, "<if test=\"sellerId != null\">", violations);
        requireContains(statement, "and p.seller_id = #{sellerId}", violations);

        if (!violations.isEmpty())
        {
            fail("seller portal product list must keep seller_id scoped at SQL level:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerPortalProductDetailMustDownPushSellerScope() throws IOException
    {
        String source = readMapperSource();
        String statement = extractStatement(source, "selectProductByIdAndSellerId");
        List<String> violations = new ArrayList<>();

        if (statement.isEmpty())
        {
            violations.add("selectProductByIdAndSellerId must exist for seller portal product detail scope");
        }
        requireContains(statement, "where p.spu_id = #{spuId}", violations);
        requireContains(statement, "and p.seller_id = #{sellerId}", violations);
        requireContains(statement, "and p.del_flag = '0'", violations);

        if (!violations.isEmpty())
        {
            fail("seller portal product details must keep seller_id scoped at SQL level:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void categoryNameMustStayStableProductSnapshotWithoutLiveCategoryPathJoin() throws IOException
    {
        String mapperSource = readMapperSource();
        String serviceSource = Files.readString(findBackendRoot().resolve(
                "product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java"),
                StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        requireContains(serviceSource, "product.setCategoryName(category.getCategoryName());", violations);
        requireNotContains(serviceSource, "category.getFullPath()", violations);
        requireNotContains(mapperSource, "categoryPath", violations);
        requireNotContains(mapperSource, "category_path", violations);
        requireNotContains(mapperSource, "categoryPathSelect", violations);
        requireNotContains(mapperSource, "categoryPathJoin", violations);
        requireNotContains(mapperSource, "product_category", violations);

        if (!violations.isEmpty())
        {
            fail("product distribution categoryName must remain the product category leaf-name snapshot, "
                    + "not a live category path projection:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamSkuPairingProjectionMustBelongToIntegrationPort() throws IOException
    {
        String mapperSource = readMapperSource();
        String serviceSource = Files.readString(findBackendRoot().resolve(
                "product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java"),
                StandardCharsets.UTF_8);
        String integrationServiceApi = Files.readString(findBackendRoot().resolve(
                "integration/src/main/java/com/ruoyi/integration/service/ISourceSkuPairingProjectionService.java"),
                StandardCharsets.UTF_8);
        String integrationMapperSource = Files.readString(findBackendRoot().resolve(
                "integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml"),
                StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        requireNotContains(mapperSource, "upstream_system_sku_pairing", violations);
        requireNotContains(mapperSource, "upstream_system_warehouse_pairing", violations);
        requireNotContains(mapperSource, "selectOfficialWarehousesBySourceDimensionGroup", violations);
        requireNotContains(mapperSource, "source_product_dimension_group", violations);
        requireNotContains(mapperSource, "source_product_warehouse_detail", violations);
        requireNotContains(mapperSource, "deleteUpstreamSkuPairingsBySystemSkuAndConnectionCodes", violations);
        requireNotContains(mapperSource, "upsertUpstreamSkuPairingsForBinding", violations);

        requireContains(serviceSource, "ObjectProvider<ISourceSkuPairingProjectionService>", violations);
        requireContains(serviceSource, "sourceSkuPairingProjectionService().deletePairingsBySystemSkuAndConnectionCodes",
                violations);
        requireContains(serviceSource, "sourceSkuPairingProjectionService().upsertPairingsForProjection(toProjection(binding))",
                violations);
        requireContains(serviceSource, "sourceSkuPairingProjectionService().selectOfficialWarehousesBySourceDimensionGroup",
                violations);
        requireContains(serviceSource, "sourceSkuPairingProjectionService().selectOfficialSourceBindingSnapshot",
                violations);

        requireContains(integrationServiceApi, "deletePairingsBySystemSkuAndConnectionCodes", violations);
        requireContains(integrationServiceApi, "upsertPairingsForProjection", violations);
        requireContains(integrationServiceApi, "selectOfficialWarehousesBySourceDimensionGroup", violations);
        requireContains(integrationServiceApi, "selectOfficialSourceBindingSnapshot", violations);

        requireContains(integrationMapperSource, "delete from upstream_system_sku_pairing", violations);
        requireContains(integrationMapperSource, "connection_code in", violations);
        requireContains(integrationMapperSource, "selectOfficialSourceBindingSnapshot", violations);
        requireContains(integrationMapperSource, "from source_product_dimension_group d", violations);
        requireContains(integrationMapperSource, "from source_product_warehouse_detail wd", violations);
        requireContains(integrationMapperSource, "inner join upstream_system_warehouse_pairing p", violations);
        requireContains(integrationMapperSource,
                "p.pairing_role = case when c.settlement_type = 'self-operated-receivable'", violations);
        requireNotContains(integrationMapperSource, "p.pairing_role = 'FULFILLMENT'", violations);
        requireContains(integrationMapperSource, "inner join warehouse w", violations);

        if (!violations.isEmpty())
        {
            fail("ProductDistributionMapper must access integration/warehouse storage through integration port:\n"
                    + String.join("\n", violations));
        }
    }

    private static Map<String, List<String>> createAllowedExternalTablesByStatement()
    {
        Map<String, List<String>> allowlist = new LinkedHashMap<>();
        allowlist.put("selectProductList", Arrays.asList(
                "inventory_overview_spu_read_model"));
        allowlist.put("selectOnSaleProductList", Arrays.asList(
                "inventory_overview_spu_read_model"));
        allowlist.put("selectProductById", Arrays.asList(
                "inventory_overview_spu_read_model"));
        allowlist.put("selectProductByIdAndSellerId", Arrays.asList(
                "inventory_overview_spu_read_model"));
        allowlist.put("selectOnSaleProductById", Arrays.asList(
                "inventory_overview_spu_read_model"));
        allowlist.put("selectSkuPageList", Arrays.asList(
                "inventory_overview_sku_read_model"));
        allowlist.put("selectSkuListBySpuId", Arrays.asList(
                "inventory_overview_sku_read_model"));
        allowlist.put("selectSkuListBySpuIdAndSellerId", Arrays.asList(
                "inventory_overview_sku_read_model"));
        allowlist.put("selectOnSaleSkuListBySpuId", Arrays.asList(
                "inventory_overview_sku_read_model"));
        allowlist.put("selectSkuById", Arrays.asList(
                "inventory_overview_sku_read_model"));
        return allowlist;
    }

    private Map<String, Set<String>> findExternalTablesByStatement(String source)
    {
        Map<String, Set<String>> externalTablesByStatement = new LinkedHashMap<>();
        Map<String, String> sqlFragmentsById = extractSqlFragmentsById(source);
        Pattern statementPattern = Pattern.compile(
                "<(select|insert|update|delete)\\s+[^>]*id=\"([^\"]+)\"[^>]*>(.*?)</\\1>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher statementMatcher = statementPattern.matcher(source);

        while (statementMatcher.find())
        {
            String statementType = statementMatcher.group(1).toLowerCase();
            String statementId = statementMatcher.group(2);
            String statementBody = expandIncludes(statementMatcher.group(3), sqlFragmentsById,
                    new LinkedHashSet<>());
            Set<String> externalTables = new LinkedHashSet<>();
            for (String table : findTables(statementType, statementBody))
            {
                if (!PRODUCT_OWNED_TABLES.contains(table))
                {
                    externalTables.add(table);
                }
            }
            if (!externalTables.isEmpty())
            {
                externalTablesByStatement.put(statementId, externalTables);
            }
        }

        return externalTablesByStatement;
    }

    private Map<String, String> extractSqlFragmentsById(String source)
    {
        Map<String, String> fragmentsById = new LinkedHashMap<>();
        Pattern fragmentPattern = Pattern.compile(
                "<sql\\s+[^>]*id=\"([^\"]+)\"[^>]*>(.*?)</sql>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher fragmentMatcher = fragmentPattern.matcher(source);
        while (fragmentMatcher.find())
        {
            fragmentsById.put(fragmentMatcher.group(1), fragmentMatcher.group(2));
        }
        return fragmentsById;
    }

    private String expandIncludes(String source, Map<String, String> sqlFragmentsById, Set<String> includeStack)
    {
        Pattern includePattern = Pattern.compile("<include\\s+[^>]*refid=\"([^\"]+)\"[^>]*/?>",
                Pattern.CASE_INSENSITIVE);
        Matcher includeMatcher = includePattern.matcher(source);
        StringBuffer expandedSource = new StringBuffer();
        while (includeMatcher.find())
        {
            String refid = includeMatcher.group(1);
            String fragment = sqlFragmentsById.get(refid);
            if (fragment == null)
            {
                includeMatcher.appendReplacement(expandedSource, Matcher.quoteReplacement(includeMatcher.group()));
                continue;
            }
            if (!includeStack.add(refid))
            {
                throw new IllegalStateException("recursive SQL include found: " + refid);
            }
            String expandedFragment = expandIncludes(fragment, sqlFragmentsById, includeStack);
            includeStack.remove(refid);
            includeMatcher.appendReplacement(expandedSource, Matcher.quoteReplacement(expandedFragment));
        }
        includeMatcher.appendTail(expandedSource);
        return expandedSource.toString();
    }

    private Set<String> findTables(String statementType, String statementBody)
    {
        Set<String> tables = new LinkedHashSet<>();
        Pattern tablePattern = Pattern.compile(
                "\\b(?:from|join|insert\\s+into|delete\\s+from)\\s+`?([a-zA-Z][a-zA-Z0-9_]*)`?",
                Pattern.CASE_INSENSITIVE);
        Matcher tableMatcher = tablePattern.matcher(statementBody);
        while (tableMatcher.find())
        {
            tables.add(tableMatcher.group(1));
        }

        if ("update".equals(statementType))
        {
            Matcher updateMatcher = Pattern.compile("^\\s*update\\s+`?([a-zA-Z][a-zA-Z0-9_]*)`?",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(statementBody);
            if (updateMatcher.find())
            {
                tables.add(updateMatcher.group(1));
            }
        }

        return tables;
    }

    private String extractSqlFragment(String source, String id)
    {
        Pattern fragmentPattern = Pattern.compile(
                "<sql\\s+[^>]*id=\"" + Pattern.quote(id) + "\"[^>]*>.*?</sql>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = fragmentPattern.matcher(source);
        if (!matcher.find())
        {
            return "";
        }
        return matcher.group();
    }

    private String extractStatement(String source, String id)
    {
        Pattern statementPattern = Pattern.compile(
                "<(select|insert|update|delete)\\s+[^>]*id=\"" + Pattern.quote(id) + "\"[^>]*>.*?</\\1>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = statementPattern.matcher(source);
        if (!matcher.find())
        {
            return "";
        }
        return matcher.group();
    }

    private void requireContains(String source, String expected, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add("missing required SQL fragment: " + expected);
        }
    }

    private void requireNotContains(String source, String forbidden, List<String> violations)
    {
        if (source.contains(forbidden))
        {
            violations.add("forbidden SQL/source fragment found: " + forbidden);
        }
    }

    private String extractResultMap(String source, String id)
    {
        String openToken = "<resultMap ";
        String idToken = "id=\"" + id + "\"";
        int searchFrom = 0;
        while (true)
        {
            int openStart = source.indexOf(openToken, searchFrom);
            if (openStart < 0)
            {
                return "";
            }
            int tagEnd = source.indexOf('>', openStart);
            if (tagEnd < 0)
            {
                return "";
            }
            String openTag = source.substring(openStart, tagEnd + 1);
            if (openTag.contains(idToken))
            {
                String closeToken = "</resultMap>";
                int closeStart = source.indexOf(closeToken, tagEnd);
                if (closeStart < 0)
                {
                    return "";
                }
                return source.substring(openStart, closeStart + closeToken.length());
            }
            searchFrom = tagEnd + 1;
        }
    }

    private String readMapperSource() throws IOException
    {
        Path mapper = findBackendRoot().resolve(
                "product/src/main/resources/mapper/product/ProductDistributionMapper.xml");
        return Files.readString(mapper, StandardCharsets.UTF_8);
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
            if (Files.isDirectory(candidate.resolve("product/src/main/resources/mapper/product")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
