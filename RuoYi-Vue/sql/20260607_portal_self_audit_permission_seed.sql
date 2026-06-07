-- 端内本人审计自助权限 seed
-- 目标：
-- 1. 补齐 /seller/account/login-logs、/seller/account/oper-logs、/seller/account/sessions 的端内权限。
-- 2. 补齐 /buyer/account/login-logs、/buyer/account/oper-logs、/buyer/account/sessions 的端内权限。
-- 3. 将权限授予当前启用的端内 OWNER 角色，保持卖家/买家端内权限表独立于 sys_*。

set names utf8mb4;
set @confirm_portal_self_audit_permission_seed := coalesce(@confirm_portal_self_audit_permission_seed, '');

delimiter //

drop procedure if exists assert_portal_self_audit_permission_seed_confirmed//
create procedure assert_portal_self_audit_permission_seed_confirmed()
begin
  if coalesce(@confirm_portal_self_audit_permission_seed, '')
      <> 'APPLY_PORTAL_SELF_AUDIT_PERMISSION_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_portal_self_audit_permission_seed = APPLY_PORTAL_SELF_AUDIT_PERMISSION_SEED before running this seed';
  end if;
end//

delimiter ;

call assert_portal_self_audit_permission_seed_confirmed();
drop procedure if exists assert_portal_self_audit_permission_seed_confirmed;

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

  if coalesce(v_auto_increment, 0) < 100000 then
    signal sqlstate '45000' set message_text = 'seller_menu auto_increment must be >= 100000 before terminal menu seed inserts';
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

  if coalesce(v_auto_increment, 0) < 200000 then
    signal sqlstate '45000' set message_text = 'buyer_menu auto_increment must be >= 200000 before terminal menu seed inserts';
  end if;
end//

delimiter ;

call assert_terminal_menu_range_ready();

call assert_seller_menu_permission_slot('seller:account:loginLog:list', 0, 'F', '', null, '',
    'seller:account:loginLog:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:operLog:list', 0, 'F', '', null, '',
    'seller:account:operLog:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:session:list', 0, 'F', '', null, '',
    'seller:account:session:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:loginLog:list', 0, 'F', '', null, '',
    'buyer:account:loginLog:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:operLog:list', 0, 'F', '', null, '',
    'buyer:account:operLog:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:session:list', 0, 'F', '', null, '',
    'buyer:account:session:list menu slot is occupied by another signature');

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_name, 0, seed.order_num, '', null, '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select '登录日志' as menu_name, 31 as order_num, 'seller:account:loginLog:list' as perms, '卖家端本人登录日志只读权限' as remark
    union all select '操作日志', 32, 'seller:account:operLog:list', '卖家端本人操作日志只读权限'
    union all select '会话列表', 33, 'seller:account:session:list', '卖家端本人会话只读权限'
) seed
where not exists (
    select 1 from seller_menu m where m.perms = seed.perms
);

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_name, 0, seed.order_num, '', null, '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select '登录日志' as menu_name, 31 as order_num, 'buyer:account:loginLog:list' as perms, '买家端本人登录日志只读权限' as remark
    union all select '操作日志', 32, 'buyer:account:operLog:list', '买家端本人操作日志只读权限'
    union all select '会话列表', 33, 'buyer:account:session:list', '买家端本人会话只读权限'
) seed
where not exists (
    select 1 from buyer_menu m where m.perms = seed.perms
);

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms in (
    'seller:account:loginLog:list',
    'seller:account:operLog:list',
    'seller:account:session:list'
)
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
join buyer_menu m on m.perms in (
    'buyer:account:loginLog:list',
    'buyer:account:operLog:list',
    'buyer:account:session:list'
)
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
