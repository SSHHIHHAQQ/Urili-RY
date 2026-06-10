package com.ruoyi.system.service.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import java.lang.reflect.Field;
import org.junit.Test;
import com.ruoyi.common.exception.ServiceException;

public class SecretCipherSupportTest
{
    @Test
    public void encryptAndDecryptShouldRoundTripWithConfiguredKey()
    {
        SecretCipherSupport support = support("0123456789abcdef0123456789abcdef", "local-v1");

        String ciphertext = support.encrypt("seller-api-secret");

        assertNotEquals("seller-api-secret", ciphertext);
        assertEquals("seller-api-secret", support.decrypt(ciphertext));
        assertEquals("local-v1", support.getEncryptionKeyId());
    }

    @Test
    public void encryptShouldFailClosedWhenKeyIsMissing()
    {
        SecretCipherSupport support = support("", "local-v1");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> support.encrypt("seller-api-secret"));

        assertEquals("缺少 URILI_SECRET_ENCRYPTION_KEY，不能保存或使用外部系统凭证", exception.getMessage());
    }

    @Test
    public void getEncryptionKeyIdShouldUseDefaultWhenBlank()
    {
        SecretCipherSupport support = support("0123456789abcdef0123456789abcdef", "");

        assertEquals("local-v1", support.getEncryptionKeyId());
    }

    private static SecretCipherSupport support(String encryptionKey, String encryptionKeyId)
    {
        SecretCipherSupport support = new SecretCipherSupport();
        inject(support, "encryptionKey", encryptionKey);
        inject(support, "encryptionKeyId", encryptionKeyId);
        return support;
    }

    private static void inject(Object target, String fieldName, Object value)
    {
        try
        {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch (Exception ex)
        {
            throw new AssertionError(ex);
        }
    }
}
