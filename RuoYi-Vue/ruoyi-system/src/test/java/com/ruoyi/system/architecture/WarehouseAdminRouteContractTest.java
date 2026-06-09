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

public class WarehouseAdminRouteContractTest
{
    @Test
    public void warehouseAdminControllerAndFrontendServiceMustUseAdminRoute() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path repoRoot = backendRoot.getParent();
        String controller = Files.readString(backendRoot.resolve(
                "warehouse/src/main/java/com/ruoyi/warehouse/controller/AdminWarehouseController.java"),
                StandardCharsets.UTF_8);
        String warehouseTs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/warehouse/warehouse.ts"), StandardCharsets.UTF_8);
        String warehouseJs = Files.readString(repoRoot.resolve(
                "react-ui/src/services/warehouse/warehouse.js"), StandardCharsets.UTF_8);
        String pageTs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Warehouse/WarehouseManagementPage.tsx"), StandardCharsets.UTF_8);
        String pageJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Warehouse/WarehouseManagementPage.js"), StandardCharsets.UTF_8);
        String officialJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Warehouse/Official/index.js"), StandardCharsets.UTF_8);
        String thirdPartyJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Warehouse/ThirdParty/index.js"), StandardCharsets.UTF_8);
        String constantsJs = Files.readString(repoRoot.resolve(
                "react-ui/src/pages/Warehouse/constants.js"), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertContains(controller, "@RequestMapping(\"/warehouse/admin\")",
                "AdminWarehouseController must stay under /warehouse/admin", violations);
        assertNotContains(controller, "@RequestMapping(\"/warehouse\")",
                "AdminWarehouseController must not expose the admin surface under /warehouse", violations);
        assertNotContains(controller, "@Anonymous",
                "AdminWarehouseController must not expose anonymous handlers", violations);
        assertNotContains(controller, "@PortalPreAuthorize",
                "AdminWarehouseController must not use portal authorization", violations);
        assertNotContains(controller, "@PortalLog",
                "AdminWarehouseController must not use portal logging", violations);
        assertNotContains(controller, "seller:",
                "AdminWarehouseController must not use seller terminal permissions", violations);
        assertNotContains(controller, "buyer:",
                "AdminWarehouseController must not use buyer terminal permissions", violations);
        assertContains(warehouseTs, "const baseUrl = '/api/warehouse/admin';",
                "warehouse TS service must call the admin route", violations);
        assertEqualsTrimmed("export * from './warehouse.ts';", warehouseJs,
                "warehouse JS service mirror must delegate to TS service", violations);
        assertEqualsTrimmed("export { default } from './WarehouseManagementPage.tsx';", pageJs,
                "warehouse page JS mirror must delegate to TSX page", violations);
        assertEqualsTrimmed("export { default } from './index.tsx';", officialJs,
                "official warehouse route JS mirror must delegate to TSX page", violations);
        assertEqualsTrimmed("export { default } from './index.tsx';", thirdPartyJs,
                "third-party warehouse route JS mirror must delegate to TSX page", violations);
        assertEqualsTrimmed("export * from './constants.ts';", constantsJs,
                "warehouse constants JS mirror must delegate to TS constants", violations);
        for (String permission : new String[] {
                "warehouse:official:list",
                "warehouse:official:add",
                "warehouse:official:edit",
                "warehouse:official:status",
                "warehouse:official:sync",
                "warehouse:thirdParty:list",
                "warehouse:thirdParty:add",
                "warehouse:thirdParty:edit",
                "warehouse:thirdParty:status"
        })
        {
            assertContains(controller, permission,
                    "AdminWarehouseController must declare permission " + permission, violations);
            assertContains(pageTs, permission,
                    "warehouse frontend page must gate permission " + permission, violations);
        }
        assertContains(pageTs, "const canList = access.hasPerms(permissions.list);",
                "warehouse page must compute list permission before loading data", violations);
        assertContains(pageTs, "if (!canList)",
                "warehouse page must short-circuit data loading without list permission", violations);

        if (!violations.isEmpty())
        {
            fail("warehouse admin route must stay on the admin namespace:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void officialWarehouseSyncMustDerivePairingRoleFromConnectionSettlementType() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String service = Files.readString(backendRoot.resolve(
                "warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java"),
                StandardCharsets.UTF_8);
        String syncConnectionsMethod = extractMethod(service, "selectSyncConnections");
        String syncCandidatesMethod = extractMethod(service, "selectSyncCandidates");
        String syncCreateMethod = extractMethod(service, "syncOfficialWarehouse");
        List<String> violations = new ArrayList<>();

        assertContains(syncConnectionsMethod, "selectOfficialSyncConnections(keyword)",
                "official warehouse sync must list both supported sync connection settlement types", violations);
        assertContains(syncCandidatesMethod, "pairingRoleForSettlementType(connection.getSettlementType())",
                "official warehouse sync candidates must derive pairing role from selected connection", violations);
        assertContains(syncCreateMethod, "selectEnabledSyncConnection(request.getConnectionCode())",
                "official warehouse sync create must select a neutral sync connection first", violations);
        assertContains(syncCreateMethod, "String pairingRole = pairingRoleForSettlementType(connection.getSettlementType())",
                "official warehouse sync create must derive pairing role from settlement type", violations);
        assertContains(syncCreateMethod, "isUpstreamWarehousePaired(connection.getConnectionCode(), upstreamWarehouseCode, pairingRole)",
                "official warehouse sync duplicate check must use derived pairing role", violations);
        assertContains(syncCreateMethod, "pairingRequest.setPairingRole(pairingRole)",
                "official warehouse sync auto-pairing must persist derived pairing role", violations);
        assertContains(service, "isOfficialSyncSettlementType(item.getSettlementType())",
                "official warehouse sync connection list must not filter to one hard-coded role", violations);
        assertContains(service, "private String pairingRoleForSettlementType(String settlementType)",
                "warehouse service must keep settlement-to-pairing-role mapping explicit", violations);
        assertContains(service, "SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE",
                "warehouse service must keep self-operated receivable connections as quote warehouse candidates",
                violations);
        assertNotContains(syncConnectionsMethod, "PAIRING_ROLE_FULFILLMENT",
                "official warehouse sync connection list must not be hard-coded to fulfillment", violations);
        assertNotContains(syncCandidatesMethod, "PAIRING_ROLE_FULFILLMENT",
                "official warehouse sync candidates must not be hard-coded to fulfillment", violations);
        assertNotContains(syncCreateMethod, "PAIRING_ROLE_FULFILLMENT",
                "official warehouse sync create must not hard-code fulfillment auto-pairing", violations);

        if (!violations.isEmpty())
        {
            fail("official warehouse sync must derive pairing role from connection settlement type:\n"
                    + String.join("\n", violations));
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

    private String extractMethod(String source, String methodName)
    {
        int methodNameIndex = source.indexOf(methodName + "(");
        if (methodNameIndex < 0)
        {
            return "";
        }
        int bodyStart = source.indexOf('{', methodNameIndex);
        if (bodyStart < 0)
        {
            return "";
        }

        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++)
        {
            char item = source.charAt(i);
            if (item == '{')
            {
                depth++;
            }
            else if (item == '}')
            {
                depth--;
                if (depth == 0)
                {
                    return source.substring(methodNameIndex, i + 1);
                }
            }
        }
        return source.substring(methodNameIndex);
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
            if (Files.isDirectory(candidate.resolve("warehouse/src/main/java"))
                    && Files.isDirectory(candidate.resolve("ruoyi-system/src/test/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
