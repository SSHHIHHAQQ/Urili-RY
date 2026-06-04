package com.ruoyi.integration.support;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;

/**
 * 本地运行密钥加密支持。
 */
@Component
public class SecretCipherSupport
{
    private static final String CIPHER_PREFIX = "v1:";

    private static final int IV_LENGTH = 12;

    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${urili.secret.encryption-key:}")
    private String encryptionKey;

    @Value("${urili.secret.encryption-key-id:default}")
    private String encryptionKeyId;

    public String encrypt(String plaintext)
    {
        if (StringUtils.isBlank(plaintext))
        {
            throw new ServiceException("凭证不能为空");
        }
        try
        {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        }
        catch (Exception ex)
        {
            throw new ServiceException("凭证加密失败");
        }
    }

    public String decrypt(String ciphertext)
    {
        if (StringUtils.isBlank(ciphertext))
        {
            throw new ServiceException("凭证未配置");
        }
        try
        {
            String encoded = ciphertext.startsWith(CIPHER_PREFIX) ? ciphertext.substring(CIPHER_PREFIX.length()) : ciphertext;
            byte[] payload = Base64.getDecoder().decode(encoded);
            if (payload.length <= IV_LENGTH)
            {
                throw new ServiceException("凭证密文格式错误");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("凭证解密失败，请检查加密密钥配置");
        }
    }

    public String getEncryptionKeyId()
    {
        return StringUtils.defaultIfBlank(encryptionKeyId, "default");
    }

    private SecretKeySpec buildKey() throws Exception
    {
        if (StringUtils.isBlank(encryptionKey))
        {
            throw new ServiceException("缺少 URILI_SECRET_ENCRYPTION_KEY，不能保存或使用上游系统凭证");
        }
        byte[] raw = encryptionKey.trim().getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes;
        try
        {
            byte[] decoded = Base64.getDecoder().decode(encryptionKey.trim());
            keyBytes = decoded.length >= 32 ? decoded : raw;
        }
        catch (IllegalArgumentException ex)
        {
            keyBytes = raw;
        }
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32)
        {
            keyBytes = MessageDigest.getInstance("SHA-256").digest(keyBytes);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
