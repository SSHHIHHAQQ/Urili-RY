package com.ruoyi.buyer.service;

import java.util.List;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalTreeSelect;

/**
 * Buyer terminal department service.
 */
public interface IBuyerPortalDeptService
{
    public List<PortalDept> selectDeptList(Long buyerId, PortalDept dept);

    public PortalDept selectDeptById(Long buyerId, Long deptId);

    public List<PortalTreeSelect> buildDeptTreeSelect(Long buyerId, PortalDept dept);

    public boolean checkDeptNameUnique(PortalDept dept);

    public boolean hasChildByDeptId(Long buyerId, Long deptId);

    public boolean checkDeptExistAccount(Long buyerId, Long deptId);

    public int insertDept(Long buyerId, PortalDept dept);

    public int updateDept(Long buyerId, PortalDept dept);

    public int deleteDeptById(Long buyerId, Long deptId);
}
