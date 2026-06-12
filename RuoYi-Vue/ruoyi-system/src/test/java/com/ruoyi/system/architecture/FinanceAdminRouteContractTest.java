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

public class FinanceAdminRouteContractTest
{
    @Test
    public void financeCurrencyControllerAndFrontendServiceMustUseAdminRoute() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path repoRoot = backendRoot.getParent();
        String controller = Files.readString(backendRoot.resolve(
                "finance/src/main/java/com/ruoyi/finance/controller/AdminCurrencyController.java"),
                StandardCharsets.UTF_8);
        String currencyServiceTs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/finance/currency.ts"), StandardCharsets.UTF_8);
        String currencyServiceJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/finance/currency.js"), StandardCharsets.UTF_8);
        String currencyPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Finance/Currency/index.tsx"), StandardCharsets.UTF_8);
        String currencyPageJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Finance/Currency/index.js"), StandardCharsets.UTF_8);
        String currencyConstantsJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Finance/Currency/constants.js"), StandardCharsets.UTF_8);
        String syncSettingsPanel = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Finance/Currency/components/SyncSettingsPanel.tsx"), StandardCharsets.UTF_8);
        String syncSettingsPanelJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Finance/Currency/components/SyncSettingsPanel.js"), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertContains(controller, "@RequestMapping(\"/finance/admin\")",
                "AdminCurrencyController must stay under /finance/admin", violations);
        assertNotContains(controller, "@RequestMapping(\"/finance\")",
                "AdminCurrencyController must not expose the admin surface under /finance", violations);
        assertAdminOnlyController(controller, "AdminCurrencyController", violations);

        for (String expectedPermission : new String[] {
                "finance:currency:list",
                "finance:currency:query",
                "finance:currency:add",
                "finance:currency:edit",
                "finance:currency:remove",
                "finance:currency:syncConfig",
                "finance:currency:sync",
                "finance:currency:log"
        })
        {
            assertContains(controller, expectedPermission,
                    "AdminCurrencyController must guard " + expectedPermission, violations);
        }

        for (String mutatingMapping : new String[] {
                "@PostMapping(\"/currencies\")",
                "@PutMapping(\"/currencies/{currencyCode}\")",
                "@PutMapping(\"/currencies/{currencyCode}/status\")",
                "@DeleteMapping(\"/currencies/{currencyCode}\")",
                "@PutMapping(\"/currency-sync-config\")",
                "@PostMapping(\"/currency-sync-config/test\")",
                "@PostMapping(\"/currency-sync-config/sync\")"
        })
        {
            assertContains(controller, mutatingMapping,
                    "AdminCurrencyController must keep mutating route " + mutatingMapping, violations);
        }

        assertContains(currencyServiceTs, "const baseUrl = '/api/finance/admin';",
                "finance currency TS service must call the admin route", violations);
        assertEqualsTrimmed(currencyServiceJs, "export * from './currency.ts';",
                "finance currency JS service mirror must be a pure TS re-export", violations);
        assertEqualsTrimmed(currencyPageJs, "export { default } from './index.tsx';",
                "finance currency page JS mirror must be a pure TSX re-export", violations);
        assertEqualsTrimmed(currencyConstantsJs, "export * from './constants.ts';",
                "finance currency constants JS mirror must be a pure TS re-export", violations);
        assertEqualsTrimmed(syncSettingsPanelJs, "export { default } from './SyncSettingsPanel.tsx';",
                "finance sync settings panel JS mirror must be a pure TSX re-export", violations);
        assertContains(currencyPage, "access.hasPerms('finance:currency:add')",
                "finance currency page must gate add action", violations);
        assertContains(currencyPage, "access.hasPerms('finance:currency:edit')",
                "finance currency page must gate edit action", violations);
        assertContains(currencyPage, "access.hasPerms('finance:currency:remove')",
                "finance currency page must gate remove action", violations);
        assertContains(currencyPage, "const canViewRateHistory = access.hasPerms('finance:currency:query')",
                "finance currency page must gate rate history query action", violations);
        assertContains(currencyPage, "const canViewSyncTab = access.hasPerms('finance:currency:syncConfig')",
                "finance currency page must gate sync tab by sync permissions", violations);
        assertContains(syncSettingsPanel, "const canViewSyncConfig = access.hasPerms('finance:currency:syncConfig')",
                "finance sync settings panel must gate sync config action", violations);
        assertContains(syncSettingsPanel, "const canViewSyncLog = access.hasPerms('finance:currency:log')",
                "finance sync settings panel must gate sync log requests", violations);
        assertContains(syncSettingsPanel, "const canSyncCurrency = access.hasPerms('finance:currency:sync')",
                "finance sync settings panel must gate sync/test actions", violations);
        assertContains(syncSettingsPanel, "if (!canViewSyncConfig)",
                "finance sync settings panel must not load sync config without permission", violations);
        assertContains(syncSettingsPanel, "{canViewSyncLog ? (",
                "finance sync settings panel must not mount sync log table without permission", violations);

