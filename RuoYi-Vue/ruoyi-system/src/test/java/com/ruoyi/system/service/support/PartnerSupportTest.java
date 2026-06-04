package com.ruoyi.system.service.support;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
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
}
