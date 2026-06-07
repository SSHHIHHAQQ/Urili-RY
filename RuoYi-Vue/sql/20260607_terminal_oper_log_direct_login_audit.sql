-- Add direct-login audit fields to seller/buyer operation logs.
-- This script is guarded and idempotent. It does not execute automatically.

set names utf8mb4;

set @confirm_terminal_oper_log_direct_login_audit := coalesce(@confirm_terminal_oper_log_direct_login_audit, '');

delimiter //

drop procedure if exists assert_terminal_oper_log_direct_login_audit_confirmed//
create procedure assert_terminal_oper_log_direct_login_audit_confirmed()
begin
  if coalesce(@confirm_terminal_oper_log_direct_login_audit, '')
     <> 'APPLY_TERMINAL_OPER_LOG_DIRECT_LOGIN_AUDIT' then
    signal sqlstate '45000' set message_text = 'set @confirm_terminal_oper_log_direct_login_audit = APPLY_TERMINAL_OPER_LOG_DIRECT_LOGIN_AUDIT before running this migration';
  end if;
end//

drop procedure if exists assert_table_exists//
create procedure assert_table_exists(in p_table varchar(64), in p_message varchar(255))
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

drop procedure if exists add_column_if_missing//
create procedure add_column_if_missing(in p_table varchar(64), in p_column varchar(64), in p_definition text)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table
      and column_name = p_column
  ) then
    set @ddl = concat('alter table ', p_table, ' add column ', p_column, ' ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_column_exists//
create procedure assert_column_exists(in p_table varchar(64), in p_column varchar(64), in p_message varchar(255))
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table
      and column_name = p_column
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

delimiter ;

call assert_terminal_oper_log_direct_login_audit_confirmed();

call assert_table_exists('seller_oper_log', 'Run 20260604_three_terminal_isolation_migration.sql before oper log direct-login audit');
call assert_table_exists('buyer_oper_log', 'Run 20260604_three_terminal_isolation_migration.sql before oper log direct-login audit');

call add_column_if_missing('seller_oper_log', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('seller_oper_log', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('seller_oper_log', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('seller_oper_log', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('seller_oper_log', 'direct_login_reason', 'varchar(255) default ''''');

call add_column_if_missing('buyer_oper_log', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('buyer_oper_log', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('buyer_oper_log', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('buyer_oper_log', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('buyer_oper_log', 'direct_login_reason', 'varchar(255) default ''''');

call assert_column_exists('seller_oper_log', 'direct_login', 'seller_oper_log.direct_login is required');
call assert_column_exists('seller_oper_log', 'direct_login_ticket_id', 'seller_oper_log.direct_login_ticket_id is required');
call assert_column_exists('seller_oper_log', 'acting_admin_id', 'seller_oper_log.acting_admin_id is required');
call assert_column_exists('seller_oper_log', 'acting_admin_name', 'seller_oper_log.acting_admin_name is required');
call assert_column_exists('seller_oper_log', 'direct_login_reason', 'seller_oper_log.direct_login_reason is required');

call assert_column_exists('buyer_oper_log', 'direct_login', 'buyer_oper_log.direct_login is required');
call assert_column_exists('buyer_oper_log', 'direct_login_ticket_id', 'buyer_oper_log.direct_login_ticket_id is required');
call assert_column_exists('buyer_oper_log', 'acting_admin_id', 'buyer_oper_log.acting_admin_id is required');
call assert_column_exists('buyer_oper_log', 'acting_admin_name', 'buyer_oper_log.acting_admin_name is required');
call assert_column_exists('buyer_oper_log', 'direct_login_reason', 'buyer_oper_log.direct_login_reason is required');

drop procedure if exists assert_terminal_oper_log_direct_login_audit_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists add_column_if_missing;
drop procedure if exists assert_column_exists;
