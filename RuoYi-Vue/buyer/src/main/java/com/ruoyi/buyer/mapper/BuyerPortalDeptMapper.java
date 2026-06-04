package com.ruoyi.buyer.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.PortalDept;

/**
 * Buyer terminal department mapper.
 */
public interface BuyerPortalDeptMapper
{
    public List<PortalDept> selectBuyerDeptList(PortalDept dept);

    public PortalDept selectBuyerDeptById(@Param("buyerId") Long buyerId, @Param("deptId") Long deptId);

    public PortalDept checkBuyerDeptNameUnique(@Param("buyerId") Long buyerId, @Param("deptName") String deptName,
            @Param("parentId") Long parentId);

    public int hasChildByDeptId(@Param("buyerId") Long buyerId, @Param("deptId") Long deptId);

    public int checkDeptExistAccount(Long deptId);

    public int insertBuyerDept(PortalDept dept);

    public int updateBuyerDept(PortalDept dept);

    public int updateBuyerDeptAncestors(@Param("buyerId") Long buyerId, @Param("oldAncestors") String oldAncestors,
            @Param("newAncestors") String newAncestors, @Param("updateBy") String updateBy);

    public int deleteBuyerDeptById(@Param("buyerId") Long buyerId, @Param("deptId") Long deptId,
            @Param("updateBy") String updateBy);
}
