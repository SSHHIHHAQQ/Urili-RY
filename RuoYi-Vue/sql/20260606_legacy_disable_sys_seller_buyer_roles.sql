-- Optional legacy cleanup for databases that previously created seller/buyer roles in sys_role.
-- Do not run as part of the default three-terminal isolation migration.
-- Confirm the target database first, then execute only when historical sys_role seller/buyer
-- records should be disabled for admin-side clarity.

set names utf8mb4;

set @confirm_legacy_sys_role_cleanup := coalesce(@confirm_legacy_sys_role_cleanup, '');
set @confirm_legacy_sys_role_cleanup_profile := coalesce(@confirm_legacy_sys_role_cleanup_profile, '');
set @legacy_sys_role_cleanup_role_keys := coalesce(@legacy_sys_role_cleanup_role_keys, '');
set @legacy_sys_role_cleanup_role_ids := coalesce(@legacy_sys_role_cleanup_role_ids, '');
set @legacy_sys_role_cleanup_expected_count := coalesce(@legacy_sys_role_cleanup_expected_count, '');
set @legacy_sys_role_cleanup_expected_signature := coalesce(@legacy_sys_role_cleanup_expected_signature, '');

delimiter //

drop procedure if exists assert_legacy_sys_role_cleanup_confirmed//
create procedure assert_legacy_sys_role_cleanup_confirmed()
begin
  if coalesce(@confirm_legacy_sys_role_cleanup, '') <> 'DISABLE_SYS_ROLE_SELLER_BUYER' then
    signal sqlstate '45000' set message_text = 'set @confirm_legacy_sys_role_cleanup = DISABLE_SYS_ROLE_SELLER_BUYER before running this legacy helper';
  end if;
  if coalesce(@confirm_legacy_sys_role_cleanup_profile, '') <> 'LEGACY_SYS_ROLE_SELLER_BUYER_CONFIRMED' then
    signal sqlstate '45000' set message_text = 'set @confirm_legacy_sys_role_cleanup_profile = LEGACY_SYS_ROLE_SELLER_BUYER_CONFIRMED before running this legacy helper';
  end if;
  if coalesce(@legacy_sys_role_cleanup_role_keys, '') <> 'seller,buyer' then
    signal sqlstate '45000' set message_text = 'set @legacy_sys_role_cleanup_role_keys = seller,buyer after previewing exact sys_role candidates';
  end if;
  if coalesce(@legacy_sys_role_cleanup_role_ids, '') = '' then
    signal sqlstate '45000' set message_text = 'set @legacy_sys_role_cleanup_role_ids to preview-confirmed comma-separated role_id values';
  end if;
  if coalesce(@legacy_sys_role_cleanup_expected_count, '') = '' then
    signal sqlstate '45000' set message_text = 'set @legacy_sys_role_cleanup_expected_count after previewing exact sys_role candidates';
  end if;
  if coalesce(@legacy_sys_role_cleanup_expected_signature, '') = '' then
    signal sqlstate '45000' set message_text = 'set @legacy_sys_role_cleanup_expected_signature after previewing exact sys_role candidates';
  end if;
end//

