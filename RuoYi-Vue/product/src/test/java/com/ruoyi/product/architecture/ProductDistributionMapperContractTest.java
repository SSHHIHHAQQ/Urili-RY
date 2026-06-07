package com.ruoyi.product.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class ProductDistributionMapperContractTest
{
    private static final List<String> SPU_FORBIDDEN_SOURCE_BINDING_PROPERTIES = Arrays.asList(
            "sourceBindingId",
            "sourceScope",
            "sourceSkuGroupKey",
            "sourceDimensionGroupKey",
            "masterSku",
            "masterProductNameSnapshot",
            "sourcePayloadHash",
            "wmsPayloadHash",
            "measureLengthCm",
            "measureWidthCm",
            "measureHeightCm",
            "measureWeightKg",
            "measureSource",
            "sourceWarehouseNames",
            "sourceWarehouseCount",
            "bindingStatus",
            "lockStatus",
            "lockedTime");

    private static final List<String> SOURCE_BINDING_RESULT_PROPERTIES = Arrays.asList(
            "bindingId",
            "sourceScope",
            "sourceSkuGroupKey",
            "sourceDimensionGroupKey",
            "masterSku",
            "masterProductNameSnapshot",
            "sourcePayloadHash",
            "wmsPayloadHash",
            "measureLengthCm",
            "measureWidthCm",
            "measureHeightCm",
            "measureWeightKg",
            "measureSource",
            "sourceWarehouseNames",
            "sourceWarehouseCount",
            "bindingStatus",
            "lockStatus",
            "lockedTime");

    private static final List<String> PRODUCT_OWNED_TABLES = Arrays.asList(
            "product_spu",
            "product_sku",
            "product_attribute_value",
            "product_image",
            "product_spu_warehouse",
            "product_sku_source_binding");

    private static final Map<String, List<String>> ALLOWED_EXTERNAL_TABLES_BY_STATEMENT =
            createAllowedExternalTablesByStatement();

    private static final List<String> INVENTORY_PLACEHOLDER_ALIASES = Arrays.asList(
            "available_stock",
            "warehouse_count",
            "inventory_status",
            "stock_update_time");

    private static final List<String> FORBIDDEN_INVENTORY_FACT_SOURCE_TOKENS = Arrays.asList(
            "source_warehouse_stock_",
            "upstream_system_sku_inventory_snapshot");

    private static final int EXPECTED_INVENTORY_PLACEHOLDER_COUNT_PER_ALIAS = 6;

    @Test
    public void spuResultMustNotMapSkuSourceBindingFields() throws IOException
    {
        String source = readMapperSource();
        String spuResult = extractResultMap(source, "ProductSpuResult");
        String sourceBindingResult = extractResultMap(source, "ProductSkuSourceBindingResult");
        List<String> violations = new ArrayList<>();

        if (spuResult.isEmpty())
        {
            violations.add("ProductSpuResult must exist");
        }
        if (sourceBindingResult.isEmpty())
        {
            violations.add("ProductSkuSourceBindingResult must exist");
        }

        for (String property : SPU_FORBIDDEN_SOURCE_BINDING_PROPERTIES)
        {
            if (spuResult.contains("property=\"" + property + "\""))
            {
                violations.add("ProductSpuResult must not map SKU binding property " + property);
            }
        }
        for (String property : SOURCE_BINDING_RESULT_PROPERTIES)
        {
            if (!sourceBindingResult.contains("property=\"" + property + "\""))
            {
                violations.add("ProductSkuSourceBindingResult must keep SKU binding property " + property);
            }
        }

        if (!violations.isEmpty())
        {
            fail("product distribution mapper must keep SPU/SKU source binding result maps separated:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void externalTableUsageMustStayWithinExplicitDebtAllowlist() throws IOException
    {
        Map<String, Set<String>> externalTablesByStatement = findExternalTablesByStatement(readMapperSource());
        List<String> violations = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : externalTablesByStatement.entrySet())
        {
            String statementId = entry.getKey();
            List<String> allowedTables = ALLOWED_EXTERNAL_TABLES_BY_STATEMENT.getOrDefault(statementId,
                    Collections.emptyList());
            for (String table : entry.getValue())
            {
                if (!allowedTables.contains(table))
                {
                    violations.add(statementId + " reads or writes external table " + table
                            + " without an explicit product mapper boundary allowlist entry");
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail("ProductDistributionMapper must not expand product -> source/integration/warehouse table debt:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventorySummaryFieldsMustStayExplicitPlaceholdersUntilInventoryFactSourceIsDesigned()
            throws IOException
    {
        String source = readMapperSource();
        List<String> violations = new ArrayList<>();

        for (String alias : INVENTORY_PLACEHOLDER_ALIASES)
        {
            int count = countExplicitNullAlias(source, alias, violations);
            if (count != EXPECTED_INVENTORY_PLACEHOLDER_COUNT_PER_ALIAS)
            {
                violations.add(alias + " must stay as " + EXPECTED_INVENTORY_PLACEHOLDER_COUNT_PER_ALIAS
                        + " explicit null placeholders until inventory fact-source aggregation is designed; actual "
                        + count);
            }
        }

        String lowerSource = source.toLowerCase();
        for (String token : FORBIDDEN_INVENTORY_FACT_SOURCE_TOKENS)
        {
            if (lowerSource.contains(token))
            {
                violations.add("ProductDistributionMapper must not assemble inventory summary from " + token
                        + " before inventory fact-source aggregation is designed");
            }
        }

        if (!violations.isEmpty())
        {
            fail("ProductDistributionMapper inventory summary fields are placeholders, not real inventory facts:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamSkuPairingProjectionDeleteMustKeepConnectionScope() throws IOException
    {
        String source = readMapperSource();
        String connectionLookup = extractStatement(source, "selectSourceConnectionCodesByDimensionGroup");
        String pairingFallbackLookup = extractStatement(source,
                "selectUpstreamSkuPairingConnectionCodesBySystemSkuAndMasterSku");
        String deleteStatement = extractStatement(source, "deleteUpstreamSkuPairingsBySystemSkuAndConnectionCodes");
        List<String> violations = new ArrayList<>();

        if (source.contains("deleteUpstreamSkuPairingsBySystemSku\""))
        {
            violations.add("upstream SKU pairing projection delete must not use naked systemSku statement id");
        }
        requireContains(connectionLookup, "select distinct connection_code", violations);
        requireContains(connectionLookup, "source_dimension_group_key = #{sourceDimensionGroupKey}", violations);
        if (Pattern.compile("\\bstatus\\s*=\\s*'ACTIVE'", Pattern.CASE_INSENSITIVE).matcher(connectionLookup).find())
        {
            violations.add("connection lookup for projection cleanup must not filter inactive source rows");
        }
        requireContains(pairingFallbackLookup, "select distinct connection_code", violations);
        requireContains(pairingFallbackLookup, "from upstream_system_sku_pairing", violations);
        requireContains(pairingFallbackLookup, "system_sku = #{systemSku}", violations);
        requireContains(pairingFallbackLookup, "master_sku = #{masterSku}", violations);
        requireContains(deleteStatement, "delete from upstream_system_sku_pairing", violations);
        requireContains(deleteStatement, "system_sku = #{systemSku}", violations);
        requireContains(deleteStatement, "connection_code in", violations);
        requireContains(deleteStatement, "collection=\"connectionCodes\"", violations);

        if (!violations.isEmpty())
        {
            fail("ProductDistributionMapper must delete upstream SKU pairings by connection_code + system_sku:\n"
                    + String.join("\n", violations));
        }
    }

    private static Map<String, List<String>> createAllowedExternalTablesByStatement()
    {
        Map<String, List<String>> allowlist = new LinkedHashMap<>();
        allowlist.put("selectSourceBindingSnapshot", Arrays.asList(
                "source_product_dimension_group",
                "source_product_warehouse_detail"));
        allowlist.put("selectOfficialWarehousesBySourceDimensionGroup", Arrays.asList(
                "source_product_warehouse_detail",
                "upstream_system_warehouse_pairing",
                "warehouse"));
        allowlist.put("selectSourceConnectionCodesByDimensionGroup", Arrays.asList(
                "source_product_warehouse_detail"));
        allowlist.put("selectUpstreamSkuPairingConnectionCodesBySystemSkuAndMasterSku", Arrays.asList(
                "upstream_system_sku_pairing"));
        allowlist.put("deleteUpstreamSkuPairingsBySystemSkuAndConnectionCodes", Arrays.asList(
                "upstream_system_sku_pairing"));
        allowlist.put("upsertUpstreamSkuPairingsForBinding", Arrays.asList(
                "source_product_warehouse_detail",
                "upstream_system_sku_pairing"));
        return allowlist;
    }

    private Map<String, Set<String>> findExternalTablesByStatement(String source)
    {
        Map<String, Set<String>> externalTablesByStatement = new LinkedHashMap<>();
        Pattern statementPattern = Pattern.compile(
                "<(select|insert|update|delete)\\s+[^>]*id=\"([^\"]+)\"[^>]*>(.*?)</\\1>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher statementMatcher = statementPattern.matcher(source);

        while (statementMatcher.find())
        {
            String statementType = statementMatcher.group(1).toLowerCase();
            String statementId = statementMatcher.group(2);
            String statementBody = statementMatcher.group(3);
            Set<String> externalTables = new LinkedHashSet<>();
            for (String table : findTables(statementType, statementBody))
            {
                if (!PRODUCT_OWNED_TABLES.contains(table))
                {
                    externalTables.add(table);
                }
            }
            if (!externalTables.isEmpty())
            {
                externalTablesByStatement.put(statementId, externalTables);
            }
        }

        return externalTablesByStatement;
    }

    private Set<String> findTables(String statementType, String statementBody)
    {
        Set<String> tables = new LinkedHashSet<>();
        Pattern tablePattern = Pattern.compile(
                "\\b(?:from|join|insert\\s+into|delete\\s+from)\\s+`?([a-zA-Z][a-zA-Z0-9_]*)`?",
                Pattern.CASE_INSENSITIVE);
        Matcher tableMatcher = tablePattern.matcher(statementBody);
        while (tableMatcher.find())
        {
            tables.add(tableMatcher.group(1));
        }

        if ("update".equals(statementType))
        {
            Matcher updateMatcher = Pattern.compile("^\\s*update\\s+`?([a-zA-Z][a-zA-Z0-9_]*)`?",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(statementBody);
            if (updateMatcher.find())
            {
                tables.add(updateMatcher.group(1));
            }
        }

        return tables;
    }

    private int countExplicitNullAlias(String source, String alias, List<String> violations)
    {
        Pattern aliasPattern = Pattern.compile("^\\s*([^\\r\\n,]+)\\s+as\\s+" + alias + "\\b",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher aliasMatcher = aliasPattern.matcher(source);
        int count = 0;
        while (aliasMatcher.find())
        {
            count++;
            String expression = aliasMatcher.group(1).trim();
            if (!"null".equalsIgnoreCase(expression))
            {
                violations.add(alias + " must remain an explicit null placeholder, but found expression "
                        + expression);
            }
        }
        return count;
    }

    private String extractStatement(String source, String id)
    {
        Pattern statementPattern = Pattern.compile(
                "<(select|insert|update|delete)\\s+[^>]*id=\"" + Pattern.quote(id) + "\"[^>]*>.*?</\\1>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = statementPattern.matcher(source);
        if (!matcher.find())
        {
            return "";
        }
        return matcher.group();
    }

    private void requireContains(String source, String expected, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add("missing required SQL fragment: " + expected);
        }
    }

    private String extractResultMap(String source, String id)
    {
        String openToken = "<resultMap ";
        String idToken = "id=\"" + id + "\"";
        int searchFrom = 0;
        while (true)
        {
            int openStart = source.indexOf(openToken, searchFrom);
            if (openStart < 0)
            {
                return "";
            }
            int tagEnd = source.indexOf('>', openStart);
            if (tagEnd < 0)
            {
                return "";
            }
            String openTag = source.substring(openStart, tagEnd + 1);
            if (openTag.contains(idToken))
            {
                String closeToken = "</resultMap>";
                int closeStart = source.indexOf(closeToken, tagEnd);
                if (closeStart < 0)
                {
                    return "";
                }
                return source.substring(openStart, closeStart + closeToken.length());
            }
            searchFrom = tagEnd + 1;
        }
    }

    private String readMapperSource() throws IOException
    {
        Path mapper = findBackendRoot().resolve(
                "product/src/main/resources/mapper/product/ProductDistributionMapper.xml");
        return Files.readString(mapper, StandardCharsets.UTF_8);
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
            if (Files.isDirectory(candidate.resolve("product/src/main/resources/mapper/product")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
