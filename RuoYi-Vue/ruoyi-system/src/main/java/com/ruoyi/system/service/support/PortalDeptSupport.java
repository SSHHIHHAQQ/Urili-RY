package com.ruoyi.system.service.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalTreeSelect;

/**
 * Shared validation and tree helpers for seller/buyer terminal departments.
 */
public class PortalDeptSupport
{
    public static final Long DEPT_ROOT_ID = 0L;

    private PortalDeptSupport()
    {
    }

    public static void normalizeDept(PortalDept dept, Long subjectId)
    {
        if (dept == null)
        {
            throw new ServiceException("部门不能为空");
        }
        if (subjectId == null)
        {
            throw new ServiceException("主体ID不能为空");
        }
        dept.setSubjectId(subjectId);
        dept.setDeptName(PartnerSupport.trimRequired(dept.getDeptName(), "部门名称不能为空"));
        dept.setParentId(dept.getParentId() == null ? DEPT_ROOT_ID : dept.getParentId());
        dept.setOrderNum(dept.getOrderNum() == null ? 0 : dept.getOrderNum());
        dept.setLeader(StringUtils.trimToEmpty(dept.getLeader()));
        dept.setPhone(StringUtils.trimToEmpty(dept.getPhone()));
        dept.setEmail(StringUtils.trimToEmpty(dept.getEmail()));
        dept.setStatus(StringUtils.defaultIfBlank(dept.getStatus(), UserConstants.NORMAL));
        dept.setRemark(StringUtils.trimToEmpty(dept.getRemark()));
        PartnerSupport.assertStatus(dept.getStatus());
    }

    public static void assertDeptNotSelfParent(PortalDept dept)
    {
        if (dept.getDeptId() != null && dept.getDeptId().equals(dept.getParentId()))
        {
            throw new ServiceException("上级部门不能选择自己");
        }
    }

    public static String buildAncestors(PortalDept parent)
    {
        if (parent == null)
        {
            return "0";
        }
        return StringUtils.defaultIfBlank(parent.getAncestors(), "0") + "," + parent.getDeptId();
    }

    public static List<PortalDept> buildDeptTree(List<PortalDept> depts)
    {
        List<PortalDept> returnList = new ArrayList<>();
        List<Long> tempList = depts.stream().map(PortalDept::getDeptId).collect(Collectors.toList());
        for (Iterator<PortalDept> iterator = depts.iterator(); iterator.hasNext();)
        {
            PortalDept dept = iterator.next();
            dept.setChildren(new ArrayList<>());
            if (!tempList.contains(dept.getParentId()))
            {
                recursionFn(depts, dept);
                returnList.add(dept);
            }
        }
        return returnList.isEmpty() ? depts : returnList;
    }

    public static List<PortalTreeSelect> buildDeptTreeSelect(List<PortalDept> depts)
    {
        return buildDeptTree(depts).stream().map(PortalTreeSelect::new).collect(Collectors.toList());
    }

    private static void recursionFn(List<PortalDept> list, PortalDept t)
    {
        List<PortalDept> childList = getChildList(list, t);
        t.setChildren(childList);
        for (PortalDept child : childList)
        {
            if (!getChildList(list, child).isEmpty())
            {
                recursionFn(list, child);
            }
        }
    }

    private static List<PortalDept> getChildList(List<PortalDept> list, PortalDept t)
    {
        List<PortalDept> tlist = new ArrayList<>();
        for (PortalDept n : list)
        {
            if (n.getParentId() != null && n.getParentId().longValue() == t.getDeptId().longValue())
            {
                tlist.add(n);
            }
        }
        return tlist;
    }
}
