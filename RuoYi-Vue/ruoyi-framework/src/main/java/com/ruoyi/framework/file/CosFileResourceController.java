package com.ruoyi.framework.file;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.ruoyi.common.constant.Constants;

/**
 * Redirects stable /profile paths to COS objects when COS storage is active.
 */
@Controller
@ConditionalOnProperty(prefix = "ruoyi.file-storage", name = "type", havingValue = "cos")
public class CosFileResourceController
{
    private final CosFileStorageService cosFileStorageService;

    public CosFileResourceController(CosFileStorageService cosFileStorageService)
    {
        this.cosFileStorageService = cosFileStorageService;
    }

    @GetMapping(Constants.RESOURCE_PREFIX + "/**")
    public void redirectToCos(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String resourcePath = requestUri.substring(contextPath.length());
        response.sendRedirect(cosFileStorageService.resolveUrl(resourcePath));
    }
}
