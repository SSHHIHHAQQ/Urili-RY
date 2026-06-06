-- Admin-side role/menu grant for seller/buyer management permissions.
-- Scope: sys_role_menu only. This does not grant seller/buyer terminal roles.

set names utf8mb4;

set @confirm_admin_partner_role_menu_grant := coalesce(@confirm_admin_partner_role_menu_grant, '');

delimiter //

drop procedure if exists assert_admin_partner_role_menu_grant_confirmed//
create procedure assert_admin_partner_role_menu_grant_confirmed()
begin
  if coalesce(@confirm_admin_partner_role_menu_grant, '') <> 'GRANT_ADMIN_PARTNER_MENUS' then
    signal sqlstate '45000' set message_text = 'set @confirm_admin_partner_role_menu_grant = GRANT_ADMIN_PARTNER_MENUS before running this grant helper';
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
end//

delimiter ;

call assert_admin_partner_role_menu_grant_confirmed();
call assert_admin_partner_menu_signature();

insert into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (2010, 2011, 2012)
where r.role_key = 'admin'
  and r.del_flag = '0'
  and not exists (
      select 1
      from sys_role_menu rm
      where rm.role_id = r.role_id
        and rm.menu_id = m.menu_id
  );

insert into sys_role_menu (role_id, menu_id)
select distinct r.role_id, child.menu_id
from sys_role r
join sys_role_menu page_grant on page_grant.role_id = r.role_id
join sys_menu page_menu on page_menu.menu_id = page_grant.menu_id
join sys_menu child on child.parent_id = page_menu.menu_id
where r.role_key = 'admin'
  and r.del_flag = '0'
  and page_menu.perms in ('seller:admin:list', 'buyer:admin:list')
  and child.menu_type = 'F'
  and (
      child.perms like 'seller:admin:%'
      or child.perms like 'buyer:admin:%'
  )
  and not exists (
      select 1
      from sys_role_menu rm
      where rm.role_id = r.role_id
        and rm.menu_id = child.menu_id
  );

drop procedure if exists assert_admin_partner_role_menu_grant_confirmed;
drop procedure if exists assert_admin_partner_menu_signature;