drop procedure if exists assert_table_exists//
create procedure assert_table_exists(in p_table varchar(64), in p_message varchar(128))
begin
  if not exists (
    select 1
    from information_schema.tables
    where table_schema = database()
      and table_name = p_table
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_no_active_terminal_sys_role_bindings//
create procedure assert_no_active_terminal_sys_role_bindings()
begin
  declare v_count int default 0;

  select count(1)
    into v_count
  from sys_role r
  inner join sys_user_role ur on ur.role_id = r.role_id
  inner join sys_user u on u.user_id = ur.user_id
  where r.role_key in ('seller', 'buyer')
    and find_in_set(r.role_key collate utf8mb4_unicode_ci,
        convert(@legacy_sys_role_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0
    and r.status = '0'
    and r.del_flag = '0'
    and u.del_flag = '0';

  if v_count > 0 then
    signal sqlstate '45000' set message_text = 'active sys_role seller/buyer bindings still exist';
  end if;
end//

drop procedure if exists assert_terminal_owner_roles_ready//
create procedure assert_terminal_owner_roles_ready()
begin
  declare v_missing_seller_owner_roles int default 0;
  declare v_missing_buyer_owner_roles int default 0;

  select count(1)
    into v_missing_seller_owner_roles
  from seller_account a
  where a.account_role = 'OWNER'
    and not exists (
      select 1
      from seller_role r
      where r.seller_id = a.seller_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
    );

  select count(1)
    into v_missing_buyer_owner_roles
  from buyer_account a
  where a.account_role = 'OWNER'
    and not exists (
      select 1
      from buyer_role r
      where r.buyer_id = a.buyer_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
    );

  if v_missing_seller_owner_roles > 0 then
    signal sqlstate '45000' set message_text = 'seller owner terminal roles are not ready before legacy sys_role cleanup';
  end if;
  if v_missing_buyer_owner_roles > 0 then
    signal sqlstate '45000' set message_text = 'buyer owner terminal roles are not ready before legacy sys_role cleanup';
  end if;
end//

drop procedure if exists assert_legacy_sys_role_cleanup_targets//
create procedure assert_legacy_sys_role_cleanup_targets()
begin
  declare v_count int default 0;
  declare v_invalid_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1)
    into v_count
  from sys_role
  where find_in_set(cast(role_id as char), @legacy_sys_role_cleanup_role_ids) > 0
    and role_key in ('seller', 'buyer')
    and find_in_set(role_key collate utf8mb4_unicode_ci,
        convert(@legacy_sys_role_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0;

  select count(1)
    into v_invalid_count
  from sys_role
  where find_in_set(cast(role_id as char), @legacy_sys_role_cleanup_role_ids) > 0
    and (
      role_key not in ('seller', 'buyer')
      or find_in_set(role_key collate utf8mb4_unicode_ci,
          convert(@legacy_sys_role_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) = 0
    );

  if v_invalid_count > 0 then
    signal sqlstate '45000' set message_text = 'legacy sys_role cleanup role_ids include non seller/buyer roles';
  end if;
  if v_count <> cast(@legacy_sys_role_cleanup_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'legacy sys_role cleanup expected count does not match role_ids';
  end if;

  select coalesce(sha2(group_concat(concat_ws(':',
        cast(role_id as char),
        coalesce(role_key, ''),
        coalesce(role_name, ''),
        coalesce(status, ''),
        coalesce(del_flag, ''))
      order by role_id separator '|'), 256), '')
    into v_signature
  from sys_role
  where find_in_set(cast(role_id as char), @legacy_sys_role_cleanup_role_ids) > 0
    and role_key in ('seller', 'buyer')
    and find_in_set(role_key collate utf8mb4_unicode_ci,
        convert(@legacy_sys_role_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0;

  if v_signature <> @legacy_sys_role_cleanup_expected_signature then
    signal sqlstate '45000' set message_text = 'legacy sys_role cleanup exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_legacy_sys_role_cleanup_confirmed();
call assert_table_exists('seller_account', 'seller_account is required before disabling legacy sys_role seller');
call assert_table_exists('buyer_account', 'buyer_account is required before disabling legacy sys_role buyer');
call assert_table_exists('seller_role', 'seller_role is required before disabling legacy sys_role seller');
call assert_table_exists('buyer_role', 'buyer_role is required before disabling legacy sys_role buyer');
call assert_no_active_terminal_sys_role_bindings();
call assert_terminal_owner_roles_ready();
call assert_legacy_sys_role_cleanup_targets();

select role_id, role_name, role_key, status, del_flag
from sys_role
where role_key in ('seller', 'buyer')
  and find_in_set(role_key collate utf8mb4_unicode_ci,
      convert(@legacy_sys_role_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0
  and find_in_set(cast(role_id as char), @legacy_sys_role_cleanup_role_ids) > 0
order by role_id;

select sha2(group_concat(concat_ws(':',
        cast(role_id as char),
        coalesce(role_key, ''),
        coalesce(role_name, ''),
        coalesce(status, ''),
        coalesce(del_flag, ''))
    order by role_id separator '|'), 256) as legacy_sys_role_cleanup_expected_signature
from sys_role
where role_key in ('seller', 'buyer')
  and find_in_set(role_key collate utf8mb4_unicode_ci,
      convert(@legacy_sys_role_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0
  and find_in_set(cast(role_id as char), @legacy_sys_role_cleanup_role_ids) > 0;

update sys_role
set status = '1',
    del_flag = '2',
    update_by = 'admin',
    update_time = sysdate(),
    remark = case
      when coalesce(remark, '') like '%three-terminal isolation moved terminal roles out of sys_role%'
        then remark
      else concat(coalesce(remark, ''), '; three-terminal isolation moved terminal roles out of sys_role')
    end
where role_key in ('seller', 'buyer')
  and find_in_set(role_key collate utf8mb4_unicode_ci,
      convert(@legacy_sys_role_cleanup_role_keys using utf8mb4) collate utf8mb4_unicode_ci) > 0
  and find_in_set(cast(role_id as char), @legacy_sys_role_cleanup_role_ids) > 0;

drop procedure if exists assert_legacy_sys_role_cleanup_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists assert_no_active_terminal_sys_role_bindings;
drop procedure if exists assert_terminal_owner_roles_ready;
drop procedure if exists assert_legacy_sys_role_cleanup_targets;
