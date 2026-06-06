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
import org.junit.Test;

public class PortalDirectLoginAuthContractTest
{
    @Test
    public void portalDirectLoginApiMustReceiveTokenFromPostBodyOnly() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (String controller : Arrays.asList(
                "seller/src/main/java/com/ruoyi/seller/controller/SellerPortalAuthController.java",
                "buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalAuthController.java"))
        {
            Path path = backendRoot.resolve(controller);
            String source = Files.readString(path, StandardCharsets.UTF_8);
            String fileName = path.getFileName().toString();

            requireContains(violations, fileName, source, "@PostMapping(\"/direct-login\")");
            requireContains(violations, fileName, source,
                    "directLogin(@RequestBody(required = false) Map<String, String> body)");
            requireAbsent(violations, fileName, source, "@GetMapping(\"/direct-login\")");
            requireAbsent(violations, fileName, source, "@RequestParam(\"directLoginToken\")");
            requireAbsent(violations, fileName, source, "@RequestParam(value = \"directLoginToken\"");
        }

        if (!violations.isEmpty())
        {
            fail("portal direct-login API must not accept directLoginToken from URL query:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void adminDirectLoginResultMustKeepTokenOutOfLoginUrl() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();
        Path supportPath = backendRoot.resolve(
                "ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java");
        String supportSource = Files.readString(supportPath, StandardCharsets.UTF_8);

        requireContains(violations, supportPath.getFileName().toString(), supportSource,
                "result.setToken(token)");
        requireContains(violations, supportPath.getFileName().toString(), supportSource,
                "result.setLoginUrl(buildLoginUrl(webUrlConfigKey, fallbackWebUrl))");
        requireContains(violations, supportPath.getFileName().toString(), supportSource,
                "assertActingAdmin(actingAdminId, actingAdminName)");
        requireContains(violations, supportPath.getFileName().toString(), supportSource,
                "if (validator == null)");
        requireAbsent(violations, supportPath.getFileName().toString(), supportSource, "directLoginToken=");
        requireAbsent(violations, supportPath.getFileName().toString(), supportSource, "URLEncoder");
        requireAbsent(violations, supportPath.getFileName().toString(), supportSource,
                "public PortalDirectLoginToken consumeToken(String portalType, String token)\n    {");

        if (!violations.isEmpty())
        {
            fail("admin direct-login response must return token separately and keep loginUrl clean:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void serviceTestStubsMustNotReintroduceTokenUrlPattern() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (String testFile : Arrays.asList(
                "seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java",
                "buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java"))
        {
            Path path = backendRoot.resolve(testFile);
            String source = Files.readString(path, StandardCharsets.UTF_8);
            String fileName = path.getFileName().toString();

            requireAbsent(violations, fileName, source, "#directLoginToken=");
            requireAbsent(violations, fileName, source, "?directLoginToken=");
        }

        if (!violations.isEmpty())
        {
            fail("service test stubs must keep direct-login token out of loginUrl:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void directLoginAuditMustCarryActingAdminIntoPortalSessionAndOperLog() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        Path tokenPayload = backendRoot.resolve(
                "ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDirectLoginToken.java");
        Path session = backendRoot.resolve(
                "ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalLoginSession.java");
        Path tokenSupport = backendRoot.resolve(
                "ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java");
        Path logAspect = backendRoot.resolve(
                "ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalLogAspect.java");
        Path preAuthorizeAspect = backendRoot.resolve(
                "ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalPreAuthorizeAspect.java");
        Path sessionProfile = backendRoot.resolve(
                "ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalSessionProfile.java");
        Path sellerMapper = backendRoot.resolve("seller/src/main/resources/mapper/seller/SellerMapper.xml");
        Path buyerMapper = backendRoot.resolve("buyer/src/main/resources/mapper/buyer/BuyerMapper.xml");
        Path migrationSql = backendRoot.resolve("sql/20260604_three_terminal_isolation_migration.sql");
        Path baseSeedSql = backendRoot.resolve("sql/seller_buyer_management_seed.sql");

        String tokenPayloadSource = Files.readString(tokenPayload, StandardCharsets.UTF_8);
        String sessionSource = Files.readString(session, StandardCharsets.UTF_8);
        String tokenSupportSource = Files.readString(tokenSupport, StandardCharsets.UTF_8);
        String logAspectSource = Files.readString(logAspect, StandardCharsets.UTF_8);
        String preAuthorizeAspectSource = Files.readString(preAuthorizeAspect, StandardCharsets.UTF_8);
        String sessionProfileSource = Files.readString(sessionProfile, StandardCharsets.UTF_8);
        String sellerMapperSource = Files.readString(sellerMapper, StandardCharsets.UTF_8);
        String buyerMapperSource = Files.readString(buyerMapper, StandardCharsets.UTF_8);
        String migrationSource = Files.readString(migrationSql, StandardCharsets.UTF_8);
        String baseSeedSource = Files.readString(baseSeedSql, StandardCharsets.UTF_8);

        for (String expected : Arrays.asList("actingAdminId", "actingAdminName", "directLoginReason"))
        {
            requireContains(violations, tokenPayload.getFileName().toString(), tokenPayloadSource, expected);
        }
        for (String expected : Arrays.asList("directLoginTicketId", "actingAdminId", "actingAdminName",
                "directLoginReason"))
        {
            requireContains(violations, session.getFileName().toString(), sessionSource, expected);
        }
        requireContains(violations, tokenSupport.getFileName().toString(), tokenSupportSource,
                "applyDirectLoginAudit(session, directLoginToken)");
        requireContains(violations, tokenSupport.getFileName().toString(), tokenSupportSource,
                "session.setActingAdminId(directLoginToken.getActingAdminId())");
        requireContains(violations, logAspect.getFileName().toString(), logAspectSource,
                "directLoginAudit{ticketId=");
        requireContains(violations, logAspect.getFileName().toString(), logAspectSource,
                "session.getActingAdminId()");
        requireContains(violations, preAuthorizeAspect.getFileName().toString(), preAuthorizeAspectSource,
                "directLoginAudit{ticketId=");
        requireContains(violations, preAuthorizeAspect.getFileName().toString(), preAuthorizeAspectSource,
                "session.getActingAdminId()");
        for (String expected : Arrays.asList("directLogin", "directLoginTicketId", "actingAdminId",
                "actingAdminName", "directLoginReason"))
        {
            requireContains(violations, sessionProfile.getFileName().toString(), sessionProfileSource, expected);
        }
        for (String expected : Arrays.asList("direct_login", "direct_login_ticket_id", "acting_admin_id",
                "acting_admin_name", "direct_login_reason"))
        {
            requireContains(violations, sellerMapper.getFileName().toString(), sellerMapperSource, expected);
            requireContains(violations, buyerMapper.getFileName().toString(), buyerMapperSource, expected);
            requireContains(violations, migrationSql.getFileName().toString(), migrationSource, expected);
            requireContains(violations, baseSeedSql.getFileName().toString(), baseSeedSource, expected);
        }

        if (!violations.isEmpty())
        {
            fail("direct-login audit must carry acting admin into portal session and oper log:\n"
                    + String.join("\n", violations));
        }
    }

    private void requireContains(List<String> violations, String fileName, String source, String expected)
    {
        if (!source.contains(expected))
        {
            violations.add(fileName + " must contain " + expected);
        }
    }

    private void requireAbsent(List<String> violations, String fileName, String source, String forbidden)
    {
        if (source.contains(forbidden))
        {
            violations.add(fileName + " must not contain " + forbidden);
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
