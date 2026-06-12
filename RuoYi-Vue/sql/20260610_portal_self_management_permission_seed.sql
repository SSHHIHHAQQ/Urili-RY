-- Portal self-management permission seed
-- Scope:
-- 1. Add seller/buyer portal self-management F permissions for accounts, roles and departments.
-- 2. Grant the permissions to current active seller/buyer OWNER roles.
-- 3. Keep terminal permission templates independent from sys_* and do not grant business menus.

set names utf8mb4;
set @confirm_portal_self_management_permission_seed := coalesce(@confirm_portal_self_management_permission_seed, '');

delimiter //

drop procedure if exists assert_portal_self_management_permission_seed_confirmed//
create procedure assert_portal_self_management_permission_seed_confirmed()
begin
  if coalesce(@confirm_portal_self_management_permission_seed, '')
      <> 'APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_portal_self_management_permission_seed = APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED before running this seed';
  end if;
end//

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
    from seller_menu
    where coalesce(perms, '') = ''
       or coalesce(perms, '') like '%*%'
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

  if exists (
    select 1
    from buyer_menu
    where coalesce(perms, '') = ''
       or coalesce(perms, '') like '%*%'
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

drop procedure if exists assert_portal_self_management_permission_seed_completed//
create procedure assert_portal_self_management_permission_seed_completed()
begin
  declare v_owner_role_count int default 0;
  declare v_permission_count int default 0;

  select count(1)
    into v_permission_count
  from seller_menu
  where perms = 'seller:portal:home'
    and parent_id = 0
    and menu_type = 'C'
    and coalesce(path, '') = '/seller/portal'
    and coalesce(component, '') = 'Seller/Portal/index'
    and coalesce(route_name, '') = 'SellerPortalHome';

  if v_permission_count <> 1 then
    signal sqlstate '45000' set message_text = 'seller portal home menu signature mismatch';
  end if;

  select count(1)
    into v_permission_count
  from buyer_menu
  where perms = 'buyer:portal:home'
    and parent_id = 0
    and menu_type = 'C'
    and coalesce(path, '') = '/buyer/portal'
    and coalesce(component, '') = 'Buyer/Portal/index'
    and coalesce(route_name, '') = 'BuyerPortalHome';

  if v_permission_count <> 1 then
    signal sqlstate '45000' set message_text = 'buyer portal home menu signature mismatch';
  end if;

  select count(1)
    into v_permission_count
  from seller_menu
  where perms in (
      'seller:account:add',
      'seller:account:edit',
      'seller:account:role:query',
      'seller:account:role:edit',
      'seller:dept:query',
      'seller:dept:add',
      'seller:dept:edit',
      'seller:dept:remove',
      'seller:role:query',
      'seller:role:add',
      'seller:role:edit',
      'seller:role:remove'
    )
    and parent_id = 0
    and menu_type = 'F'
    and coalesce(path, '') = ''
    and coalesce(component, '') = ''
    and coalesce(route_name, '') = '';

  if v_permission_count <> 12 then
    signal sqlstate '45000' set message_text = 'seller self-management permissions were not created';
  end if;

  select count(1)
    into v_permission_count
  from buyer_menu
  where perms in (
      'buyer:account:add',
      'buyer:account:edit',
      'buyer:account:role:query',
      'buyer:account:role:edit',
      'buyer:dept:query',
      'buyer:dept:add',
      'buyer:dept:edit',
      'buyer:dept:remove',
      'buyer:role:query',
      'buyer:role:add',
      'buyer:role:edit',
      'buyer:role:remove'
    )
    and parent_id = 0
    and menu_type = 'F'
    and coalesce(path, '') = ''
    and coalesce(component, '') = ''
    and coalesce(route_name, '') = '';

  if v_permission_count <> 12 then
    signal sqlstate '45000' set message_text = 'buyer self-management permissions were not created';
  end if;

  select count(1)
    into v_permission_count
  from seller_menu
  where perms in (
      'seller:account:list',
      'seller:account:add',
      'seller:account:edit',
      'seller:account:role:query',
      'seller:account:role:edit',
      'seller:account:loginLog:list',
      'seller:account:operLog:list',
      'seller:account:session:list',
      'seller:dept:list',
      'seller:dept:query',
      'seller:dept:add',
      'seller:dept:edit',
      'seller:dept:remove',
      'seller:role:list',
      'seller:role:query',
      'seller:role:add',
      'seller:role:edit',
      'seller:role:remove'
    )
    and parent_id = 0
    and menu_type = 'F'
    and coalesce(path, '') = ''
    and coalesce(component, '') = ''
    and coalesce(route_name, '') = '';

  if v_permission_count <> 18 then
    signal sqlstate '45000' set message_text = 'seller self-management root button menu signature mismatch';
  end if;

  select count(1)
    into v_permission_count
  from buyer_menu
  where perms in (
      'buyer:account:list',
      'buyer:account:add',
      'buyer:account:edit',
      'buyer:account:role:query',
      'buyer:account:role:edit',
      'buyer:account:loginLog:list',
      'buyer:account:operLog:list',
      'buyer:account:session:list',
      'buyer:dept:list',
      'buyer:dept:query',
      'buyer:dept:add',
      'buyer:dept:edit',
      'buyer:dept:remove',
      'buyer:role:list',
      'buyer:role:query',
      'buyer:role:add',
      'buyer:role:edit',
      'buyer:role:remove'
    )
    and parent_id = 0
    and menu_type = 'F'
    and coalesce(path, '') = ''
    and coalesce(component, '') = ''
    and coalesce(route_name, '') = '';

  if v_permission_count <> 18 then
    signal sqlstate '45000' set message_text = 'buyer self-management root button menu signature mismatch';
  end if;

  select count(1)
    into v_owner_role_count
  from seller_role r
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner';

  select count(1)
    into v_permission_count
  from seller_role r
  join seller_role_menu rm on rm.seller_role_id = r.seller_role_id
  join seller_menu m on m.seller_menu_id = rm.seller_menu_id
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner'
    and m.perms in (
      'seller:portal:home',
      'seller:account:list',
      'seller:account:add',
      'seller:account:edit',
      'seller:account:role:query',
      'seller:account:role:edit',
      'seller:account:loginLog:list',
      'seller:account:operLog:list',
      'seller:account:session:list',
      'seller:dept:list',
      'seller:dept:query',
      'seller:dept:add',
      'seller:dept:edit',
      'seller:dept:remove',
      'seller:role:list',
      'seller:role:query',
      'seller:role:add',
      'seller:role:edit',
      'seller:role:remove'
    );

  if v_permission_count <> v_owner_role_count * 19 then
    signal sqlstate '45000' set message_text = 'seller owner roles self-management permission exact grant count mismatch';
  end if;

  if exists (
    select 1
    from seller_role_menu rm
    join seller_role r on r.seller_role_id = rm.seller_role_id
    join seller_menu m on m.seller_menu_id = rm.seller_menu_id
    left join (
      select 'seller:portal:home' as perms
      union all select 'seller:account:list'
      union all select 'seller:account:add'
      union all select 'seller:account:edit'
      union all select 'seller:account:role:query'
      union all select 'seller:account:role:edit'
      union all select 'seller:account:loginLog:list'
      union all select 'seller:account:operLog:list'
      union all select 'seller:account:session:list'
      union all select 'seller:dept:list'
      union all select 'seller:dept:query'
      union all select 'seller:dept:add'
      union all select 'seller:dept:edit'
      union all select 'seller:dept:remove'
      union all select 'seller:role:list'
      union all select 'seller:role:query'
      union all select 'seller:role:add'
      union all select 'seller:role:edit'
      union all select 'seller:role:remove'
    ) expected on expected.perms = m.perms
    where r.del_flag = '0'
      and r.status = '0'
      and r.role_key = 'owner'
      and expected.perms is null
  ) then
    signal sqlstate '45000' set message_text = 'seller owner role menu contains non self-management permission grants';
  end if;

  select count(1)
    into v_owner_role_count
  from buyer_role r
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner';

  select count(1)
    into v_permission_count
  from buyer_role r
  join buyer_role_menu rm on rm.buyer_role_id = r.buyer_role_id
  join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner'
    and m.perms in (
      'buyer:portal:home',
      'buyer:account:list',
      'buyer:account:add',
      'buyer:account:edit',
      'buyer:account:role:query',
      'buyer:account:role:edit',
      'buyer:account:loginLog:list',
      'buyer:account:operLog:list',
      'buyer:account:session:list',
      'buyer:dept:list',
      'buyer:dept:query',
      'buyer:dept:add',
      'buyer:dept:edit',
      'buyer:dept:remove',
      'buyer:role:list',
      'buyer:role:query',
      'buyer:role:add',
      'buyer:role:edit',
      'buyer:role:remove'
    );

  if v_permission_count <> v_owner_role_count * 19 then
    signal sqlstate '45000' set message_text = 'buyer owner roles self-management permission exact grant count mismatch';
  end if;

  if exists (
    select 1
    from buyer_role_menu rm
    join buyer_role r on r.buyer_role_id = rm.buyer_role_id
    join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
    left join (
      select 'buyer:portal:home' as perms
      union all select 'buyer:account:list'
      union all select 'buyer:account:add'
      union all select 'buyer:account:edit'
      union all select 'buyer:account:role:query'
      union all select 'buyer:account:role:edit'
      union all select 'buyer:account:loginLog:list'
      union all select 'buyer:account:operLog:list'
      union all select 'buyer:account:session:list'
      union all select 'buyer:dept:list'
      union all select 'buyer:dept:query'
      union all select 'buyer:dept:add'
      union all select 'buyer:dept:edit'
      union all select 'buyer:dept:remove'
      union all select 'buyer:role:list'
      union all select 'buyer:role:query'
      union all select 'buyer:role:add'
      union all select 'buyer:role:edit'
      union all select 'buyer:role:remove'
      union all select 'buyer:product:center:list'
      union all select 'buyer:product:center:query'
    ) expected on expected.perms = m.perms
    where r.del_flag = '0'
      and r.status = '0'
      and r.role_key = 'owner'
      and expected.perms is null
  ) then
    signal sqlstate '45000' set message_text = 'buyer owner role menu contains non self-management permission grants';
  end if;
end//

delimiter ;

call assert_portal_self_management_permission_seed_confirmed();
call assert_terminal_menu_range_ready();

call assert_seller_menu_permission_slot('seller:portal:home', 0, 'C', '/seller/portal', 'Seller/Portal/index', 'SellerPortalHome',
    'seller:portal:home menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:list', 0, 'F', '', null, '',
    'seller:account:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:add', 0, 'F', '', null, '',
    'seller:account:add menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:edit', 0, 'F', '', null, '',
    'seller:account:edit menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:role:query', 0, 'F', '', null, '',
    'seller:account:role:query menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:role:edit', 0, 'F', '', null, '',
    'seller:account:role:edit menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:loginLog:list', 0, 'F', '', null, '',
    'seller:account:loginLog:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:operLog:list', 0, 'F', '', null, '',
    'seller:account:operLog:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:session:list', 0, 'F', '', null, '',
    'seller:account:session:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:list', 0, 'F', '', null, '',
    'seller:dept:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:query', 0, 'F', '', null, '',
    'seller:dept:query menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:add', 0, 'F', '', null, '',
    'seller:dept:add menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:edit', 0, 'F', '', null, '',
    'seller:dept:edit menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:remove', 0, 'F', '', null, '',
    'seller:dept:remove menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:list', 0, 'F', '', null, '',
    'seller:role:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:query', 0, 'F', '', null, '',
    'seller:role:query menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:add', 0, 'F', '', null, '',
    'seller:role:add menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:edit', 0, 'F', '', null, '',
    'seller:role:edit menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:remove', 0, 'F', '', null, '',
    'seller:role:remove menu slot is occupied by another signature');

call assert_buyer_menu_permission_slot('buyer:portal:home', 0, 'C', '/buyer/portal', 'Buyer/Portal/index', 'BuyerPortalHome',
    'buyer:portal:home menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:list', 0, 'F', '', null, '',
    'buyer:account:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:add', 0, 'F', '', null, '',
    'buyer:account:add menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:edit', 0, 'F', '', null, '',
    'buyer:account:edit menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:role:query', 0, 'F', '', null, '',
    'buyer:account:role:query menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:role:edit', 0, 'F', '', null, '',
    'buyer:account:role:edit menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:loginLog:list', 0, 'F', '', null, '',
    'buyer:account:loginLog:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:operLog:list', 0, 'F', '', null, '',
    'buyer:account:operLog:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:session:list', 0, 'F', '', null, '',
    'buyer:account:session:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:list', 0, 'F', '', null, '',
    'buyer:dept:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:query', 0, 'F', '', null, '',
    'buyer:dept:query menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:add', 0, 'F', '', null, '',
    'buyer:dept:add menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:edit', 0, 'F', '', null, '',
    'buyer:dept:edit menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:remove', 0, 'F', '', null, '',
    'buyer:dept:remove menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:list', 0, 'F', '', null, '',
    'buyer:role:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:query', 0, 'F', '', null, '',
    'buyer:role:query menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:add', 0, 'F', '', null, '',
    'buyer:role:add menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:edit', 0, 'F', '', null, '',
    'buyer:role:edit menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:remove', 0, 'F', '', null, '',
    'buyer:role:remove menu slot is occupied by another signature');

start transaction;

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_name, 0, seed.order_num, '', null, '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select '账号新增' as menu_name, 31 as order_num, 'seller:account:add' as perms, '卖家端账号新增权限' as remark
    union all select '账号修改', 32, 'seller:account:edit', '卖家端账号修改权限'
    union all select '账号角色查询', 33, 'seller:account:role:query', '卖家端账号角色查询权限'
    union all select '账号角色分配', 34, 'seller:account:role:edit', '卖家端账号角色分配权限'
    union all select '部门详情', 41, 'seller:dept:query', '卖家端部门详情查询权限'
    union all select '部门新增', 42, 'seller:dept:add', '卖家端部门新增权限'
    union all select '部门修改', 43, 'seller:dept:edit', '卖家端部门修改权限'
    union all select '部门删除', 44, 'seller:dept:remove', '卖家端部门删除权限'
    union all select '角色详情', 46, 'seller:role:query', '卖家端角色详情查询权限'
    union all select '角色新增', 47, 'seller:role:add', '卖家端角色新增权限'
    union all select '角色修改', 48, 'seller:role:edit', '卖家端角色修改权限'
    union all select '角色删除', 49, 'seller:role:remove', '卖家端角色删除权限'
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
    select '账号新增' as menu_name, 31 as order_num, 'buyer:account:add' as perms, '买家端账号新增权限' as remark
    union all select '账号修改', 32, 'buyer:account:edit', '买家端账号修改权限'
    union all select '账号角色查询', 33, 'buyer:account:role:query', '买家端账号角色查询权限'
    union all select '账号角色分配', 34, 'buyer:account:role:edit', '买家端账号角色分配权限'
    union all select '部门详情', 41, 'buyer:dept:query', '买家端部门详情查询权限'
    union all select '部门新增', 42, 'buyer:dept:add', '买家端部门新增权限'
    union all select '部门修改', 43, 'buyer:dept:edit', '买家端部门修改权限'
    union all select '部门删除', 44, 'buyer:dept:remove', '买家端部门删除权限'
    union all select '角色详情', 46, 'buyer:role:query', '买家端角色详情查询权限'
    union all select '角色新增', 47, 'buyer:role:add', '买家端角色新增权限'
    union all select '角色修改', 48, 'buyer:role:edit', '买家端角色修改权限'
    union all select '角色删除', 49, 'buyer:role:remove', '买家端角色删除权限'
) seed
where not exists (
    select 1 from buyer_menu m where m.perms = seed.perms
);

