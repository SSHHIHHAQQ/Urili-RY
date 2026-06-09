package com.ruoyi.integration.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class IntegrationModuleBoundaryContractTest
{
    private static final Pattern FORBIDDEN_IMPORT = Pattern.compile(
            "^import\\s+com\\.ruoyi\\.(product|warehouse)\\.(mapper|service\\.impl)\\..*;",
            Pattern.MULTILINE);

    @Test
    public void integrationMustUsePublicPortsInsteadOfProductOrWarehouseInternals() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path javaRoot = backendRoot.resolve("integration/src/main/java");
        List<String> violations = new ArrayList<>();

        for (Path file : javaFiles(javaRoot))
        {
            String source = read(file);
            if (FORBIDDEN_IMPORT.matcher(source).find())
            {
                violations.add(backendRoot.relativize(file).toString());
            }
        }

        if (!violations.isEmpty())
        {
            fail("integration must use public ports, not product/warehouse mapper or impl classes:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void integrationPomMustNotDependOnWarehouseModule() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String integrationPom = read(backendRoot.resolve("integration/pom.xml"));
        List<String> violations = new ArrayList<>();

        assertNotContains(integrationPom, "<artifactId>warehouse</artifactId>",
                "integration must not add a direct warehouse module dependency", violations);

        if (!violations.isEmpty())
        {
            fail("integration module dependency boundary failed:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void integrationSourceRefreshMustStayOnInventoryAndWarehousePorts() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String upstreamService = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java"));
        String sourceRefreshService = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/impl/SourceReadModelRefreshServiceImpl.java"));
        String sourceStockLookup = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/impl/SourceWarehouseStockInventoryLookupServiceImpl.java"));
        List<String> violations = new ArrayList<>();

        assertContains(upstreamService, "ObjectProvider<IWarehouseFactLookupService> warehouseFactLookupService",
                "upstream system service must resolve warehouse facts through the integration-owned port", violations);
        assertContains(upstreamService, "ObjectProvider<IInventoryOverviewService> inventoryOverviewService",
                "upstream system service must refresh inventory through the inventory public service", violations);
        assertContains(sourceRefreshService, "ObjectProvider<IInventoryOverviewService> inventoryOverviewService",
                "source read model refresh must notify inventory through the inventory public service", violations);
        assertContains(sourceStockLookup, "implements InventorySourceWarehouseStockLookupService",
                "source stock lookup must implement the inventory-owned source stock port", violations);
        assertContains(sourceStockLookup, "selectOfficialMasterStocksBySourceSkuKeys",
                "source stock lookup must expose official source-stock slices through the inventory-owned port",
                violations);
        assertContains(sourceStockLookup, "upstreamSystemMapper.selectOfficialMasterSourceStocksBySourceSkuKeys(sourceKeys)",
                "source stock lookup must delegate source-stock slices to the integration mapper", violations);

        if (!violations.isEmpty())
        {
            fail("integration source refresh boundary failed:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void integrationMustOwnWarehousePairingProjectionQueries() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String projectionApi = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/IUpstreamWarehousePairingProjectionService.java"));
        String projectionImpl = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamWarehousePairingProjectionServiceImpl.java"));
        String mapperApi = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/mapper/UpstreamSystemMapper.java"));
        String mapperXml = read(backendRoot.resolve(
                "integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml"));
        List<String> violations = new ArrayList<>();

        assertContains(projectionApi, "selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes",
                "integration must expose warehouse pairing snapshots through a stable projection port", violations);
        assertContains(projectionImpl, "implements IUpstreamWarehousePairingProjectionService",
                "warehouse pairing projection implementation must implement the public port", violations);
        assertContains(projectionImpl,
                "upstreamSystemMapper.selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes(",
                "warehouse pairing projection implementation must delegate to the integration mapper", violations);
        assertContains(mapperApi, "selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes",
                "integration mapper must own the warehouse pairing snapshot query", violations);
        assertContains(mapperXml, "id=\"selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes\"",
                "integration mapper XML must define the warehouse pairing snapshot query", violations);
        assertContains(mapperXml, "from upstream_system_warehouse_pairing p",
                "warehouse pairing snapshot query must read the integration-owned pairing table", violations);
        assertContains(mapperXml, "left join upstream_system_connection c on c.connection_code = p.connection_code",
                "warehouse pairing snapshot query must enrich connection display fields inside integration", violations);
        assertContains(mapperXml, "p.status = 'ACTIVE'",
                "warehouse pairing snapshot query must expose active pairing snapshots only", violations);

        if (!violations.isEmpty())
        {
            fail("integration warehouse pairing projection boundary failed:\n" + String.join("\n", violations));
        }
    }

    private List<Path> javaFiles(Path root) throws IOException
    {
        try (Stream<Path> files = Files.walk(root))
        {
            return files.filter((file) -> Files.isRegularFile(file) && file.toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
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
            violations.add(message + ": forbidden " + forbidden);
        }
    }

    private String read(Path file) throws IOException
    {
        return Files.readString(file, StandardCharsets.UTF_8);
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
            if (Files.isDirectory(candidate.resolve("integration/src/main/java"))
                    && Files.isRegularFile(candidate.resolve("integration/pom.xml")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
