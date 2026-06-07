package com.ruoyi.product.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

public class ProductModuleBoundaryContractTest
{
    @Test
    public void productModuleMustNotImportIntegrationImplServices() throws IOException
    {
        Path productMain = findBackendRoot().resolve("product/src/main/java");
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(productMain))
        {
            files.filter((file) -> Files.isRegularFile(file) && file.toString().endsWith(".java"))
                    .forEach((file) -> {
                        try
                        {
                            String source = Files.readString(file, StandardCharsets.UTF_8);
                            if (source.contains("com.ruoyi.integration.service.impl."))
                            {
                                violations.add(productMain.relativize(file).toString());
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
            fail("product module must depend on integration public service/facade contracts, not integration impl classes:\n"
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
            if (Files.isDirectory(candidate.resolve("product/src/main/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
