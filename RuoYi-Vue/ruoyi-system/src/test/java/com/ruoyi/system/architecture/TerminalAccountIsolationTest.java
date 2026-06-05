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
import java.util.stream.Stream;
import org.junit.Test;

public class TerminalAccountIsolationTest
{
    private static final Pattern FORBIDDEN_SYS_ACCOUNT_REFERENCE = Pattern.compile(
            "\\b(?:sys_user|sys_role|sys_menu|sys_dept|sys_user_role|sys_role_menu|SysUser|SysRole|SysMenu|SysDept|PortalAccountSupport|PortalAccountMapper)\\b"
                    + "|\\b(?:seller_account|buyer_account)\\.user_id\\b",
            Pattern.CASE_INSENSITIVE);

    @Test
    public void sellerAndBuyerModulesMustNotReuseAdminSysAccountControlPlane() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (String module : Arrays.asList("seller", "buyer"))
        {
            collectForbiddenReferences(backendRoot, module, "src/main/java", violations);
            collectForbiddenReferences(backendRoot, module, "src/main/resources", violations);
        }

        if (!violations.isEmpty())
        {
            fail("seller/buyer terminal account control planes must not reuse admin sys_* tables or Sys* objects:\n"
                    + String.join("\n", violations));
        }
    }

    private void collectForbiddenReferences(Path backendRoot, String module, String sourceDirectory,
            List<String> violations) throws IOException
    {
        Path root = backendRoot.resolve(module).resolve(sourceDirectory);
        if (!Files.isDirectory(root))
        {
            return;
        }

        try (Stream<Path> paths = Files.walk(root))
        {
            paths.filter(this::isScannedSourceFile)
                    .forEach(path -> collectForbiddenReference(backendRoot, path, violations));
        }
    }

    private boolean isScannedSourceFile(Path path)
    {
        String name = path.toString();
        return name.endsWith(".java") || name.endsWith(".xml");
    }

    private void collectForbiddenReference(Path backendRoot, Path path, List<String> violations)
    {
        try
        {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++)
            {
                Matcher matcher = FORBIDDEN_SYS_ACCOUNT_REFERENCE.matcher(lines.get(i));
                if (matcher.find())
                {
                    violations.add(backendRoot.relativize(path) + ":" + (i + 1) + " -> " + matcher.group());
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to read " + path, e);
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
