-- Admin-side role/menu grant for seller/buyer management permissions.
-- Scope: sys_role_menu only. This does not grant seller/buyer terminal roles.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_admin_partner_role_menu_grant := coalesce(@confirm_admin_partner_role_menu_grant, '');
set @admin_partner_role_menu_grant_role_ids := coalesce(@admin_partner_role_menu_grant_role_ids, '');
set @admin_partner_role_menu_grant_expected_role_count := coalesce(@admin_partner_role_menu_grant_expected_role_count, '');
set @admin_partner_role_menu_grant_expected_role_signature := coalesce(@admin_partner_role_menu_grant_expected_role_signature, '');
set @admin_partner_role_menu_grant_expected_grant_count := coalesce(@admin_partner_role_menu_grant_expected_grant_count, '');
set @admin_partner_role_menu_grant_expected_grant_signature := coalesce(@admin_partner_role_menu_grant_expected_grant_signature, '');

delimiter //

drop procedure if exists assert_admin_partner_role_menu_grant_confirmed//
create procedure assert_admin_partner_role_menu_grant_confirmed()
begin
  if coalesce(@confirm_admin_partner_role_menu_grant, '') <> 'GRANT_ADMIN_PARTNER_MENUS' then
    signal sqlstate '45000' set message_text = 'set @confirm_admin_partner_role_menu_grant = GRANT_ADMIN_PARTNER_MENUS before running this grant helper';
  end if;

  if coalesce(@admin_partner_role_menu_grant_role_ids, '') = '' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_role_menu_grant_role_ids to preview-confirmed comma-separated admin role_id values';
  end if;

  if coalesce(@admin_partner_role_menu_grant_expected_role_count, '') = '' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_role_menu_grant_expected_role_count after previewing exact admin sys_role rows';
  end if;

  if coalesce(@admin_partner_role_menu_grant_expected_role_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_role_menu_grant_expected_role_signature after previewing exact admin sys_role rows';
  end if;

  if coalesce(@admin_partner_role_menu_grant_expected_grant_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_role_menu_grant_expected_grant_count after previewing exact admin sys_role_menu grant rows';
  end if;

  if coalesce(@admin_partner_role_menu_grant_expected_grant_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_role_menu_grant_expected_grant_signature after previewing exact admin sys_role_menu grant rows';
  end if;
end//

drop procedure if exists assert_admin_partner_menu_signature//
create procedure assert_admin_partner_menu_signature()
begin
  declare v_count int default 0;

  select count(1)
    into v_count
  from sys_role
  where role_key = 'admin'
    and status = '0'
    and del_flag = '0';

  if v_count = 0 then
    signal sqlstate '45000' set message_text = 'admin sys_role is required before granting partner menus';
  end if;

  select count(1)
    into v_count
  from sys_menu
  where (menu_id = 2010 and menu_name = '主体管理' and parent_id = 0 and menu_type = 'M')
     or (menu_id = 2011 and menu_name = '卖家管理' and parent_id = 2010 and menu_type = 'C'
         and perms = 'seller:admin:list')
     or (menu_id = 2012 and menu_name = '买家管理' and parent_id = 2010 and menu_type = 'C'
         and perms = 'buyer:admin:list');

  if v_count <> 3 then
    signal sqlstate '45000' set message_text = 'partner sys_menu signature does not match expected seller/buyer admin pages';
  end if;

  select count(1)
    into v_count
  from sys_menu
  where (menu_id = 2010 and parent_id = 0 and menu_type = 'M'
         and coalesce(path, '') = 'partner'
         and coalesce(component, '') = ''
         and coalesce(route_name, '') = 'PartnerManagement'
         and coalesce(perms, '') = '')
     or (menu_id = 2011 and parent_id = 2010 and menu_type = 'C'
         and coalesce(path, '') = 'seller'
         and coalesce(component, '') = 'Seller/index'
         and coalesce(route_name, '') = 'Seller'
         and perms = 'seller:admin:list')
     or (menu_id = 2012 and parent_id = 2010 and menu_type = 'C'
         and coalesce(path, '') = 'buyer'
         and coalesce(component, '') = 'Buyer/index'
         and coalesce(route_name, '') = 'Buyer'
         and perms = 'buyer:admin:list');

  if v_count <> 3 then
    signal sqlstate '45000' set message_text = 'partner sys_menu path/component/route signature does not match expected seller/buyer admin pages';
  end if;
end//

drop procedure if exists assert_admin_partner_child_menu_signature//
create procedure assert_admin_partner_child_menu_signature()
begin
  declare v_count int default 0;

  drop temporary table if exists tmp_admin_partner_child_menu_signature;
  create temporary table tmp_admin_partner_child_menu_signature (
    menu_id bigint not null primary key,
    parent_id bigint not null,
    perms varchar(100) not null
  ) engine=memory;

  insert into tmp_admin_partner_child_menu_signature (menu_id, parent_id, perms)
  values
    (2200, 2011, 'seller:admin:query'),
    (2201, 2011, 'seller:admin:add'),
    (2202, 2011, 'seller:admin:edit'),
    (2203, 2011, 'seller:admin:changeStatus'),
    (2205, 2011, 'seller:admin:directLogin'),
    (2206, 2011, 'seller:admin:forceLogout'),
    (2256, 2011, 'seller:admin:session:list'),
    (2210, 2012, 'buyer:admin:query'),
    (2211, 2012, 'buyer:admin:add'),
    (2212, 2012, 'buyer:admin:edit'),
    (2213, 2012, 'buyer:admin:changeStatus'),
    (2215, 2012, 'buyer:admin:directLogin'),
    (2216, 2012, 'buyer:admin:forceLogout'),
    (2257, 2012, 'buyer:admin:session:list'),
    (2220, 2011, 'seller:admin:menu:list'),
    (2221, 2011, 'seller:admin:menu:query'),
    (2222, 2011, 'seller:admin:menu:add'),
    (2223, 2011, 'seller:admin:menu:edit'),
    (2224, 2011, 'seller:admin:menu:remove'),
    (2225, 2011, 'seller:admin:role:list'),
    (2226, 2011, 'seller:admin:role:query'),
    (2227, 2011, 'seller:admin:role:add'),
    (2228, 2011, 'seller:admin:role:edit'),
    (2229, 2011, 'seller:admin:role:remove'),
    (2230, 2012, 'buyer:admin:menu:list'),
    (2231, 2012, 'buyer:admin:menu:query'),
    (2232, 2012, 'buyer:admin:menu:add'),
    (2233, 2012, 'buyer:admin:menu:edit'),
    (2234, 2012, 'buyer:admin:menu:remove'),
    (2235, 2012, 'buyer:admin:role:list'),
    (2236, 2012, 'buyer:admin:role:query'),
    (2237, 2012, 'buyer:admin:role:add'),
    (2238, 2012, 'buyer:admin:role:edit'),
    (2239, 2012, 'buyer:admin:role:remove'),
    (2240, 2011, 'seller:admin:dept:list'),
    (2241, 2011, 'seller:admin:dept:query'),
    (2242, 2011, 'seller:admin:dept:add'),
    (2243, 2011, 'seller:admin:dept:edit'),
    (2244, 2011, 'seller:admin:dept:remove'),
    (2245, 2012, 'buyer:admin:dept:list'),
    (2246, 2012, 'buyer:admin:dept:query'),
    (2247, 2012, 'buyer:admin:dept:add'),
    (2248, 2012, 'buyer:admin:dept:edit'),
    (2249, 2012, 'buyer:admin:dept:remove'),
    (2250, 2011, 'seller:admin:loginLog:list'),
    (2251, 2011, 'seller:admin:operLog:list'),
    (2252, 2011, 'seller:admin:ticket:list'),
    (2310, 2011, 'seller:admin:account:list'),
    (2311, 2011, 'seller:admin:account:add'),
    (2312, 2011, 'seller:admin:account:edit'),
    (2322, 2011, 'seller:admin:account:lock'),
    (2313, 2011, 'seller:admin:account:resetPwd'),
    (2314, 2011, 'seller:admin:account:role:query'),
    (2315, 2011, 'seller:admin:account:role:edit'),
    (2253, 2012, 'buyer:admin:loginLog:list'),
    (2254, 2012, 'buyer:admin:operLog:list'),
    (2255, 2012, 'buyer:admin:ticket:list'),
    (2316, 2012, 'buyer:admin:account:list'),
    (2317, 2012, 'buyer:admin:account:add'),
    (2318, 2012, 'buyer:admin:account:edit'),
    (2323, 2012, 'buyer:admin:account:lock'),
    (2319, 2012, 'buyer:admin:account:resetPwd'),
    (2320, 2012, 'buyer:admin:account:role:query'),
    (2321, 2012, 'buyer:admin:account:role:edit');

  select count(1) into v_count
  from tmp_admin_partner_child_menu_signature;

  if v_count <> 64 then
    signal sqlstate '45000' set message_text = 'partner child menu signature seed count must be 64';
  end if;

  select count(1) into v_count
  from tmp_admin_partner_child_menu_signature seed
  left join sys_menu m
    on m.menu_id = seed.menu_id
   and m.parent_id = seed.parent_id
   and m.menu_type = 'F'
   and coalesce(m.path, '') = '#'
   and coalesce(m.component, '') = ''
   and coalesce(m.route_name, '') = ''
   and m.perms = seed.perms
  where m.menu_id is null;

  if v_count <> 0 then
    signal sqlstate '45000' set message_text = 'partner sys_menu child button signature does not match expected seller/buyer admin buttons';
  end if;

end//

drop procedure if exists assert_admin_partner_role_menu_grant_targets//
create procedure assert_admin_partner_role_menu_grant_targets()
begin
  declare v_count int default 0;
  declare v_input_count int default 0;
  declare v_invalid_count int default 0;
  declare v_signature varchar(64) default '';
  declare v_grant_count int default 0;
  declare v_grant_signature varchar(64) default '';

  select count(1)
    into v_invalid_count
  from sys_role r
  where find_in_set(cast(r.role_id as char), @admin_partner_role_menu_grant_role_ids) > 0
    and (r.role_key <> 'admin' or r.status <> '0' or r.del_flag <> '0');

  if v_invalid_count > 0 then
    signal sqlstate '45000' set message_text = 'admin partner role-menu grant role_ids include non-admin, inactive, or deleted roles';
  end if;

  set v_input_count = 1
      + length(trim(both ',' from @admin_partner_role_menu_grant_role_ids))
      - length(replace(trim(both ',' from @admin_partner_role_menu_grant_role_ids), ',', ''));

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':', r.role_id, coalesce(r.role_key, ''), coalesce(r.role_name, ''),
             coalesce(r.status, ''), coalesce(r.del_flag, ''))
           order by r.role_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from sys_role r
  where r.role_key = 'admin'
    and r.status = '0'
    and r.del_flag = '0'
    and find_in_set(cast(r.role_id as char), @admin_partner_role_menu_grant_role_ids) > 0;

  if v_input_count <> v_count then
    signal sqlstate '45000' set message_text = 'admin partner role-menu grant role_ids must match active admin roles exactly';
  end if;

  if v_count <> cast(@admin_partner_role_menu_grant_expected_role_count as unsigned) then
    signal sqlstate '45000' set message_text = 'admin partner role-menu grant expected role count does not match role_ids';
  end if;

  if lower(v_signature) <> lower(@admin_partner_role_menu_grant_expected_role_signature) then
    signal sqlstate '45000' set message_text = 'admin partner role-menu grant exact role signature mismatch';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':', grant_target.role_id, grant_target.menu_id)
           order by grant_target.role_id, grant_target.menu_id separator '|'
         ), ''), 256)
    into v_grant_count, v_grant_signature
  from (
      select r.role_id, m.menu_id
      from sys_role r
      join sys_menu m on m.menu_id in (2010, 2011, 2012)
      where r.role_key = 'admin'
        and r.status = '0'
        and r.del_flag = '0'
        and find_in_set(cast(r.role_id as char), @admin_partner_role_menu_grant_role_ids) > 0
        and not exists (
            select 1
            from sys_role_menu rm
            where rm.role_id = r.role_id
              and rm.menu_id = m.menu_id
        )
      union
      select distinct r.role_id, child.menu_id
      from sys_role r
      join sys_menu page_menu on page_menu.menu_id in (2011, 2012)
      join sys_menu child on child.parent_id = page_menu.menu_id
      where r.role_key = 'admin'
        and r.status = '0'
        and r.del_flag = '0'
        and find_in_set(cast(r.role_id as char), @admin_partner_role_menu_grant_role_ids) > 0
        and page_menu.perms in ('seller:admin:list', 'buyer:admin:list')
        and child.menu_type = 'F'
        and coalesce(child.path, '') = '#'
        and coalesce(child.component, '') = ''
        and coalesce(child.route_name, '') = ''
        and substring_index(child.perms, ':', 1) = substring_index(page_menu.perms, ':', 1)
        and child.menu_id in (
            2200, 2201, 2202, 2203, 2205, 2206, 2256,
            2210, 2211, 2212, 2213, 2215, 2216, 2257,
            2220, 2221, 2222, 2223, 2224, 2225, 2226, 2227, 2228, 2229,
            2230, 2231, 2232, 2233, 2234, 2235, 2236, 2237, 2238, 2239,
            2240, 2241, 2242, 2243, 2244,
            2245, 2246, 2247, 2248, 2249,
            2250, 2251, 2252, 2253, 2254, 2255,
            2310, 2311, 2312, 2313, 2314, 2315, 2316, 2317, 2318, 2319, 2320, 2321, 2322, 2323
        )
        and child.perms in (
            'seller:admin:query', 'seller:admin:add', 'seller:admin:edit',
            'seller:admin:changeStatus', 'seller:admin:directLogin', 'seller:admin:forceLogout',
            'seller:admin:session:list',
            'buyer:admin:query', 'buyer:admin:add', 'buyer:admin:edit',
            'buyer:admin:changeStatus', 'buyer:admin:directLogin', 'buyer:admin:forceLogout',
            'buyer:admin:session:list',
            'seller:admin:menu:list', 'seller:admin:menu:query', 'seller:admin:menu:add',
            'seller:admin:menu:edit', 'seller:admin:menu:remove',
            'seller:admin:role:list', 'seller:admin:role:query', 'seller:admin:role:add',
            'seller:admin:role:edit', 'seller:admin:role:remove',
            'buyer:admin:menu:list', 'buyer:admin:menu:query', 'buyer:admin:menu:add',
            'buyer:admin:menu:edit', 'buyer:admin:menu:remove',
            'buyer:admin:role:list', 'buyer:admin:role:query', 'buyer:admin:role:add',
            'buyer:admin:role:edit', 'buyer:admin:role:remove',
            'seller:admin:dept:list', 'seller:admin:dept:query', 'seller:admin:dept:add',
            'seller:admin:dept:edit', 'seller:admin:dept:remove',
            'buyer:admin:dept:list', 'buyer:admin:dept:query', 'buyer:admin:dept:add',
            'buyer:admin:dept:edit', 'buyer:admin:dept:remove',
            'seller:admin:loginLog:list', 'seller:admin:operLog:list', 'seller:admin:ticket:list',
            'buyer:admin:loginLog:list', 'buyer:admin:operLog:list', 'buyer:admin:ticket:list',
            'seller:admin:account:list', 'seller:admin:account:add', 'seller:admin:account:edit',
            'seller:admin:account:lock', 'seller:admin:account:resetPwd',
            'seller:admin:account:role:query', 'seller:admin:account:role:edit',
            'buyer:admin:account:list', 'buyer:admin:account:add', 'buyer:admin:account:edit',
            'buyer:admin:account:lock', 'buyer:admin:account:resetPwd',
            'buyer:admin:account:role:query', 'buyer:admin:account:role:edit'
        )
        and not exists (
            select 1
            from sys_role_menu rm
            where rm.role_id = r.role_id
              and rm.menu_id = child.menu_id
        )
  ) grant_target;

  if v_grant_count <> cast(@admin_partner_role_menu_grant_expected_grant_count as unsigned) then
    signal sqlstate '45000' set message_text = 'admin partner role-menu grant exact target count mismatch';
  end if;

  if lower(v_grant_signature) <> lower(@admin_partner_role_menu_grant_expected_grant_signature) then
    signal sqlstate '45000' set message_text = 'admin partner role-menu grant exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_admin_partner_role_menu_grant_completed//
