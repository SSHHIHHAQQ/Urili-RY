package com.ruoyi.seller.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.seller.mapper.SellerPortalDeptMapper;
import com.ruoyi.seller.service.ISellerPortalDeptService;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalTreeSelect;
import com.ruoyi.system.service.support.PortalDeptSupport;

/**
 * Seller terminal department service.
 */
@Service
public class SellerPortalDeptServiceImpl implements ISellerPortalDeptService
{
    @Autowired
    private SellerPortalDeptMapper deptMapper;

    @Autowired
    private ISellerService sellerService;

    @Override
    public List<PortalDept> selectDeptList(Long sellerId, PortalDept dept)
    {
        sellerService.selectSellerById(sellerId);
        PortalDept query = dept == null ? new PortalDept() : dept;
        query.setSubjectId(sellerId);
        return deptMapper.selectSellerDeptList(query);
    }

    @Override
    public PortalDept selectDeptById(Long sellerId, Long deptId)
    {
        sellerService.selectSellerById(sellerId);
        PortalDept dept = deptMapper.selectSellerDeptById(sellerId, deptId);
        if (dept == null)
        {
            throw new ServiceException("卖家端部门不存在");
        }
        return dept;
    }

    @Override
    public List<PortalTreeSelect> buildDeptTreeSelect(Long sellerId, PortalDept dept)
    {
        return PortalDeptSupport.buildDeptTreeSelect(selectDeptList(sellerId, dept));
    }

    @Override
    public boolean checkDeptNameUnique(PortalDept dept)
    {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        PortalDept info = deptMapper.checkSellerDeptNameUnique(dept.getSubjectId(), dept.getDeptName(), dept.getParentId());
        return info == null || info.getDeptId().longValue() == deptId.longValue();
    }

    @Override
    public boolean hasChildByDeptId(Long sellerId, Long deptId)
    {
        return deptMapper.hasChildByDeptId(sellerId, deptId) > 0;
    }

    @Override
    public boolean checkDeptExistAccount(Long sellerId, Long deptId)
    {
        return deptMapper.checkDeptExistAccount(sellerId, deptId) > 0;
    }

    @Override
    public int insertDept(Long sellerId, PortalDept dept)
    {
        sellerService.selectSellerById(sellerId);
        PortalDeptSupport.normalizeDept(dept, sellerId);
        setAncestors(sellerId, dept);
        if (!checkDeptNameUnique(dept))
        {
            throw new ServiceException("新增卖家端部门失败，部门名称已存在");
        }
        dept.setCreateBy(SecurityUtils.getUsername());
        return deptMapper.insertSellerDept(dept);
    }

    @Override
    @Transactional
    public int updateDept(Long sellerId, PortalDept dept)
    {
        PortalDept oldDept = selectDeptById(sellerId, dept.getDeptId());
        PortalDeptSupport.normalizeDept(dept, sellerId);
        PortalDeptSupport.assertDeptNotSelfParent(dept);
        setAncestors(sellerId, dept);
        if (!checkDeptNameUnique(dept))
        {
            throw new ServiceException("修改卖家端部门失败，部门名称已存在");
        }
        dept.setUpdateBy(SecurityUtils.getUsername());
        int rows = deptMapper.updateSellerDept(dept);
        if (!StringUtils.equals(oldDept.getAncestors(), dept.getAncestors()))
        {
            deptMapper.updateSellerDeptAncestors(sellerId, oldDept.getAncestors() + "," + oldDept.getDeptId(),
                    dept.getAncestors() + "," + dept.getDeptId(), SecurityUtils.getUsername());
        }
        return rows;
    }

    @Override
    public int deleteDeptById(Long sellerId, Long deptId)
    {
        selectDeptById(sellerId, deptId);
        if (hasChildByDeptId(sellerId, deptId))
        {
            throw new ServiceException("存在子部门，不允许删除");
        }
        if (checkDeptExistAccount(sellerId, deptId))
        {
            throw new ServiceException("部门存在账号，不允许删除");
        }
        return deptMapper.deleteSellerDeptById(sellerId, deptId, SecurityUtils.getUsername());
    }

    private void setAncestors(Long sellerId, PortalDept dept)
    {
        if (PortalDeptSupport.DEPT_ROOT_ID.equals(dept.getParentId()))
        {
            dept.setAncestors("0");
            return;
        }
        PortalDept parent = selectDeptById(sellerId, dept.getParentId());
        PortalDeptSupport.assertDeptParentNotDescendant(dept, parent);
        dept.setAncestors(PortalDeptSupport.buildAncestors(parent));
    }
}
