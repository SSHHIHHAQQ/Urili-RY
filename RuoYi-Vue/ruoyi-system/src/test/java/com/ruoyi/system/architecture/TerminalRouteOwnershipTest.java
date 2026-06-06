package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class TerminalRouteOwnershipTest
{
    private static final Pattern TERMINAL_MAPPING = Pattern.compile(
            "@(?:RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\s*\\(\\s*(?:(?:value|path)\\s*=\\s*)?\\{?\\s*\"/(?:seller|buyer)(?:/|\")",
            Pattern.DOTALL);
    private static final Pattern CLASS_LEVEL_ANONYMOUS = Pattern.compile("@Anonymous\\s*(?:\\R\\s*)*public\\s+class");
    private static final Pattern HANDLER_MAPPING = Pattern.compile("@(?:GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\b");
    private static final Pattern PUBLIC_METHOD = Pattern.compile("public\\s+[^\\(]+\\s+(\\w+)\\s*\\(");
    private static final Pattern CLIENT_IDENTITY_PARAMETER = Pattern.compile(
            "\\b(?:sellerId|buyerId|subjectId|accountId|terminal)\\b");
    private static final Pattern ADMIN_LOGIN_CONTEXT_REFERENCE = Pattern.compile(
            "\\bSecurityUtils\\s*\\.\\s*(?:getLoginUser|getUserId|getUsername)\\s*\\("
                    + "|(?<![\\.\\w])(?:getLoginUser|getUserId|getUsername)\\s*\\("
                    + "|\\bLoginUser\\b|\\bSysUser\\b");

    @Test
    public void productModuleMustNotExposeSellerOrBuyerPortalRoutes() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path productSourceRoot = backendRoot.resolve("product/src/main/java");
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(productSourceRoot))
        {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collectTerminalRouteViolation(backendRoot, path, violations));
        }

        if (!violations.isEmpty())
        {
            fail("seller/buyer portal routes must live in seller/buyer modules, not product:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void sellerPortalHandlersMustUseSellerSecurityTemplate() throws IOException
    {
        assertPortalHandlersUseSecurityTemplate("seller");
    }

    @Test
    public void buyerPortalHandlersMustUseBuyerSecurityTemplate() throws IOException
    {
        assertPortalHandlersUseSecurityTemplate("buyer");
    }

    @Test
    public void sellerPortalHandlersMustNotAcceptClientIdentityScope() throws IOException
    {
        assertPortalHandlersDoNotAcceptClientIdentityScope("seller");
    }

    @Test
    public void buyerPortalHandlersMustNotAcceptClientIdentityScope() throws IOException
    {
        assertPortalHandlersDoNotAcceptClientIdentityScope("buyer");
    }

    @Test
    public void sellerPortalHandlersMustNotUseAdminLoginContext() throws IOException
    {
        assertPortalHandlersDoNotUseAdminLoginContext("seller");
    }

    @Test
    public void buyerPortalHandlersMustNotUseAdminLoginContext() throws IOException
    {
        assertPortalHandlersDoNotUseAdminLoginContext("buyer");
    }

    private void assertPortalHandlersUseSecurityTemplate(String terminal) throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();
        List<Path> controllers = discoverProtectedPortalControllers(backendRoot, terminal);

        if (controllers.isEmpty())
        {
            violations.add(terminal + " module must have protected portal controllers");
        }

        for (Path path : controllers)
        {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            String relativePath = backendRoot.relativize(path).toString();

            if (CLASS_LEVEL_ANONYMOUS.matcher(source).find())
            {
                violations.add(relativePath + " must not use class-level @Anonymous");
            }
            if (source.contains("@PreAuthorize"))
            {
                violations.add(relativePath + " must not use admin @PreAuthorize on portal endpoints");
            }

            List<HandlerMethod> handlers = extractHandlerMethods(source);
            if (handlers.isEmpty())
            {
                violations.add(relativePath + " has no portal handler methods");
            }
            for (HandlerMethod handler : handlers)
            {
                assertHandlerSecurityTemplate(terminal, relativePath, handler, violations);
            }
        }

        if (!violations.isEmpty())
        {
            fail(terminal + " portal handlers must use the terminal security template:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertPortalHandlersDoNotAcceptClientIdentityScope(String terminal) throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();
        List<Path> controllers = discoverProtectedPortalControllers(backendRoot, terminal);

        if (controllers.isEmpty())
        {
            violations.add(terminal + " module must have protected portal controllers");
        }

        for (Path path : controllers)
        {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            String relativePath = backendRoot.relativize(path).toString();
            List<HandlerMethod> handlers = extractHandlerMethods(source);

            for (HandlerMethod handler : handlers)
            {
                Matcher matcher = CLIENT_IDENTITY_PARAMETER.matcher(handler.declaration);
                if (matcher.find())
                {
                    violations.add(relativePath + "#" + handler.name
                            + " must not accept client-provided identity parameter `" + matcher.group()
                            + "`; derive terminal identity from PortalSessionContext instead");
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail(terminal + " portal handlers must not accept client-provided identity scope:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertPortalHandlersDoNotUseAdminLoginContext(String terminal) throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();
        List<Path> controllers = discoverProtectedPortalControllers(backendRoot, terminal);

        if (controllers.isEmpty())
        {
            violations.add(terminal + " module must have protected portal controllers");
        }

        for (Path path : controllers)
        {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            String relativePath = backendRoot.relativize(path).toString();
            List<HandlerMethod> handlers = extractHandlerMethods(source);

            for (HandlerMethod handler : handlers)
            {
                Matcher matcher = ADMIN_LOGIN_CONTEXT_REFERENCE.matcher(handler.body);
                if (matcher.find())
                {
                    violations.add(relativePath + "#" + handler.name
                            + " must not use RuoYi admin login context `" + matcher.group()
                            + "`; derive terminal actor from PortalSessionContext instead");
                }
            }
        }

        if (!violations.isEmpty())
        {
            fail(terminal + " portal handlers must not use admin login context:\n"
                    + String.join("\n", violations));
        }
    }

    private List<Path> discoverProtectedPortalControllers(Path backendRoot, String terminal) throws IOException
    {
        Path controllerRoot = backendRoot.resolve(terminal)
                .resolve("src/main/java/com/ruoyi/" + terminal + "/controller");
        try (Stream<Path> paths = Files.walk(controllerRoot))
        {
            return paths.filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .filter(path -> !path.getFileName().toString().startsWith("Admin"))
                    .filter(path -> !path.getFileName().toString().endsWith("PortalAuthController.java"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private void collectTerminalRouteViolation(Path backendRoot, Path path, List<String> violations)
    {
        try
        {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = TERMINAL_MAPPING.matcher(source);
            if (matcher.find())
            {
                violations.add(backendRoot.relativize(path).toString() + " -> " + matcher.group());
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to read " + path, e);
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
                    String declaration = collectMethodDeclaration(lines, i);
                    String methodBody = collectMethodBody(lines, i);
                    handlers.add(new HandlerMethod(extractMethodName(lines[i]), annotationText, declaration,
                            methodBody));
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
            String line = lines[i];
            declaration.append(line).append('\n');
            if (line.contains("{"))
            {
                break;
            }
        }
        return declaration.toString();
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

    private String extractMethodName(String methodLine)
    {
        Matcher matcher = PUBLIC_METHOD.matcher(methodLine);
        return matcher.find() ? matcher.group(1) : methodLine.trim();
    }

    private void assertHandlerSecurityTemplate(String terminal, String relativePath, HandlerMethod handler,
            List<String> violations)
    {
        String prefix = relativePath + "#" + handler.name;
        if (!handler.annotations.contains("@Anonymous"))
        {
            violations.add(prefix + " must declare method-level @Anonymous");
        }
        if (!terminalAnnotation("PortalPreAuthorize", terminal).matcher(handler.annotations).find())
        {
            violations.add(prefix + " must declare @PortalPreAuthorize(terminal = \"" + terminal + "\")");
        }
        if (!terminalAnnotation("PortalLog", terminal).matcher(handler.annotations).find())
        {
            violations.add(prefix + " must declare @PortalLog(terminal = \"" + terminal + "\")");
        }
        if (!handler.body.contains("PortalSessionContext.requireSession(\"" + terminal + "\")"))
        {
            violations.add(prefix + " must derive identity from PortalSessionContext.requireSession(\""
                    + terminal + "\")");
        }
    }

    private Pattern terminalAnnotation(String annotationName, String terminal)
    {
        return Pattern.compile("@" + annotationName + "\\s*\\([^)]*terminal\\s*=\\s*\"" + terminal + "\"",
                Pattern.DOTALL);
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
            if (Files.isDirectory(candidate.resolve("product/src/main/java"))
                    && Files.isDirectory(candidate.resolve("seller/src/main/java"))
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
