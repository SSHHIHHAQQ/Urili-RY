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

public class SqlExecutionGuardContractTest
{
    private static final Pattern HIGH_IMPACT_STATEMENT = Pattern.compile(
            "(?im)^\\s*(?:insert(?:\\s+ignore)?\\s+into|replace\\s+into|update\\s+|delete\\s+(?:from\\s+|\\w+\\s+from\\s+)|alter\\s+table|create\\s+table|create\\s+(?:or\\s+replace\\s+)?view|create\\s+(?:unique\\s+)?index|drop\\s+table|drop\\s+view|truncate\\s+table|rename\\s+table)\\b");

    private static final Pattern HIGH_IMPACT_SQL_HINT = Pattern.compile(
            "(?i)\\b(?:insert(?:\\s+ignore)?\\s+into|replace\\s+into|update\\s+|delete\\s+(?:from\\s+|\\w+\\s+from\\s+)|alter\\s+table|create\\s+table|create\\s+(?:or\\s+replace\\s+)?view|create\\s+(?:unique\\s+)?index|drop\\s+table|drop\\s+view|truncate\\s+table|rename\\s+table)\\b");

    private static final Pattern DYNAMIC_HIGH_IMPACT_SQL_HINT = Pattern.compile(
            "(?is)\\bset\\s+@ddl\\s*=\\s*concat\\s*\\(\\s*'\\s*(?:alter\\s+table|create\\s+table|create\\s+(?:or\\s+replace\\s+)?view|create\\s+(?:unique\\s+)?index|drop\\s+table|drop\\s+view|truncate\\s+table|rename\\s+table)\\b");

    private static final Pattern BARE_CREATE_INDEX_STATEMENT = Pattern.compile(
            "(?im)^\\s*create\\s+(?:unique\\s+)?index\\b");

    private static final Pattern DESTRUCTIVE_DROP_TABLE_STATEMENT = Pattern.compile(
            "(?im)^\\s*drop\\s+table\\s+if\\s+exists\\s+");

    private static final Pattern DATED_SQL_FILE = Pattern.compile("20\\d{6}.*\\.sql");

    private static final Pattern CONFIRM_CALL = Pattern.compile(
            "(?i)call\\s+assert_[a-z0-9_]+_confirmed\\s*\\(\\s*\\)\\s*;");

    private static final Pattern TERMINAL_MENU_INSERT = Pattern.compile(
            "(?is)insert\\s+into\\s+(seller_menu|buyer_menu)\\b.*?;");

