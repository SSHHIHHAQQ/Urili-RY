-- Optional cleanup for non-admin roles that inherited seller/buyer admin button grants.
-- Scope: sys_role_menu only. This keeps the page grant and removes child button grants
-- only for explicitly listed non-admin role_key values.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_admin_partner_non_admin_button_cleanup := coalesce(@confirm_admin_partner_non_admin_button_cleanup, '');
set @admin_partner_button_cleanup_role_keys := coalesce(@admin_partner_button_cleanup_role_keys, '');
set @admin_partner_button_cleanup_role_ids := coalesce(@admin_partner_button_cleanup_role_ids, '');
set @admin_partner_button_cleanup_expected_delete_count := coalesce(@admin_partner_button_cleanup_expected_delete_count, '');
set @admin_partner_button_cleanup_expected_signature := coalesce(@admin_partner_button_cleanup_expected_signature, '');

delimiter //

drop procedure if exists assert_admin_partner_non_admin_button_cleanup_confirmed//
create procedure assert_admin_partner_non_admin_button_cleanup_confirmed()
begin
  if coalesce(@confirm_admin_partner_non_admin_button_cleanup, '')
      <> 'CLEANUP_NON_ADMIN_PARTNER_BUTTON_GRANTS' then
    signal sqlstate '45000' set message_text = 'set @confirm_admin_partner_non_admin_button_cleanup = CLEANUP_NON_ADMIN_PARTNER_BUTTON_GRANTS before running this cleanup helper';
  end if;

  if coalesce(@admin_partner_button_cleanup_role_keys, '') = '' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_button_cleanup_role_keys to an explicit comma-separated role_key list';
  end if;

  if coalesce(@admin_partner_button_cleanup_role_ids, '') = '' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_button_cleanup_role_ids to preview-confirmed comma-separated role_id values';
  end if;

  if coalesce(@admin_partner_button_cleanup_expected_delete_count, '') = '' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_button_cleanup_expected_delete_count after previewing exact sys_role_menu rows';
  end if;

  if coalesce(@admin_partner_button_cleanup_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_button_cleanup_expected_signature after previewing exact sys_role_menu rows';
  end if;
end//

drop procedure if exists assert_admin_partner_menu_signature//
create procedure assert_admin_partner_menu_signature()
begin
  declare v_count int default 0;

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

drop procedure if exists assert_admin_partner_button_cleanup_targets//
create procedure assert_admin_partner_button_cleanup_targets()
begin
  declare v_delete_count int default 0;
  declare v_invalid_role_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1)
    into v_invalid_role_count
  from sys_role r
  where find_in_set(cast(r.role_id as char), @admin_partner_button_cleanup_role_ids) > 0
    and (
      r.role_key = 'admin'
      or find_in_set(r.role_key collate utf8mb4_unicode_ci,
          convert(@admin_partner_button_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) = 0
    );

  if v_invalid_role_count > 0 then
    signal sqlstate '45000' set message_text = 'admin partner button cleanup role_ids include admin or unlisted role_key values';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':', child_grant.role_id, child_grant.menu_id, child.perms)
           order by child_grant.role_id, child_grant.menu_id separator '|'
         ), ''), 256)
    into v_delete_count, v_signature
  from sys_role_menu child_grant
  join sys_role r on r.role_id = child_grant.role_id
  join sys_menu child on child.menu_id = child_grant.menu_id
  join sys_menu page_menu on page_menu.menu_id = child.parent_id
  join sys_role_menu page_grant on page_grant.role_id = r.role_id
      and page_grant.menu_id = page_menu.menu_id
  where r.del_flag = '0'
    and r.role_key <> 'admin'
    and find_in_set(r.role_key collate utf8mb4_unicode_ci,
        convert(@admin_partner_button_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0
    and find_in_set(cast(r.role_id as char), @admin_partner_button_cleanup_role_ids) > 0
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
    );

  if v_delete_count <> cast(@admin_partner_button_cleanup_expected_delete_count as unsigned) then
    signal sqlstate '45000' set message_text = 'admin partner button cleanup expected delete count does not match target rows';
  end if;

  if lower(v_signature) <> lower(@admin_partner_button_cleanup_expected_signature) then
    signal sqlstate '45000' set message_text = 'admin partner button cleanup exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_admin_partner_non_admin_button_cleanup_confirmed();
call assert_admin_partner_menu_signature();
call assert_admin_partner_button_cleanup_targets();

select r.role_id, r.role_key, r.role_name, count(distinct child.menu_id) as inherited_child_grants
from sys_role r
join sys_role_menu page_grant on page_grant.role_id = r.role_id
join sys_menu page_menu on page_menu.menu_id = page_grant.menu_id
join sys_role_menu child_grant on child_grant.role_id = r.role_id
join sys_menu child on child.menu_id = child_grant.menu_id
where r.del_flag = '0'
  and r.role_key <> 'admin'
  and find_in_set(r.role_key collate utf8mb4_unicode_ci,
      convert(@admin_partner_button_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0
  and find_in_set(cast(r.role_id as char), @admin_partner_button_cleanup_role_ids) > 0
  and page_menu.perms in ('seller:admin:list', 'buyer:admin:list')
  and child.parent_id = page_menu.menu_id
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
group by r.role_id, r.role_key, r.role_name
order by r.role_id;

delete child_grant
from sys_role_menu child_grant
join sys_role r on r.role_id = child_grant.role_id
join sys_menu child on child.menu_id = child_grant.menu_id
join sys_menu page_menu on page_menu.menu_id = child.parent_id
join sys_role_menu page_grant on page_grant.role_id = r.role_id
    and page_grant.menu_id = page_menu.menu_id
where r.del_flag = '0'
  and r.role_key <> 'admin'
  and find_in_set(r.role_key collate utf8mb4_unicode_ci,
      convert(@admin_partner_button_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0
  and find_in_set(cast(r.role_id as char), @admin_partner_button_cleanup_role_ids) > 0
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
  );

drop procedure if exists assert_admin_partner_non_admin_button_cleanup_confirmed;
drop procedure if exists assert_admin_partner_menu_signature;
drop procedure if exists assert_admin_partner_button_cleanup_targets;
