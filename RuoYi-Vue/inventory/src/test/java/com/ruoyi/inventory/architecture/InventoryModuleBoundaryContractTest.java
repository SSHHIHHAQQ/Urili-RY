package com.ruoyi.inventory.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class InventoryModuleBoundaryContractTest
{
    private static final Pattern FORBIDDEN_CROSS_MODULE_IMPORT = Pattern.compile(
            "^import\\s+com\\.ruoyi\\.(product|integration|warehouse)\\.(mapper|service\\.impl)\\..*;",
            Pattern.MULTILINE);

    @Test
    public void inventoryJavaMustNotImportCrossModuleMappersOrImplServices() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path javaRoot = backendRoot.resolve("inventory/src/main/java");
        List<String> violations = new ArrayList<>();

        for (Path file : javaFiles(javaRoot))
        {
            String source = read(file);
            if (FORBIDDEN_CROSS_MODULE_IMPORT.matcher(source).find())
            {
                violations.add(backendRoot.relativize(file).toString());
            }
        }

        if (!violations.isEmpty())
        {
            fail("inventory must depend on cross-module public ports, not mapper/impl classes:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void inventoryResourcesMustNotReadIntegrationSourceStockOrUpstreamTables() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path resourceRoot = backendRoot.resolve("inventory/src/main/resources");
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(resourceRoot))
        {
            files.filter((file) -> Files.isRegularFile(file) && file.toString().endsWith(".xml"))
                    .forEach((file) -> {
                        try
                        {
                            String source = read(file);
                            String relativePath = backendRoot.relativize(file).toString().replace('\\', '/');
                            if (source.contains("source_warehouse_stock_") || source.contains("upstream_system_"))
                            {
                                violations.add(relativePath);
                            }
                        }
                        catch (IOException e)
                        {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        if (!violations.isEmpty())
        {
            fail("inventory resources must use public module ports instead of integration source stock/upstream tables:\n"
                    + String.join("\n", violations));
        }
    }

    private List<Path> javaFiles(Path root) throws IOException
    {
        try (Stream<Path> files = Files.walk(root))
        {
            return files.filter((file) -> Files.isRegularFile(file) && file.toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private String read(Path file) throws IOException
    {
        return Files.readString(file, StandardCharsets.UTF_8);
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
            if (Files.isDirectory(candidate.resolve("inventory/src/main/java"))
                    && Files.isDirectory(candidate.resolve("inventory/src/main/resources")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
