package com.ruoyi.system.domain;

import java.io.Serializable;

/**
 * Portal token issue payload plus its persisted session snapshot.
 */
public class PortalLoginIssue implements Serializable
{
    private static final long serialVersionUID = 1L;

    private PortalLoginResult result;

    private PortalLoginSession session;

    public PortalLoginResult getResult()
    {
        return result;
    }

    public void setResult(PortalLoginResult result)
    {
        this.result = result;
    }

    public PortalLoginSession getSession()
    {
        return session;
    }

    public void setSession(PortalLoginSession session)
    {
        this.session = session;
    }
}
