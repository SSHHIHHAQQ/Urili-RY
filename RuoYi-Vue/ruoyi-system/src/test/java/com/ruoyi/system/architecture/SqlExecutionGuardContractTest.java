package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class SqlExecutionGuardContractTest
{
    private static final Pattern HIGH_IMPACT_STATEMENT = Pattern.compile(
            "(?im)^\\s*(?:insert(?:\\s+ignore)?\\s+into|replace\\s+into|update\\s+|delete\\s+(?:from\\s+|\\w+\\s+from\\s+)|alter\\s+table|create\\s+table|create\\s+(?:or\\s+replace\\s+)?view|create\\s+(?:unique\\s+)?index|drop\\s+index|drop\\s+table|drop\\s+view|truncate\\s+table|rename\\s+table)\\b");

    private static final Pattern HIGH_IMPACT_SQL_HINT = Pattern.compile(
            "(?i)\\b(?:insert(?:\\s+ignore)?\\s+into|replace\\s+into|update\\s+|delete\\s+(?:from\\s+|\\w+\\s+from\\s+)|alter\\s+table|create\\s+table|create\\s+(?:or\\s+replace\\s+)?view|create\\s+(?:unique\\s+)?index|drop\\s+index|drop\\s+table|drop\\s+view|truncate\\s+table|rename\\s+table)\\b");

    private static final Pattern DYNAMIC_HIGH_IMPACT_SQL_HINT = Pattern.compile(
            "(?is)\\bset\\s+@(?:ddl|dml|sql)\\s*=\\s*(?:concat\\s*\\([^;]*?)?'\\s*(?:insert(?:\\s+ignore)?\\s+into|replace\\s+into|update\\s+|delete\\s+(?:from\\s+|\\w+\\s+from\\s+)|alter\\s+table|create\\s+table|create\\s+(?:or\\s+replace\\s+)?view|create\\s+(?:unique\\s+)?index|drop\\s+index|drop\\s+table|drop\\s+view|truncate\\s+table|rename\\s+table)");

    private static final Pattern DYNAMIC_HIGH_IMPACT_EXECUTION_POINT = Pattern.compile(
            "(?im)^\\s*prepare\\s+\\w+\\s+from\\s+@(?:ddl|dml)\\s*;");

    private static final Pattern BARE_CREATE_INDEX_STATEMENT = Pattern.compile(
            "(?im)^\\s*create\\s+(?:unique\\s+)?index\\b");

    private static final Pattern DESTRUCTIVE_DROP_TABLE_STATEMENT = Pattern.compile(
            "(?im)^\\s*drop\\s+table\\s+if\\s+exists\\s+");

    private static final Pattern DATED_SQL_FILE = Pattern.compile("20\\d{6}.*\\.sql");

    private static final Pattern CONFIRM_CALL = Pattern.compile(
            "(?i)call\\s+assert_[a-z0-9_]+_confirmed\\s*\\(\\s*\\)\\s*;");

    private static final Pattern CONFIRM_CALL_NAME = Pattern.compile(
            "(?i)call\\s+(assert_[a-z0-9_]+_confirmed)\\s*\\(\\s*\\)\\s*;");

    private static final Pattern CONFIRMED_PROCEDURE_DECLARATION = Pattern.compile(
            "(?i)create\\s+procedure\\s+(assert_[a-z0-9_]+_confirmed)\\s*\\(");

    private static final Pattern TERMINAL_MENU_MUTATION = Pattern.compile(
            "(?is)(?:insert\\s+into\\s+(seller_menu|buyer_menu)\\b.*?|update\\s+(seller_menu|buyer_menu)\\b.*?|delete\\s+from\\s+(seller_menu|buyer_menu)\\b.*?)\\s*;");

    private static final Pattern TERMINAL_PAGE_MENU_WITH_BLANK_COMPONENT = Pattern.compile(
            "(?is)(?:\\bnull\\b|'')\\s*,\\s*''\\s*,\\s*''\\s*,\\s*1\\s*,\\s*0\\s*,\\s*'C'");

    private static final Pattern TERMINAL_MENU_UPDATE_WITH_BLANK_COMPONENT = Pattern.compile(
            "(?is)\\bupdate\\s+(?:seller_menu|buyer_menu)\\b[^;]*\\bcomponent\\s*=\\s*(?:''|null)");

    private static final Pattern TERMINAL_MENU_UPDATE_WITH_BLANK_PERMS = Pattern.compile(
            "(?is)\\bupdate\\s+(?:seller_menu|buyer_menu)\\b[^;]*\\bperms\\s*=\\s*(?:''|null)");

    @Test
    public void highImpactSqlScriptsMustRequireExplicitConfirmToken() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();

        assertGuard(backendRoot, "sql/20260604_three_terminal_isolation_migration.sql",
                "@confirm_three_terminal_isolation_migration",
                "APPLY_THREE_TERMINAL_ISOLATION_MIGRATION", violations);
        assertGuard(backendRoot, "sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql",
                "@confirm_legacy_sys_user_backfill",
                "BACKFILL_TERMINAL_ACCOUNTS_FROM_SYS_USER", violations);
        assertGuard(backendRoot, "sql/20260604_source_product_library_sku_candidate_fields.sql",
                "@confirm_source_product_library_sku_candidate_fields",
                "APPLY_SOURCE_PRODUCT_LIBRARY_SKU_CANDIDATE_FIELDS", violations);
        assertGuard(backendRoot, "sql/20260604_upstream_system_code_correction.sql",
                "@confirm_upstream_system_code_correction",
                "APPLY_UPSTREAM_SYSTEM_CODE_CORRECTION", violations);
        assertGuard(backendRoot, "sql/20260604_portal_direct_login_ticket.sql",
                "@confirm_portal_direct_login_ticket_migration",
                "APPLY_PORTAL_DIRECT_LOGIN_TICKET_MIGRATION", violations);
        assertGuard(backendRoot, "sql/20260604_portal_account_list_permission_seed.sql",
                "@confirm_portal_account_list_permission_seed",
                "APPLY_PORTAL_ACCOUNT_LIST_PERMISSION_SEED", violations);
        assertGuard(backendRoot, "sql/20260604_portal_dept_role_list_permission_seed.sql",
                "@confirm_portal_dept_role_list_permission_seed",
                "APPLY_PORTAL_DEPT_ROLE_LIST_PERMISSION_SEED", violations);
        assertGuard(backendRoot, "sql/20260604_portal_product_category_permission_seed.sql",
                "@confirm_portal_product_category_permission_seed",
                "APPLY_PORTAL_PRODUCT_CATEGORY_PERMISSION_SEED", violations);
        assertGuard(backendRoot, "sql/20260604_seller_product_schema_permission_seed.sql",
                "@confirm_seller_product_schema_permission_seed",
                "APPLY_SELLER_PRODUCT_SCHEMA_PERMISSION_SEED", violations);
        assertGuard(backendRoot, "sql/20260604_buyer_product_schema_permission_seed.sql",
                "@confirm_buyer_product_schema_permission_seed",
                "APPLY_BUYER_PRODUCT_SCHEMA_PERMISSION_SEED", violations);
        assertGuard(backendRoot, "sql/20260604_product_category_attribute_seed.sql",
                "@confirm_product_category_attribute_seed",
                "APPLY_PRODUCT_CATEGORY_ATTRIBUTE_SEED", violations);
        assertGuard(backendRoot, "sql/20260605_mall_product_distribution_seed.sql",
                "@confirm_mall_product_distribution_seed",
                "APPLY_MALL_PRODUCT_DISTRIBUTION_SEED", violations);
        assertGuard(backendRoot, "sql/20260605_seller_account_lock_control.sql",
                "@confirm_seller_account_lock_control",
                "APPLY_SELLER_ACCOUNT_LOCK_CONTROL", violations);
        assertGuard(backendRoot, "sql/20260605_buyer_account_lock_control.sql",
                "@confirm_buyer_account_lock_control",
                "APPLY_BUYER_ACCOUNT_LOCK_CONTROL", violations);
        assertGuard(backendRoot, "sql/20260605_terminal_owner_account_unique_constraint.sql",
                "@confirm_terminal_owner_account_unique_constraint",
                "APPLY_TERMINAL_OWNER_ACCOUNT_UNIQUE_CONSTRAINT", violations);
        assertGuard(backendRoot, "sql/20260606_admin_partner_page_direct_login_seed.sql",
                "@confirm_admin_partner_page_direct_login_seed",
                "APPLY_ADMIN_PARTNER_PAGE_DIRECT_LOGIN_SEED", violations);
        assertGuard(backendRoot, "sql/20260606_admin_partner_role_menu_grant.sql",
                "@confirm_admin_partner_role_menu_grant",
                "GRANT_ADMIN_PARTNER_MENUS", violations);
        assertGuard(backendRoot, "sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql",
                "@confirm_admin_partner_non_admin_button_cleanup",
                "CLEANUP_NON_ADMIN_PARTNER_BUTTON_GRANTS", violations);
        assertGuard(backendRoot, "sql/20260606_legacy_disable_sys_seller_buyer_roles.sql",
                "@confirm_legacy_sys_role_cleanup",
                "DISABLE_SYS_ROLE_SELLER_BUYER", violations);
        assertGuard(backendRoot, "sql/20260606_terminal_log_scope_indexes.sql",
                "@confirm_terminal_log_scope_indexes",
                "APPLY_TERMINAL_LOG_SCOPE_INDEXES", violations);
        assertGuard(backendRoot, "sql/20260606_source_warehouse_stock_menu_rename.sql",
                "@confirm_source_warehouse_stock_menu_rename",
                "APPLY_SOURCE_WAREHOUSE_STOCK_MENU_RENAME", violations);
        assertGuard(backendRoot, "sql/20260606_product_spu_warehouse_binding.sql",
                "@confirm_product_spu_warehouse_binding",
                "APPLY_PRODUCT_SPU_WAREHOUSE_BINDING", violations);
        assertGuard(backendRoot, "sql/20260606_upstream_inventory_dimension_sync.sql",
                "@confirm_upstream_inventory_dimension_sync",
                "APPLY_UPSTREAM_INVENTORY_DIMENSION_SYNC", violations);
        assertGuard(backendRoot, "sql/20260606_upstream_sync_staging_diff.sql",
                "@confirm_upstream_sync_staging_diff",
                "APPLY_UPSTREAM_SYNC_STAGING_DIFF", violations);
        assertGuard(backendRoot, "sql/20260607_source_product_read_model.sql",
                "@confirm_source_product_read_model",
                "APPLY_SOURCE_PRODUCT_READ_MODEL", violations);
        assertGuard(backendRoot, "sql/20260607_source_warehouse_stock_read_model.sql",
                "@confirm_source_warehouse_stock_read_model",
                "APPLY_SOURCE_WAREHOUSE_STOCK_READ_MODEL", violations);
        assertGuard(backendRoot, "sql/20260607_terminal_login_log_direct_login_audit.sql",
                "@confirm_terminal_login_log_direct_login_audit",
                "APPLY_TERMINAL_LOGIN_LOG_DIRECT_LOGIN_AUDIT", violations);
        assertGuard(backendRoot, "sql/20260607_terminal_oper_log_direct_login_audit.sql",
                "@confirm_terminal_oper_log_direct_login_audit",
                "APPLY_TERMINAL_OPER_LOG_DIRECT_LOGIN_AUDIT", violations);
        assertGuard(backendRoot, "sql/20260607_terminal_menu_id_range_isolation.sql",
                "@confirm_terminal_menu_id_range_isolation",
                "APPLY_TERMINAL_MENU_ID_RANGE_ISOLATION", violations);
        assertGuard(backendRoot, "sql/20260607_portal_self_audit_permission_seed.sql",
                "@confirm_portal_self_audit_permission_seed",
                "APPLY_PORTAL_SELF_AUDIT_PERMISSION_SEED", violations);
        assertGuard(backendRoot, "sql/20260607_admin_partner_owner_reset_permission_cleanup.sql",
                "@confirm_admin_partner_owner_reset_permission_cleanup",
                "CLEANUP_ADMIN_PARTNER_OWNER_RESET_PERMISSION", violations);
        assertGuard(backendRoot, "sql/20260607_upstream_task_component_split.sql",
                "@confirm_upstream_task_component_split",
                "APPLY_UPSTREAM_TASK_COMPONENT_SPLIT", violations);
        assertGuard(backendRoot, "sql/20260607_upstream_pairing_role_binding.sql",
                "@confirm_upstream_pairing_role_binding",
                "APPLY_UPSTREAM_PAIRING_ROLE_BINDING", violations);
        assertGuard(backendRoot, "sql/20260607_product_sku_source_binding.sql",
                "@confirm_product_sku_source_binding",
                "APPLY_PRODUCT_SKU_SOURCE_BINDING", violations);
        assertGuard(backendRoot, "sql/20260607_inventory_overview_platform_stock.sql",
                "@confirm_inventory_overview_platform_stock",
                "APPLY_INVENTORY_OVERVIEW_PLATFORM_STOCK", violations);
        assertGuard(backendRoot, "sql/20260608_inventory_overview_sku_baseline_refresh.sql",
                "@confirm_inventory_overview_sku_baseline_refresh",
                "APPLY_INVENTORY_OVERVIEW_SKU_BASELINE_REFRESH", violations);
        assertGuard(backendRoot, "sql/20260608_terminal_menu_auto_increment_reset.sql",
                "@confirm_terminal_menu_auto_increment_reset",
                "APPLY_TERMINAL_MENU_AUTO_INCREMENT_RESET", violations);
        assertGuard(backendRoot, "sql/20260608_product_review.sql",
                "@confirm_product_review",
                "APPLY_PRODUCT_REVIEW", violations);
        assertGuard(backendRoot, "sql/20260608_product_center_menu_seed.sql",
                "@confirm_product_center_menu_seed",
                "APPLY_PRODUCT_CENTER_MENU_SEED", violations);
        assertGuard(backendRoot, "sql/20260609_inventory_adjustment_review.sql",
                "@confirm_inventory_adjustment_review",
                "APPLY_INVENTORY_ADJUSTMENT_REVIEW", violations);
        assertGuard(backendRoot, "sql/20260609_inventory_auto_wms_stock_sync_policy.sql",
                "@confirm_inventory_auto_wms_stock_sync_policy",
                "APPLY_INVENTORY_AUTO_WMS_STOCK_SYNC_POLICY", violations);
        assertGuard(backendRoot, "sql/20260609_product_code_pool_job.sql",
                "@confirm_product_code_pool_job",
                "APPLY_PRODUCT_CODE_POOL_JOB", violations);
        assertGuard(backendRoot, "sql/20260610_terminal_portal_home_menu_seed.sql",
                "@confirm_terminal_portal_home_menu_seed",
                "APPLY_TERMINAL_PORTAL_HOME_MENU_SEED", violations);
        assertGuard(backendRoot, "sql/20260610_portal_self_management_permission_seed.sql",
                "@confirm_portal_self_management_permission_seed",
                "APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED", violations);
        assertGuard(backendRoot, "sql/20260610_terminal_owner_product_permission_cleanup.sql",
                "@confirm_terminal_owner_product_permission_cleanup",
                "CLEAN_TERMINAL_OWNER_PRODUCT_PERMISSION_GRANTS", violations);
        assertGuard(backendRoot, "sql/20260608_overseas_channel_carrier_menu_restructure.sql",
                "@confirm_overseas_channel_carrier_menu_restructure",
                "APPLY_OVERSEAS_CHANNEL_CARRIER_MENU_RESTRUCTURE", violations);
        assertGuard(backendRoot, "sql/20260610_customer_logistics_channel_management.sql",
                "@confirm_customer_logistics_channel_management",
                "APPLY_CUSTOMER_LOGISTICS_CHANNEL_MANAGEMENT", violations);
        assertGuard(backendRoot, "sql/20260611_customer_channel_quote_mapping.sql",
                "@confirm_customer_channel_quote_mapping",
                "APPLY_CUSTOMER_CHANNEL_QUOTE_MAPPING", violations);
        assertGuard(backendRoot, "sql/20260611_quote_scheme_value_fee_rule.sql",
                "@confirm_quote_scheme_value_fee_rule",
                "APPLY_QUOTE_SCHEME_VALUE_FEE_RULE", violations);
        assertGuard(backendRoot, "sql/20260610_system_channel_main_channel_pairing_scope.sql",
                "@confirm_system_channel_main_channel_pairing_scope",
                "APPLY_SYSTEM_CHANNEL_MAIN_CHANNEL_PAIRING_SCOPE", violations);
        assertGuard(backendRoot, "sql/seller_buyer_management_seed.sql",
                "@confirm_seller_buyer_management_seed",
                "APPLY_SELLER_BUYER_MANAGEMENT_SEED", violations);
        assertGuard(backendRoot, "sql/warehouse_management_seed.sql",
                "@confirm_warehouse_management_seed",
                "APPLY_WAREHOUSE_MANAGEMENT_SEED", violations);
        assertGuard(backendRoot, "sql/top_menu_seed.sql",
                "@confirm_top_menu_seed",
                "APPLY_TOP_MENU_SEED", violations);
        assertGuard(backendRoot, "sql/business_menu_seed.sql",
                "@confirm_business_menu_seed",
                "APPLY_BUSINESS_MENU_SEED", violations);
        assertGuard(backendRoot, "sql/currency_configuration_seed.sql",
                "@confirm_currency_configuration_seed",
                "APPLY_CURRENCY_CONFIGURATION_SEED", violations);
        assertGuard(backendRoot, "sql/upstream_system_management_seed.sql",
                "@confirm_upstream_system_management_seed",
                "APPLY_UPSTREAM_SYSTEM_MANAGEMENT_SEED", violations);
        assertGuard(backendRoot, "sql/warehouse_us_address_seed.sql",
                "@confirm_warehouse_us_address_seed",
                "APPLY_WAREHOUSE_US_ADDRESS_SEED", violations);

        if (!violations.isEmpty())
        {
            fail("high impact SQL scripts must fail closed without explicit confirmation:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void datedHighImpactSqlScriptsMustBeAutoDiscoveredAndGuarded() throws IOException
    {
        Path sqlDir = findWorkspaceRoot().resolve("RuoYi-Vue/sql");
        List<String> violations = new ArrayList<>();

        try (java.util.stream.Stream<Path> sqlFiles = Files.list(sqlDir))
        {
            for (Path sqlFile : sqlFiles
                    .filter(path -> DATED_SQL_FILE.matcher(path.getFileName().toString()).matches())
                    .sorted()
                    .toList())
            {
                String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
                if (containsHighImpactSql(source))
                {
                    assertAutoDiscoveredGuard(sqlFile, source, violations);
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail("dated high impact SQL scripts must be auto-discovered and fail closed:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void allIncrementalHighImpactSqlScriptsMustBeAutoDiscoveredAndGuarded() throws IOException
    {
        Path sqlDir = findWorkspaceRoot().resolve("RuoYi-Vue/sql");
        List<String> violations = new ArrayList<>();

        try (java.util.stream.Stream<Path> sqlFiles = Files.list(sqlDir))
        {
            for (Path sqlFile : sqlFiles
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList())
            {
                String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
                if (source.contains("URILI_BOOTSTRAP_ONLY_SQL"))
                {
                    continue;
                }
                if (containsHighImpactSql(source))
                {
                    assertAutoDiscoveredGuard(sqlFile, source, violations);
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail("incremental high impact SQL scripts must be auto-discovered and fail closed:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void highImpactSqlDiscoveryMustRecognizeStandaloneDropIndex()
    {
        if (!containsHighImpactSql("drop index idx_seller_login_log_account_time on seller_login_log;"))
        {
            fail("standalone drop index must be treated as high impact SQL");
        }
        if (!containsHighImpactSql("set @ddl = concat('drop index ', 'idx_seller_login_log_account_time', ' on seller_login_log')"))
        {
            fail("dynamic drop index must be treated as high impact SQL");
        }
        if (!containsHighImpactSql("set @sql = concat('update ', p_table, ' set status = ''1''')"))
        {
            fail("dynamic update must be treated as high impact SQL");
        }
        if (!containsHighImpactSql("set @dml = concat('delete from ', p_table, ' where id = 1')"))
        {
            fail("dynamic delete must be treated as high impact SQL");
        }
    }

    @Test
    public void autoDiscoveredSqlGuardOrderMustUseConfirmedProcedureCall()
    {
        List<String> violations = new ArrayList<>();

        assertConfirmationCallBeforeDml(Paths.get("bad_order.sql"),
                "delimiter ;\n"
                        + "call assert_table_exists('seller', 'seller table is required');\n"
                        + "insert into seller_menu (perms) values ('seller:bad');\n"
                        + "call assert_bad_order_confirmed();\n",
                violations);

        if (violations.isEmpty())
        {
            fail("auto-discovered SQL guard order must not accept unrelated assert calls before DDL/DML");
        }

        violations.clear();
        assertConfirmationCallBeforeDml(Paths.get("wrong_confirm.sql"),
                "delimiter //\n"
                        + "create procedure assert_expected_confirmed()\n"
                        + "begin\n"
                        + "  signal sqlstate '45000' set message_text = 'expected';\n"
                        + "end//\n"
                        + "delimiter ;\n"
                        + "call assert_other_confirmed();\n"
                        + "insert into seller_menu (perms) values ('seller:wrong');\n",
                violations);

        if (violations.isEmpty())
        {
            fail("auto-discovered SQL guard order must not accept a different confirmed procedure before DDL/DML");
        }

        violations.clear();
        assertConfirmationCallBeforeDml(Paths.get("undeclared_confirm.sql"),
                "delimiter ;\n"
                        + "call assert_missing_confirmed();\n"
                        + "insert into seller_menu (perms) values ('seller:missing');\n",
                violations);

        if (violations.isEmpty())
        {
            fail("auto-discovered SQL guard order must not accept an undeclared confirmed procedure before DDL/DML");
        }

        violations.clear();
        assertConfirmationCallBeforeDml(Paths.get("dynamic_bad_order.sql"),
                "delimiter //\n"
                        + "create procedure assert_dynamic_confirmed()\n"
                        + "begin\n"
                        + "  signal sqlstate '45000' set message_text = 'dynamic';\n"
                        + "end//\n"
                        + "delimiter ;\n"
                        + "set @dml = concat('update seller_account set status = ''1'' where seller_id = 1');\n"
                        + "prepare stmt from @dml;\n"
                        + "execute stmt;\n"
                        + "call assert_dynamic_confirmed();\n",
                violations);

        if (violations.isEmpty())
        {
            fail("auto-discovered SQL guard order must not accept dynamic DML before confirmation");
        }

        violations.clear();
        assertConfirmationCallBeforeDml(Paths.get("good_order.sql"),
                "delimiter //\n"
                        + "create procedure assert_good_order_confirmed()\n"
                        + "begin\n"
                        + "  signal sqlstate '45000' set message_text = 'good';\n"
                        + "end//\n"
                        + "delimiter ;\n"
                        + "call assert_good_order_confirmed();\n"
                        + "call assert_table_exists('seller', 'seller table is required');\n"
                        + "insert into seller_menu (perms) values ('seller:good');\n",
                violations);

        if (!violations.isEmpty())
        {
            fail("auto-discovered SQL guard order must accept confirmed procedure calls before DDL/DML:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void datedSqlScriptsMustNotUseBareCreateIndex() throws IOException
    {
        Path sqlDir = findWorkspaceRoot().resolve("RuoYi-Vue/sql");
        List<String> violations = new ArrayList<>();

        try (java.util.stream.Stream<Path> sqlFiles = Files.list(sqlDir))
        {
            for (Path sqlFile : sqlFiles
                    .filter(path -> DATED_SQL_FILE.matcher(path.getFileName().toString()).matches())
                    .sorted()
                    .toList())
            {
                String source = stripLineComments(Files.readString(sqlFile, StandardCharsets.UTF_8));
                Matcher matcher = BARE_CREATE_INDEX_STATEMENT.matcher(source);
                if (matcher.find())
                {
                    violations.add(sqlFile.getFileName()
                            + " must use an idempotent index helper instead of bare CREATE INDEX");
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail("dated SQL scripts must be safe to replay for index creation:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void destructiveBootstrapSqlMustStayExplicitlyBootstrapOnly() throws IOException
    {
        Path sqlDir = findWorkspaceRoot().resolve("RuoYi-Vue/sql");
        Path ruoyiBaselineSql = sqlDir.resolve("ry_20260417.sql");
        Path quartzBaselineSql = sqlDir.resolve("quartz.sql");
        List<String> violations = new ArrayList<>();

        assertBootstrapOnlySql(ruoyiBaselineSql, violations);
        assertBootstrapOnlySql(quartzBaselineSql, violations);

        try (java.util.stream.Stream<Path> sqlFiles = Files.list(sqlDir))
        {
            for (Path sqlFile : sqlFiles
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .filter(path -> !path.getFileName().toString().equals("ry_20260417.sql"))
                    .filter(path -> !path.getFileName().toString().equals("quartz.sql"))
                    .sorted()
                    .toList())
            {
                String source = stripLineComments(Files.readString(sqlFile, StandardCharsets.UTF_8));
                if (DESTRUCTIVE_DROP_TABLE_STATEMENT.matcher(source).find())
                {
                    violations.add(sqlFile.getFileName()
                            + " must not use destructive DROP TABLE; only bootstrap-only baseline SQL may do this");
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail("destructive bootstrap SQL must stay isolated from incremental replay scripts:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void legacySysUserBackfillHelperMustStayDoubleConfirmedAndFailFast() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path legacySql = backendRoot.resolve("sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql");
        Path currentMigration = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String legacySource = Files.readString(legacySql, StandardCharsets.UTF_8);
        String migrationSource = Files.readString(currentMigration, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "@confirm_legacy_sys_user_backfill_profile");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "LEGACY_SYS_USER_ACCOUNT_MIXED_DB");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "before 20260604_three_terminal_isolation_migration.sql");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "Fresh three-terminal");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "left join sys_user u on u.user_id = a.user_id");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "seller_account legacy backfill has rows missing sys_user");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "buyer_account legacy backfill has rows missing sys_user");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "seller_account legacy backfill would leave blank password rows");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "buyer_account legacy backfill would leave blank password rows");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "call assert_legacy_sys_user_backfill_confirmed();");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "call assert_legacy_user_id_binding_exists();");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "@legacy_seller_account_ids");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "@legacy_seller_expected_count");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "@legacy_seller_expected_signature");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "set @legacy_seller_expected_signature after previewing exact seller_account/sys_user rows");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "seller_account legacy backfill exact target signature mismatch");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "preview-confirmed comma-separated seller_account_id values");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "seller_account legacy backfill account_ids include rows outside legacy user_id scope");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "seller_account legacy backfill expected count does not match target rows");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "find_in_set(cast(a.seller_account_id as char), @legacy_seller_account_ids) > 0");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "call assert_legacy_seller_backfill_targets();");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "@legacy_buyer_account_ids");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "@legacy_buyer_expected_count");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "@legacy_buyer_expected_signature");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "set @legacy_buyer_expected_signature after previewing exact buyer_account/sys_user rows");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "buyer_account legacy backfill exact target signature mismatch");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "preview-confirmed comma-separated buyer_account_id values");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "buyer_account legacy backfill account_ids include rows outside legacy user_id scope");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "buyer_account legacy backfill expected count does not match target rows");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "find_in_set(cast(a.buyer_account_id as char), @legacy_buyer_account_ids) > 0");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "call assert_legacy_buyer_backfill_targets();");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "legacy sys_user backfill requires seller_account.user_id or buyer_account.user_id");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "legacy sys_user backfill requires at least one terminal account row with user_id");
        assertAppearsBefore(violations, legacySql.getFileName().toString(), legacySource,
                "call assert_legacy_user_id_binding_exists();",
                "call assert_legacy_seller_backfill_targets();");
        assertAppearsBefore(violations, legacySql.getFileName().toString(), legacySource,
                "call assert_legacy_seller_backfill_targets();",
                "call migrate_seller_account_from_sys_user();");
        assertAppearsBefore(violations, legacySql.getFileName().toString(), legacySource,
                "call assert_legacy_buyer_backfill_targets();",
                "call migrate_buyer_account_from_sys_user();");

        if (migrationSource.contains("BACKFILL_TERMINAL_ACCOUNTS_FROM_SYS_USER")
                || migrationSource.contains("migrate_seller_account_from_sys_user")
                || migrationSource.contains("migrate_buyer_account_from_sys_user")
                || migrationSource.contains("join sys_user"))
        {
            violations.add(currentMigration.getFileName()
                    + " must not absorb legacy sys_user terminal account backfill");
        }

        if (!violations.isEmpty())
        {
            fail("legacy sys_user account backfill must stay explicit, double-confirmed, and fail-fast:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void legacySysRoleCleanupMustKeepPreviewConfirmedTargets() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path legacyRoleSql = backendRoot.resolve("sql/20260606_legacy_disable_sys_seller_buyer_roles.sql");
        String source = Files.readString(legacyRoleSql, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@confirm_legacy_sys_role_cleanup_profile",
                "@legacy_sys_role_cleanup_role_keys",
                "@legacy_sys_role_cleanup_role_ids",
                "@legacy_sys_role_cleanup_expected_count",
                "@legacy_sys_role_cleanup_expected_signature",
                "LEGACY_SYS_ROLE_SELLER_BUYER_CONFIRMED",
                "preview-confirmed comma-separated role_id values",
                "set @legacy_sys_role_cleanup_expected_signature after previewing exact sys_role candidates",
                "create procedure assert_terminal_owner_roles_ready",
                "seller owner terminal roles are not ready before legacy sys_role cleanup",
                "buyer owner terminal roles are not ready before legacy sys_role cleanup",
                "create procedure assert_legacy_sys_role_cleanup_targets",
                "legacy sys_role cleanup role_ids include non seller/buyer roles",
                "legacy sys_role cleanup role_ids must match seller/buyer roles exactly",
                "legacy sys_role cleanup expected count does not match role_ids",
                "legacy sys_role cleanup exact target signature mismatch",
                "length(trim(both ',' from @legacy_sys_role_cleanup_role_ids))",
                "legacy_sys_role_cleanup_expected_signature",
                "call assert_legacy_sys_role_cleanup_targets();",
                "create procedure assert_legacy_sys_role_cleanup_completed",
                "legacy sys_role cleanup completed count does not match role_ids",
                "legacy sys_role cleanup has remaining active target roles",
                "start transaction;",
                "call assert_legacy_sys_role_cleanup_completed();",
                "commit;",
                "drop procedure if exists assert_legacy_sys_role_cleanup_completed;"
        })
        {
            requireContains(violations, legacyRoleSql.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, legacyRoleSql.getFileName().toString(), source,
                "call assert_terminal_owner_roles_ready();", "call assert_legacy_sys_role_cleanup_targets();");
        assertAppearsBefore(violations, legacyRoleSql.getFileName().toString(), source,
                "call assert_legacy_sys_role_cleanup_targets();", "update sys_role");
        assertAppearsBefore(violations, legacyRoleSql.getFileName().toString(), source,
                "call assert_legacy_sys_role_cleanup_targets();", "start transaction;");
        assertAppearsBefore(violations, legacyRoleSql.getFileName().toString(), source,
                "start transaction;", "update sys_role");
        assertAppearsBefore(violations, legacyRoleSql.getFileName().toString(), source,
                "update sys_role", "call assert_legacy_sys_role_cleanup_completed();");
        assertAppearsBefore(violations, legacyRoleSql.getFileName().toString(), source,
                "call assert_legacy_sys_role_cleanup_completed();", "commit;");
        assertAppearsBefore(violations, legacyRoleSql.getFileName().toString(), source,
                "commit;", "drop procedure if exists assert_legacy_sys_role_cleanup_confirmed;");

        if (!violations.isEmpty())
        {
            fail("legacy sys_role cleanup must stay preview-confirmed and target-bounded:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void adminPartnerButtonCleanupMustKeepPreviewConfirmedTargetSignature() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path cleanupSql = backendRoot.resolve("sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql");
        String source = Files.readString(cleanupSql, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@admin_partner_button_cleanup_expected_delete_count",
                "@admin_partner_button_cleanup_expected_signature",
                "set @admin_partner_button_cleanup_expected_signature after previewing exact sys_role_menu rows",
                "sha2(coalesce(group_concat(",
                "concat_ws(':', child_grant.role_id, child_grant.menu_id, child.perms)",
                "admin partner button cleanup exact target signature mismatch",
                "call assert_admin_partner_button_cleanup_targets();",
                "create procedure assert_admin_partner_button_cleanup_completed",
                "admin partner button cleanup has remaining child grant rows",
                "start transaction;",
                "call assert_admin_partner_button_cleanup_completed();",
                "commit;",
                "drop procedure if exists assert_admin_partner_button_cleanup_completed;"
        })
        {
            requireContains(violations, cleanupSql.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_button_cleanup_targets();", "delete child_grant");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_button_cleanup_targets();", "start transaction;");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "start transaction;", "delete child_grant");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "delete child_grant", "call assert_admin_partner_button_cleanup_completed();");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_button_cleanup_completed();", "commit;");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "commit;", "drop procedure if exists assert_admin_partner_non_admin_button_cleanup_confirmed;");

        if (!violations.isEmpty())
        {
            fail("admin partner button cleanup must keep preview-confirmed exact target signature:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void adminPartnerOwnerResetCleanupMustKeepPreviewConfirmedTargetSignatures() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path cleanupSql = backendRoot.resolve("sql/20260607_admin_partner_owner_reset_permission_cleanup.sql");
        String source = Files.readString(cleanupSql, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@admin_partner_owner_reset_expected_role_menu_count",
                "@admin_partner_owner_reset_expected_menu_count",
                "@admin_partner_owner_reset_expected_role_menu_signature",
                "@admin_partner_owner_reset_expected_menu_signature",
                "set @admin_partner_owner_reset_expected_role_menu_signature after previewing exact sys_role_menu rows",
                "set @admin_partner_owner_reset_expected_menu_signature after previewing exact sys_menu rows",
                "concat_ws(':', rm.role_id, rm.menu_id)",
                "concat_ws(':',",
                "m.menu_id,",
                "admin partner owner reset role-menu exact target signature mismatch",
                "admin partner owner reset menu exact target signature mismatch",
                "call assert_admin_partner_owner_reset_permission_cleanup_targets();",
                "create procedure assert_admin_partner_owner_reset_permission_cleanup_completed",
                "admin partner owner reset role-menu cleanup has remaining rows",
                "admin partner owner reset menu cleanup has remaining rows",
                "start transaction;",
                "call assert_admin_partner_owner_reset_permission_cleanup_completed();",
                "commit;",
                "drop procedure if exists assert_admin_partner_owner_reset_permission_cleanup_completed;"
        })
        {
            requireContains(violations, cleanupSql.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_owner_reset_permission_cleanup_targets();", "delete rm");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_owner_reset_permission_cleanup_targets();", "delete m");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_owner_reset_permission_cleanup_targets();", "start transaction;");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "start transaction;", "delete rm");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "delete rm", "delete m");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "delete m", "call assert_admin_partner_owner_reset_permission_cleanup_completed();");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_owner_reset_permission_cleanup_completed();", "commit;");

        if (!violations.isEmpty())
        {
            fail("admin partner owner reset cleanup must keep preview-confirmed exact target signatures:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void directLoginAuditSqlMustAssertColumnDefinitionsAfterReplaySafeModify() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();

        for (Path sqlFile : new Path[] {
                backendRoot.resolve("sql/20260607_terminal_login_log_direct_login_audit.sql"),
                backendRoot.resolve("sql/20260607_terminal_oper_log_direct_login_audit.sql")
        })
        {
            String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
            String fileName = sqlFile.getFileName().toString();
            String suffix = fileName.contains("_login_log_") ? "login_log" : "oper_log";

            for (String expected : new String[] {
                    "create procedure modify_direct_login_audit_columns_if_needed",
                    "create procedure assert_direct_login_audit_column_contract",
                    "direct-login audit columns must exist before definition modify",
                    "direct-login audit column contract mismatch",
                    "direct_login tinyint(1) not null default 0",
                    "modify direct_login_ticket_id bigint(20) default null",
                    "modify acting_admin_id bigint(20) default null",
                    "modify acting_admin_name varchar(64) default",
                    "modify direct_login_reason varchar(255) default",
                    "coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>')",
                    "call modify_direct_login_audit_columns_if_needed('seller_" + suffix + "');",
                    "call modify_direct_login_audit_columns_if_needed('buyer_" + suffix + "');",
                    "call assert_direct_login_audit_column_contract('seller_" + suffix + "');",
                    "call assert_direct_login_audit_column_contract('buyer_" + suffix + "');",
                    "drop procedure if exists modify_direct_login_audit_columns_if_needed;",
                    "drop procedure if exists assert_direct_login_audit_column_contract;"
            })
            {
                requireContains(violations, fileName, source, expected);
            }

            assertAppearsBefore(violations, fileName, source,
                    "call add_column_if_missing('buyer_" + suffix + "', 'direct_login_reason'",
                    "call modify_direct_login_audit_columns_if_needed('seller_" + suffix + "');");
            assertAppearsBefore(violations, fileName, source,
                    "call modify_direct_login_audit_columns_if_needed('buyer_" + suffix + "');",
                    "call assert_column_exists('seller_" + suffix + "', 'direct_login'");
            assertAppearsBefore(violations, fileName, source,
                    "call assert_column_exists('buyer_" + suffix + "', 'direct_login_reason'",
                    "call assert_direct_login_audit_column_contract('seller_" + suffix + "');");
        }

        if (!violations.isEmpty())
        {
            fail("direct-login audit SQL must replay-safely modify and assert column definitions:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void threeTerminalIsolationMigrationMustFailClosedBeforeUsernameUniqueIndexMigration()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path migrationSql = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String source = Files.readString(migrationSql, StandardCharsets.UTF_8);
        String fileName = migrationSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_no_duplicate_terminal_user_name",
                "seller_account has duplicate user_name values before username unique index migration",
                "buyer_account has duplicate user_name values before username unique index migration",
                "call recreate_index_if_mismatch('seller_account', 'uk_seller_account_username'",
                "call recreate_index_if_mismatch('buyer_account', 'uk_buyer_account_username'",
                "call assert_index_definition('seller_account', 'uk_seller_account_username'",
                "call assert_index_definition('buyer_account', 'uk_buyer_account_username'",
                "drop procedure if exists assert_no_duplicate_terminal_user_name"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "call assert_no_duplicate_terminal_user_name('seller_account'",
                "call drop_index_if_exists('seller_account', 'uk_seller_account_user');");
        assertAppearsBefore(violations, fileName, source,
                "call assert_no_duplicate_terminal_user_name('buyer_account'",
                "call drop_column_if_exists('buyer_account', 'user_id');");
        assertAppearsBefore(violations, fileName, source,
                "call assert_no_duplicate_terminal_user_name('seller_account'",
                "call recreate_index_if_mismatch('seller_account', 'uk_seller_account_username'");
        assertAppearsBefore(violations, fileName, source,
                "call recreate_index_if_mismatch('seller_account', 'uk_seller_account_username'",
                "call assert_index_definition('seller_account', 'uk_seller_account_username'");

        if (!violations.isEmpty())
        {
            fail("three-terminal isolation migration must fail closed before username unique index migration:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void threeTerminalIsolationMigrationMustLockAccountNormalizeAndUserIdDropTargets()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path migrationSql = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String source = Files.readString(migrationSql, StandardCharsets.UTF_8);
        String fileName = migrationSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@three_terminal_seller_account_normalize_expected_count",
                "@three_terminal_seller_account_normalize_expected_signature",
                "@three_terminal_buyer_account_normalize_expected_count",
                "@three_terminal_buyer_account_normalize_expected_signature",
                "@three_terminal_seller_user_id_drop_expected_count",
                "@three_terminal_seller_user_id_drop_expected_signature",
                "@three_terminal_buyer_user_id_drop_expected_count",
                "@three_terminal_buyer_user_id_drop_expected_signature",
                "set @three_terminal_seller_account_normalize_expected_signature after previewing exact seller_account normalize rows",
                "set @three_terminal_buyer_account_normalize_expected_signature after previewing exact buyer_account normalize rows",
                "set @three_terminal_seller_user_id_drop_expected_signature after previewing seller_account user_id column",
                "set @three_terminal_buyer_user_id_drop_expected_signature after previewing buyer_account user_id column",
                "create procedure assert_seller_account_normalize_targets",
                "create procedure assert_buyer_account_normalize_targets",
                "create procedure assert_terminal_account_user_id_drop_target",
                "seller_account normalize exact target count mismatch",
                "seller_account normalize exact target signature mismatch",
                "buyer_account normalize exact target count mismatch",
                "buyer_account normalize exact target signature mismatch",
                "seller_account user_id column drop exact target count mismatch",
                "seller_account user_id column drop exact target signature mismatch",
                "buyer_account user_id column drop exact target count mismatch",
                "buyer_account user_id column drop exact target signature mismatch",
                "call assert_seller_account_normalize_targets();",
                "call assert_buyer_account_normalize_targets();",
                "call assert_terminal_account_user_id_drop_target(",
                "drop procedure if exists assert_seller_account_normalize_targets",
                "drop procedure if exists assert_buyer_account_normalize_targets",
                "drop procedure if exists assert_terminal_account_user_id_drop_target"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "call assert_seller_account_normalize_targets();",
                "update seller_account");
        assertAppearsBefore(violations, fileName, source,
                "call assert_buyer_account_normalize_targets();",
                "update buyer_account");
        assertAccountNormalizeUpdateIsScoped(violations, fileName, source, "seller_account");
        assertAccountNormalizeUpdateIsScoped(violations, fileName, source, "buyer_account");
        assertAppearsBefore(violations, fileName, source,
                "'seller_account user_id column drop exact target signature mismatch'",
                "call drop_column_if_exists('seller_account', 'user_id');");
        assertAppearsBefore(violations, fileName, source,
                "'buyer_account user_id column drop exact target signature mismatch'",
                "call drop_column_if_exists('buyer_account', 'user_id');");

        if (!violations.isEmpty())
        {
            fail("three-terminal isolation migration must lock account normalize and user_id drop targets:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void adminPartnerRoleMenuGrantMustVerifyExactChildMenuSignatureAndExactAdminRoleTargets()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path grantSql = backendRoot.resolve("sql/20260606_admin_partner_role_menu_grant.sql");
        String source = Files.readString(grantSql, StandardCharsets.UTF_8);
        String fileName = grantSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@admin_partner_role_menu_grant_role_ids",
                "@admin_partner_role_menu_grant_expected_role_count",
                "@admin_partner_role_menu_grant_expected_role_signature",
                "@admin_partner_role_menu_grant_expected_grant_count",
                "@admin_partner_role_menu_grant_expected_grant_signature",
                "set @admin_partner_role_menu_grant_role_ids to preview-confirmed comma-separated admin role_id values",
                "set @admin_partner_role_menu_grant_expected_role_count after previewing exact admin sys_role rows",
                "set @admin_partner_role_menu_grant_expected_role_signature after previewing exact admin sys_role rows",
                "set @admin_partner_role_menu_grant_expected_grant_count after previewing exact admin sys_role_menu grant rows",
                "set @admin_partner_role_menu_grant_expected_grant_signature after previewing exact admin sys_role_menu grant rows",
                "create procedure assert_admin_partner_child_menu_signature",
                "create procedure assert_admin_partner_role_menu_grant_targets",
                "create procedure assert_admin_partner_role_menu_grant_completed",
                "tmp_admin_partner_child_menu_signature",
                "partner child menu signature seed count must be 64",
                "partner sys_menu child button signature does not match expected seller/buyer admin buttons",
                "admin partner role-menu grant role_ids include non-admin, inactive, or deleted roles",
                "admin partner role-menu grant role_ids must match active admin roles exactly",
                "admin partner role-menu grant expected role count does not match role_ids",
                "admin partner role-menu grant exact role signature mismatch",
                "admin partner role-menu grant exact target count mismatch",
                "admin partner role-menu grant exact target signature mismatch",
                "admin partner role-menu grant completed missing root/page menu grants",
                "admin partner role-menu grant completed missing child button grants",
                "grant_target.role_id, grant_target.menu_id",
                "join sys_menu page_menu on page_menu.menu_id in (2011, 2012)",
                "find_in_set(cast(r.role_id as char), @admin_partner_role_menu_grant_role_ids) > 0",
                "length(trim(both ',' from @admin_partner_role_menu_grant_role_ids))",
                "and m.parent_id = seed.parent_id",
                "and m.perms = seed.perms",
                "(2322, 2011, 'seller:admin:account:lock')",
                "(2323, 2012, 'buyer:admin:account:lock')",
                "call assert_admin_partner_child_menu_signature();",
                "call assert_admin_partner_role_menu_grant_targets();",
                "call assert_admin_partner_role_menu_grant_completed();",
                "start transaction;",
                "commit;",
                "drop procedure if exists assert_admin_partner_role_menu_grant_completed",
                "drop procedure if exists assert_admin_partner_child_menu_signature"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "call assert_admin_partner_child_menu_signature();",
                "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_admin_partner_role_menu_grant_targets();",
                "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;",
                "insert into sys_role_menu");
        assertAppearsBefore(violations, fileName, source,
                "insert into sys_role_menu (role_id, menu_id)\nselect distinct r.role_id, child.menu_id",
                "commit;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_admin_partner_role_menu_grant_completed();",
                "commit;");
        requireNotContains(violations, fileName, source, "join sys_role_menu page_grant");
        requireExactOccurrenceCount(violations, fileName, source,
                "join sys_menu page_menu on page_menu.menu_id in (2011, 2012)", 2);
        requireExactOccurrenceCount(violations, fileName, source,
                "join sys_menu child on child.parent_id = page_menu.menu_id", 2);
        requireExactOccurrenceCount(violations, fileName, source,
                "where r.role_key = 'admin'\n        and r.status = '0'\n        and r.del_flag = '0'", 2);
        requireExactOccurrenceCount(violations, fileName, source,
                "where r.role_key = 'admin'\n  and r.status = '0'\n  and r.del_flag = '0'", 2);
        requireExactOccurrenceCount(violations, fileName, source,
                "drop temporary table if exists tmp_admin_partner_child_menu_signature;", 2);
        int completedCallIndex = source.indexOf("call assert_admin_partner_role_menu_grant_completed();");
        int childSignatureCleanupIndex = completedCallIndex < 0 ? -1
                : source.indexOf("drop temporary table if exists tmp_admin_partner_child_menu_signature;",
                        completedCallIndex);
        if (completedCallIndex < 0 || childSignatureCleanupIndex < 0)
        {
            violations.add(fileName
                    + " must keep tmp_admin_partner_child_menu_signature until after completed assertion");
        }
        int commitAfterCompletedIndex = completedCallIndex < 0 ? -1 : source.indexOf("commit;", completedCallIndex);
        if (commitAfterCompletedIndex < 0 || childSignatureCleanupIndex < commitAfterCompletedIndex)
        {
            violations.add(fileName
                    + " must cleanup tmp_admin_partner_child_menu_signature only after commit");
        }
        int procedureCleanupIndex = childSignatureCleanupIndex < 0 ? -1
                : source.indexOf("drop procedure if exists assert_admin_partner_role_menu_grant_confirmed;",
                        childSignatureCleanupIndex);
        if (procedureCleanupIndex < 0)
        {
            violations.add(fileName
                    + " must cleanup tmp_admin_partner_child_menu_signature before dropping procedures");
        }
        assertAppearsBefore(violations, fileName, source,
                "commit;",
                "drop procedure if exists assert_admin_partner_role_menu_grant_confirmed;");

        if (!violations.isEmpty())
        {
            fail("admin partner role-menu grant must verify exact child menu signatures before granting:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalMenuSeedsAndMigrationMustEnforceUniqueTerminalPermsBeforeRoleBinding()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        Path migrationSql = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String seedSource = Files.readString(seedSql, StandardCharsets.UTF_8);
        String migrationSource = Files.readString(migrationSql, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "perms_unique_key",
                "unique key uk_seller_menu_perms (perms_unique_key)",
                "unique key uk_buyer_menu_perms (perms_unique_key)",
                "seller_menu contains invalid terminal perms",
                "buyer_menu contains invalid terminal perms",
                "seller_menu page menus require component under Seller/",
                "buyer_menu page menus require component under Buyer/",
                "coalesce(trim(component), '') not like 'Seller/%'",
                "coalesce(trim(component), '') not like 'Buyer/%'",
                "seller_menu perms must be unique before terminal role grants",
                "buyer_menu perms must be unique before terminal role grants",
                "menu_type in ('C', 'F')",
                "where coalesce(trim(perms), '') <> ''",
                "call add_column_if_missing('seller_menu', 'perms_unique_key'",
                "call add_column_if_missing('buyer_menu', 'perms_unique_key'",
                "call recreate_index_if_mismatch('seller_menu', 'uk_seller_menu_perms'",
                "call recreate_index_if_mismatch('buyer_menu', 'uk_buyer_menu_perms'",
                "call assert_terminal_menu_perms_unique_index('seller_menu', 'uk_seller_menu_perms'",
                "call assert_terminal_menu_perms_unique_index('buyer_menu', 'uk_buyer_menu_perms'"
        })
        {
            requireContains(violations, seedSql.getFileName().toString(), seedSource, expected);
        }
        assertAppearsBefore(violations, seedSql.getFileName().toString(), seedSource,
                "call assert_terminal_menu_range_ready();",
                "call add_column_if_missing('seller_menu', 'perms_unique_key'");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), seedSource,
                "call add_column_if_missing('seller_menu', 'perms_unique_key'",
                "call recreate_index_if_mismatch('seller_menu', 'uk_seller_menu_perms'");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), seedSource,
                "call assert_terminal_menu_perms_unique_index('seller_menu', 'uk_seller_menu_perms'",
                "insert into seller_role_menu");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), seedSource,
                "call assert_terminal_menu_perms_unique_index('buyer_menu', 'uk_buyer_menu_perms'",
                "insert into buyer_role_menu");

        for (String expected : new String[] {
                "create procedure assert_terminal_menu_integrity_ready",
                "perms_unique_key",
                "unique key uk_seller_menu_perms (perms_unique_key)",
                "unique key uk_buyer_menu_perms (perms_unique_key)",
                "seller_menu contains invalid terminal perms",
                "buyer_menu contains invalid terminal perms",
                "seller_menu page menus require component under Seller/",
                "buyer_menu page menus require component under Buyer/",
                "coalesce(trim(component), '') not like 'Seller/%'",
                "coalesce(trim(component), '') not like 'Buyer/%'",
                "seller_menu perms must be unique before terminal role grants",
                "buyer_menu perms must be unique before terminal role grants",
                "menu_type in ('C', 'F')",
                "where coalesce(trim(perms), '') <> ''",
                "call add_column_if_missing('seller_menu', 'perms_unique_key'",
                "call add_column_if_missing('buyer_menu', 'perms_unique_key'",
                "call assert_terminal_menu_integrity_ready();",
                "call recreate_index_if_mismatch('seller_menu', 'uk_seller_menu_perms'",
                "call recreate_index_if_mismatch('buyer_menu', 'uk_buyer_menu_perms'",
                "call assert_index_definition('seller_menu', 'uk_seller_menu_perms'",
                "call assert_index_definition('buyer_menu', 'uk_buyer_menu_perms'",
                "drop procedure if exists assert_terminal_menu_integrity_ready"
        })
        {
            requireContains(violations, migrationSql.getFileName().toString(), migrationSource, expected);
        }
        assertAppearsBefore(violations, migrationSql.getFileName().toString(), migrationSource,
                "call assert_terminal_menu_integrity_ready();",
                "call add_column_if_missing('seller_menu', 'perms_unique_key'");
        assertAppearsBefore(violations, migrationSql.getFileName().toString(), migrationSource,
                "call add_column_if_missing('seller_menu', 'perms_unique_key'",
                "call recreate_index_if_mismatch('seller_menu', 'uk_seller_menu_perms'");
        assertAppearsBefore(violations, migrationSql.getFileName().toString(), migrationSource,
                "call assert_index_definition('seller_menu', 'uk_seller_menu_perms'",
                "create table if not exists seller_login_log");

        if (!violations.isEmpty())
        {
            fail("terminal menu seeds and migration must fail closed before perms-based role binding:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamSystemCodeCorrectionMustLockExactTargets() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260604_upstream_system_code_correction.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@upstream_system_code_correction_expected_count",
                "@upstream_system_code_correction_expected_signature",
                "set @upstream_system_code_correction_expected_count after previewing exact upstream code correction rows",
                "set @upstream_system_code_correction_expected_signature after previewing exact upstream code correction rows",
                "create procedure assert_upstream_system_code_correction_targets",
                "connection_code",
                "dict_code",
                "sha2(coalesce(group_concat(target order by target separator '\\n'), ''), 256)",
                "upstream system code correction exact target count mismatch",
                "upstream system code correction exact target signature mismatch",
                "call assert_upstream_system_code_correction_targets();",
                "start transaction;",
                "commit;",
                "drop procedure if exists assert_upstream_system_code_correction_targets"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call assert_upstream_system_code_correction_targets();",
                "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;",
                "update upstream_system_connection");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;",
                "delete from sys_dict_data");
        assertAppearsBefore(violations, fileName, source,
                "delete from sys_dict_data\nwhere dict_type = 'upstream_settlement_type'",
                "commit;");

        if (!violations.isEmpty())
        {
            fail("upstream system code correction must lock exact update/delete targets:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamTaskComponentSplitMustLockExactSysJobTargets() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260607_upstream_task_component_split.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@upstream_task_component_split_expected_count",
                "@upstream_task_component_split_expected_signature",
                "create procedure assert_upstream_task_component_split_targets",
                "sha2(coalesce(group_concat(",
                "sys_job exact target count mismatch before upstream task component split",
                "sys_job exact target signature mismatch before upstream task component split",
                "upstream task component split SKU info sys_job target must be unique before update",
                "upstream task component split inventory sys_job target must be unique before update",
                "upstream task component split warehouse sys_job target must be unique before update",
                "upstream task component split logistics channel sys_job target must be unique before update",
                "upstream task component split SKU dimension sys_job target must be unique before update",
                "call assert_upstream_task_component_split_targets();",
                "start transaction;",
                "commit;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call assert_upstream_task_component_split_targets();",
                "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;",
                "update sys_job");
        assertAppearsBefore(violations, fileName, source,
                "update sys_job",
                "commit;");

        if (!violations.isEmpty())
        {
            fail("upstream task component split must lock exact sys_job targets before DML:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void productConfigChangeLogMigrationMustAssertSchemaAndLockBackfillTargets() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260605_product_config_change_log.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);",
                "@product_config_change_log_backfill_expected_count",
                "@product_config_change_log_backfill_expected_signature",
                "set @product_config_change_log_backfill_expected_count after previewing exact product config update_time backfill rows",
                "set @product_config_change_log_backfill_expected_signature after previewing exact product config update_time backfill rows",
                "create procedure assert_product_config_change_log_column_contract",
                "create procedure assert_product_config_change_log_index_definition",
                "create procedure assert_product_config_change_log_index_contract",
                "create procedure assert_product_config_change_log_backfill_targets",
                "product_config_change_log column contract mismatch",
                "product_config_change_log index contract mismatch",
                "call assert_product_config_change_log_index_definition('PRIMARY', 'log_id', 0);",
                "call assert_product_config_change_log_index_definition('idx_product_config_change_log_biz'",
                "sha2(coalesce(group_concat(target order by target separator '|'), ''), 256)",
                "'product_category'",
                "'product_attribute'",
                "'product_attribute_option'",
                "'product_category_attribute'",
                "product config update_time backfill exact target count mismatch",
                "product config update_time backfill exact target signature mismatch",
                "call assert_product_config_change_log_column_contract();",
                "call assert_product_config_change_log_index_contract();",
                "call assert_product_config_change_log_backfill_targets();",
                "start transaction;",
                "commit;",
                "drop procedure if exists assert_product_config_change_log_backfill_targets;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "create table if not exists product_config_change_log",
                "call assert_product_config_change_log_column_contract();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_product_config_change_log_index_contract();",
                "call assert_product_config_change_log_backfill_targets();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_product_config_change_log_backfill_targets();",
                "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "update product_category");
        assertAppearsBefore(violations, fileName, source,
                "update product_category_attribute", "commit;");

        if (!violations.isEmpty())
        {
            fail("product config change log migration must assert schema and lock historical backfill targets:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void currencyRateSyncJobMigrationMustLockExactTargetsBeforeUpsert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260604_currency_rate_sync_job.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@currency_rate_sync_job_expected_count",
                "@currency_rate_sync_job_expected_signature",
                "@currency_rate_sync_config_expected_count",
                "@currency_rate_sync_config_expected_signature",
                "create procedure assert_currency_rate_sync_job_targets",
                "create procedure assert_currency_rate_sync_config_targets",
                "currency rate sync sys_job invoke_target must be unique before upsert",
                "currency rate sync sys_job exact target count mismatch",
                "currency rate sync sys_job exact target signature mismatch",
                "SHOWAPI_BANK_RATE sync config provider_code must be unique before update",
                "SHOWAPI_BANK_RATE sync config exact target count mismatch",
                "SHOWAPI_BANK_RATE sync config exact target signature mismatch",
                "sha2(coalesce(group_concat(",
                "where invoke_target = 'currencyRateSyncTask.syncDailyRates'",
                "where provider_code = 'SHOWAPI_BANK_RATE'",
                "call assert_currency_rate_sync_job_targets();",
                "call assert_currency_rate_sync_config_targets();",
                "start transaction;",
                "WHERE NOT EXISTS (",
                "commit;",
                "drop procedure if exists assert_currency_rate_sync_job_targets;",
                "drop procedure if exists assert_currency_rate_sync_config_targets;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        requireNotContains(violations, fileName, source.toLowerCase(), "limit 1");
        assertAppearsBefore(violations, fileName, source,
                "call assert_currency_rate_sync_job_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_currency_rate_sync_config_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "UPDATE sys_job");
        assertAppearsBefore(violations, fileName, source,
                "UPDATE finance_currency_sync_config", "commit;");

        if (!violations.isEmpty())
        {
            fail("currency rate sync job migration must lock exact sys_job and sync config targets:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamSkuSyncJobMigrationMustLockExactSysJobTargetBeforeUpsert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260605_upstream_sku_sync_job.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@upstream_sku_sync_job_expected_count",
                "@upstream_sku_sync_job_expected_signature",
                "set @upstream_sku_sync_job_expected_count after previewing exact upstream SKU sys_job row",
                "set @upstream_sku_sync_job_expected_signature after previewing exact upstream SKU sys_job row",
                "create procedure assert_upstream_sku_sync_job_targets",
                "upstream SKU sync sys_job invoke_target must be unique before upsert",
                "upstream SKU sync sys_job exact target count mismatch",
                "upstream SKU sync sys_job exact target signature mismatch",
                "sha2(coalesce(group_concat(",
                "where invoke_target = 'upstreamSystemTask.syncSkus'",
                "call assert_upstream_sku_sync_job_targets();",
                "start transaction;",
                "WHERE NOT EXISTS (",
                "commit;",
                "drop procedure if exists assert_upstream_sku_sync_job_targets;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        requireNotContains(violations, fileName, source.toLowerCase(), "limit 1");
        assertAppearsBefore(violations, fileName, source,
                "call assert_upstream_sku_sync_job_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "UPDATE sys_job");
        assertAppearsBefore(violations, fileName, source,
                "INSERT INTO sys_job", "commit;");

        if (!violations.isEmpty())
        {
            fail("upstream SKU sync job migration must lock exact sys_job target before upsert:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamInventoryDimensionSyncMustLockRoleMenuAndSysJobTargetsBeforeDml() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260606_upstream_inventory_dimension_sync.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@upstream_inventory_role_menu_expected_count",
                "@upstream_inventory_role_menu_expected_signature",
                "@upstream_inventory_sync_job_expected_count",
                "@upstream_inventory_sync_job_expected_signature",
                "set @upstream_inventory_role_menu_expected_count after previewing exact upstream inventory sys_role_menu grant rows",
                "set @upstream_inventory_role_menu_expected_signature after previewing exact upstream inventory sys_role_menu grant rows",
                "set @upstream_inventory_sync_job_expected_count after previewing exact upstream inventory sys_job row",
                "set @upstream_inventory_sync_job_expected_signature after previewing exact upstream inventory sys_job row",
                "create procedure assert_upstream_inventory_role_menu_targets",
                "create procedure assert_upstream_inventory_sync_job_targets",
                "upstream inventory role-menu exact target count mismatch",
                "upstream inventory role-menu exact target signature mismatch",
                "upstream inventory sys_job invoke_target must be unique before upsert",
                "upstream inventory sys_job exact target count mismatch",
                "upstream inventory sys_job exact target signature mismatch",
                "sha2(coalesce(group_concat(",
                "call assert_upstream_inventory_role_menu_targets();",
                "call assert_upstream_inventory_sync_job_targets();",
                "start transaction;",
                "where invoke_target = 'upstreamSystemTask.syncInventory'",
                "where not exists (",
                "commit;",
                "drop procedure if exists assert_upstream_inventory_role_menu_targets;",
                "drop procedure if exists assert_upstream_inventory_sync_job_targets;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        requireNotContains(violations, fileName, source.toLowerCase(), "limit 1");
        requireNotContains(violations, fileName, source.toLowerCase(), "@job_id");
        assertAppearsBefore(violations, fileName, source,
                "call assert_upstream_inventory_role_menu_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_upstream_inventory_sync_job_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into sys_role_menu");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "update sys_job");
        assertAppearsBefore(violations, fileName, source,
                "insert into sys_job", "commit;");

        if (!violations.isEmpty())
        {
            fail("upstream inventory dimension sync must lock exact sys_role_menu/sys_job targets before DML:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamSyncStagingDiffMustLockSysJobTargetsBeforeUpsert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260606_upstream_sync_staging_diff.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@upstream_sync_staging_diff_job_expected_count",
                "@upstream_sync_staging_diff_job_expected_signature",
                "set @upstream_sync_staging_diff_job_expected_count after previewing exact upstream sync staging diff sys_job rows",
                "set @upstream_sync_staging_diff_job_expected_signature after previewing exact upstream sync staging diff sys_job rows",
                "create procedure assert_upstream_sync_staging_diff_job_targets",
                "upstream sync staging diff SKU info sys_job target must be unique before upsert",
                "upstream sync staging diff warehouse sys_job target must be unique before upsert",
                "upstream sync staging diff logistics channel sys_job target must be unique before upsert",
                "upstream sync staging diff SKU dimension sys_job target must be unique before upsert",
                "upstream sync staging diff inventory sys_job target must be unique before upsert",
                "upstream sync staging diff must run before upstream task component split sys_job targets exist",
                "upstream sync staging diff sys_job exact target count mismatch",
                "upstream sync staging diff sys_job exact target signature mismatch",
                "sha2(coalesce(group_concat(",
                "call assert_upstream_sync_staging_diff_job_targets();",
                "start transaction;",
                "where invoke_target in ('upstreamSystemTask.syncSkus', 'upstreamSystemTask.syncSkuInfo')",
                "where not exists (",
                "commit;",
                "drop procedure if exists assert_upstream_sync_staging_diff_job_targets;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        requireNotContains(violations, fileName, source.toLowerCase(), "limit 1");
        requireNotContains(violations, fileName, source.toLowerCase(), "@job_id");
        assertAppearsBefore(violations, fileName, source,
                "call assert_upstream_sync_staging_diff_job_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "update sys_job");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into sys_job");
        assertAppearsBefore(violations, fileName, source,
                "insert into sys_job", "commit;");

        if (!violations.isEmpty())
        {
            fail("upstream sync staging diff must lock exact sys_job targets before upsert:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamPairingRoleBindingMustLockExactBackfillTargets() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260607_upstream_pairing_role_binding.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@upstream_pairing_warehouse_role_expected_count",
                "@upstream_pairing_warehouse_role_expected_signature",
                "@upstream_pairing_logistics_role_expected_count",
                "@upstream_pairing_logistics_role_expected_signature",
                "@upstream_pairing_logistics_warehouse_expected_count",
                "@upstream_pairing_logistics_warehouse_expected_signature",
                "create procedure assert_upstream_pairing_role_backfill_targets",
                "create procedure assert_upstream_pairing_warehouse_code_backfill_targets",
                "warehouse pairing role exact target count mismatch",
                "logistics pairing role exact target signature mismatch",
                "logistics warehouse-code backfill exact target count mismatch",
                "logistics warehouse-code backfill exact target signature mismatch",
                "call assert_upstream_pairing_role_backfill_targets();",
                "call assert_upstream_pairing_warehouse_code_backfill_targets();"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call assert_upstream_pairing_role_backfill_targets();",
                "update upstream_system_warehouse_pairing");
        assertAppearsBefore(violations, fileName, source,
                "call assert_upstream_pairing_warehouse_code_backfill_targets();",
                "update upstream_system_logistics_channel_pairing l");

        if (!violations.isEmpty())
        {
            fail("upstream pairing role binding must lock exact backfill targets before DML:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryOverviewSkuBaselineRefreshMustLockNoSourceDictTarget() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260608_inventory_overview_sku_baseline_refresh.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@inventory_overview_no_source_dict_expected_count",
                "@inventory_overview_no_source_dict_expected_signature",
                "create procedure assert_inventory_overview_no_source_dict_target",
                "inventory_status NO_SOURCE dict row must exist exactly once before sku baseline refresh",
                "inventory_status NO_SOURCE dict exact target count mismatch",
                "inventory_status NO_SOURCE dict exact target signature mismatch",
                "call assert_inventory_overview_no_source_dict_target();"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call assert_inventory_overview_no_source_dict_target();",
                "update sys_dict_data");

        if (!violations.isEmpty())
        {
            fail("inventory overview sku baseline refresh must lock exact NO_SOURCE dict target before DML:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryOverviewPlatformStockInitialSeedMustLockWriteTargetsAndUseTransaction() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260607_inventory_overview_platform_stock.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@inventory_overview_platform_stock_dict_seed_expected_count",
                "@inventory_overview_platform_stock_dict_seed_expected_signature",
                "@inventory_overview_platform_stock_menu_seed_expected_count",
                "@inventory_overview_platform_stock_menu_seed_expected_signature",
                "@inventory_overview_platform_stock_row_expected_count",
                "@inventory_overview_platform_stock_row_expected_signature",
                "set @inventory_overview_platform_stock_dict_seed_expected_count after previewing inventory overview dict seed rows",
                "set @inventory_overview_platform_stock_menu_seed_expected_count after previewing inventory overview menu seed rows",
                "set @inventory_overview_platform_stock_row_expected_count after previewing exact inventory overview platform stock rows",
                "create procedure inventory_overview_platform_stock_assert_count_signature",
                "create procedure assert_inventory_overview_platform_stock_seed_completed",
                "tmp_inventory_overview_platform_stock_write_targets",
                "tmp_inventory_overview_platform_stock_menu_expected",
                "inventory overview platform stock dict seed exact target count mismatch",
                "inventory overview platform stock menu seed exact target count mismatch",
                "inventory overview platform stock rows exact target count mismatch",
                "inventory overview platform stock rows exact target signature mismatch",
                "inventory overview platform stock sys_menu final signature mismatch",
                "inventory_sku_warehouse_stock final state mismatch",
                "inventory_overview_sku_read_model final state mismatch",
                "inventory_overview_spu_read_model final state mismatch",
                "call inventory_overview_platform_stock_assert_count_signature(",
                "call assert_inventory_overview_platform_stock_seed_completed();",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_inventory_overview_platform_stock_write_targets",
                "drop temporary table if exists tmp_inventory_overview_platform_stock_menu_expected",
                "drop procedure if exists assert_inventory_overview_platform_stock_seed_completed;",
                "drop procedure if exists inventory_overview_platform_stock_assert_count_signature",
                "on duplicate key update"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call inventory_overview_platform_stock_assert_count_signature(",
                "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into sys_dict_type");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into inventory_sku_warehouse_stock(");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into inventory_overview_sku_read_model(");
        assertAppearsBefore(violations, fileName, source,
                "insert into inventory_overview_spu_read_model(", "commit;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_inventory_overview_platform_stock_seed_completed();", "commit;");
        assertAppearsBefore(violations, fileName, source,
                "commit;", "drop procedure if exists assert_inventory_overview_platform_stock_seed_completed;");

        if (!violations.isEmpty())
        {
            fail("inventory overview platform stock initial seed must lock exact write targets before DML:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryOverviewSkuBaselineRefreshMustLockWarehouseStockTargets() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260608_inventory_overview_sku_baseline_refresh.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@inventory_overview_official_master_expected_count",
                "@inventory_overview_official_master_expected_signature",
                "@inventory_overview_source_unbound_expected_count",
                "@inventory_overview_source_unbound_expected_signature",
                "@inventory_overview_unmatched_official_expected_count",
                "@inventory_overview_unmatched_official_expected_signature",
                "@inventory_overview_third_party_expected_count",
                "@inventory_overview_third_party_expected_signature",
                "@inventory_overview_no_warehouse_expected_count",
                "@inventory_overview_no_warehouse_expected_signature",
                "@inventory_overview_obsolete_stock_expected_count",
                "@inventory_overview_obsolete_stock_expected_signature",
                "create procedure inventory_overview_assert_count_signature",
                "create procedure assert_inventory_overview_official_master_targets",
                "create procedure assert_inventory_overview_source_unbound_targets",
                "create procedure assert_inventory_overview_unmatched_official_targets",
                "create procedure assert_inventory_overview_third_party_targets",
                "create procedure assert_inventory_overview_no_warehouse_targets",
                "create procedure assert_inventory_overview_obsolete_stock_targets",
                "inventory overview OFFICIAL_MASTER exact target count mismatch",
                "inventory overview SOURCE_UNBOUND exact target count mismatch",
                "inventory overview UNMATCHED_OFFICIAL exact target count mismatch",
                "inventory overview THIRD_PARTY exact target count mismatch",
                "inventory overview NO_WAREHOUSE exact target count mismatch",
                "inventory overview obsolete stock exact target count mismatch",
                "inventory overview OFFICIAL_MASTER exact target signature mismatch",
                "inventory overview SOURCE_UNBOUND exact target signature mismatch",
                "inventory overview UNMATCHED_OFFICIAL exact target signature mismatch",
                "inventory overview THIRD_PARTY exact target signature mismatch",
                "inventory overview NO_WAREHOUSE exact target signature mismatch",
                "inventory overview obsolete stock exact target signature mismatch",
                "call assert_inventory_overview_official_master_targets();",
                "call assert_inventory_overview_source_unbound_targets();",
                "call assert_inventory_overview_unmatched_official_targets();",
                "call assert_inventory_overview_third_party_targets();",
                "call assert_inventory_overview_no_warehouse_targets();",
                "call assert_inventory_overview_obsolete_stock_targets();",
                "drop procedure if exists assert_inventory_overview_official_master_targets;",
                "drop procedure if exists assert_inventory_overview_source_unbound_targets;",
                "drop procedure if exists assert_inventory_overview_unmatched_official_targets;",
                "drop procedure if exists assert_inventory_overview_third_party_targets;",
                "drop procedure if exists assert_inventory_overview_no_warehouse_targets;",
                "drop procedure if exists assert_inventory_overview_obsolete_stock_targets;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        for (String targetCall : new String[] {
                "call assert_inventory_overview_official_master_targets();",
                "call assert_inventory_overview_source_unbound_targets();",
                "call assert_inventory_overview_unmatched_official_targets();",
                "call assert_inventory_overview_third_party_targets();",
                "call assert_inventory_overview_no_warehouse_targets();",
                "call assert_inventory_overview_obsolete_stock_targets();"
        })
        {
            assertAppearsBefore(violations, fileName, source, targetCall, "start transaction;");
            assertAppearsBefore(violations, fileName, source, targetCall, "insert into inventory_sku_warehouse_stock(");
        }

        if (!violations.isEmpty())
        {
            fail("inventory overview sku baseline refresh must lock exact warehouse stock targets before DML:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryOverviewSkuBaselineRefreshMustRebuildReadModelsInTransaction() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260608_inventory_overview_sku_baseline_refresh.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "start transaction;",
                "delete st\nfrom inventory_sku_warehouse_stock st",
                "current_stock_keys",
                "delete from inventory_overview_sku_read_model;",
                "delete from inventory_overview_spu_read_model;",
                "insert into inventory_overview_sku_read_model(",
                "insert into inventory_overview_spu_read_model(",
                "commit;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "update sys_dict_data");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "delete st\nfrom inventory_sku_warehouse_stock st");
        assertAppearsBefore(violations, fileName, source,
                "delete st\nfrom inventory_sku_warehouse_stock st", "insert into inventory_sku_warehouse_stock(");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into inventory_sku_warehouse_stock(");
        assertAppearsBefore(violations, fileName, source,
                "insert into inventory_sku_warehouse_stock(", "delete from inventory_overview_sku_read_model;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "delete from inventory_overview_sku_read_model;");
        assertAppearsBefore(violations, fileName, source,
                "delete from inventory_overview_spu_read_model;", "insert into inventory_overview_sku_read_model(");
        assertAppearsBefore(violations, fileName, source,
                "insert into inventory_overview_sku_read_model(", "insert into inventory_overview_spu_read_model(");
        assertAppearsBefore(violations, fileName, source,
                "insert into inventory_overview_spu_read_model(", "commit;");

        if (!violations.isEmpty())
        {
            fail("inventory overview sku baseline refresh must atomically rebuild read models:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryPermissionMustNotGrantIntegrationPermissionFromInventoryMenu() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path inventorySql = backendRoot.resolve("sql/20260606_upstream_inventory_dimension_sync.sql");
        Path businessMenuSeed = backendRoot.resolve("sql/business_menu_seed.sql");
        Path sourceWarehouseStockMenuSeed = backendRoot.resolve("sql/20260606_source_warehouse_stock_menu_rename.sql");
        String inventorySource = Files.readString(inventorySql, StandardCharsets.UTF_8);
        String businessMenuSource = Files.readString(businessMenuSeed, StandardCharsets.UTF_8);
        String sourceWarehouseStockMenuSource = Files.readString(sourceWarehouseStockMenuSeed, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        if (inventorySource.contains("'inventory:sourceWarehouse:list'"))
        {
            violations.add(inventorySql.getFileName()
                    + " must not use inventory:sourceWarehouse:list to grant integration inventory permissions");
        }
        if (!inventorySource.contains("where source_menu.perms = 'integration:upstream:query'"))
        {
            violations.add(inventorySql.getFileName()
                    + " must grant integration inventory query only from integration:upstream:query");
        }
        if (businessMenuSource.contains("inventory:sourceWarehouse:list")
                || businessMenuSource.contains("(2421, '来源仓库库存'")
                || businessMenuSource.contains("where menu_id = 2421"))
        {
            violations.add(businessMenuSeed.getFileName()
                    + " must not own source warehouse stock menu 2421");
        }
        for (String expected : new String[] {
                "call assert_source_warehouse_stock_sys_menu_guard();",
                "Inventory/SourceWarehouseStock/index",
                "SourceWarehouseStock",
                "inventory:sourceWarehouse:list",
                "where not exists (select 1 from sys_menu where menu_id = 2421)"
        })
        {
            requireContains(violations, sourceWarehouseStockMenuSeed.getFileName().toString(),
                    sourceWarehouseStockMenuSource, expected);
        }

        if (!violations.isEmpty())
        {
            fail("inventory menu and integration permissions must stay separated:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamInventoryDimensionSyncMustNotOwnUpstreamManagementMenuButtons() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path inventorySql = backendRoot.resolve("sql/20260606_upstream_inventory_dimension_sync.sql");
        String source = Files.readString(inventorySql, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n").toLowerCase();
        List<String> violations = new ArrayList<>();

        for (String forbidden : new String[] {
                "insert into sys_menu",
                "(2307, '仓库尺寸重量同步'",
                "(2308, 'SKU库存查看'",
                "(2309, 'SKU库存同步'",
                "on duplicate key update\n    menu_name = values(menu_name)"
        })
        {
            requireNotContains(violations, inventorySql.getFileName().toString(), normalizedSource,
                    forbidden.toLowerCase());
        }

        for (String expected : new String[] {
                "create procedure assert_upstream_inventory_menu_owner_ready",
                "where menu_id = 2307",
                "and perms = 'integration:upstream:dimensionSync'",
                "where menu_id = 2308",
                "and perms = 'integration:upstream:inventoryQuery'",
                "where menu_id = 2309",
                "and perms = 'integration:upstream:inventorySync'",
                "upstream_system_management_seed.sql to own menu 2307",
                "upstream_system_management_seed.sql to own menu 2308",
                "upstream_system_management_seed.sql to own menu 2309",
                "call assert_upstream_inventory_menu_owner_ready();",
                "drop procedure if exists assert_upstream_inventory_menu_owner_ready"
        })
        {
            requireContains(violations, inventorySql.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, inventorySql.getFileName().toString(), source,
                "call assert_upstream_inventory_dimension_sync_confirmed();",
                "call assert_upstream_inventory_menu_owner_ready();");
        assertAppearsBefore(violations, inventorySql.getFileName().toString(), source,
                "call assert_upstream_inventory_menu_owner_ready();", "insert into sys_role_menu");

        if (!violations.isEmpty())
        {
            fail("upstream inventory dimension sync must depend on the upstream-system menu owner, not own 2307-2309:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerBuyerManagementSeedMustNotOverwritePortalWebUrlConfig() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String source = Files.readString(seedSql, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "select '卖家端前端地址', 'portal.seller.web.url', 'http://127.0.0.1:8001/seller/direct-login'",
                "where not exists (select 1 from sys_config where config_key = 'portal.seller.web.url')",
                "select '买家端前端地址', 'portal.buyer.web.url', 'http://127.0.0.1:8001/buyer/direct-login'",
                "where not exists (select 1 from sys_config where config_key = 'portal.buyer.web.url')"
        })
        {
            requireContains(violations, seedSql.getFileName().toString(), source, expected);
        }
        requireNotContains(violations, seedSql.getFileName().toString(), source,
                "update sys_config\nset config_name = '卖家端前端地址'");
        requireNotContains(violations, seedSql.getFileName().toString(), source,
                "update sys_config\nset config_name = '买家端前端地址'");

        if (!violations.isEmpty())
        {
            fail("seller/buyer management seed must only insert missing portal web url config:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerBuyerManagementSeedMustRequireExplicitProfileBeforeBootstrapOrPatchWork()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String source = Files.readString(seedSql, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "set @seller_buyer_management_seed_profile",
                "create procedure assert_seller_buyer_management_seed_profile",
                "select target database before running seller/buyer management seed",
                "set @seller_buyer_management_seed_profile to FRESH_BOOTSTRAP or PATCH_EXISTING before running this seed",
                "seller/buyer seed requires top_menu_seed partner root 2010 before DDL",
                "seller/buyer seed FRESH_BOOTSTRAP profile requires no existing terminal tables; use PATCH_EXISTING",
                "seller/buyer seed PATCH_EXISTING profile requires existing terminal tables; use FRESH_BOOTSTRAP",
                "from information_schema.tables",
                "'seller_account_role'",
                "'buyer_role_menu'",
                "call assert_seller_buyer_management_seed_profile();",
                "call assert_terminal_menu_range_ready();",
                "drop procedure if exists assert_seller_buyer_management_seed_profile"
        })
        {
            requireContains(violations, seedSql.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_seller_buyer_management_seed_confirmed();",
                "call assert_seller_buyer_management_seed_profile();");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_seller_buyer_management_seed_profile();",
                "create table if not exists seller (");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_seller_buyer_management_seed_profile();",
                "insert into sys_config");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_terminal_menu_range_ready();",
                "insert into sys_config");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_terminal_menu_range_ready();",
                "insert into seller_role");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_terminal_menu_range_ready();",
                "insert into buyer_role");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_seller_buyer_management_seed_profile();",
                "insert into seller_role");

        if (!violations.isEmpty())
        {
            fail("seller/buyer management seed must require an explicit profile before bootstrap or patch work:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerBuyerManagementSeedMustFailClosedOnInactiveTerminalOwnerRoles()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String source = Files.readString(seedSql, StandardCharsets.UTF_8);
        String fileName = seedSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_terminal_owner_role_slots_ready",
                "join seller s on s.seller_id = r.seller_id",
                "join buyer b on b.buyer_id = r.buyer_id",
                "coalesce(r.status, '') <> '0'",
                "coalesce(r.del_flag, '') <> '0'",
                "seller owner role must be active before terminal owner grants",
                "buyer owner role must be active before terminal owner grants",
                "coalesce(r.role_name, '') <> 'Owner'",
                "coalesce(r.role_sort, -1) <> 1",
                "coalesce(r.remark, '') <> '默认卖家端 Owner 角色'",
                "coalesce(r.remark, '') <> '默认买家端 Owner 角色'",
                "seller owner role signature mismatch before terminal owner grants",
                "buyer owner role signature mismatch before terminal owner grants",
                "call assert_terminal_owner_role_slots_ready();",
                "join seller_role r on r.seller_id = a.seller_id\n"
                        + "                  and r.role_key = 'owner'\n"
                        + "                  and r.status = '0'\n"
                        + "                  and r.del_flag = '0'",
                "join buyer_role r on r.buyer_id = a.buyer_id\n"
                        + "                 and r.role_key = 'owner'\n"
                        + "                 and r.status = '0'\n"
                        + "                 and r.del_flag = '0'",
                "drop procedure if exists assert_terminal_owner_role_slots_ready"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_owner_role_slots_ready();",
                "insert into seller_role");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_owner_role_slots_ready();",
                "insert into buyer_role");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_owner_role_slots_ready();",
                "insert into seller_account_role");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_owner_role_slots_ready();",
                "insert into buyer_account_role");

        if (!violations.isEmpty())
        {
            fail("seller/buyer management seed must fail closed on inactive terminal owner roles:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalOwnerRoleSeedScriptsMustUseActiveOwnerRolesBeforeAccountBinding()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();

        Path accountSeed = backendRoot.resolve("sql/20260604_portal_account_list_permission_seed.sql");
        String accountSeedSource = Files.readString(accountSeed, StandardCharsets.UTF_8);
        for (String expected : new String[] {
                "where r.seller_id = s.seller_id\n"
                        + "        and r.role_key = 'owner'\n"
                        + "        and r.status = '0'\n"
                        + "        and r.del_flag = '0'",
                "where r.buyer_id = b.buyer_id\n"
                        + "        and r.role_key = 'owner'\n"
                        + "        and r.status = '0'\n"
                        + "        and r.del_flag = '0'",
                "join seller_role r on r.seller_id = a.seller_id\n"
                        + "                  and r.role_key = 'owner'\n"
                        + "                  and r.status = '0'\n"
                        + "                  and r.del_flag = '0'",
                "join buyer_role r on r.buyer_id = a.buyer_id\n"
                        + "                 and r.role_key = 'owner'\n"
                        + "                 and r.status = '0'\n"
                        + "                 and r.del_flag = '0'"
        })
        {
            requireContains(violations, accountSeed.getFileName().toString(), accountSeedSource, expected);
        }

        Path sellerSchemaSeed = backendRoot.resolve("sql/20260604_seller_product_schema_permission_seed.sql");
        String sellerSchemaSeedSource = Files.readString(sellerSchemaSeed, StandardCharsets.UTF_8);
        for (String expected : new String[] {
                "where r.seller_id = s.seller_id\n"
                        + "        and r.role_key = 'owner'\n"
                        + "        and r.status = '0'\n"
                        + "        and r.del_flag = '0'",
                "join seller_role r on r.seller_id = a.seller_id\n"
                        + "                  and r.role_key = 'owner'\n"
                        + "                  and r.status = '0'\n"
                        + "                  and r.del_flag = '0'"
        })
        {
            requireContains(violations, sellerSchemaSeed.getFileName().toString(), sellerSchemaSeedSource, expected);
        }

        Path buyerSchemaSeed = backendRoot.resolve("sql/20260604_buyer_product_schema_permission_seed.sql");
        String buyerSchemaSeedSource = Files.readString(buyerSchemaSeed, StandardCharsets.UTF_8);
        for (String expected : new String[] {
                "where r.buyer_id = b.buyer_id\n"
                        + "        and r.role_key = 'owner'\n"
                        + "        and r.status = '0'\n"
                        + "        and r.del_flag = '0'",
                "join buyer_role r on r.buyer_id = a.buyer_id\n"
                        + "                 and r.role_key = 'owner'\n"
                        + "                 and r.status = '0'\n"
                        + "                 and r.del_flag = '0'"
        })
        {
            requireContains(violations, buyerSchemaSeed.getFileName().toString(), buyerSchemaSeedSource, expected);
        }

        Path legacyRoleSql = backendRoot.resolve("sql/20260606_legacy_disable_sys_seller_buyer_roles.sql");
        String legacyRoleSource = Files.readString(legacyRoleSql, StandardCharsets.UTF_8);
        for (String expected : new String[] {
                "where r.seller_id = a.seller_id\n"
                        + "        and r.role_key = 'owner'\n"
                        + "        and r.status = '0'\n"
                        + "        and r.del_flag = '0'",
                "where r.buyer_id = a.buyer_id\n"
                        + "        and r.role_key = 'owner'\n"
                        + "        and r.status = '0'\n"
                        + "        and r.del_flag = '0'"
        })
        {
            requireContains(violations, legacyRoleSql.getFileName().toString(), legacyRoleSource, expected);
        }

        if (!violations.isEmpty())
        {
            fail("terminal owner role seed scripts must require active owner roles before account binding:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void accountLockMenuSeedsMustGuardSysMenuSlotsBeforeUpsert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();

        assertAccountLockMenuGuard(backendRoot.resolve("sql/20260605_seller_account_lock_control.sql"),
                2322, 2011, "seller:admin:account:lock", "seller account lock menu signature", violations);
        assertAccountLockMenuGuard(backendRoot.resolve("sql/20260605_buyer_account_lock_control.sql"),
                2323, 2012, "buyer:admin:account:lock", "buyer account lock menu signature", violations);

        if (!violations.isEmpty())
        {
            fail("account lock menu seeds must guard sys_menu slots before upsert:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void accountLockControlMustLockExactNormalizeTargetsBeforeUpdate() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();

        assertAccountLockNormalizeGuard(backendRoot.resolve("sql/20260605_seller_account_lock_control.sql"),
                "seller", "seller_account", "seller_account_id", "seller_id", violations);
        assertAccountLockNormalizeGuard(backendRoot.resolve("sql/20260605_buyer_account_lock_control.sql"),
                "buyer", "buyer_account", "buyer_account_id", "buyer_id", violations);

        if (!violations.isEmpty())
        {
            fail("account lock control scripts must lock exact normalize targets before account update:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalPermissionSeedsMustGuardMenuSlotsBeforeRoleBinding() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();
        String[] sellerBaselinePerms = new String[] {
                "seller:account:list",
                "seller:account:add",
                "seller:account:edit",
                "seller:account:role:query",
                "seller:account:role:edit",
                "seller:account:loginLog:list",
                "seller:account:operLog:list",
                "seller:account:session:list",
                "seller:dept:list",
                "seller:dept:query",
                "seller:dept:add",
                "seller:dept:edit",
                "seller:dept:remove",
                "seller:role:list",
                "seller:role:query",
                "seller:role:add",
                "seller:role:edit",
                "seller:role:remove",
                "seller:product:category:list",
                "seller:product:schema:query",
                "seller:product:distribution:list",
                "seller:product:distribution:query"
        };
        String[] buyerBaselinePerms = new String[] {
                "buyer:account:list",
                "buyer:account:add",
                "buyer:account:edit",
                "buyer:account:role:query",
                "buyer:account:role:edit",
                "buyer:account:loginLog:list",
                "buyer:account:operLog:list",
                "buyer:account:session:list",
                "buyer:dept:list",
                "buyer:dept:query",
                "buyer:dept:add",
                "buyer:dept:edit",
                "buyer:dept:remove",
                "buyer:role:list",
                "buyer:role:query",
                "buyer:role:add",
                "buyer:role:edit",
                "buyer:role:remove",
                "buyer:product:category:list",
                "buyer:product:schema:query",
                "buyer:product:distribution:list",
                "buyer:product:distribution:query"
        };

        assertTerminalPermissionSeedMenuGuard(backendRoot.resolve("sql/seller_buyer_management_seed.sql"),
                sellerBaselinePerms, buyerBaselinePerms, violations);
        assertTerminalPermissionSeedMenuGuard(backendRoot.resolve("sql/20260604_portal_account_list_permission_seed.sql"),
                new String[] {"seller:account:list"}, new String[] {"buyer:account:list"}, violations);
        assertTerminalPermissionSeedMenuGuard(backendRoot.resolve("sql/20260604_portal_dept_role_list_permission_seed.sql"),
                new String[] {"seller:dept:list", "seller:role:list"},
                new String[] {"buyer:dept:list", "buyer:role:list"}, violations);
        assertTerminalPermissionSeedMenuOnlyGuard(backendRoot.resolve("sql/20260604_portal_product_category_permission_seed.sql"),
                new String[] {"seller:product:category:list"},
                new String[] {"buyer:product:category:list"}, violations);
        assertTerminalPermissionSeedMenuOnlyGuard(backendRoot.resolve("sql/20260604_seller_product_schema_permission_seed.sql"),
                new String[] {"seller:product:schema:query"}, new String[0], violations);
        assertTerminalPermissionSeedMenuOnlyGuard(backendRoot.resolve("sql/20260604_buyer_product_schema_permission_seed.sql"),
                new String[0], new String[] {"buyer:product:schema:query"}, violations);
        assertTerminalPermissionSeedMenuGuard(backendRoot.resolve("sql/20260607_portal_self_audit_permission_seed.sql"),
                new String[] {
                        "seller:account:loginLog:list",
                        "seller:account:operLog:list",
                        "seller:account:session:list"
                },
                new String[] {
                        "buyer:account:loginLog:list",
                        "buyer:account:operLog:list",
                        "buyer:account:session:list"
                }, violations);
        assertTerminalPermissionSeedMenuGuard(backendRoot.resolve("sql/20260610_portal_self_management_permission_seed.sql"),
                new String[] {
                        "seller:account:add",
                        "seller:account:edit",
                        "seller:account:role:query",
                        "seller:account:role:edit",
                        "seller:dept:query",
                        "seller:dept:add",
                        "seller:dept:edit",
                        "seller:dept:remove",
                        "seller:role:query",
                        "seller:role:add",
                        "seller:role:edit",
                        "seller:role:remove"
                },
                new String[] {
                        "buyer:account:add",
                        "buyer:account:edit",
                        "buyer:account:role:query",
                        "buyer:account:role:edit",
                        "buyer:dept:query",
                        "buyer:dept:add",
                        "buyer:dept:edit",
                        "buyer:dept:remove",
                        "buyer:role:query",
                        "buyer:role:add",
                        "buyer:role:edit",
                        "buyer:role:remove"
                }, violations);

        if (!violations.isEmpty())
        {
            fail("terminal permission seeds must fail-closed guard seller_menu/buyer_menu signatures:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void splitTerminalPermissionSeedsMustFailClosedOnInvalidOrDuplicatePermsBeforeRoleBinding()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();

        assertSplitTerminalPermissionSeedPreflight(
                backendRoot.resolve("sql/20260604_portal_account_list_permission_seed.sql"),
                true, true, violations);
        assertSplitTerminalPermissionSeedPreflight(
                backendRoot.resolve("sql/20260604_portal_dept_role_list_permission_seed.sql"),
                true, true, violations);
        assertSplitTerminalPermissionSeedPreflight(
                backendRoot.resolve("sql/20260604_portal_product_category_permission_seed.sql"),
                true, true, violations);
        assertSplitTerminalPermissionSeedPreflight(
                backendRoot.resolve("sql/20260604_seller_product_schema_permission_seed.sql"),
                true, false, violations);
        assertSplitTerminalPermissionSeedPreflight(
                backendRoot.resolve("sql/20260604_buyer_product_schema_permission_seed.sql"),
                false, true, violations);
        assertSplitTerminalPermissionSeedPreflight(
                backendRoot.resolve("sql/20260607_portal_self_audit_permission_seed.sql"),
                true, true, violations);
        assertSplitTerminalPermissionSeedPreflight(
                backendRoot.resolve("sql/20260610_terminal_portal_home_menu_seed.sql"),
                true, true, violations);
        assertSplitTerminalPermissionSeedPreflight(
                backendRoot.resolve("sql/20260610_portal_self_management_permission_seed.sql"),
                true, true, violations);

        if (!violations.isEmpty())
        {
            fail("split terminal permission seeds must fail closed on invalid or duplicate terminal perms before role binding:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void splitTerminalPermissionSeedsMustWrapRoleBindingInTransactionAndAssertCompletion()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();

        assertSplitTerminalPermissionSeedTransaction(
                backendRoot.resolve("sql/20260604_portal_account_list_permission_seed.sql"),
                "assert_portal_account_list_permission_seed_completed",
                "insert into seller_role", violations);
        assertSplitTerminalPermissionSeedTransaction(
                backendRoot.resolve("sql/20260604_portal_dept_role_list_permission_seed.sql"),
                "assert_portal_dept_role_list_permission_seed_completed",
                "insert into seller_role", violations);
        assertSplitTerminalPermissionSeedTransaction(
                backendRoot.resolve("sql/20260604_portal_product_category_permission_seed.sql"),
                "assert_portal_product_category_permission_seed_completed",
                "insert into seller_menu", violations);
        assertSplitTerminalPermissionSeedTransaction(
                backendRoot.resolve("sql/20260604_seller_product_schema_permission_seed.sql"),
                "assert_seller_product_schema_permission_seed_completed",
                "insert into seller_role", violations);
        assertSplitTerminalPermissionSeedTransaction(
                backendRoot.resolve("sql/20260604_buyer_product_schema_permission_seed.sql"),
                "assert_buyer_product_schema_permission_seed_completed",
                "insert into buyer_role", violations);
        assertSplitTerminalPermissionSeedTransaction(
                backendRoot.resolve("sql/20260607_portal_self_audit_permission_seed.sql"),
                "assert_portal_self_audit_permission_seed_completed",
                "insert into seller_menu", violations);
        assertSplitTerminalPermissionSeedTransaction(
                backendRoot.resolve("sql/20260610_terminal_portal_home_menu_seed.sql"),
                "assert_terminal_portal_home_menu_seed_completed",
                "insert into seller_menu", violations);
        assertSplitTerminalPermissionSeedTransaction(
                backendRoot.resolve("sql/20260610_portal_self_management_permission_seed.sql"),
                "assert_portal_self_management_permission_seed_completed",
                "insert into seller_menu", violations);
        String portalSelfAuditSource = Files.readString(
                backendRoot.resolve("sql/20260607_portal_self_audit_permission_seed.sql"),
                StandardCharsets.UTF_8);
        requireContains(violations, "20260607_portal_self_audit_permission_seed.sql", portalSelfAuditSource,
                "seller owner roles self audit permission exact grant count mismatch");
        requireContains(violations, "20260607_portal_self_audit_permission_seed.sql", portalSelfAuditSource,
                "buyer owner roles self audit permission exact grant count mismatch");
        String portalHomeSource = Files.readString(
                backendRoot.resolve("sql/20260610_terminal_portal_home_menu_seed.sql"),
                StandardCharsets.UTF_8);
        requireContains(violations, "20260610_terminal_portal_home_menu_seed.sql", portalHomeSource,
                "seller owner roles portal home page menu exact grant count mismatch");
        requireContains(violations, "20260610_terminal_portal_home_menu_seed.sql", portalHomeSource,
                "buyer owner roles portal home page menu exact grant count mismatch");
        String portalSelfManagementSource = Files.readString(
                backendRoot.resolve("sql/20260610_portal_self_management_permission_seed.sql"),
                StandardCharsets.UTF_8);
        requireContains(violations, "20260610_portal_self_management_permission_seed.sql", portalSelfManagementSource,
                "seller owner roles self-management permission exact grant count mismatch");
        requireContains(violations, "20260610_portal_self_management_permission_seed.sql", portalSelfManagementSource,
                "buyer owner roles self-management permission exact grant count mismatch");
        requireContains(violations, "20260610_portal_self_management_permission_seed.sql", portalSelfManagementSource,
                "v_owner_role_count * 19");
        requireContains(violations, "20260610_portal_self_management_permission_seed.sql", portalSelfManagementSource,
                "coalesce(perms, '') like '%*%'");
        requireContains(violations, "20260610_portal_self_management_permission_seed.sql", portalSelfManagementSource,
                "seller owner role menu contains non self-management permission grants");
        requireContains(violations, "20260610_portal_self_management_permission_seed.sql", portalSelfManagementSource,
                "buyer owner role menu contains non self-management permission grants");
        requireContains(violations, "20260610_portal_self_management_permission_seed.sql", portalSelfManagementSource,
                "delete rm\nfrom seller_role_menu rm\njoin seller_role r on r.seller_role_id = rm.seller_role_id");
        requireContains(violations, "20260610_portal_self_management_permission_seed.sql", portalSelfManagementSource,
                "delete rm\nfrom buyer_role_menu rm\njoin buyer_role r on r.buyer_role_id = rm.buyer_role_id");
        requireContains(violations, "20260610_portal_self_management_permission_seed.sql", portalSelfManagementSource,
                "where r.del_flag = '0'\n  and r.status = '0'\n  and r.role_key = 'owner'\n  and expected.perms is null;");

        if (!violations.isEmpty())
        {
            fail("split terminal permission seeds must wrap DML and verify completion before commit:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void portalSelfManagementSeedMustGrantPortalHomeAsPageMenu()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/20260610_portal_self_management_permission_seed.sql");
        String source = Files.readString(seedSql, StandardCharsets.UTF_8);
        String fileName = seedSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "join seller_menu m on m.perms = 'seller:portal:home'",
                "and coalesce(m.menu_type, '') = 'C'",
                "and coalesce(m.path, '') = '/seller/portal'",
                "and coalesce(m.component, '') = 'Seller/Portal/index'",
                "and coalesce(m.route_name, '') = 'SellerPortalHome'",
                "join buyer_menu m on m.perms = 'buyer:portal:home'",
                "and coalesce(m.path, '') = '/buyer/portal'",
                "and coalesce(m.component, '') = 'Buyer/Portal/index'",
                "and coalesce(m.route_name, '') = 'BuyerPortalHome'"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertSelfManagementRootButtonGrantExcludesPortalHome(source, fileName, "seller", violations);
        assertSelfManagementRootButtonGrantExcludesPortalHome(source, fileName, "buyer", violations);

        if (!violations.isEmpty())
        {
            fail("portal self-management seed must grant portal home as C page menu and keep root F grants separate:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalPortalHomeMenuSeedMustGuardPageMenuSignaturesAndOwnerGrants()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/20260610_terminal_portal_home_menu_seed.sql");
        String source = Files.readString(seedSql, StandardCharsets.UTF_8);
        String fileName = seedSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "call assert_seller_menu_permission_slot('seller:portal:home', 0, 'C', '/seller/portal', 'Seller/Portal/index', 'SellerPortalHome'",
                "call assert_buyer_menu_permission_slot('buyer:portal:home', 0, 'C', '/buyer/portal', 'Buyer/Portal/index', 'BuyerPortalHome'",
                "seller:portal:home menu slot is occupied by another signature",
                "buyer:portal:home menu slot is occupied by another signature",
                "seller portal home page menu was not created with expected signature",
                "buyer portal home page menu was not created with expected signature",
                "seller owner roles must have portal home page menu",
                "buyer owner roles must have portal home page menu",
                "seller owner roles portal home page menu exact grant count mismatch",
                "buyer owner roles portal home page menu exact grant count mismatch",
                "and m.parent_id = 0",
                "and coalesce(m.menu_type, '') = 'C'",
                "and coalesce(m.path, '') = '/seller/portal'",
                "and coalesce(m.component, '') = 'Seller/Portal/index'",
                "and coalesce(m.route_name, '') = 'SellerPortalHome'",
                "and coalesce(m.path, '') = '/buyer/portal'",
                "and coalesce(m.component, '') = 'Buyer/Portal/index'",
                "and coalesce(m.route_name, '') = 'BuyerPortalHome'"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "call assert_seller_menu_permission_slot('seller:portal:home'", "insert into seller_menu");
        assertAppearsBefore(violations, fileName, source,
                "call assert_buyer_menu_permission_slot('buyer:portal:home'", "insert into buyer_menu");
        assertAppearsBefore(violations, fileName, source,
                "insert into seller_menu", "insert into seller_role_menu");
        assertAppearsBefore(violations, fileName, source,
                "insert into buyer_menu", "insert into buyer_role_menu");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_portal_home_menu_seed_completed();", "commit;");

        if (!violations.isEmpty())
        {
            fail("terminal portal home menu seed must guard C page signatures and exact owner grants:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalOwnerProductPermissionCleanupMustLockExactRoleMenuTargetsAndKeepMenus()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path cleanupSql = backendRoot.resolve("sql/20260610_terminal_owner_product_permission_cleanup.sql");
        String source = Files.readString(cleanupSql, StandardCharsets.UTF_8);
        String fileName = cleanupSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@terminal_owner_product_cleanup_expected_seller_count",
                "@terminal_owner_product_cleanup_expected_seller_signature",
                "@terminal_owner_product_cleanup_expected_buyer_count",
                "@terminal_owner_product_cleanup_expected_buyer_signature",
                "set @terminal_owner_product_cleanup_expected_seller_count after previewing exact seller owner product grants",
                "set @terminal_owner_product_cleanup_expected_seller_signature after previewing exact seller owner product grants",
                "set @terminal_owner_product_cleanup_expected_buyer_count after previewing exact buyer owner product grants",
                "set @terminal_owner_product_cleanup_expected_buyer_signature after previewing exact buyer owner product grants",
                "create procedure assert_seller_owner_product_permission_cleanup_targets",
                "create procedure assert_buyer_owner_product_permission_cleanup_targets",
                "seller owner product grant exact target count mismatch",
                "seller owner product grant exact target signature mismatch",
                "buyer owner product grant exact target count mismatch",
                "buyer owner product grant exact target signature mismatch",
                "seller owner product grants cleanup has remaining rows",
                "buyer owner product grants cleanup has remaining rows",
                "seller product permission menu definitions must remain after cleanup",
                "buyer product permission menu definitions must remain after cleanup",
                "delete rm\nfrom seller_role_menu rm",
                "delete rm\nfrom buyer_role_menu rm",
                "m.perms like 'seller:product:%'",
                "m.perms like 'buyer:product:%'",
                "r.role_key = 'owner'",
                "r.status = '0'",
                "r.del_flag = '0'",
                "sha2(coalesce(group_concat(",
                "call assert_terminal_owner_product_permission_cleanup_confirmed();",
                "call assert_terminal_owner_product_permission_cleanup_completed();"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        requireNotContains(violations, fileName, source, "delete from seller_menu");
        requireNotContains(violations, fileName, source, "delete from buyer_menu");
        assertAppearsBefore(violations, fileName, source,
                "call assert_seller_owner_product_permission_cleanup_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_buyer_owner_product_permission_cleanup_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "delete rm\nfrom seller_role_menu rm");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "delete rm\nfrom buyer_role_menu rm");
        assertAppearsBefore(violations, fileName, source,
                "delete rm\nfrom buyer_role_menu rm",
                "call assert_terminal_owner_product_permission_cleanup_completed();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_owner_product_permission_cleanup_completed();", "commit;");

        if (!violations.isEmpty())
        {
            fail("terminal owner product permission cleanup must lock exact role_menu targets and keep menus:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerBuyerManagementSeedMustBootstrapDirectLoginAuditAndTicketSchema()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String source = Files.readString(seedSql, StandardCharsets.UTF_8);
        String fileName = seedSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String tableName : new String[] {
                "seller_login_log",
                "buyer_login_log",
                "seller_oper_log",
                "buyer_oper_log",
                "seller_session",
                "buyer_session"
        })
        {
            assertCreateTableContains(source, fileName, tableName, new String[] {
                    "direct_login          tinyint(1)      not null default 0",
                    "direct_login_ticket_id bigint(20)     default null",
                    "acting_admin_id       bigint(20)      default null",
                    "acting_admin_name     varchar(64)     default ''",
                    "direct_login_reason   varchar(255)    default ''"
            }, violations);
        }

        assertCreateTableContains(source, fileName, "portal_direct_login_ticket", new String[] {
                "ticket_id             bigint(20)      not null auto_increment",
                "terminal              varchar(20)     not null",
                "target_subject_id     bigint(20)      not null",
                "target_subject_no     varchar(64)     default ''",
                "target_account_id     bigint(20)      not null",
                "target_user_name      varchar(64)     not null",
                "acting_admin_id       bigint(20)      not null",
                "acting_admin_name     varchar(64)     not null",
                "reason                varchar(255)    default ''",
                "token_hash            varchar(64)     not null",
                "expire_time           datetime        not null",
                "used_time             datetime        default null",
                "used_ip               varchar(128)    default ''",
                "status                varchar(20)     not null default 'ISSUED'",
                "primary key (ticket_id)",
                "unique key uk_portal_direct_login_ticket_hash (token_hash)",
                "key idx_portal_direct_login_ticket_target (terminal, target_subject_id, target_account_id)",
                "key idx_portal_direct_login_ticket_admin_time (acting_admin_id, create_time)",
                "key idx_portal_direct_login_ticket_status_expire (status, expire_time)"
        }, violations);

        for (String expected : new String[] {
                "where not exists (select 1 from sys_config where config_key = 'portal.seller.web.url')",
                "where not exists (select 1 from sys_config where config_key = 'portal.buyer.web.url')"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        if (!violations.isEmpty())
        {
            fail("seller_buyer_management_seed.sql must bootstrap direct-login audit/ticket schema without losing admin control:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerBuyerManagementSeedMustPreflightRoleMenuAndAssertFinalState()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String source = Files.readString(seedSql, StandardCharsets.UTF_8);
        String fileName = seedSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "seller_role_menu has orphan or out-of-range seller_menu_id values",
                "buyer_role_menu has orphan or out-of-range buyer_menu_id values",
                "create procedure assert_seller_buyer_management_seed_completed",
                "seller/buyer management sys_menu final signature mismatch",
                "seller/buyer management portal web url config is incomplete",
                "seller/buyer management seller terminal permission final state mismatch",
                "seller/buyer management buyer terminal permission final state mismatch",
                "seller_role_menu final state has orphan or out-of-range seller_menu_id values",
                "buyer_role_menu final state has orphan or out-of-range buyer_menu_id values",
                "seller_account_role final state has orphan or cross-subject role bindings",
                "buyer_account_role final state has orphan or cross-subject role bindings",
                "seller owner account role binding final state mismatch",
                "buyer owner account role binding final state mismatch",
                "seller owner role terminal permission grants final state mismatch",
                "buyer owner role terminal permission grants final state mismatch",
                "seller owner role terminal permission grants final state has unexpected permissions",
                "buyer owner role terminal permission grants final state has unexpected permissions",
                "left join seller_account_role ar on ar.seller_account_id = a.seller_account_id",
                "left join buyer_account_role ar on ar.buyer_account_id = a.buyer_account_id",
                "left join seller_role_menu rm on rm.seller_role_id = r.seller_role_id",
                "left join buyer_role_menu rm on rm.buyer_role_id = r.buyer_role_id",
                "start transaction;",
                "commit;",
                "call assert_seller_buyer_management_seed_completed();",
                "call assert_seller_buyer_management_seed_completed();\n\ncommit;",
                "commit;\n\ndrop temporary table if exists tmp_seller_buyer_sys_menu_guard;",
                "drop procedure if exists assert_seller_buyer_management_seed_completed"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_menu_range_ready();", "insert into sys_config");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_menu_range_ready();", "insert into seller_role_menu");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_owner_role_slots_ready();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into sys_dict_type");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into seller_role");
        assertAppearsBefore(violations, fileName, source,
                "insert into buyer_role_menu", "call assert_seller_buyer_management_seed_completed();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_seller_buyer_management_seed_completed();", "commit;");

        if (!violations.isEmpty())
        {
            fail("seller/buyer management seed must preflight role-menu integrity and assert final state:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalMenuIdRangeIsolationMustKeepScopedMigrationSequence() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260607_terminal_menu_id_range_isolation.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@terminal_menu_range_seller_menu_expected_count",
                "@terminal_menu_range_seller_menu_expected_signature",
                "@terminal_menu_range_seller_role_menu_expected_count",
                "@terminal_menu_range_seller_role_menu_expected_signature",
                "@terminal_menu_range_buyer_menu_expected_count",
                "@terminal_menu_range_buyer_menu_expected_signature",
                "@terminal_menu_range_buyer_role_menu_expected_count",
                "@terminal_menu_range_buyer_role_menu_expected_signature",
                "set @terminal_menu_range_seller_menu_expected_count after previewing exact seller_menu low-ID or low-parent rows",
                "set @terminal_menu_range_buyer_menu_expected_count after previewing exact buyer_menu low-ID or low-parent rows",
                "create procedure assert_terminal_menu_ids_are_migratable",
                "create procedure assert_no_terminal_menu_parent_orphans",
                "create procedure assert_terminal_role_menu_ids_are_migratable",
                "create procedure assert_terminal_menu_id_range_expected_targets",
                "create procedure assert_terminal_menu_ids_are_in_final_ranges",
                "create procedure assert_terminal_role_menu_ids_are_in_final_ranges",
                "call assert_no_terminal_menu_orphans();",
                "call assert_no_terminal_menu_parent_orphans();",
                "call assert_terminal_menu_ids_are_migratable();",
                "call assert_terminal_role_menu_ids_are_migratable();",
                "call assert_terminal_menu_id_range_expected_targets();",
                "seller_menu low-ID exact target signature mismatch",
                "seller_role_menu low-ID exact target signature mismatch",
                "buyer_menu low-ID exact target signature mismatch",
                "buyer_role_menu low-ID exact target signature mismatch",
                "update seller_role_menu\nset seller_menu_id = seller_menu_id + 100000",
                "update seller_menu\nset parent_id = parent_id + 100000",
                "update seller_menu\nset seller_menu_id = seller_menu_id + 100000",
                "update buyer_role_menu\nset buyer_menu_id = buyer_menu_id + 200000",
                "update buyer_menu\nset parent_id = parent_id + 200000",
                "update buyer_menu\nset buyer_menu_id = buyer_menu_id + 200000",
                "call assert_terminal_menu_ids_are_in_final_ranges();",
                "call assert_terminal_role_menu_ids_are_in_final_ranges();"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        requireContains(violations, fileName, source,
                "seller_menu has orphan parent_id values");
        requireContains(violations, fileName, source,
                "buyer_menu has orphan parent_id values");
        requireContains(violations, fileName, source,
                "where (m.seller_menu_id > 0\n    and m.seller_menu_id < 100000)\n    or (m.parent_id > 0\n    and m.parent_id < 100000);");
        requireContains(violations, fileName, source,
                "where (m.buyer_menu_id > 0\n    and m.buyer_menu_id < 100000)\n    or (m.parent_id > 0\n    and m.parent_id < 100000);");
        requireContains(violations, fileName, source,
                "call assert_no_terminal_menu_orphans();\ncall assert_no_terminal_menu_parent_orphans();\ncall assert_terminal_menu_ids_are_migratable();");
        requireContains(violations, fileName, source,
                "update buyer_menu\nset buyer_menu_id = buyer_menu_id + 200000\nwhere buyer_menu_id > 0\n  and buyer_menu_id < 100000;\n\ncall assert_no_terminal_menu_orphans();\ncall assert_no_terminal_menu_parent_orphans();\ncall assert_terminal_menu_ids_are_in_final_ranges();");
        requireContains(violations, fileName, source,
                "call assert_no_terminal_menu_orphans();\ncall assert_no_terminal_menu_parent_orphans();\ncall assert_terminal_menu_ids_are_in_final_ranges();\ncall assert_terminal_role_menu_ids_are_in_final_ranges();\n\ncommit;");
        if (source.contains("reset_terminal_menu_auto_increment") || source.toLowerCase().contains("auto_increment ="))
        {
            violations.add(fileName + " must not mix auto_increment reset DDL into the ID-range migration transaction");
        }

        assertAppearsBefore(violations, fileName, source,
                "call assert_no_terminal_menu_parent_orphans();",
                "call assert_terminal_menu_ids_are_migratable();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_menu_ids_are_migratable();",
                "call assert_terminal_role_menu_ids_are_migratable();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_role_menu_ids_are_migratable();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_menu_id_range_expected_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source, "start transaction;",
                "update seller_role_menu\nset seller_menu_id = seller_menu_id + 100000");
        assertAppearsBefore(violations, fileName, source,
                "update seller_role_menu\nset seller_menu_id = seller_menu_id + 100000",
                "update seller_menu\nset parent_id = parent_id + 100000");
        assertAppearsBefore(violations, fileName, source,
                "update seller_menu\nset parent_id = parent_id + 100000",
                "update seller_menu\nset seller_menu_id = seller_menu_id + 100000");
        assertAppearsBefore(violations, fileName, source,
                "update seller_menu\nset seller_menu_id = seller_menu_id + 100000",
                "update buyer_role_menu\nset buyer_menu_id = buyer_menu_id + 200000");
        assertAppearsBefore(violations, fileName, source,
                "update buyer_role_menu\nset buyer_menu_id = buyer_menu_id + 200000",
                "update buyer_menu\nset parent_id = parent_id + 200000");
        assertAppearsBefore(violations, fileName, source,
                "update buyer_menu\nset parent_id = parent_id + 200000",
                "update buyer_menu\nset buyer_menu_id = buyer_menu_id + 200000");
        assertAppearsBefore(violations, fileName, source,
                "update buyer_menu\nset buyer_menu_id = buyer_menu_id + 200000",
                "call assert_terminal_menu_ids_are_in_final_ranges();");

        if (!violations.isEmpty())
        {
            fail("terminal menu ID range isolation must preserve scoped role-menu, parent, ID, and auto-increment migration order:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalMenuAutoIncrementResetMustBeSeparateConfirmedStep() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260608_terminal_menu_auto_increment_reset.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@confirm_terminal_menu_auto_increment_reset",
                "@terminal_menu_auto_increment_seller_expected_count",
                "@terminal_menu_auto_increment_seller_expected_signature",
                "@terminal_menu_auto_increment_buyer_expected_count",
                "@terminal_menu_auto_increment_buyer_expected_signature",
                "APPLY_TERMINAL_MENU_AUTO_INCREMENT_RESET",
                "create procedure assert_terminal_menu_auto_increment_reset_confirmed",
                "create procedure assert_no_terminal_menu_orphans",
                "create procedure assert_no_terminal_menu_parent_orphans",
                "create procedure assert_terminal_menu_ids_are_in_final_ranges",
                "create procedure assert_terminal_role_menu_ids_are_in_final_ranges",
                "create procedure assert_terminal_menu_auto_increment_targets",
                "create procedure reset_terminal_menu_auto_increment",
                "create procedure assert_terminal_menu_auto_increment_value",
                "set @terminal_menu_auto_increment_seller_expected_count after previewing exact seller_menu rows",
                "set @terminal_menu_auto_increment_seller_expected_signature after previewing exact seller_menu rows",
                "set @terminal_menu_auto_increment_buyer_expected_count after previewing exact buyer_menu rows",
                "set @terminal_menu_auto_increment_buyer_expected_signature after previewing exact buyer_menu rows",
                "seller_menu auto_increment reset exact target count mismatch",
                "seller_menu auto_increment reset exact target signature mismatch",
                "buyer_menu auto_increment reset exact target count mismatch",
                "buyer_menu auto_increment reset exact target signature mismatch",
                "call assert_terminal_menu_auto_increment_reset_confirmed();",
                "call assert_no_terminal_menu_orphans();",
                "call assert_no_terminal_menu_parent_orphans();",
                "call assert_terminal_menu_ids_are_in_final_ranges();",
                "call assert_terminal_role_menu_ids_are_in_final_ranges();",
                "call assert_terminal_menu_auto_increment_targets();",
                "in p_ceiling_exclusive bigint",
                "if @next_terminal_menu_auto_increment >= p_ceiling_exclusive then",
                "auto_increment would exceed reserved terminal menu ID range",
                "select auto_increment",
                "auto_increment post-assert mismatch",
                "call reset_terminal_menu_auto_increment('seller_menu', 'seller_menu_id', 100000, 200000);",
                "call reset_terminal_menu_auto_increment('buyer_menu', 'buyer_menu_id', 200000, 300000);",
                "call assert_terminal_menu_auto_increment_value('seller_menu', 'seller_menu_id', 100000, 200000);",
                "call assert_terminal_menu_auto_increment_value('buyer_menu', 'buyer_menu_id', 200000, 300000);",
                "select greatest(', p_floor, ', coalesce(max(', p_id_column, '), 0) + 1)",
                "alter table `', p_table, '` auto_increment = ",
                "drop procedure if exists assert_terminal_menu_auto_increment_value;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_role_menu_ids_are_in_final_ranges();",
                "call assert_terminal_menu_auto_increment_targets();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_menu_auto_increment_targets();",
                "call reset_terminal_menu_auto_increment('seller_menu', 'seller_menu_id', 100000, 200000);");
        assertAppearsBefore(violations, fileName, source,
                "call reset_terminal_menu_auto_increment('buyer_menu', 'buyer_menu_id', 200000, 300000);",
                "call assert_terminal_menu_auto_increment_value('seller_menu', 'seller_menu_id', 100000, 200000);");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_menu_auto_increment_value('seller_menu', 'seller_menu_id', 100000, 200000);",
                "call assert_terminal_menu_auto_increment_value('buyer_menu', 'buyer_menu_id', 200000, 300000);");
        assertAppearsBefore(violations, fileName, source,
                "call reset_terminal_menu_auto_increment('buyer_menu', 'buyer_menu_id', 200000, 300000);",
                "drop procedure if exists assert_terminal_menu_auto_increment_reset_confirmed;");

        if (!violations.isEmpty())
        {
            fail("terminal menu auto_increment reset must be a separate confirmed post-range step:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalMenuSeedsMustBeAutoDiscoveredAndStayTerminalScoped() throws IOException
    {
        Path sqlDir = findWorkspaceRoot().resolve("RuoYi-Vue/sql");
        List<String> violations = new ArrayList<>();

        try (java.util.stream.Stream<Path> sqlFiles = Files.list(sqlDir))
        {
            for (Path sqlFile : sqlFiles
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList())
            {
                String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
                String fileName = sqlFile.getFileName().toString();
                if (isTerminalMenuMaintenanceSql(fileName))
                {
                    continue;
                }
                assertAutoDiscoveredTerminalMenuSeed(source, fileName, "seller", violations);
                assertAutoDiscoveredTerminalMenuSeed(source, fileName, "buyer", violations);
            }
        }

        if (!violations.isEmpty())
        {
            fail("terminal menu SQL seeds must be auto-discovered and stay terminal-scoped:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalMenuSeedDiscoveryMustCatchUpdateAndDeleteMutations()
    {
        List<String> violations = new ArrayList<>();
        assertAutoDiscoveredTerminalMenuSeed(
                "update seller_menu set perms = 'seller:product:list' where seller_menu_id = 100001;",
                "synthetic_terminal_menu_update.sql", "seller", violations);
        assertAutoDiscoveredTerminalMenuSeed(
                "delete from buyer_menu where perms = 'buyer:product:list';",
                "synthetic_terminal_menu_delete.sql", "buyer", violations);

        if (violations.size() < 2)
        {
            fail("terminal menu update/delete mutations must trigger terminal-scoped seed guard assertions");
        }
    }

    @Test
    public void terminalMenuSeedDiscoveryMustRejectBlankUpdateValues()
    {
        List<String> blankComponentViolations = new ArrayList<>();
        assertAutoDiscoveredTerminalMenuSeed(
                "create procedure assert_seller_menu_permission_slot(in p_perms varchar(100)) begin select 1; end;\n"
                        + "call assert_seller_menu_permission_slot('seller:product:list');\n"
                        + "update seller_menu set component = '' where seller_menu_id = 100001;",
                "synthetic_terminal_menu_blank_component_update.sql", "seller", blankComponentViolations);
        if (blankComponentViolations.stream()
                .noneMatch(violation -> violation.contains("must not update terminal menu component to blank")))
        {
            fail("terminal menu update mutations must reject blank component updates:\n"
                    + String.join("\n", blankComponentViolations));
        }

        List<String> blankPermsViolations = new ArrayList<>();
        assertAutoDiscoveredTerminalMenuSeed(
                "create procedure assert_buyer_menu_permission_slot(in p_perms varchar(100)) begin select 1; end;\n"
                        + "call assert_buyer_menu_permission_slot('buyer:product:list');\n"
                        + "update buyer_menu set perms = '' where buyer_menu_id = 200001;",
                "synthetic_terminal_menu_blank_perms_update.sql", "buyer", blankPermsViolations);
        if (blankPermsViolations.stream()
                .noneMatch(violation -> violation.contains("must not update terminal menu perms to blank")))
        {
            fail("terminal menu update mutations must reject blank perms updates:\n"
                    + String.join("\n", blankPermsViolations));
        }
    }

    @Test
    public void warehouseManagementMenuSeedMustGuardSysMenuSlotsBeforeUpsert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/warehouse_management_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_warehouse_management_sys_menu_guard",
                "tmp_warehouse_management_sys_menu_guard",
                "warehouse management parent sys_menu 2020 is required before warehouse management seed",
                "warehouse management sys_menu id slot is occupied by another menu",
                "warehouse management sys_menu signature is already used by another menu",
                "m.parent_id <> seed.parent_id",
                "coalesce(m.menu_type, '') <> coalesce(seed.menu_type, '')",
                "insert into tmp_warehouse_management_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(2021, 2020, 'C', 'official', 'Warehouse/Official/index', 'OfficialWarehouse', 'warehouse:official:list')",
                "(2022, 2020, 'C', 'third-party', 'Warehouse/ThirdParty/index', 'ThirdPartyWarehouse', 'warehouse:thirdParty:list')",
                "(202101, 2021, 'F', '#', '', '', 'warehouse:official:list')",
                "(202102, 2021, 'F', '#', '', '', 'warehouse:official:add')",
                "(202103, 2021, 'F', '#', '', '', 'warehouse:official:edit')",
                "(202104, 2021, 'F', '#', '', '', 'warehouse:official:status')",
                "(202105, 2021, 'F', '#', '', '', 'warehouse:official:sync')",
                "(202201, 2022, 'F', '#', '', '', 'warehouse:thirdParty:list')",
                "(202202, 2022, 'F', '#', '', '', 'warehouse:thirdParty:add')",
                "(202203, 2022, 'F', '#', '', '', 'warehouse:thirdParty:edit')",
                "(202204, 2022, 'F', '#', '', '', 'warehouse:thirdParty:status')",
                "call assert_warehouse_management_sys_menu_guard();",
                "create procedure assert_warehouse_management_seed_completed",
                "call assert_warehouse_management_seed_completed();",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_warehouse_management_sys_menu_guard",
                "drop procedure if exists assert_warehouse_management_seed_completed",
                "drop procedure if exists assert_warehouse_management_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_warehouse_management_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_warehouse_management_sys_menu_guard();", "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "insert into sys_dict_type");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_warehouse_management_seed_completed();", "commit;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "commit;", "drop procedure if exists assert_warehouse_management_seed_completed;");

        if (!violations.isEmpty())
        {
            fail("warehouse management menu seed must guard sys_menu slots before upsert:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void productCategoryAttributeMenuSeedMustGuardSysMenuSlotsBeforeUpsert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260604_product_category_attribute_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_product_category_attribute_sys_menu_guard",
                "create procedure assert_product_category_attribute_seed_completed",
                "tmp_product_category_attribute_sys_menu_guard",
                "tmp_product_category_attribute_seed_expected",
                "product category attribute sys_menu id slot is occupied by another menu",
                "product category attribute sys_menu signature is already used by another menu",
                "product category attribute seed completion mismatch",
                "and m.parent_id = seed.parent_id",
                "and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')",
                "insert into tmp_product_category_attribute_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(2060, 0, 'M', 'product', '', 'ProductManagement', '')",
                "(2440, 2090, 'C', 'product-category', 'Product/Category/index', 'ProductCategoryConfig', 'product:category:list')",
                "(2440, 2090, 'C', 'product-category', 'Common/PlannedPage/index', 'ProductCategoryConfig', 'basic:productCategory:list')",
                "(2441, 2090, 'C', 'product-attribute', 'Product/Attribute/index', 'ProductAttributeConfig', 'product:attribute:list')",
                "(2441, 2090, 'C', 'product-attribute', 'Common/PlannedPage/index', 'ProductAttributeConfig', 'basic:productAttribute:list')",
                "(2470, 2440, 'F', '#', '', '', 'product:category:query')",
                "(2480, 2441, 'F', '#', '', '', 'product:categoryAttribute:preview')",
                "call assert_product_category_attribute_sys_menu_guard();",
                "call assert_product_category_attribute_seed_completed();",
                "start transaction;",
                "commit;",
                "on duplicate key update",
                "drop temporary table if exists tmp_product_category_attribute_seed_expected",
                "drop temporary table if exists tmp_product_category_attribute_sys_menu_guard",
                "drop procedure if exists assert_product_category_attribute_seed_completed;",
                "drop procedure if exists assert_product_category_attribute_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_category_attribute_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_category_attribute_sys_menu_guard();", "update sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "insert into sys_dict_type");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_category_attribute_sys_menu_guard();", "update sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_category_attribute_seed_completed();", "commit;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "commit;", "drop procedure if exists assert_product_category_attribute_seed_completed;");

        if (!violations.isEmpty())
        {
            fail("product category/attribute menu seed must guard sys_menu slots before upsert:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void businessMenuSeedMustNotOverwriteSpecializedMenusAndMustGuardSysMenuSlots() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/business_menu_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_business_menu_sys_menu_guard",
                "tmp_business_menu_sys_menu_guard",
                "business menu parent sys_menu entries are required before business menu seed",
                "menu_id = 2050",
                "menu_id = 2100",
                "parent_id  bigint       not null",
                "menu_type  char(1)      not null",
                "business sys_menu id slot is occupied by another menu",
                "business sys_menu signature is already used by another menu",
                "(2400, 2060, 'C', 'list', 'Product/SourceProductLibrary/index', 'SourceProductLibrary', 'integration:upstream:query')",
                "(2400, 2060, 'C', 'list', 'Product/SourceProductLibrary/index', 'SourceProductLibrary', 'product:list:list')",
                "(2400, 2060, 'C', 'list', 'Common/PlannedPage/index', 'ProductList', 'product:list:list')",
                "(2452, 2100, 'C', 'inventory-adjustment', 'Inventory/AdjustmentReview/index', 'InventoryAdjustmentReview', 'review:inventoryAdjustment:list')",
                "call assert_business_menu_sys_menu_guard();",
                "create procedure assert_business_menu_seed_completed",
                "tmp_business_menu_seed_expected",
                "business menu seed completion mismatch",
                "call assert_business_menu_seed_completed();",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_business_menu_sys_menu_guard",
                "drop temporary table if exists tmp_business_menu_seed_expected",
                "drop procedure if exists assert_business_menu_seed_completed",
                "drop procedure if exists assert_business_menu_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        for (String forbidden : new String[] {
                "(2402, '商城商品列表'",
                "product:distribution:list",
                "(2412, '售后管理'",
                "order:afterSale:list",
                "(2420, '库存总览'",
                "inventory:overview:list",
                "(2421, '来源仓库库存'",
                "inventory:sourceWarehouse:list",
                "(2440, '商品分类配置'",
                "(2441, '商品属性配置'",
                "basic:productCategory:list",
                "basic:productAttribute:list",
                "Keep the source warehouse stock entry as a placeholder",
                "where menu_id = 2421"
        })
        {
            requireNotContains(violations, sqlFile.getFileName().toString(), source.replace("\r\n", "\n"), forbidden);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_business_menu_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_business_menu_sys_menu_guard();", "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_business_menu_seed_completed();", "commit;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "commit;", "drop temporary table if exists tmp_business_menu_sys_menu_guard");

        if (!violations.isEmpty())
        {
            fail("business menu seed must not overwrite specialized menu owners and must guard sys_menu slots:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sourceProductLibraryMenuComponentMigrationMustGuardMenu2400BeforeUpdate() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260605_source_product_library_menu_component.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);",
                "@source_product_library_menu_component_expected_count",
                "@source_product_library_menu_component_expected_signature",
                "create procedure assert_source_product_library_menu_component_guard",
                "create procedure assert_source_product_library_menu_component_targets",
                "tmp_source_product_library_menu_component_guard",
                "source product library menu component sys_menu id slot is occupied by another menu",
                "source product library menu component sys_menu signature is already used by another menu",
                "set @source_product_library_menu_component_expected_count after previewing exact source product library sys_menu rows",
                "set @source_product_library_menu_component_expected_signature after previewing exact source product library sys_menu rows",
                "source product library sys_menu exact target count mismatch",
                "source product library sys_menu exact target signature mismatch",
                "sha2(coalesce(group_concat(distinct",
                "(2400, 'list', 'Product/SourceProductLibrary/index'",
                "'SourceProductLibrary', 'integration:upstream:query', 'C')",
                "(2400, 'list', 'Product/SourceProductLibrary/index'",
                "'SourceProductLibrary', 'product:list:list', 'C')",
                "(2400, 'list', 'Common/PlannedPage/index'",
                "'ProductList', 'product:list:list', 'C')",
                "call assert_source_product_library_menu_component_guard();",
                "call assert_source_product_library_menu_component_targets();",
                "drop temporary table if exists tmp_source_product_library_menu_component_guard",
                "drop procedure if exists assert_source_product_library_menu_component_targets",
                "drop procedure if exists assert_source_product_library_menu_component_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_product_library_menu_component_guard();", "update sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_product_library_menu_component_guard();",
                "call assert_source_product_library_menu_component_targets();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_product_library_menu_component_targets();", "update sys_menu");

        if (!violations.isEmpty())
        {
            fail("source product library menu component migration must guard menu 2400 before update:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void mallProductDistributionMenuSeedMustOwnAndGuardDistributionMenu() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260605_mall_product_distribution_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@mall_product_distribution_dict_seed_expected_count",
                "@mall_product_distribution_dict_seed_expected_signature",
                "@mall_product_distribution_disabled_status_expected_count",
                "@mall_product_distribution_disabled_status_expected_signature",
                "@mall_product_distribution_menu_seed_expected_count",
                "@mall_product_distribution_menu_seed_expected_signature",
                "create procedure assert_mall_product_distribution_count_signature",
                "create procedure assert_mall_product_distribution_seed_completed",
                "tmp_mall_product_distribution_write_targets",
                "tmp_mall_product_distribution_seed_expected",
                "mall product distribution dict seed exact target count mismatch",
                "mall product distribution DISABLED exact target count mismatch",
                "mall product distribution menu seed exact target count mismatch",
                "mall product distribution dict type seed completion mismatch",
                "mall product distribution dict data seed completion mismatch",
                "mall product distribution disabled status completion mismatch",
                "mall product distribution sys_menu seed completion mismatch",
                "create procedure assert_mall_product_distribution_sys_menu_guard",
                "tmp_mall_product_distribution_sys_menu_guard",
                "mall product distribution sys_menu id slot is occupied by another menu",
                "mall product distribution sys_menu signature is already used by another menu",
                "and m.parent_id = seed.parent_id",
                "and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')",
                "insert into tmp_mall_product_distribution_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(2402, 2060, 'C', 'distribution', 'Product/Distribution/index', 'DistributionProduct', 'product:distribution:list')",
                "(2481, 2402, 'F', '#', '', '', 'product:distribution:query')",
                "(2482, 2402, 'F', '#', '', '', 'product:distribution:add')",
                "(2483, 2402, 'F', '#', '', '', 'product:distribution:edit')",
                "(2484, 2402, 'F', '#', '', '', 'product:distribution:status')",
                "(2485, 2402, 'F', '#', '', '', 'product:distribution:price')",
                "(2486, 2402, 'F', '#', '', '', 'product:distribution:log')",
                "(2488, 2402, 'F', '#', '', '', 'product:distribution:remove')",
                "call assert_mall_product_distribution_sys_menu_guard();",
                "call assert_mall_product_distribution_count_signature(",
                "(2402, '商城商品列表', 2060, 15, 'distribution', 'Product/Distribution/index'",
                "product:distribution:list",
                "union all select 2485, '商城商品调价', 5, 'product:distribution:price'",
                "union all select 2486, '商城商品操作日志', 6, 'product:distribution:log'",
                "union all select 2488, '商城商品删除', 7, 'product:distribution:remove'",
                "call assert_mall_product_distribution_seed_completed();",
                "on duplicate key update",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_mall_product_distribution_write_targets",
                "drop temporary table if exists tmp_mall_product_distribution_seed_expected",
                "drop temporary table if exists tmp_mall_product_distribution_sys_menu_guard",
                "drop procedure if exists assert_mall_product_distribution_seed_completed;",
                "drop procedure if exists assert_mall_product_distribution_sys_menu_guard",
                "drop procedure if exists assert_mall_product_distribution_count_signature"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_mall_product_distribution_count_signature(",
                "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "insert into sys_dict_type");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_mall_product_distribution_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into sys_menu", "commit;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_mall_product_distribution_seed_completed();", "commit;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "commit;", "drop procedure if exists assert_mall_product_distribution_seed_completed;");

        if (!violations.isEmpty())
        {
            fail("mall product distribution seed must own and guard menu 2402:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void orderAfterSaleMenuSeedMustOwnAndGuardAfterSaleMenu() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260605_order_after_sale_menu_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);",
                "@order_after_sale_menu_seed_expected_count",
                "@order_after_sale_menu_seed_expected_signature",
                "create procedure assert_order_after_sale_sys_menu_guard",
                "create procedure assert_order_after_sale_menu_seed_targets",
                "tmp_order_after_sale_sys_menu_guard",
                "order after-sale sys_menu id slot is occupied by another menu",
                "order after-sale sys_menu signature is already used by another menu",
                "set @order_after_sale_menu_seed_expected_count after previewing exact order after-sale sys_menu rows",
                "set @order_after_sale_menu_seed_expected_signature after previewing exact order after-sale sys_menu rows",
                "order after-sale sys_menu exact target count mismatch",
                "order after-sale sys_menu exact target signature mismatch",
                "sha2(coalesce(group_concat(distinct",
                "and m.parent_id = seed.parent_id",
                "and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')",
                "insert into tmp_order_after_sale_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(2412, 2070, 'C', 'after-sale', 'Common/PlannedPage/index', 'AfterSaleManagement', 'order:afterSale:list')",
                "call assert_order_after_sale_sys_menu_guard();",
                "call assert_order_after_sale_menu_seed_targets();",
                "(2412, '售后管理', 2070, 15, 'after-sale', 'Common/PlannedPage/index'",
                "order:afterSale:list",
                "drop temporary table if exists tmp_order_after_sale_sys_menu_guard",
                "drop procedure if exists assert_order_after_sale_menu_seed_targets",
                "drop procedure if exists assert_order_after_sale_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_order_after_sale_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_order_after_sale_sys_menu_guard();",
                "call assert_order_after_sale_menu_seed_targets();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_order_after_sale_menu_seed_targets();", "insert into sys_menu");

        if (!violations.isEmpty())
        {
            fail("order after-sale menu seed must own and guard menu 2412:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sourceWarehouseStockMenuSeedMustOwnAndGuardSourceWarehouseStockMenu() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260606_source_warehouse_stock_menu_rename.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);",
                "@source_warehouse_stock_menu_rename_expected_count",
                "@source_warehouse_stock_menu_rename_expected_signature",
                "create procedure assert_source_warehouse_stock_sys_menu_guard",
                "create procedure assert_source_warehouse_stock_menu_rename_targets",
                "tmp_source_warehouse_stock_sys_menu_guard",
                "source warehouse stock sys_menu id slot is occupied by another menu",
                "source warehouse stock sys_menu signature is already used by another menu",
                "set @source_warehouse_stock_menu_rename_expected_count after previewing exact source warehouse stock sys_menu rows",
                "set @source_warehouse_stock_menu_rename_expected_signature after previewing exact source warehouse stock sys_menu rows",
                "source warehouse stock sys_menu exact target count mismatch",
                "source warehouse stock sys_menu exact target signature mismatch",
                "sha2(coalesce(group_concat(distinct",
                "and m.parent_id = seed.parent_id",
                "and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')",
                "insert into tmp_source_warehouse_stock_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(2421, 2080, 'C', 'source-warehouse-stock', 'Inventory/SourceWarehouseStock/index', 'SourceWarehouseStock', 'inventory:sourceWarehouse:list')",
                "(2421, 2080, 'C', 'source-warehouse-stock', 'Common/PlannedPage/index', 'SourceWarehouseStock', 'inventory:sourceWarehouse:list')",
                "call assert_source_warehouse_stock_sys_menu_guard();",
                "call assert_source_warehouse_stock_menu_rename_targets();",
                "update sys_menu",
                "where menu_id = 2421",
                "select 2421, '来源仓库库存', 2080, 10, 'source-warehouse-stock', 'Inventory/SourceWarehouseStock/index'",
                "where not exists (select 1 from sys_menu where menu_id = 2421)",
                "drop temporary table if exists tmp_source_warehouse_stock_sys_menu_guard",
                "drop procedure if exists assert_source_warehouse_stock_menu_rename_targets",
                "drop procedure if exists assert_source_warehouse_stock_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_warehouse_stock_sys_menu_guard();", "update sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_warehouse_stock_sys_menu_guard();",
                "call assert_source_warehouse_stock_menu_rename_targets();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_warehouse_stock_menu_rename_targets();", "update sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "update sys_menu", "insert into sys_menu");

        if (!violations.isEmpty())
        {
            fail("source warehouse stock menu seed must own and guard menu 2421:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamSystemManagementMenuSeedMustGuardSysMenuSlotsBeforeUpsert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/upstream_system_management_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_upstream_system_management_sys_menu_guard",
                "tmp_upstream_system_management_sys_menu_guard",
                "upstream system management parent sys_menu 2030 is required before upstream system management seed",
                "menu_id = 2030",
                "parent_id  bigint       not null",
                "menu_type  char(1)      not null",
                "upstream system management sys_menu id slot is occupied by another menu",
                "upstream system management sys_menu signature is already used by another menu",
                "(2031, 2030, 'C', 'upstream-system', 'UpstreamSystem/index', 'UpstreamSystem', 'integration:upstream:list')",
                "(2300, 2031, 'F', '#', '', '', 'integration:upstream:query')",
                "(2301, 2031, 'F', '#', '', '', 'integration:upstream:add')",
                "(2302, 2031, 'F', '#', '', '', 'integration:upstream:edit')",
                "(2303, 2031, 'F', '#', '', '', 'integration:upstream:credential')",
                "(2304, 2031, 'F', '#', '', '', 'integration:upstream:sync')",
                "(2305, 2031, 'F', '#', '', '', 'integration:upstream:pair')",
                "(2306, 2031, 'F', '#', '', '', 'integration:upstream:log')",
                "(2307, 2031, 'F', '#', '', '', 'integration:upstream:dimensionSync')",
                "(2308, 2031, 'F', '#', '', '', 'integration:upstream:inventoryQuery')",
                "(2309, 2031, 'F', '#', '', '', 'integration:upstream:inventorySync')",
                "call assert_upstream_system_management_sys_menu_guard();",
                "create procedure assert_upstream_system_management_seed_completed",
                "call assert_upstream_system_management_seed_completed();",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_upstream_system_management_sys_menu_guard",
                "drop procedure if exists assert_upstream_system_management_seed_completed",
                "drop procedure if exists assert_upstream_system_management_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_upstream_system_management_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_upstream_system_management_sys_menu_guard();", "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "insert into sys_dict_type");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_upstream_system_management_seed_completed();", "commit;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "commit;", "drop procedure if exists assert_upstream_system_management_seed_completed;");

        if (!violations.isEmpty())
        {
            fail("upstream system management menu seed must guard sys_menu slots before upsert:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void partnerRootMenu2010MustStayTopOwnedWithGuardedCompatibilitySeeds() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path topMenuSeed = backendRoot.resolve("sql/top_menu_seed.sql");
        Path sellerBuyerSeed = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        Path directLoginSeed = backendRoot.resolve("sql/20260606_admin_partner_page_direct_login_seed.sql");
        String topMenuSource = Files.readString(topMenuSeed, StandardCharsets.UTF_8);
        String sellerBuyerSource = Files.readString(sellerBuyerSeed, StandardCharsets.UTF_8);
        String directLoginSource = Files.readString(directLoginSeed, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        requireContains(violations, topMenuSeed.getFileName().toString(), topMenuSource,
                "(2010, 0, 'M', 'partner', '', 'PartnerManagement', '')");
        requireContains(violations, topMenuSeed.getFileName().toString(), topMenuSource,
                "(2010, '主体管理', 0, 5, 'partner', null, '', 'PartnerManagement'");
        assertAppearsBefore(violations, topMenuSeed.getFileName().toString(), topMenuSource,
                "call assert_top_menu_sys_menu_guard();", "insert into sys_menu");

        requireContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "create procedure assert_seller_buyer_sys_menu_seed_guard");
        requireContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "seller/buyer seed requires top_menu_seed partner root 2010");
        requireContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "m.menu_id = 2010");
        requireContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "coalesce(m.parent_id, -1) = 0");
        requireContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "coalesce(m.menu_type, '') = 'M'");
        requireContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "insert into tmp_seller_buyer_sys_menu_guard (menu_id, parent_id, menu_type, path, component, route_name, perms)");
        requireContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "(2011, 2010, 'C', 'seller', 'Seller/index', 'Seller', 'seller:admin:list')");
        requireContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "(2205, 2011, 'F', '#', '', '', 'seller:admin:directLogin')");
        requireNotContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "(2010, '主体管理', 0, 5, 'partner', null, '', 'PartnerManagement'");
        requireNotContains(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "(2010, 'partner', '', 'PartnerManagement', '')");
        assertAppearsBefore(violations, sellerBuyerSeed.getFileName().toString(), sellerBuyerSource,
                "call assert_seller_buyer_sys_menu_seed_guard();", "insert into sys_menu");

        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "create procedure assert_partner_root_menu_exists");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "admin direct-login seed requires top_menu_seed partner root 2010");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_partner_root_menu_exists();");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_sys_menu_slot(2011, 2010, 'C', 'seller', 'Seller/index', 'Seller', 'seller:admin:list'");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_sys_menu_slot(2205, 2011, 'F', '#', '', '', 'seller:admin:directLogin'");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "create procedure assert_admin_partner_page_direct_login_seed_completed");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "admin partner page direct-login seed completion mismatch");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_admin_partner_page_direct_login_seed_completed();");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "start transaction;");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "commit;");
        requireContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "drop procedure if exists assert_admin_partner_page_direct_login_seed_completed");
        requireNotContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "(2010, '主体管理', 0, 5, 'partner', null, '', 'PartnerManagement'");
        requireNotContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_sys_menu_slot(2010");
        requireNotContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_sys_menu_signature_available(2010");
        assertAppearsBefore(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_partner_root_menu_exists();", "insert into sys_menu");
        assertAppearsBefore(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_sys_menu_slot(2205, 2011, 'F', '#', '', '', 'seller:admin:directLogin'",
                "start transaction;");
        assertAppearsBefore(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "start transaction;", "insert into sys_menu");
        assertAppearsBefore(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_admin_partner_page_direct_login_seed_completed();", "commit;");

        if (!violations.isEmpty())
        {
            fail("partner root menu 2010 must remain top-owned and dependent seeds must only assert it:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void topMenuSeedMustGuardSysMenuSlotsBeforeUpsert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/top_menu_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_top_menu_sys_menu_guard",
                "tmp_top_menu_sys_menu_guard",
                "top menu sys_menu id slot is occupied by another menu",
                "top menu sys_menu signature is already used by another menu",
                "coalesce(m.parent_id, -1) = seed.parent_id",
                "coalesce(m.menu_type, '') = seed.menu_type",
                "insert into tmp_top_menu_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(1, 0, 'M', 'system', '', '', '')",
                "(2, 0, 'M', 'monitor', '', '', '')",
                "(3, 0, 'M', 'tool', '', '', '')",
                "(108, 0, 'M', 'log-center', '', 'LogCenter', '')",
                "(108, 0, 'M', 'log', '', '', '')",
                "(2010, 0, 'M', 'partner', '', 'PartnerManagement', '')",
                "(2100, 0, 'M', 'review-center', '', 'ReviewCenter', '')",
                "call assert_top_menu_sys_menu_guard();",
                "create procedure assert_top_menu_seed_completed",
                "tmp_top_menu_seed_expected",
                "top menu seed completion mismatch",
                "call assert_top_menu_seed_completed();",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_top_menu_sys_menu_guard",
                "drop temporary table if exists tmp_top_menu_seed_expected",
                "drop procedure if exists assert_top_menu_seed_completed",
                "drop procedure if exists assert_top_menu_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_top_menu_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_top_menu_sys_menu_guard();", "update sys_menu\nset order_num = 90");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_top_menu_sys_menu_guard();", "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "update sys_menu\nset order_num = 90");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_top_menu_seed_completed();", "commit;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "commit;", "drop temporary table if exists tmp_top_menu_sys_menu_guard");

        if (!violations.isEmpty())
        {
            fail("top menu seed must guard reused RuoYi sys_menu slots before upsert/update:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void topMenuSeedLegacyCleanupMustFailClosedBeforeUpdatingLegacyMenus() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/top_menu_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_top_menu_legacy_cleanup_guard",
                "tmp_top_menu_legacy_cleanup_guard",
                "top menu legacy cleanup sys_menu id slot is occupied by another menu",
                "top menu legacy cleanup sys_menu signature is already used by another menu",
                "(2040, 0, '渠道管理', 'urili-channel', '', '', '', 'M')",
                "(2040, 0, '渠道管理', 'channel', '', '', '', 'M')",
                "(2000, 0, 'URILI运营后台', null, null, null, '', 'M')",
                "call assert_top_menu_legacy_cleanup_guard();",
                "drop temporary table if exists tmp_top_menu_legacy_cleanup_guard",
                "drop procedure if exists assert_top_menu_legacy_cleanup_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_top_menu_legacy_cleanup_guard();", "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_top_menu_legacy_cleanup_guard();", "where menu_id = 2040;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_top_menu_legacy_cleanup_guard();", "where menu_id = 2000;");

        if (!violations.isEmpty())
        {
            fail("top menu seed legacy cleanup must guard retired sys_menu slots before updates:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void overseasChannelCarrierMenuLegacyCleanupMustUseExactTargetSignature() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260608_overseas_channel_carrier_menu_restructure.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@overseas_channel_legacy_role_menu_expected_delete_count",
                "@overseas_channel_legacy_menu_expected_delete_count",
                "@overseas_channel_restructure_menu_expected_target_count",
                "@overseas_channel_legacy_role_menu_expected_signature",
                "@overseas_channel_legacy_menu_expected_signature",
                "@overseas_channel_restructure_menu_expected_target_signature",
                "set @overseas_channel_legacy_role_menu_expected_signature after previewing exact sys_role_menu rows",
                "set @overseas_channel_legacy_menu_expected_signature after previewing exact sys_menu rows",
                "set @overseas_channel_restructure_menu_expected_target_signature after previewing exact sys_menu restructure rows",
                "create procedure assert_legacy_channel_cleanup_targets",
                "create procedure assert_overseas_channel_restructure_targets",
                "create procedure assert_overseas_channel_restructure_completed",
                "concat_ws(':', rm.role_id, rm.menu_id)",
                "m.menu_id in (2031, 2041, 2042, 2054)",
                "overseas channel legacy role-menu exact target signature mismatch",
                "overseas channel legacy menu exact target signature mismatch",
                "overseas channel restructure menu exact target signature mismatch",
                "call assert_legacy_channel_cleanup_targets();",
                "call assert_overseas_channel_restructure_targets();",
                "call assert_overseas_channel_restructure_completed();",
                "start transaction;",
                "commit;",
                "delete rm\nfrom sys_role_menu rm\nwhere rm.menu_id = 2040;",
                "delete m\nfrom sys_menu m\nwhere m.menu_id = 2040",
                "and m.menu_name = '渠道管理'",
                "drop procedure if exists assert_legacy_channel_cleanup_targets"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        requireNotContains(violations, sqlFile.getFileName().toString(), source,
                "delete from sys_role_menu\nwhere menu_id = 2040;");
        requireNotContains(violations, sqlFile.getFileName().toString(), source,
                "delete from sys_menu\nwhere menu_id = 2040;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_legacy_channel_cleanup_targets();",
                "delete rm\nfrom sys_role_menu rm\nwhere rm.menu_id = 2040;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_overseas_channel_restructure_targets();",
                "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;",
                "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;",
                "delete rm\nfrom sys_role_menu rm\nwhere rm.menu_id = 2040;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_legacy_channel_cleanup_targets();",
                "delete m\nfrom sys_menu m\nwhere m.menu_id = 2040");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "delete m\nfrom sys_menu m\nwhere m.menu_id = 2040",
                "commit;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_overseas_channel_restructure_completed();",
                "commit;");

        if (!violations.isEmpty())
        {
            fail("overseas channel/carrier legacy menu cleanup must require exact previewed delete targets:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void productCenterMenuSeedMustFailClosedForParentAndSlots() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260608_product_center_menu_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_product_center_parent_menu_ready",
                "create procedure assert_product_center_sys_menu_guard",
                "create procedure assert_product_center_menu_seed_targets",
                "create procedure assert_product_center_menu_seed_completed",
                "@product_center_menu_seed_expected_count",
                "@product_center_menu_seed_expected_signature",
                "set @product_center_menu_seed_expected_count after previewing exact product center sys_menu rows",
                "set @product_center_menu_seed_expected_signature after previewing exact product center sys_menu rows",
                "tmp_product_center_sys_menu_guard",
                "sha2(coalesce(group_concat(distinct",
                "product management parent menu 2060 signature does not match expected",
                "product center sys_menu id slot is occupied by another menu",
                "product center sys_menu signature is already used by another menu",
                "product center sys_menu exact target count mismatch",
                "product center sys_menu exact target signature mismatch",
                "product center sys_menu seed completion mismatch",
                "(2404, 2060, 'C', 'center', 'Product/ProductCenter/index', 'ProductCenter', 'product:center:list')",
                "(2487, 2404, 'F', '#', '', '', 'product:center:query')",
                "call assert_product_center_parent_menu_ready();",
                "call assert_product_center_sys_menu_guard();",
                "call assert_product_center_menu_seed_targets();",
                "call assert_product_center_menu_seed_completed();",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_product_center_sys_menu_guard",
                "drop procedure if exists assert_product_center_menu_seed_completed",
                "drop procedure if exists assert_product_center_menu_seed_targets",
                "drop procedure if exists assert_product_center_sys_menu_guard",
                "drop procedure if exists assert_product_center_parent_menu_ready"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "insert into tmp_product_center_sys_menu_guard", "call assert_product_center_parent_menu_ready();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_product_center_parent_menu_ready();", "call assert_product_center_sys_menu_guard();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_product_center_sys_menu_guard();", "call assert_product_center_menu_seed_targets();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_product_center_menu_seed_targets();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_product_center_menu_seed_targets();", "insert into sys_menu");
        assertAppearsBefore(violations, fileName, source,
                "call assert_product_center_menu_seed_targets();",
                "select 2487, '商品中心查询', 2404");
        assertAppearsBefore(violations, fileName, source,
                "call assert_product_center_menu_seed_completed();", "commit;");
        assertAppearsBefore(violations, fileName, source,
                "commit;", "drop temporary table if exists tmp_product_center_sys_menu_guard");

        if (!violations.isEmpty())
        {
            fail("product center menu seed must fail closed before inserting page or button menus:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void productReviewMenuSeedMustFailClosedForParentAndButtonSlots() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260608_product_review.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_product_review_sys_menu_guard",
                "tmp_product_review_sys_menu_guard",
                "product review page menu 2451 must exist with expected signature before product review migration",
                "product review sys_menu id slot is occupied by another menu",
                "product review sys_menu permission is already used by another menu",
                "create procedure assert_product_review_schema_ready",
                "create procedure assert_product_review_seed_targets",
                "create procedure assert_product_review_seed_target_signatures",
                "create procedure assert_product_review_seed_completed",
                "@product_review_menu_expected_count",
                "@product_review_menu_expected_signature",
                "@product_review_dict_type_expected_count",
                "@product_review_dict_type_expected_signature",
                "@product_review_dict_data_expected_count",
                "@product_review_dict_data_expected_signature",
                "set @product_review_menu_expected_count after previewing exact product review sys_menu rows",
                "set @product_review_menu_expected_signature after previewing exact product review sys_menu rows",
                "set @product_review_dict_type_expected_count after previewing exact product review sys_dict_type rows",
                "set @product_review_dict_type_expected_signature after previewing exact product review sys_dict_type rows",
                "set @product_review_dict_data_expected_count after previewing exact product review sys_dict_data rows",
                "set @product_review_dict_data_expected_signature after previewing exact product review sys_dict_data rows",
                "tmp_product_review_dict_type_seed",
                "tmp_product_review_dict_data_seed",
                "tmp_product_review_column_contract",
                "tmp_product_review_index_contract",
                "sha2(coalesce(group_concat(distinct",
                "ordinal_position",
                "column_type",
                "column_default",
                "from information_schema.columns",
                "from information_schema.statistics",
                "product_review schema column contract mismatch",
                "product_review schema index contract mismatch",
                "product review dict type target is occupied by incompatible row",
                "product review dict data sort slot is occupied by incompatible row",
                "product review dict data seed completion mismatch",
                "product review sys_menu exact target count mismatch",
                "product review sys_menu exact target signature mismatch",
                "product review sys_dict_type exact target count mismatch",
                "product review sys_dict_type exact target signature mismatch",
                "product review sys_dict_data exact target count mismatch",
                "product review sys_dict_data exact target signature mismatch",
                "product review sys_menu seed completion mismatch",
                "coalesce(m.component, '') in (coalesce(seed.component, ''), 'Common/PlannedPage/index')",
                "m.menu_id <> seed.menu_id",
                "(2451, 2100, 'C', 'product-distribution', 'Product/Review/index', 'ProductDistributionReview', 'review:productDistribution:list')",
                "(2491, 2451, 'F', '#', '', '', 'review:productDistribution:query')",
                "(2492, 2451, 'F', '#', '', '', 'review:productDistribution:approve')",
                "(2493, 2451, 'F', '#', '', '', 'review:productDistribution:reject')",
                "(2494, 2451, 'F', '#', '', '', 'review:productDistribution:log')",
                "('product_review_request', 'review_no', 2, 'varchar(64)', 'NO', null, '')",
                "('product_review_operation_log', 'operation_time', 10, 'datetime', 'NO', 'current_timestamp', '')",
                "('product_review_request', 'uk_product_review_no', 0, 1, 'review_no')",
                "('product_review_request', 'idx_product_review_pending_key', 1, 1, 'active_pending_key')",
                "call assert_product_review_sys_menu_guard();",
                "call assert_product_review_seed_targets();",
                "call assert_product_review_seed_target_signatures();",
                "call assert_product_review_schema_ready();",
                "call assert_product_review_seed_completed();",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_product_review_dict_data_seed",
                "drop temporary table if exists tmp_product_review_dict_type_seed",
                "drop temporary table if exists tmp_product_review_index_contract",
                "drop temporary table if exists tmp_product_review_column_contract",
                "drop temporary table if exists tmp_product_review_sys_menu_guard",
                "drop procedure if exists assert_product_review_seed_completed",
                "drop procedure if exists assert_product_review_seed_target_signatures",
                "drop procedure if exists assert_product_review_seed_targets",
                "drop procedure if exists assert_product_review_schema_ready",
                "drop procedure if exists assert_product_review_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_sys_menu_guard();", "update sys_menu\nset menu_name = '商品审核'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_sys_menu_guard();", "select seed.menu_id, seed.menu_name, 2451");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_sys_menu_guard();", "create table if not exists product_review_request");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_sys_menu_guard();", "insert into sys_dict_type");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_seed_targets();", "create table if not exists product_review_request");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_seed_targets();",
                "create temporary table if not exists tmp_product_review_column_contract");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_seed_targets();",
                "call assert_product_review_seed_target_signatures();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_seed_target_signatures();",
                "create table if not exists product_review_request");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_seed_target_signatures();",
                "insert into sys_dict_type");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into tmp_product_review_column_contract", "create table if not exists product_review_request");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into tmp_product_review_index_contract", "create table if not exists product_review_request");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_schema_ready();", "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "insert into sys_dict_type");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_review_seed_completed();", "commit;");

        if (!violations.isEmpty())
        {
            fail("product review menu seed must fail closed before updating parent or inserting buttons:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryAutoWmsStockSyncPolicyMustUseExactTargetsAndSchemaContracts() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260609_inventory_auto_wms_stock_sync_policy.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@inventory_auto_wms_menu_expected_count",
                "@inventory_auto_wms_menu_expected_signature",
                "@inventory_auto_wms_dict_type_expected_count",
                "@inventory_auto_wms_dict_type_expected_signature",
                "@inventory_auto_wms_dict_data_expected_count",
                "@inventory_auto_wms_dict_data_expected_signature",
                "set @inventory_auto_wms_menu_expected_count after previewing exact inventory auto wms sys_menu rows",
                "set @inventory_auto_wms_dict_type_expected_signature after previewing exact inventory auto wms sys_dict_type rows",
                "set @inventory_auto_wms_dict_data_expected_signature after previewing exact inventory auto wms sys_dict_data rows",
                "create procedure assert_inventory_auto_wms_seed_targets",
                "create procedure assert_inventory_auto_wms_schema_ready",
                "create procedure assert_inventory_auto_wms_stock_sync_policy_completed",
                "tmp_inventory_auto_wms_sys_menu_seed",
                "tmp_inventory_auto_wms_dict_type_seed",
                "tmp_inventory_auto_wms_dict_data_seed",
                "tmp_inventory_auto_wms_column_contract",
                "tmp_inventory_auto_wms_index_contract",
                "sha2(coalesce(group_concat(distinct",
                "inventory auto wms sys_menu exact target count mismatch",
                "inventory auto wms sys_dict_type exact target signature mismatch",
                "inventory auto wms sys_dict_data exact target signature mismatch",
                "inventory auto wms schema column contract mismatch",
                "inventory auto wms schema index contract mismatch",
                "inventory auto wms sys_menu seed completion mismatch",
                "inventory auto wms dict type seed completion mismatch",
                "inventory auto wms dict data seed completion mismatch",
                "(242005, '库存同步方式设置', 2420, 5, 'F', '#', '', '', 'inventory:overview:syncPolicy', '#'",
                "('库存同步方式', 'inventory_stock_sync_mode')",
                "(1, '手动设置平台库存', 'MANUAL', 'inventory_stock_sync_mode', 'default', 'N')",
                "(9, '自动同步WMS库存', 'AUTO_SOURCE_SYNC', 'inventory_operation_type', '', 'N')",
                "('inventory_stock_sync_policy', 'policy_key', 'varchar(200)', 'NO', null)",
                "('inventory_sku_warehouse_stock', 'sync_mode', 'varchar(32)', 'NO', 'MANUAL')",
                "('inventory_overview_spu_read_model', 'sync_policy_scope_summary', 'varchar(32)', 'NO', 'SYSTEM')",
                "('inventory_stock_sync_policy', 'uk_inventory_stock_sync_policy_key', 0, 1, 'policy_key')",
                "('inventory_sku_warehouse_stock', 'idx_inventory_stock_sync_mode', 1, 2, 'sync_policy_scope')",
                "call assert_inventory_auto_wms_seed_targets();",
                "call assert_inventory_auto_wms_schema_ready();",
                "call assert_inventory_auto_wms_stock_sync_policy_completed();",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_inventory_auto_wms_index_contract",
                "drop procedure if exists assert_inventory_auto_wms_stock_sync_policy_completed",
                "drop procedure if exists assert_inventory_auto_wms_schema_ready",
                "drop procedure if exists assert_inventory_auto_wms_seed_targets"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "call assert_inventory_auto_wms_menu_guard();",
                "create temporary table if not exists tmp_inventory_auto_wms_sys_menu_seed");
        assertAppearsBefore(violations, fileName, source,
                "call assert_inventory_auto_wms_seed_targets();",
                "create table if not exists inventory_stock_sync_policy");
        assertAppearsBefore(violations, fileName, source,
                "insert into tmp_inventory_auto_wms_column_contract",
                "create table if not exists inventory_stock_sync_policy");
        assertAppearsBefore(violations, fileName, source,
                "insert into tmp_inventory_auto_wms_index_contract",
                "create table if not exists inventory_stock_sync_policy");
        assertAppearsBefore(violations, fileName, source,
                "call add_index_if_missing('inventory_sku_warehouse_stock', 'idx_inventory_stock_sync_policy'",
                "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_inventory_auto_wms_schema_ready();",
                "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into sys_dict_type");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into sys_menu");
        assertAppearsBefore(violations, fileName, source,
                "call assert_inventory_auto_wms_stock_sync_policy_completed();", "commit;");
        assertAppearsBefore(violations, fileName, source,
                "commit;", "drop temporary table if exists tmp_inventory_auto_wms_index_contract;");

        if (!violations.isEmpty())
        {
            fail("inventory auto WMS stock sync policy migration must fail closed on target and schema drift:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryAdjustmentReviewMustUseExactTargetsAndCompletionAssert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260609_inventory_adjustment_review.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@inventory_adjustment_review_menu_expected_count",
                "@inventory_adjustment_review_menu_expected_signature",
                "@inventory_adjustment_review_job_expected_count",
                "@inventory_adjustment_review_job_expected_signature",
                "create procedure assert_inventory_adjustment_review_target_signatures",
                "create procedure assert_inventory_adjustment_review_completed",
                "tmp_inventory_adjustment_review_sys_menu",
                "sha2(coalesce(group_concat(distinct",
                "inventory adjustment review sys_menu exact target count mismatch",
                "inventory adjustment review sys_job exact target signature mismatch",
                "inventory adjustment review sys_menu seed completion mismatch",
                "inventory adjustment review default policy completion mismatch",
                "inventory adjustment review default binding completion mismatch",
                "inventory adjustment review sys_job completion mismatch",
                "(2452, '库存调整审核', 2100, 15, 'C', 'inventory-adjustment'",
                "(2504, '库存调整审核驳回', 2452, 4, 'F', '#', '', '', 'review:inventoryAdjustment:reject'",
                "call assert_inventory_adjustment_review_parent_ready();",
                "call assert_inventory_adjustment_review_menu_slots();",
                "call assert_inventory_adjustment_review_target_signatures();",
                "call assert_inventory_adjustment_review_completed();",
                "start transaction;",
                "commit;",
                "drop procedure if exists assert_inventory_adjustment_review_completed"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "call assert_inventory_adjustment_review_menu_slots();",
                "call assert_inventory_adjustment_review_target_signatures();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_inventory_adjustment_review_target_signatures();",
                "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;",
                "insert into inventory_adjustment_review_policy");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "insert into sys_menu");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", "update sys_job");
        assertAppearsBefore(violations, fileName, source,
                "call assert_inventory_adjustment_review_completed();", "commit;");
        assertAppearsBefore(violations, fileName, source,
                "commit;", "drop procedure if exists assert_inventory_adjustment_review_completed;");

        if (!violations.isEmpty())
        {
            fail("inventory adjustment review migration must keep exact target and completion guards:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void currencyMenuSeedMustHaveSingleFinanceOwnerAndGuardSysMenuSlots() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path businessMenuSeed = backendRoot.resolve("sql/business_menu_seed.sql");
        Path currencySeed = backendRoot.resolve("sql/currency_configuration_seed.sql");
        String businessMenuSource = Files.readString(businessMenuSeed, StandardCharsets.UTF_8);
        String currencySource = Files.readString(currencySeed, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        requireNotContains(violations, businessMenuSeed.getFileName().toString(), businessMenuSource,
                "basic:currency:list");
        requireNotContains(violations, businessMenuSeed.getFileName().toString(), businessMenuSource,
                "(2442, '币种配置'");

        for (String expected : new String[] {
                "create procedure assert_currency_configuration_sys_menu_guard",
                "tmp_currency_configuration_sys_menu_guard",
                "currency configuration sys_menu id slot is occupied by another menu",
                "currency configuration sys_menu signature is already used by another menu",
                "and m.parent_id = seed.parent_id",
                "and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')",
                "insert into tmp_currency_configuration_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(2442, 2050, 'C', 'currency', 'Finance/Currency/index', 'FinanceCurrency', 'finance:currency:list')",
                "(2442, 2050, 'C', 'currency', 'Common/PlannedPage/index', 'CurrencyConfig', 'basic:currency:list')",
                "(2460, 2442, 'F', '#', '', '', 'finance:currency:query')",
                "(2466, 2442, 'F', '#', '', '', 'finance:currency:log')",
                "call assert_currency_configuration_sys_menu_guard();",
                "create procedure assert_currency_configuration_seed_completed",
                "call assert_currency_configuration_seed_completed();",
                "start transaction;",
                "commit;",
                "drop temporary table if exists tmp_currency_configuration_sys_menu_guard",
                "drop procedure if exists assert_currency_configuration_seed_completed",
                "drop procedure if exists assert_currency_configuration_sys_menu_guard"
        })
        {
            requireContains(violations, currencySeed.getFileName().toString(), currencySource, expected);
        }
        assertAppearsBefore(violations, currencySeed.getFileName().toString(), currencySource,
                "call assert_currency_configuration_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, currencySeed.getFileName().toString(), currencySource,
                "call assert_currency_configuration_sys_menu_guard();", "start transaction;");
        assertAppearsBefore(violations, currencySeed.getFileName().toString(), currencySource,
                "start transaction;", "insert into sys_dict_type");
        assertAppearsBefore(violations, currencySeed.getFileName().toString(), currencySource,
                "call assert_currency_configuration_seed_completed();", "commit;");
        assertAppearsBefore(violations, currencySeed.getFileName().toString(), currencySource,
                "commit;", "drop procedure if exists assert_currency_configuration_seed_completed;");

        if (!violations.isEmpty())
        {
            fail("currency menu seed must have a single finance owner and guard sys_menu slots before upsert:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void warehouseUsAddressSeedMustUseTransactionalDmlAndCompletionAssert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/warehouse_us_address_seed.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_warehouse_us_address_seed_completed",
                "v_state_count <> 51",
                "v_city_count <> 32058",
                "place_geoid = '0100100'",
                "place_geoid = '5686665'",
                "start transaction;",
                "call assert_warehouse_us_address_seed_completed();",
                "commit;",
                "drop procedure if exists assert_warehouse_us_address_seed_completed"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;", "insert into us_state");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into us_state", "insert into us_city");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_warehouse_us_address_seed_completed();", "commit;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "commit;", "drop procedure if exists assert_warehouse_us_address_seed_completed;");

        if (!violations.isEmpty())
        {
            fail("warehouse US address seed must wrap DML and verify completion before commit:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sourceProductLibrarySkuCandidateFieldsMustUseReplaySafeColumnHelper() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260604_source_product_library_sku_candidate_fields.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n");
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure add_column_if_missing",
                "create procedure assert_column_exists",
                "upstream_system_sku_candidate.master_product_name column is required before source product library field migration",
                "from information_schema.columns",
                "column_name = p_column",
                "call assert_column_exists('upstream_system_sku_candidate', 'master_product_name'",
                "call add_column_if_missing('upstream_system_sku_candidate', 'product_alias_name'",
                "call add_column_if_missing('upstream_system_sku_candidate', 'source_payload_hash'",
                "call create_index_if_missing('upstream_system_sku_candidate', 'idx_upstream_sku_candidate_main_code'",
                "drop procedure if exists add_column_if_missing",
                "drop procedure if exists assert_column_exists"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        requireNotContains(violations, sqlFile.getFileName().toString(), normalizedSource,
                "alter table upstream_system_sku_candidate\n  add column");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_product_library_sku_candidate_fields_confirmed();",
                "call assert_column_exists('upstream_system_sku_candidate', 'master_product_name'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_column_exists('upstream_system_sku_candidate', 'master_product_name'",
                "call add_column_if_missing('upstream_system_sku_candidate', 'product_alias_name'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call add_column_if_missing('upstream_system_sku_candidate', 'source_payload_hash'",
                "call create_index_if_missing('upstream_system_sku_candidate', 'idx_upstream_sku_candidate_main_code'");

        if (!violations.isEmpty())
        {
            fail("source product library SKU candidate field migration must be safe to replay:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void upstreamSyncStagingDiffMustUseReplaySafeColumnHelper() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260606_upstream_sync_staging_diff.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n");
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure add_column_if_missing",
                "create procedure assert_column_exists",
                "upstream_system_warehouse_candidate.status column is required before upstream sync staging diff migration",
                "upstream_system_logistics_channel_candidate.status column is required before upstream sync staging diff migration",
                "upstream_system_sku_candidate.source_payload_hash column is required before upstream sync staging diff migration",
                "call assert_column_exists('upstream_system_warehouse_candidate', 'status'",
                "call add_column_if_missing('upstream_system_warehouse_candidate', 'source_payload_json'",
                "call add_column_if_missing('upstream_system_warehouse_candidate', 'source_payload_hash'",
                "call assert_column_exists('upstream_system_logistics_channel_candidate', 'status'",
                "call add_column_if_missing('upstream_system_logistics_channel_candidate', 'source_payload_json'",
                "call add_column_if_missing('upstream_system_logistics_channel_candidate', 'source_payload_hash'",
                "call assert_column_exists('upstream_system_sku_candidate', 'source_payload_hash'",
                "call add_column_if_missing('upstream_system_sku_candidate', 'wms_payload_json'",
                "call add_column_if_missing('upstream_system_sku_candidate', 'wms_payload_hash'",
                "drop procedure if exists add_column_if_missing",
                "drop procedure if exists assert_column_exists"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        for (String forbidden : new String[] {
                "alter table upstream_system_warehouse_candidate add column",
                "alter table upstream_system_logistics_channel_candidate add column",
                "alter table upstream_system_sku_candidate add column"
        })
        {
            requireNotContains(violations, sqlFile.getFileName().toString(), normalizedSource, forbidden);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_upstream_sync_staging_diff_confirmed();",
                "call assert_column_exists('upstream_system_warehouse_candidate', 'status'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_column_exists('upstream_system_sku_candidate', 'source_payload_hash'",
                "call add_column_if_missing('upstream_system_sku_candidate', 'wms_payload_json'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call add_column_if_missing('upstream_system_sku_candidate', 'wms_payload_hash'",
                "create table if not exists upstream_system_sync_state");

        if (!violations.isEmpty())
        {
            fail("upstream sync staging diff migration must use replay-safe column helpers:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void integrationFreshBootstrapMustDeclareMandatoryPostSeedSqlChain() throws IOException
    {
        Path workspaceRoot = findWorkspaceRoot();
        Path backendRoot = workspaceRoot.resolve("RuoYi-Vue");
        Path manifestFile = workspaceRoot.resolve("docs/architecture/integration-bootstrap-required-sql.md");
        Path seedFile = backendRoot.resolve("sql/upstream_system_management_seed.sql");
        Path inventoryFile = backendRoot.resolve("sql/20260606_upstream_inventory_dimension_sync.sql");
        Path stagingFile = backendRoot.resolve("sql/20260606_upstream_sync_staging_diff.sql");
        Path productReadModelFile = backendRoot.resolve("sql/20260607_source_product_read_model.sql");
        Path stockReadModelFile = backendRoot.resolve("sql/20260607_source_warehouse_stock_read_model.sql");

        String manifest = Files.readString(manifestFile, StandardCharsets.UTF_8);
        String seed = Files.readString(seedFile, StandardCharsets.UTF_8);
        String normalizedSeed = seed.replace("\r\n", "\n").toLowerCase();
        String inventory = Files.readString(inventoryFile, StandardCharsets.UTF_8);
        String staging = Files.readString(stagingFile, StandardCharsets.UTF_8);
        String productReadModel = Files.readString(productReadModelFile, StandardCharsets.UTF_8);
        String stockReadModel = Files.readString(stockReadModelFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertGuard(backendRoot, "sql/upstream_system_management_seed.sql",
                "@confirm_upstream_system_management_seed",
                "APPLY_UPSTREAM_SYSTEM_MANAGEMENT_SEED", violations);
        assertGuard(backendRoot, "sql/20260606_upstream_inventory_dimension_sync.sql",
                "@confirm_upstream_inventory_dimension_sync",
                "APPLY_UPSTREAM_INVENTORY_DIMENSION_SYNC", violations);
        assertGuard(backendRoot, "sql/20260606_upstream_sync_staging_diff.sql",
                "@confirm_upstream_sync_staging_diff",
                "APPLY_UPSTREAM_SYNC_STAGING_DIFF", violations);
        assertGuard(backendRoot, "sql/20260607_source_product_read_model.sql",
                "@confirm_source_product_read_model",
                "APPLY_SOURCE_PRODUCT_READ_MODEL", violations);
        assertGuard(backendRoot, "sql/20260607_source_warehouse_stock_read_model.sql",
                "@confirm_source_warehouse_stock_read_model",
                "APPLY_SOURCE_WAREHOUSE_STOCK_READ_MODEL", violations);

        for (String expected : new String[] {
                "Integration Fresh Bootstrap 必跑 SQL 清单",
                "Fresh Bootstrap 必跑顺序",
                "RuoYi-Vue/sql/upstream_system_management_seed.sql",
                "RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql",
                "RuoYi-Vue/sql/20260606_upstream_sync_staging_diff.sql",
                "RuoYi-Vue/sql/20260607_source_product_read_model.sql",
                "RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql",
                "staging/state/batch",
                "OFFICIAL_MASTER",
                "不能只运行 `upstream_system_management_seed.sql`"
        })
        {
            requireContains(violations, manifestFile.getFileName().toString(), manifest, expected);
        }
        assertAppearsBefore(violations, manifestFile.getFileName().toString(), manifest,
                "RuoYi-Vue/sql/upstream_system_management_seed.sql",
                "RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql");
        assertAppearsBefore(violations, manifestFile.getFileName().toString(), manifest,
                "RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql",
                "RuoYi-Vue/sql/20260606_upstream_sync_staging_diff.sql");
        assertAppearsBefore(violations, manifestFile.getFileName().toString(), manifest,
                "RuoYi-Vue/sql/20260606_upstream_sync_staging_diff.sql",
                "RuoYi-Vue/sql/20260607_source_product_read_model.sql");
        assertAppearsBefore(violations, manifestFile.getFileName().toString(), manifest,
                "RuoYi-Vue/sql/20260607_source_product_read_model.sql",
                "RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql");

        requireContains(violations, seedFile.getFileName().toString(), seed,
                "docs/architecture/integration-bootstrap-required-sql.md");
        for (String unexpected : new String[] {
                "create table if not exists upstream_system_sync_state",
                "create table if not exists upstream_system_sync_batch",
                "create table if not exists upstream_system_warehouse_candidate_stage",
                "create table if not exists upstream_system_logistics_channel_candidate_stage",
                "create table if not exists upstream_system_sku_candidate_stage",
                "create table if not exists upstream_system_sku_dimension_stage",
                "create table if not exists upstream_system_sku_inventory_snapshot",
                "create table if not exists source_product_group",
                "create table if not exists source_product_dimension_group",
                "create table if not exists source_product_warehouse_detail",
                "create table if not exists source_warehouse_stock_group",
                "create table if not exists source_warehouse_stock_detail",
                "create table if not exists source_warehouse_stock_filter_metric",
                "delete from source_product_group",
                "delete from source_warehouse_stock_group"
        })
        {
            requireNotContains(violations, seedFile.getFileName().toString(), normalizedSeed, unexpected);
        }

        for (String expected : new String[] {
                "create table if not exists upstream_system_sku_inventory_snapshot",
                "create table if not exists upstream_system_inventory_sync_state"
        })
        {
            requireContains(violations, inventoryFile.getFileName().toString(), inventory, expected);
        }
        for (String expected : new String[] {
                "call add_column_if_missing('upstream_system_sku_candidate', 'wms_payload_hash'",
                "create table if not exists upstream_system_sync_state",
                "create table if not exists upstream_system_sync_batch",
                "create table if not exists upstream_system_warehouse_candidate_stage",
                "create table if not exists upstream_system_logistics_channel_candidate_stage",
                "create table if not exists upstream_system_sku_candidate_stage",
                "create table if not exists upstream_system_sku_dimension_stage"
        })
        {
            requireContains(violations, stagingFile.getFileName().toString(), staging, expected);
        }
        for (String expected : new String[] {
                "source product read model requires upstream_system_sku_candidate",
                "call assert_column_exists('upstream_system_sku_candidate', 'wms_payload_hash'",
                "create table if not exists source_product_group",
                "create table if not exists source_product_dimension_group",
                "create table if not exists source_product_warehouse_detail"
        })
        {
            requireContains(violations, productReadModelFile.getFileName().toString(), productReadModel, expected);
        }
        for (String expected : new String[] {
                "source warehouse stock read model requires upstream_system_sku_inventory_snapshot",
                "create table if not exists source_warehouse_stock_group",
                "create table if not exists source_warehouse_stock_detail",
                "create table if not exists source_warehouse_stock_filter_metric"
        })
        {
            requireContains(violations, stockReadModelFile.getFileName().toString(), stockReadModel, expected);
        }

        if (!violations.isEmpty())
        {
            fail("integration fresh bootstrap must declare and preserve mandatory post-seed SQL chain:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void threeTerminalIsolationMigrationMustUseReplaySafeAccountModifyHelper() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n");
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure modify_terminal_account_identity_columns_if_needed",
                "from information_schema.columns",
                "terminal account identity columns are required before account identity modify",
                "lower(c.data_type) <> expected.expected_type",
                "coalesce(c.character_maximum_length, -1) <> expected.expected_length",
                "c.is_nullable <> expected.expected_nullable",
                "coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>')",
                "alter table `', p_table, '` modify user_name varchar(30) not null, modify nick_name varchar(30) not null default '''', modify password varchar(100) not null comment ''密码密文''",
                "create procedure assert_no_blank_terminal_passwords",
                "terminal account password column is required before password preflight",
                "where password is null or trim(password) = ', char(39), char(39)",
                "seller_account contains blank passwords; reset or backfill before isolation migration",
                "buyer_account contains blank passwords; reset or backfill before isolation migration",
                "call modify_terminal_account_identity_columns_if_needed('seller_account');",
                "call modify_terminal_account_identity_columns_if_needed('buyer_account');",
                "call assert_no_blank_terminal_passwords('seller_account'",
                "call assert_no_blank_terminal_passwords('buyer_account'",
                "drop procedure if exists modify_terminal_account_identity_columns_if_needed",
                "drop procedure if exists assert_no_blank_terminal_passwords"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        for (String forbidden : new String[] {
                "alter table seller_account modify user_name varchar(30) not null",
                "alter table buyer_account modify user_name varchar(30) not null",
                "modify password varchar(100) not null default ''''",
                "password = coalesce(password, '')"
        })
        {
            requireNotContains(violations, sqlFile.getFileName().toString(), normalizedSource, forbidden);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call drop_column_if_exists('buyer_account', 'user_id');",
                "call modify_terminal_account_identity_columns_if_needed('seller_account');");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_no_blank_terminal_passwords('seller_account'",
                "update seller_account");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_no_blank_terminal_passwords('buyer_account'",
                "update buyer_account");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call modify_terminal_account_identity_columns_if_needed('seller_account');",
                "call recreate_index_if_mismatch('seller_account', 'uk_seller_account_username'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call modify_terminal_account_identity_columns_if_needed('buyer_account');",
                "call recreate_index_if_mismatch('buyer_account', 'uk_buyer_account_username'");

        if (!violations.isEmpty())
        {
            fail("three terminal isolation migration must use a replay-safe account identity modify helper:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalAccountPasswordColumnsMustNotDefaultToBlank() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        Path migrationSql = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String seedSource = Files.readString(seedSql, StandardCharsets.UTF_8).replace("\r\n", "\n").toLowerCase();
        String migrationSource = Files.readString(migrationSql, StandardCharsets.UTF_8).replace("\r\n", "\n")
                .toLowerCase();
        List<String> violations = new ArrayList<>();

        for (String forbidden : new String[] {
                "password              varchar(100)    not null default ''",
                "modify password varchar(100) not null default ''''",
                "union all select 'password', 'varchar', 100, 'no', ''"
        })
        {
            requireNotContains(violations, seedSql.getFileName().toString(), seedSource, forbidden);
            requireNotContains(violations, migrationSql.getFileName().toString(), migrationSource, forbidden);
        }
        for (String expected : new String[] {
                "password              varchar(100)    not null                   comment '密码密文'"
        })
        {
            requireContains(violations, seedSql.getFileName().toString(), seedSource, expected);
        }
        for (String expected : new String[] {
                "union all select 'password', 'varchar', 100, 'no', cast(null as char)",
                "modify password varchar(100) not null comment ''密码密文''",
                "password              varchar(100)    not null,"
        })
        {
            requireContains(violations, migrationSql.getFileName().toString(), migrationSource, expected);
        }

        if (!violations.isEmpty())
        {
            fail("terminal account password columns must fail closed instead of defaulting to blank strings:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerBuyerManagementSeedMustConvergePatchExistingPasswordColumnsFailClosed() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String source = Files.readString(seedSql, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n").toLowerCase();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_no_blank_terminal_passwords",
                "terminal account password column is required before password final structure check",
                "where password is null or trim(password) = ',",
                "seller_account contains blank passwords; reset or backfill before seller/buyer management seed",
                "buyer_account contains blank passwords; reset or backfill before seller/buyer management seed",
                "create procedure modify_terminal_account_password_column_if_needed",
                "lower(c.data_type) <> 'varchar'",
                "coalesce(c.character_maximum_length, -1) <> 100",
                "c.is_nullable <> 'NO'",
                "c.column_default is not null",
                "alter table `', p_table, '` modify password varchar(100) not null comment ''密码密文''",
                "create procedure assert_terminal_account_password_column_final",
                "seller_account.password final column must be varchar(100) not null without default",
                "buyer_account.password final column must be varchar(100) not null without default",
                "call assert_no_blank_terminal_passwords('seller_account'",
                "call assert_no_blank_terminal_passwords('buyer_account'",
                "call modify_terminal_account_password_column_if_needed('seller_account');",
                "call modify_terminal_account_password_column_if_needed('buyer_account');",
                "call assert_terminal_account_password_column_final('seller_account'",
                "call assert_terminal_account_password_column_final('buyer_account'",
                "drop procedure if exists assert_no_blank_terminal_passwords",
                "drop procedure if exists modify_terminal_account_password_column_if_needed",
                "drop procedure if exists assert_terminal_account_password_column_final"
        })
        {
            requireContains(violations, seedSql.getFileName().toString(), source, expected);
        }
        for (String forbidden : new String[] {
                "modify password varchar(100) not null default ''''",
                "password = coalesce(password, '')",
                "password              varchar(100)    not null default ''"
        })
        {
            requireNotContains(violations, seedSql.getFileName().toString(), normalizedSource, forbidden);
        }
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_no_blank_terminal_passwords('seller_account'",
                "call modify_terminal_account_password_column_if_needed('seller_account');");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_no_blank_terminal_passwords('buyer_account'",
                "call modify_terminal_account_password_column_if_needed('buyer_account');");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call modify_terminal_account_password_column_if_needed('seller_account');",
                "create table if not exists seller_dept");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "call assert_terminal_account_password_column_final('seller_account'",
                "call assert_seller_buyer_management_seed_completed();");

        if (!violations.isEmpty())
        {
            fail("seller/buyer management PATCH_EXISTING seed must converge password columns fail-closed:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void threeTerminalIsolationMigrationMustRunPreflightBeforeFirstDdlAndDocumentPartialApplyRisk()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String lowerSource = source.toLowerCase();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "non-transactional migration",
                "ddl causes implicit commits",
                "may be partially applied",
                "complete legacy preflight before execution",
                "does not repair data",
                "fix the failure cause and rerun the same script"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), lowerSource, expected);
        }
        for (String expected : new String[] {
                "create procedure assert_database_selected",
                "select target database before running three-terminal isolation migration",
                "create procedure assert_existing_column_if_table_present",
                "from information_schema.tables",
                "create procedure assert_three_terminal_isolation_preflight",
                "call assert_database_selected();",
                "call assert_existing_column_if_table_present('seller_account', 'seller_account_id'",
                "call assert_existing_column_if_table_present('seller_account', 'seller_id'",
                "call assert_existing_column_if_table_present('seller_account', 'account_role'",
                "call assert_existing_column_if_table_present('buyer_account', 'buyer_account_id'",
                "call assert_existing_column_if_table_present('buyer_account', 'buyer_id'",
                "call assert_existing_column_if_table_present('buyer_account', 'account_role'",
                "call assert_no_legacy_account_user_bindings('seller_account'",
                "call assert_no_legacy_account_user_bindings('buyer_account'",
                "call assert_three_terminal_isolation_preflight();",
                "drop procedure if exists assert_three_terminal_isolation_preflight",
                "drop procedure if exists assert_existing_column_if_table_present",
                "drop procedure if exists assert_database_selected",
                "drop procedure if exists assert_no_legacy_account_user_bindings"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        requireNotContains(violations, sqlFile.getFileName().toString(), lowerSource, "start transaction;");
        requireNotContains(violations, sqlFile.getFileName().toString(), lowerSource, "rollback;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_three_terminal_isolation_migration_confirmed();",
                "call assert_three_terminal_isolation_preflight();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "create procedure assert_three_terminal_isolation_preflight",
                "call assert_three_terminal_isolation_preflight();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_three_terminal_isolation_preflight();",
                "create table if not exists seller_account");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_three_terminal_isolation_preflight();",
                "update seller_account");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_three_terminal_isolation_preflight();",
                "call drop_column_if_exists('seller_account', 'user_id');");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_three_terminal_isolation_preflight();",
                "call drop_column_if_exists('buyer_account', 'user_id');");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_database_selected();",
                "call assert_existing_column_if_table_present('seller_account', 'seller_account_id'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_existing_column_if_table_present('buyer_account', 'account_role'",
                "call assert_no_legacy_account_user_bindings('seller_account'");

        if (!violations.isEmpty())
        {
            fail("three terminal isolation migration must preflight before DDL/DML and document partial-apply risk:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void threeTerminalIsolationMigrationMustAssertFinalTerminalContracts()
            throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_three_terminal_required_column",
                "create procedure assert_three_terminal_isolation_migration_completed",
                "call assert_three_terminal_required_column('seller_account', 'seller_account_id'",
                "call assert_three_terminal_required_column('seller_account', 'seller_id'",
                "call assert_three_terminal_required_column('seller_account', 'dept_id'",
                "call assert_three_terminal_required_column('seller_account', 'user_name'",
                "call assert_three_terminal_required_column('seller_account', 'password'",
                "call assert_three_terminal_required_column('seller_account', 'account_role'",
                "call assert_three_terminal_required_column('seller_account', 'status'",
                "call assert_three_terminal_required_column('seller_account', 'lock_status'",
                "call assert_three_terminal_required_column('seller_account', 'last_login_time'",
                "call assert_three_terminal_required_column('seller_account', 'pwd_update_time'",
                "call assert_three_terminal_required_column('buyer_account', 'buyer_account_id'",
                "call assert_three_terminal_required_column('buyer_account', 'buyer_id'",
                "call assert_three_terminal_required_column('buyer_account', 'dept_id'",
                "call assert_three_terminal_required_column('buyer_account', 'user_name'",
                "call assert_three_terminal_required_column('buyer_account', 'password'",
                "call assert_three_terminal_required_column('buyer_account', 'account_role'",
                "call assert_three_terminal_required_column('buyer_account', 'status'",
                "call assert_three_terminal_required_column('buyer_account', 'lock_status'",
                "call assert_three_terminal_required_column('buyer_account', 'last_login_time'",
                "call assert_three_terminal_required_column('buyer_account', 'pwd_update_time'",
                "table_name = 'seller_account'",
                "column_name = 'user_id'",
                "seller_account.user_id must be removed after isolation migration",
                "table_name = 'buyer_account'",
                "buyer_account.user_id must be removed after isolation migration",
                "seller_menu_id not between 100000 and 199999",
                "parent_id not between 100000 and 199999",
                "buyer_menu_id not between 200000 and 299999",
                "parent_id not between 200000 and 299999",
                "left join seller_role r on r.seller_role_id = rm.seller_role_id",
                "left join seller_menu m on m.seller_menu_id = rm.seller_menu_id",
                "seller_role_menu contains orphan role or menu ids",
                "left join buyer_role r on r.buyer_role_id = rm.buyer_role_id",
                "left join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id",
                "buyer_role_menu contains orphan role or menu ids",
                "call assert_three_terminal_required_column('seller_login_log', 'direct_login'",
                "call assert_three_terminal_required_column('seller_login_log', 'direct_login_ticket_id'",
                "call assert_three_terminal_required_column('seller_login_log', 'acting_admin_id'",
                "call assert_three_terminal_required_column('seller_login_log', 'acting_admin_name'",
                "call assert_three_terminal_required_column('seller_login_log', 'direct_login_reason'",
                "call assert_three_terminal_required_column('buyer_login_log', 'direct_login'",
                "call assert_three_terminal_required_column('buyer_login_log', 'direct_login_ticket_id'",
                "call assert_three_terminal_required_column('buyer_login_log', 'acting_admin_id'",
                "call assert_three_terminal_required_column('buyer_login_log', 'acting_admin_name'",
                "call assert_three_terminal_required_column('buyer_login_log', 'direct_login_reason'",
                "call assert_three_terminal_required_column('seller_oper_log', 'direct_login'",
                "call assert_three_terminal_required_column('buyer_oper_log', 'direct_login_reason'",
                "call assert_three_terminal_required_column('seller_session', 'direct_login'",
                "call assert_three_terminal_required_column('buyer_session', 'direct_login_reason'",
                "call assert_three_terminal_isolation_migration_completed();",
                "drop procedure if exists assert_three_terminal_isolation_migration_completed",
                "drop procedure if exists assert_three_terminal_required_column"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "create procedure assert_three_terminal_isolation_migration_completed",
                "call assert_three_terminal_isolation_migration_completed();");
        assertAppearsBefore(violations, fileName, source,
                "call add_column_if_missing('buyer_session', 'direct_login_reason'",
                "call assert_three_terminal_isolation_migration_completed();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_three_terminal_isolation_migration_completed();",
                "drop procedure if exists add_column_if_missing;");
        assertAppearsBefore(violations, fileName, source,
                "call assert_three_terminal_isolation_migration_completed();",
                "drop procedure if exists assert_three_terminal_isolation_migration_completed;");

        if (!violations.isEmpty())
        {
            fail("three terminal isolation migration must assert final terminal contracts:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void portalDirectLoginTicketMigrationMustUseReplaySafeModifyHelper() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260604_portal_direct_login_ticket.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n");
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure modify_portal_direct_login_ticket_columns_if_needed",
                "create procedure assert_portal_direct_login_ticket_identity_contract",
                "@portal_direct_login_ticket_normalize_expected_count",
                "@portal_direct_login_ticket_normalize_expected_signature",
                "set @portal_direct_login_ticket_normalize_expected_count after previewing exact portal_direct_login_ticket normalize rows",
                "set @portal_direct_login_ticket_normalize_expected_signature after previewing exact portal_direct_login_ticket normalize rows",
                "create procedure assert_portal_direct_login_ticket_normalize_targets",
                "portal_direct_login_ticket normalize exact target count mismatch",
                "portal_direct_login_ticket normalize exact target signature mismatch",
                "create procedure assert_portal_direct_login_ticket_column_contract",
                "portal_direct_login_ticket.ticket_id must be bigint not null auto_increment",
                "portal_direct_login_ticket primary key must be ticket_id",
                "portal_direct_login_ticket expected columns are required before ticket column modify",
                "lower(c.data_type) <> expected.expected_type",
                "coalesce(c.character_maximum_length, -1) <> expected.expected_length",
                "c.is_nullable <> expected.expected_nullable",
                "coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>')",
                "portal_direct_login_ticket final column contract mismatch",
                "alter table portal_direct_login_ticket modify terminal varchar(20) not null",
                "modify token_hash varchar(64) not null",
                "modify expire_time datetime not null",
                "terminal is null",
                "terminal not in ('seller', 'buyer')",
                "target_subject_id is null",
                "target_subject_id <= 0",
                "target_account_id is null",
                "target_account_id <= 0",
                "target_user_name is null",
                "trim(target_user_name) = ''",
                "acting_admin_id is null",
                "acting_admin_id <= 0",
                "acting_admin_name is null",
                "trim(acting_admin_name) = ''",
                "trim(token_hash) = ''",
                "status is null",
                "status not in ('ISSUED', 'USED', 'EXPIRED')",
                "status = 'USED' and used_time is null",
                "status = 'ISSUED' and used_time is not null",
                "call assert_portal_direct_login_ticket_normalize_targets();",
                "call modify_portal_direct_login_ticket_columns_if_needed();",
                "call assert_portal_direct_login_ticket_column_contract();",
                "call assert_portal_direct_login_ticket_identity_contract();",
                "call assert_portal_direct_login_ticket_column_contract();\ncall assert_portal_direct_login_ticket_identity_contract();\n\ncall recreate_index_if_mismatch",
                "drop procedure if exists assert_portal_direct_login_ticket_normalize_targets",
                "drop procedure if exists assert_portal_direct_login_ticket_column_contract",
                "drop procedure if exists assert_portal_direct_login_ticket_identity_contract",
                "drop procedure if exists modify_portal_direct_login_ticket_columns_if_needed"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        requireNotContains(violations, sqlFile.getFileName().toString(), normalizedSource,
                "alter table portal_direct_login_ticket\n  modify terminal varchar(20) not null");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_no_invalid_direct_login_ticket_rows();",
                "call modify_portal_direct_login_ticket_columns_if_needed();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_portal_direct_login_ticket_normalize_targets();",
                "update portal_direct_login_ticket set target_subject_no = '' where target_subject_no is null;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_column_exists('portal_direct_login_ticket', 'ticket_id'",
                "call assert_portal_direct_login_ticket_identity_contract();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call modify_portal_direct_login_ticket_columns_if_needed();",
                "call assert_portal_direct_login_ticket_column_contract();");

        if (!violations.isEmpty())
        {
            fail("portal direct-login ticket migration must use a replay-safe ticket column modify helper:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sourceWarehouseStockReadModelMustStayReplaySafeAndScoped() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260607_source_warehouse_stock_read_model.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n").toLowerCase();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "set @confirm_source_warehouse_stock_read_model",
                "APPLY_SOURCE_WAREHOUSE_STOCK_READ_MODEL",
                "create procedure assert_source_warehouse_stock_read_model_confirmed",
                "create procedure assert_table_exists",
                "create procedure assert_column_exists",
                "create procedure assert_source_warehouse_stock_read_model_completed",
                "call assert_source_warehouse_stock_read_model_confirmed();",
                "source warehouse stock read model requires upstream_system_sku_inventory_snapshot",
                "source warehouse stock read model requires upstream_system_connection",
                "call assert_table_exists('upstream_system_sku_inventory_snapshot'",
                "call assert_table_exists('upstream_system_connection'",
                "call assert_column_exists('upstream_system_sku_inventory_snapshot', 'inventory_snapshot_id'",
                "call assert_column_exists('upstream_system_sku_inventory_snapshot', 'connection_code'",
                "call assert_column_exists('upstream_system_sku_inventory_snapshot', 'upstream_warehouse_code'",
                "call assert_column_exists('upstream_system_sku_inventory_snapshot', 'master_sku'",
                "call assert_column_exists('upstream_system_sku_inventory_snapshot', 'inventory_scope'",
                "call assert_column_exists('upstream_system_sku_inventory_snapshot', 'status'",
                "call assert_column_exists('upstream_system_connection', 'connection_code'",
                "call assert_column_exists('upstream_system_connection', 'system_kind'",
                "call assert_column_exists('upstream_system_connection', 'master_warehouse_name'",
                "drop procedure if exists assert_table_exists",
                "drop procedure if exists assert_column_exists",
                "create table if not exists source_warehouse_stock_group",
                "create table if not exists source_warehouse_stock_detail",
                "create table if not exists source_warehouse_stock_filter_metric",
                "primary key (source_stock_group_key)",
                "primary key (inventory_snapshot_id)",
                "primary key (metric_key)",
                "unique key uk_source_warehouse_stock_group_natural",
                "unique key uk_source_warehouse_stock_filter_metric_natural",
                "set group_concat_max_len = 1048576;",
                "drop temporary table if exists tmp_source_warehouse_stock_detail;",
                "drop temporary table if exists tmp_source_warehouse_stock_group;",
                "drop temporary table if exists tmp_source_warehouse_stock_filter_metric;",
                "create temporary table tmp_source_warehouse_stock_detail like source_warehouse_stock_detail;",
                "create temporary table tmp_source_warehouse_stock_group like source_warehouse_stock_group;",
                "create temporary table tmp_source_warehouse_stock_filter_metric like source_warehouse_stock_filter_metric;",
                "insert into tmp_source_warehouse_stock_detail(",
                "insert into tmp_source_warehouse_stock_group(",
                "from tmp_source_warehouse_stock_detail d",
                "insert into tmp_source_warehouse_stock_filter_metric(",
                "start transaction;",
                "delete from source_warehouse_stock_filter_metric where repository_scope = 'OFFICIAL_MASTER';",
                "delete from source_warehouse_stock_group where repository_scope = 'OFFICIAL_MASTER';",
                "delete from source_warehouse_stock_detail where repository_scope = 'OFFICIAL_MASTER';",
                "select * from tmp_source_warehouse_stock_detail;",
                "select * from tmp_source_warehouse_stock_group;",
                "select * from tmp_source_warehouse_stock_filter_metric;",
                "source warehouse stock detail read model completed count mismatch",
                "source warehouse stock detail read model completed missing expected rows",
                "source warehouse stock group read model completed count mismatch",
                "source warehouse stock group read model completed missing expected keys",
                "source warehouse stock filter read model completed count mismatch",
                "source warehouse stock filter read model completed missing expected keys",
                "call assert_source_warehouse_stock_read_model_completed();",
                "commit;",
                "from upstream_system_sku_inventory_snapshot s",
                "left join upstream_system_connection c on c.connection_code = s.connection_code",
                "concat('SOURCE_STOCK:', sha2(concat(",
                "char_length(trim(coalesce(s.master_sku, ''))), ':', trim(coalesce(s.master_sku, ''))",
                "char_length(trim(coalesce(s.master_product_name, ''))), ':', trim(coalesce(s.master_product_name, ''))",
                "group_concat(distinct nullif(d.master_warehouse_name, '') order by d.master_warehouse_name separator ' ')",
                "group_concat(distinct nullif(d.upstream_warehouse_code, '') order by d.upstream_warehouse_code separator ' ')",
                "group_concat(distinct nullif(d.upstream_warehouse_name, '') order by d.upstream_warehouse_name separator ' ')",
                "group_concat(distinct nullif(d.system_warehouse_code, '') order by d.system_warehouse_code separator ' ')",
                "group_concat(distinct nullif(d.system_warehouse_name, '') order by d.system_warehouse_name separator ' ')",
                "group_concat(distinct nullif(d.system_sku, '') order by d.system_sku separator ' ')",
                "group_concat(distinct nullif(d.system_sku_name, '') order by d.system_sku_name separator ' ')",
                "group_concat(distinct nullif(d.customer_name, '') order by d.customer_name separator ' ')",
                "where d.repository_scope = 'OFFICIAL_MASTER'",
                "SOURCE_STOCK_METRIC:",
                "filter_type, filter_value",
                "union all",
                "drop temporary table if exists tmp_source_warehouse_stock_filter_metric;",
                "drop procedure if exists assert_source_warehouse_stock_read_model_completed",
                "drop procedure if exists assert_source_warehouse_stock_read_model_confirmed"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }

        for (String unexpected : new String[] {
                "truncate table source_warehouse_stock_group",
                "truncate table source_warehouse_stock_detail",
                "truncate table source_warehouse_stock_filter_metric",
                "drop table source_warehouse_stock_group",
                "drop table source_warehouse_stock_detail",
                "drop table source_warehouse_stock_filter_metric",
                "delete from source_warehouse_stock_group;",
                "delete from source_warehouse_stock_detail;",
                "delete from source_warehouse_stock_filter_metric;",
                "group_concat(distinct nullif(d.master_warehouse_name, '') separator ' ')",
                "group_concat(distinct nullif(d.upstream_warehouse_code, '') separator ' ')",
                "group_concat(distinct nullif(d.upstream_warehouse_name, '') separator ' ')",
                "group_concat(distinct nullif(d.system_warehouse_code, '') separator ' ')",
                "group_concat(distinct nullif(d.system_warehouse_name, '') separator ' ')",
                "group_concat(distinct nullif(d.system_sku, '') separator ' ')",
                "group_concat(distinct nullif(d.system_sku_name, '') separator ' ')",
                "group_concat(distinct nullif(d.customer_name, '') separator ' ')"
        })
        {
            requireNotContains(violations, sqlFile.getFileName().toString(), normalizedSource, unexpected);
        }

        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_warehouse_stock_read_model_confirmed();",
                "call assert_table_exists('upstream_system_sku_inventory_snapshot'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_column_exists('upstream_system_connection', 'master_warehouse_name'",
                "create table if not exists source_warehouse_stock_group");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "create table if not exists source_warehouse_stock_group",
                "create table if not exists source_warehouse_stock_detail");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "create table if not exists source_warehouse_stock_detail",
                "create table if not exists source_warehouse_stock_filter_metric");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "set group_concat_max_len = 1048576;",
                "create temporary table tmp_source_warehouse_stock_detail like source_warehouse_stock_detail;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "set group_concat_max_len = 1048576;",
                "create temporary table tmp_source_warehouse_stock_filter_metric like source_warehouse_stock_filter_metric;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into tmp_source_warehouse_stock_detail(",
                "insert into tmp_source_warehouse_stock_group(");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into tmp_source_warehouse_stock_group(",
                "insert into tmp_source_warehouse_stock_filter_metric(");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into tmp_source_warehouse_stock_filter_metric(",
                "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;",
                "delete from source_warehouse_stock_filter_metric where repository_scope = 'OFFICIAL_MASTER';");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "delete from source_warehouse_stock_detail where repository_scope = 'OFFICIAL_MASTER';",
                "select * from tmp_source_warehouse_stock_detail;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "select * from tmp_source_warehouse_stock_filter_metric;",
                "call assert_source_warehouse_stock_read_model_completed();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_warehouse_stock_read_model_completed();",
                "commit;");

        if (!violations.isEmpty())
        {
            fail("source warehouse stock read model SQL must stay replay-safe and scoped:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sourceProductReadModelMustStayReplaySafeAndScoped() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260607_source_product_read_model.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n").toLowerCase();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "set @confirm_source_product_read_model",
                "APPLY_SOURCE_PRODUCT_READ_MODEL",
                "create procedure assert_source_product_read_model_confirmed",
                "create procedure assert_table_exists",
                "create procedure assert_column_exists",
                "create procedure assert_source_product_read_model_completed",
                "call assert_source_product_read_model_confirmed();",
                "source product read model requires upstream_system_sku_candidate",
                "source product read model requires upstream_system_connection",
                "source product read model requires upstream_system_sku_pairing",
                "call assert_table_exists('upstream_system_sku_candidate'",
                "call assert_table_exists('upstream_system_connection'",
                "call assert_table_exists('upstream_system_sku_pairing'",
                "call assert_column_exists('upstream_system_sku_candidate', 'connection_code'",
                "call assert_column_exists('upstream_system_sku_candidate', 'master_sku'",
                "call assert_column_exists('upstream_system_sku_candidate', 'master_product_name'",
                "call assert_column_exists('upstream_system_sku_candidate', 'status'",
                "call assert_column_exists('upstream_system_sku_candidate', 'search_text'",
                "call assert_column_exists('upstream_system_connection', 'connection_code'",
                "call assert_column_exists('upstream_system_connection', 'system_kind'",
                "call assert_column_exists('upstream_system_connection', 'master_warehouse_name'",
                "call assert_column_exists('upstream_system_sku_pairing', 'connection_code'",
                "call assert_column_exists('upstream_system_sku_pairing', 'master_sku'",
                "call assert_column_exists('upstream_system_sku_pairing', 'sku_pairing_id'",
                "drop procedure if exists assert_table_exists",
                "drop procedure if exists assert_column_exists",
                "create table if not exists source_product_group",
                "create table if not exists source_product_dimension_group",
                "create table if not exists source_product_warehouse_detail",
                "primary key (source_sku_group_key)",
                "primary key (source_dimension_group_key)",
                "unique key uk_source_product_warehouse_row",
                "(repository_scope, connection_code, master_sku, source_dimension_group_key)",
                "set group_concat_max_len = 1048576;",
                "drop temporary table if exists tmp_source_product_group;",
                "drop temporary table if exists tmp_source_product_dimension_group;",
                "drop temporary table if exists tmp_source_product_warehouse_detail;",
                "create temporary table tmp_source_product_group like source_product_group;",
                "create temporary table tmp_source_product_dimension_group like source_product_dimension_group;",
                "create temporary table tmp_source_product_warehouse_detail like source_product_warehouse_detail;",
                "insert into tmp_source_product_group(",
                "insert into tmp_source_product_dimension_group(",
                "insert into tmp_source_product_warehouse_detail(",
                "start transaction;",
                "delete from source_product_warehouse_detail where repository_scope = 'OFFICIAL_MASTER';",
                "delete from source_product_dimension_group where repository_scope = 'OFFICIAL_MASTER';",
                "delete from source_product_group where repository_scope = 'OFFICIAL_MASTER';",
                "insert into source_product_group",
                "insert into source_product_dimension_group",
                "insert into source_product_warehouse_detail(",
                "select * from tmp_source_product_group;",
                "select * from tmp_source_product_dimension_group;",
                "from tmp_source_product_warehouse_detail;",
                "source product group read model completed count mismatch",
                "source product group read model completed missing expected keys",
                "source product dimension read model completed count mismatch",
                "source product dimension read model completed missing expected keys",
                "source product warehouse read model completed count mismatch",
                "source product warehouse read model completed missing expected rows",
                "call assert_source_product_read_model_completed();",
                "commit;",
                "from upstream_system_sku_candidate c",
                "inner join upstream_system_connection conn",
                "left join upstream_system_sku_pairing p",
                "where (conn.system_kind = 'lingxing-wms' or conn.system_kind = 'LINGXING_WMS')",
                "concat('OFFICIAL_MASTER:', sha2(concat(",
                "char_length(trim(coalesce(c.master_sku, ''))), ':', trim(coalesce(c.master_sku, ''))",
                "char_length(trim(coalesce(c.master_product_name, ''))), ':', trim(coalesce(c.master_product_name, ''))",
                "group_concat(distinct conn.master_warehouse_name order by conn.master_warehouse_name separator ' ')",
                "group_concat(distinct nullif(p.system_sku, '') order by p.system_sku separator ' ')",
                "group_concat(distinct nullif(p.system_sku_name, '') order by p.system_sku_name separator ' ')",
                "group_concat(distinct nullif(p.customer_name, '') order by p.customer_name separator ' ')",
                "group_concat(distinct c.connection_code order by c.connection_code separator ' ')",
                "on duplicate key update",
                "drop temporary table if exists tmp_source_product_warehouse_detail;",
                "drop procedure if exists assert_source_product_read_model_completed",
                "drop procedure if exists assert_source_product_read_model_confirmed"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }

        for (String unexpected : new String[] {
                "truncate table source_product_group",
                "truncate table source_product_dimension_group",
                "truncate table source_product_warehouse_detail",
                "drop table source_product_group",
                "drop table source_product_dimension_group",
                "drop table source_product_warehouse_detail",
                "delete from source_product_group;",
                "delete from source_product_dimension_group;",
                "delete from source_product_warehouse_detail;",
                "(repository_scope, connection_code, master_sku),",
                "group_concat(distinct conn.master_warehouse_name separator ' ')",
                "group_concat(distinct nullif(p.system_sku, '') separator ' ')",
                "group_concat(distinct nullif(p.system_sku_name, '') separator ' ')",
                "group_concat(distinct nullif(p.customer_name, '') separator ' ')",
                "group_concat(distinct c.connection_code separator ' ')"
        })
        {
            requireNotContains(violations, sqlFile.getFileName().toString(), normalizedSource, unexpected);
        }

        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_product_read_model_confirmed();",
                "call assert_table_exists('upstream_system_sku_candidate'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_column_exists('upstream_system_sku_pairing', 'customer_name'",
                "create table if not exists source_product_group");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "create table if not exists source_product_group",
                "create table if not exists source_product_dimension_group");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "create table if not exists source_product_dimension_group",
                "create table if not exists source_product_warehouse_detail");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "set group_concat_max_len = 1048576;",
                "create temporary table tmp_source_product_group like source_product_group;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into tmp_source_product_group(",
                "insert into tmp_source_product_dimension_group(");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into tmp_source_product_dimension_group(",
                "insert into tmp_source_product_warehouse_detail(");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "insert into tmp_source_product_warehouse_detail(",
                "start transaction;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "start transaction;",
                "delete from source_product_warehouse_detail where repository_scope = 'OFFICIAL_MASTER';");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "delete from source_product_group where repository_scope = 'OFFICIAL_MASTER';",
                "select * from tmp_source_product_group;");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "from tmp_source_product_warehouse_detail;",
                "call assert_source_product_read_model_completed();");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_product_read_model_completed();",
                "commit;");
        if (!violations.isEmpty())
        {
            fail("source product read model SQL must stay replay-safe and scoped:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void productDistributionStatusPriceLogMustUseReplaySafeColumnAndModifyHelpers() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260605_product_distribution_status_price_log.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n");
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@product_distribution_status_price_log_spu_disabled_expected_count",
                "@product_distribution_status_price_log_spu_disabled_expected_signature",
                "@product_distribution_status_price_log_sku_disabled_expected_count",
                "@product_distribution_status_price_log_sku_disabled_expected_signature",
                "create procedure add_column_if_missing",
                "create procedure modify_product_sku_sale_price_if_needed",
                "create procedure assert_product_distribution_status_price_log_expected_targets",
                "product_sku.sale_price column is required before product distribution status price migration",
                "product_spu DISABLED exact target count mismatch",
                "product_spu DISABLED exact target signature mismatch",
                "product_sku DISABLED exact target count mismatch",
                "product_sku DISABLED exact target signature mismatch",
                "data_type <> 'decimal'",
                "numeric_precision <> 18",
                "numeric_scale <> 4",
                "is_nullable <> 'YES'",
                "column_comment <> '销售价'",
                "alter table product_sku modify column sale_price decimal(18,4) null comment '销售价'",
                "call add_column_if_missing('product_spu', 'control_status'",
                "call add_column_if_missing('product_sku', 'control_status'",
                "call modify_product_sku_sale_price_if_needed();",
                "call assert_product_distribution_status_price_log_expected_targets();",
                "drop procedure if exists assert_product_distribution_status_price_log_expected_targets",
                "drop procedure if exists modify_product_sku_sale_price_if_needed",
                "drop procedure if exists add_column_if_missing"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        requireNotContains(violations, sqlFile.getFileName().toString(), normalizedSource,
                "alter table product_spu\n    add column");
        requireNotContains(violations, sqlFile.getFileName().toString(), normalizedSource,
                "alter table product_sku\n    add column");
        requireNotContains(violations, sqlFile.getFileName().toString(), source,
                "insert into sys_menu");
        requireNotContains(violations, sqlFile.getFileName().toString(), source,
                "2485");
        requireNotContains(violations, sqlFile.getFileName().toString(), source,
                "2486");
        requireNotContains(violations, sqlFile.getFileName().toString(), source,
                "product:distribution:price");
        requireNotContains(violations, sqlFile.getFileName().toString(), source,
                "product:distribution:log");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_distribution_status_price_log_confirmed();",
                "call add_column_if_missing('product_spu', 'control_status'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call modify_product_sku_sale_price_if_needed();",
                "update product_spu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_distribution_status_price_log_expected_targets();",
                "update product_spu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_distribution_status_price_log_expected_targets();",
                "update product_sku");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call modify_product_sku_sale_price_if_needed();",
                "call create_index_if_missing('product_spu', 'idx_product_spu_control_status'");

        if (!violations.isEmpty())
        {
            fail("product distribution status price log migration must be safe to replay:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void absorbedSeedFollowupScriptsMustUseReplaySafeColumnHelpers() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path currencySql = backendRoot.resolve("sql/20260604_currency_showapi_sync_migration.sql");
        Path skuDimensionSql = backendRoot.resolve("sql/20260605_mall_product_sku_dimension_fields.sql");
        Path editorSampleSql = backendRoot.resolve("sql/20260605_mall_product_editor_ui_sample_data.sql");
        String currencySource = Files.readString(currencySql, StandardCharsets.UTF_8);
        String skuDimensionSource = Files.readString(skuDimensionSql, StandardCharsets.UTF_8);
        String editorSampleSource = Files.readString(editorSampleSql, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "@currency_showapi_sync_config_expected_count",
                "@currency_showapi_sync_config_expected_signature",
                "@currency_showapi_currency_expected_count",
                "@currency_showapi_currency_expected_signature",
                "@currency_showapi_dict_expected_count",
                "@currency_showapi_dict_expected_signature",
                "set @currency_showapi_sync_config_expected_count after previewing exact finance_currency_sync_config targets",
                "set @currency_showapi_currency_expected_count after previewing exact finance_currency targets",
                "set @currency_showapi_dict_expected_count after previewing exact sys_dict_data currency_code targets",
                "create procedure add_column_if_missing",
                "create procedure assert_showapi_provider_conflict_absent",
                "create procedure assert_currency_showapi_sync_targets",
                "create procedure assert_currency_showapi_sync_completed",
                "finance currency sync config has both GENERIC_RATES and SHOWAPI_BANK_RATE",
                "currency ShowAPI sync config exact target count mismatch",
                "currency ShowAPI finance_currency exact target count mismatch",
                "currency ShowAPI sys_dict_data exact target count mismatch",
                "currency ShowAPI sync migration completed with stale GENERIC_RATES provider",
                "currency ShowAPI sync config completed state missing expected provider row",
                "currency ShowAPI migration completed with invalid currency_code dictionary default",
                "call add_column_if_missing('finance_currency_sync_config', 'rate_anchor_time'",
                "call assert_showapi_provider_conflict_absent();",
                "call assert_currency_showapi_sync_targets();",
                "start transaction;",
                "call assert_currency_showapi_sync_completed();",
                "commit;",
                "drop procedure if exists assert_currency_showapi_sync_completed",
                "drop procedure if exists assert_currency_showapi_sync_targets",
                "drop procedure if exists assert_showapi_provider_conflict_absent",
                "drop procedure if exists add_column_if_missing"
        })
        {
            requireContains(violations, currencySql.getFileName().toString(), currencySource, expected);
        }
        requireNotContains(violations, currencySql.getFileName().toString(), currencySource.replace("\r\n", "\n"),
                "alter table finance_currency_sync_config\n  add column");
        assertAppearsBefore(violations, currencySql.getFileName().toString(), currencySource,
                "call add_column_if_missing('finance_currency_sync_config', 'rate_anchor_time'",
                "call assert_showapi_provider_conflict_absent();");
        assertAppearsBefore(violations, currencySql.getFileName().toString(), currencySource,
                "call assert_showapi_provider_conflict_absent();",
                "call assert_currency_showapi_sync_targets();");
        assertAppearsBefore(violations, currencySql.getFileName().toString(), currencySource,
                "call assert_currency_showapi_sync_targets();",
                "start transaction;");
        assertAppearsBefore(violations, currencySql.getFileName().toString(), currencySource,
                "start transaction;",
                "update finance_currency_sync_config");
        assertAppearsBefore(violations, currencySql.getFileName().toString(), currencySource,
                "update sys_dict_data",
                "call assert_currency_showapi_sync_completed();");
        assertAppearsBefore(violations, currencySql.getFileName().toString(), currencySource,
                "call assert_currency_showapi_sync_completed();",
                "commit;");

        for (String expected : new String[] {
                "create procedure add_column_if_missing",
                "call add_column_if_missing('product_sku', 'length_value'",
                "call add_column_if_missing('product_sku', 'width_value'",
                "call add_column_if_missing('product_sku', 'height_value'",
                "drop procedure if exists add_column_if_missing"
        })
        {
            requireContains(violations, skuDimensionSql.getFileName().toString(), skuDimensionSource, expected);
        }
        requireNotContains(violations, skuDimensionSql.getFileName().toString(), skuDimensionSource.replace("\r\n", "\n"),
                "alter table product_sku\n  add column");
        assertAppearsBefore(violations, skuDimensionSql.getFileName().toString(), skuDimensionSource,
                "call add_column_if_missing('product_sku', 'height_value'",
                "update product_sku");

        for (String expected : new String[] {
                "@mall_product_editor_title_backfill_expected_count",
                "@mall_product_editor_title_backfill_expected_signature",
                "@mall_product_editor_demo_existing_expected_count",
                "@mall_product_editor_demo_existing_expected_signature",
                "set @mall_product_editor_title_backfill_expected_count after previewing exact product_spu title backfill rows",
                "set @mall_product_editor_demo_existing_expected_count after previewing existing SPUDEMO20260605/SKUDEMO20260605 rows",
                "create procedure assert_mall_product_editor_count_signature",
                "mall product editor title backfill exact target count mismatch",
                "mall product editor demo data exact target count mismatch",
                "call assert_mall_product_editor_count_signature(",
                "create procedure add_column_if_missing",
                "call add_column_if_missing('product_spu', 'product_name_en'",
                "call add_column_if_missing('product_spu', 'detail_content'",
                "start transaction;",
                "commit;",
                "drop procedure if exists add_column_if_missing",
                "drop procedure if exists assert_mall_product_editor_count_signature"
        })
        {
            requireContains(violations, editorSampleSql.getFileName().toString(), editorSampleSource, expected);
        }
        requireNotContains(violations, editorSampleSql.getFileName().toString(), editorSampleSource.replace("\r\n", "\n"),
                "alter table product_spu\n  add column");
        assertAppearsBefore(violations, editorSampleSql.getFileName().toString(), editorSampleSource,
                "call assert_mall_product_editor_count_signature(",
                "start transaction;");
        assertAppearsBefore(violations, editorSampleSql.getFileName().toString(), editorSampleSource,
                "call add_column_if_missing('product_spu', 'detail_content'",
                "update product_spu");
        assertAppearsBefore(violations, editorSampleSql.getFileName().toString(), editorSampleSource,
                "start transaction;", "update product_spu");
        assertAppearsBefore(violations, editorSampleSql.getFileName().toString(), editorSampleSource,
                "insert into product_image (owner_type, owner_id, spu_id, sku_id, image_url, image_role, sort_order, create_by, create_time)",
                "commit;");

        if (!violations.isEmpty())
        {
            fail("follow-up scripts with columns already absorbed by seed baselines must be safe to replay:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalLogScopeIndexesMustKeepDynamicDdlGuarded() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260606_terminal_log_scope_indexes.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_column_exists",
                "create procedure recreate_index_if_mismatch",
                "create procedure assert_index_definition",
                "prepare stmt from @ddl;",
                "set @ddl = concat('alter table `', p_table_name, '` drop index `', p_index_name, '`')",
                "set @ddl = concat('alter table `', p_table_name, '` add ', p_index_definition)",
                "drop procedure if exists recreate_index_if_mismatch;",
                "drop procedure if exists assert_index_definition;",
                "drop procedure if exists assert_column_exists;"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        for (String[] target : new String[][] {
                { "seller_login_log", "seller_account_id", "login_time", "idx_seller_login_log_account_time" },
                { "seller_login_log", "seller_id", "login_time", "idx_seller_login_log_seller_time" },
                { "buyer_login_log", "buyer_account_id", "login_time", "idx_buyer_login_log_account_time" },
                { "buyer_login_log", "buyer_id", "login_time", "idx_buyer_login_log_buyer_time" },
                { "seller_oper_log", "seller_account_id", "oper_time", "idx_seller_oper_log_account_time" },
                { "seller_oper_log", "seller_id", "oper_time", "idx_seller_oper_log_seller_time" },
                { "buyer_oper_log", "buyer_account_id", "oper_time", "idx_buyer_oper_log_account_time" },
                { "buyer_oper_log", "buyer_id", "oper_time", "idx_buyer_oper_log_buyer_time" }
        })
        {
            String tableName = target[0];
            String scopeColumn = target[1];
            String timeColumn = target[2];
            String indexName = target[3];
            requireContains(violations, fileName, source,
                    "call assert_column_exists('" + tableName + "', '" + scopeColumn + "'");
            requireContains(violations, fileName, source,
                    "call recreate_index_if_mismatch(\n  '" + tableName + "',\n  '" + indexName + "',");
            requireContains(violations, fileName, source,
                    "'" + scopeColumn + "," + timeColumn + "'");
            requireContains(violations, fileName, source,
                    "'key " + indexName + " (" + scopeColumn + ", " + timeColumn + ")'");
            requireContains(violations, fileName, source,
                    "call assert_index_definition('" + tableName + "', '" + indexName + "',");
            assertAppearsBefore(violations, fileName, source,
                    "call assert_column_exists('" + tableName + "', '" + scopeColumn + "'",
                    "call recreate_index_if_mismatch(\n  '" + tableName + "',\n  '" + indexName + "',");
            assertAppearsBefore(violations, fileName, source,
                    "call recreate_index_if_mismatch(\n  '" + tableName + "',\n  '" + indexName + "',",
                    "call assert_index_definition('" + tableName + "', '" + indexName + "',");
        }

        requireExactOccurrenceCount(violations, fileName, source, "call assert_column_exists(", 8);
        requireExactOccurrenceCount(violations, fileName, source, "call recreate_index_if_mismatch(", 8);
        requireExactOccurrenceCount(violations, fileName, source, "call assert_index_definition(", 8);
        requireExactOccurrenceCount(violations, fileName, source, "prepare stmt from @ddl;", 2);
        assertRecreateIndexTargetsOnlyTerminalLogTables(violations, fileName, source);
        assertAppearsBefore(violations, fileName, source,
                "call assert_column_exists('buyer_oper_log', 'buyer_id'",
                "call recreate_index_if_mismatch(");
        assertAppearsBefore(violations, fileName, source,
                "call recreate_index_if_mismatch(\n  'buyer_oper_log',\n  'idx_buyer_oper_log_buyer_time',",
                "call assert_index_definition('seller_login_log', 'idx_seller_login_log_account_time'");

        if (!violations.isEmpty())
        {
            fail("terminal log scope index migration must keep dynamic DDL guarded:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalOwnerUniqueConstraintMustAssertBaselineBeforeDynamicDdl() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260605_terminal_owner_account_unique_constraint.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_table_exists",
                "create procedure assert_column_exists",
                "call assert_table_exists('seller_account'",
                "call assert_table_exists('buyer_account'",
                "call assert_column_exists('seller_account', 'seller_id'",
                "call assert_column_exists('seller_account', 'account_role'",
                "call assert_column_exists('buyer_account', 'buyer_id'",
                "call assert_column_exists('buyer_account', 'account_role'",
                "drop procedure if exists assert_table_exists",
                "drop procedure if exists assert_column_exists"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }

        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_column_exists('buyer_account', 'account_role'",
                "call assert_no_duplicate_owner_account('seller_account'");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_no_duplicate_owner_account('buyer_account'",
                "call add_column_if_missing('seller_account'");

        if (!violations.isEmpty())
        {
            fail("terminal OWNER unique constraint migration must fail closed before dynamic DDL:\n"
                    + String.join("\n", violations));
        }
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

    private void requireExactOccurrenceCount(List<String> violations, String fileName, String source,
            String target, int expectedCount)
    {
        int actualCount = 0;
        int index = source.indexOf(target);
        while (index >= 0)
        {
            actualCount++;
            index = source.indexOf(target, index + target.length());
        }
        if (actualCount != expectedCount)
        {
            violations.add(fileName + " must contain exactly " + expectedCount + " occurrences of "
                    + target + ", actual " + actualCount);
        }
    }

    private void assertRecreateIndexTargetsOnlyTerminalLogTables(List<String> violations, String fileName,
            String source)
    {
        Set<String> allowedTables = Set.of("seller_login_log", "buyer_login_log", "seller_oper_log", "buyer_oper_log");
        Pattern callPattern = Pattern.compile("(?is)call\\s+recreate_index_if_mismatch\\s*\\(\\s*'([^']+)'");
        Matcher matcher = callPattern.matcher(source);
        while (matcher.find())
        {
            String tableName = matcher.group(1);
            if (!allowedTables.contains(tableName))
            {
                violations.add(fileName + " must not recreate terminal log scope indexes on " + tableName);
            }
        }
    }

    private void assertAccountNormalizeUpdateIsScoped(List<String> violations, String fileName, String source,
            String tableName)
    {
        Pattern statementPattern = Pattern.compile("(?is)update\\s+" + tableName + "\\s+set\\s+.*?;");
        Matcher matcher = statementPattern.matcher(source);
        if (!matcher.find())
        {
            violations.add(fileName + " must update " + tableName + " normalize targets");
            return;
        }

        String statement = matcher.group();
        for (String expected : new String[] {
                "where coalesce(user_name, '') = ''",
                "or coalesce(nick_name, '') = ''",
                "or email is null",
                "or phonenumber is null",
                "or coalesce(status, '') = ''",
                "or coalesce(lock_status, '') not in ('0', '1')",
                "or lock_reason is null",
                "or pwd_update_time is null"
        })
        {
            requireContains(violations, fileName + " " + tableName + " normalize update", statement, expected);
        }
    }

    private boolean containsHighImpactSql(String source)
    {
        String withoutLineComments = stripLineComments(source);
        return HIGH_IMPACT_SQL_HINT.matcher(withoutLineComments).find()
                || DYNAMIC_HIGH_IMPACT_SQL_HINT.matcher(withoutLineComments).find();
    }

    private String stripLineComments(String source)
    {
        StringBuilder builder = new StringBuilder(source.length());
        for (String line : source.split("\\R", -1))
        {
            int commentStart = line.indexOf("--");
            builder.append(commentStart >= 0 ? line.substring(0, commentStart) : line).append('\n');
        }
        return builder.toString();
    }

    private void assertAutoDiscoveredGuard(Path sqlFile, String source, List<String> violations)
    {
        String fileName = sqlFile.getFileName().toString();
        if (!source.contains("set @confirm_"))
        {
            violations.add(fileName + " must set a @confirm_* token");
        }
        if (!source.contains("@confirm_"))
        {
            violations.add(fileName + " must declare a @confirm_* variable");
        }
        if (!source.contains("signal sqlstate '45000'"))
        {
            violations.add(fileName + " must signal when confirmation is missing");
        }
        if (!CONFIRM_CALL.matcher(source).find())
        {
            violations.add(fileName + " must call assert_*_confirmed() before DDL/DML");
        }
        assertConfirmationCallBeforeDml(sqlFile, source, violations);
    }

    private void assertBootstrapOnlySql(Path sqlFile, List<String> violations) throws IOException
    {
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        String marker = "URILI_BOOTSTRAP_ONLY_SQL";

        requireContains(violations, fileName, source, marker);
        requireContains(violations, fileName, source, "bootstrap-only baseline initialization");
        requireContains(violations, fileName, source, "fresh database initialization only");
        requireContains(violations, fileName, source, "must not be treated as an incremental migration");
        if (!DESTRUCTIVE_DROP_TABLE_STATEMENT.matcher(source).find())
        {
            violations.add(fileName + " is marked bootstrap-only but does not contain destructive DROP TABLE");
        }
        assertAppearsBefore(violations, fileName, source.toLowerCase(), marker.toLowerCase(), "drop table if exists");
    }

    private void assertAccountLockMenuGuard(Path sqlFile, int menuId, int parentId, String perms,
            String signatureMessage, List<String> violations) throws IOException
    {
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "create procedure assert_sys_menu_slot");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "create procedure assert_sys_menu_signature_available");
        String parentGuard = parentId == 2011 ? "assert_partner_seller_parent_menu_ready"
                : "assert_partner_buyer_parent_menu_ready";
        String parentPerm = parentId == 2011 ? "seller:admin:list" : "buyer:admin:list";
        String parentMessage = parentId == 2011 ? "partner seller parent menu signature does not match expected"
                : "partner buyer parent menu signature does not match expected";
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "create procedure " + parentGuard);
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "coalesce(route_name, '') = 'PartnerManagement'");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "coalesce(perms, '') = '" + parentPerm + "'");
        requireContains(violations, sqlFile.getFileName().toString(), source, parentMessage);
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "coalesce(parent_id, -1) <> p_parent_id");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "coalesce(menu_type, '') <> coalesce(p_menu_type, '')");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_slot(" + menuId + ", " + parentId + ", 'F', '#', '', '', '" + perms + "'");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_signature_available(" + menuId + ", '#', '', '', '" + perms + "'");
        requireContains(violations, sqlFile.getFileName().toString(), source, signatureMessage);
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call " + parentGuard + "();", "call assert_sys_menu_slot(" + menuId);
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call " + parentGuard + "();", "call add_column_if_missing(");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_slot(" + menuId, "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_slot(" + menuId, "call add_column_if_missing(");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_signature_available(" + menuId, "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_signature_available(" + menuId, "call add_column_if_missing(");
    }

    private void assertAccountLockNormalizeGuard(Path sqlFile, String terminal, String tableName,
            String accountIdColumn, String subjectIdColumn, List<String> violations) throws IOException
    {
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        String procedureName = "assert_" + terminal + "_account_lock_normalize_targets";
        String columnsReadyProcedureName = "assert_" + terminal + "_account_lock_columns_ready";
        String indexReadyProcedureName = "assert_" + terminal + "_account_lock_index_ready";
        String expectedCount = "@" + terminal + "_account_lock_normalize_expected_count";
        String expectedSignature = "@" + terminal + "_account_lock_normalize_expected_signature";

        for (String expected : new String[] {
                "set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);",
                expectedCount,
                expectedSignature,
                "create procedure " + procedureName,
                "create procedure " + columnsReadyProcedureName,
                "create procedure " + indexReadyProcedureName,
                "sha2(coalesce(group_concat(",
                accountIdColumn,
                subjectIdColumn,
                "coalesce(user_name, '')",
                "coalesce(lock_status, '')",
                "coalesce(lock_reason, '')",
                "and column_name = 'lock_status'",
                "and column_type = 'char(1)'",
                "and column_name = 'lock_reason'",
                "and column_type = 'varchar(500)'",
                "seq_in_index = 1 and column_name = '" + subjectIdColumn + "'",
                "seq_in_index = 2 and column_name = 'lock_status'",
                tableName + ".lock_status definition does not match expected",
                tableName + ".lock_reason definition does not match expected",
                tableName + " lock index definition does not match expected",
                tableName + " lock normalize exact target count mismatch",
                tableName + " lock normalize exact target signature mismatch",
                "call " + columnsReadyProcedureName + "();",
                "call " + procedureName + "();",
                "call " + indexReadyProcedureName + "();",
                "drop procedure if exists " + columnsReadyProcedureName + ";",
                "drop procedure if exists " + indexReadyProcedureName + ";",
                "drop procedure if exists " + procedureName + ";"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call add_column_if_missing(\n  '" + tableName + "',\n  'lock_reason',",
                "call " + columnsReadyProcedureName + "();");
        assertAppearsBefore(violations, fileName, source,
                "call " + columnsReadyProcedureName + "();", "call " + procedureName + "();");
        assertAppearsBefore(violations, fileName, source,
                "  'lock_reason',", "call " + procedureName + "();");
        assertAppearsBefore(violations, fileName, source,
                "call " + procedureName + "();", "update " + tableName);
        assertAppearsBefore(violations, fileName, source,
                "create procedure " + procedureName, "call " + procedureName + "();");
        assertAppearsBefore(violations, fileName, source,
                "call add_index_if_missing(\n  '" + tableName + "',",
                "call " + indexReadyProcedureName + "();");
    }

    private void assertTerminalPermissionSeedMenuGuard(Path sqlFile, String[] sellerPerms, String[] buyerPerms,
            List<String> violations) throws IOException
    {
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();

        if (sellerPerms.length > 0)
        {
            assertTerminalPermissionSeedMenuGuard(source, fileName, "seller", sellerPerms, violations);
        }
        if (buyerPerms.length > 0)
        {
            assertTerminalPermissionSeedMenuGuard(source, fileName, "buyer", buyerPerms, violations);
        }
    }

    private void assertTerminalPermissionSeedMenuOnlyGuard(Path sqlFile, String[] sellerPerms, String[] buyerPerms,
            List<String> violations) throws IOException
    {
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();

        if (sellerPerms.length > 0)
        {
            assertTerminalPermissionSeedMenuOnlyGuard(source, fileName, "seller", sellerPerms, violations);
        }
        if (buyerPerms.length > 0)
        {
            assertTerminalPermissionSeedMenuOnlyGuard(source, fileName, "buyer", buyerPerms, violations);
        }
    }

    private void assertSplitTerminalPermissionSeedPreflight(Path sqlFile, boolean expectSeller, boolean expectBuyer,
            List<String> violations) throws IOException
    {
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();

        if (expectSeller)
        {
            assertSplitTerminalPermissionSeedPreflight(source, fileName, "seller", violations);
        }
        if (expectBuyer)
        {
            assertSplitTerminalPermissionSeedPreflight(source, fileName, "buyer", violations);
        }
    }

    private void assertSplitTerminalPermissionSeedPreflight(String source, String fileName, String terminal,
            List<String> violations)
    {
        String menuTable = terminal + "_menu";
        String roleMenuTable = terminal + "_role_menu";
        String componentRoot = Character.toUpperCase(terminal.charAt(0)) + terminal.substring(1) + "/";

        for (String expected : new String[] {
                menuTable + " contains invalid terminal perms",
                menuTable + " page menus require component under " + componentRoot,
                "coalesce(trim(component), '') not like '" + componentRoot + "%'",
                menuTable + " perms must be unique before terminal role grants",
                "duplicate_" + menuTable + "_perms"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        if (source.contains("insert into " + roleMenuTable))
        {
            assertAppearsBefore(violations, fileName, source,
                    "call assert_terminal_menu_range_ready();", "insert into " + roleMenuTable);
        }
        else
        {
            assertAppearsBefore(violations, fileName, source,
                    "call assert_terminal_menu_range_ready();", "insert into " + menuTable);
        }
    }

    private void assertSplitTerminalPermissionSeedTransaction(Path sqlFile, String completedProcedure,
            String firstDml, List<String> violations) throws IOException
    {
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();

        for (String expected : new String[] {
                "create procedure " + completedProcedure,
                "start transaction;",
                "call " + completedProcedure + "();",
                "commit;",
                "drop procedure if exists " + completedProcedure + ";"
        })
        {
            requireContains(violations, fileName, source, expected);
        }
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_menu_range_ready();", "start transaction;");
        assertAppearsBefore(violations, fileName, source,
                "start transaction;", firstDml);
        for (String dml : new String[] {
                "insert into seller_menu",
                "insert into buyer_menu",
                "insert into seller_role",
                "insert into buyer_role",
                "insert into seller_role_menu",
                "insert into buyer_role_menu"
        })
        {
            if (source.contains(dml))
            {
                assertAppearsBefore(violations, fileName, source,
                        "start transaction;", dml);
                assertAppearsBefore(violations, fileName, source,
                        dml, "call " + completedProcedure + "();");
            }
        }
        assertAppearsBefore(violations, fileName, source,
                "call " + completedProcedure + "();", "commit;");
        assertAppearsBefore(violations, fileName, source,
                "commit;", "drop procedure if exists " + completedProcedure + ";");
    }

    private void assertCreateTableContains(String source, String fileName, String tableName, String[] expectedFragments,
            List<String> violations)
    {
        String createPrefix = "create table if not exists " + tableName + " (";
        int startIndex = source.indexOf(createPrefix);
        if (startIndex < 0)
        {
            violations.add(fileName + " must create " + tableName);
            return;
        }
        int endIndex = source.indexOf(") engine=innodb", startIndex);
        if (endIndex < 0)
        {
            violations.add(fileName + " must finish create table " + tableName + " with engine=innodb");
            return;
        }

        String tableSource = source.substring(startIndex, endIndex);
        for (String expected : expectedFragments)
        {
            requireContains(violations, fileName + " " + tableName, tableSource, expected);
        }
    }

    private void assertTerminalPermissionSeedMenuGuard(String source, String fileName, String terminal, String[] perms,
            List<String> violations)
    {
        String menuTable = terminal + "_menu";
        String roleMenuTable = terminal + "_role_menu";
        String assertProcedure = "assert_" + terminal + "_menu_permission_slot";

        requireContains(violations, fileName, source, "create procedure " + assertProcedure);
        for (String perm : perms)
        {
            requireContains(violations, fileName, source,
                    "call " + assertProcedure + "('" + perm + "', 0, 'F', '', null, ''");
            requireContains(violations, fileName, source, perm + " menu slot is occupied by another signature");
        }
        requireContains(violations, fileName, source, "from " + roleMenuTable + " rm");
        requireContains(violations, fileName, source, "join " + menuTable + " m on m.perms");
        requireContains(violations, fileName, source, "coalesce(parent_id, -1) <> p_parent_id");
        requireContains(violations, fileName, source, "and m.parent_id = 0");
        requireContains(violations, fileName, source, "and coalesce(m.menu_type, '') = 'F'");
        requireContains(violations, fileName, source, "and coalesce(m.path, '') = ''");
        requireContains(violations, fileName, source, "and coalesce(m.component, '') = ''");
        requireContains(violations, fileName, source, "and coalesce(m.route_name, '') = ''");
        requireContains(violations, fileName, source, "and r.role_key = 'owner'");
        assertAppearsBefore(violations, fileName, source,
                "call " + assertProcedure + "('" + perms[0] + "'", "insert into " + menuTable);
        assertAppearsBefore(violations, fileName, source,
                "call " + assertProcedure + "('" + perms[0] + "'", "insert into " + roleMenuTable);
    }

    private void assertTerminalPermissionSeedMenuOnlyGuard(String source, String fileName, String terminal,
            String[] perms, List<String> violations)
    {
        String menuTable = terminal + "_menu";
        String roleMenuTable = terminal + "_role_menu";
        String assertProcedure = "assert_" + terminal + "_menu_permission_slot";

        requireContains(violations, fileName, source, "create procedure " + assertProcedure);
        for (String perm : perms)
        {
            requireContains(violations, fileName, source,
                    "call " + assertProcedure + "('" + perm + "', 0, 'F', '', null, ''");
            requireContains(violations, fileName, source, perm + " menu slot is occupied by another signature");
        }
        requireContains(violations, fileName, source, "insert into " + menuTable);
        requireNotContains(violations, fileName, source, "insert into " + roleMenuTable);
        requireNotContains(violations, fileName, source, "owner roles must have product");
        requireContains(violations, fileName, source, "Do not default-grant");
        assertAppearsBefore(violations, fileName, source,
                "call " + assertProcedure + "('" + perms[0] + "'", "insert into " + menuTable);
    }

    private void assertSelfManagementRootButtonGrantExcludesPortalHome(String source, String fileName, String terminal,
            List<String> violations)
    {
        String roleTable = terminal + "_role";
        String menuTable = terminal + "_menu";
        String prefix = "from " + roleTable + " r\njoin " + menuTable + " m on m.perms in (";
        int searchStart = 0;
        while (searchStart < source.length())
        {
            int startIndex = source.indexOf(prefix, searchStart);
            if (startIndex < 0)
            {
                break;
            }
            int endIndex = source.indexOf("where r.del_flag", startIndex);
            if (endIndex < 0)
            {
                violations.add(fileName + " has unterminated " + terminal + " root F owner grant block");
                return;
            }
            String block = source.substring(startIndex, endIndex);
            if (block.contains("'" + terminal + ":account:") || block.contains("'" + terminal + ":dept:")
                    || block.contains("'" + terminal + ":role:"))
            {
                requireNotContains(violations, fileName + " " + terminal + " root F owner grant", block,
                        "'" + terminal + ":portal:home'");
                requireContains(violations, fileName + " " + terminal + " root F owner grant", block,
                        "and m.parent_id = 0");
                requireContains(violations, fileName + " " + terminal + " root F owner grant", block,
                        "and coalesce(m.menu_type, '') = 'F'");
                return;
            }
            searchStart = endIndex;
        }

        violations.add(fileName + " must keep a separate root F owner grant block for " + terminal
                + " self-management button/list permissions");
    }

    private void assertAutoDiscoveredTerminalMenuSeed(String source, String fileName, String terminal,
            List<String> violations)
    {
        String menuTable = terminal + "_menu";
        String roleMenuTable = terminal + "_role_menu";
        String assertProcedure = "assert_" + terminal + "_menu_permission_slot";
        String otherTerminal = "seller".equals(terminal) ? "buyer" : "seller";
        Matcher matcher = TERMINAL_MENU_MUTATION.matcher(source);
        boolean hasMenuMutation = false;
        String firstMutationStatement = null;

        while (matcher.find())
        {
            String matchedTable = firstNonNull(matcher.group(1), matcher.group(2), matcher.group(3));
            if (!menuTable.equals(matchedTable))
            {
                continue;
            }
            hasMenuMutation = true;
            String statement = matcher.group();
            if (firstMutationStatement == null)
            {
                firstMutationStatement = statement;
            }
            String statementName = fileName + " " + menuTable + " mutation";
            requireNotContains(violations, statementName, statement, "'" + terminal + ":admin:");
            requireNotContains(violations, statementName, statement, "'" + otherTerminal + ":");
            requireNotContains(violations, statementName, statement, "'*'");
            if (matcher.group(1) != null)
            {
                requireContains(violations, statementName, statement, "menu_type");
                requireContains(violations, statementName, statement, "perms");
                requireContains(violations, statementName, statement, "'" + terminal + ":");
                if (TERMINAL_PAGE_MENU_WITH_BLANK_COMPONENT.matcher(statement).find())
                {
                    violations.add(statementName + " must not insert a C menu with blank component");
                }
            }
            else if (matcher.group(2) != null)
            {
                if (TERMINAL_MENU_UPDATE_WITH_BLANK_COMPONENT.matcher(statement).find())
                {
                    violations.add(statementName + " must not update terminal menu component to blank");
                }
                if (TERMINAL_MENU_UPDATE_WITH_BLANK_PERMS.matcher(statement).find())
                {
                    violations.add(statementName + " must not update terminal menu perms to blank");
                }
            }
        }

        if (!hasMenuMutation)
        {
            return;
        }

        requireContains(violations, fileName, source, "create procedure " + assertProcedure);
        requireContains(violations, fileName, source, "call " + assertProcedure + "('" + terminal + ":");
        assertAppearsBefore(violations, fileName, source, "call " + assertProcedure + "('" + terminal + ":",
                firstMutationStatement);

        if (source.contains("insert into " + roleMenuTable))
        {
            requireContains(violations, fileName, source, "join " + menuTable + " m on m.perms");
            requireContains(violations, fileName, source, "and m.parent_id =");
            requireContains(violations, fileName, source, "and coalesce(m.menu_type, '')");
            requireContains(violations, fileName, source, "and coalesce(m.component, '')");
            assertAppearsBefore(violations, fileName, source, "call " + assertProcedure + "('" + terminal + ":",
                    "insert into " + roleMenuTable);
        }
    }

    private boolean isTerminalMenuMaintenanceSql(String fileName)
    {
        return "20260607_terminal_menu_id_range_isolation.sql".equals(fileName)
                || "20260608_terminal_menu_auto_increment_reset.sql".equals(fileName);
    }

    private String firstNonNull(String first, String second, String third)
    {
        if (first != null)
        {
            return first;
        }
        if (second != null)
        {
            return second;
        }
        return third;
    }

    private void assertAppearsBefore(List<String> violations, String fileName, String source, String first,
            String second)
    {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        if (firstIndex < 0 || secondIndex < 0 || firstIndex > secondIndex)
        {
            violations.add(fileName + " must contain " + first + " before " + second);
        }
    }

    private void assertGuard(Path backendRoot, String relativeSqlPath, String variableName, String confirmToken,
            List<String> violations) throws IOException
    {
        Path sqlFile = backendRoot.resolve(relativeSqlPath);
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        if (!source.contains("set " + variableName))
        {
            violations.add(sqlFile.getFileName() + " must set " + variableName);
        }
        if (!source.contains(confirmToken))
        {
            violations.add(sqlFile.getFileName() + " must require " + confirmToken);
        }
        if (!source.contains("signal sqlstate '45000'"))
        {
            violations.add(sqlFile.getFileName() + " must signal when confirmation is missing");
        }
        if (!CONFIRM_CALL.matcher(source).find())
        {
            violations.add(sqlFile.getFileName() + " must call its confirmation procedure before DDL/DML");
        }
        assertConfirmationCallBeforeDml(sqlFile, source, violations);
    }

    private void assertConfirmationCallBeforeDml(Path sqlFile, String source, List<String> violations)
    {
        String lowerSource = source.toLowerCase();
        int executableStart = lowerSource.indexOf("delimiter ;");
        String executableSource = executableStart >= 0 ? source.substring(executableStart) : source;
        int firstHighImpactStatement = firstHighImpactStatementIndex(executableSource);
        if (firstHighImpactStatement < 0)
        {
            return;
        }
        Matcher confirmMatcher = CONFIRM_CALL_NAME.matcher(executableSource);
        if (!confirmMatcher.find() || confirmMatcher.start() > firstHighImpactStatement)
        {
            violations.add(sqlFile.getFileName() + " must call its confirmation procedure before first DDL/DML");
            return;
        }

        Set<String> declaredConfirmProcedures = findDeclaredConfirmProcedures(source);
        String calledConfirmProcedure = confirmMatcher.group(1).toLowerCase();
        if (declaredConfirmProcedures.isEmpty())
        {
            violations.add(sqlFile.getFileName() + " must declare its confirmation procedure before calling it");
            return;
        }
        if (!declaredConfirmProcedures.contains(calledConfirmProcedure))
        {
            violations.add(sqlFile.getFileName() + " must call its own declared confirmation procedure before first DDL/DML");
        }
    }

    private int firstHighImpactStatementIndex(String source)
    {
        int first = firstMatchStart(HIGH_IMPACT_STATEMENT, source);
        first = minNonNegative(first, firstMatchStart(DYNAMIC_HIGH_IMPACT_SQL_HINT, source));
        first = minNonNegative(first, firstMatchStart(DYNAMIC_HIGH_IMPACT_EXECUTION_POINT, source));
        return first;
    }

    private int firstMatchStart(Pattern pattern, String source)
    {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.start() : -1;
    }

    private int minNonNegative(int left, int right)
    {
        if (left < 0)
        {
            return right;
        }
        if (right < 0)
        {
            return left;
        }
        return Math.min(left, right);
    }

    private Set<String> findDeclaredConfirmProcedures(String source)
    {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = CONFIRMED_PROCEDURE_DECLARATION.matcher(source);
        while (matcher.find())
        {
            result.add(matcher.group(1).toLowerCase());
        }
        return result;
    }

    private Path findWorkspaceRoot()
    {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path[] candidates = new Path[] {
                cwd,
                cwd.resolve("..").normalize(),
                cwd.resolve("../..").normalize()
        };

        for (Path candidate : candidates)
        {
            if (Files.isDirectory(candidate.resolve("RuoYi-Vue"))
                    && Files.isDirectory(candidate.resolve("react-ui")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate E:\\Urili-Ruoyi workspace root from " + cwd);
    }
}