create procedure assert_admin_partner_role_menu_grant_completed()
begin
  declare v_missing_count int default 0;

  select count(1)
    into v_missing_count
  from sys_role role_seed
  join sys_menu menu_seed on menu_seed.menu_id in (2010, 2011, 2012)
  left join sys_role_menu granted
    on granted.role_id = role_seed.role_id
   and granted.menu_id = menu_seed.menu_id
  where role_seed.role_key = 'admin'
    and role_seed.status = '0'
    and role_seed.del_flag = '0'
    and find_in_set(cast(role_seed.role_id as char), @admin_partner_role_menu_grant_role_ids) > 0
    and granted.role_id is null;

  if v_missing_count <> 0 then
    signal sqlstate '45000' set message_text = 'admin partner role-menu grant completed missing root/page menu grants';
  end if;

  select count(1)
    into v_missing_count
  from sys_role role_seed
  join tmp_admin_partner_child_menu_signature seed
  join sys_menu child
    on child.menu_id = seed.menu_id
   and child.parent_id = seed.parent_id
   and child.perms = seed.perms
   and child.menu_type = 'F'
   and coalesce(child.path, '') = '#'
   and coalesce(child.component, '') = ''
   and coalesce(child.route_name, '') = ''
  left join sys_role_menu granted
    on granted.role_id = role_seed.role_id
   and granted.menu_id = child.menu_id
  where role_seed.role_key = 'admin'
    and role_seed.status = '0'
    and role_seed.del_flag = '0'
    and find_in_set(cast(role_seed.role_id as char), @admin_partner_role_menu_grant_role_ids) > 0
    and granted.role_id is null;

  if v_missing_count <> 0 then
    signal sqlstate '45000' set message_text = 'admin partner role-menu grant completed missing child button grants';
  end if;
