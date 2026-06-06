package com.ruoyi.framework.aspectj;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PortalLogAspectContractTest
{
    @Test
    public void portalLogAspectMustRecordFailureWhenSessionIsMissing() throws IOException
    {
        Path sourcePath = findBackendRoot().resolve(
                "ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalLogAspect.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        if (source.contains("if (session == null)\r\n            {\r\n                return;\r\n            }")
                || source.contains("if (session == null)\n            {\n                return;\n            }"))
        {
            violations.add("PortalLogAspect must not silently return when portal session is missing");
        }
        if (!source.contains("端内会话不存在"))
        {
            violations.add("PortalLogAspect must record a missing-session failure reason");
        }
        if (!source.contains("BusinessStatus.FAIL.ordinal()"))
        {
            violations.add("PortalLogAspect must mark missing-session audit records as failed");
        }
        if (!source.contains("controllerLog.allowAnonymous()"))
        {
            violations.add("PortalLogAspect must explicitly gate anonymous auth audit by annotation flag");
        }
        if (!source.contains("resolveSessionFromLoginResult(controllerLog.terminal(), jsonResult)")
                || !source.contains("AjaxResult.DATA_TAG")
                || !source.contains("portalTokenSupport.getSession(expectedTerminal, loginResult.getToken())"))
        {
            violations.add("PortalLogAspect must resolve successful portal auth responses back to their stored sessions");
        }
        if (!source.contains("directLoginAudit{ticketId=")
                || !source.contains("session.getActingAdminId()")
                || !source.contains("session.getDirectLoginReason()"))
        {
            violations.add("PortalLogAspect must append direct-login acting admin audit context to oper_param");
        }

        if (!violations.isEmpty())
        {
            fail(String.join("\n", violations));
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
            if (Files.isDirectory(candidate.resolve("ruoyi-framework/src/main/java"))
                    && Files.isDirectory(candidate.resolve("seller/src/main/java"))
                    && Files.isDirectory(candidate.resolve("buyer/src/main/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
