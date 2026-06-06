-- Legacy helper for historical databases that still have seller_account.user_id
-- or buyer_account.user_id pointing at sys_user.
--
-- Run this file only when migrating an old mixed-account database, and run it
-- before 20260604_three_terminal_isolation_migration.sql. Fresh three-terminal
-- databases must not use this file.

set names utf8mb4;

set @confirm_legacy_sys_user_backfill := coalesce(@confirm_legacy_sys_user_backfill, '');
set @confirm_legacy_sys_user_backfill_profile := coalesce(@confirm_legacy_sys_user_backfill_profile, '');
set @legacy_seller_missing_sys_user_rows := 0;
set @legacy_seller_blank_password_rows := 0;
set @legacy_buyer_missing_sys_user_rows := 0;
set @legacy_buyer_blank_password_rows := 0;
set @legacy_seller_user_id_rows := 0;
set @legacy_buyer_user_id_rows := 0;

delimiter //

drop procedure if exists assert_legacy_sys_user_backfill_confirmed//
create procedure assert_legacy_sys_user_backfill_confirmed()
begin
  if coalesce(@confirm_legacy_sys_user_backfill, '') <> 'BACKFILL_TERMINAL_ACCOUNTS_FROM_SYS_USER' then
    signal sqlstate '45000' set message_text = 'set @confirm_legacy_sys_user_backfill = BACKFILL_TERMINAL_ACCOUNTS_FROM_SYS_USER before running this legacy helper';
  end if;
  if coalesce(@confirm_legacy_sys_user_backfill_profile, '') <> 'LEGACY_SYS_USER_ACCOUNT_MIXED_DB' then
    signal sqlstate '45000' set message_text = 'set @confirm_legacy_sys_user_backfill_profile = LEGACY_SYS_USER_ACCOUNT_MIXED_DB before running this legacy helper';
  end if;
end//

drop procedure if exists assert_legacy_user_id_binding_exists//
create procedure assert_legacy_user_id_binding_exists()
begin
  declare v_seller_user_id_column_count int default 0;
  declare v_buyer_user_id_column_count int default 0;

  select count(1)
    into v_seller_user_id_column_count
  from information_schema.columns
  where table_schema = database()
    and table_name = 'seller_account'
    and column_name = 'user_id';

  select count(1)
    into v_buyer_user_id_column_count
  from information_schema.columns
  where table_schema = database()
    and table_name = 'buyer_account'
    and column_name = 'user_id';

  if v_seller_user_id_column_count = 0 and v_buyer_user_id_column_count = 0 then
    signal sqlstate '45000' set message_text = 'legacy sys_user backfill requires seller_account.user_id or buyer_account.user_id';
  end if;

  if v_seller_user_id_column_count > 0 then
    set @legacy_seller_user_id_rows_sql = 'select count(1) into @legacy_seller_user_id_rows from seller_account where user_id is not null';
    prepare stmt from @legacy_seller_user_id_rows_sql;
    execute stmt;
    deallocate prepare stmt;
  end if;

  if v_buyer_user_id_column_count > 0 then
    set @legacy_buyer_user_id_rows_sql = 'select count(1) into @legacy_buyer_user_id_rows from buyer_account where user_id is not null';
    prepare stmt from @legacy_buyer_user_id_rows_sql;
    execute stmt;
    deallocate prepare stmt;
  end if;

  if coalesce(@legacy_seller_user_id_rows, 0) + coalesce(@legacy_buyer_user_id_rows, 0) = 0 then
    signal sqlstate '45000' set message_text = 'legacy sys_user backfill requires at least one terminal account row with user_id';
  end if;
end//

