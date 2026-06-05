package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class AdminAccountPermissionUiContractTest
{
    @Test
    public void sellerAndBuyerAccountActionsMustUseAccountScopedUiPermissions() throws IOException
    {
        Path workspaceRoot = findWorkspaceRoot();
        List<String> violations = new ArrayList<>();

        assertTerminalPageConfig(workspaceRoot, "Seller", "seller", violations);
        assertTerminalPageConfig(workspaceRoot, "Buyer", "buyer", violations);
        assertPartnerManagementPageGates(workspaceRoot, violations);
        assertPartnerAccountModalGates(workspaceRoot, violations);

        if (!violations.isEmpty())
        {
            fail("admin account/session UI actions must be gated by terminal-scoped permissions:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertTerminalPageConfig(Path workspaceRoot, String pageName, String terminal, List<String> violations)
            throws IOException
    {
        Path page = workspaceRoot.resolve("react-ui/src/pages/" + pageName + "/index.tsx");
        String source = Files.readString(page, StandardCharsets.UTF_8);
        assertContains(source, "accountPermissions: {", page, violations);
        assertContains(source, "list: '" + terminal + ":admin:account:list'", page, violations);
        assertContains(source, "add: '" + terminal + ":admin:account:add'", page, violations);
        assertContains(source, "edit: '" + terminal + ":admin:account:edit'", page, violations);
        assertContains(source, "resetPwd: '" + terminal + ":admin:account:resetPwd'", page, violations);
        assertContains(source, "lock: '" + terminal + ":admin:account:lock'", page, violations);
        assertContains(source, "roleQuery: '" + terminal + ":admin:account:role:query'", page, violations);
        assertContains(source, "roleEdit: '" + terminal + ":admin:account:role:edit'", page, violations);
        assertContains(source, "listSubjectSessions: getAdmin" + pageName + "Sessions", page, violations);
        assertContains(source, "listAccountSessions: getAdmin" + pageName + "AccountSessions", page, violations);
        assertContains(source, "forceLogoutSubject: forceLogoutAdmin" + pageName + "Sessions", page, violations);
        assertContains(source, "forceLogoutAccount: forceLogoutAdmin" + pageName + "AccountSessions", page,
                violations);
    }

    private void assertPartnerManagementPageGates(Path workspaceRoot, List<String> violations) throws IOException
    {
        Path page = workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx");
        String source = Files.readString(page, StandardCharsets.UTF_8);
        assertContains(source, "const accountPermissions = config.accountPermissions ??", page, violations);
        assertContains(source, "hidden={!access.hasPerms(accountPermissions.list)}", page, violations);
        assertContains(source, "PartnerSessionModal", page, violations);
        assertContains(source, "config.services.listSubjectSessions", page, violations);
        assertContains(source, "config.services.forceLogoutSubject", page, violations);
        assertContains(source, "access.hasPerms(`${permPrefix}:forceLogout`)", page, violations);
    }

    private void assertPartnerAccountModalGates(Path workspaceRoot, List<String> violations) throws IOException
    {
        Path modal = workspaceRoot.resolve("react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx");
        String source = Files.readString(modal, StandardCharsets.UTF_8);
        assertContains(source, "const accountPermissions = config.accountPermissions ??", modal, violations);
        assertContains(source, "access.hasPerms(accountPermissions.roleQuery)", modal, violations);
        assertContains(source, "access.hasPerms(accountPermissions.roleEdit)", modal, violations);
        assertContains(source, "access.hasPerms(accountPermissions.resetPwd)", modal, violations);
        assertContains(source, "access.hasPerms(accountPermissions.lock", modal, violations);
        assertContains(source, "hidden={!access.hasPerms(accountPermissions.edit)}", modal, violations);
        assertContains(source, "hidden={!access.hasPerms(accountPermissions.add)}", modal, violations);
        assertContains(source, "PartnerSessionModal", modal, violations);
        assertContains(source, "config.services.listAccountSessions", modal, violations);
        assertContains(source, "config.services.forceLogoutAccount", modal, violations);
        assertContains(source, "access.hasPerms(`${permPrefix}:forceLogout`)", modal, violations);
    }

    private void assertContains(String source, String expected, Path path, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(path.getFileName() + " must contain " + expected);
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
