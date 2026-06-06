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
            "(?m)^\\s*(?:insert\\s+into|update\\s+|delete\\s+|alter\\s+table|create\\s+table)\\b");

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
        assertGuard(backendRoot, "sql/20260606_upstream_inventory_dimension_sync.sql",
                "@confirm_upstream_inventory_dimension_sync",
                "APPLY_UPSTREAM_INVENTORY_DIMENSION_SYNC", violations);
        assertGuard(backendRoot, "sql/seller_buyer_management_seed.sql",
                "@confirm_seller_buyer_management_seed",
                "APPLY_SELLER_BUYER_MANAGEMENT_SEED", violations);
        assertGuard(backendRoot, "sql/warehouse_management_seed.sql",
                "@confirm_warehouse_management_seed",
                "APPLY_WAREHOUSE_MANAGEMENT_SEED", violations);
        assertGuard(backendRoot, "sql/top_menu_seed.sql",
                "@confirm_top_menu_seed",
                "APPLY_TOP_MENU_SEED", violations);

        if (!violations.isEmpty())
        {
            fail("high impact SQL scripts must fail closed without explicit confirmation:\n"
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
                "legacy sys_user backfill requires seller_account.user_id or buyer_account.user_id");
        requireContains(violations, legacySql.getFileName().toString(), legacySource,
                "legacy sys_user backfill requires at least one terminal account row with user_id");

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
    public void inventoryPermissionMustNotGrantIntegrationPermissionFromInventoryMenu() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path inventorySql = backendRoot.resolve("sql/20260606_upstream_inventory_dimension_sync.sql");
        Path businessMenuSeed = backendRoot.resolve("sql/business_menu_seed.sql");
        String inventorySource = Files.readString(inventorySql, StandardCharsets.UTF_8);
        String businessMenuSource = Files.readString(businessMenuSeed, StandardCharsets.UTF_8);
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
        if (!businessMenuSource.contains("set component = 'Common/PlannedPage/index'"))
        {
            violations.add(businessMenuSeed.getFileName()
                    + " must keep source warehouse stock as a placeholder by default");
        }

        if (!violations.isEmpty())
        {
            fail("inventory menu and integration permissions must stay separated:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerBuyerManagementSeedMustConvergePortalWebUrlConfig() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String source = Files.readString(seedSql, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        for (String expected : new String[] {
                "update sys_config",
                "config_name = '卖家端前端地址'",
                "config_value = 'http://127.0.0.1:8001/seller/direct-login'",
                "where config_key = 'portal.seller.web.url'",
                "config_name = '买家端前端地址'",
                "config_value = 'http://127.0.0.1:8001/buyer/direct-login'",
                "where config_key = 'portal.buyer.web.url'"
        })
        {
            requireContains(violations, seedSql.getFileName().toString(), source, expected);
        }
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "config_value = 'http://127.0.0.1:8001/seller/direct-login'",
                "select '卖家端前端地址', 'portal.seller.web.url'");
        assertAppearsBefore(violations, seedSql.getFileName().toString(), source,
                "config_value = 'http://127.0.0.1:8001/buyer/direct-login'",
                "select '买家端前端地址', 'portal.buyer.web.url'");

        if (!violations.isEmpty())
        {
            fail("seller/buyer management seed must converge portal web url config:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void accountLockMenuSeedsMustGuardSysMenuSlotsBeforeUpsert() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        List<String> violations = new ArrayList<>();

        assertAccountLockMenuGuard(backendRoot.resolve("sql/20260605_seller_account_lock_control.sql"),
                2322, "seller:admin:account:lock", "seller account lock menu signature", violations);
        assertAccountLockMenuGuard(backendRoot.resolve("sql/20260605_buyer_account_lock_control.sql"),
                2323, "buyer:admin:account:lock", "buyer account lock menu signature", violations);

        if (!violations.isEmpty())
        {
            fail("account lock menu seeds must guard sys_menu slots before upsert:\n"
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

    private void assertAccountLockMenuGuard(Path sqlFile, int menuId, String perms, String signatureMessage,
            List<String> violations) throws IOException
    {
        String source = Files.readString(sqlFile, StandardCharsets.UTF_8);
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "create procedure assert_sys_menu_slot");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "create procedure assert_sys_menu_signature_available");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_slot(" + menuId + ", '#', '', '', '" + perms + "'");
        requireContains(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_signature_available(" + menuId + ", '#', '', '', '" + perms + "'");
        requireContains(violations, sqlFile.getFileName().toString(), source, signatureMessage);
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_slot(" + menuId, "insert into sys_menu");
        assertAppearsBefore(violations, sqlFile.getFileName().toString(), source,
                "call assert_sys_menu_signature_available(" + menuId, "insert into sys_menu");
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