end//

delimiter ;

call assert_admin_partner_role_menu_grant_confirmed();
call assert_admin_partner_menu_signature();
call assert_admin_partner_child_menu_signature();
call assert_admin_partner_role_menu_grant_targets();

start transaction;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (2010, 2011, 2012)
where r.role_key = 'admin'
  and r.status = '0'
  and r.del_flag = '0'
  and find_in_set(cast(r.role_id as char), @admin_partner_role_menu_grant_role_ids) > 0
  and not exists (
      select 1
      from sys_role_menu rm
      where rm.role_id = r.role_id
        and rm.menu_id = m.menu_id
  );

insert into sys_role_menu (role_id, menu_id)
select distinct r.role_id, child.menu_id
from sys_role r
join sys_menu page_menu on page_menu.menu_id in (2011, 2012)
join sys_menu child on child.parent_id = page_menu.menu_id
where r.role_key = 'admin'
  and r.status = '0'
  and r.del_flag = '0'
  and find_in_set(cast(r.role_id as char), @admin_partner_role_menu_grant_role_ids) > 0
  and page_menu.perms in ('seller:admin:list', 'buyer:admin:list')
  and child.menu_type = 'F'
  and coalesce(child.path, '') = '#'
  and coalesce(child.component, '') = ''
  and coalesce(child.route_name, '') = ''
  and substring_index(child.perms, ':', 1) = substring_index(page_menu.perms, ':', 1)
  and child.menu_id in (
      2200, 2201, 2202, 2203, 2205, 2206, 2256,
      2210, 2211, 2212, 2213, 2215, 2216, 2257,
      2220, 2221, 2222, 2223, 2224, 2225, 2226, 2227, 2228, 2229,
      2230, 2231, 2232, 2233, 2234, 2235, 2236, 2237, 2238, 2239,
      2240, 2241, 2242, 2243, 2244,
      2245, 2246, 2247, 2248, 2249,
      2250, 2251, 2252, 2253, 2254, 2255,
      2310, 2311, 2312, 2313, 2314, 2315, 2316, 2317, 2318, 2319, 2320, 2321, 2322, 2323
  )
  and child.perms in (
      'seller:admin:query', 'seller:admin:add', 'seller:admin:edit',
      'seller:admin:changeStatus', 'seller:admin:directLogin', 'seller:admin:forceLogout',
      'seller:admin:session:list',
      'buyer:admin:query', 'buyer:admin:add', 'buyer:admin:edit',
      'buyer:admin:changeStatus', 'buyer:admin:directLogin', 'buyer:admin:forceLogout',
      'buyer:admin:session:list',
      'seller:admin:menu:list', 'seller:admin:menu:query', 'seller:admin:menu:add',
      'seller:admin:menu:edit', 'seller:admin:menu:remove',
      'seller:admin:role:list', 'seller:admin:role:query', 'seller:admin:role:add',
      'seller:admin:role:edit', 'seller:admin:role:remove',
      'buyer:admin:menu:list', 'buyer:admin:menu:query', 'buyer:admin:menu:add',
      'buyer:admin:menu:edit', 'buyer:admin:menu:remove',
      'buyer:admin:role:list', 'buyer:admin:role:query', 'buyer:admin:role:add',
      'buyer:admin:role:edit', 'buyer:admin:role:remove',
      'seller:admin:dept:list', 'seller:admin:dept:query', 'seller:admin:dept:add',
      'seller:admin:dept:edit', 'seller:admin:dept:remove',
      'buyer:admin:dept:list', 'buyer:admin:dept:query', 'buyer:admin:dept:add',
      'buyer:admin:dept:edit', 'buyer:admin:dept:remove',
      'seller:admin:loginLog:list', 'seller:admin:operLog:list', 'seller:admin:ticket:list',
      'buyer:admin:loginLog:list', 'buyer:admin:operLog:list', 'buyer:admin:ticket:list',
      'seller:admin:account:list', 'seller:admin:account:add', 'seller:admin:account:edit',
      'seller:admin:account:lock', 'seller:admin:account:resetPwd',
      'seller:admin:account:role:query', 'seller:admin:account:role:edit',
      'buyer:admin:account:list', 'buyer:admin:account:add', 'buyer:admin:account:edit',
      'buyer:admin:account:lock', 'buyer:admin:account:resetPwd',
      'buyer:admin:account:role:query', 'buyer:admin:account:role:edit'
  )
  and not exists (
      select 1
      from sys_role_menu rm
      where rm.role_id = r.role_id
        and rm.menu_id = child.menu_id
  );

call assert_admin_partner_role_menu_grant_completed();

commit;

drop temporary table if exists tmp_admin_partner_child_menu_signature;

drop procedure if exists assert_admin_partner_role_menu_grant_confirmed;
drop procedure if exists assert_admin_partner_menu_signature;
drop procedure if exists assert_admin_partner_child_menu_signature;
drop procedure if exists assert_admin_partner_role_menu_grant_targets;
drop procedure if exists assert_admin_partner_role_menu_grant_completed;
