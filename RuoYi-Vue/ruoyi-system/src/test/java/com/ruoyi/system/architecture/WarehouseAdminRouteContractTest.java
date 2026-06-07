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
        assertContains(warehouseJs, "const baseUrl = '/api/warehouse/admin';",
                "warehouse JS service mirror must call the admin route", violations);

        if (!violations.isEmpty())
        {
            fail("warehouse admin route must stay on the admin namespace:\n" + String.join("\n", violations));
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
            if (Files.isDirectory(candidate.resolve("warehouse/src/main/java"))
                    && Files.isDirectory(candidate.resolve("ruoyi-system/src/test/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
