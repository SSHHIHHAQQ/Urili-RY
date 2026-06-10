package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

public class TerminalSeedPermissionContractTest
{
    private static final Pattern PORTAL_PRE_AUTHORIZE = Pattern.compile("@PortalPreAuthorize\\s*\\(([^)]*)\\)",
            Pattern.DOTALL);
    private static final Pattern HAS_PERMI = Pattern.compile("hasPermi\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern TERMINAL_SEED_PERM = Pattern.compile("'((?:seller|buyer):[^']+)'");

    @Test
    public void comprehensiveSeedMustContainTerminalPortalPermissions() throws IOException
    {
        Path workspaceRoot = findWorkspaceRoot();
        Path backendRoot = workspaceRoot.resolve("RuoYi-Vue");
        Path seed = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String sql = Files.readString(seed, StandardCharsets.UTF_8);
        Set<String> seededPerms = readSeededTerminalPerms(sql);
        List<String> violations = new ArrayList<>();

        assertTerminalPortalPermissions(seed, sql, "seller", violations);
        assertTerminalPortalPermissions(seed, sql, "buyer", violations);
        assertSourcePortalPermissionsSeeded(backendRoot, seed, seededPerms, "seller", violations);
        assertSourcePortalPermissionsSeeded(backendRoot, seed, seededPerms, "buyer", violations);
        assertContains(sql, "insert into seller_role", seed, violations);
        assertContains(sql, "insert into buyer_role", seed, violations);
        assertContains(sql, "insert into seller_account_role", seed, violations);
        assertContains(sql, "insert into buyer_account_role", seed, violations);
        assertContains(sql, "insert into seller_role_menu", seed, violations);
        assertContains(sql, "insert into buyer_role_menu", seed, violations);
        assertContains(sql, "set @confirm_seller_buyer_management_seed", seed, violations);
        assertContains(sql, "call assert_seller_buyer_management_seed_confirmed();", seed, violations);
        assertContains(sql, "set @seller_buyer_management_seed_profile", seed, violations);
        assertContains(sql, "FRESH_BOOTSTRAP", seed, violations);
        assertContains(sql, "PATCH_EXISTING", seed, violations);
        assertContains(sql, "call assert_seller_buyer_management_seed_profile();", seed, violations);
        assertContains(sql, "call assert_seller_buyer_sys_menu_seed_guard();", seed, violations);
        assertContains(sql, "tmp_seller_buyer_sys_menu_guard", seed, violations);

        if (!violations.isEmpty())
        {
            fail("seller_buyer_management_seed.sql must initialize terminal portal permissions:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void managementSeedMustNotRestoreSubjectLevelOwnerPasswordResetPermissions() throws IOException
    {
        Path backendRoot = findWorkspaceRoot().resolve("RuoYi-Vue");
        Path seed = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String sql = Files.readString(seed, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertNotContains(sql, "seller:admin:resetPwd", seed, violations);
        assertNotContains(sql, "buyer:admin:resetPwd", seed, violations);
        assertNotContains(sql, "(2204,", seed, violations);
        assertNotContains(sql, "(2214,", seed, violations);
        assertContains(sql, "seller:admin:account:resetPwd", seed, violations);
        assertContains(sql, "buyer:admin:account:resetPwd", seed, violations);

        if (!violations.isEmpty())
        {
            fail("seller/buyer management seed must keep password reset account-scoped:\n"
                    + String.join("\n", violations));
        }
    }

    private Set<String> readSeededTerminalPerms(String sql)
    {
        Matcher matcher = TERMINAL_SEED_PERM.matcher(sql);
        Set<String> perms = new HashSet<>();
        while (matcher.find())
        {
            perms.add(matcher.group(1));
        }
        return perms;
    }

    private void assertSourcePortalPermissionsSeeded(Path backendRoot, Path seed, Set<String> seededPerms,
            String terminal, List<String> violations) throws IOException
    {
        Path sourceRoot = backendRoot.resolve(terminal).resolve("src/main/java");
        try (Stream<Path> paths = Files.walk(sourceRoot))
        {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> assertFilePortalPermissionsSeeded(backendRoot, path, seed, seededPerms,
                            terminal, violations));
        }
    }

    private void assertFilePortalPermissionsSeeded(Path backendRoot, Path path, Path seed, Set<String> seededPerms,
            String terminal, List<String> violations)
    {
        try
        {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            Matcher annotationMatcher = PORTAL_PRE_AUTHORIZE.matcher(source);
            while (annotationMatcher.find())
            {
                String annotation = annotationMatcher.group(1);
                if (!annotation.contains("terminal = \"" + terminal + "\""))
                {
                    continue;
                }
                Matcher permissionMatcher = HAS_PERMI.matcher(annotation);
                while (permissionMatcher.find())
                {
                    String permission = permissionMatcher.group(1);
                    if (!permission.startsWith(terminal + ":"))
                    {
                        violations.add(backendRoot.relativize(path) + " declares " + terminal
                                + " portal permission with wrong prefix: " + permission);
                    }
                    if (!seededPerms.contains(permission))
                    {
                        violations.add(backendRoot.relativize(path) + " declares " + permission
                                + " but " + seed.getFileName() + " does not seed it");
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to read " + path, e);
        }
    }

    private void assertTerminalPortalPermissions(Path seed, String sql, String terminal, List<String> violations)
    {
        assertContains(sql, terminal + ":account:list", seed, violations);
        assertContains(sql, terminal + ":account:add", seed, violations);
        assertContains(sql, terminal + ":account:edit", seed, violations);
        assertContains(sql, terminal + ":account:role:query", seed, violations);
        assertContains(sql, terminal + ":account:role:edit", seed, violations);
        assertContains(sql, terminal + ":account:loginLog:list", seed, violations);
        assertContains(sql, terminal + ":account:operLog:list", seed, violations);
        assertContains(sql, terminal + ":account:session:list", seed, violations);
        assertContains(sql, terminal + ":dept:list", seed, violations);
        assertContains(sql, terminal + ":dept:query", seed, violations);
        assertContains(sql, terminal + ":dept:add", seed, violations);
        assertContains(sql, terminal + ":dept:edit", seed, violations);
        assertContains(sql, terminal + ":dept:remove", seed, violations);
        assertContains(sql, terminal + ":role:list", seed, violations);
        assertContains(sql, terminal + ":role:query", seed, violations);
        assertContains(sql, terminal + ":role:add", seed, violations);
        assertContains(sql, terminal + ":role:edit", seed, violations);
        assertContains(sql, terminal + ":role:remove", seed, violations);
        assertContains(sql, terminal + ":product:category:list", seed, violations);
        assertContains(sql, terminal + ":product:schema:query", seed, violations);
    }

    private void assertContains(String source, String expected, Path path, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(path.getFileName() + " must contain " + expected);
        }
    }

    private void assertNotContains(String source, String forbidden, Path path, List<String> violations)
    {
        if (source.contains(forbidden))
        {
            violations.add(path.getFileName() + " must not contain " + forbidden);
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
