-- Admin-side base page and direct-login permission seed for seller/buyer management.
-- Scope: standalone incremental seed only. It closes the gap when environments replay
-- split 2026-06-04/05 permission seeds without replaying seller_buyer_management_seed.sql.

set names utf8mb4;

set @confirm_admin_partner_page_direct_login_seed := coalesce(@confirm_admin_partner_page_direct_login_seed, '');

delimiter //

drop procedure if exists assert_admin_partner_page_direct_login_seed_confirmed//
create procedure assert_admin_partner_page_direct_login_seed_confirmed()
begin
  if coalesce(@confirm_admin_partner_page_direct_login_seed, '')
      <> 'APPLY_ADMIN_PARTNER_PAGE_DIRECT_LOGIN_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_admin_partner_page_direct_login_seed = APPLY_ADMIN_PARTNER_PAGE_DIRECT_LOGIN_SEED before running this seed';
  end if;
end//

delimiter ;

call assert_admin_partner_page_direct_login_seed_confirmed();
drop procedure if exists assert_admin_partner_page_direct_login_seed_confirmed;

delimiter //

drop procedure if exists assert_sys_menu_slot//
create procedure assert_sys_menu_slot(
  in p_menu_id bigint,
  in p_parent_id bigint,
  in p_menu_type char(1),
  in p_path varchar(200),
  in p_component varchar(255),
  in p_route_name varchar(50),
  in p_perms varchar(100),
  in p_message varchar(128)
)
begin
  if exists (
    select 1
    from sys_menu
    where menu_id = p_menu_id
      and (
        coalesce(parent_id, -1) <> p_parent_id
        or coalesce(menu_type, '') <> coalesce(p_menu_type, '')
        or coalesce(path, '') <> coalesce(p_path, '')
        or coalesce(component, '') <> coalesce(p_component, '')
        or coalesce(route_name, '') <> coalesce(p_route_name, '')
        or coalesce(perms, '') <> coalesce(p_perms, '')
      )
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_partner_root_menu_exists//
create procedure assert_partner_root_menu_exists()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2010
      and coalesce(parent_id, -1) = 0
      and coalesce(menu_type, '') = 'M'
      and coalesce(path, '') = 'partner'
      and coalesce(component, '') = ''
      and coalesce(route_name, '') = 'PartnerManagement'
      and coalesce(perms, '') = ''
  ) then
    signal sqlstate '45000' set message_text = 'admin direct-login seed requires top_menu_seed partner root 2010';
  end if;
end//

