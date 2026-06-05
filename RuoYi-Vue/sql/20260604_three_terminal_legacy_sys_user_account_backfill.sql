-- Legacy helper for historical databases that still have seller_account.user_id
-- or buyer_account.user_id pointing at sys_user.
--
-- Run this file only when migrating an old mixed-account database, and run it
-- before 20260604_three_terminal_isolation_migration.sql. Fresh three-terminal
-- databases must not use this file.

set names utf8mb4;

delimiter //

drop procedure if exists migrate_seller_account_from_sys_user//
create procedure migrate_seller_account_from_sys_user()
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = 'seller_account' and column_name = 'user_id'
  ) then
    set @dml = 'update seller_account a left join sys_user u on u.user_id = a.user_id
      set a.user_name = coalesce(nullif(a.user_name, ''''), u.user_name, concat(''seller_'', a.seller_account_id)),
          a.nick_name = coalesce(nullif(a.nick_name, ''''), u.nick_name, a.user_name),
          a.password = coalesce(nullif(a.password, ''''), u.password, ''''),
          a.email = coalesce(nullif(a.email, ''''), u.email, ''''),
          a.phonenumber = coalesce(nullif(a.phonenumber, ''''), u.phonenumber, ''''),
          a.status = coalesce(nullif(a.status, ''''), u.status, ''0''),
          a.last_login_ip = coalesce(nullif(a.last_login_ip, ''''), u.login_ip, ''''),
          a.last_login_time = coalesce(a.last_login_time, u.login_date),
          a.pwd_update_time = coalesce(a.pwd_update_time, u.pwd_update_date, sysdate())';
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
    set @dml = 'update buyer_account a left join sys_user u on u.user_id = a.user_id
      set a.user_name = coalesce(nullif(a.user_name, ''''), u.user_name, concat(''buyer_'', a.buyer_account_id)),
          a.nick_name = coalesce(nullif(a.nick_name, ''''), u.nick_name, a.user_name),
          a.password = coalesce(nullif(a.password, ''''), u.password, ''''),
          a.email = coalesce(nullif(a.email, ''''), u.email, ''''),
          a.phonenumber = coalesce(nullif(a.phonenumber, ''''), u.phonenumber, ''''),
          a.status = coalesce(nullif(a.status, ''''), u.status, ''0''),
          a.last_login_ip = coalesce(nullif(a.last_login_ip, ''''), u.login_ip, ''''),
          a.last_login_time = coalesce(a.last_login_time, u.login_date),
          a.pwd_update_time = coalesce(a.pwd_update_time, u.pwd_update_date, sysdate())';
    prepare stmt from @dml;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

delimiter ;

call migrate_seller_account_from_sys_user();
call migrate_buyer_account_from_sys_user();

drop procedure if exists migrate_seller_account_from_sys_user;
drop procedure if exists migrate_buyer_account_from_sys_user;