drop procedure if exists migrate_seller_account_from_sys_user//
create procedure migrate_seller_account_from_sys_user()
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = 'seller_account' and column_name = 'user_id'
  ) then
    set @preview = 'select ''seller_account'' as target_table, count(1) as legacy_rows, sum(case when u.user_id is null then 1 else 0 end) as missing_sys_user_rows, sum(case when coalesce(a.user_name, '''') = '''' or coalesce(a.password, '''') = '''' or coalesce(a.nick_name, '''') = '''' then 1 else 0 end) as rows_with_blank_account_fields from seller_account a left join sys_user u on u.user_id = a.user_id where a.user_id is not null';
    prepare stmt from @preview;
    execute stmt;
    deallocate prepare stmt;

    set @guard = 'select
        sum(case when u.user_id is null then 1 else 0 end),
        sum(case when coalesce(nullif(a.password, ''''), u.password, '''') = '''' then 1 else 0 end)
      into @legacy_seller_missing_sys_user_rows, @legacy_seller_blank_password_rows
      from seller_account a left join sys_user u on u.user_id = a.user_id
      where a.user_id is not null';
    prepare stmt from @guard;
    execute stmt;
    deallocate prepare stmt;

    if coalesce(@legacy_seller_missing_sys_user_rows, 0) > 0 then
      signal sqlstate '45000' set message_text = 'seller_account legacy backfill has rows missing sys_user';
    end if;
    if coalesce(@legacy_seller_blank_password_rows, 0) > 0 then
      signal sqlstate '45000' set message_text = 'seller_account legacy backfill would leave blank password rows';
    end if;

    set @dml = 'update seller_account a left join sys_user u on u.user_id = a.user_id
      set a.user_name = coalesce(nullif(a.user_name, ''''), u.user_name, concat(''seller_'', a.seller_account_id)),
          a.nick_name = coalesce(nullif(a.nick_name, ''''), u.nick_name, a.user_name),
          a.password = coalesce(nullif(a.password, ''''), u.password, ''''),
          a.email = coalesce(nullif(a.email, ''''), u.email, ''''),
          a.phonenumber = coalesce(nullif(a.phonenumber, ''''), u.phonenumber, ''''),
          a.status = coalesce(nullif(a.status, ''''), u.status, ''0''),
          a.last_login_ip = coalesce(nullif(a.last_login_ip, ''''), u.login_ip, ''''),
          a.last_login_time = coalesce(a.last_login_time, u.login_date),
          a.pwd_update_time = coalesce(a.pwd_update_time, u.pwd_update_date, sysdate())
      where a.user_id is not null';
    prepare stmt from @dml;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists migrate_buyer_account_from_sys_user//
create procedure migrate_buyer_account_from_sys_user()
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = 'buyer_account' and column_name = 'user_id'
  ) then
    set @preview = 'select ''buyer_account'' as target_table, count(1) as legacy_rows, sum(case when u.user_id is null then 1 else 0 end) as missing_sys_user_rows, sum(case when coalesce(a.user_name, '''') = '''' or coalesce(a.password, '''') = '''' or coalesce(a.nick_name, '''') = '''' then 1 else 0 end) as rows_with_blank_account_fields from buyer_account a left join sys_user u on u.user_id = a.user_id where a.user_id is not null';
    prepare stmt from @preview;
    execute stmt;
    deallocate prepare stmt;

    set @guard = 'select
        sum(case when u.user_id is null then 1 else 0 end),
        sum(case when coalesce(nullif(a.password, ''''), u.password, '''') = '''' then 1 else 0 end)
      into @legacy_buyer_missing_sys_user_rows, @legacy_buyer_blank_password_rows
      from buyer_account a left join sys_user u on u.user_id = a.user_id
      where a.user_id is not null';
    prepare stmt from @guard;
    execute stmt;
    deallocate prepare stmt;

    if coalesce(@legacy_buyer_missing_sys_user_rows, 0) > 0 then
      signal sqlstate '45000' set message_text = 'buyer_account legacy backfill has rows missing sys_user';
    end if;
    if coalesce(@legacy_buyer_blank_password_rows, 0) > 0 then
      signal sqlstate '45000' set message_text = 'buyer_account legacy backfill would leave blank password rows';
    end if;

    set @dml = 'update buyer_account a left join sys_user u on u.user_id = a.user_id
      set a.user_name = coalesce(nullif(a.user_name, ''''), u.user_name, concat(''buyer_'', a.buyer_account_id)),
          a.nick_name = coalesce(nullif(a.nick_name, ''''), u.nick_name, a.user_name),
          a.password = coalesce(nullif(a.password, ''''), u.password, ''''),
          a.email = coalesce(nullif(a.email, ''''), u.email, ''''),
          a.phonenumber = coalesce(nullif(a.phonenumber, ''''), u.phonenumber, ''''),
          a.status = coalesce(nullif(a.status, ''''), u.status, ''0''),
          a.last_login_ip = coalesce(nullif(a.last_login_ip, ''''), u.login_ip, ''''),
          a.last_login_time = coalesce(a.last_login_time, u.login_date),
          a.pwd_update_time = coalesce(a.pwd_update_time, u.pwd_update_date, sysdate())
      where a.user_id is not null';
    prepare stmt from @dml;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

delimiter ;

call assert_legacy_sys_user_backfill_confirmed();
call assert_legacy_user_id_binding_exists();
call migrate_seller_account_from_sys_user();
call migrate_buyer_account_from_sys_user();

drop procedure if exists assert_legacy_sys_user_backfill_confirmed;
drop procedure if exists assert_legacy_user_id_binding_exists;
drop procedure if exists migrate_seller_account_from_sys_user;
drop procedure if exists migrate_buyer_account_from_sys_user;
