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
            "\\b(?:sys_user|sys_role|sys_menu|sys_dept|sys_user_role|sys_role_menu|SysUser|SysRole|SysMenu|SysDept|LoginUser|PortalAccountSupport|PortalAccountMapper)\\b"
                    + "|\\b(?:seller_account|buyer_account)\\.user_id\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_ID_ONLY_SIGNATURE = Pattern.compile(
            "\\b(?:public\\s+)?[\\w<>\\[\\], ?]+\\s+\\w*Account\\w*\\s*\\(\\s*(?:@Param\\(\"(?:sellerAccountId|buyerAccountId|accountId)\"\\)\\s*)?Long\\s+(?:sellerAccountId|buyerAccountId|accountId)\\s*\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_STATEMENT = Pattern.compile(
            "<(select|update|delete|insert)\\b[^>]*id=\"([^\"]+)\"[\\s\\S]*?</\\1>",
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

    @Test
    public void portalSharedAuthSupportMustNotReuseAdminLoginUserControlPlane() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        collectForbiddenReferences(backendRoot, "ruoyi-system", "src/main/java/com/ruoyi/system/service/support",
                violations);
        collectForbiddenReference(backendRoot, backendRoot.resolve(
                "ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalPreAuthorizeAspect.java"),
                violations);
        collectForbiddenReference(backendRoot, backendRoot.resolve(
                "ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalLogAspect.java"),
                violations);

        if (!violations.isEmpty())
        {
            fail("portal shared auth/log support must not reuse admin LoginUser or sys_* account control plane:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalPermissionMappersMustKeepSubjectScopeAtSqlBoundary() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        assertContains(backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/mapper/SellerPortalPermissionMapper.java"),
                "selectSellerMenuIdsByRoleId(@Param(\"sellerId\") Long sellerId", violations);
        assertContains(backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/mapper/SellerPortalPermissionMapper.java"),
                "countSellerAccountRoleByRoleId(@Param(\"sellerId\") Long sellerId", violations);
        assertContains(backendRoot.resolve("seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml"),
                "and r.seller_id = #{sellerId}", violations);
        assertContains(backendRoot.resolve("seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml"),
                "and a.seller_id = #{sellerId}", violations);

        assertContains(backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerPortalPermissionMapper.java"),
                "selectBuyerMenuIdsByRoleId(@Param(\"buyerId\") Long buyerId", violations);
        assertContains(backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerPortalPermissionMapper.java"),
                "countBuyerAccountRoleByRoleId(@Param(\"buyerId\") Long buyerId", violations);
        assertContains(backendRoot.resolve("buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml"),
                "and r.buyer_id = #{buyerId}", violations);
        assertContains(backendRoot.resolve("buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml"),
                "and a.buyer_id = #{buyerId}", violations);

        if (!violations.isEmpty())
        {
            fail("terminal permission mappers must keep subject scope in mapper signatures and SQL:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalAccountMappersMustKeepSubjectScopeWithoutAccountOnlyLookup() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        assertContains(backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java"),
                "selectSellerAccountByIdAndSellerId(@Param(\"sellerId\") Long sellerId", violations);
        assertContains(backendRoot.resolve("seller/src/main/resources/mapper/seller/SellerMapper.xml"),
                "where a.seller_id = #{sellerId}", violations);
        assertContains(backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/service/ISellerService.java"),
                "selectSellerAccountById(Long sellerId, Long sellerAccountId)", violations);
        assertContains(backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java"),
                "selectSellerAccountById(Long sellerId, Long sellerAccountId)", violations);
        assertNotContains(backendRoot.resolve("seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java"),
                "selectSellerAccountById(Long sellerAccountId)", violations);
        assertNotContains(backendRoot.resolve("seller/src/main/resources/mapper/seller/SellerMapper.xml"),
                "<select id=\"selectSellerAccountById\"", violations);
        scanForbiddenMapperCall(
                backendRoot.resolve("seller/src/main/java"),
                "sellerMapper.selectSellerAccountById(",
                backendRoot, violations);
        scanForbiddenAccountOnlyServiceLookup(
                backendRoot.resolve("seller/src/main/java"),
                "selectSellerAccountById",
                backendRoot, violations);
        scanForbiddenText(
                backendRoot.resolve("seller/src/test/java"),
                "\"selectSellerAccountById\".equals(methodName)",
                backendRoot, violations);

        assertContains(backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java"),
                "selectBuyerAccountByIdAndBuyerId(@Param(\"buyerId\") Long buyerId", violations);
        assertContains(backendRoot.resolve("buyer/src/main/resources/mapper/buyer/BuyerMapper.xml"),
                "where a.buyer_id = #{buyerId}", violations);
        assertContains(backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/service/IBuyerService.java"),
                "selectBuyerAccountById(Long buyerId, Long buyerAccountId)", violations);
        assertContains(backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java"),
                "selectBuyerAccountById(Long buyerId, Long buyerAccountId)", violations);
        assertNotContains(backendRoot.resolve("buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java"),
                "selectBuyerAccountById(Long buyerAccountId)", violations);
        assertNotContains(backendRoot.resolve("buyer/src/main/resources/mapper/buyer/BuyerMapper.xml"),
                "<select id=\"selectBuyerAccountById\"", violations);
        scanForbiddenMapperCall(
                backendRoot.resolve("buyer/src/main/java"),
                "buyerMapper.selectBuyerAccountById(",
                backendRoot, violations);
        scanForbiddenAccountOnlyServiceLookup(
                backendRoot.resolve("buyer/src/main/java"),
                "selectBuyerAccountById",
                backendRoot, violations);
        scanForbiddenText(
                backendRoot.resolve("buyer/src/test/java"),
                "\"selectBuyerAccountById\".equals(methodName)",
                backendRoot, violations);
        scanForbiddenGenericAccountIdOnlySignatures(backendRoot, "seller", violations);
        scanForbiddenGenericAccountIdOnlySignatures(backendRoot, "buyer", violations);
        scanForbiddenAccountSqlWithoutSubjectScope(backendRoot, "seller", violations);
        scanForbiddenAccountSqlWithoutSubjectScope(backendRoot, "buyer", violations);

        if (!violations.isEmpty())
        {
            fail("terminal account mapper accountId-only lookup is forbidden in production service code:\n"
                    + String.join("\n", violations));
        }
    }

    private void scanForbiddenGenericAccountIdOnlySignatures(Path backendRoot, String terminal,
            List<String> violations) throws IOException
    {
        Path sourceRoot = backendRoot.resolve(terminal).resolve("src/main/java");
        try (Stream<Path> paths = Files.walk(sourceRoot))
        {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList())
            {
                String relativePath = backendRoot.relativize(path).toString().replace('\\', '/');
                if (!relativePath.contains("/controller/") && !relativePath.contains("/service/")
                        && !relativePath.contains("/mapper/"))
                {
                    continue;
                }
                String source = normalizeWhitespace(Files.readString(path, StandardCharsets.UTF_8));
                Matcher matcher = ACCOUNT_ID_ONLY_SIGNATURE.matcher(source);
                while (matcher.find())
                {
                    violations.add(backendRoot.relativize(path)
                            + " must not expose accountId-only method signature: " + matcher.group());
                }
            }
        }
    }

    private void scanForbiddenAccountSqlWithoutSubjectScope(Path backendRoot, String terminal,
            List<String> violations) throws IOException
    {
        Path mapperRoot = backendRoot.resolve(terminal).resolve("src/main/resources");
        String accountTable = terminal + "_account";
        String accountRoleTable = terminal + "_account_role";
        String accountIdColumn = terminal + "_account_id";
        String subjectIdColumn = terminal + "_id";
        String subjectIdParam = terminal + "Id";

        try (Stream<Path> paths = Files.walk(mapperRoot))
        {
            for (Path path : paths.filter(path -> path.toString().endsWith(".xml")).toList())
            {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                Matcher matcher = XML_STATEMENT.matcher(source);
                while (matcher.find())
                {
                    String statementId = matcher.group(2);
                    String statement = normalizeWhitespace(matcher.group());
                    if (!statement.contains(accountIdColumn + " = #{")
                            || !touchesAccountScopeTable(statement, accountTable, accountRoleTable))
                    {
                        continue;
                    }
                    if (statement.contains(subjectIdColumn + " = #{" + subjectIdParam + "}"))
                    {
                        continue;
                    }
                    violations.add(backendRoot.relativize(path) + "#" + statementId
                            + " must scope " + accountIdColumn + " filters by " + subjectIdColumn);
                }
            }
        }
    }

    private boolean touchesAccountScopeTable(String statement, String accountTable, String accountRoleTable)
    {
        return statement.contains("from " + accountTable + " ")
                || statement.contains("join " + accountTable + " ")
                || statement.contains("update " + accountTable + " ")
                || statement.contains("delete from " + accountTable + " ")
                || statement.contains("insert into " + accountTable)
                || statement.contains("from " + accountRoleTable + " ")
                || statement.contains("join " + accountRoleTable + " ")
                || statement.contains("update " + accountRoleTable + " ")
                || statement.contains("delete from " + accountRoleTable + " ")
                || statement.contains("insert into " + accountRoleTable);
    }

    private String normalizeWhitespace(String source)
    {
        return source.replaceAll("\\s+", " ").trim();
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

    private void assertContains(Path path, String expected, List<String> violations) throws IOException
    {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        if (!source.contains(expected))
        {
            violations.add(findBackendRoot().relativize(path) + " must contain " + expected);
        }
    }

    private void assertNotContains(Path path, String forbidden, List<String> violations) throws IOException
    {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        if (source.contains(forbidden))
        {
            violations.add(findBackendRoot().relativize(path) + " must not contain " + forbidden);
        }
    }

    private void scanForbiddenMapperCall(Path sourceRoot, String mapperCall, Path backendRoot, List<String> violations)
            throws IOException
    {
        try (Stream<Path> paths = Files.walk(sourceRoot))
        {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList())
            {
                assertMapperCallAbsent(path, mapperCall, backendRoot, violations);
            }
        }
    }

    private void assertMapperCallAbsent(Path path, String mapperCall, Path backendRoot, List<String> violations)
            throws IOException
    {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++)
        {
            if (!lines.get(i).contains(mapperCall))
            {
                continue;
            }
            violations.add(backendRoot.relativize(path) + ":" + (i + 1) + " -> " + mapperCall);
        }
    }

    private void scanForbiddenAccountOnlyServiceLookup(Path sourceRoot, String methodName, Path backendRoot,
            List<String> violations) throws IOException
    {
        try (Stream<Path> paths = Files.walk(sourceRoot))
        {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList())
            {
                assertAccountOnlyServiceLookupAbsent(path, methodName, backendRoot, violations);
            }
        }
    }

    private void scanForbiddenText(Path sourceRoot, String forbidden, Path backendRoot, List<String> violations)
            throws IOException
    {
        try (Stream<Path> paths = Files.walk(sourceRoot))
        {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList())
            {
                assertTextAbsent(path, forbidden, backendRoot, violations);
            }
        }
    }

    private void assertTextAbsent(Path path, String forbidden, Path backendRoot, List<String> violations)
            throws IOException
    {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++)
        {
            if (!lines.get(i).contains(forbidden))
            {
                continue;
            }
            violations.add(backendRoot.relativize(path) + ":" + (i + 1) + " -> " + forbidden);
        }
    }

    private void assertAccountOnlyServiceLookupAbsent(Path path, String methodName, Path backendRoot,
            List<String> violations) throws IOException
    {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++)
        {
            String line = lines.get(i);
            int callIndex = line.indexOf(methodName + "(");
            if (callIndex < 0)
            {
                continue;
            }
            String invocation = line.substring(callIndex);
            if (invocation.contains(","))
            {
                continue;
            }
            violations.add(backendRoot.relativize(path) + ":" + (i + 1) + " -> " + methodName + "(accountId)");
        }
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
