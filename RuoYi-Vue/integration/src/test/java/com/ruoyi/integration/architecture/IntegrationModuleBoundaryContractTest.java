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

    @Test
    public void upstreamSyncMustUseRuoyiDispatcherTaskLifecycle() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String syncService = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSyncServiceImpl.java"));
        String dispatchTask = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/task/UpstreamSyncDispatchTask.java"));
        String lifecycleSql = read(backendRoot.resolve("sql/20260612_upstream_sync_task_lifecycle.sql"));
        List<String> violations = new ArrayList<>();

        assertContains(syncService, "enqueueSyncTasks(",
                "manual and scheduled upstream sync must enqueue durable tasks", violations);
        assertContains(syncService, "dispatchPendingTasks()",
                "upstream sync service must expose the RuoYi dispatcher entry", violations);
        assertContains(syncService, "SYNC_TRIGGER_SCHEDULED",
                "scheduled sync must be represented as a queued scheduled trigger", violations);
        assertNotContains(syncService, "manualSyncSubmitter",
                "upstream sync service must not dispatch through the old in-memory manual submitter", violations);
        assertContains(dispatchTask, "upstreamSyncService.dispatchPendingTasks()",
                "RuoYi task bean must delegate to the durable dispatcher", violations);
        assertContains(dispatchTask, "@Component(\"upstreamSyncDispatchTask\")",
                "RuoYi task bean name must exactly match the Quartz invoke target", violations);
        assertContains(lifecycleSql, "upstreamSyncDispatchTask.dispatch",
                "migration must register the RuoYi Quartz dispatcher invoke target", violations);
        assertContains(lifecycleSql, "'0/30 * * * * ?'",
                "dispatcher cron must run every 30 seconds", violations);
        assertContains(lifecycleSql, "concurrent = '1'",
                "dispatcher job must be non-concurrent", violations);

        if (!violations.isEmpty())
        {
            fail("upstream sync task lifecycle contract failed:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void lingxingHttpRequestsMustUseStartLogAndHardTimeout() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String client = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java"));
        String factory = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/sync/UpstreamLingxingClientFactory.java"));
        String clockGuard = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/sync/UpstreamClockHealthGuard.java"));
        String mapperApi = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/mapper/UpstreamSystemMapper.java"));
        String mapperXml = read(backendRoot.resolve(
                "integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml"));
        List<String> violations = new ArrayList<>();

        assertContains(client, "log.setStatus(\"STARTED\")",
                "Lingxing request log must be inserted before the external HTTP call starts", violations);
        assertContains(client, "HttpResponse<String> response = sendWithHardTimeout(request)",
                "Lingxing requests must go through the hard-timeout wrapper", violations);
        assertContains(client, "httpClient.sendAsync(request",
                "Lingxing requests must not block the task thread with synchronous send", violations);
        assertContains(client, "future.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)",
                "Lingxing requests must have a hard timeout around async execution", violations);
        assertContains(client, "future.cancel(true)",
                "Lingxing timeout handling must cancel the in-flight future", violations);
        assertContains(client, "LINGXING_TIMEOUT",
                "Lingxing timeout failures must be classified explicitly", violations);
        assertNotContains(client, "httpClient.send(request",
                "Lingxing requests must not use blocking HttpClient#send", violations);
        assertContains(factory, "entry.setRequestLogId(log.getRequestLogId())",
                "request logger must keep the generated request log id for completion updates", violations);
        assertContains(factory, "upstreamSystemMapper.updateRequestLog(log)",
                "request logger must update the started log row on completion", violations);
        assertContains(factory, "clockHealthGuard.assertSystemClockHealthy()",
                "Lingxing client creation must fail fast when the local clock is unhealthy", violations);
        assertContains(clockGuard, "selectDatabaseEpochMillis()",
                "clock guard must compare the host clock against the active database clock", violations);
        assertContains(clockGuard, "LOCAL_CLOCK_SKEW",
                "clock guard must classify local host clock drift explicitly", violations);
        assertContains(mapperApi, "int updateRequestLog(UpstreamRequestLog log)",
                "mapper API must expose request log completion updates", violations);
        assertContains(mapperApi, "Long selectDatabaseEpochMillis()",
                "mapper API must expose active database time for clock health checks", violations);
        assertContains(mapperXml, "<update id=\"updateRequestLog\"",
                "mapper XML must update request log completion fields", violations);
        assertContains(mapperXml, "<select id=\"selectDatabaseEpochMillis\"",
                "mapper XML must define active database time lookup for clock health checks", violations);

        if (!violations.isEmpty())
        {
            fail("Lingxing hard-timeout request logging contract failed:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamSyncTaskCompletionMustNotOverwriteTerminalRows() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String syncService = read(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSyncServiceImpl.java"));
        String taskMapperXml = read(backendRoot.resolve(
                "integration/src/main/resources/mapper/integration/UpstreamSyncTaskMapper.xml"));
        List<String> violations = new ArrayList<>();

        assertContains(syncService, "isRetriableTaskStatus(task.getStatus())",
                "retry must be limited to terminal task states in the service layer", violations);
        assertContains(syncService, "SYNC_TASK_STATUS_SKIPPED",
                "skipped task state must belong to the task lifecycle constants", violations);
        assertContains(taskMapperXml, "and status in ('CLAIMED', 'RUNNING')",
                "task completion must not overwrite rows already closed by timeout, cancel, or recovery", violations);

        if (!violations.isEmpty())
        {
            fail("upstream sync terminal row contract failed:\n" + String.join("\n", violations));
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
