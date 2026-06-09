package com.ruoyi.warehouse.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

public class WarehouseModuleBoundaryContractTest
{
    @Test
    public void warehouseModuleMustUseSellerLookupPortInsteadOfSellerStorage() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String mapperXml = Files.readString(backendRoot.resolve(
                "warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml"), StandardCharsets.UTF_8)
                .toLowerCase();
        String mapperApi = Files.readString(backendRoot.resolve(
                "warehouse/src/main/java/com/ruoyi/warehouse/mapper/WarehouseMapper.java"), StandardCharsets.UTF_8);
        String serviceApi = Files.readString(backendRoot.resolve(
                "warehouse/src/main/java/com/ruoyi/warehouse/service/WarehouseSellerLookupService.java"),
                StandardCharsets.UTF_8);
        String serviceImpl = Files.readString(backendRoot.resolve(
                "warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java"),
                StandardCharsets.UTF_8);
        String controller = Files.readString(backendRoot.resolve(
                "warehouse/src/main/java/com/ruoyi/warehouse/controller/AdminWarehouseController.java"),
                StandardCharsets.UTF_8);
        String thirdPartyList = extractMethod(controller, "thirdPartyList");
        List<String> violations = new ArrayList<>();

        requireNotContains(mapperXml, " join seller ", violations);
        requireNotContains(mapperXml, " from seller ", violations);
        requireNotContains(mapperXml, " from seller_account ", violations);
        requireNotContains(mapperXml, " join seller_account ", violations);
        requireNotContains(mapperApi, "selectNormalSellerOptions", violations);
        requireNotContains(mapperApi, "countNormalSellerById", violations);

        requireContains(serviceApi, "boolean isNormalSeller(Long sellerId);", violations);
        requireContains(serviceApi, "List<WarehouseSellerProfile> selectSellerProfilesByIds", violations);
        requireContains(serviceApi, "List<WarehouseSellerProfile> selectSellerProfilesByKeyword", violations);
        requireContains(serviceImpl, "ObjectProvider<WarehouseSellerLookupService> sellerLookupServiceProvider",
                violations);
        requireContains(serviceImpl, "prepareThirdPartyWarehouseQuery(Warehouse query)", violations);
        requireContains(serviceImpl, "enrichSellerProfiles(warehouses);", violations);
        requireContains(thirdPartyList, "warehouseService.prepareThirdPartyWarehouseQuery(query)", violations);
        assertBefore(thirdPartyList, "warehouseService.prepareThirdPartyWarehouseQuery(query)", "startPage()",
                "third-party warehouse seller keyword preprocessing must run before PageHelper starts", violations);

        if (!violations.isEmpty())
        {
            fail("warehouse module must read seller facts through the seller lookup port:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void warehouseModuleMustNotImportSellerImplementation() throws IOException
    {
        Path warehouseMain = findBackendRoot().resolve("warehouse/src/main/java");
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(warehouseMain))
        {
            files.filter((file) -> Files.isRegularFile(file) && file.toString().endsWith(".java"))
                    .forEach((file) -> {
                        try
                        {
                            String source = Files.readString(file, StandardCharsets.UTF_8);
                            if (source.contains("com.ruoyi.seller."))
                            {
                                violations.add(warehouseMain.relativize(file).toString());
                            }
                        }
                        catch (IOException e)
                        {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        if (!violations.isEmpty())
        {
            fail("warehouse module must not import seller module internals directly:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void warehouseModuleMustUseIntegrationProjectionPortInsteadOfUpstreamStorage() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path warehouseResources = backendRoot.resolve("warehouse/src/main/resources");
        Path warehouseMain = backendRoot.resolve("warehouse/src/main/java");
        String serviceImpl = Files.readString(backendRoot.resolve(
                "warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java"),
                StandardCharsets.UTF_8);
        String projectionApi = Files.readString(backendRoot.resolve(
                "integration/src/main/java/com/ruoyi/integration/service/IUpstreamWarehousePairingProjectionService.java"),
                StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(warehouseResources))
        {
            files.filter((file) -> Files.isRegularFile(file) && file.toString().endsWith(".xml"))
                    .forEach((file) -> {
                        try
                        {
                            String source = Files.readString(file, StandardCharsets.UTF_8).toLowerCase();
                            if (source.contains("upstream_system_"))
                            {
                                violations.add("warehouse resource must not read upstream storage: "
                                        + warehouseResources.relativize(file));
                            }
                        }
                        catch (IOException e)
                        {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        try (Stream<Path> files = Files.walk(warehouseMain))
        {
            files.filter((file) -> Files.isRegularFile(file) && file.toString().endsWith(".java"))
                    .forEach((file) -> {
                        try
                        {
                            String source = Files.readString(file, StandardCharsets.UTF_8);
                            if (source.contains("com.ruoyi.integration.mapper.")
                                    || source.contains("com.ruoyi.integration.service.impl."))
                            {
                                violations.add("warehouse module must not import integration internals directly: "
                                        + warehouseMain.relativize(file));
                            }
                        }
                        catch (IOException e)
                        {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        requireContains(serviceImpl,
                "ObjectProvider<IUpstreamWarehousePairingProjectionService> warehousePairingProjectionServiceProvider",
                violations);
        requireContains(serviceImpl, "enrichWarehousePairings(warehouses);", violations);
        requireContains(serviceImpl, "enrichWarehousePairing(warehouse);", violations);
        requireContains(serviceImpl, "selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes", violations);
        requireContains(projectionApi, "selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes", violations);

        if (!violations.isEmpty())
        {
            fail("warehouse module must read upstream pairing facts through the integration projection port:\n"
                    + String.join("\n", violations));
        }
    }

    private void requireContains(String source, String expected, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add("missing required source fragment: " + expected);
        }
    }

    private void requireNotContains(String source, String forbidden, List<String> violations)
    {
        if (source.contains(forbidden))
        {
            violations.add("forbidden source fragment found: " + forbidden);
        }
    }

    private void assertBefore(String source, String first, String second, String message, List<String> violations)
    {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        if (firstIndex < 0 || secondIndex < 0 || firstIndex > secondIndex)
        {
            violations.add(message);
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
            if (Files.isDirectory(candidate.resolve("warehouse/src/main/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
