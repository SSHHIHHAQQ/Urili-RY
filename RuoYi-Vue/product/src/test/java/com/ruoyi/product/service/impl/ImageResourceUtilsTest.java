package com.ruoyi.product.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.file.ImageResourceUtils;

public class ImageResourceUtilsTest
{
    @Test
    public void normalizeStoredImageResourceRejectsDataUrl()
    {
        try
        {
            ImageResourceUtils.normalizeStoredImageResource("data:image/png;base64,iVBORw0KGgo=", "商品图片");
        }
        catch (ServiceException e)
        {
            assertEquals("商品图片必须先上传到文件存储，不能保存 base64 内容", e.getMessage());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void inlineImageDetectsRawImageBase64Signatures()
    {
        assertTrue(ImageResourceUtils.isInlineImage("/9j/" + "A".repeat(120)));
        assertTrue(ImageResourceUtils.isInlineImage("iVBOR" + "A".repeat(120)));
        assertFalse(ImageResourceUtils.isInlineImage("/profile/upload/2026/06/10/demo.png"));
    }

    @Test
    public void normalizeStoredImageResourceKeepsStableProfilePath()
    {
        assertEquals("/profile/upload/2026/06/10/a.png",
            ImageResourceUtils.normalizeStoredImageResource("/api/profile/upload/2026/06/10/a.png", "商品图片"));
        assertEquals("/profile/upload/2026/06/10/a.png",
            ImageResourceUtils.normalizeStoredImageResource(
                "https://cdn.example.com/profile/upload/2026/06/10/a.png?q-sign-time=1#preview", "商品图片"));
    }

    @Test
    public void normalizeDetailContentImageResourcesNormalizesImageUrlBlocks()
    {
        String content = "{\"version\":1,\"blocks\":[{\"type\":\"IMAGE\",\"imageUrl\":\"/api/profile/upload/a.png\"}]}";

        String result = ImageResourceUtils.normalizeDetailContentImageResources(content, "详情图文图片");

        assertTrue(result.contains("\"imageUrl\":\"/profile/upload/a.png\""));
    }
    @Test
    public void redactInlineImagePayloadsRemovesDataUrlBody()
    {
        String result = ImageResourceUtils.redactInlineImagePayloads(
            "{\"dataUrl\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJ\"}");

        assertFalse(result.contains("data:image"));
        assertFalse(result.contains("iVBOR"));
        assertTrue(result.contains("inline image"));
    }
}
