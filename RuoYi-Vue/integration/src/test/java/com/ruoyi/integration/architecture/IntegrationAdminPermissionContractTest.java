package com.ruoyi.integration.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class IntegrationAdminPermissionContractTest
{
    private static final Pattern INTEGRATION_ADMIN_CLASS_MAPPING = Pattern.compile(
            "@RequestMapping\\s*\\(\\s*(?:(?:value|path)\\s*=\\s*)?\"/integration/admin/[^\\\"]+\"",
            Pattern.DOTALL);
    private static final Pattern HANDLER_MAPPING = Pattern.compile(
            "@(?:GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\b");
    private static final Pattern SENSITIVE_MAPPING = Pattern.compile(
            "@(?:PostMapping|PutMapping|DeleteMapping|PatchMapping)\\b");
    private static final Pattern PUBLIC_METHOD = Pattern.compile("public\\s+[^\\(]+\\s+(\\w+)\\s*\\(");

    @Test
    public void integrationAdminControllersMustUseAdminSecurityTemplate() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (Path controller : findIntegrationAdminControllers(backendRoot))
        {
            assertAdminSecurityTemplate(backendRoot, controller, violations);
        }

        if (!violations.isEmpty())
        {
            fail("integration admin controllers must stay on the admin security surface:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamSystemHandlersMustUsePrecisePermissions() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path controller = backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/controller/AdminUpstreamSystemController.java");
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        List<HandlerMethod> handlers = extractHandlerMethods(source);
        List<String> violations = new ArrayList<>();

        assertHandlerAuthorization(handlers, "list", "@ss.hasPermi('integration:upstream:list')", violations);
        assertHandlerAuthorization(handlers, "get", "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(handlers, "add", "@ss.hasPermi('integration:upstream:add')", violations);
        assertHandlerAuthorization(handlers, "edit", "@ss.hasPermi('integration:upstream:edit')", violations);
        assertHandlerAuthorization(handlers, "credentials", "@ss.hasPermi('integration:upstream:credential')", violations);
        assertHandlerAuthorization(handlers, "status", "@ss.hasPermi('integration:upstream:edit')", violations);
        assertHandlerAuthorization(handlers, "order", "@ss.hasPermi('integration:upstream:edit')", violations);
        assertHandlerAuthorization(handlers, "authorize", "@ss.hasPermi('integration:upstream:sync')", violations);
        assertHandlerAuthorization(handlers, "sync",
                "@ss.hasAnyPermi('integration:upstream:sync,integration:upstream:dimensionSync,"
                        + "integration:upstream:inventorySync')",
                violations);
        assertSourceContains(source, "checkSyncPermissions(request)",
                "AdminUpstreamSystemController#sync must re-check selected sync permissions", violations);
        assertSourceContains(source, "requireSyncPermission(\"integration:upstream:dimensionSync\"",
                "AdminUpstreamSystemController must enforce dimension sync permission in code", violations);
        assertSourceContains(source, "requireSyncPermission(\"integration:upstream:inventorySync\"",
                "AdminUpstreamSystemController must enforce inventory sync permission in code", violations);
        assertSourceContains(source, "requireSyncPermission(\"integration:upstream:sync\"",
                "AdminUpstreamSystemController must enforce base sync permission in code", violations);
        assertHandlerAuthorization(handlers, "syncSkus", "@ss.hasPermi('integration:upstream:sync')", violations);
        assertHandlerAuthorization(handlers, "syncSkuDimensions",
                "@ss.hasPermi('integration:upstream:dimensionSync')", violations);
        assertHandlerAuthorization(handlers, "syncSelectedSkuDimensions",
                "@ss.hasPermi('integration:upstream:dimensionSync')", violations);
        assertHandlerAuthorization(handlers, "syncInventory",
                "@ss.hasPermi('integration:upstream:inventorySync')", violations);
        assertHandlerAuthorization(handlers, "inventory",
                "@ss.hasPermi('integration:upstream:inventoryQuery')", violations);
        assertHandlerAuthorization(handlers, "inventorySyncState",
                "@ss.hasPermi('integration:upstream:inventoryQuery')", violations);
        assertHandlerAuthorization(handlers, "syncStates", "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(handlers, "warehouses", "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(handlers, "warehousePairings",
                "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(handlers, "addWarehousePairing",
                "@ss.hasPermi('integration:upstream:pair')", violations);
        assertHandlerAuthorization(handlers, "deleteWarehousePairing",
                "@ss.hasPermi('integration:upstream:pair')", violations);
        assertHandlerAuthorization(handlers, "logisticsChannels",
                "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(handlers, "logisticsChannelPairings",
                "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(handlers, "addLogisticsChannelPairing",
                "@ss.hasPermi('integration:upstream:pair')", violations);
        assertHandlerAuthorization(handlers, "deleteLogisticsChannelPairing",
                "@ss.hasPermi('integration:upstream:pair')", violations);
        assertHandlerAuthorization(handlers, "skus", "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(handlers, "skuPairings", "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(handlers, "addSkuPairing", "@ss.hasPermi('integration:upstream:pair')", violations);
        assertHandlerAuthorization(handlers, "deleteSkuPairing",
                "@ss.hasPermi('integration:upstream:pair')", violations);
        assertHandlerAuthorization(handlers, "skuSyncState",
                "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(handlers, "requestLogs", "@ss.hasPermi('integration:upstream:log')", violations);

        if (!violations.isEmpty())
        {
            fail("upstream system admin handlers must keep precise permissions:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamPairingDeletesMustKeepConnectionScope() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String controller = Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/controller/AdminUpstreamSystemController.java"),
                StandardCharsets.UTF_8);
        String serviceApi = Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/IUpstreamSystemService.java"),
                StandardCharsets.UTF_8);
        String serviceImpl = Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java"),
                StandardCharsets.UTF_8);
        String mapperApi = Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/mapper/UpstreamSystemMapper.java"),
                StandardCharsets.UTF_8);
        String mapperXml = Files.readString(backendRoot.resolve(
                "integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml"),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        String frontendService = Files.readString(backendRoot.resolve(
                "../react-ui/src/services/integration/upstreamSystem.ts").normalize(), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertSourceContains(controller, "@DeleteMapping(\"/{connectionCode}/warehouse-pairings/{warehousePairingId}\")",
                "warehouse pairing delete route must include connectionCode", violations);
        assertSourceContains(controller,
                "upstreamSystemService.deleteWarehousePairing(connectionCode, warehousePairingId)",
                "warehouse pairing delete controller must forward connectionCode", violations);
        assertSourceContains(controller,
                "@DeleteMapping(\"/{connectionCode}/logistics-channel-pairings/{logisticsChannelPairingId}\")",
                "logistics pairing delete route must include connectionCode", violations);
        assertSourceContains(controller,
                "upstreamSystemService.deleteLogisticsChannelPairing(connectionCode, logisticsChannelPairingId)",
                "logistics pairing delete controller must forward connectionCode", violations);
        assertSourceContains(controller, "@DeleteMapping(\"/{connectionCode}/sku-pairings/{skuPairingId}\")",
                "sku pairing delete route must include connectionCode", violations);
        assertSourceContains(controller, "upstreamSystemService.deleteSkuPairing(connectionCode, skuPairingId)",
                "sku pairing delete controller must forward connectionCode", violations);

        assertSourceContains(serviceApi, "int deleteWarehousePairing(String connectionCode, Long warehousePairingId)",
                "warehouse pairing service API must require connectionCode", violations);
        assertSourceContains(serviceApi,
                "int deleteLogisticsChannelPairing(String connectionCode, Long logisticsChannelPairingId)",
                "logistics pairing service API must require connectionCode", violations);
        assertSourceContains(serviceApi, "int deleteSkuPairing(String connectionCode, Long skuPairingId)",
                "sku pairing service API must require connectionCode", violations);
        assertSourceContains(serviceImpl, "upstreamSystemMapper.deleteWarehousePairing(connectionCode, warehousePairingId)",
                "warehouse pairing service must delete by connectionCode + id", violations);
        assertSourceContains(serviceImpl,
                "upstreamSystemMapper.deleteLogisticsChannelPairing(connectionCode, logisticsChannelPairingId)",
                "logistics pairing service must delete by connectionCode + id", violations);
        assertSourceContains(serviceImpl,
                "upstreamSystemMapper.selectSkuPairingById(connectionCode, skuPairingId)",
                "sku pairing service must load by connectionCode + id", violations);
        assertSourceContains(serviceImpl, "upstreamSystemMapper.deleteSkuPairing(connectionCode, skuPairingId)",
                "sku pairing service must delete by connectionCode + id", violations);

        assertSourceContains(mapperApi,
                "int deleteWarehousePairing(@Param(\"connectionCode\") String connectionCode",
                "warehouse pairing mapper API must require connectionCode", violations);
        assertSourceContains(mapperApi,
                "int deleteLogisticsChannelPairing(@Param(\"connectionCode\") String connectionCode",
                "logistics pairing mapper API must require connectionCode", violations);
        assertSourceContains(mapperApi, "selectSkuPairingById(@Param(\"connectionCode\") String connectionCode",
                "sku pairing mapper select must require connectionCode", violations);
        assertSourceContains(mapperApi, "int deleteSkuPairing(@Param(\"connectionCode\") String connectionCode",
                "sku pairing mapper delete must require connectionCode", violations);
        assertSourceContains(mapperXml, "delete from upstream_system_warehouse_pairing\n        where connection_code = #{connectionCode}\n          and warehouse_pairing_id = #{warehousePairingId}",
                "warehouse pairing SQL delete must include connectionCode + id", violations);
        assertSourceContains(mapperXml, "delete from upstream_system_logistics_channel_pairing\n        where connection_code = #{connectionCode}\n          and logistics_channel_pairing_id = #{logisticsChannelPairingId}",
                "logistics pairing SQL delete must include connectionCode + id", violations);
        assertSourceContains(mapperXml, "from upstream_system_sku_pairing\n        where connection_code = #{connectionCode}\n          and sku_pairing_id = #{skuPairingId}",
                "sku pairing SQL select must include connectionCode + id", violations);
        assertSourceContains(mapperXml, "delete from upstream_system_sku_pairing\n        where connection_code = #{connectionCode}\n          and sku_pairing_id = #{skuPairingId}",
                "sku pairing SQL delete must include connectionCode + id", violations);
        assertSourceContains(frontendService,
                "deleteWarehousePairing(connectionCode: string, warehousePairingId: number)",
                "frontend warehouse pairing delete service must require connectionCode", violations);
        assertSourceContains(frontendService,
                "deleteLogisticsChannelPairing(connectionCode: string, logisticsChannelPairingId: number)",
                "frontend logistics pairing delete service must require connectionCode", violations);
        assertSourceContains(frontendService, "deleteSkuPairing(connectionCode: string, skuPairingId: number)",
                "frontend sku pairing delete service must require connectionCode", violations);

        if (!violations.isEmpty())
        {
            fail("upstream pairing deletes must not use naked pairing ids:\n" + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamWarehousePairingMustUseWarehouseFactLookup() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String serviceImpl = Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java"),
                StandardCharsets.UTF_8);
        String factLookupApi = Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/IWarehouseFactLookupService.java"),
                StandardCharsets.UTF_8);
        String factProfile = Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/domain/WarehouseFact.java"),
                StandardCharsets.UTF_8);
        String warehouseFactLookup = Files.readString(backendRoot.resolve(
                "warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseFactLookupServiceImpl.java"),
                StandardCharsets.UTF_8);
        String integrationPom = Files.readString(backendRoot.resolve("integration/pom.xml"), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertSourceContains(serviceImpl, "ObjectProvider<IWarehouseFactLookupService> warehouseFactLookupService",
                "upstream pairing service must use the warehouse fact lookup port", violations);
        assertSourceContains(serviceImpl, "WarehouseFact systemWarehouse = requireNormalOfficialWarehouse",
                "warehouse pairing insert must resolve system warehouse from facts", violations);
        assertSourceContains(serviceImpl, "pairing.setSystemWarehouseCode(systemWarehouse.getWarehouseCode())",
                "warehouse pairing insert must store fact-source warehouse code", violations);
        assertSourceContains(serviceImpl, "pairing.setSystemWarehouseName(systemWarehouse.getWarehouseName())",
                "warehouse pairing insert must store fact-source warehouse name", violations);
        assertSourceNotContains(serviceImpl, "pairing.setSystemWarehouseName(trimRequired(request.getSystemWarehouseName()",
                "warehouse pairing insert must not trust request systemWarehouseName", violations);

        assertSourceContains(factLookupApi, "WarehouseFact selectNormalOfficialWarehouseByCode(String warehouseCode);",
                "integration warehouse fact lookup API must expose normal official warehouse lookup", violations);
        assertSourceContains(factProfile, "private String warehouseCode;",
                "warehouse fact profile must carry warehouse code", violations);
        assertSourceContains(factProfile, "private String warehouseName;",
                "warehouse fact profile must carry warehouse name", violations);
        assertSourceContains(warehouseFactLookup, "implements IWarehouseFactLookupService",
                "warehouse module must implement the integration fact lookup port", violations);
        assertSourceContains(warehouseFactLookup, "warehouseMapper.selectWarehouseByCode(normalizedCode)",
                "warehouse fact lookup must read warehouse facts by code", violations);
        assertSourceContains(warehouseFactLookup, "!KIND_OFFICIAL.equals(warehouse.getWarehouseKind())",
                "warehouse fact lookup must reject non-official warehouses", violations);
        assertSourceContains(warehouseFactLookup, "!STATUS_NORMAL.equals(warehouse.getStatus())",
                "warehouse fact lookup must reject disabled warehouses", violations);
        assertSourceContains(warehouseFactLookup, "fact.setWarehouseName(warehouse.getWarehouseName())",
                "warehouse fact lookup must return the fact-source warehouse name", violations);
        assertSourceNotContains(integrationPom, "<artifactId>warehouse</artifactId>",
                "integration module must not depend directly on warehouse module", violations);

        if (!violations.isEmpty())
        {
            fail("upstream warehouse pairing must close warehouse fact lookup contract:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void integrationReadModelHandlersMustUseCurrentAdminMenuPermissions() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        List<HandlerMethod> sourceProductHandlers = extractHandlerMethods(Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/controller/AdminSourceProductController.java"),
                StandardCharsets.UTF_8));
        assertHandlerAuthorization(sourceProductHandlers, "list",
                "@ss.hasPermi('integration:upstream:query')", violations);
        assertHandlerAuthorization(sourceProductHandlers, "groupDetail",
                "@ss.hasPermi('integration:upstream:query')",
                violations);

        List<HandlerMethod> stockHandlers = extractHandlerMethods(Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/controller/AdminSourceWarehouseStockController.java"),
                StandardCharsets.UTF_8));
        assertHandlerAuthorization(stockHandlers, "list", "@ss.hasPermi('inventory:sourceWarehouse:list')",
                violations);
        assertHandlerAuthorization(stockHandlers, "groupList", "@ss.hasPermi('inventory:sourceWarehouse:list')",
                violations);
        assertHandlerAuthorization(stockHandlers, "groupDetail", "@ss.hasPermi('inventory:sourceWarehouse:list')",
                violations);
        assertHandlerAuthorization(stockHandlers, "masterWarehouseOptions",
                "@ss.hasPermi('inventory:sourceWarehouse:list')", violations);
        assertHandlerAuthorization(stockHandlers, "sourceWarehouseOptions",
                "@ss.hasPermi('inventory:sourceWarehouse:list')", violations);

        if (!violations.isEmpty())
        {
            fail("integration read model handlers must keep current admin menu permissions:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void integrationAdminPermissionsMustExistInSeedSql() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String upstreamSeed = Files.readString(backendRoot.resolve("sql/upstream_system_management_seed.sql"),
                StandardCharsets.UTF_8);
        String businessSeed = Files.readString(backendRoot.resolve("sql/business_menu_seed.sql"),
                StandardCharsets.UTF_8);
        String sourceProductSeed = Files.readString(
                backendRoot.resolve("sql/20260605_source_product_library_menu_component.sql"),
                StandardCharsets.UTF_8);
        String sourceWarehouseSeed = Files.readString(
                backendRoot.resolve("sql/20260606_source_warehouse_stock_menu_rename.sql"),
                StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String permission : new String[] {
                "integration:upstream:list",
                "integration:upstream:query",
                "integration:upstream:add",
                "integration:upstream:edit",
                "integration:upstream:credential",
                "integration:upstream:sync",
                "integration:upstream:pair",
                "integration:upstream:log",
                "integration:upstream:dimensionSync",
                "integration:upstream:inventoryQuery",
                "integration:upstream:inventorySync"
        })
        {
            assertSourceContains(upstreamSeed, permission,
                    "upstream_system_management_seed.sql must seed " + permission, violations);
        }

        if (!businessSeed.contains("integration:upstream:query") && !sourceProductSeed.contains("integration:upstream:query"))
        {
            violations.add("source product library menu permission integration:upstream:query must exist in seed SQL");
        }
        assertSourceContains(sourceWarehouseSeed, "inventory:sourceWarehouse:list",
                "source warehouse stock admin permission must exist in seed SQL", violations);

        if (!violations.isEmpty())
        {
            fail("integration admin permissions must be backed by seed SQL:\n"
                    + String.join("\n", violations));
        }
    }

    private List<Path> findIntegrationAdminControllers(Path backendRoot) throws IOException
    {
        Path controllerRoot = backendRoot.resolve("integration/src/main/java/com/ruoyi/integration/controller");
        try (Stream<Path> paths = Files.walk(controllerRoot))
        {
            return paths.filter(path -> path.getFileName().toString().startsWith("Admin"))
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private void assertAdminSecurityTemplate(Path backendRoot, Path controller, List<String> violations)
            throws IOException
    {
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        String relativePath = backendRoot.relativize(controller).toString();

        if (!INTEGRATION_ADMIN_CLASS_MAPPING.matcher(source).find())
        {
            violations.add(relativePath + " must use an /integration/admin class-level route");
        }
        if (source.contains("@Anonymous") || source.contains("@PortalPreAuthorize") || source.contains("@PortalLog"))
        {
            violations.add(relativePath + " must use admin annotations, not anonymous or portal annotations");
        }
        if (source.contains("seller:") || source.contains("buyer:"))
        {
            violations.add(relativePath + " must not use seller/buyer terminal permissions");
        }

        List<HandlerMethod> handlers = extractHandlerMethods(source);
        if (handlers.isEmpty())
        {
            violations.add(relativePath + " has no mapped admin handler methods");
        }
        for (HandlerMethod handler : handlers)
        {
            String prefix = relativePath + "#" + handler.name;
            if (!handler.annotations.contains("@PreAuthorize"))
            {
                violations.add(prefix + " must declare @PreAuthorize");
            }
            if (SENSITIVE_MAPPING.matcher(handler.annotations).find() && !handler.annotations.contains("@Log"))
            {
                violations.add(prefix + " must declare @Log for mutating operations");
            }
        }
    }

    private void assertHandlerAuthorization(List<HandlerMethod> handlers, String methodName, String expectedPermission,
            List<String> violations)
    {
        HandlerMethod target = handlers.stream()
                .filter(handler -> methodName.equals(handler.name))
                .findFirst()
                .orElse(null);
        if (target == null)
        {
            violations.add(methodName + " handler must exist");
            return;
        }
        if (!target.annotations.contains(expectedPermission))
        {
            violations.add(methodName + " handler must require " + expectedPermission);
        }
    }

    private void assertSourceContains(String source, String expected, String message, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(message + ": missing " + expected);
        }
    }

    private void assertSourceNotContains(String source, String forbidden, String message, List<String> violations)
    {
        if (source.contains(forbidden))
        {
            violations.add(message + ": forbidden " + forbidden);
        }
    }

    private List<HandlerMethod> extractHandlerMethods(String source)
    {
        String[] lines = source.split("\\R", -1);
        List<HandlerMethod> handlers = new ArrayList<>();
        List<String> annotations = new ArrayList<>();

        for (String line : lines)
        {
            String trimmed = line.trim();
            if (trimmed.startsWith("@") || annotationsNeedContinuation(annotations))
            {
                annotations.add(line);
                continue;
            }

            if (trimmed.startsWith("public ") && trimmed.contains("("))
            {
                String annotationText = String.join("\n", annotations);
                if (HANDLER_MAPPING.matcher(annotationText).find())
                {
                    handlers.add(new HandlerMethod(extractMethodName(line), annotationText));
                }
                annotations.clear();
                continue;
            }

            if (!trimmed.isEmpty())
            {
                annotations.clear();
            }
        }

        return handlers;
    }

    private boolean annotationsNeedContinuation(List<String> annotations)
    {
        if (annotations.isEmpty())
        {
            return false;
        }
        int balance = 0;
        for (String annotation : annotations)
        {
            for (int i = 0; i < annotation.length(); i++)
            {
                char c = annotation.charAt(i);
                if (c == '(')
                {
                    balance++;
                }
                else if (c == ')')
                {
                    balance--;
                }
            }
        }
        return balance > 0;
    }

    private String extractMethodName(String methodLine)
    {
        Matcher matcher = PUBLIC_METHOD.matcher(methodLine);
        return matcher.find() ? matcher.group(1) : methodLine.trim();
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
                    && Files.isDirectory(candidate.resolve("sql")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }

    private static class HandlerMethod
    {
        private final String name;
        private final String annotations;

        private HandlerMethod(String name, String annotations)
        {
            this.name = name;
            this.annotations = annotations;
        }
    }
}
