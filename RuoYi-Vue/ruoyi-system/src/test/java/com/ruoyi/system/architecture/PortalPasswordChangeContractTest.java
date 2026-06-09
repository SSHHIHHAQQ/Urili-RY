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

public class PortalPasswordChangeContractTest
{
    @Test
    public void sellerAndBuyerPortalPasswordChangeMustStayTerminalScoped() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path repoRoot = backendRoot.getParent();
        List<String> violations = new ArrayList<>();

        assertPortalControllerPasswordChange(backendRoot.resolve(
                "seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java"),
                "seller", "updateSellerOwnPassword", violations);
        assertPortalControllerPasswordChange(backendRoot.resolve(
                "buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java"),
                "buyer", "updateBuyerOwnPassword", violations);
        assertPortalPasswordService(repoRoot.resolve("react-ui/src/services/portal/session.ts"), violations);
        assertExactSource(repoRoot.resolve("react-ui/src/services/portal/session.js"),
                "export * from './session.ts';", violations);

        if (!violations.isEmpty())
        {
            fail("portal password change must stay terminal-scoped:\n" + String.join("\n", violations));
        }
    }

    private void assertPortalControllerPasswordChange(Path controller, String terminal, String serviceCall,
            List<String> violations) throws IOException
    {
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        String block = extractMethodBlock(source, "updatePassword");

        assertContains(block, "@PutMapping(\"/account/password\")", controller, violations);
        assertContains(block, "@Anonymous", controller, violations);
        assertContains(block, "@PortalPreAuthorize(terminal = \"" + terminal + "\")", controller, violations);
        assertContains(block, "@PortalLog(terminal = \"" + terminal + "\"", controller, violations);
        assertContains(block, "PortalSessionContext.requireSession(\"" + terminal + "\")", controller, violations);
        assertContains(block, serviceCall + "(session, request)", controller, violations);
        assertNotContains(block, "@PreAuthorize", controller, violations);
        assertNotContains(block, "sellerId", controller, violations);
        assertNotContains(block, "buyerId", controller, violations);
        assertNotContains(block, "accountId", controller, violations);
    }

    private void assertPortalPasswordService(Path serviceFile, List<String> violations) throws IOException
    {
        String source = Files.readString(serviceFile, StandardCharsets.UTF_8);
        String block = extractFunctionBlock(source, "updatePortalPassword");

        assertContains(block, "buildPortalUrl(terminal, '/account/password')", serviceFile, violations);
        assertContains(block, "method: 'PUT'", serviceFile, violations);
        assertContains(block, "buildPortalAuthHeaders(terminal)", serviceFile, violations);
        assertContains(block, "isToken: false", serviceFile, violations);
        assertContains(source, "updatePortalPassword('seller', data)", serviceFile, violations);
        assertContains(source, "updatePortalPassword('buyer', data)", serviceFile, violations);
        assertNotContains(block, "/api/seller", serviceFile, violations);
        assertNotContains(block, "/api/buyer", serviceFile, violations);
        assertNotContains(block, "getAccessToken", serviceFile, violations);
    }

    private String extractMethodBlock(String source, String methodName)
    {
        int methodIndex = source.indexOf(" " + methodName + "(");
        if (methodIndex < 0)
        {
            return "";
        }
        int blockStart = source.indexOf('{', methodIndex);
        int start = findAnnotationChainStart(source, methodIndex);
        int blockEnd = findMatchingBrace(source, blockStart);
        return blockEnd < 0 ? source.substring(start) : source.substring(start, blockEnd + 1);
    }

    private int findAnnotationChainStart(String source, int methodIndex)
    {
        int lineStart = source.lastIndexOf('\n', methodIndex);
        int start = lineStart < 0 ? 0 : lineStart + 1;
        while (start > 0)
        {
            int previousLineEnd = start - 1;
            if (previousLineEnd > 0 && source.charAt(previousLineEnd - 1) == '\r')
            {
                previousLineEnd--;
            }
            int previousLineStart = source.lastIndexOf('\n', Math.max(0, previousLineEnd - 1));
            previousLineStart = previousLineStart < 0 ? 0 : previousLineStart + 1;
            String previousLine = source.substring(previousLineStart, previousLineEnd).trim();
            if (!previousLine.startsWith("@"))
            {
                break;
            }
            start = previousLineStart;
        }
        return start;
    }

    private String extractFunctionBlock(String source, String functionName)
    {
        int functionIndex = source.indexOf("function " + functionName);
        if (functionIndex < 0)
        {
            return "";
        }
        int blockStart = source.indexOf('{', functionIndex);
        int blockEnd = findMatchingBrace(source, blockStart);
        return blockEnd < 0 ? source.substring(functionIndex) : source.substring(functionIndex, blockEnd + 1);
    }

    private int findMatchingBrace(String source, int blockStart)
    {
        if (blockStart < 0)
        {
            return -1;
        }
        int depth = 0;
        for (int index = blockStart; index < source.length(); index++)
        {
            char current = source.charAt(index);
            if (current == '{')
            {
                depth++;
            }
            else if (current == '}')
            {
                depth--;
                if (depth == 0)
                {
                    return index;
                }
            }
        }
        return -1;
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

    private void assertExactSource(Path path, String expected, List<String> violations) throws IOException
    {
        String source = Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n").trim();
        if (!source.equals(expected))
        {
            violations.add(path.getFileName() + " must be a pure re-export to the guarded TS portal session service");
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
                    && Files.isDirectory(candidate.resolve("buyer/src/main/java"))
                    && Files.isDirectory(candidate.resolve("ruoyi-system/src/test/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
