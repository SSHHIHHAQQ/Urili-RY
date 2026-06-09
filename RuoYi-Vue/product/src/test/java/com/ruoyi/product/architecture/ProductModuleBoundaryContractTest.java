package com.ruoyi.product.architecture;

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

public class ProductModuleBoundaryContractTest
{
    private static final Pattern FORBIDDEN_CROSS_MODULE_IMPORT = Pattern.compile(
            "^import\\s+com\\.ruoyi\\.(integration|inventory|warehouse|seller|buyer)\\.(mapper|service\\.impl)\\..*;",
            Pattern.MULTILINE);

    @Test
    public void productModuleMustUsePublicCrossModuleContracts() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path productMain = backendRoot.resolve("product/src/main/java");
        List<String> violations = new ArrayList<>();

        for (Path file : javaFiles(productMain))
        {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            if (FORBIDDEN_CROSS_MODULE_IMPORT.matcher(source).find())
            {
                violations.add(backendRoot.relativize(file).toString());
            }
        }

        if (!violations.isEmpty())
        {
            fail("product module must use public cross-module contracts, not mapper/impl internals:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void productAdminControllersMustStayOnAdminRoutesAndPermissions() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path controllerRoot = backendRoot.resolve("product/src/main/java/com/ruoyi/product/controller");
        List<String> violations = new ArrayList<>();

        for (Path file : javaFiles(controllerRoot).stream()
                .filter((path) -> path.getFileName().toString().startsWith("Admin"))
                .filter((path) -> path.getFileName().toString().endsWith("Controller.java"))
                .collect(Collectors.toList()))
        {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            String relativePath = backendRoot.relativize(file).toString();
            if (source.contains("seller:") || source.contains("buyer:"))
            {
                violations.add(relativePath + " must not use seller/buyer terminal permission strings");
            }
            if (source.contains("\"/seller") || source.contains("\"/buyer")
                    || source.contains("'/seller") || source.contains("'/buyer"))
            {
                violations.add(relativePath + " must not expose seller/buyer portal routes");
            }
        }

        if (!violations.isEmpty())
        {
            fail("product admin controllers must stay on the admin terminal surface:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void productInventoryLookupMustImplementInventoryOwnedPortOnly() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String lookupService = Files.readString(backendRoot.resolve(
                "product/src/main/java/com/ruoyi/product/service/impl/ProductInventoryLookupServiceImpl.java"),
                StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        requireContains(lookupService, "implements InventoryProductLookupService", violations);
        requireContains(lookupService, "private ProductDistributionMapper productDistributionMapper;", violations);
        requireContains(lookupService, "selectInventorySkuIdsBySpuId(spuId)", violations);
        requireContains(lookupService, "selectInventorySkuSnapshotsBySpuId(spuId)", violations);
        requireContains(lookupService, "selectInventorySkuSnapshotsBySkuIds(skuIds)", violations);
        requireContains(lookupService, "selectInventorySourceBindingSnapshotsBySpuId(spuId)", violations);
        requireContains(lookupService, "selectInventoryWarehouseSnapshotsBySpuId(spuId)", violations);
        requireContains(lookupService, "selectInventorySourceSkuKeysBySpuId(spuId)", violations);
        requireContains(lookupService, "selectInventorySpuIdsBySourceSkuKeys(sourceKeys)", violations);
        requireNotContains(lookupService, "com.ruoyi.inventory.mapper", violations);
        requireNotContains(lookupService, "com.ruoyi.inventory.service.impl", violations);

        if (!violations.isEmpty())
        {
            fail("product inventory lookup must implement the inventory-owned port without inventory internals:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void productReviewServiceMustKeepStableAuditIdsAndLogPermissionBoundary() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String serviceSource = Files.readString(backendRoot.resolve(
                "product/src/main/java/com/ruoyi/product/service/impl/ProductReviewServiceImpl.java"),
                StandardCharsets.UTF_8);
        String detailMethod = extractMethod(serviceSource, "selectReviewById");
        List<String> violations = new ArrayList<>();

        requireContains(serviceSource, "Long currentAdminUserId = currentUserId();", violations);
        requireContains(serviceSource, "review.setSubmitSubjectId(currentAdminUserId);", violations);
        requireContains(serviceSource, "review.setSubmitAccountId(currentAdminUserId);", violations);
        requireContains(serviceSource, "review.setReviewerId(currentUserId());", violations);
        requireContains(serviceSource, "log.setOperatorId(currentUserId());", violations);
        requireContains(serviceSource, "return SecurityUtils.getUserId();", violations);
        requireContains(serviceSource, "return productReviewMapper.selectReviewOperationLogs(reviewId);", violations);
        requireNotContains(detailMethod, "setLogs(", violations);
        requireNotContains(detailMethod, "selectReviewOperationLogs", violations);

        if (!violations.isEmpty())
        {
            fail("product review service must keep stable audit IDs and keep logs behind log permission:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void productSalesStatusChangesMustCarryReasonToOperationLog() throws IOException
    {
        Path backendRoot = findBackendRoot();
        String serviceSource = Files.readString(backendRoot.resolve(
                "product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java"),
                StandardCharsets.UTF_8);
        String controllerSource = Files.readString(backendRoot.resolve(
                "product/src/main/java/com/ruoyi/product/controller/AdminProductDistributionController.java"),
                StandardCharsets.UTF_8);
        String statusRequestSource = Files.readString(backendRoot.resolve(
                "product/src/main/java/com/ruoyi/product/domain/ProductStatusUpdateRequest.java"),
                StandardCharsets.UTF_8);
        String batchStatusRequestSource = Files.readString(backendRoot.resolve(
                "product/src/main/java/com/ruoyi/product/domain/ProductBatchStatusUpdateRequest.java"),
                StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        requireContains(statusRequestSource, "private String reason;", violations);
        requireContains(batchStatusRequestSource, "private String reason;", violations);
        requireContains(controllerSource, "request.getStatus(), request.getReason()", violations);
        requireContains(controllerSource, "request.getStatus(),\n                request.getReason()", violations);
        requireContains(controllerSource, "syncSkuStatus, request.getReason()", violations);
        requireContains(serviceSource, "int updateSpuStatus(Long spuId, String status, String reason)", violations);
        requireContains(serviceSource, "int updateSkuStatus(Long spuId, Long skuId, String status, String reason)",
                violations);
        requireContains(serviceSource,
                "int batchUpdateSpuStatus(List<Long> spuIds, String status, boolean syncSkuStatus, String reason)",
                violations);
        requireContains(serviceSource, "int batchUpdateSkuStatus(List<Long> skuIds, String status, String reason)",
                violations);
        requireContains(serviceSource, "normalizeSalesStatusReason(targetStatus, reason)", violations);
        requireContains(serviceSource, "recordSpuStatusLog(batchNo, product, targetStatus, normalizedReason)",
                violations);
        requireContains(serviceSource, "recordSkuStatusLog(batchNo, product, sku, targetStatus, normalizedReason)",
                violations);
        requireContains(serviceSource, "recordSkuStatusLog(batchNo, product, sku, skuTargetStatus, reason)",
                violations);
        if (countOccurrences(serviceSource, "log.setReason(reason);") < 2)
        {
            violations.add("sales status operation logs must set reason for both SPU and SKU changes");
        }
        requireNotContains(serviceSource, "recordSpuStatusLog(batchNo, product, targetStatus);", violations);
        requireNotContains(serviceSource, "recordSkuStatusLog(batchNo, product, sku, targetStatus);", violations);

        if (!violations.isEmpty())
        {
            fail("product sales status changes must keep an explicit reason in audit logs:\n"
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

    private int countOccurrences(String source, String fragment)
    {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(fragment, index)) >= 0)
        {
            count++;
            index += fragment.length();
        }
        return count;
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

    private List<Path> javaFiles(Path root) throws IOException
    {
        try (Stream<Path> files = Files.walk(root))
        {
            return files.filter((file) -> Files.isRegularFile(file) && file.toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
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
            if (Files.isDirectory(candidate.resolve("product/src/main/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
