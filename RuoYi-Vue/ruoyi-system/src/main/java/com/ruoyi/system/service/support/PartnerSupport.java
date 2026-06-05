package com.ruoyi.system.service.support;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PartnerProfile;
import com.ruoyi.system.domain.PartnerProfile.Attachment;

/**
 * Shared internal helpers for buyer/seller modules.
 */
public class PartnerSupport
{
    public static final String STATUS_NORMAL = "0";

    public static final String ACCOUNT_LOCK_STATUS_UNLOCKED = "0";

    public static final String ACCOUNT_LOCK_STATUS_LOCKED = "1";

    public static final String ACCOUNT_ROLE_OWNER = "OWNER";

    public static final String ACCOUNT_ROLE_ADMIN = "ADMIN";

    public static final String ACCOUNT_ROLE_STAFF = "STAFF";

    public static final String SUBJECT_TYPE_COMPANY = "COMPANY";

    public static final String DEFAULT_LEVEL = "L1";

    public static final String DEFAULT_OWNER_PASSWORD = "U12346";

    private static final List<String> SUBJECT_TYPES = List.of("COMPANY", "PERSON", "OTHER");

    private static final List<String> ACCOUNT_ROLES = List.of(ACCOUNT_ROLE_OWNER, ACCOUNT_ROLE_ADMIN,
            ACCOUNT_ROLE_STAFF);

    private static final int NO_BASE_YEAR = 2026;

    private static final int NO_MAX_DAILY_SEQUENCE = 9999;

    private static final String MANAGED_ATTACHMENT_PREFIX = Constants.RESOURCE_PREFIX + "/";

    private static final List<String> ATTACHMENT_EXTENSIONS = List.of("bmp", "gif", "jpg", "jpeg", "png", "pdf", "doc",
            "docx", "xls", "xlsx", "csv", "txt");

    private PartnerSupport()
    {
    }

    public static void normalizeCommonProfile(PartnerProfile profile)
    {
        normalizeCommonProfile(profile, null);
    }

    public static void normalizeCommonProfile(PartnerProfile profile, String unchangedLegacyAttachmentUrl)
    {
        profile.setUsername(StringUtils.trimToEmpty(profile.getUsername()));
        profile.setStatus(StringUtils.defaultIfBlank(profile.getStatus(), STATUS_NORMAL));
        profile.setLegalId(StringUtils.trimToEmpty(profile.getLegalId()));
        profile.setBusinessLicenseNo(StringUtils.trimToEmpty(profile.getBusinessLicenseNo()));
        profile.setCountryCode(trimRequired(profile.getCountryCode(), "国家/地区不能为空").toUpperCase());
        if (profile.getCountryCode().length() != 2)
        {
            throw new ServiceException("国家/地区代码必须是2位代码");
        }
        profile.setStateProvince(StringUtils.trimToEmpty(profile.getStateProvince()));
        profile.setCity(trimRequired(profile.getCity(), "城市不能为空"));
        profile.setPostalCode(trimRequired(profile.getPostalCode(), "邮编不能为空"));
        profile.setAddressLine1(trimRequired(profile.getAddressLine1(), "地址1不能为空"));
        profile.setAddressLine2(StringUtils.trimToEmpty(profile.getAddressLine2()));
        profile.setContactName(trimRequired(profile.getContactName(), "联系人不能为空"));
        profile.setContactPhone(trimRequired(profile.getContactPhone(), "手机号不能为空"));
        profile.setContactEmail(StringUtils.trimToEmpty(profile.getContactEmail()));
        normalizeAttachment(profile, unchangedLegacyAttachmentUrl);
    }

    public static String normalizeSubjectType(String value)
    {
        String type = trimRequired(StringUtils.defaultIfBlank(value, SUBJECT_TYPE_COMPANY), "主体类型不能为空").toUpperCase();
        if (!SUBJECT_TYPES.contains(type))
        {
            throw new ServiceException("主体类型不正确");
        }
        return type;
    }

    public static String normalizeAccountRole(String value)
    {
        String role = trimRequired(StringUtils.defaultIfBlank(value, ACCOUNT_ROLE_STAFF), "账号角色不能为空").toUpperCase();
        if (!ACCOUNT_ROLES.contains(role))
        {
            throw new ServiceException("账号角色不正确");
        }
        return role;
    }

    public static String normalizeLevel(String value, String message)
    {
        return trimRequired(StringUtils.defaultIfBlank(value, DEFAULT_LEVEL), message).toUpperCase();
    }

    public static void assertStatus(String status)
    {
        if (!STATUS_NORMAL.equals(status) && !"1".equals(status))
        {
            throw new ServiceException("状态不正确");
        }
    }

