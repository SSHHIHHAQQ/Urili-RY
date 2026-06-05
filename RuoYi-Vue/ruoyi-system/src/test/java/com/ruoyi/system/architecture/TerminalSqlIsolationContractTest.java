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
