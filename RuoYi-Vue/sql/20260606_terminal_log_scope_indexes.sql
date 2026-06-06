-- Terminal log query scope indexes.
-- Scope: seller/buyer terminal login and operation logs only.
-- Purpose: keep subject/account scoped log queries aligned with the independent terminal log tables.

set names utf8mb4;
set @confirm_terminal_log_scope_indexes := coalesce(@confirm_terminal_log_scope_indexes, '');

drop procedure if exists recreate_index_if_mismatch;
drop procedure if exists assert_index_definition;
drop procedure if exists assert_column_exists;
drop procedure if exists assert_terminal_log_scope_indexes_confirmed;

delimiter //
create procedure assert_terminal_log_scope_indexes_confirmed()
begin
  if coalesce(@confirm_terminal_log_scope_indexes, '')
      <> 'APPLY_TERMINAL_LOG_SCOPE_INDEXES' then
    signal sqlstate '45000' set message_text = 'set @confirm_terminal_log_scope_indexes = APPLY_TERMINAL_LOG_SCOPE_INDEXES before running this migration';
  end if;
end//

create procedure assert_column_exists(
  in p_table_name varchar(64),
  in p_column_name varchar(64),
  in p_message varchar(128)
)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table_name
      and column_name = p_column_name
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

create procedure recreate_index_if_mismatch(
  in p_table_name varchar(64),
  in p_index_name varchar(64),
  in p_expected_columns varchar(512),
  in p_expected_non_unique int,
  in p_index_definition varchar(512)
)
begin
  declare v_index_count int default 0;
  declare v_actual_columns text default '';
  declare v_actual_non_unique int default null;

  select count(distinct index_name),
         coalesce(group_concat(column_name order by seq_in_index separator ','), ''),
         max(non_unique)
    into v_index_count, v_actual_columns, v_actual_non_unique
  from information_schema.statistics
  where table_schema = database()
    and table_name = p_table_name
    and index_name = p_index_name;

  if v_index_count > 0
      and (v_actual_columns <> p_expected_columns or v_actual_non_unique <> p_expected_non_unique) then
    set @ddl = concat('alter table `', p_table_name, '` drop index `', p_index_name, '`');
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
    set v_index_count = 0;
  end if;

  if v_index_count = 0 then
    set @ddl = concat('alter table `', p_table_name, '` add ', p_index_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

create procedure assert_index_definition(
  in p_table_name varchar(64),
  in p_index_name varchar(64),
  in p_expected_columns varchar(512),
  in p_expected_non_unique int,
  in p_message varchar(128)
)
begin
  declare v_index_count int default 0;
  declare v_actual_columns text default '';
  declare v_actual_non_unique int default null;

  select count(distinct index_name),
         coalesce(group_concat(column_name order by seq_in_index separator ','), ''),
         max(non_unique)
    into v_index_count, v_actual_columns, v_actual_non_unique
  from information_schema.statistics
  where table_schema = database()
    and table_name = p_table_name
    and index_name = p_index_name;

  if v_index_count <> 1
      or v_actual_columns <> p_expected_columns
      or v_actual_non_unique <> p_expected_non_unique then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//
delimiter ;

call assert_terminal_log_scope_indexes_confirmed();
call assert_column_exists('seller_login_log', 'seller_account_id', 'Run 20260604_three_terminal_isolation_migration.sql before log indexes');
call assert_column_exists('seller_login_log', 'seller_id', 'Run 20260604_three_terminal_isolation_migration.sql before log indexes');
call assert_column_exists('buyer_login_log', 'buyer_account_id', 'Run 20260604_three_terminal_isolation_migration.sql before log indexes');
call assert_column_exists('buyer_login_log', 'buyer_id', 'Run 20260604_three_terminal_isolation_migration.sql before log indexes');
call assert_column_exists('seller_oper_log', 'seller_account_id', 'Run 20260604_three_terminal_isolation_migration.sql before log indexes');
call assert_column_exists('seller_oper_log', 'seller_id', 'Run 20260604_three_terminal_isolation_migration.sql before log indexes');
call assert_column_exists('buyer_oper_log', 'buyer_account_id', 'Run 20260604_three_terminal_isolation_migration.sql before log indexes');
call assert_column_exists('buyer_oper_log', 'buyer_id', 'Run 20260604_three_terminal_isolation_migration.sql before log indexes');

call recreate_index_if_mismatch(
  'seller_login_log',
  'idx_seller_login_log_account_time',
  'seller_account_id,login_time',
  1,
  'key idx_seller_login_log_account_time (seller_account_id, login_time)'
);
call recreate_index_if_mismatch(
  'seller_login_log',
  'idx_seller_login_log_seller_time',
  'seller_id,login_time',
  1,
  'key idx_seller_login_log_seller_time (seller_id, login_time)'
);
call recreate_index_if_mismatch(
  'buyer_login_log',
  'idx_buyer_login_log_account_time',
  'buyer_account_id,login_time',
  1,
  'key idx_buyer_login_log_account_time (buyer_account_id, login_time)'
);
call recreate_index_if_mismatch(
  'buyer_login_log',
  'idx_buyer_login_log_buyer_time',
  'buyer_id,login_time',
  1,
  'key idx_buyer_login_log_buyer_time (buyer_id, login_time)'
);
call recreate_index_if_mismatch(
  'seller_oper_log',
  'idx_seller_oper_log_account_time',
  'seller_account_id,oper_time',
  1,
  'key idx_seller_oper_log_account_time (seller_account_id, oper_time)'
);
call recreate_index_if_mismatch(
  'seller_oper_log',
  'idx_seller_oper_log_seller_time',
  'seller_id,oper_time',
  1,
  'key idx_seller_oper_log_seller_time (seller_id, oper_time)'
);
call recreate_index_if_mismatch(
  'buyer_oper_log',
  'idx_buyer_oper_log_account_time',
  'buyer_account_id,oper_time',
  1,
  'key idx_buyer_oper_log_account_time (buyer_account_id, oper_time)'
);
call recreate_index_if_mismatch(
  'buyer_oper_log',
  'idx_buyer_oper_log_buyer_time',
  'buyer_id,oper_time',
  1,
  'key idx_buyer_oper_log_buyer_time (buyer_id, oper_time)'
);

call assert_index_definition('seller_login_log', 'idx_seller_login_log_account_time',
  'seller_account_id,login_time', 1, 'seller_login_log account/time index is invalid');
call assert_index_definition('seller_login_log', 'idx_seller_login_log_seller_time',
  'seller_id,login_time', 1, 'seller_login_log seller/time index is invalid');
call assert_index_definition('buyer_login_log', 'idx_buyer_login_log_account_time',
  'buyer_account_id,login_time', 1, 'buyer_login_log account/time index is invalid');
call assert_index_definition('buyer_login_log', 'idx_buyer_login_log_buyer_time',
  'buyer_id,login_time', 1, 'buyer_login_log buyer/time index is invalid');
call assert_index_definition('seller_oper_log', 'idx_seller_oper_log_account_time',
  'seller_account_id,oper_time', 1, 'seller_oper_log account/time index is invalid');
call assert_index_definition('seller_oper_log', 'idx_seller_oper_log_seller_time',
  'seller_id,oper_time', 1, 'seller_oper_log seller/time index is invalid');
call assert_index_definition('buyer_oper_log', 'idx_buyer_oper_log_account_time',
  'buyer_account_id,oper_time', 1, 'buyer_oper_log account/time index is invalid');
call assert_index_definition('buyer_oper_log', 'idx_buyer_oper_log_buyer_time',
  'buyer_id,oper_time', 1, 'buyer_oper_log buyer/time index is invalid');

drop procedure if exists recreate_index_if_mismatch;
drop procedure if exists assert_index_definition;
drop procedure if exists assert_column_exists;
drop procedure if exists assert_terminal_log_scope_indexes_confirmed;