drop procedure if exists assert_sys_menu_signature_available//
create procedure assert_sys_menu_signature_available(
  in p_menu_id bigint,
  in p_path varchar(200),
  in p_component varchar(255),
  in p_route_name varchar(50),
  in p_perms varchar(100),
  in p_message varchar(128)
)
begin
  if exists (
    select 1
    from sys_menu
    where menu_id <> p_menu_id
      and coalesce(path, '') = coalesce(p_path, '')
      and coalesce(component, '') = coalesce(p_component, '')
      and coalesce(route_name, '') = coalesce(p_route_name, '')
      and coalesce(perms, '') = coalesce(p_perms, '')
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_admin_partner_page_direct_login_seed_completed//
create procedure assert_admin_partner_page_direct_login_seed_completed()
begin
  if (
    select count(1)
    from sys_menu
    where (
        menu_id = 2011
        and coalesce(menu_name, '') = '卖家管理'
        and coalesce(parent_id, -1) = 2010
        and coalesce(order_num, -1) = 5
        and coalesce(path, '') = 'seller'
        and coalesce(component, '') = 'Seller/index'
        and coalesce(route_name, '') = 'Seller'
        and coalesce(menu_type, '') = 'C'
        and coalesce(visible, '') = '0'
        and coalesce(status, '') = '0'
        and coalesce(perms, '') = 'seller:admin:list'
      )
      or (
        menu_id = 2012
        and coalesce(menu_name, '') = '买家管理'
        and coalesce(parent_id, -1) = 2010
        and coalesce(order_num, -1) = 10
        and coalesce(path, '') = 'buyer'
        and coalesce(component, '') = 'Buyer/index'
        and coalesce(route_name, '') = 'Buyer'
        and coalesce(menu_type, '') = 'C'
        and coalesce(visible, '') = '0'
        and coalesce(status, '') = '0'
        and coalesce(perms, '') = 'buyer:admin:list'
      )
      or (
        menu_id = 2205
        and coalesce(menu_name, '') = '卖家免密登录'
        and coalesce(parent_id, -1) = 2011
        and coalesce(order_num, -1) = 30
        and coalesce(path, '') = '#'
        and coalesce(component, '') = ''
        and coalesce(route_name, '') = ''
        and coalesce(menu_type, '') = 'F'
        and coalesce(visible, '') = '0'
        and coalesce(status, '') = '0'
        and coalesce(perms, '') = 'seller:admin:directLogin'
      )
      or (
        menu_id = 2215
        and coalesce(menu_name, '') = '买家免密登录'
        and coalesce(parent_id, -1) = 2012
        and coalesce(order_num, -1) = 30
        and coalesce(path, '') = '#'
        and coalesce(component, '') = ''
        and coalesce(route_name, '') = ''
        and coalesce(menu_type, '') = 'F'
        and coalesce(visible, '') = '0'
        and coalesce(status, '') = '0'
        and coalesce(perms, '') = 'buyer:admin:directLogin'
      )
  ) <> 4 then
    signal sqlstate '45000' set message_text = 'admin partner page direct-login seed completion mismatch';
  end if;
end//

delimiter ;

call assert_partner_root_menu_exists();

call assert_sys_menu_slot(2011, 2010, 'C', 'seller', 'Seller/index', 'Seller', 'seller:admin:list', 'sys_menu 2011 is occupied by another menu');
call assert_sys_menu_slot(2012, 2010, 'C', 'buyer', 'Buyer/index', 'Buyer', 'buyer:admin:list', 'sys_menu 2012 is occupied by another menu');
call assert_sys_menu_slot(2205, 2011, 'F', '#', '', '', 'seller:admin:directLogin', 'sys_menu 2205 is occupied by another menu');
call assert_sys_menu_slot(2215, 2012, 'F', '#', '', '', 'buyer:admin:directLogin', 'sys_menu 2215 is occupied by another menu');

call assert_sys_menu_signature_available(2011, 'seller', 'Seller/index', 'Seller', 'seller:admin:list', 'seller admin menu signature is already used by another menu');
call assert_sys_menu_signature_available(2012, 'buyer', 'Buyer/index', 'Buyer', 'buyer:admin:list', 'buyer admin menu signature is already used by another menu');
call assert_sys_menu_signature_available(2205, '#', '', '', 'seller:admin:directLogin', 'seller direct-login menu signature is already used by another menu');
call assert_sys_menu_signature_available(2215, '#', '', '', 'buyer:admin:directLogin', 'buyer direct-login menu signature is already used by another menu');

start transaction;

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2011, '卖家管理', 2010, 5, 'seller', 'Seller/index', '', 'Seller',
     1, 0, 'C', '0', '0', 'seller:admin:list', 'ShopOutlined', 'admin',
     sysdate(), '', null, '管理端卖家管理'),
    (2012, '买家管理', 2010, 10, 'buyer', 'Buyer/index', '', 'Buyer',
     1, 0, 'C', '0', '0', 'buyer:admin:list', 'UserOutlined', 'admin',
     sysdate(), '', null, '管理端买家管理'),
    (2205, '卖家免密登录', 2011, 30, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:directLogin', '#', 'admin',
     sysdate(), '', null, ''),
    (2215, '买家免密登录', 2012, 30, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:directLogin', '#', 'admin',
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

call assert_admin_partner_page_direct_login_seed_completed();

commit;

drop procedure if exists assert_sys_menu_slot;
drop procedure if exists assert_partner_root_menu_exists;
drop procedure if exists assert_sys_menu_signature_available;
drop procedure if exists assert_admin_partner_page_direct_login_seed_completed;
