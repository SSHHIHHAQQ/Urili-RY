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

public class PortalLoginSessionConsistencyContractTest
{
    @Test
    public void portalLoginSuccessMustCleanRedisTokenWhenDatabaseSessionRecordingFails() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        assertLoginIssueCompensation(violations, backendRoot.resolve(
                "seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java"),
                "loginSeller", "directLoginSeller", "seller");
        assertLoginIssueCompensation(violations, backendRoot.resolve(
                "buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java"),
                "loginBuyer", "directLoginBuyer", "buyer");

        if (!violations.isEmpty())
        {
            fail("portal login/session persistence must not leave active Redis tokens without DB session records:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void forceLogoutMustAuditEachOnlineSessionWithDirectLoginContext() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        assertForceLogoutSessionAudit(violations, backendRoot, "seller", "Seller", "sellerId", "sellerAccountId");
        assertForceLogoutSessionAudit(violations, backendRoot, "buyer", "Buyer", "buyerId", "buyerAccountId");

        if (!violations.isEmpty())
        {
            fail("terminal force logout must load online sessions and preserve direct-login audit context:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertLoginIssueCompensation(List<String> violations, Path path, String loginMethod,
            String directLoginMethod, String terminal) throws IOException
    {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        assertMethodCompensation(violations, path.getFileName().toString(), source, loginMethod, terminal);
        assertMethodCompensation(violations, path.getFileName().toString(), source, directLoginMethod, terminal);
    }

    private void assertMethodCompensation(List<String> violations, String fileName, String source, String methodName,
            String terminal)
    {
        String block = methodBlock(source, "public PortalLoginResult " + methodName + "(");
        if (!block.contains("@Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)"))
        {
            violations.add(fileName + "#" + methodName
                    + " must run inside an explicit rollback transaction while preserving business failure logs");
        }
        if (!block.contains("portalTokenSupport.createLogin(\"" + terminal + "\""))
        {
            violations.add(fileName + "#" + methodName + " must issue a terminal-scoped portal login token");
        }
        if (!block.contains("catch (RuntimeException e)")
                || !block.contains("portalTokenSupport.deleteLoginToken(issue.getSession())")
                || !block.contains("throw e;"))
        {
            violations.add(fileName + "#" + methodName
                    + " must delete the issued Redis token if login log/session DB recording fails");
        }
    }

    private void assertForceLogoutSessionAudit(List<String> violations, Path backendRoot, String terminal,
            String classPrefix, String subjectIdParam, String accountIdParam) throws IOException
    {
        Path service = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal + "/service/impl/"
                + classPrefix + "ServiceImpl.java");
        Path mapper = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal + "/mapper/"
                + classPrefix + "Mapper.java");
        Path mapperXml = backendRoot.resolve(terminal + "/src/main/resources/mapper/" + terminal + "/"
                + classPrefix + "Mapper.xml");

        String serviceSource = Files.readString(service, StandardCharsets.UTF_8);
        String mapperSource = Files.readString(mapper, StandardCharsets.UTF_8);
        String mapperXmlSource = Files.readString(mapperXml, StandardCharsets.UTF_8);

        String selectMethod = "selectOnline" + classPrefix + "SessionList";
        String forceAuditMethod = "record" + classPrefix + "ForceLogoutAudit";
        String forceSessionAuditMethod = "record" + classPrefix + "ForceLogoutSessionAudit";

        requireContains(mapperSource, "List<PortalLoginSession> " + selectMethod + "(@Param(\"" + subjectIdParam
                + "\") Long " + subjectIdParam + ",", mapper, violations);
        requireContains(serviceSource, selectMethod + "(" + subjectIdParam + ", " + accountIdParam + ")", service,
                violations);
        requireContains(serviceSource, forceAuditMethod + "(" + subjectIdParam + ", " + accountIdParam
                + ", reason, sessions)", service, violations);
        requireContains(serviceSource, "sessions.stream().map(PortalLoginSession::getTokenId)", service, violations);
        requireContains(serviceSource, "private void " + forceSessionAuditMethod + "(PortalLoginSession session",
                service, violations);
        requireContains(serviceSource, "portalTokenSupport.buildDirectLoginLog(session.getSubjectId(), session.getAccountId()",
                service, violations);

        requireContains(mapperXmlSource, "<select id=\"" + selectMethod + "\" resultType=\"PortalLoginSession\">",
                mapperXml, violations);
        requireContains(mapperXmlSource, "and status = '0'", mapperXml, violations);
        requireContains(mapperXmlSource, "and logout_time is null", mapperXml, violations);
        requireContains(mapperXmlSource, "and expire_time >= sysdate()", mapperXml, violations);
        requireContains(mapperXmlSource, "direct_login as directLogin", mapperXml, violations);
        requireContains(mapperXmlSource, "direct_login_ticket_id as directLoginTicketId", mapperXml, violations);
        requireContains(mapperXmlSource, "acting_admin_id as actingAdminId", mapperXml, violations);
        requireContains(mapperXmlSource, "acting_admin_name as actingAdminName", mapperXml, violations);
        requireContains(mapperXmlSource, "direct_login_reason as directLoginReason", mapperXml, violations);
    }

    private void requireContains(String source, String expected, Path path, List<String> violations)
    {
        if (!source.contains(expected))
        {
            violations.add(path.getFileName() + " must contain " + expected);
        }
    }

    private String methodBlock(String source, String signature)
    {
        int signatureIndex = source.indexOf(signature);
        if (signatureIndex < 0)
        {
            return "";
        }
        int start = source.lastIndexOf("\n    @Override", signatureIndex);
        if (start < 0)
        {
            start = signatureIndex;
        }
        int nextOverride = source.indexOf("\n    @Override", signatureIndex + signature.length());
        int nextPrivate = source.indexOf("\n    private ", signatureIndex + signature.length());
        int end = firstPositive(nextOverride, nextPrivate);
        if (end < 0)
        {
            end = source.length();
        }
        return source.substring(start, end);
    }

    private int firstPositive(int first, int second)
    {
        if (first < 0)
        {
            return second;
        }
        if (second < 0)
        {
            return first;
        }
        return Math.min(first, second);
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
