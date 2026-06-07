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

public class PortalAnonymousEndpointContractTest
{
    private static final Pattern HANDLER_MAPPING = Pattern.compile("@(?:GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\b");

    private static final Pattern PUBLIC_METHOD = Pattern.compile("public\\s+[^\\(]+\\s+(\\w+)\\s*\\(");

    private static final List<String> FORBIDDEN_SCOPE_PARAM_NAMES = Arrays.asList(
            "sellerId",
            "buyerId",
            "subjectId",
            "accountId",
            "sellerAccountId",
            "buyerAccountId"
    );

    @Test
    public void portalAnonymousEndpointsMustDeclarePortalBoundaryContracts() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        for (String terminal : Arrays.asList("seller", "buyer"))
        {
            Path controllerRoot = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal + "/controller");
            try (Stream<Path> files = Files.walk(controllerRoot))
            {
                files.filter(path -> path.getFileName().toString().endsWith(".java"))
                        .forEach(path -> assertPortalAuthAndAnonymousEndpoints(path, terminal, violations));
            }
        }

        if (!violations.isEmpty())
        {
            fail("portal anonymous/auth endpoints must keep terminal boundary contracts:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertPortalAuthAndAnonymousEndpoints(Path path, String terminal, List<String> violations)
    {
        try
        {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            List<HandlerMethod> handlers = extractHandlerMethods(source);
            boolean authController = path.getFileName().toString().endsWith("PortalAuthController.java");

            for (HandlerMethod handler : handlers)
            {
                if (authController)
                {
                    assertAuthEndpointContract(path, terminal, handler, violations);
                }
                if (handler.annotations.contains("@Anonymous"))
                {
                    assertAnonymousEndpointContract(path, terminal, handler, violations);
                    assertSelfAuditEndpointPermission(path, terminal, handler, violations);
                }
            }
        }
        catch (IOException e)
        {
            throw new AssertionError(e);
        }
    }

    private void assertAuthEndpointContract(Path path, String terminal, HandlerMethod handler, List<String> violations)
    {
        String prefix = relative(path) + "#" + handler.name;
        if (handler.annotations.contains("@Anonymous"))
        {
            violations.add(prefix + " auth endpoints must use SecurityConfig permitAll, not @Anonymous");
        }
        if (handler.annotations.contains("@PortalPreAuthorize"))
        {
            violations.add(prefix + " auth endpoints must not require an existing portal session");
        }
        if (!terminalAnnotation("PortalLog", terminal).matcher(handler.annotations).find())
        {
            violations.add(prefix + " must declare @PortalLog terminal " + terminal);
        }
        if (!handler.annotations.contains("allowAnonymous = true"))
        {
            violations.add(prefix + " must allow anonymous portal auth audit");
        }
        if (!handler.annotations.contains("isSaveResponseData = false"))
        {
            violations.add(prefix + " must not save login response token in portal audit logs");
        }
    }

    private void assertAnonymousEndpointContract(Path path, String terminal, HandlerMethod handler,
            List<String> violations)
    {
        String prefix = relative(path) + "#" + handler.name;
        if (!terminalAnnotation("PortalPreAuthorize", terminal).matcher(handler.annotations).find())
        {
            violations.add(prefix + " @Anonymous must be paired with @PortalPreAuthorize terminal " + terminal);
        }
        if (!terminalAnnotation("PortalLog", terminal).matcher(handler.annotations).find())
        {
            violations.add(prefix + " @Anonymous must be paired with @PortalLog terminal " + terminal);
        }
        if (!handler.body.contains("PortalSessionContext.requireSession(\"" + terminal + "\")"))
        {
            violations.add(prefix + " @Anonymous portal endpoint must derive terminal session with PortalSessionContext");
        }
        for (String paramName : FORBIDDEN_SCOPE_PARAM_NAMES)
        {
            if (containsWord(handler.declaration, paramName))
            {
                violations.add(prefix + " portal endpoint must not accept client identity scope parameter " + paramName);
            }
        }
    }

    private void assertSelfAuditEndpointPermission(Path path, String terminal, HandlerMethod handler,
            List<String> violations)
    {
        if (!path.getFileName().toString().equals(capitalize(terminal) + "PortalController.java"))
        {
            return;
        }
        String expectedPermission = null;
        if ("accountLoginLogs".equals(handler.name))
        {
            expectedPermission = terminal + ":account:loginLog:list";
        }
        else if ("accountOperLogs".equals(handler.name))
        {
            expectedPermission = terminal + ":account:operLog:list";
        }
        else if ("accountSessions".equals(handler.name))
        {
            expectedPermission = terminal + ":account:session:list";
        }
        else if ("accounts".equals(handler.name))
        {
            expectedPermission = terminal + ":account:list";
        }
        else if ("depts".equals(handler.name))
        {
            expectedPermission = terminal + ":dept:list";
        }
        else if ("roles".equals(handler.name))
        {
            expectedPermission = terminal + ":role:list";
        }
        if (expectedPermission != null && !handler.annotations.contains("hasPermi = \"" + expectedPermission + "\""))
        {
            violations.add(relative(path) + "#" + handler.name
                    + " must require terminal portal permission " + expectedPermission);
        }
        if ("accountLoginLogs".equals(handler.name))
        {
            assertHandlerBodyContains(path, handler,
                    "return getDataTable(" + terminal + "Service.select" + capitalize(terminal)
                            + "OwnLoginLogList(session, log));",
                    violations);
        }
        else if ("accountOperLogs".equals(handler.name))
        {
            assertHandlerBodyContains(path, handler,
                    "return getDataTable(" + terminal + "Service.select" + capitalize(terminal)
                            + "OwnOperLogList(session, log));",
                    violations);
        }
        else if ("accountSessions".equals(handler.name))
        {
            assertHandlerBodyContains(path, handler,
                    "return getDataTable(" + terminal + "Service.select" + capitalize(terminal)
                            + "OwnSessionList(session));",
                    violations);
        }
    }

    private void assertHandlerBodyContains(Path path, HandlerMethod handler, String expected, List<String> violations)
    {
        if (!handler.body.contains(expected))
        {
            violations.add(relative(path) + "#" + handler.name
                    + " must return portal self-audit rows from portal-visible service result " + expected);
        }
    }

    private void assertHandlerBodyAbsent(Path path, HandlerMethod handler, String forbidden, List<String> violations)
    {
        if (handler.body.contains(forbidden))
        {
            violations.add(relative(path) + "#" + handler.name
                    + " must not return internal audit rows directly with " + forbidden);
        }
    }

    private List<HandlerMethod> extractHandlerMethods(String source)
    {
        String[] lines = source.split("\\R", -1);
        List<HandlerMethod> handlers = new ArrayList<>();
        List<String> annotations = new ArrayList<>();

        for (int i = 0; i < lines.length; i++)
        {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("@") || annotationsNeedContinuation(annotations))
            {
                annotations.add(lines[i]);
                continue;
            }

            if (trimmed.startsWith("public ") && trimmed.contains("("))
            {
                String annotationText = String.join("\n", annotations);
                if (HANDLER_MAPPING.matcher(annotationText).find())
                {
                    handlers.add(new HandlerMethod(extractMethodName(lines[i]), annotationText,
                            collectMethodDeclaration(lines, i), collectMethodBody(lines, i)));
                }
                annotations.clear();
                continue;
            }

            if (!trimmed.isEmpty())
            {
                annotations.clear();
            }
        }

        return handlers;
    }

    private String collectMethodDeclaration(String[] lines, int methodLine)
    {
        StringBuilder declaration = new StringBuilder();
        for (int i = methodLine; i < lines.length; i++)
        {
            declaration.append(lines[i]).append('\n');
            if (lines[i].contains("{"))
            {
                break;
            }
        }
        return declaration.toString();
    }

    private String collectMethodBody(String[] lines, int methodLine)
    {
        StringBuilder body = new StringBuilder();
        int braceBalance = 0;
        boolean started = false;

        for (int i = methodLine; i < lines.length; i++)
        {
            String line = lines[i];
            body.append(line).append('\n');
            for (int j = 0; j < line.length(); j++)
            {
                char c = line.charAt(j);
                if (c == '{')
                {
                    braceBalance++;
                    started = true;
                }
                else if (c == '}')
                {
                    braceBalance--;
                }
            }
            if (started && braceBalance == 0)
            {
                break;
            }
        }

        return body.toString();
    }

    private boolean annotationsNeedContinuation(List<String> annotations)
    {
        if (annotations.isEmpty())
        {
            return false;
        }
        int balance = 0;
        for (String annotation : annotations)
        {
            for (int i = 0; i < annotation.length(); i++)
            {
                char c = annotation.charAt(i);
                if (c == '(')
                {
                    balance++;
                }
                else if (c == ')')
                {
                    balance--;
                }
            }
        }
        return balance > 0;
    }

    private String extractMethodName(String methodLine)
    {
        Matcher matcher = PUBLIC_METHOD.matcher(methodLine);
        return matcher.find() ? matcher.group(1) : methodLine.trim();
    }

    private Pattern terminalAnnotation(String annotationName, String terminal)
    {
        return Pattern.compile("@" + annotationName + "\\s*\\([^)]*terminal\\s*=\\s*\"" + terminal + "\"",
                Pattern.DOTALL);
    }

    private boolean containsWord(String source, String word)
    {
        return source.matches("(?s).*\\b" + word + "\\b.*");
    }

    private String capitalize(String value)
    {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private String relative(Path path)
    {
        return findBackendRoot().relativize(path).toString().replace('\\', '/');
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

    private static class HandlerMethod
    {
        private final String name;

        private final String annotations;

        private final String declaration;

        private final String body;

        private HandlerMethod(String name, String annotations, String declaration, String body)
        {
            this.name = name;
            this.annotations = annotations;
            this.declaration = declaration;
            this.body = body;
        }
    }
}