        if (!violations.isEmpty())
        {
            fail("finance currency admin route must stay on the admin namespace:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void financeQuoteSchemeMustStayInFinanceAdminModule() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path repoRoot = backendRoot.getParent();
        String controller = Files.readString(backendRoot.resolve(
                "finance/src/main/java/com/ruoyi/finance/controller/AdminQuoteSchemeController.java"),
                StandardCharsets.UTF_8);
        String service = Files.readString(backendRoot.resolve(
                "finance/src/main/java/com/ruoyi/finance/service/impl/QuoteSchemeServiceImpl.java"),
                StandardCharsets.UTF_8);
        String mapperXml = Files.readString(backendRoot.resolve(
                "finance/src/main/resources/mapper/finance/QuoteSchemeMapper.xml"),
                StandardCharsets.UTF_8);
        String sql = Files.readString(backendRoot.resolve(
                "sql/20260610_quote_scheme_phase1.sql"), StandardCharsets.UTF_8);
        String valueFeeSql = Files.readString(backendRoot.resolve(
                "sql/20260611_quote_scheme_value_fee_rule.sql"), StandardCharsets.UTF_8);
        String serviceTs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/finance/quoteScheme.ts"), StandardCharsets.UTF_8);
        String serviceJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/finance/quoteScheme.js"), StandardCharsets.UTF_8);
        String page = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Finance/QuoteScheme/index.tsx"), StandardCharsets.UTF_8);
        String pageJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Finance/QuoteScheme/index.js"), StandardCharsets.UTF_8);
        String billingCompatPage = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Billing/QuoteScheme/index.tsx"), StandardCharsets.UTF_8);
        String billingCompatPageJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Billing/QuoteScheme/index.js"), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertContains(controller, "@RequestMapping(\"/finance/admin/quote-schemes\")",
                "AdminQuoteSchemeController must stay under /finance/admin/quote-schemes", violations);
        assertAdminOnlyController(controller, "AdminQuoteSchemeController", violations);
        for (String expectedPermission : new String[] {
                "finance:quoteScheme:list",
                "finance:quoteScheme:query",
                "finance:quoteScheme:add",
                "finance:quoteScheme:edit",
                "finance:quoteScheme:status",
                "finance:quoteScheme:warehouse",
                "finance:quoteScheme:channel",
                "finance:quoteScheme:valueFee"
        })
        {
            assertContains(controller, expectedPermission,
                    "AdminQuoteSchemeController must guard " + expectedPermission, violations);
        }
        for (String mutatingMapping : new String[] {
                "@PostMapping",
                "@PutMapping(\"/{schemeId}\")",
                "@PutMapping(\"/{schemeId}/status\")",
                "@PutMapping(\"/{schemeId}/warehouses\")",
                "@PostMapping(\"/{schemeId}/channels\")",
                "@PutMapping(\"/{schemeId}/channels/{schemeChannelId}\")",
                "@DeleteMapping(\"/{schemeId}/channels/{schemeChannelId}\")",
                "@PostMapping(\"/{schemeId}/value-fees\")",
                "@PutMapping(\"/{schemeId}/value-fees/{valueFeeRuleId}\")",
                "@DeleteMapping(\"/{schemeId}/value-fees/{valueFeeRuleId}\")"
        })
        {
            assertContains(controller, mutatingMapping,
                    "AdminQuoteSchemeController must keep mutating route " + mutatingMapping, violations);
        }

        assertContains(service, "ObjectProvider<QuoteSchemeBuyerLookupService>",
                "quote scheme service must use buyer lookup port instead of buyer mapper", violations);
        assertContains(service, "ObjectProvider<QuoteSchemeWarehouseLookupService>",
                "quote scheme service must use warehouse lookup port instead of warehouse mapper", violations);
        assertContains(service, "ObjectProvider<QuoteSchemeCustomerChannelLookupService>",
                "quote scheme service must use customer channel lookup port instead of logistics mapper", violations);
        assertContains(service, "ObjectProvider<QuoteSchemeSystemChannelLookupService>",
                "quote scheme service must use system channel lookup port instead of logistics mapper", violations);
        assertContains(service, "QuoteSchemeValueFeeRule",
                "quote scheme service must manage value fee rules inside finance module", violations);
        assertContains(service, "VALUE_FEE_TRIGGER_ORDER_CANCELLED",
                "quote scheme service must restrict phase1 value fee trigger", violations);
        assertContains(service, "selectQuoteSchemeValueFeeRuleByChannelAndTrigger",
                "quote scheme service must prevent duplicate value fee rules", violations);
        assertContains(service, "normalizeWarehouseCodesForScope",
                "quote scheme service must normalize warehouse scope as a single warehouse", violations);
        assertContains(service, "scheme.setWarehouseScopeMode(WAREHOUSE_INCLUDE)",
                "quote scheme service must force quote schemes to one selected warehouse", violations);
        assertContains(service, "仓库必须且只能选择一个",
                "quote scheme service must require exactly one warehouse selection", violations);
        assertContains(service, "assertEffectivePriorityNotConflicting",
                "quote scheme service must reject overlapping enabled schemes with the same priority", violations);
        assertContains(service, "countOverlappingEnabledSchemeWithSamePriority",
                "quote scheme service must delegate overlapping priority checks to mapper", violations);
        assertContains(controller, "@GetMapping(\"/options/system-channels\")",
                "AdminQuoteSchemeController must expose finance-owned system channel options for cost schemes", violations);
        assertContains(controller, "@GetMapping(\"/{schemeId}/value-fees/list\")",
                "AdminQuoteSchemeController must expose finance-owned value fee list route", violations);
        assertNotContains(service, "com.ruoyi.buyer.mapper", "finance quote scheme service must not import buyer mapper", violations);
        assertNotContains(service, "com.ruoyi.warehouse.mapper", "finance quote scheme service must not import warehouse mapper", violations);
        assertNotContains(service, "com.ruoyi.logistics.mapper", "finance quote scheme service must not import logistics mapper", violations);
        assertContains(mapperXml, "order by s.effective_priority desc, s.effective_time desc, s.scheme_id desc",
                "quote scheme mapper must order overlapping schemes by effective priority first", violations);
        assertContains(mapperXml, "countOverlappingEnabledSchemeWithSamePriority",
                "quote scheme mapper must expose overlapping priority conflict query", violations);
        assertContains(mapperXml, "s.status = 'ENABLED'",
                "quote scheme overlapping priority check must only compare enabled schemes", violations);
        assertContains(mapperXml, "s.effective_priority = #{effectivePriority}",
                "quote scheme overlapping priority check must compare the effective priority", violations);

        assertContains(sql, "set @confirm_quote_scheme_phase1",
                "quote scheme SQL must require an explicit confirmation token", violations);
        assertContains(sql, "APPLY_QUOTE_SCHEME_PHASE1",
                "quote scheme SQL must document the confirmation token", violations);
        assertContains(sql, "signal sqlstate '45000'",
                "quote scheme SQL must fail closed on guard violations", violations);
        for (String expectedSql : new String[] {
                "create table if not exists quote_scheme",
                "create table if not exists quote_scheme_scope",
                "create table if not exists quote_scheme_warehouse",
                "create table if not exists quote_scheme_channel",
                "scheme_type",
                "warehouse_scope_mode",
                "effective_priority",
                "quote_scheme_fee_source_mode",
                "menu_id = 2053",
                "Finance/QuoteScheme/index",
                "finance:quoteScheme:list",
                "(2545, 2053, 'F', 'finance:quoteScheme:channel')"
        })
        {
            assertContains(sql, expectedSql, "quote scheme SQL must contain " + expectedSql, violations);
        }

        assertContains(valueFeeSql, "set @confirm_quote_scheme_value_fee_rule",
                "quote scheme value fee SQL must require an explicit confirmation token", violations);
        assertContains(valueFeeSql, "APPLY_QUOTE_SCHEME_VALUE_FEE_RULE",
                "quote scheme value fee SQL must document the confirmation token", violations);
        assertContains(valueFeeSql, "signal sqlstate '45000'",
                "quote scheme value fee SQL must fail closed on guard violations", violations);
        for (String expectedValueFeeSql : new String[] {
                "create table if not exists quote_scheme_value_fee_rule",
                "quote_scheme_value_fee_trigger",
                "quote_scheme_value_fee_calc_method",
                "quote_scheme_value_fee_direction",
                "ORDER_CANCELLED",
                "PERCENT",
                "FIXED_AMOUNT",
                "INCREASE",
                "DECREASE",
                "finance:quoteScheme:valueFee",
                "(2546, 2053, 'F', 'finance:quoteScheme:valueFee')"
        })
        {
            assertContains(valueFeeSql, expectedValueFeeSql,
                    "quote scheme value fee SQL must contain " + expectedValueFeeSql, violations);
        }

        assertContains(serviceTs, "const baseUrl = '/api/finance/admin/quote-schemes';",
                "quote scheme TS service must call the finance admin route", violations);
        assertContains(serviceTs, "getQuoteSchemeValueFees",
                "quote scheme TS service must expose value fee list request", violations);
        assertContains(serviceTs, "/value-fees/list",
                "quote scheme TS service must call value fee list route", violations);
        assertEqualsTrimmed(serviceJs, "export * from './quoteScheme.ts';",
                "quote scheme JS service mirror must be a pure TS re-export", violations);
        assertEqualsTrimmed(pageJs, "export { default } from './index.tsx';",
                "quote scheme finance page JS mirror must be a pure TSX re-export", violations);
        assertEqualsTrimmed(billingCompatPage, "export { default } from '../../Finance/QuoteScheme';",
                "quote scheme billing compatibility page must re-export the finance page", violations);
        assertEqualsTrimmed(billingCompatPageJs, "export { default } from './index.tsx';",
                "quote scheme billing compatibility JS mirror must be a pure TSX re-export", violations);
        for (String expectedPermission : new String[] {
                "finance:quoteScheme:add",
                "finance:quoteScheme:edit",
                "finance:quoteScheme:status",
                "finance:quoteScheme:channel",
                "finance:quoteScheme:valueFee"
        })
        {
            assertContains(page, expectedPermission,
                    "quote scheme page must gate " + expectedPermission, violations);
        }
        assertContains(page, "label: '增值费'",
                "quote scheme page must expose the value fee tab", violations);
        assertContains(page, "QuoteSchemeValueFeeRule",
                "quote scheme page must render value fee rules", violations);
        assertContains(page, "normalizeWarehouseCodes",
                "quote scheme page must normalize warehouse form value before saving", violations);
        assertContains(page, "warehouseCodes: normalizeWarehouseCodes",
                "quote scheme page must submit at most one warehouse code", violations);
        assertContains(page, "warehouseScopeMode: 'INCLUDE'",
                "quote scheme page must submit warehouse scope as a selected warehouse", violations);
        assertContains(page, "name=\"warehouseCodes\"\n                label=\"仓库\"",
                "quote scheme page must render one merged warehouse selector", violations);
        assertNotContains(page, "label=\"仓库范围\"",
                "quote scheme page must not render a separate warehouse scope field", violations);
        assertNotContains(page, "label=\"适用仓库\"",
                "quote scheme page must not render the old warehouse field label", violations);
        assertNotContains(page, "quote_scheme_warehouse_scope_mode",
                "quote scheme page must not load warehouse scope options", violations);
        assertContains(page, "getPersistedProTableSearch({ fieldCount: 6 }, 'finance-quote-scheme')",
                "quote scheme page must use persisted ProTable search", violations);
        assertContains(page, "getProTablePagination()",
                "quote scheme page must keep pagination at the table bottom", violations);
        assertContains(page, "getProTableScroll(1600)",
                "quote scheme page must keep table body scroll constrained", violations);

        if (!violations.isEmpty())
        {
            fail("finance quote scheme must stay on the finance admin surface:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void financeFeeEstimateMustStayInFinanceAdminModule() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path repoRoot = backendRoot.getParent();
        String controller = Files.readString(backendRoot.resolve(
                "finance/src/main/java/com/ruoyi/finance/controller/AdminFeeEstimateController.java"),
                StandardCharsets.UTF_8);
        String service = Files.readString(backendRoot.resolve(
                "finance/src/main/java/com/ruoyi/finance/service/impl/FeeEstimateServiceImpl.java"),
                StandardCharsets.UTF_8);
        String requestDto = Files.readString(backendRoot.resolve(
                "finance/src/main/java/com/ruoyi/finance/domain/request/FeeEstimateRequest.java"),
                StandardCharsets.UTF_8);
        String skuQuery = Files.readString(backendRoot.resolve(
                "finance/src/main/java/com/ruoyi/finance/domain/query/FeeEstimateSkuQuery.java"),
                StandardCharsets.UTF_8);
        String skuSnapshot = Files.readString(backendRoot.resolve(
                "finance/src/main/java/com/ruoyi/finance/domain/FeeEstimateSkuSnapshot.java"),
                StandardCharsets.UTF_8);
        String skuLookup = Files.readString(backendRoot.resolve(
                "product/src/main/java/com/ruoyi/product/service/impl/ProductFeeEstimateSkuLookupServiceImpl.java"),
                StandardCharsets.UTF_8);
        String sql = Files.readString(backendRoot.resolve(
                "sql/20260612_fee_estimate_menu_seed.sql"), StandardCharsets.UTF_8);
        String serviceTs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/finance/feeEstimate.ts"), StandardCharsets.UTF_8);
        String serviceJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/finance/feeEstimate.js"), StandardCharsets.UTF_8);
        String page = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Finance/FeeEstimate/index.tsx"), StandardCharsets.UTF_8);
        String pageJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Finance/FeeEstimate/index.js"), StandardCharsets.UTF_8);
        String frontendManifest = Files.readString(repoRoot.resolve(
                "react-ui/tests/three-terminal.manifest.json"), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertContains(controller, "@RequestMapping(\"/finance/admin/fee-estimate\")",
                "AdminFeeEstimateController must stay under /finance/admin/fee-estimate", violations);
        assertAdminOnlyController(controller, "AdminFeeEstimateController", violations);
        for (String expectedPermission : new String[] {
                "finance:feeEstimate:query",
                "finance:feeEstimate:calculate"
        })
        {
            assertContains(controller, expectedPermission,
                    "AdminFeeEstimateController must guard " + expectedPermission, violations);
        }
        assertContains(controller, "@GetMapping(\"/options\")",
                "fee estimate controller must expose options route", violations);
        assertContains(controller, "@GetMapping(\"/skus/list\")",
                "fee estimate controller must expose SKU lookup route", violations);
        assertContains(controller, "skus(FeeEstimateSkuQuery query)",
                "fee estimate SKU lookup must use structured filters instead of a broad keyword", violations);
        assertContains(controller, "@PostMapping(\"/calculate\")",
                "fee estimate controller must expose calculate route", violations);
        assertContains(controller, "isSaveRequestData = false",
                "fee estimate controller must not persist destination payloads in sys_oper_log", violations);
        assertContains(requestDto, "destinationAddress2",
                "fee estimate request DTO must carry destination address line 2", violations);
        assertContains(requestDto, "estimateView",
                "fee estimate request DTO must distinguish operations and buyer simulation views", violations);
        assertContains(requestDto, "warehouseCodes",
                "fee estimate request DTO must allow auto-best warehouse constraints", violations);
        assertContains(requestDto, "customerChannelCode",
                "fee estimate request DTO must allow buyer simulation manual channel selection", violations);
        for (String field : new String[] {
                "sourceWarehouseCode",
                "skuCode",
                "productName"
        })
        {
            assertContains(skuQuery, field,
                    "fee estimate SKU query must expose structured filter " + field, violations);
        }
        assertContains(skuSnapshot, "sourceWarehouseCodes",
                "fee estimate SKU snapshot must expose source warehouse codes for common-warehouse selection guards",
                violations);
        assertContains(skuSnapshot, "availableStock",
                "fee estimate SKU snapshot must expose platform available stock for the selector", violations);

        assertContains(service, "ObjectProvider<FinanceFeeEstimateSkuLookupService>",
                "fee estimate service must use product lookup port instead of product mapper", violations);
        assertContains(service, "ObjectProvider<FinanceFeeEstimateLogisticsLookupService>",
                "fee estimate service must use logistics lookup port instead of logistics mapper", violations);
        assertContains(service, "ObjectProvider<FinanceFeeEstimateExternalService>",
                "fee estimate service must use external estimate port instead of integration client", violations);
        assertContains(service, "ObjectProvider<QuoteSchemeBuyerLookupService>",
                "fee estimate service must use buyer lookup port instead of buyer mapper", violations);
        assertContains(service, "SELECTION_AUTO_BEST",
                "fee estimate service must support automatic best-channel selection mode", violations);
        assertContains(service, "VIEW_BUYER_SIMULATION",
                "fee estimate service must support buyer simulation view", violations);
        assertContains(service, "selectWinningSchemesByWarehouse",
                "fee estimate service must resolve quote scheme priority before route expansion", violations);
        assertContains(service, "resolveCommonWarehouseCandidates",
                "fee estimate service must resolve common SKU warehouse candidates before route expansion", violations);
        assertContains(service, "ObjectProvider<QuoteSchemeWarehouseLookupService>",
                "fee estimate service must use warehouse lookup port instead of warehouse mapper", violations);
        assertContains(service, "MODE_SKU",
                "fee estimate service must support SKU input mode", violations);
        assertContains(service, "MODE_MANUAL",
                "fee estimate service must support manual package mode", violations);
        assertContains(service, "Arrays.sort(sides)",
                "fee estimate service must sort SKU dimensions before merging", violations);
        assertContains(service, "edge1 = edge1.add(sides[0].multiply(BigDecimal.valueOf(quantity)))",
                "fee estimate service must sum the shortest SKU side by quantity", violations);
        assertContains(service, "edge2 = max(edge2, sides[1])",
                "fee estimate service must use the max middle side", violations);
        assertContains(service, "edge3 = max(edge3, sides[2])",
                "fee estimate service must use the max longest side", violations);
        assertContains(service, "VOLUME_WEIGHT_DIVISOR",
                "fee estimate service must expose chargeable weight calculation", violations);
        assertContains(service, "buildEstimateResult",
                "fee estimate service must call the external estimate port for executable routes", violations);
        assertNotContains(service, "com.ruoyi.product.mapper",
                "finance fee estimate service must not import product mapper", violations);
        assertNotContains(service, "com.ruoyi.logistics.mapper",
                "finance fee estimate service must not import logistics mapper", violations);
        assertNotContains(service, "LingxingOpenApiClient",
                "finance fee estimate service must not couple directly to Lingxing client", violations);

        assertContains(skuLookup, "implements FinanceFeeEstimateSkuLookupService",
                "product module must implement the finance SKU lookup port", violations);
        assertContains(skuLookup, "selectSkuPageList",
                "product SKU lookup must reuse the product distribution SKU list mapper", violations);
        assertContains(skuLookup, "selectSkuById",
                "product SKU lookup must reuse the product distribution SKU detail mapper", violations);
        assertContains(skuLookup, "selectSkuWarehouseCandidatesByIds",
                "product SKU lookup must expose SKU warehouse candidates for automatic route resolution", violations);
        assertContains(skuLookup, "setSourceWarehouseCode",
                "product SKU lookup must pass source warehouse filter to product mapper", violations);
        assertContains(skuLookup, "setSkuCode",
                "product SKU lookup must pass SKU filter to product mapper", violations);
        assertContains(skuLookup, "setProductName",
                "product SKU lookup must pass product name filter to product mapper", violations);
        assertContains(skuLookup, "setSourceWarehouseCodes",
                "product SKU lookup must expose source warehouse codes to the fee estimate selector", violations);
        assertContains(skuLookup, "setAvailableStock(sku.getAvailableStock())",
                "product SKU lookup must expose platform available stock to the fee estimate selector", violations);
        assertContains(skuLookup, "selectWarehousesBySpuId(sku.getSpuId())",
                "product SKU lookup must reuse product warehouse bindings for source warehouse intersection", violations);
        for (String sourceMeasureField : new String[] {
                "getMeasureLengthCm",
                "getMeasureWidthCm",
                "getMeasureHeightCm",
                "getMeasureWeightKg"
        })
        {
            assertContains(skuLookup, sourceMeasureField,
                    "product SKU lookup must expose source warehouse measurement field " + sourceMeasureField,
                    violations);
        }

        assertContains(sql, "set @confirm_fee_estimate_menu_seed",
                "fee estimate menu SQL must require an explicit confirmation token", violations);
        assertContains(sql, "APPLY_FEE_ESTIMATE_MENU_SEED",
                "fee estimate menu SQL must document the confirmation token", violations);
        assertContains(sql, "signal sqlstate '45000'",
                "fee estimate menu SQL must fail closed on guard violations", violations);
        assertContains(sql, "(2550, 2050, 'C', 'fee-estimate', 'Finance/FeeEstimate/index'",
                "fee estimate menu SQL must place the page under finance management", violations);
        assertContains(sql, "and order_num = 1",
                "fee estimate menu SQL must keep the page first under finance management", violations);
        assertContains(sql, "finance:feeEstimate:list",
                "fee estimate menu SQL must contain the page permission", violations);
        assertContains(sql, "finance:feeEstimate:query",
                "fee estimate menu SQL must contain the query permission", violations);
        assertContains(sql, "finance:feeEstimate:calculate",
                "fee estimate menu SQL must contain the calculate permission", violations);
        assertNotContains(sql, "create table if not exists fee_estimate",
                "fee estimate menu SQL must not create a business table", violations);

        assertContains(serviceTs, "const baseUrl = '/api/finance/admin/fee-estimate';",
                "fee estimate TS service must call the finance admin route", violations);
        for (String method : new String[] {
                "getFeeEstimateOptions",
                "getFeeEstimateSkus",
                "calculateFeeEstimate"
        })
        {
            assertContains(serviceTs, method,
                    "fee estimate TS service must expose " + method, violations);
        }
        assertEqualsTrimmed(serviceJs, "export * from './feeEstimate.ts';",
                "fee estimate JS service mirror must be a pure TS re-export", violations);
        assertEqualsTrimmed(pageJs, "export { default } from './index.tsx';",
                "fee estimate page JS mirror must be a pure TSX re-export", violations);
        assertContains(page, "access.hasPerms('finance:feeEstimate:query')",
                "fee estimate page must gate query requests", violations);
        assertContains(page, "access.hasPerms('finance:feeEstimate:calculate')",
                "fee estimate page must gate calculate requests", violations);
        assertContains(page, "inputModeOptions",
                "fee estimate page must keep SKU and manual modes explicit", violations);
        assertContains(page, "viewTabs",
                "fee estimate page must expose operations and buyer simulation tabs", violations);
        assertContains(page, "VIEW_BUYER_SIMULATION",
                "fee estimate page must render buyer simulation mode", violations);
        assertContains(page, "label=\"包裹方式\"",
                "fee estimate page must move the package mode selector to the left condition panel", violations);
        assertContains(page, "options={inputModeOptions}",
                "fee estimate page must render the package mode selector from the shared options", violations);
        assertContains(page, "label=\"仓库/渠道选择方式\"",
                "buyer simulation must use the confirmed warehouse/channel selection wording", violations);
        assertContains(page, "label=\"商品/SKU\"",
                "buyer simulation must select products from the left condition panel", violations);
        assertContains(page, "renderBuyerSkuPicker",
                "buyer simulation must use a picker button instead of a huge SKU dropdown", violations);
        assertContains(page, "title=\"选择商品 SKU\"",
                "buyer simulation SKU selection must use a modal table", violations);
        assertContains(page, "skuSelectorColumns",
                "buyer simulation SKU selection must expose table columns", violations);
        assertContains(page, "rowSelection={{",
                "buyer simulation SKU modal must support table row selection", violations);
        assertContains(page, "preserveSelectedRowKeys: true",
                "buyer simulation SKU modal must preserve cross-page selections", violations);
        assertContains(page, "dataIndex: 'sourceWarehouseCode'",
                "buyer simulation SKU modal must expose source warehouse filtering", violations);
        assertContains(page, "dataIndex: 'skuCode'",
                "buyer simulation SKU modal must expose SKU filtering", violations);
        assertContains(page, "sourceWarehouseCode: params.sourceWarehouseCode",
                "buyer simulation SKU modal must submit source warehouse filter", violations);
        assertContains(page, "skuCode: params.skuCode",
                "buyer simulation SKU modal must submit SKU filter", violations);
        assertContains(page, "productName: params.productName",
                "buyer simulation SKU modal must submit product name filter", violations);
        assertContains(page, "title: '可用库存'",
                "buyer simulation SKU modal must show platform available stock", violations);
        assertContains(page, "dataIndex: 'availableStock'",
                "buyer simulation SKU modal must bind platform available stock", violations);
        assertNotContains(page, "keyword: params.keyword",
                "buyer simulation SKU modal must not use broad keyword filtering", violations);
        assertNotContains(page, "dataIndex: 'keyword'",
                "buyer simulation SKU modal must not render the broad keyword search field", violations);
        assertContains(page, "getCommonSourceWarehouseCodes",
                "buyer simulation SKU modal must compute the common source warehouse intersection", violations);
        assertContains(page, "canMergeSkuBySourceWarehouse",
                "buyer simulation SKU modal must block SKUs without a common source warehouse", violations);
        assertContains(page, "getCheckboxProps: (record) =>",
                "buyer simulation SKU modal must disable non-intersecting SKU rows", violations);
        assertContains(page, "所选 SKU 必须至少有一个共同来源仓",
                "buyer simulation must validate common source warehouse before applying or calculating", violations);
        assertContains(page, "共同来源仓",
                "buyer simulation must show the common source warehouse to operators", violations);
        assertContains(page, "label=\"限制仓库\"",
                "auto-best buyer simulation must allow optional warehouse constraints", violations);
        assertContains(page, "label=\"选择客户渠道\"",
                "manual buyer simulation must require a customer channel", violations);
        assertContains(page, "候选解析",
                "buyer simulation must show candidate resolution diagnostics", violations);
        assertContains(page, "effectivePackageInputMode === 'SKU'",
                "fee estimate page must keep SKU mode mutually exclusive", violations);
        assertContains(page, "packageInputMode === 'MANUAL'",
                "fee estimate page must keep manual mode mutually exclusive", violations);
        assertContains(page, "name=\"destinationAddress2\"",
                "fee estimate page must collect destination address line 2", violations);
        assertContains(page, "destinationAddress2: values.destinationAddress2",
                "fee estimate page must submit destination address line 2", violations);
        assertContains(page, "mode=\"multiple\"",
                "fee estimate page must allow multi-channel selection", violations);
        assertContains(page, "placeholder=\"不选则展示全部渠道\"",
                "fee estimate page must support all channels when none selected", violations);
        assertContains(page, "placeholder=\"不选则全部可用仓库参与计算\"",
                "buyer auto-best mode must treat empty warehouse constraints as all candidate warehouses", violations);
        assertContains(page, "RequestID:",
                "fee estimate page must show the request id above the result list", violations);
        assertContains(page, "title: '包裹尺寸'",
                "fee estimate result must show package size", violations);
        assertContains(page, "title: '实重'",
                "fee estimate result must show actual weight", violations);
        assertContains(page, "title: '体积重'",
                "fee estimate result must show volume weight", violations);
        assertContains(page, "title: '计费重'",
                "fee estimate result must show chargeable weight", violations);
        assertNotContains(page, "title: '包裹数量'",
                "fee estimate result must not expose package count as a main column", violations);
        assertContains(page, "styles.conditionPanel",
                "fee estimate page must render a left condition panel", violations);
        assertContains(page, "styles.packagePanel",
                "fee estimate page must render the upper package panel in SKU mode", violations);
        assertContains(page, "styles.resolvePanel",
                "buyer simulation must render candidate resolution panel", violations);
        assertContains(page, "styles.resultPanel",
                "fee estimate page must render the lower result panel", violations);
        assertContains(page, "styles.manualWorkspace",
                "fee estimate page must let manual mode expand result panel across the right workspace", violations);
        assertContains(page, "styles.manualFields",
                "fee estimate page must keep manual dimensions inside the left condition panel", violations);

        assertContains(frontendManifest, "\"tests/finance-fee-estimate-contract.test.ts\"",
                "fee estimate frontend contract must be manifest-owned", violations);

        if (!violations.isEmpty())
        {
            fail("finance fee estimate must stay on the finance admin surface:\n" + String.join("\n", violations));
        }
    }

    private void assertAdminOnlyController(String controller, String controllerName, List<String> violations)
    {
        assertNotContains(controller, "@Anonymous", controllerName + " must not expose anonymous handlers", violations);
        assertNotContains(controller, "@PortalPreAuthorize", controllerName + " must not use portal authorization", violations);
        assertNotContains(controller, "@PortalLog", controllerName + " must not use portal logging", violations);
        assertNotContains(controller, "seller:", controllerName + " must not use seller terminal permissions", violations);
        assertNotContains(controller, "buyer:", controllerName + " must not use buyer terminal permissions", violations);
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

    private void assertEqualsTrimmed(String source, String expected, String message, List<String> violations)
    {
        if (!source.trim().equals(expected))
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
            if (Files.isDirectory(candidate.resolve("finance/src/main/java"))
                    && Files.isDirectory(candidate.resolve("ruoyi-system/src/test/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
