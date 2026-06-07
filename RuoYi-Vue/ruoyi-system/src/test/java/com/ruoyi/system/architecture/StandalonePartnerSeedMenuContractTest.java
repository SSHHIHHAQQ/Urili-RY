package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Test;

public class StandalonePartnerSeedMenuContractTest
{
    @Test
    public void standalonePartnerSeedMustKeepDependentMenuTreeAndDirectLoginButtons() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path seed = backendRoot.resolve("sql/20260606_admin_partner_page_direct_login_seed.sql");
        String sql = readText(seed);
        List<String> violations = new ArrayList<>();
        Map<Long, MenuSeedRow> rows = parseMenuSeedRows(sql, violations);

        requireContains(violations, seed.getFileName().toString(), sql, "create procedure assert_sys_menu_slot");
        requireContains(violations, seed.getFileName().toString(), sql,
                "create procedure assert_sys_menu_signature_available");
        requireContains(violations, seed.getFileName().toString(), sql,
                "signal sqlstate '45000' set message_text = p_message");
        requireContains(violations, seed.getFileName().toString(), sql,
                "create procedure assert_partner_root_menu_exists");
        requireContains(violations, seed.getFileName().toString(), sql,
                "admin direct-login seed requires top_menu_seed partner root 2010");
        requireContains(violations, seed.getFileName().toString(), sql,
                "call assert_partner_root_menu_exists();");

