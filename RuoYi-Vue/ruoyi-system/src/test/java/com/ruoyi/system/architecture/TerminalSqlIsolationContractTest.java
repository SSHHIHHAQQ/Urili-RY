package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Test;

public class TerminalSqlIsolationContractTest
{
    private static final Pattern TERMINAL_LOG_TABLE_FROM_SYS_TEMPLATE = Pattern.compile(
            "create\\s+table\\s+if\\s+not\\s+exists\\s+"
                    + "(seller|buyer)_(login|oper)_log\\s+like\\s+sys_(logininfor|oper_log)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_SYS_USER_ACCOUNT_BACKFILL = Pattern.compile(
            "(migrate_(seller|buyer)_account_from_sys_user|join\\s+sys_user|left\\s+join\\s+sys_user)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_SYS_ROLE_SELLER_BUYER_MUTATION = Pattern.compile(
            "update\\s+sys_role[\\s\\S]*role_key\\s+in\\s*\\(\\s*'seller'\\s*,\\s*'buyer'\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    @Test
    public void terminalLogTablesMustUseExplicitIndependentDdl() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (String script : Arrays.asList(
                "sql/20260604_three_terminal_isolation_migration.sql",
                "sql/seller_buyer_management_seed.sql"))
        {
            Path path = backendRoot.resolve(script);
            String sql = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = TERMINAL_LOG_TABLE_FROM_SYS_TEMPLATE.matcher(sql);
            while (matcher.find())
            {
                violations.add(script + " must not derive " + matcher.group(1) + "_" + matcher.group(2)
                        + "_log from sys_" + matcher.group(3));
            }
        }

        if (!violations.isEmpty())
        {
            fail("seller/buyer terminal log tables must not be created with RuoYi sys_* table templates:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalLoginLogTablesMustCarryDirectLoginAuditFields() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (String script : Arrays.asList(
                "sql/20260604_three_terminal_isolation_migration.sql",
                "sql/20260607_terminal_login_log_direct_login_audit.sql",
                "sql/seller_buyer_management_seed.sql"))
        {
            Path path = backendRoot.resolve(script);
            String sql = readText(path);
            for (String table : Arrays.asList("seller_login_log", "buyer_login_log"))
            {
                for (String expected : Arrays.asList(
                        "direct_login",
                        "direct_login_ticket_id",
                        "acting_admin_id",
                        "acting_admin_name",
                        "direct_login_reason"))
                {
                    requireContains(violations, path.getFileName().toString() + " " + table, sql, expected);
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail("seller/buyer terminal login logs must carry direct-login audit fields:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalPortalMenuIdsMustStayInDisjointNumericRanges() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (String script : Arrays.asList(
                "sql/20260604_three_terminal_isolation_migration.sql",
                "sql/seller_buyer_management_seed.sql"))
        {
            Path path = backendRoot.resolve(script);
            String sql = readText(path);
            requireContains(violations, path.getFileName().toString(), sql,
                    "engine=innodb auto_increment=100000 comment = '卖家端菜单权限表'");
            requireContains(violations, path.getFileName().toString(), sql,
                    "engine=innodb auto_increment=200000 comment = '买家端菜单权限表'");
        }

        Path rangeMigration = backendRoot.resolve("sql/20260607_terminal_menu_id_range_isolation.sql");
        String rangeSql = readText(rangeMigration);
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "@confirm_terminal_menu_id_range_isolation");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "APPLY_TERMINAL_MENU_ID_RANGE_ISOLATION");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "seller_menu contains IDs outside seller range 100000-199999");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "buyer_menu contains IDs inside reserved seller range 100000-199999");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "buyer_menu contains IDs outside buyer range 200000-299999");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "update seller_role_menu\nset seller_menu_id = seller_menu_id + 100000");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "update seller_menu\nset parent_id = parent_id + 100000");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "update seller_menu\nset seller_menu_id = seller_menu_id + 100000");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "update buyer_role_menu\nset buyer_menu_id = buyer_menu_id + 200000");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "update buyer_menu\nset parent_id = parent_id + 200000");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "update buyer_menu\nset buyer_menu_id = buyer_menu_id + 200000");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "create procedure reset_terminal_menu_auto_increment");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "call reset_terminal_menu_auto_increment('seller_menu', 'seller_menu_id', 100000)");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "call reset_terminal_menu_auto_increment('buyer_menu', 'buyer_menu_id', 200000)");
        requireContains(violations, rangeMigration.getFileName().toString(), rangeSql,
                "select greatest(', p_floor, ', coalesce(max(', p_id_column, '), 0) + 1)");

        if (!violations.isEmpty())
        {
            fail("seller/buyer terminal menu numeric IDs must stay in disjoint ranges:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalPermissionSeedsMustAssertMenuIdRangesBeforeMenuInserts() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path sqlRoot = backendRoot.resolve("sql");
        List<Path> scripts;
        try (java.util.stream.Stream<Path> paths = Files.walk(sqlRoot))
        {
            scripts = paths
                    .filter(path -> path.toString().endsWith(".sql"))
                    .collect(Collectors.toList());
        }

        List<String> violations = new ArrayList<>();
        for (Path path : scripts)
        {
            String sql = readText(path);
            String lowerSql = sql.toLowerCase();
            boolean insertsSellerMenu = lowerSql.contains("insert into seller_menu");
            boolean insertsBuyerMenu = lowerSql.contains("insert into buyer_menu");
            if (!insertsSellerMenu && !insertsBuyerMenu)
            {
                continue;
            }

            String scriptName = backendRoot.relativize(path).toString().replace('\\', '/');
            requireContains(violations, scriptName, sql, "create procedure assert_terminal_menu_range_ready");
            requireContains(violations, scriptName, sql, "call assert_terminal_menu_range_ready();");
            requireContains(violations, scriptName, sql, "drop procedure if exists assert_terminal_menu_range_ready");
            requireMenuRangeGuardRunsBeforeInsert(scriptName, lowerSql, insertsSellerMenu, insertsBuyerMenu, violations);

            if (insertsSellerMenu)
            {
                requireContains(violations, scriptName, sql,
                        "seller_menu contains IDs outside seller range 100000-199999");
                requireContains(violations, scriptName, sql,
                        "seller_menu auto_increment must be >= 100000 before terminal menu seed inserts");
            }
            if (insertsBuyerMenu)
            {
                requireContains(violations, scriptName, sql,
                        "buyer_menu contains IDs outside buyer range 200000-299999");
                requireContains(violations, scriptName, sql,
                        "buyer_menu auto_increment must be >= 200000 before terminal menu seed inserts");
            }
        }

        if (!violations.isEmpty())
        {
            fail("terminal permission seed scripts must fail closed when menu ID ranges are not ready:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalOperLogTablesMustCarryDirectLoginAuditFields() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (String script : Arrays.asList(
                "sql/20260604_three_terminal_isolation_migration.sql",
                "sql/20260607_terminal_oper_log_direct_login_audit.sql",
                "sql/seller_buyer_management_seed.sql"))
        {
            Path path = backendRoot.resolve(script);
            String sql = readText(path);
            for (String table : Arrays.asList("seller_oper_log", "buyer_oper_log"))
            {
                for (String expected : Arrays.asList(
                        "direct_login",
                        "direct_login_ticket_id",
                        "acting_admin_id",
                        "acting_admin_name",
                        "direct_login_reason"))
                {
                    requireOperLogAuditColumn(path.getFileName().toString(), sql, table, expected, violations);
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail("seller/buyer terminal operation logs must carry direct-login audit fields:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void currentIsolationMigrationMustNotBackfillTerminalAccountsFromSysUser() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path path = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String sql = Files.readString(path, StandardCharsets.UTF_8);
        Matcher matcher = LEGACY_SYS_USER_ACCOUNT_BACKFILL.matcher(sql);
        List<String> violations = new ArrayList<>();

        while (matcher.find())
        {
            violations.add(path.getFileName() + " contains legacy sys_user account backfill token: "
                    + matcher.group(1));
        }

        if (!violations.isEmpty())
        {
            fail("current three-terminal isolation migration must not backfill seller/buyer accounts from sys_user;\n"
                    + "use the explicit legacy helper only for old mixed-account databases:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void currentIsolationMigrationMustNotMutateLegacySysRoles() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path path = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String sql = Files.readString(path, StandardCharsets.UTF_8);

        if (LEGACY_SYS_ROLE_SELLER_BUYER_MUTATION.matcher(sql).find())
        {
            fail("current three-terminal isolation migration must not mutate legacy sys_role seller/buyer roles;\n"
                    + "use sql/20260606_legacy_disable_sys_seller_buyer_roles.sql only after the target database is confirmed");
        }
    }

    @Test
    public void terminalLogScopeIndexPatchMustCoverSubjectAndAccountQueries() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path path = backendRoot.resolve("sql/20260606_terminal_log_scope_indexes.sql");
        String sql = readText(path);
        List<String> violations = new ArrayList<>();

        requireContains(violations, path.getFileName().toString(), sql, "create procedure assert_column_exists");
        requireContains(violations, path.getFileName().toString(), sql, "create procedure recreate_index_if_mismatch");
        requireContains(violations, path.getFileName().toString(), sql, "create procedure assert_index_definition");
        requireContains(violations, path.getFileName().toString(), sql,
                "Run 20260604_three_terminal_isolation_migration.sql before log indexes");

        for (String expected : Arrays.asList(
                "call recreate_index_if_mismatch(\n  'seller_login_log',\n  'idx_seller_login_log_account_time',\n  'seller_account_id,login_time'",
                "call recreate_index_if_mismatch(\n  'seller_login_log',\n  'idx_seller_login_log_seller_time',\n  'seller_id,login_time'",
                "call recreate_index_if_mismatch(\n  'buyer_login_log',\n  'idx_buyer_login_log_account_time',\n  'buyer_account_id,login_time'",
                "call recreate_index_if_mismatch(\n  'buyer_login_log',\n  'idx_buyer_login_log_buyer_time',\n  'buyer_id,login_time'",
                "call recreate_index_if_mismatch(\n  'seller_oper_log',\n  'idx_seller_oper_log_account_time',\n  'seller_account_id,oper_time'",
                "call recreate_index_if_mismatch(\n  'seller_oper_log',\n  'idx_seller_oper_log_seller_time',\n  'seller_id,oper_time'",
                "call recreate_index_if_mismatch(\n  'buyer_oper_log',\n  'idx_buyer_oper_log_account_time',\n  'buyer_account_id,oper_time'",
                "call recreate_index_if_mismatch(\n  'buyer_oper_log',\n  'idx_buyer_oper_log_buyer_time',\n  'buyer_id,oper_time'",
                "call assert_index_definition('seller_login_log', 'idx_seller_login_log_account_time'",
                "call assert_index_definition('buyer_oper_log', 'idx_buyer_oper_log_buyer_time'"))
        {
            if (!sql.contains(expected))
            {
                violations.add(path.getFileName() + " must contain " + expected);
            }
        }

        if (!violations.isEmpty())
        {
            fail("terminal log scope indexes must cover subject/account scoped queries:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalOwnerAccountUniqueColumnsMustKeepStoredGeneratedExpressionChecks() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (String script : Arrays.asList(
                "sql/20260604_three_terminal_isolation_migration.sql",
                "sql/20260605_terminal_owner_account_unique_constraint.sql"))
        {
            Path path = backendRoot.resolve(script);
            String sql = readText(path);
            assertOwnerExpressionContract(path.getFileName().toString(), sql, "seller_account",
                    "owner_unique_seller_id", "seller_id", "uk_seller_account_owner", violations);
            assertOwnerExpressionContract(path.getFileName().toString(), sql, "buyer_account",
                    "owner_unique_buyer_id", "buyer_id", "uk_buyer_account_owner", violations);
        }

        Path seed = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String seedSql = readText(seed);
        requireContains(violations, seed.getFileName().toString(), seedSql,
                "owner_unique_seller_id bigint(20) generated always as (case when account_role = 'OWNER' then seller_id else null end) stored");
        requireContains(violations, seed.getFileName().toString(), seedSql,
                "owner_unique_buyer_id bigint(20) generated always as (case when account_role = 'OWNER' then buyer_id else null end) stored");
        requireContains(violations, seed.getFileName().toString(), seedSql,
                "unique key uk_seller_account_owner (owner_unique_seller_id)");
        requireContains(violations, seed.getFileName().toString(), seedSql,
                "unique key uk_buyer_account_owner (owner_unique_buyer_id)");

        if (!violations.isEmpty())
        {
            fail("terminal OWNER account uniqueness must stay on stored generated columns with expression checks:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void legacyDisableSysSellerBuyerRolesMustFailFastAndBeIdempotent() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path path = backendRoot.resolve("sql/20260606_legacy_disable_sys_seller_buyer_roles.sql");
        String sql = readText(path);
        List<String> violations = new ArrayList<>();

        requireContains(violations, path.getFileName().toString(), sql, "create procedure assert_table_exists");
        requireContains(violations, path.getFileName().toString(), sql,
                "create procedure assert_no_active_terminal_sys_role_bindings");
        for (String table : Arrays.asList("seller_account", "buyer_account", "seller_role", "buyer_role"))
        {
            requireContains(violations, path.getFileName().toString(), sql,
                    "call assert_table_exists('" + table + "'");
        }
        requireContains(violations, path.getFileName().toString(), sql, "inner join sys_user_role ur");
        requireContains(violations, path.getFileName().toString(), sql, "inner join sys_user u");
        requireContains(violations, path.getFileName().toString(), sql,
                "call assert_no_active_terminal_sys_role_bindings()");
        requireContains(violations, path.getFileName().toString(), sql,
                "case\n      when coalesce(remark, '') like '%three-terminal isolation moved terminal roles out of sys_role%'");

        if (!violations.isEmpty())
        {
            fail("legacy sys_role seller/buyer cleanup must be explicit, guarded, and idempotent:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalPermissionMapperWritesMustKeepSubjectGuardsInSql() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();
        String sellerPermission = readText(backendRoot.resolve(
                "seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml"));
        String buyerPermission = readText(backendRoot.resolve(
                "buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml"));
        String sellerDept = readText(backendRoot.resolve(
                "seller/src/main/resources/mapper/seller/SellerPortalDeptMapper.xml"));
        String buyerDept = readText(backendRoot.resolve(
                "buyer/src/main/resources/mapper/buyer/BuyerPortalDeptMapper.xml"));

        requireContains(violations, "SellerPortalPermissionMapper.xml", sellerPermission,
                "inner join seller_account a on a.seller_account_id = ar.seller_account_id");
        requireContains(violations, "SellerPortalPermissionMapper.xml", sellerPermission,
                "delete ar\n        from seller_account_role ar");
        requireContains(violations, "SellerPortalPermissionMapper.xml", sellerPermission,
                "where a.seller_id = #{sellerId}");
        requireContains(violations, "SellerPortalPermissionMapper.xml", sellerPermission,
                "inner join seller_role r on r.seller_role_id = rm.seller_role_id");
        requireContains(violations, "SellerPortalPermissionMapper.xml", sellerPermission,
                "where r.seller_id = #{sellerId}");

        requireContains(violations, "BuyerPortalPermissionMapper.xml", buyerPermission,
                "inner join buyer_account a on a.buyer_account_id = ar.buyer_account_id");
        requireContains(violations, "BuyerPortalPermissionMapper.xml", buyerPermission,
                "delete ar\n        from buyer_account_role ar");
        requireContains(violations, "BuyerPortalPermissionMapper.xml", buyerPermission,
                "where a.buyer_id = #{buyerId}");
        requireContains(violations, "BuyerPortalPermissionMapper.xml", buyerPermission,
                "inner join buyer_role r on r.buyer_role_id = rm.buyer_role_id");
        requireContains(violations, "BuyerPortalPermissionMapper.xml", buyerPermission,
                "where r.buyer_id = #{buyerId}");

        requireContains(violations, "SellerPortalDeptMapper.xml", sellerDept,
                "where seller_id = #{sellerId}\n          and dept_id = #{deptId}");
        requireContains(violations, "BuyerPortalDeptMapper.xml", buyerDept,
                "where buyer_id = #{buyerId}\n          and dept_id = #{deptId}");

        if (!violations.isEmpty())
        {
            fail("seller/buyer permission and department mapper writes must keep subject guards in SQL:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalAccountMapperWritesMustKeepSubjectGuardsInSql() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();
        String sellerMapper = readText(backendRoot.resolve(
                "seller/src/main/resources/mapper/seller/SellerMapper.xml"));
        String buyerMapper = readText(backendRoot.resolve(
                "buyer/src/main/resources/mapper/buyer/BuyerMapper.xml"));
        String sellerReset = extractXmlStatement(sellerMapper, "update", "resetSellerAccountPassword");
        String sellerLoginInfo = extractXmlStatement(sellerMapper, "update", "updateSellerAccountLoginInfo");
        String buyerReset = extractXmlStatement(buyerMapper, "update", "resetBuyerAccountPassword");
        String buyerLoginInfo = extractXmlStatement(buyerMapper, "update", "updateBuyerAccountLoginInfo");

        requireContains(violations, "resetSellerAccountPassword", sellerReset, "and seller_id = #{sellerId}");
        requireContains(violations, "updateSellerAccountLoginInfo", sellerLoginInfo, "and seller_id = #{sellerId}");
        requireContains(violations, "resetBuyerAccountPassword", buyerReset, "and buyer_id = #{buyerId}");
        requireContains(violations, "updateBuyerAccountLoginInfo", buyerLoginInfo, "and buyer_id = #{buyerId}");

        if (!violations.isEmpty())
        {
            fail("seller/buyer account mapper writes must keep subject guards in SQL:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertOwnerExpressionContract(String fileName, String sql, String table, String column,
            String subjectColumn, String indexName, List<String> violations)
    {
        requireContains(violations, fileName, sql, "assert_owner_generated_column");
        requireContains(violations, fileName, sql, "generation_expression");
        requireContains(violations, fileName, sql, "upper(v_extra) not like '%STORED GENERATED%'");
        requireContains(violations, fileName, sql, "v_normalized_expression <> v_expected_expression");
        requireContains(violations, fileName, sql, "replace(v_normalized_expression, '\\\\', '')");
        requireContains(violations, fileName, sql, "replace(v_normalized_expression, '_utf8mb3', '')");
        requireContains(violations, fileName, sql, "replace(v_normalized_expression, '_utf8mb4', '')");
        requireContains(violations, fileName, sql,
                "set v_expected_expression = concat('casewhenaccount_role=ownerthen', lower(p_subject_column), 'elsenullend')");
        requireContains(violations, fileName, sql,
                "call add_column_if_missing('" + table + "', '" + column
                        + "', 'bigint(20) generated always as (case when account_role = ''OWNER'' then "
                        + subjectColumn + " else null end) stored')");
        requireContains(violations, fileName, sql,
                "call assert_owner_generated_column('" + table + "', '" + column + "', '" + subjectColumn + "'");
        requireContains(violations, fileName, sql, "create procedure recreate_index_if_mismatch");
        requireContains(violations, fileName, sql, "create procedure assert_index_definition");
        requireContains(violations, fileName, sql,
                "call recreate_index_if_mismatch('" + table + "', '" + indexName + "',\n  '" + column
                        + "', 0, 'unique key " + indexName + " (" + column + ")')");
        requireContains(violations, fileName, sql,
                "call assert_index_definition('" + table + "', '" + indexName + "',\n  '" + column
                        + "', 0");
    }

    private String readText(Path path) throws IOException
    {
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private String extractXmlStatement(String xml, String tagName, String id)
    {
        String openToken = "<" + tagName + " id=\"" + id + "\"";
        int openStart = xml.indexOf(openToken);
        if (openStart < 0)
        {
            return "";
        }
        int bodyStart = xml.indexOf('>', openStart);
        if (bodyStart < 0)
        {
            return "";
        }
        String closeToken = "</" + tagName + ">";
        int closeStart = xml.indexOf(closeToken, bodyStart);
        if (closeStart < 0)
        {
            return "";
        }
        return xml.substring(bodyStart + 1, closeStart);
    }

    private void requireOperLogAuditColumn(String fileName, String sql, String table, String column,
            List<String> violations)
    {
        String createTable = extractCreateTableStatement(sql, table);
        if (!createTable.isEmpty())
        {
            requireContains(violations, fileName + " " + table + " DDL", createTable, column);
            return;
        }
        requireContains(violations, fileName + " " + table + " patch", sql,
                "call add_column_if_missing('" + table + "', '" + column + "'");
        requireContains(violations, fileName + " " + table + " patch", sql,
                "call assert_column_exists('" + table + "', '" + column + "'");
    }

    private String extractCreateTableStatement(String sql, String table)
    {
        String openToken = "create table if not exists " + table;
        String lowerSql = sql.toLowerCase();
        int openStart = lowerSql.indexOf(openToken);
        if (openStart < 0)
        {
            return "";
        }
        int closeStart = lowerSql.indexOf(") engine=", openStart);
        if (closeStart < 0)
        {
            return "";
        }
        return sql.substring(openStart, closeStart);
    }

    private void requireContains(List<String> violations, String fileName, String source, String expected)
    {
        if (!source.contains(expected))
        {
            violations.add(fileName + " must contain: " + expected);
        }
    }

    private void requireMenuRangeGuardRunsBeforeInsert(String scriptName, String lowerSql, boolean insertsSellerMenu,
            boolean insertsBuyerMenu, List<String> violations)
    {
        int callIndex = lowerSql.indexOf("call assert_terminal_menu_range_ready();");
        int firstInsertIndex = Integer.MAX_VALUE;
        if (insertsSellerMenu)
        {
            firstInsertIndex = Math.min(firstInsertIndex, lowerSql.indexOf("insert into seller_menu"));
        }
        if (insertsBuyerMenu)
        {
            firstInsertIndex = Math.min(firstInsertIndex, lowerSql.indexOf("insert into buyer_menu"));
        }
        if (callIndex < 0 || callIndex > firstInsertIndex)
        {
            violations.add(scriptName + " must call assert_terminal_menu_range_ready before terminal menu inserts");
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
}
