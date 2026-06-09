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

public class RouterVoPermissionContractTest
{
    @Test
    public void adminGetRoutersMustExposeMenuPermsForFrontendAuthority() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path routerVoFile = backendRoot.resolve("ruoyi-system/src/main/java/com/ruoyi/system/domain/vo/RouterVo.java");
        Path sysMenuServiceFile = backendRoot.resolve(
                "ruoyi-system/src/main/java/com/ruoyi/system/service/impl/SysMenuServiceImpl.java");
        String routerVoSource = Files.readString(routerVoFile, StandardCharsets.UTF_8);
        String sysMenuServiceSource = Files.readString(sysMenuServiceFile, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        requireContains(violations, routerVoFile, routerVoSource, "private String perms;");
        requireContains(violations, routerVoFile, routerVoSource, "public String getPerms()");
        requireContains(violations, routerVoFile, routerVoSource, "public void setPerms(String perms)");

        requireContains(violations, sysMenuServiceFile, sysMenuServiceSource, "router.setPerms(menu.getPerms());");
        requireContains(violations, sysMenuServiceFile, sysMenuServiceSource, "children.setPerms(menu.getPerms());");
        if (countOccurrences(sysMenuServiceSource, "children.setPerms(menu.getPerms());") < 2)
        {
            violations.add(backendRoot.relativize(sysMenuServiceFile)
                    + " must copy menu perms into both frame and inner-link child routes");
        }

        if (!violations.isEmpty())
        {
            fail("/getRouters must keep perms in RouterVo so React remote menus can build route authority:\n"
                    + String.join("\n", violations));
        }
    }

    private void requireContains(List<String> violations, Path file, String source, String expected)
    {
        if (!source.contains(expected))
        {
            violations.add(findBackendRoot().relativize(file) + " must contain " + expected);
        }
    }

    private int countOccurrences(String source, String expected)
    {
        int count = 0;
        int index = source.indexOf(expected);
        while (index >= 0)
        {
            count++;
            index = source.indexOf(expected, index + expected.length());
        }
        return count;
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
            if (Files.isRegularFile(candidate.resolve("ruoyi-system/src/main/java/com/ruoyi/system/domain/vo/RouterVo.java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