        requireRow(rows, 2011L, "卖家管理", 2010L, "seller", "Seller/index", "Seller",
                "seller:admin:list", violations);
        requireRow(rows, 2012L, "买家管理", 2010L, "buyer", "Buyer/index", "Buyer",
                "buyer:admin:list", violations);
        requireRow(rows, 2205L, "卖家免密登录", 2011L, "#", "", "", "seller:admin:directLogin",
                violations);
        requireRow(rows, 2215L, "买家免密登录", 2012L, "#", "", "", "buyer:admin:directLogin",
                violations);
        requireContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_slot(2011, 2010, 'C', 'seller', 'Seller/index', 'Seller', 'seller:admin:list'");
        requireContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_slot(2012, 2010, 'C', 'buyer', 'Buyer/index', 'Buyer', 'buyer:admin:list'");
        requireContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_slot(2205, 2011, 'F', '#', '', '', 'seller:admin:directLogin'");
        requireContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_slot(2215, 2012, 'F', '#', '', '', 'buyer:admin:directLogin'");
        requireContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_signature_available(2011, 'seller', 'Seller/index', 'Seller', 'seller:admin:list'");
        requireContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_signature_available(2012, 'buyer', 'Buyer/index', 'Buyer', 'buyer:admin:list'");
        requireContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_signature_available(2205, '#', '', '', 'seller:admin:directLogin'");
        requireContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_signature_available(2215, '#', '', '', 'buyer:admin:directLogin'");
        requireNotContains(violations, seed.getFileName().toString(), sql,
                "(2010, '主体管理', 0, 5, 'partner', null, '', 'PartnerManagement'");
        requireNotContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_slot(2010");
        requireNotContains(violations, seed.getFileName().toString(), sql,
                "call assert_sys_menu_signature_available(2010");

        for (MenuSeedRow row : rows.values())
        {
            if (row.name.contains("?"))
            {
                violations.add("menu_id " + row.id + " must not contain replacement question marks in menu_name");
            }
            assertCanReachRoot(rows, row, violations);
        }

        if (!violations.isEmpty())
        {
            fail("standalone partner menu seed must keep a readable sys_menu tree under top-owned 2010:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalAdminMenuSeedDuplicatesMustStayConsistent() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path sqlRoot = backendRoot.resolve("sql");
        Map<Long, MenuSeedRow> canonicalRows = new HashMap<>();
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.list(sqlRoot))
        {
            paths.filter(path -> path.toString().endsWith(".sql"))
                    .forEach(path -> assertTerminalMenuSeedRowsConsistent(path, canonicalRows, violations));
        }

        if (!violations.isEmpty())
        {
            fail("duplicate seller/buyer admin sys_menu seeds must keep identical route, permission, and visibility fields:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertTerminalMenuSeedRowsConsistent(Path path, Map<Long, MenuSeedRow> canonicalRows,
            List<String> violations)
    {
        String sql;
        try
        {
            sql = readText(path);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to read " + path, e);
        }

        for (MenuSeedRow row : parseMenuSeedRowList(sql, path.getFileName().toString(), violations))
        {
            if (!isTerminalAdminMenuId(row.id))
            {
                continue;
            }

            MenuSeedRow canonical = canonicalRows.get(row.id);
            if (canonical == null)
            {
                canonicalRows.put(row.id, row);
                continue;
            }

            if (!canonical.signature().equals(row.signature()))
            {
                violations.add("menu_id " + row.id + " differs between " + canonical.source + " and "
                        + row.source + ": expected " + canonical.signature() + ", actual " + row.signature());
            }
        }
    }

    private Map<Long, MenuSeedRow> parseMenuSeedRows(String sql, List<String> violations)
    {
        Map<Long, MenuSeedRow> rows = new HashMap<>();
        for (MenuSeedRow row : parseMenuSeedRowList(sql, "standalone seed", violations))
        {
            if (rows.containsKey(row.id))
            {
                violations.add("duplicate sys_menu menu_id in standalone seed: " + row.id);
                continue;
            }
            rows.put(row.id, row);
        }
        return rows;
    }

    private List<MenuSeedRow> parseMenuSeedRowList(String sql, String source, List<String> violations)
    {
        List<MenuSeedRow> rows = new ArrayList<>();
        for (String rowText : extractValueRows(sql))
        {
            List<String> columns = splitSqlColumns(rowText);
            if (columns.size() < 14)
            {
                violations.add(source + " sys_menu seed row has too few columns: " + rowText);
                continue;
            }

            try
            {
                Long id = parseLongLiteral(columns.get(0));
                rows.add(new MenuSeedRow(id,
                        unquote(columns.get(1)),
                        parseLongLiteral(columns.get(2)),
                        columns.get(3).trim(),
                        unquote(columns.get(4)),
                        unquote(columns.get(5)),
                        unquote(columns.get(7)),
                        columns.get(10).trim(),
                        columns.get(11).trim(),
                        columns.get(12).trim(),
                        unquote(columns.get(13)),
                        source));
            }
            catch (NumberFormatException e)
            {
                violations.add(source + " cannot parse sys_menu seed row ids: " + rowText);
            }
        }
        return rows;
    }

    private List<String> extractValueRows(String sql)
    {
        String lower = sql.toLowerCase(Locale.ROOT);
        List<String> rows = new ArrayList<>();
        int searchFrom = 0;
        int insertIndex;
        while ((insertIndex = lower.indexOf("insert into sys_menu", searchFrom)) >= 0)
        {
            int statementEnd = lower.indexOf(";", insertIndex);
            int valuesIndex = lower.indexOf("values", insertIndex);
            if (valuesIndex < 0 || statementEnd >= 0 && valuesIndex > statementEnd)
            {
                searchFrom = statementEnd >= 0 ? statementEnd + 1 : insertIndex + 1;
                continue;
            }

            int updateIndex = lower.indexOf("on duplicate key update", valuesIndex);
            int blockEnd = updateIndex >= 0 && (statementEnd < 0 || updateIndex < statementEnd)
                    ? updateIndex
                    : statementEnd;
            if (blockEnd < 0)
            {
                blockEnd = sql.length();
            }

            rows.addAll(extractParenthesizedRows(sql.substring(valuesIndex + "values".length(), blockEnd)));
            searchFrom = blockEnd + 1;
        }
        return rows;
    }

    private List<String> extractParenthesizedRows(String block)
    {
        List<String> rows = new ArrayList<>();
        boolean inQuote = false;
        int depth = 0;
        int rowStart = -1;
        for (int i = 0; i < block.length(); i++)
        {
            char c = block.charAt(i);
            if (c == '\'')
            {
                if (inQuote && i + 1 < block.length() && block.charAt(i + 1) == '\'')
                {
                    i++;
                    continue;
                }
                inQuote = !inQuote;
            }
            if (inQuote)
            {
                continue;
            }
            if (c == '(')
            {
                if (depth == 0)
                {
                    rowStart = i + 1;
                }
                depth++;
            }
            else if (c == ')')
            {
                depth--;
                if (depth == 0 && rowStart >= 0)
                {
                    rows.add(block.substring(rowStart, i));
                    rowStart = -1;
                }
            }
        }
        return rows;
    }

    private List<String> splitSqlColumns(String rowText)
    {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        int functionDepth = 0;
        for (int i = 0; i < rowText.length(); i++)
        {
            char c = rowText.charAt(i);
            if (c == '\'')
            {
                current.append(c);
                if (inQuote && i + 1 < rowText.length() && rowText.charAt(i + 1) == '\'')
                {
                    current.append(rowText.charAt(++i));
                    continue;
                }
                inQuote = !inQuote;
                continue;
            }
            if (!inQuote)
            {
                if (c == '(')
                {
                    functionDepth++;
                }
                else if (c == ')' && functionDepth > 0)
                {
                    functionDepth--;
                }
                else if (c == ',' && functionDepth == 0)
                {
                    columns.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        columns.add(current.toString().trim());
        return columns;
    }

    private void requireRow(Map<Long, MenuSeedRow> rows, Long id, String name, Long parentId, String path,
            String component, String routeName, String perms, List<String> violations)
    {
        MenuSeedRow row = rows.get(id);
        if (row == null)
        {
            violations.add("standalone seed must contain menu_id " + id);
            return;
        }
        requireEquals(violations, id, "menu_name", name, row.name);
        requireEquals(violations, id, "parent_id", parentId, row.parentId);
        requireEquals(violations, id, "path", path, row.path);
        requireEquals(violations, id, "component", component, row.component);
        requireEquals(violations, id, "route_name", routeName, row.routeName);
        requireEquals(violations, id, "perms", perms, row.perms);
    }

    private void assertCanReachRoot(Map<Long, MenuSeedRow> rows, MenuSeedRow row, List<String> violations)
    {
        Set<Long> seen = new HashSet<>();
        MenuSeedRow current = row;
        while (current.parentId != 0)
        {
            if (!seen.add(current.id))
            {
                violations.add("standalone seed has a parent cycle at menu_id " + current.id);
                return;
            }
            if (current.parentId == 2010L)
            {
                return;
            }
            current = rows.get(current.parentId);
            if (current == null)
            {
                violations.add("menu_id " + row.id + " has orphan parent_id " + row.parentId);
                return;
            }
        }
    }

    private void requireEquals(List<String> violations, Long id, String field, Object expected, Object actual)
    {
        if (expected == null ? actual != null : !expected.equals(actual))
        {
            violations.add("menu_id " + id + " must keep " + field + " = " + expected + ", actual = " + actual);
        }
    }

    private boolean isTerminalAdminMenuId(Long id)
    {
        return id == 2010L || id == 2011L || id == 2012L || id >= 2200L && id <= 2323L;
    }

    private String unquote(String value)
    {
        String trimmed = value.trim();
        if ("null".equalsIgnoreCase(trimmed))
        {
            return null;
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'"))
        {
            return trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
        }
        return trimmed;
    }

    private Long parseLongLiteral(String value)
    {
        return Long.valueOf(unquote(value).trim());
    }

    private String readText(Path path) throws IOException
    {
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private void requireContains(List<String> violations, String fileName, String source, String expected)
    {
        if (!source.contains(expected))
        {
            violations.add(fileName + " must contain: " + expected);
        }
    }

    private void requireNotContains(List<String> violations, String fileName, String source, String unexpected)
    {
        if (source.contains(unexpected))
        {
            violations.add(fileName + " must not contain: " + unexpected);
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
            if (Files.isDirectory(candidate.resolve("seller/src/main/java"))
                    && Files.isDirectory(candidate.resolve("buyer/src/main/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }

    private static final class MenuSeedRow
    {
        private final Long id;
        private final String name;
        private final Long parentId;
        private final String orderNum;
        private final String path;
        private final String component;
        private final String routeName;
        private final String menuType;
        private final String visible;
        private final String status;
        private final String perms;
        private final String source;

        private MenuSeedRow(Long id, String name, Long parentId, String path, String component,
                String routeName, String perms)
        {
            this(id, name, parentId, "", path, component, routeName, "", "", "", perms, "");
        }

        private MenuSeedRow(Long id, String name, Long parentId, String orderNum, String path, String component,
                String routeName, String menuType, String visible, String status, String perms, String source)
        {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
            this.orderNum = orderNum;
            this.path = path;
            this.component = component;
            this.routeName = routeName;
            this.menuType = menuType;
            this.visible = visible;
            this.status = status;
            this.perms = perms;
            this.source = source;
        }

        private String signature()
        {
            return name + "|" + parentId + "|" + orderNum + "|" + path + "|" + component + "|" + routeName
                    + "|" + menuType + "|" + visible + "|" + status + "|" + perms;
        }
    }
}
