-- Terminal account OWNER uniqueness constraint.
-- Confirmed scope: remote DDL is allowed for the three-terminal isolation task.
-- Purpose: each seller/buyer subject can have at most one OWNER account.

set names utf8mb4;
set @confirm_terminal_owner_account_unique_constraint := coalesce(@confirm_terminal_owner_account_unique_constraint, '');

delimiter //

drop procedure if exists assert_terminal_owner_account_unique_constraint_confirmed//
create procedure assert_terminal_owner_account_unique_constraint_confirmed()
begin
  if coalesce(@confirm_terminal_owner_account_unique_constraint, '')
      <> 'APPLY_TERMINAL_OWNER_ACCOUNT_UNIQUE_CONSTRAINT' then
    signal sqlstate '45000' set message_text = 'set @confirm_terminal_owner_account_unique_constraint = APPLY_TERMINAL_OWNER_ACCOUNT_UNIQUE_CONSTRAINT before running this migration';
  end if;
end//

drop procedure if exists add_column_if_missing//
create procedure add_column_if_missing(in p_table varchar(64), in p_column varchar(64), in p_definition text)
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = p_table and column_name = p_column
  ) then
    set @ddl = concat('alter table ', p_table, ' add column ', p_column, ' ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists recreate_index_if_mismatch//
create procedure recreate_index_if_mismatch(
  in p_table varchar(64),
  in p_index varchar(64),
  in p_expected_columns varchar(512),
  in p_expected_non_unique int,
  in p_definition text
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
  where table_schema = database() and table_name = p_table and index_name = p_index;

  if v_index_count > 0
      and (v_actual_columns <> p_expected_columns or v_actual_non_unique <> p_expected_non_unique) then
    set @ddl = concat('alter table `', p_table, '` drop index `', p_index, '`');
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
    set v_index_count = 0;
  end if;

  if v_index_count = 0 then
    set @ddl = concat('alter table `', p_table, '` add ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_index_definition//
create procedure assert_index_definition(
  in p_table varchar(64),
  in p_index varchar(64),
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
  where table_schema = database() and table_name = p_table and index_name = p_index;

  if v_index_count <> 1
      or v_actual_columns <> p_expected_columns
      or v_actual_non_unique <> p_expected_non_unique then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_no_duplicate_owner_account//
create procedure assert_no_duplicate_owner_account(in p_table varchar(64), in p_subject_column varchar(64), in p_message varchar(128))
begin
  set @duplicate_owner_count = 0;
  set @sql = concat('select count(*) into @duplicate_owner_count from (select ', p_subject_column,
      ' from ', p_table, ' where account_role = ''OWNER'' group by ', p_subject_column, ' having count(*) > 1) t');
  prepare stmt from @sql;
  execute stmt;
  deallocate prepare stmt;
  if @duplicate_owner_count > 0 then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_owner_generated_column//
create procedure assert_owner_generated_column(
  in p_table varchar(64),
  in p_column varchar(64),
  in p_subject_column varchar(64),
  in p_message varchar(128)
)
begin
  declare v_column_count int default 0;
  declare v_extra text default '';
  declare v_generation_expression text default '';
  declare v_normalized_expression text default '';
  declare v_expected_expression text default '';

  select count(1), coalesce(max(extra), ''), coalesce(max(generation_expression), '')
    into v_column_count, v_extra, v_generation_expression
  from information_schema.columns
  where table_schema = database() and table_name = p_table and column_name = p_column;

  set v_normalized_expression = lower(v_generation_expression);
  set v_normalized_expression = replace(v_normalized_expression, '`', '');
  set v_normalized_expression = replace(v_normalized_expression, ' ', '');
  set v_normalized_expression = replace(v_normalized_expression, '\n', '');
  set v_normalized_expression = replace(v_normalized_expression, '\r', '');
  set v_normalized_expression = replace(v_normalized_expression, '(', '');
  set v_normalized_expression = replace(v_normalized_expression, ')', '');
  set v_normalized_expression = replace(v_normalized_expression, '\\', '');
  set v_normalized_expression = replace(v_normalized_expression, '_utf8mb3', '');
  set v_normalized_expression = replace(v_normalized_expression, '_utf8mb4', '');
  set v_normalized_expression = replace(v_normalized_expression, '''', '');
  set v_expected_expression = concat('casewhenaccount_role=ownerthen', lower(p_subject_column), 'elsenullend');

  if v_column_count <> 1
      or upper(v_extra) not like '%STORED GENERATED%'
      or v_normalized_expression <> v_expected_expression then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

delimiter ;

call assert_terminal_owner_account_unique_constraint_confirmed();
call assert_no_duplicate_owner_account('seller_account', 'seller_id', 'seller_account has duplicate OWNER accounts');
call assert_no_duplicate_owner_account('buyer_account', 'buyer_id', 'buyer_account has duplicate OWNER accounts');

call add_column_if_missing('seller_account', 'owner_unique_seller_id', 'bigint(20) generated always as (case when account_role = ''OWNER'' then seller_id else null end) stored');
call add_column_if_missing('buyer_account', 'owner_unique_buyer_id', 'bigint(20) generated always as (case when account_role = ''OWNER'' then buyer_id else null end) stored');

call assert_owner_generated_column('seller_account', 'owner_unique_seller_id', 'seller_id', 'seller owner generated column definition is invalid');
call assert_owner_generated_column('buyer_account', 'owner_unique_buyer_id', 'buyer_id', 'buyer owner generated column definition is invalid');

call recreate_index_if_mismatch('seller_account', 'uk_seller_account_owner',
  'owner_unique_seller_id', 0, 'unique key uk_seller_account_owner (owner_unique_seller_id)');
call recreate_index_if_mismatch('buyer_account', 'uk_buyer_account_owner',
  'owner_unique_buyer_id', 0, 'unique key uk_buyer_account_owner (owner_unique_buyer_id)');

call assert_index_definition('seller_account', 'uk_seller_account_owner',
  'owner_unique_seller_id', 0, 'seller_account OWNER unique index is invalid');
call assert_index_definition('buyer_account', 'uk_buyer_account_owner',
  'owner_unique_buyer_id', 0, 'buyer_account OWNER unique index is invalid');

drop procedure if exists add_column_if_missing;
drop procedure if exists recreate_index_if_mismatch;
drop procedure if exists assert_index_definition;
drop procedure if exists assert_no_duplicate_owner_account;
drop procedure if exists assert_owner_generated_column;
drop procedure if exists assert_terminal_owner_account_unique_constraint_confirmed;
