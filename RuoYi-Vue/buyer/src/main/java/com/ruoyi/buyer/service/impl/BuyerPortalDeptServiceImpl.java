package com.ruoyi.buyer.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.buyer.mapper.BuyerPortalDeptMapper;
import com.ruoyi.buyer.service.IBuyerPortalDeptService;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalTreeSelect;
import com.ruoyi.system.service.support.PortalDeptSupport;

/**
 * Buyer terminal department service.
 */
@Service
public class BuyerPortalDeptServiceImpl implements IBuyerPortalDeptService
{
    @Autowired
    private BuyerPortalDeptMapper deptMapper;

    @Autowired
    private IBuyerService buyerService;

    @Override
    public List<PortalDept> selectDeptList(Long buyerId, PortalDept dept)
    {
        buyerService.selectBuyerById(buyerId);
        PortalDept query = dept == null ? new PortalDept() : dept;
        query.setSubjectId(buyerId);
        return deptMapper.selectBuyerDeptList(query);
    }

    @Override
    public PortalDept selectDeptById(Long buyerId, Long deptId)
    {
        buyerService.selectBuyerById(buyerId);
        PortalDept dept = deptMapper.selectBuyerDeptById(buyerId, deptId);
        if (dept == null)
        {
            throw new ServiceException("买家端部门不存在");
        }
        return dept;
    }

    @Override
    public List<PortalTreeSelect> buildDeptTreeSelect(Long buyerId, PortalDept dept)
    {
        return PortalDeptSupport.buildDeptTreeSelect(selectDeptList(buyerId, dept));
    }

    @Override
    public boolean checkDeptNameUnique(PortalDept dept)
    {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        PortalDept info = deptMapper.checkBuyerDeptNameUnique(dept.getSubjectId(), dept.getDeptName(), dept.getParentId());
        return info == null || info.getDeptId().longValue() == deptId.longValue();
    }

    @Override
    public boolean hasChildByDeptId(Long buyerId, Long deptId)
    {
        return deptMapper.hasChildByDeptId(buyerId, deptId) > 0;
    }

    @Override
    public boolean checkDeptExistAccount(Long buyerId, Long deptId)
    {
        return deptMapper.checkDeptExistAccount(buyerId, deptId) > 0;
    }

    @Override
    public int insertDept(Long buyerId, PortalDept dept)
    {
        buyerService.selectBuyerById(buyerId);
        PortalDeptSupport.normalizeDept(dept, buyerId);
        setAncestors(buyerId, dept);
        if (!checkDeptNameUnique(dept))
        {
            throw new ServiceException("新增买家端部门失败，部门名称已存在");
        }
        dept.setCreateBy(SecurityUtils.getUsername());
        return deptMapper.insertBuyerDept(dept);
    }

    @Override
    @Transactional
    public int updateDept(Long buyerId, PortalDept dept)
    {
        PortalDept oldDept = selectDeptById(buyerId, dept.getDeptId());
        PortalDeptSupport.normalizeDept(dept, buyerId);
        PortalDeptSupport.assertDeptNotSelfParent(dept);
        setAncestors(buyerId, dept);
        if (!checkDeptNameUnique(dept))
        {
            throw new ServiceException("修改买家端部门失败，部门名称已存在");
        }
        dept.setUpdateBy(SecurityUtils.getUsername());
        int rows = deptMapper.updateBuyerDept(dept);
        if (!StringUtils.equals(oldDept.getAncestors(), dept.getAncestors()))
        {
            deptMapper.updateBuyerDeptAncestors(buyerId, oldDept.getAncestors() + "," + oldDept.getDeptId(),
                    dept.getAncestors() + "," + dept.getDeptId(), SecurityUtils.getUsername());
        }
        return rows;
    }

    @Override
    public int deleteDeptById(Long buyerId, Long deptId)
    {
        selectDeptById(buyerId, deptId);
        if (hasChildByDeptId(buyerId, deptId))
        {
            throw new ServiceException("存在子部门，不允许删除");
        }
        if (checkDeptExistAccount(buyerId, deptId))
        {
            throw new ServiceException("部门存在账号，不允许删除");
        }
        return deptMapper.deleteBuyerDeptById(buyerId, deptId, SecurityUtils.getUsername());
    }

    private void setAncestors(Long buyerId, PortalDept dept)
    {
        if (PortalDeptSupport.DEPT_ROOT_ID.equals(dept.getParentId()))
        {
            dept.setAncestors("0");
            return;
        }
        PortalDept parent = selectDeptById(buyerId, dept.getParentId());
        dept.setAncestors(PortalDeptSupport.buildAncestors(parent));
    }
}
