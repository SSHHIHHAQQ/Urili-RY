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
