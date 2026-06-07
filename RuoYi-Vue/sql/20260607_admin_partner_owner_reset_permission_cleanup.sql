-- Cleanup deprecated admin subject-level seller/buyer owner password reset permissions.
-- Scope: sys_role_menu and sys_menu only.
-- This does not remove account-scoped reset permissions:
--   seller:admin:account:resetPwd
--   buyer:admin:account:resetPwd

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_admin_partner_owner_reset_permission_cleanup :=
    coalesce(@confirm_admin_partner_owner_reset_permission_cleanup, '');
set @admin_partner_owner_reset_expected_role_menu_count :=
    coalesce(@admin_partner_owner_reset_expected_role_menu_count, '');
set @admin_partner_owner_reset_expected_menu_count :=
    coalesce(@admin_partner_owner_reset_expected_menu_count, '');
set @admin_partner_owner_reset_expected_role_menu_signature :=
    coalesce(@admin_partner_owner_reset_expected_role_menu_signature, '');
set @admin_partner_owner_reset_expected_menu_signature :=
    coalesce(@admin_partner_owner_reset_expected_menu_signature, '');

delimiter //

drop procedure if exists assert_admin_partner_owner_reset_permission_cleanup_confirmed//
create procedure assert_admin_partner_owner_reset_permission_cleanup_confirmed()
begin
  if coalesce(@confirm_admin_partner_owner_reset_permission_cleanup, '')
      <> 'CLEANUP_ADMIN_PARTNER_OWNER_RESET_PERMISSION' then
    signal sqlstate '45000' set message_text = 'set @confirm_admin_partner_owner_reset_permission_cleanup = CLEANUP_ADMIN_PARTNER_OWNER_RESET_PERMISSION before running this cleanup';
  end if;

  if coalesce(@admin_partner_owner_reset_expected_role_menu_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_owner_reset_expected_role_menu_count after previewing exact sys_role_menu rows';
  end if;

  if coalesce(@admin_partner_owner_reset_expected_menu_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_owner_reset_expected_menu_count after previewing exact sys_menu rows';
  end if;

  if coalesce(@admin_partner_owner_reset_expected_role_menu_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_owner_reset_expected_role_menu_signature after previewing exact sys_role_menu rows';
  end if;

  if coalesce(@admin_partner_owner_reset_expected_menu_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @admin_partner_owner_reset_expected_menu_signature after previewing exact sys_menu rows';
  end if;
end//

drop procedure if exists assert_admin_partner_owner_reset_permission_cleanup_targets//
create procedure assert_admin_partner_owner_reset_permission_cleanup_targets()
begin
  declare v_bad_menu_count int default 0;
  declare v_role_menu_count int default 0;
  declare v_menu_count int default 0;
  declare v_role_menu_signature varchar(64) default '';
  declare v_menu_signature varchar(64) default '';

  select count(1)
    into v_bad_menu_count
  from sys_menu m
  where m.menu_id in (2204, 2214)
    and not (
      (m.menu_id = 2204
        and coalesce(m.parent_id, -1) = 2011
        and coalesce(m.menu_type, '') = 'F'
        and coalesce(m.path, '') = '#'
        and coalesce(m.component, '') = ''
        and coalesce(m.route_name, '') = ''
        and coalesce(m.perms, '') = 'seller:admin:resetPwd')
      or
      (m.menu_id = 2214
        and coalesce(m.parent_id, -1) = 2012
        and coalesce(m.menu_type, '') = 'F'
        and coalesce(m.path, '') = '#'
        and coalesce(m.component, '') = ''
        and coalesce(m.route_name, '') = ''
        and coalesce(m.perms, '') = 'buyer:admin:resetPwd')
    );

  if v_bad_menu_count > 0 then
    signal sqlstate '45000' set message_text = 'admin partner owner reset cleanup target menu_id is occupied by another signature';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':', rm.role_id, rm.menu_id)
           order by rm.role_id, rm.menu_id separator '|'
         ), ''), 256)
    into v_role_menu_count, v_role_menu_signature
  from sys_role_menu rm
  where rm.menu_id in (2204, 2214);

  if v_role_menu_count <> cast(@admin_partner_owner_reset_expected_role_menu_count as unsigned) then
    signal sqlstate '45000' set message_text = 'admin partner owner reset role-menu cleanup expected count does not match target rows';
  end if;

  if lower(v_role_menu_signature) <> lower(@admin_partner_owner_reset_expected_role_menu_signature) then
    signal sqlstate '45000' set message_text = 'admin partner owner reset role-menu exact target signature mismatch';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             m.menu_id,
             coalesce(m.parent_id, ''),
             coalesce(m.menu_type, ''),
             coalesce(m.path, ''),
             coalesce(m.component, ''),
             coalesce(m.route_name, ''),
             coalesce(m.perms, '')
           )
           order by m.menu_id separator '|'
         ), ''), 256)
    into v_menu_count, v_menu_signature
  from sys_menu m
  where (m.menu_id = 2204
        and coalesce(m.parent_id, -1) = 2011
        and coalesce(m.menu_type, '') = 'F'
        and coalesce(m.path, '') = '#'
        and coalesce(m.component, '') = ''
        and coalesce(m.route_name, '') = ''
        and coalesce(m.perms, '') = 'seller:admin:resetPwd')
     or (m.menu_id = 2214
        and coalesce(m.parent_id, -1) = 2012
        and coalesce(m.menu_type, '') = 'F'
        and coalesce(m.path, '') = '#'
        and coalesce(m.component, '') = ''
        and coalesce(m.route_name, '') = ''
        and coalesce(m.perms, '') = 'buyer:admin:resetPwd');

  if v_menu_count <> cast(@admin_partner_owner_reset_expected_menu_count as unsigned) then
    signal sqlstate '45000' set message_text = 'admin partner owner reset menu cleanup expected count does not match target rows';
  end if;

  if lower(v_menu_signature) <> lower(@admin_partner_owner_reset_expected_menu_signature) then
    signal sqlstate '45000' set message_text = 'admin partner owner reset menu exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_admin_partner_owner_reset_permission_cleanup_confirmed();
call assert_admin_partner_owner_reset_permission_cleanup_targets();

select m.menu_id, m.parent_id, m.menu_type, m.path, m.component, m.route_name, m.perms,
       count(rm.role_id) as role_menu_grants
from sys_menu m
left join sys_role_menu rm on rm.menu_id = m.menu_id
where m.menu_id in (2204, 2214)
group by m.menu_id, m.parent_id, m.menu_type, m.path, m.component, m.route_name, m.perms
order by m.menu_id;

delete rm
from sys_role_menu rm
where rm.menu_id in (2204, 2214);

delete m
from sys_menu m
where (m.menu_id = 2204
      and coalesce(m.parent_id, -1) = 2011
      and coalesce(m.menu_type, '') = 'F'
      and coalesce(m.path, '') = '#'
      and coalesce(m.component, '') = ''
      and coalesce(m.route_name, '') = ''
      and coalesce(m.perms, '') = 'seller:admin:resetPwd')
   or (m.menu_id = 2214
      and coalesce(m.parent_id, -1) = 2012
      and coalesce(m.menu_type, '') = 'F'
      and coalesce(m.path, '') = '#'
      and coalesce(m.component, '') = ''
      and coalesce(m.route_name, '') = ''
      and coalesce(m.perms, '') = 'buyer:admin:resetPwd');

drop procedure if exists assert_admin_partner_owner_reset_permission_cleanup_confirmed;
drop procedure if exists assert_admin_partner_owner_reset_permission_cleanup_targets;
