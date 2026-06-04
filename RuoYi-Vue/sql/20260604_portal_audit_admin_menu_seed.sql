-- Admin-side permission seed for seller/buyer terminal audit read-only lists.
-- Scope: only sys_menu permission buttons used by the admin console.

set names utf8mb4;

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2250, '卖家登录日志列表', 2011, 110, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:loginLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2251, '卖家操作日志列表', 2011, 115, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:operLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2252, '卖家免密票据列表', 2011, 120, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:ticket:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2253, '买家登录日志列表', 2012, 110, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:loginLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2254, '买家操作日志列表', 2012, 115, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:operLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2255, '买家免密票据列表', 2012, 120, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:ticket:list', '#', 'admin',
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
