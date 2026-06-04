package com.ruoyi.seller.service;

import java.util.List;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalTreeSelect;

/**
 * Seller terminal department service.
 */
public interface ISellerPortalDeptService
{
    public List<PortalDept> selectDeptList(Long sellerId, PortalDept dept);

    public PortalDept selectDeptById(Long sellerId, Long deptId);

    public List<PortalTreeSelect> buildDeptTreeSelect(Long sellerId, PortalDept dept);

    public boolean checkDeptNameUnique(PortalDept dept);

    public boolean hasChildByDeptId(Long sellerId, Long deptId);

    public boolean checkDeptExistAccount(Long deptId);

    public int insertDept(Long sellerId, PortalDept dept);

    public int updateDept(Long sellerId, PortalDept dept);

    public int deleteDeptById(Long sellerId, Long deptId);
}
