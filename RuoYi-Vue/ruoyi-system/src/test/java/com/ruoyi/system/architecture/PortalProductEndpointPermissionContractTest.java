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
import org.junit.Test;

public class PortalProductEndpointPermissionContractTest
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
    public void portalProductEndpointsMustUseTerminalPermissionsAndSessionScope() throws IOException
    {
        Path backendRoot = findBackendRoot();
        List<String> violations = new ArrayList<>();

        assertProductSchemaController(backendRoot, "seller", violations);
        assertProductSchemaController(backendRoot, "buyer", violations);
        assertSellerDistributionController(backendRoot, violations);
        assertBuyerDistributionController(backendRoot, violations);

        if (!violations.isEmpty())
        {
            fail("portal product endpoints must keep terminal permission and session contracts:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void terminalProductServicesMustUseReadOnlyProductPortalPort() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path sellerService = backendRoot.resolve(
                "seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java");
        Path buyerService = backendRoot.resolve(
                "buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java");
        Path productCenterService = backendRoot.resolve(
                "product/src/main/java/com/ruoyi/product/service/impl/ProductCenterServiceImpl.java");
        String sellerSource = Files.readString(sellerService, StandardCharsets.UTF_8);
        String buyerSource = Files.readString(buyerService, StandardCharsets.UTF_8);
        String productCenterSource = Files.readString(productCenterService, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        assertUsesReadOnlyProductPort(sellerService, sellerSource, violations);
        assertUsesReadOnlyProductPort(buyerService, buyerSource, violations);
        assertUsesReadOnlyProductPort(productCenterService, productCenterSource, violations);

        if (!sellerSource.contains("toPortalProduct(product, session.getSubjectId())"))
        {
            violations.add(relative(sellerService)
                    + " list/detail mapping must pass current sellerId into embedded SKU mapping");
        }
        if (!sellerSource.contains("productPortalDistributionService.selectSellerSkuList(product.getSpuId(), sellerId)"))
        {
            violations.add(relative(sellerService)
                    + " embedded SKU mapping must use selectSellerSkuList(spuId, sellerId)");
        }
        if (!sellerSource.contains("productPortalDistributionService.selectSellerProductById(spuId, session.getSubjectId())"))
        {
            violations.add(relative(sellerService)
                    + " product detail ownership lookup must use selectSellerProductById(spuId, sellerId)");
        }
        if (sellerSource.contains("result.setSkus(toPortalSkus(product.getSkus()))"))
        {
            violations.add(relative(sellerService)
                    + " must not expose ProductSpu#getSkus() directly on seller portal list/detail responses");
        }
        String compactSource = sellerSource.replaceAll("\\s+", "");
        if (compactSource.contains("productPortalDistributionService.selectSellerProductById(spuId)"))
        {
            violations.add(relative(sellerService)
                    + " must not call single-argument selectSellerProductById(spuId) from seller portal");
        }
        if (compactSource.contains("productPortalDistributionService.selectSellerSkuList(spuId)"))
        {
            violations.add(relative(sellerService)
                    + " must not call single-argument selectSellerSkuList(spuId) from seller portal");
        }

        if (!violations.isEmpty())
        {
            fail("terminal product services must use the read-only product portal port and keep seller scope:\n"
                    + String.join("\n", violations));
        }
    }

    private void assertUsesReadOnlyProductPort(Path service, String source, List<String> violations)
    {
        if (source.contains("IProductDistributionService"))
        {
            violations.add(relative(service)
                    + " must not inject the read/write IProductDistributionService into terminal read-only surfaces");
        }
        if (source.contains("ProductSellerLookupService"))
        {
            violations.add(relative(service)
                    + " must not lookup seller snapshots from terminal/product-center read-only surfaces");
        }
        if (!source.contains("IProductPortalDistributionService"))
        {
            violations.add(relative(service)
                    + " must inject IProductPortalDistributionService for terminal/product-center read-only queries");
        }
    }

    private void assertProductSchemaController(Path backendRoot, String terminal, List<String> violations)
            throws IOException
    {
        Path controller = backendRoot.resolve(terminal + "/src/main/java/com/ruoyi/" + terminal
                + "/controller/" + capitalize(terminal) + "PortalProductSchemaController.java");
        List<HandlerMethod> handlers = extractHandlerMethods(Files.readString(controller, StandardCharsets.UTF_8));

        assertPortalHandler(controller, handlers, "categories", terminal, terminal + ":product:category:list",
                "PortalSessionContext.requireSession(\"" + terminal + "\")",
                "productPortalSchemaService.selectPortalCategories()", violations);
        assertPortalHandler(controller, handlers, "schema", terminal, terminal + ":product:schema:query",
                "PortalSessionContext.requireSession(\"" + terminal + "\")",
                "productPortalSchemaService.selectPortalSchema(categoryId)", violations);
    }

    private void assertSellerDistributionController(Path backendRoot, List<String> violations) throws IOException
    {
        Path controller = backendRoot.resolve(
                "seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java");
        List<HandlerMethod> handlers = extractHandlerMethods(Files.readString(controller, StandardCharsets.UTF_8));

        assertPortalHandler(controller, handlers, "list", "seller", "seller:product:distribution:list",
                "PortalSessionContext.requireSession(\"seller\")",
                "sellerPortalProductService.selectOwnProductList(session, query)", violations);
        assertPortalHandler(controller, handlers, "get", "seller", "seller:product:distribution:query",
                "PortalSessionContext.requireSession(\"seller\")",
                "sellerPortalProductService.selectOwnProductById(session, spuId)", violations);
        assertPortalHandler(controller, handlers, "skus", "seller", "seller:product:distribution:query",
                "PortalSessionContext.requireSession(\"seller\")",
                "sellerPortalProductService.selectOwnSkuList(session, spuId)", violations);
    }

    private void assertBuyerDistributionController(Path backendRoot, List<String> violations) throws IOException
    {
        Path controller = backendRoot.resolve(
                "buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductDistributionController.java");
        List<HandlerMethod> handlers = extractHandlerMethods(Files.readString(controller, StandardCharsets.UTF_8));

        assertPortalHandler(controller, handlers, "list", "buyer", "buyer:product:distribution:list",
                "PortalSessionContext.requireSession(\"buyer\")",
                "buyerPortalProductService.selectVisibleProductList(session, query)", violations);
        assertPortalHandler(controller, handlers, "get", "buyer", "buyer:product:distribution:query",
                "PortalSessionContext.requireSession(\"buyer\")",
                "buyerPortalProductService.selectVisibleProductById(session, spuId)", violations);
        assertPortalHandler(controller, handlers, "skus", "buyer", "buyer:product:distribution:query",
                "PortalSessionContext.requireSession(\"buyer\")",
                "buyerPortalProductService.selectVisibleSkuList(session, spuId)", violations);
    }

    private void assertPortalHandler(Path controller, List<HandlerMethod> handlers, String methodName, String terminal,
            String expectedPermission, String expectedSessionCall, String expectedServiceCall, List<String> violations)
    {
        HandlerMethod handler = handlers.stream()
                .filter(item -> methodName.equals(item.name))
                .findFirst()
                .orElse(null);
        String prefix = relative(controller) + "#" + methodName;
        if (handler == null)
        {
            violations.add(prefix + " must exist");
            return;
        }
        if (!handler.annotations.contains("@Anonymous"))
        {
            violations.add(prefix + " must be @Anonymous for portal token filter boundary");
        }
        if (!handler.annotations.contains("@PortalPreAuthorize(terminal = \"" + terminal + "\", hasPermi = \""
                + expectedPermission + "\")"))
        {
            violations.add(prefix + " must require " + expectedPermission);
        }
        if (!handler.annotations.contains("@PortalLog(terminal = \"" + terminal + "\""))
        {
            violations.add(prefix + " must declare @PortalLog terminal " + terminal);
        }
        if (!handler.annotations.contains("isSaveResponseData = false"))
        {
            violations.add(prefix + " must not save portal product responses in audit logs");
        }
        if (!handler.body.contains(expectedSessionCall))
        {
            violations.add(prefix + " must derive scope from current " + terminal + " session");
        }
        if (!handler.body.contains(expectedServiceCall))
        {
            violations.add(prefix + " must delegate through terminal service call " + expectedServiceCall);
        }
        for (String paramName : FORBIDDEN_SCOPE_PARAM_NAMES)
        {
            if (containsWord(handler.declaration, paramName))
            {
                violations.add(prefix + " must not accept client identity scope parameter " + paramName);
            }
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