delete rm
from seller_role_menu rm
join seller_role r on r.seller_role_id = rm.seller_role_id
join seller_menu m on m.seller_menu_id = rm.seller_menu_id
left join (
    select 'seller:portal:home' as perms
    union all select 'seller:account:list'
    union all select 'seller:account:add'
    union all select 'seller:account:edit'
    union all select 'seller:account:role:query'
    union all select 'seller:account:role:edit'
    union all select 'seller:account:loginLog:list'
    union all select 'seller:account:operLog:list'
    union all select 'seller:account:session:list'
    union all select 'seller:dept:list'
    union all select 'seller:dept:query'
    union all select 'seller:dept:add'
    union all select 'seller:dept:edit'
    union all select 'seller:dept:remove'
    union all select 'seller:role:list'
    union all select 'seller:role:query'
    union all select 'seller:role:add'
    union all select 'seller:role:edit'
    union all select 'seller:role:remove'
) expected on expected.perms = m.perms
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and expected.perms is null;

delete rm
from buyer_role_menu rm
join buyer_role r on r.buyer_role_id = rm.buyer_role_id
join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
left join (
    select 'buyer:portal:home' as perms
    union all select 'buyer:account:list'
    union all select 'buyer:account:add'
    union all select 'buyer:account:edit'
    union all select 'buyer:account:role:query'
    union all select 'buyer:account:role:edit'
    union all select 'buyer:account:loginLog:list'
    union all select 'buyer:account:operLog:list'
    union all select 'buyer:account:session:list'
    union all select 'buyer:dept:list'
    union all select 'buyer:dept:query'
    union all select 'buyer:dept:add'
    union all select 'buyer:dept:edit'
    union all select 'buyer:dept:remove'
    union all select 'buyer:role:list'
    union all select 'buyer:role:query'
    union all select 'buyer:role:add'
    union all select 'buyer:role:edit'
    union all select 'buyer:role:remove'
) expected on expected.perms = m.perms
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and expected.perms is null;

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

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms in (
    'seller:account:add',
    'seller:account:list',
    'seller:account:edit',
    'seller:account:role:query',
    'seller:account:role:edit',
    'seller:account:loginLog:list',
    'seller:account:operLog:list',
    'seller:account:session:list',
    'seller:dept:list',
    'seller:dept:query',
    'seller:dept:add',
    'seller:dept:edit',
    'seller:dept:remove',
    'seller:role:list',
    'seller:role:query',
    'seller:role:add',
    'seller:role:edit',
    'seller:role:remove'
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

insert into buyer_role_menu (buyer_role_id, buyer_menu_id)
select r.buyer_role_id, m.buyer_menu_id
from buyer_role r
join buyer_menu m on m.perms in (
    'buyer:account:list',
    'buyer:account:add',
    'buyer:account:edit',
    'buyer:account:role:query',
    'buyer:account:role:edit',
    'buyer:account:loginLog:list',
    'buyer:account:operLog:list',
    'buyer:account:session:list',
    'buyer:dept:list',
    'buyer:dept:query',
    'buyer:dept:add',
    'buyer:dept:edit',
    'buyer:dept:remove',
    'buyer:role:list',
    'buyer:role:query',
    'buyer:role:add',
    'buyer:role:edit',
    'buyer:role:remove'
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

call assert_portal_self_management_permission_seed_completed();
commit;

drop procedure if exists assert_seller_menu_permission_slot;
drop procedure if exists assert_buyer_menu_permission_slot;
drop procedure if exists assert_terminal_menu_range_ready;
drop procedure if exists assert_portal_self_management_permission_seed_completed;
drop procedure if exists assert_portal_self_management_permission_seed_confirmed;