    private static final Pattern TERMINAL_PAGE_MENU_WITH_BLANK_COMPONENT = Pattern.compile(
            "(?is)(?:\\bnull\\b|'')\\s*,\\s*''\\s*,\\s*''\\s*,\\s*1\\s*,\\s*0\\s*,\\s*'C'");

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
                "legacy sys_role cleanup expected count does not match role_ids",
                "legacy sys_role cleanup exact target signature mismatch",
                "legacy_sys_role_cleanup_expected_signature",
                "call assert_legacy_sys_role_cleanup_targets();"
        })
        {
            requireContains(violations, legacyRoleSql.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, legacyRoleSql.getFileName().toString(), source,
                "call assert_terminal_owner_roles_ready();", "call assert_legacy_sys_role_cleanup_targets();");
        assertAppearsBefore(violations, legacyRoleSql.getFileName().toString(), source,
                "call assert_legacy_sys_role_cleanup_targets();", "update sys_role");

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
                "call assert_admin_partner_button_cleanup_targets();"
        })
        {
            requireContains(violations, cleanupSql.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_button_cleanup_targets();", "delete child_grant");

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
                "call assert_admin_partner_owner_reset_permission_cleanup_targets();"
        })
        {
            requireContains(violations, cleanupSql.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_owner_reset_permission_cleanup_targets();", "delete rm");
        assertAppearsBefore(violations, cleanupSql.getFileName().toString(), source,
                "call assert_admin_partner_owner_reset_permission_cleanup_targets();", "delete m");

        if (!violations.isEmpty())
        {
            fail("admin partner owner reset cleanup must keep preview-confirmed exact target signatures:\n"
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
    public void adminPartnerRoleMenuGrantMustVerifyExactChildMenuSignature() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path grantSql = backendRoot.resolve("sql/20260606_admin_partner_role_menu_grant.sql");
        String source = Files.readString(grantSql, StandardCharsets.UTF_8);
        String fileName = grantSql.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure assert_admin_partner_child_menu_signature",
                "tmp_admin_partner_child_menu_signature",
                "partner child menu signature seed count must be 64",
                "partner sys_menu child button signature does not match expected seller/buyer admin buttons",
                "and m.parent_id = seed.parent_id",
                "and m.perms = seed.perms",
                "(2322, 2011, 'seller:admin:account:lock')",
                "(2323, 2012, 'buyer:admin:account:lock')",
                "call assert_admin_partner_child_menu_signature();",
                "drop procedure if exists assert_admin_partner_child_menu_signature"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "call assert_admin_partner_child_menu_signature();",
                "select distinct r.role_id, child.menu_id");

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
                "unique key uk_seller_menu_perms (perms)",
                "unique key uk_buyer_menu_perms (perms)",
                "seller_menu contains invalid terminal perms",
                "buyer_menu contains invalid terminal perms",
                "seller_menu page menus require component",
                "buyer_menu page menus require component",
                "seller_menu perms must be unique before terminal role grants",
                "buyer_menu perms must be unique before terminal role grants",
                "call add_index_if_missing('seller_menu', 'uk_seller_menu_perms'",
                "call add_index_if_missing('buyer_menu', 'uk_buyer_menu_perms'",
                "call assert_terminal_menu_perms_unique_index('seller_menu', 'uk_seller_menu_perms'",
                "call assert_terminal_menu_perms_unique_index('buyer_menu', 'uk_buyer_menu_perms'"
        })
        {
            requireContains(violations, seedSql.getFileName().toString(), seedSource, expected);
        }
        assertAppearsBefore(violations, seedSql.getFileName().toString(), seedSource,
                "call assert_terminal_menu_range_ready();",
                "call add_index_if_missing('seller_menu', 'uk_seller_menu_perms'");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), seedSource,
                "call assert_terminal_menu_perms_unique_index('seller_menu', 'uk_seller_menu_perms'",
                "insert into seller_role_menu");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), seedSource,
                "call assert_terminal_menu_perms_unique_index('buyer_menu', 'uk_buyer_menu_perms'",
                "insert into buyer_role_menu");

        for (String expected : new String[] {
                "create procedure assert_terminal_menu_integrity_ready",
                "unique key uk_seller_menu_perms (perms)",
                "unique key uk_buyer_menu_perms (perms)",
                "seller_menu contains invalid terminal perms",
                "buyer_menu contains invalid terminal perms",
                "seller_menu page menus require component",
                "buyer_menu page menus require component",
                "seller_menu perms must be unique before terminal role grants",
                "buyer_menu perms must be unique before terminal role grants",
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
    public void inventoryOverviewSkuBaselineRefreshMustRebuildReadModelsInTransaction() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260608_inventory_overview_sku_baseline_refresh.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String fileName = sqlFile.getFileName().toString();
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "start transaction;",
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
    public void terminalPermissionSeedsMustGuardMenuSlotsBeforeRoleBinding() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();
        String[] sellerBaselinePerms = new String[] {
                "seller:account:list",
                "seller:account:loginLog:list",
                "seller:account:operLog:list",
                "seller:account:session:list",
                "seller:dept:list",
                "seller:role:list",
                "seller:product:category:list",
                "seller:product:schema:query",
                "seller:product:distribution:list",
                "seller:product:distribution:query"
        };
        String[] buyerBaselinePerms = new String[] {
                "buyer:account:list",
                "buyer:account:loginLog:list",
                "buyer:account:operLog:list",
                "buyer:account:session:list",
                "buyer:dept:list",
                "buyer:role:list",
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
        assertTerminalPermissionSeedMenuGuard(backendRoot.resolve("sql/20260604_portal_product_category_permission_seed.sql"),
                new String[] {"seller:product:category:list"},
                new String[] {"buyer:product:category:list"}, violations);
        assertTerminalPermissionSeedMenuGuard(backendRoot.resolve("sql/20260604_seller_product_schema_permission_seed.sql"),
                new String[] {"seller:product:schema:query"}, new String[0], violations);
        assertTerminalPermissionSeedMenuGuard(backendRoot.resolve("sql/20260604_buyer_product_schema_permission_seed.sql"),
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

        if (!violations.isEmpty())
        {
            fail("terminal permission seeds must fail-closed guard seller_menu/buyer_menu signatures:\n"
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
                "create procedure assert_terminal_menu_ids_are_migratable",
                "create procedure assert_terminal_role_menu_ids_are_migratable",
                "create procedure assert_terminal_menu_ids_are_in_final_ranges",
                "create procedure assert_terminal_role_menu_ids_are_in_final_ranges",
                "create procedure reset_terminal_menu_auto_increment",
                "call assert_no_terminal_menu_orphans();",
                "call assert_terminal_menu_ids_are_migratable();",
                "call assert_terminal_role_menu_ids_are_migratable();",
                "update seller_role_menu\nset seller_menu_id = seller_menu_id + 100000",
                "update seller_menu\nset parent_id = parent_id + 100000",
                "update seller_menu\nset seller_menu_id = seller_menu_id + 100000",
                "update buyer_role_menu\nset buyer_menu_id = buyer_menu_id + 200000",
                "update buyer_menu\nset parent_id = parent_id + 200000",
                "update buyer_menu\nset buyer_menu_id = buyer_menu_id + 200000",
                "call assert_terminal_menu_ids_are_in_final_ranges();",
                "call assert_terminal_role_menu_ids_are_in_final_ranges();",
                "call reset_terminal_menu_auto_increment('seller_menu', 'seller_menu_id', 100000);",
                "call reset_terminal_menu_auto_increment('buyer_menu', 'buyer_menu_id', 200000);"
        })
        {
            requireContains(violations, fileName, source, expected);
        }

        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_menu_ids_are_migratable();",
                "call assert_terminal_role_menu_ids_are_migratable();");
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_role_menu_ids_are_migratable();", "start transaction;");
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
        assertAppearsBefore(violations, fileName, source,
                "call assert_terminal_role_menu_ids_are_in_final_ranges();",
                "call reset_terminal_menu_auto_increment('seller_menu', 'seller_menu_id', 100000);");
        assertAppearsBefore(violations, fileName, source,
                "call reset_terminal_menu_auto_increment('buyer_menu', 'buyer_menu_id', 200000);",
                "\ncommit;");

        if (!violations.isEmpty())
        {
            fail("terminal menu ID range isolation must preserve scoped role-menu, parent, ID, and auto-increment migration order:\n"
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
                "drop temporary table if exists tmp_warehouse_management_sys_menu_guard",
                "drop procedure if exists assert_warehouse_management_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_warehouse_management_sys_menu_guard();", "insert into sys_menu");

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
                "tmp_product_category_attribute_sys_menu_guard",
                "product category attribute sys_menu id slot is occupied by another menu",
                "product category attribute sys_menu signature is already used by another menu",
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
                "drop temporary table if exists tmp_product_category_attribute_sys_menu_guard",
                "drop procedure if exists assert_product_category_attribute_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_category_attribute_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_product_category_attribute_sys_menu_guard();", "update sys_menu");

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
                "(2400, 2060, 'C', 'list', 'Product/SourceProductLibrary/index', 'SourceProductLibrary', 'product:list:list')",
                "(2452, 2100, 'C', 'inventory-adjustment', 'Common/PlannedPage/index', 'InventoryAdjustmentReview', 'review:inventoryAdjustment:list')",
                "call assert_business_menu_sys_menu_guard();",
                "drop temporary table if exists tmp_business_menu_sys_menu_guard",
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
                "create procedure assert_source_product_library_menu_component_guard",
                "tmp_source_product_library_menu_component_guard",
                "source product library menu component sys_menu id slot is occupied by another menu",
                "source product library menu component sys_menu signature is already used by another menu",
                "(2400, 'list', 'Product/SourceProductLibrary/index'",
                "'SourceProductLibrary', 'product:list:list', 'C')",
                "(2400, 'list', 'Common/PlannedPage/index'",
                "'ProductList', 'product:list:list', 'C')",
                "call assert_source_product_library_menu_component_guard();",
                "drop temporary table if exists tmp_source_product_library_menu_component_guard",
                "drop procedure if exists assert_source_product_library_menu_component_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_product_library_menu_component_guard();", "update sys_menu");

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
                "create procedure assert_mall_product_distribution_sys_menu_guard",
                "tmp_mall_product_distribution_sys_menu_guard",
                "mall product distribution sys_menu id slot is occupied by another menu",
                "mall product distribution sys_menu signature is already used by another menu",
                "and m.parent_id = seed.parent_id",
                "and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')",
                "insert into tmp_mall_product_distribution_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(2402, 2060, 'C', 'distribution', 'Product/Distribution/index', 'DistributionProduct', 'product:distribution:list')",
                "call assert_mall_product_distribution_sys_menu_guard();",
                "(2402, '商城商品列表', 2060, 15, 'distribution', 'Product/Distribution/index'",
                "product:distribution:list",
                "union all select 2485, '商城商品调价', 5, 'product:distribution:price'",
                "union all select 2486, '商城商品操作日志', 6, 'product:distribution:log'",
                "drop temporary table if exists tmp_mall_product_distribution_sys_menu_guard",
                "drop procedure if exists assert_mall_product_distribution_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_mall_product_distribution_sys_menu_guard();", "insert into sys_menu");

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
                "create procedure assert_order_after_sale_sys_menu_guard",
                "tmp_order_after_sale_sys_menu_guard",
                "order after-sale sys_menu id slot is occupied by another menu",
                "order after-sale sys_menu signature is already used by another menu",
                "and m.parent_id = seed.parent_id",
                "and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')",
                "insert into tmp_order_after_sale_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(2412, 2070, 'C', 'after-sale', 'Common/PlannedPage/index', 'AfterSaleManagement', 'order:afterSale:list')",
                "call assert_order_after_sale_sys_menu_guard();",
                "(2412, '售后管理', 2070, 15, 'after-sale', 'Common/PlannedPage/index'",
                "order:afterSale:list",
                "drop temporary table if exists tmp_order_after_sale_sys_menu_guard",
                "drop procedure if exists assert_order_after_sale_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_order_after_sale_sys_menu_guard();", "insert into sys_menu");

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
                "create procedure assert_source_warehouse_stock_sys_menu_guard",
                "tmp_source_warehouse_stock_sys_menu_guard",
                "source warehouse stock sys_menu id slot is occupied by another menu",
                "source warehouse stock sys_menu signature is already used by another menu",
                "and m.parent_id = seed.parent_id",
                "and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')",
                "insert into tmp_source_warehouse_stock_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms)",
                "(2421, 2080, 'C', 'source-warehouse-stock', 'Inventory/SourceWarehouseStock/index', 'SourceWarehouseStock', 'inventory:sourceWarehouse:list')",
                "(2421, 2080, 'C', 'source-warehouse-stock', 'Common/PlannedPage/index', 'SourceWarehouseStock', 'inventory:sourceWarehouse:list')",
                "call assert_source_warehouse_stock_sys_menu_guard();",
                "update sys_menu",
                "where menu_id = 2421",
                "select 2421, '来源仓库库存', 2080, 10, 'source-warehouse-stock', 'Inventory/SourceWarehouseStock/index'",
                "where not exists (select 1 from sys_menu where menu_id = 2421)",
                "drop temporary table if exists tmp_source_warehouse_stock_sys_menu_guard",
                "drop procedure if exists assert_source_warehouse_stock_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_source_warehouse_stock_sys_menu_guard();", "update sys_menu");
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
                "drop temporary table if exists tmp_upstream_system_management_sys_menu_guard",
                "drop procedure if exists assert_upstream_system_management_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_upstream_system_management_sys_menu_guard();", "insert into sys_menu");

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
        requireNotContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "(2010, '主体管理', 0, 5, 'partner', null, '', 'PartnerManagement'");
        requireNotContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_sys_menu_slot(2010");
        requireNotContains(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_sys_menu_signature_available(2010");
        assertAppearsBefore(violations, directLoginSeed.getFileName().toString(), directLoginSource,
                "call assert_partner_root_menu_exists();", "insert into sys_menu");

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
                "drop temporary table if exists tmp_top_menu_sys_menu_guard",
                "drop procedure if exists assert_top_menu_sys_menu_guard"
        })
        {
            requireContains(violations, sqlFile.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_top_menu_sys_menu_guard();", "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_top_menu_sys_menu_guard();", "update sys_menu\nset order_num = 90");

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
                "drop temporary table if exists tmp_currency_configuration_sys_menu_guard",
                "drop procedure if exists assert_currency_configuration_sys_menu_guard"
        })
        {
            requireContains(violations, currencySeed.getFileName().toString(), currencySource, expected);
        }
        assertAppearsBefore(violations, currencySeed.getFileName().toString(), currencySource,
                "call assert_currency_configuration_sys_menu_guard();", "insert into sys_menu");

        if (!violations.isEmpty())
        {
            fail("currency menu seed must have a single finance owner and guard sys_menu slots before upsert:\n"
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
    public void portalDirectLoginTicketMigrationMustUseReplaySafeModifyHelper() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path sqlFile = backendRoot.resolve("sql/20260604_portal_direct_login_ticket.sql");
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        String normalizedSource = source.replace("\r\n", "\n");
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "create procedure modify_portal_direct_login_ticket_columns_if_needed",
                "portal_direct_login_ticket expected columns are required before ticket column modify",
                "lower(c.data_type) <> expected.expected_type",
                "coalesce(c.character_maximum_length, -1) <> expected.expected_length",
                "c.is_nullable <> expected.expected_nullable",
                "coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>')",
                "alter table portal_direct_login_ticket modify terminal varchar(20) not null",
                "modify token_hash varchar(64) not null",
                "modify expire_time datetime not null",
                "call modify_portal_direct_login_ticket_columns_if_needed();",
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
                "call modify_portal_direct_login_ticket_columns_if_needed();",
                "call recreate_index_if_mismatch('portal_direct_login_ticket', 'uk_portal_direct_login_ticket_hash'");

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
                "create procedure add_column_if_missing",
                "create procedure modify_product_sku_sale_price_if_needed",
                "product_sku.sale_price column is required before product distribution status price migration",
                "data_type <> 'decimal'",
                "numeric_precision <> 18",
                "numeric_scale <> 4",
                "is_nullable <> 'YES'",
                "column_comment <> '销售价'",
                "alter table product_sku modify column sale_price decimal(18,4) null comment '销售价'",
                "call add_column_if_missing('product_spu', 'control_status'",
                "call add_column_if_missing('product_sku', 'control_status'",
                "call modify_product_sku_sale_price_if_needed();",
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
                "create procedure add_column_if_missing",
                "create procedure assert_showapi_provider_conflict_absent",
                "finance currency sync config has both GENERIC_RATES and SHOWAPI_BANK_RATE",
                "call add_column_if_missing('finance_currency_sync_config', 'rate_anchor_time'",
                "call assert_showapi_provider_conflict_absent();",
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
                "update finance_currency_sync_config");

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
                "create procedure add_column_if_missing",
                "call add_column_if_missing('product_spu', 'product_name_en'",
                "call add_column_if_missing('product_spu', 'detail_content'",
                "drop procedure if exists add_column_if_missing"
        })
        {
            requireContains(violations, editorSampleSql.getFileName().toString(), editorSampleSource, expected);
        }
        requireNotContains(violations, editorSampleSql.getFileName().toString(), editorSampleSource.replace("\r\n", "\n"),
                "alter table product_spu\n  add column");
        assertAppearsBefore(violations, editorSampleSql.getFileName().toString(), editorSampleSource,
                "call add_column_if_missing('product_spu', 'detail_content'",
                "update product_spu");

        if (!violations.isEmpty())
        {
            fail("follow-up scripts with columns already absorbed by seed baselines must be safe to replay:\n"
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
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "parent_id <> p_parent_id");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "coalesce(menu_type, '') <> coalesce(p_menu_type, '')");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_slot(" + menuId + ", " + parentId + ", 'F', '#', '', '', '" + perms + "'");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_signature_available(" + menuId + ", '#', '', '', '" + perms + "'");
        requireContains(violations, sqlFile.getFileName().toString(), source, signatureMessage);
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_slot(" + menuId, "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_signature_available(" + menuId, "insert into sys_menu");
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
        requireContains(violations, fileName, source, "and m.parent_id = 0");
        requireContains(violations, fileName, source, "and coalesce(m.menu_type, '') = 'F'");
        requireContains(violations, fileName, source, "and coalesce(m.path, '') = ''");
        requireContains(violations, fileName, source, "and coalesce(m.component, '') = ''");
        requireContains(violations, fileName, source, "and coalesce(m.route_name, '') = ''");
        assertAppearsBefore(violations, fileName, source,
                "call " + assertProcedure + "('" + perms[0] + "'", "insert into " + menuTable);
        assertAppearsBefore(violations, fileName, source,
                "call " + assertProcedure + "('" + perms[0] + "'", "insert into " + roleMenuTable);
    }

    private void assertAutoDiscoveredTerminalMenuSeed(String source, String fileName, String terminal,
            List<String> violations)
    {
        String menuTable = terminal + "_menu";
        String roleMenuTable = terminal + "_role_menu";
        String assertProcedure = "assert_" + terminal + "_menu_permission_slot";
        String otherTerminal = "seller".equals(terminal) ? "buyer" : "seller";
        Matcher matcher = TERMINAL_MENU_INSERT.matcher(source);
        boolean hasMenuInsert = false;

        while (matcher.find())
        {
            if (!menuTable.equals(matcher.group(1)))
            {
                continue;
            }
            hasMenuInsert = true;
            String statement = matcher.group();
            String statementName = fileName + " " + menuTable + " insert";
            requireContains(violations, statementName, statement, "menu_type");
            requireContains(violations, statementName, statement, "perms");
            requireContains(violations, statementName, statement, "'" + terminal + ":");
            requireNotContains(violations, statementName, statement, "'" + terminal + ":admin:");
            requireNotContains(violations, statementName, statement, "'" + otherTerminal + ":");
            requireNotContains(violations, statementName, statement, "'*'");
            if (TERMINAL_PAGE_MENU_WITH_BLANK_COMPONENT.matcher(statement).find())
            {
                violations.add(statementName + " must not insert a C menu with blank component");
            }
        }

        if (!hasMenuInsert)
        {
            return;
        }

        requireContains(violations, fileName, source, "create procedure " + assertProcedure);
        requireContains(violations, fileName, source, "call " + assertProcedure + "('" + terminal + ":");
        assertAppearsBefore(violations, fileName, source, "call " + assertProcedure + "('" + terminal + ":",
                "insert into " + menuTable);

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
        if (!source.contains("call assert_") || !source.contains("_confirmed();"))
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
        Matcher matcher = HIGH_IMPACT_STATEMENT.matcher(executableSource);
        if (!matcher.find())
        {
            return;
        }
        int confirmCall = executableSource.indexOf("call assert_");
        int firstHighImpactStatement = matcher.start();
        if (confirmCall < 0 || confirmCall > firstHighImpactStatement)
        {
            violations.add(sqlFile.getFileName() + " must call its confirmation procedure before first DDL/DML");
        }
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
