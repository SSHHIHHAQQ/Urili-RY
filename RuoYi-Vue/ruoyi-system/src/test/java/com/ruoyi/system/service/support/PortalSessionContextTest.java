package com.ruoyi.system.service.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.After;
import org.junit.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalLoginSession;

public class PortalSessionContextTest
{
    @After
    public void tearDown()
    {
        PortalSessionContext.clear();
    }

    @Test
    public void getSessionReturnsOnlyExpectedTerminal()
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal("seller");
        session.setSubjectId(9L);
        session.setAccountId(8L);

        PortalSessionContext.setSession(session);

        assertEquals(session, PortalSessionContext.getSession("seller"));
        assertNull(PortalSessionContext.getSession("buyer"));
    }

    @Test(expected = ServiceException.class)
    public void requireSessionRejectsMissingTerminal()
    {
        PortalSessionContext.requireSession("seller");
    }

    @Test
    public void clearRemovesCurrentSession()
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal("buyer");
        PortalSessionContext.setSession(session);

        PortalSessionContext.clear();

        assertNull(PortalSessionContext.getSession("buyer"));
    }
}
