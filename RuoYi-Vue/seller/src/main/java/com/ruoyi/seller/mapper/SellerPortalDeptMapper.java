package com.ruoyi.seller.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.PortalDept;

/**
 * Seller terminal department mapper.
 */
public interface SellerPortalDeptMapper
{
    public List<PortalDept> selectSellerDeptList(PortalDept dept);

    public PortalDept selectSellerDeptById(@Param("sellerId") Long sellerId, @Param("deptId") Long deptId);

    public PortalDept checkSellerDeptNameUnique(@Param("sellerId") Long sellerId, @Param("deptName") String deptName,
            @Param("parentId") Long parentId);

    public int hasChildByDeptId(@Param("sellerId") Long sellerId, @Param("deptId") Long deptId);

    public int checkDeptExistAccount(@Param("sellerId") Long sellerId, @Param("deptId") Long deptId);

    public int insertSellerDept(PortalDept dept);

    public int updateSellerDept(PortalDept dept);

    public int updateSellerDeptAncestors(@Param("sellerId") Long sellerId, @Param("oldAncestors") String oldAncestors,
            @Param("newAncestors") String newAncestors, @Param("updateBy") String updateBy);

    public int deleteSellerDeptById(@Param("sellerId") Long sellerId, @Param("deptId") Long deptId,
            @Param("updateBy") String updateBy);
}
