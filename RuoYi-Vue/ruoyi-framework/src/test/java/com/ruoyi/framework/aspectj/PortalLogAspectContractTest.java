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
        if (source.contains("if (session == null && controllerLog.allowAnonymous())"))
        {
            violations.add("PortalLogAspect must not resolve anonymous auth audit after reading an old request token");
        }
        assertAppearsBefore(source, "PortalLogAspect.java", "if (controllerLog.allowAnonymous())",
                "portalTokenSupport.getSession(controllerLog.terminal())", violations);
        if (!source.contains("resolveSessionFromLoginResult(controllerLog.terminal(), jsonResult)")
                || !source.contains("AjaxResult.DATA_TAG")
                || !source.contains("portalTokenSupport.getSession(expectedTerminal, loginResult.getToken())"))
        {
            violations.add("PortalLogAspect must resolve successful portal auth responses back to their stored sessions");
        }
        if (!source.contains("applyDirectLoginAudit(operLog, session)")
                || !source.contains("operLog.setDirectLogin(Boolean.FALSE)")
                || !source.contains("operLog.setDirectLogin(Boolean.TRUE)")
                || !source.contains("operLog.setDirectLoginTicketId(session.getDirectLoginTicketId())")
                || !source.contains("operLog.setActingAdminId(session.getActingAdminId())")
                || !source.contains("operLog.setActingAdminName(session.getActingAdminName())")
                || !source.contains("operLog.setDirectLoginReason(session.getDirectLoginReason())"))
        {
            violations.add("PortalLogAspect must copy direct-login audit context into PortalOperLog structured fields");
        }
        if (!source.contains("directLoginAudit{ticketId=")
                || !source.contains("appendDirectLoginAuditParam(operLog, session)"))
        {
            violations.add("PortalLogAspect must keep the direct-login oper_param compatibility prefix");
        }
        if (!source.contains("filterRequestParamMap(paramsMap, excludeParamNames)"))
        {
            violations.add("PortalLogAspect must filter portal request param maps before serializing oper_param");
        }
        for (String expected : List.of("subjectId", "accountId", "sellerId", "buyerId", "sellerAccountId",
                "buyerAccountId", "directLoginTicketId", "actingAdminId", "actingAdminName",
                "directLoginReason", "terminal", "tokenId", "operParam", "jsonResult"))
        {
            if (!source.contains("\"" + expected + "\""))
            {
                violations.add("PortalLogAspect must exclude portal scope/audit field: " + expected);
            }
        }

        if (!violations.isEmpty())
        {
            fail(String.join("\n", violations));
        }
    }

    @Test
    public void portalPreAuthorizeAspectMustKeepDirectLoginAuditOnAuthorizationFailures() throws IOException
    {
        Path sourcePath = findBackendRoot().resolve(
                "ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalPreAuthorizeAspect.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        if (!source.contains("recordAuthorizationFailure(joinPoint, portalPreAuthorize, e)"))
        {
            violations.add("PortalPreAuthorizeAspect must audit authorization failures before rethrowing");
        }
        if (!source.contains("portalTokenSupport.getSession(portalPreAuthorize.terminal())"))
        {
            violations.add("PortalPreAuthorizeAspect must resolve the current terminal session for denied requests");
        }
        if (!source.contains("applyDirectLoginAudit(operLog, session)"))
        {
            violations.add("PortalPreAuthorizeAspect must attach direct-login audit scope to denied requests");
        }
        if (!source.contains("operLog.setDirectLogin(Boolean.FALSE)")
                || !source.contains("operLog.setDirectLogin(Boolean.TRUE)")
                || !source.contains("operLog.setDirectLoginTicketId(session.getDirectLoginTicketId())")
                || !source.contains("operLog.setActingAdminId(session.getActingAdminId())")
                || !source.contains("operLog.setActingAdminName(session.getActingAdminName())")
                || !source.contains("operLog.setDirectLoginReason(session.getDirectLoginReason())"))
        {
            violations.add("PortalPreAuthorizeAspect must copy direct-login audit context into structured fields");
        }
        if (!source.contains("directLoginAudit{ticketId=")
                || !source.contains("appendDirectLoginAuditParam(operLog, session)"))
        {
            violations.add("PortalPreAuthorizeAspect must keep the direct-login oper_param compatibility prefix");
        }
        assertAppearsBefore(source, "PortalPreAuthorizeAspect.java",
                "applyDirectLoginAudit(operLog, session)",
                "AsyncFactory.recordPortalOper(portalPreAuthorize.terminal(), operLog)", violations);

        if (!violations.isEmpty())
        {
            fail(String.join("\n", violations));
        }
    }

    private void assertAppearsBefore(String source, String fileName, String first, String second,
            List<String> violations)
    {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        if (firstIndex < 0 || secondIndex < 0 || firstIndex > secondIndex)
        {
            violations.add(fileName + " must evaluate " + first + " before " + second);
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
