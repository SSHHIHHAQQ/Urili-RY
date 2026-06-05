-- 管理端卖家账号维护细权限 seed
-- 只补 sys_menu 中的卖家账号域权限，不执行表结构变更。

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2310, '卖家账号列表', 2011, 125, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2311, '卖家账号新增', 2011, 130, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2312, '卖家账号修改', 2011, 135, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2313, '卖家账号重置密码', 2011, 140, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:resetPwd', '#', 'admin',
     sysdate(), '', null, ''),
    (2314, '卖家账号角色查询', 2011, 145, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:role:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2315, '卖家账号角色分配', 2011, 150, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:role:edit', '#', 'admin',
     sysdate(), '', null, '')
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
