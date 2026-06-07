package com.ruoyi.system.service.support;

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.junit.Test;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;

public class PartnerSupportTest
{
    @Test
    public void normalizePasswordChangeReturnsValidNewPassword()
    {
        assertEquals("New123",
                PartnerSupport.normalizePasswordChange("Old123", "New123", "New123"));
    }

    @Test(expected = ServiceException.class)
    public void normalizePasswordChangeRejectsMismatchedConfirmation()
    {
        PartnerSupport.normalizePasswordChange("Old123", "New123", "Other123");
    }

    @Test(expected = ServiceException.class)
    public void normalizePasswordChangeRejectsShortPassword()
    {
        PartnerSupport.normalizePasswordChange("Old123", "N123", "N123");
    }

    @Test
    public void normalizeAccountRoleDefaultsAndUppercasesKnownRoles()
    {
        assertEquals(PartnerSupport.ACCOUNT_ROLE_STAFF, PartnerSupport.normalizeAccountRole(null));
        assertEquals(PartnerSupport.ACCOUNT_ROLE_OWNER, PartnerSupport.normalizeAccountRole(" owner "));
        assertEquals(PartnerSupport.ACCOUNT_ROLE_ADMIN, PartnerSupport.normalizeAccountRole("admin"));
    }

    @Test(expected = ServiceException.class)
    public void normalizeAccountRoleRejectsUnknownRole()
    {
        PartnerSupport.normalizeAccountRole("root");
    }

    @Test
    public void normalizeAccountLockStatusDefaultsAndAcceptsLockedValue()
    {
        assertEquals(PartnerSupport.ACCOUNT_LOCK_STATUS_UNLOCKED, PartnerSupport.normalizeAccountLockStatus(null));
        assertEquals(PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED, PartnerSupport.normalizeAccountLockStatus("1"));
    }

    @Test(expected = ServiceException.class)
    public void normalizeAccountLockStatusRejectsUnknownValue()
    {
        PartnerSupport.normalizeAccountLockStatus("2");
    }

    @Test
    public void assertCountryRegionCodeAcceptsConfiguredCodeIgnoringCase()
    {
        PartnerSupport.assertCountryRegionCode(" cn ", List.of(dict("CN"), dict("US")));
    }

    @Test(expected = ServiceException.class)
    public void assertCountryRegionCodeRejectsCodeOutsideDictionary()
    {
        PartnerSupport.assertCountryRegionCode("ZZ", List.of(dict("CN"), dict("US")));
    }

    @Test(expected = ServiceException.class)
    public void assertCountryRegionCodeRejectsNonLetterCodeBeforeDictionaryMatch()
    {
        PartnerSupport.assertCountryRegionCode("1@", List.of(dict("1@")));
    }

    @Test(expected = ServiceException.class)
    public void assertCountryRegionCodeFailsClosedWhenDictionaryIsMissing()
    {
        PartnerSupport.assertCountryRegionCode("CN", List.of());
    }

    private SysDictData dict(String value)
    {
        SysDictData data = new SysDictData();
        data.setDictValue(value);
        return data;
    }
}
