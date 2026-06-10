-- Terminal portal home page menu seed
-- Scope:
-- 1. Add seller/buyer portal home page C menus for terminal getRouters.
-- 2. Grant the page menus to current active seller/buyer Owner roles.
-- 3. Keep terminal menu ownership independent from sys_menu/sys_role/sys_user.

set names utf8mb4;
set @confirm_terminal_portal_home_menu_seed := coalesce(@confirm_terminal_portal_home_menu_seed, '');

delimiter //

drop procedure if exists assert_terminal_portal_home_menu_seed_confirmed//
create procedure assert_terminal_portal_home_menu_seed_confirmed()
begin
  if coalesce(@confirm_terminal_portal_home_menu_seed, '')
      <> 'APPLY_TERMINAL_PORTAL_HOME_MENU_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_terminal_portal_home_menu_seed = APPLY_TERMINAL_PORTAL_HOME_MENU_SEED before running this seed';
  end if;
end//

delimiter ;

call assert_terminal_portal_home_menu_seed_confirmed();
drop procedure if exists assert_terminal_portal_home_menu_seed_confirmed;

delimiter //

drop procedure if exists assert_seller_menu_permission_slot//
create procedure assert_seller_menu_permission_slot(
  in p_perms varchar(100),
  in p_parent_id bigint,
  in p_menu_type char(1),
  in p_path varchar(200),
  in p_component varchar(255),
  in p_route_name varchar(50),
  in p_message varchar(128)
)
begin
  if exists (
    select 1
    from seller_menu
    where perms = p_perms
      and (
        coalesce(parent_id, -1) <> p_parent_id
        or coalesce(menu_type, '') <> coalesce(p_menu_type, '')
        or coalesce(path, '') <> coalesce(p_path, '')
        or coalesce(component, '') <> coalesce(p_component, '')
        or coalesce(route_name, '') <> coalesce(p_route_name, '')
      )
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_buyer_menu_permission_slot//
create procedure assert_buyer_menu_permission_slot(
  in p_perms varchar(100),
  in p_parent_id bigint,
  in p_menu_type char(1),
  in p_path varchar(200),
  in p_component varchar(255),
  in p_route_name varchar(50),
  in p_message varchar(128)
)
begin
  if exists (
    select 1
    from buyer_menu
    where perms = p_perms
      and (
        coalesce(parent_id, -1) <> p_parent_id
        or coalesce(menu_type, '') <> coalesce(p_menu_type, '')
        or coalesce(path, '') <> coalesce(p_path, '')
        or coalesce(component, '') <> coalesce(p_component, '')
        or coalesce(route_name, '') <> coalesce(p_route_name, '')
      )
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_terminal_menu_range_ready//
create procedure assert_terminal_menu_range_ready()
begin
  declare v_table_count int default 0;
  declare v_auto_increment bigint default 0;

  select count(1)
    into v_table_count
  from information_schema.tables
  where table_schema = database()
    and table_name = 'seller_menu';

  if v_table_count = 0 then
    signal sqlstate '45000' set message_text = 'seller_menu table is required before terminal menu seed inserts';
  end if;

  if exists (
    select 1
    from seller_menu
    where seller_menu_id > 0
      and (seller_menu_id < 100000 or seller_menu_id >= 200000)
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu contains IDs outside seller range 100000-199999';
  end if;

  select auto_increment
    into v_auto_increment
  from information_schema.tables
  where table_schema = database()
    and table_name = 'seller_menu';

  if coalesce(v_auto_increment, 0) < 100000
      or coalesce(v_auto_increment, 0) >= 200000 then
    signal sqlstate '45000' set message_text = 'seller_menu auto_increment must be between 100000 and 199999 before terminal menu seed inserts';
  end if;

  if exists (
    select 1
    from seller_menu
    where coalesce(perms, '') = ''
       or coalesce(perms, '') = '*'
       or coalesce(perms, '') not like 'seller:%'
       or coalesce(perms, '') like 'seller:admin:%'
       or coalesce(perms, '') like 'buyer:%'
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu contains invalid terminal perms';
  end if;

  if exists (
    select 1
    from seller_menu
    where menu_type = 'C'
      and (
        coalesce(trim(component), '') = ''
        or coalesce(trim(component), '') not like 'Seller/%'
      )
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu page menus require component under Seller/';
  end if;

  if exists (
    select 1
    from (
      select perms
      from seller_menu
      group by perms
      having count(1) > 1
    ) duplicate_seller_menu_perms
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu perms must be unique before terminal role grants';
  end if;

  select count(1)
    into v_table_count
  from information_schema.tables
  where table_schema = database()
    and table_name = 'buyer_menu';

  if v_table_count = 0 then
    signal sqlstate '45000' set message_text = 'buyer_menu table is required before terminal menu seed inserts';
  end if;

  if exists (
    select 1
    from buyer_menu
    where buyer_menu_id > 0
      and (buyer_menu_id < 200000 or buyer_menu_id >= 300000)
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu contains IDs outside buyer range 200000-299999';
  end if;

  select auto_increment
    into v_auto_increment
  from information_schema.tables
  where table_schema = database()
    and table_name = 'buyer_menu';

  if coalesce(v_auto_increment, 0) < 200000
      or coalesce(v_auto_increment, 0) >= 300000 then
    signal sqlstate '45000' set message_text = 'buyer_menu auto_increment must be between 200000 and 299999 before terminal menu seed inserts';
  end if;

  if exists (
    select 1
    from buyer_menu
    where coalesce(perms, '') = ''
       or coalesce(perms, '') = '*'
       or coalesce(perms, '') not like 'buyer:%'
       or coalesce(perms, '') like 'buyer:admin:%'
       or coalesce(perms, '') like 'seller:%'
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu contains invalid terminal perms';
  end if;

  if exists (
    select 1
    from buyer_menu
    where menu_type = 'C'
      and (
        coalesce(trim(component), '') = ''
        or coalesce(trim(component), '') not like 'Buyer/%'
      )
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu page menus require component under Buyer/';
  end if;

  if exists (
    select 1
    from (
      select perms
      from buyer_menu
      group by perms
      having count(1) > 1
    ) duplicate_buyer_menu_perms
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu perms must be unique before terminal role grants';
  end if;
end//

drop procedure if exists assert_terminal_portal_home_menu_seed_completed//
create procedure assert_terminal_portal_home_menu_seed_completed()
begin
  declare v_owner_role_count int default 0;
  declare v_grant_count int default 0;

  if not exists (
    select 1
    from seller_menu
    where perms = 'seller:portal:home'
      and parent_id = 0
      and menu_type = 'C'
      and path = '/seller/portal'
      and component = 'Seller/Portal/index'
      and route_name = 'SellerPortalHome'
  ) then
    signal sqlstate '45000' set message_text = 'seller portal home page menu was not created with expected signature';
  end if;

  if exists (
    select 1
    from seller_role r
    where r.del_flag = '0'
      and r.status = '0'
      and r.role_key = 'owner'
      and not exists (
        select 1
        from seller_role_menu rm
        join seller_menu m on m.seller_menu_id = rm.seller_menu_id
        where rm.seller_role_id = r.seller_role_id
          and m.perms = 'seller:portal:home'
          and m.parent_id = 0
          and coalesce(m.menu_type, '') = 'C'
          and coalesce(m.path, '') = '/seller/portal'
          and coalesce(m.component, '') = 'Seller/Portal/index'
          and coalesce(m.route_name, '') = 'SellerPortalHome'
      )
  ) then
    signal sqlstate '45000' set message_text = 'seller owner roles must have portal home page menu';
  end if;

  select count(1)
    into v_owner_role_count
  from seller_role r
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner';

  select count(1)
    into v_grant_count
  from seller_role r
  join seller_role_menu rm on rm.seller_role_id = r.seller_role_id
  join seller_menu m on m.seller_menu_id = rm.seller_menu_id
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner'
    and m.perms = 'seller:portal:home'
    and m.parent_id = 0
    and coalesce(m.menu_type, '') = 'C'
    and coalesce(m.path, '') = '/seller/portal'
    and coalesce(m.component, '') = 'Seller/Portal/index'
    and coalesce(m.route_name, '') = 'SellerPortalHome';

  if v_grant_count <> v_owner_role_count then
    signal sqlstate '45000' set message_text = 'seller owner roles portal home page menu exact grant count mismatch';
  end if;

  if not exists (
    select 1
    from buyer_menu
    where perms = 'buyer:portal:home'
      and parent_id = 0
      and menu_type = 'C'
      and path = '/buyer/portal'
      and component = 'Buyer/Portal/index'
      and route_name = 'BuyerPortalHome'
  ) then
    signal sqlstate '45000' set message_text = 'buyer portal home page menu was not created with expected signature';
  end if;

  if exists (
    select 1
    from buyer_role r
    where r.del_flag = '0'
      and r.status = '0'
      and r.role_key = 'owner'
      and not exists (
        select 1
        from buyer_role_menu rm
        join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
        where rm.buyer_role_id = r.buyer_role_id
          and m.perms = 'buyer:portal:home'
          and m.parent_id = 0
          and coalesce(m.menu_type, '') = 'C'
          and coalesce(m.path, '') = '/buyer/portal'
          and coalesce(m.component, '') = 'Buyer/Portal/index'
          and coalesce(m.route_name, '') = 'BuyerPortalHome'
      )
  ) then
    signal sqlstate '45000' set message_text = 'buyer owner roles must have portal home page menu';
  end if;

  select count(1)
    into v_owner_role_count
  from buyer_role r
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner';

  select count(1)
    into v_grant_count
  from buyer_role r
  join buyer_role_menu rm on rm.buyer_role_id = r.buyer_role_id
  join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner'
    and m.perms = 'buyer:portal:home'
    and m.parent_id = 0
    and coalesce(m.menu_type, '') = 'C'
    and coalesce(m.path, '') = '/buyer/portal'
    and coalesce(m.component, '') = 'Buyer/Portal/index'
    and coalesce(m.route_name, '') = 'BuyerPortalHome';

  if v_grant_count <> v_owner_role_count then
    signal sqlstate '45000' set message_text = 'buyer owner roles portal home page menu exact grant count mismatch';
  end if;
end//

delimiter ;

call assert_terminal_menu_range_ready();

call assert_seller_menu_permission_slot('seller:portal:home', 0, 'C', '/seller/portal', 'Seller/Portal/index', 'SellerPortalHome',
    'seller:portal:home menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:portal:home', 0, 'C', '/buyer/portal', 'Buyer/Portal/index', 'BuyerPortalHome',
    'buyer:portal:home menu slot is occupied by another signature');

start transaction;

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '卖家端首页', 0, 1, '/seller/portal', 'Seller/Portal/index', '', 'SellerPortalHome',
    1, 0, 'C', '0', '0', 'seller:portal:home', 'home', 'admin',
    sysdate(), '', null, '卖家端 Portal 首页菜单'
where not exists (
    select 1 from seller_menu where perms = 'seller:portal:home'
);

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '买家端首页', 0, 1, '/buyer/portal', 'Buyer/Portal/index', '', 'BuyerPortalHome',
    1, 0, 'C', '0', '0', 'buyer:portal:home', 'home', 'admin',
    sysdate(), '', null, '买家端 Portal 首页菜单'
where not exists (
    select 1 from buyer_menu where perms = 'buyer:portal:home'
);

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms = 'seller:portal:home'
                  and m.parent_id = 0
                  and coalesce(m.menu_type, '') = 'C'
                  and coalesce(m.path, '') = '/seller/portal'
                  and coalesce(m.component, '') = 'Seller/Portal/index'
                  and coalesce(m.route_name, '') = 'SellerPortalHome'
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and not exists (
      select 1
      from seller_role_menu rm
      where rm.seller_role_id = r.seller_role_id
        and rm.seller_menu_id = m.seller_menu_id
  );

insert into buyer_role_menu (buyer_role_id, buyer_menu_id)
select r.buyer_role_id, m.buyer_menu_id
from buyer_role r
join buyer_menu m on m.perms = 'buyer:portal:home'
                 and m.parent_id = 0
                 and coalesce(m.menu_type, '') = 'C'
                 and coalesce(m.path, '') = '/buyer/portal'
                 and coalesce(m.component, '') = 'Buyer/Portal/index'
                 and coalesce(m.route_name, '') = 'BuyerPortalHome'
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );

call assert_terminal_portal_home_menu_seed_completed();
commit;

drop procedure if exists assert_seller_menu_permission_slot;
drop procedure if exists assert_buyer_menu_permission_slot;
drop procedure if exists assert_terminal_menu_range_ready;
drop procedure if exists assert_terminal_portal_home_menu_seed_completed;
