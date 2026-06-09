-- Add direct-login audit fields to seller/buyer login result logs.
-- This script is guarded and idempotent. It does not change portal_direct_login_ticket semantics.

set names utf8mb4;

set @confirm_terminal_login_log_direct_login_audit := coalesce(@confirm_terminal_login_log_direct_login_audit, '');

delimiter //

drop procedure if exists assert_terminal_login_log_direct_login_audit_confirmed//
create procedure assert_terminal_login_log_direct_login_audit_confirmed()
begin
  if coalesce(@confirm_terminal_login_log_direct_login_audit, '')
     <> 'APPLY_TERMINAL_LOGIN_LOG_DIRECT_LOGIN_AUDIT' then
    signal sqlstate '45000' set message_text = 'set @confirm_terminal_login_log_direct_login_audit = APPLY_TERMINAL_LOGIN_LOG_DIRECT_LOGIN_AUDIT before running this migration';
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

drop procedure if exists modify_direct_login_audit_columns_if_needed//
create procedure modify_direct_login_audit_columns_if_needed(in p_table varchar(64))
begin
  declare v_missing_count int default 0;
  declare v_mismatch_count int default 0;

  select count(1)
    into v_missing_count
  from (
    select 'direct_login' as expected_column
    union all select 'direct_login_ticket_id'
    union all select 'acting_admin_id'
    union all select 'acting_admin_name'
    union all select 'direct_login_reason'
  ) expected
  left join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = p_table
   and c.column_name = expected.expected_column
  where c.column_name is null;

  if v_missing_count > 0 then
    signal sqlstate '45000' set message_text = 'direct-login audit columns must exist before definition modify';
  end if;

  select count(1)
    into v_mismatch_count
  from (
    select 'direct_login' as expected_column, 'tinyint' as expected_type, null as expected_length, 'NO' as expected_nullable, '0' as expected_default
    union all select 'direct_login_ticket_id', 'bigint', null, 'YES', null
    union all select 'acting_admin_id', 'bigint', null, 'YES', null
    union all select 'acting_admin_name', 'varchar', 64, 'YES', ''
    union all select 'direct_login_reason', 'varchar', 255, 'YES', ''
  ) expected
  join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = p_table
   and c.column_name = expected.expected_column
  where lower(c.data_type) <> expected.expected_type
     or (expected.expected_length is not null and coalesce(c.character_maximum_length, -1) <> expected.expected_length)
     or c.is_nullable <> expected.expected_nullable
     or coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>');

  if v_mismatch_count > 0 then
    set @ddl = concat(
      'alter table `', p_table, '` ',
      'modify direct_login tinyint(1) not null default 0, ',
      'modify direct_login_ticket_id bigint(20) default null, ',
      'modify acting_admin_id bigint(20) default null, ',
      'modify acting_admin_name varchar(64) default '''', ',
      'modify direct_login_reason varchar(255) default '''''
    );
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_direct_login_audit_column_contract//
create procedure assert_direct_login_audit_column_contract(in p_table varchar(64))
begin
  declare v_mismatch_count int default 0;

  select count(1)
    into v_mismatch_count
  from (
    select 'direct_login' as expected_column, 'tinyint' as expected_type, null as expected_length, 'NO' as expected_nullable, '0' as expected_default
    union all select 'direct_login_ticket_id', 'bigint', null, 'YES', null
    union all select 'acting_admin_id', 'bigint', null, 'YES', null
    union all select 'acting_admin_name', 'varchar', 64, 'YES', ''
    union all select 'direct_login_reason', 'varchar', 255, 'YES', ''
  ) expected
  left join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = p_table
   and c.column_name = expected.expected_column
  where c.column_name is null
     or lower(c.data_type) <> expected.expected_type
     or (expected.expected_length is not null and coalesce(c.character_maximum_length, -1) <> expected.expected_length)
     or c.is_nullable <> expected.expected_nullable
     or coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>');

  if v_mismatch_count > 0 then
    signal sqlstate '45000' set message_text = 'direct-login audit column contract mismatch';
  end if;
end//

delimiter ;

call assert_terminal_login_log_direct_login_audit_confirmed();

call assert_table_exists('seller_login_log', 'Run 20260604_three_terminal_isolation_migration.sql before login log direct-login audit');
call assert_table_exists('buyer_login_log', 'Run 20260604_three_terminal_isolation_migration.sql before login log direct-login audit');

call add_column_if_missing('seller_login_log', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('seller_login_log', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('seller_login_log', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('seller_login_log', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('seller_login_log', 'direct_login_reason', 'varchar(255) default ''''');

call add_column_if_missing('buyer_login_log', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('buyer_login_log', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('buyer_login_log', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('buyer_login_log', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('buyer_login_log', 'direct_login_reason', 'varchar(255) default ''''');

call modify_direct_login_audit_columns_if_needed('seller_login_log');
call modify_direct_login_audit_columns_if_needed('buyer_login_log');

call assert_column_exists('seller_login_log', 'direct_login', 'seller_login_log.direct_login is required');
call assert_column_exists('seller_login_log', 'direct_login_ticket_id', 'seller_login_log.direct_login_ticket_id is required');
call assert_column_exists('seller_login_log', 'acting_admin_id', 'seller_login_log.acting_admin_id is required');
call assert_column_exists('seller_login_log', 'acting_admin_name', 'seller_login_log.acting_admin_name is required');
call assert_column_exists('seller_login_log', 'direct_login_reason', 'seller_login_log.direct_login_reason is required');

call assert_column_exists('buyer_login_log', 'direct_login', 'buyer_login_log.direct_login is required');
call assert_column_exists('buyer_login_log', 'direct_login_ticket_id', 'buyer_login_log.direct_login_ticket_id is required');
call assert_column_exists('buyer_login_log', 'acting_admin_id', 'buyer_login_log.acting_admin_id is required');
call assert_column_exists('buyer_login_log', 'acting_admin_name', 'buyer_login_log.acting_admin_name is required');
call assert_column_exists('buyer_login_log', 'direct_login_reason', 'buyer_login_log.direct_login_reason is required');
call assert_direct_login_audit_column_contract('seller_login_log');
call assert_direct_login_audit_column_contract('buyer_login_log');

drop procedure if exists assert_terminal_login_log_direct_login_audit_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists add_column_if_missing;
drop procedure if exists assert_column_exists;
drop procedure if exists modify_direct_login_audit_columns_if_needed;
drop procedure if exists assert_direct_login_audit_column_contract;
