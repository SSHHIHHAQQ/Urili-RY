package com.ruoyi.system.architecture;

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
import org.junit.Test;

public class TerminalRoleMenuMapperIsolationContractTest
{
    @Test
    public void roleMenuMapperStatementsMustUseCurrentTerminalTablesOnly() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        assertTerminalRoleMenuMapper(backendRoot.resolve(
                "seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml"),
                "seller", "buyer", 100000, 200000, violations);
        assertTerminalRoleMenuMapper(backendRoot.resolve(
                "buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml"),
                "buyer", "seller", 200000, 300000, violations);

        if (!violations.isEmpty())
        {
            fail("terminal role-menu mappers must stay isolated to current terminal tables:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertTerminalRoleMenuMapper(Path mapper, String terminal, String otherTerminal, int minMenuId,
            int maxMenuId, List<String> violations) throws IOException
    {
        String xml = Files.readString(mapper, StandardCharsets.UTF_8);
        String countStatement = extractStatement(xml, "select", "count" + capitalize(terminal) + "MenusByIds");
        String batchStatement = extractStatement(xml, "insert", "batch" + capitalize(terminal) + "RoleMenu");
        String fileName = mapper.getFileName().toString();

        requireContains(fileName, "count" + capitalize(terminal) + "MenusByIds", countStatement,
                "from " + terminal + "_menu", violations);
        requireContains(fileName, "count" + capitalize(terminal) + "MenusByIds", countStatement,
                terminal + "_menu_id &gt;= " + minMenuId, violations);
        requireContains(fileName, "count" + capitalize(terminal) + "MenusByIds", countStatement,
                terminal + "_menu_id &lt; " + maxMenuId, violations);
        requireContains(fileName, "count" + capitalize(terminal) + "MenusByIds", countStatement,
                "status = '0'", violations);
        requireAbsent(fileName, "count" + capitalize(terminal) + "MenusByIds", countStatement,
                "sys_menu", violations);
        requireAbsent(fileName, "count" + capitalize(terminal) + "MenusByIds", countStatement,
                otherTerminal + "_menu", violations);

        requireContains(fileName, "batch" + capitalize(terminal) + "RoleMenu", batchStatement,
                "insert into " + terminal + "_role_menu", violations);
        requireContains(fileName, "batch" + capitalize(terminal) + "RoleMenu", batchStatement,
                "from " + terminal + "_role", violations);
        requireContains(fileName, "batch" + capitalize(terminal) + "RoleMenu", batchStatement,
                "inner join " + terminal + "_menu", violations);
        requireContains(fileName, "batch" + capitalize(terminal) + "RoleMenu", batchStatement,
                "m.status = '0'", violations);
        requireAbsent(fileName, "batch" + capitalize(terminal) + "RoleMenu", batchStatement,
                "sys_menu", violations);
        requireAbsent(fileName, "batch" + capitalize(terminal) + "RoleMenu", batchStatement,
                otherTerminal + "_menu", violations);
        requireAbsent(fileName, "batch" + capitalize(terminal) + "RoleMenu", batchStatement,
                otherTerminal + "_role", violations);
    }

    private String extractStatement(String xml, String tagName, String id)
    {
        Pattern pattern = Pattern.compile("(?is)<" + tagName + "\\b[^>]*id=\"" + id + "\"[^>]*>(.*?)</"
                + tagName + ">");
        Matcher matcher = pattern.matcher(xml);
        if (!matcher.find())
        {
            throw new AssertionError("Mapper statement " + id + " not found");
        }
        return matcher.group(1).replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private void requireContains(String fileName, String statement, String source, String expected,
            List<String> violations)
    {
        if (!source.contains(expected.toLowerCase()))
        {
            violations.add(fileName + "#" + statement + " must contain " + expected);
        }
    }

    private void requireAbsent(String fileName, String statement, String source, String forbidden,
            List<String> violations)
    {
        if (source.contains(forbidden.toLowerCase()))
        {
            violations.add(fileName + "#" + statement + " must not contain " + forbidden);
        }
    }

    private String capitalize(String value)
    {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
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
            if (Files.isDirectory(candidate.resolve("seller/src/main/java"))
                    && Files.isDirectory(candidate.resolve("buyer/src/main/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
