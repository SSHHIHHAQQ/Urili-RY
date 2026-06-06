package com.ruoyi.system.service.support;

import static org.junit.Assert.assertThrows;
import org.junit.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalDept;

public class PortalDeptSupportTest
{
    @Test
    public void assertDeptParentNotDescendantRejectsMovingDeptUnderItsChild()
    {
        PortalDept dept = dept(10L, "0");
        PortalDept child = dept(12L, "0,10");

        assertThrows(ServiceException.class, () -> PortalDeptSupport.assertDeptParentNotDescendant(dept, child));
    }

    @Test
    public void assertDeptParentNotDescendantAllowsSiblingParent()
    {
        PortalDept dept = dept(10L, "0");
        PortalDept sibling = dept(12L, "0");

        PortalDeptSupport.assertDeptParentNotDescendant(dept, sibling);
    }

    private PortalDept dept(Long deptId, String ancestors)
    {
        PortalDept dept = new PortalDept();
        dept.setDeptId(deptId);
        dept.setAncestors(ancestors);
        return dept;
    }
}
