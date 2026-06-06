-- Order after-sale menu seed.
-- Scope: add a second-level placeholder menu under "订单管理".
-- No order/after-sale business table, API, button permission, or real page is created here.

set names utf8mb4;

set @confirm_order_after_sale_menu_seed := coalesce(@confirm_order_after_sale_menu_seed, '');

delimiter //

drop procedure if exists assert_order_after_sale_menu_seed_confirmed//
create procedure assert_order_after_sale_menu_seed_confirmed()
begin
  if coalesce(@confirm_order_after_sale_menu_seed, '')
      <> 'APPLY_ORDER_AFTER_SALE_MENU_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_order_after_sale_menu_seed = APPLY_ORDER_AFTER_SALE_MENU_SEED before running this migration';
  end if;
end//

delimiter ;

call assert_order_after_sale_menu_seed_confirmed();
drop procedure if exists assert_order_after_sale_menu_seed_confirmed;

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2412, '售后管理', 2070, 15, 'after-sale', 'Common/PlannedPage/index', '', 'AfterSaleManagement',
     1, 0, 'C', '0', '0', 'order:afterSale:list', 'IssuesCloseOutlined', 'admin',
     sysdate(), '', null, '订单管理菜单：售后管理，占位入口')
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
