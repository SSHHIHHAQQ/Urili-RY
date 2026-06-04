package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Tree select node for seller/buyer terminal trees.
 */
public class PortalTreeSelect implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private String label;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<PortalTreeSelect> children;

    public PortalTreeSelect()
    {
    }

    public PortalTreeSelect(PortalMenu menu)
    {
        this.id = menu.getMenuId();
        this.label = menu.getMenuName();
        this.children = menu.getChildren().stream().map(PortalTreeSelect::new).collect(Collectors.toList());
    }

    public PortalTreeSelect(PortalDept dept)
    {
        this.id = dept.getDeptId();
        this.label = dept.getDeptName();
        this.children = dept.getChildren().stream().map(PortalTreeSelect::new).collect(Collectors.toList());
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public List<PortalTreeSelect> getChildren()
    {
        return children;
    }

    public void setChildren(List<PortalTreeSelect> children)
    {
        this.children = children;
    }
}
