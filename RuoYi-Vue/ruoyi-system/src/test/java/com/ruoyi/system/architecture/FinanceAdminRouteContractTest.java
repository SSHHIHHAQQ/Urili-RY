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
        assertContains(controller, "@GetMapping(\"/options/system-channels\")",
                "AdminQuoteSchemeController must expose finance-owned system channel options for cost schemes", violations);
        assertContains(controller, "@GetMapping(\"/{schemeId}/value-fees/list\")",
                "AdminQuoteSchemeController must expose finance-owned value fee list route", violations);
        assertNotContains(service, "com.ruoyi.buyer.mapper", "finance quote scheme service must not import buyer mapper", violations);
        assertNotContains(service, "com.ruoyi.warehouse.mapper", "finance quote scheme service must not import warehouse mapper", violations);
        assertNotContains(service, "com.ruoyi.logistics.mapper", "finance quote scheme service must not import logistics mapper", violations);
        assertContains(mapperXml, "order by s.effective_priority desc, s.effective_time desc, s.scheme_id desc",
                "quote scheme mapper must order overlapping schemes by effective priority first", violations);

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
