-- 端内账号只读列表权限 seed
-- 目标：
-- 1. 为现有卖家/买家主体补齐默认端内 Owner 角色。
-- 2. 将现有 OWNER 账号绑定到默认端内 Owner 角色。
-- 3. 补齐 /seller/accounts 与 /buyer/accounts 所需端内权限，并授予当前已有启用端内角色。

set names utf8mb4;
set @confirm_portal_account_list_permission_seed := coalesce(@confirm_portal_account_list_permission_seed, '');

delimiter //

drop procedure if exists assert_portal_account_list_permission_seed_confirmed//
create procedure assert_portal_account_list_permission_seed_confirmed()
begin
  if coalesce(@confirm_portal_account_list_permission_seed, '')
      <> 'APPLY_PORTAL_ACCOUNT_LIST_PERMISSION_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_portal_account_list_permission_seed = APPLY_PORTAL_ACCOUNT_LIST_PERMISSION_SEED before running this seed';
  end if;
end//

delimiter ;

call assert_portal_account_list_permission_seed_confirmed();
drop procedure if exists assert_portal_account_list_permission_seed_confirmed;

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
        parent_id <> p_parent_id
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
        parent_id <> p_parent_id
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
end//

delimiter ;

call assert_terminal_menu_range_ready();

insert into seller_role
    (seller_id, role_name, role_key, role_sort, status, del_flag,
     create_by, create_time, update_by, update_time, remark)
select s.seller_id, 'Owner', 'owner', 1, '0', '0',
       'admin', sysdate(), '', null, '默认卖家端 Owner 角色'
from seller s
where s.status = '0'
  and not exists (
      select 1
      from seller_role r
      where r.seller_id = s.seller_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
  );

insert into buyer_role
    (buyer_id, role_name, role_key, role_sort, status, del_flag,
     create_by, create_time, update_by, update_time, remark)
select b.buyer_id, 'Owner', 'owner', 1, '0', '0',
       'admin', sysdate(), '', null, '默认买家端 Owner 角色'
from buyer b
where b.status = '0'
  and not exists (
      select 1
      from buyer_role r
      where r.buyer_id = b.buyer_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
  );

call assert_seller_menu_permission_slot('seller:account:list', 0, 'F', '', null, '',
    'seller:account:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:list', 0, 'F', '', null, '',
    'buyer:account:list menu slot is occupied by another signature');

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '账号列表', 0, 30, '', null, '', '',
    1, 0, 'F', '0', '0', 'seller:account:list', '#', 'admin',
    sysdate(), '', null, '卖家端账号只读列表权限'
where not exists (
    select 1 from seller_menu where perms = 'seller:account:list'
);

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '账号列表', 0, 30, '', null, '', '',
    1, 0, 'F', '0', '0', 'buyer:account:list', '#', 'admin',
    sysdate(), '', null, '买家端账号只读列表权限'
where not exists (
    select 1 from buyer_menu where perms = 'buyer:account:list'
);

insert into seller_account_role (seller_account_id, seller_role_id)
select a.seller_account_id, r.seller_role_id
from seller_account a
join seller_role r on r.seller_id = a.seller_id
                  and r.role_key = 'owner'
                  and r.del_flag = '0'
where a.account_role = 'OWNER'
  and not exists (
      select 1
      from seller_account_role ar
      where ar.seller_account_id = a.seller_account_id
        and ar.seller_role_id = r.seller_role_id
  );

insert into buyer_account_role (buyer_account_id, buyer_role_id)
select a.buyer_account_id, r.buyer_role_id
from buyer_account a
join buyer_role r on r.buyer_id = a.buyer_id
                 and r.role_key = 'owner'
                 and r.del_flag = '0'
where a.account_role = 'OWNER'
  and not exists (
      select 1
      from buyer_account_role ar
      where ar.buyer_account_id = a.buyer_account_id
        and ar.buyer_role_id = r.buyer_role_id
  );

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms = 'seller:account:list'
                  and m.parent_id = 0
                  and coalesce(m.menu_type, '') = 'F'
                  and coalesce(m.path, '') = ''
                  and coalesce(m.component, '') = ''
                  and coalesce(m.route_name, '') = ''
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
join buyer_menu m on m.perms = 'buyer:account:list'
                 and m.parent_id = 0
                 and coalesce(m.menu_type, '') = 'F'
                 and coalesce(m.path, '') = ''
                 and coalesce(m.component, '') = ''
                 and coalesce(m.route_name, '') = ''
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );

drop procedure if exists assert_seller_menu_permission_slot;
drop procedure if exists assert_buyer_menu_permission_slot;
drop procedure if exists assert_terminal_menu_range_ready;
