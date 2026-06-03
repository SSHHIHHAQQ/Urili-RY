-- Top-level menu seed for the RuoYi validation project.
-- Scope: top-level sys_menu directories only. No page, API, button permission, or business table is created here.

set names utf8mb4;

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2010, '主体管理', 0, 1, 'partner', null, '', 'PartnerManagement',
     1, 0, 'M', '0', '0', '', 'TeamOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：主体管理'),
    (2060, '商品管理', 0, 2, 'product', null, '', 'ProductManagement',
     1, 0, 'M', '0', '0', '', 'ShoppingOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：商品管理'),
    (2070, '订单管理', 0, 3, 'order', null, '', 'OrderManagement',
     1, 0, 'M', '0', '0', '', 'OrderedListOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：订单管理'),
    (2080, '库存管理', 0, 4, 'inventory', null, '', 'InventoryManagement',
     1, 0, 'M', '0', '0', '', 'StockOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：库存管理'),
    (2020, '仓库管理', 0, 5, 'warehouse', null, '', 'WarehouseManagement',
     1, 0, 'M', '0', '0', '', 'HomeOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：仓库管理，复用原仓储管理菜单位'),
    (2030, '海外仓服务设置', 0, 6, 'overseas-warehouse-service', null, '', 'OverseasWarehouseServiceManagement',
     1, 0, 'M', '0', '0', '', 'GlobalOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：海外仓服务设置，复用原上游系统菜单位'),
    (2050, '财务管理', 0, 7, 'finance', null, '', 'FinanceManagement',
     1, 0, 'M', '0', '0', '', 'AccountBookOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：财务管理，复用原计费管理菜单位'),
    (108, '日志中心', 0, 8, 'log-center', null, '', 'LogCenter',
     1, 0, 'M', '0', '0', '', 'FileTextOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：日志中心，复用若依原日志管理目录'),
    (3, '工具中心', 0, 9, 'tool', null, '', '',
     1, 0, 'M', '0', '0', '', 'ToolOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：工具中心，复用若依原系统工具目录')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query = values(query),
    route_name = values(route_name),
    is_frame = values(is_frame),
    is_cache = values(is_cache),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    update_by = 'admin',
    update_time = sysdate(),
    remark = values(remark);

-- Keep RuoYi native admin/monitor entries after the requested top-level menu order.
update sys_menu
set order_num = 90,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 1;

update sys_menu
set order_num = 91,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2;

-- The previous channel draft is not part of the requested top-level menu list.
-- Keep it for reference, but disable it so it does not appear as an active top-level menu.
update sys_menu
set order_num = 80,
    visible = '1',
    status = '1',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '本轮顶级菜单口径未纳入，保留历史草案'
where menu_id = 2040;

-- If an earlier seed created the old wrapper root, keep it inactive.
update sys_menu
set visible = '1',
    status = '1',
    order_num = 99,
    update_by = 'admin',
    update_time = sysdate(),
    remark = '已由独立顶级菜单替代，保留历史草案'
where menu_id = 2000;