    public static String normalizeAccountLockStatus(String value)
    {
        String status = StringUtils.defaultIfBlank(value, ACCOUNT_LOCK_STATUS_UNLOCKED);
        if (!ACCOUNT_LOCK_STATUS_UNLOCKED.equals(status) && !ACCOUNT_LOCK_STATUS_LOCKED.equals(status))
        {
            throw new ServiceException("锁定状态不正确");
        }
        return status;
    }

    public static boolean isAccountLocked(String lockStatus)
    {
        return ACCOUNT_LOCK_STATUS_LOCKED.equals(lockStatus);
    }

    public static synchronized String generateNo(String prefix, Function<String, String> maxNoLookup)
    {
        LocalDate today = LocalDate.now();
        String noPrefix = buildNoPrefix(prefix, today);
        String maxNo = maxNoLookup.apply(noPrefix);
        int nextSequence = 1;

        if (StringUtils.isNotBlank(maxNo))
        {
            String sequenceText = maxNo.substring(noPrefix.length());
            nextSequence = Integer.parseInt(sequenceText) + 1;
        }

        if (nextSequence > NO_MAX_DAILY_SEQUENCE)
        {
            throw new ServiceException("今日编号已达上限9999");
        }

        return noPrefix + String.format("%04d", nextSequence);
    }

    public static String buildOwnerNickName(String name, String shortName, String contactName)
    {
        String value = StringUtils.defaultIfBlank(contactName, StringUtils.defaultIfBlank(shortName, name));
        return limitLength(value, 30);
    }

    public static String trimRequired(String value, String message)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(trimmed))
        {
            throw new ServiceException(message);
        }
        return trimmed;
    }

    public static String limitLength(String value, int maxLength)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (trimmed.length() <= maxLength)
        {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    public static String normalizePasswordChange(String oldPassword, String newPassword, String confirmPassword)
    {
        if (StringUtils.isEmpty(oldPassword))
        {
            throw new ServiceException("旧密码不能为空");
        }
        if (StringUtils.isEmpty(newPassword))
        {
            throw new ServiceException("新密码不能为空");
        }
        if (newPassword.length() < UserConstants.PASSWORD_MIN_LENGTH
                || newPassword.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }
        if (StringUtils.isEmpty(confirmPassword))
        {
            throw new ServiceException("确认密码不能为空");
        }
        if (!StringUtils.equals(newPassword, confirmPassword))
        {
            throw new ServiceException("两次密码输入不一致");
        }
        return newPassword;
    }

    private static String buildNoPrefix(String prefix, LocalDate date)
    {
        int yearOffset = date.getYear() - NO_BASE_YEAR;
        if (yearOffset < 0 || yearOffset > 25)
        {
            throw new ServiceException("编号年份超出编码范围");
        }

        char yearCode = (char) ('A' + yearOffset);
        char monthCode = (char) ('A' + date.getMonthValue() - 1);
        return prefix + yearCode + monthCode + String.format("%02d", date.getDayOfMonth());
    }

    private static void normalizeAttachment(PartnerProfile profile, String unchangedLegacyAttachmentUrl)
    {
        Attachment attachment = profile.getAttachment();
        if (attachment == null)
        {
            profile.setAttachment(null);
            return;
        }

        attachment.setFileName(trimRequired(attachment.getFileName(), "附件名称不能为空"));
        attachment.setMimeType(trimRequired(StringUtils.defaultIfBlank(attachment.getMimeType(), "application/octet-stream"), "附件类型不能为空"));
        if (attachment.getSizeBytes() == null || attachment.getSizeBytes() < 0)
        {
            attachment.setSizeBytes(0L);
        }
        String fileUrl = trimRequired(attachment.getFileUrl(), "附件地址不能为空");
        validateAttachmentFileUrl(fileUrl, unchangedLegacyAttachmentUrl);
        validateAttachmentExtension(fileUrl);
        attachment.setFileUrl(fileUrl);
        profile.setAttachment(attachment);
    }

    private static void validateAttachmentFileUrl(String fileUrl, String unchangedLegacyAttachmentUrl)
    {
        if (StringUtils.startsWith(fileUrl, MANAGED_ATTACHMENT_PREFIX))
        {
            return;
        }

        if (StringUtils.equals(fileUrl, unchangedLegacyAttachmentUrl) && StringUtils.startsWithIgnoreCase(fileUrl, "data:"))
        {
            return;
        }

        throw new ServiceException("附件必须先通过文件服务上传");
    }

    private static void validateAttachmentExtension(String fileUrl)
    {
        if (!StringUtils.startsWith(fileUrl, MANAGED_ATTACHMENT_PREFIX))
        {
            return;
        }

        String path = StringUtils.substringBefore(fileUrl, "?");
        String extension = StringUtils.substringAfterLast(path, ".").toLowerCase();
        if (!ATTACHMENT_EXTENSIONS.contains(extension))
        {
            throw new ServiceException("附件类型仅支持图片、PDF、Word、Excel、CSV 或 TXT");
        }
    }
}
