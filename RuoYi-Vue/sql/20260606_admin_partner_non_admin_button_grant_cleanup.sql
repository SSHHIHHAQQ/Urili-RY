-- Optional cleanup for non-admin roles that inherited seller/buyer admin button grants.
-- Scope: sys_role_menu only. This keeps the page grant and removes child button grants
-- only for explicitly listed non-admin role_key values.

set names utf8mb4;

set @confirm_admin_partner_non_admin_button_cleanup := coalesce(@confirm_admin_partner_non_admin_button_cleanup, '');
set @admin_partner_button_cleanup_role_keys := coalesce(@admin_partner_button_cleanup_role_keys, '');
set @admin_partner_button_cleanup_role_ids := coalesce(@admin_partner_button_cleanup_role_ids, '');
set @admin_partner_button_cleanup_expected_delete_count := coalesce(@admin_partner_button_cleanup_expected_delete_count, '');

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
end//

drop procedure if exists assert_admin_partner_button_cleanup_targets//
create procedure assert_admin_partner_button_cleanup_targets()
begin
  declare v_delete_count int default 0;
  declare v_invalid_role_count int default 0;

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

  select count(1)
    into v_delete_count
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
    and (
        child.perms like 'seller:admin:%'
        or child.perms like 'buyer:admin:%'
    );

  if v_delete_count <> cast(@admin_partner_button_cleanup_expected_delete_count as unsigned) then
    signal sqlstate '45000' set message_text = 'admin partner button cleanup expected delete count does not match target rows';
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
  and (
      child.perms like 'seller:admin:%'
      or child.perms like 'buyer:admin:%'
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
  and (
      child.perms like 'seller:admin:%'
      or child.perms like 'buyer:admin:%'
  );

drop procedure if exists assert_admin_partner_non_admin_button_cleanup_confirmed;
drop procedure if exists assert_admin_partner_menu_signature;
drop procedure if exists assert_admin_partner_button_cleanup_targets;
