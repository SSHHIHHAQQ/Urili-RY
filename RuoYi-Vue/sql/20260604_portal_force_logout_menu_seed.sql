-- Admin-side permission seed for seller/buyer terminal force logout control.
-- Scope: only sys_menu permission buttons used by the admin console.

set names utf8mb4;

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2206, '卖家强制踢出', 2011, 32, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:forceLogout', '#', 'admin',
     sysdate(), '', null, ''),
    (2216, '买家强制踢出', 2012, 32, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:forceLogout', '#', 'admin',
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
