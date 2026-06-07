package com.ruoyi.system.service.impl;

import static org.junit.Assert.assertEquals;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.system.domain.vo.RouterVo;

public class SysMenuServiceImplTest
{
    @Test
    public void buildMenusCopiesMenuPermsToNestedRoutePayload()
    {
        SysMenu dir = menu(2000L, 0L, "库存管理", "inventory", UserConstants.TYPE_DIR, UserConstants.LAYOUT, null);
        SysMenu page = menu(2421L, 2000L, "来源仓库库存", "source-warehouse-stock",
                UserConstants.TYPE_MENU, "Inventory/SourceWarehouseStock/index", "inventory:sourceWarehouse:list");
        dir.setChildren(Collections.singletonList(page));

        List<RouterVo> routers = new SysMenuServiceImpl().buildMenus(Collections.singletonList(dir));

        RouterVo child = routers.get(0).getChildren().get(0);
        assertEquals("inventory:sourceWarehouse:list", child.getPerms());
    }

    private SysMenu menu(Long menuId, Long parentId, String menuName, String path, String menuType, String component,
            String perms)
    {
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setMenuName(menuName);
        menu.setPath(path);
        menu.setRouteName(path);
        menu.setMenuType(menuType);
        menu.setComponent(component);
        menu.setPerms(perms);
        menu.setVisible("0");
        menu.setStatus("0");
        menu.setIsCache("0");
        menu.setIsFrame(UserConstants.NO_FRAME);
        menu.setIcon("");
        return menu;
    }
}
