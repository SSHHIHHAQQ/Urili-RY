package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;

public class PortalAdminAuditBindingContractTest
{
    @Test
    public void sellerAdminAuditHandlersMustBindQueryModelsAndForwardThemUnchanged() throws IOException
    {
        assertAdminAuditBindings("seller", "Seller", "sellerService");
    }

    @Test
    public void buyerAdminAuditHandlersMustBindQueryModelsAndForwardThemUnchanged() throws IOException
    {
        assertAdminAuditBindings("buyer", "Buyer", "buyerService");
    }

    private void assertAdminAuditBindings(String terminal, String classPrefix, String serviceName) throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path controller = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal
                + "/controller/Admin" + classPrefix + "Controller.java");
        String source = Files.readString(controller, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        String fileName = controller.getFileName().toString();

        assertPattern(source, "public\\s+TableDataInfo\\s+loginLogs\\s*\\(\\s*PortalLoginLog\\s+log\\s*\\)",
                fileName + "#loginLogs must bind PortalLoginLog log directly", violations);
        assertPattern(source, "public\\s+TableDataInfo\\s+operLogs\\s*\\(\\s*PortalOperLog\\s+log\\s*\\)",
                fileName + "#operLogs must bind PortalOperLog log directly", violations);
        assertPattern(source,
                "public\\s+TableDataInfo\\s+directLoginTickets\\s*\\(\\s*PortalDirectLoginTicket\\s+ticket\\s*\\)",
                fileName + "#directLoginTickets must bind PortalDirectLoginTicket ticket directly", violations);
        assertContains(source, serviceName + ".select" + classPrefix + "LoginLogList(log)",
                fileName + "#loginLogs must forward the bound log query", violations);
        assertContains(source, serviceName + ".select" + classPrefix + "OperLogList(log)",
                fileName + "#operLogs must forward the bound log query", violations);
        assertContains(source, serviceName + ".select" + classPrefix + "DirectLoginTicketList(ticket)",
                fileName + "#directLoginTickets must forward the bound ticket query", violations);

        if (!violations.isEmpty())
        {
            fail("portal admin audit handlers must preserve subject/account filters from request binding:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertPattern(String source, String regex, String message, List<String> violations)
    {
        if (!Pattern.compile(regex, Pattern.DOTALL).matcher(source).find())
        {
            violations.add(message);
        }
    }

    private void assertContains(String source, String expected, String message, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(message + ": missing " + expected);
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
