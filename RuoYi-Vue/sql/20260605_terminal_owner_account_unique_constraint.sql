-- Terminal account OWNER uniqueness constraint.
-- Confirmed scope: remote DDL is allowed for the three-terminal isolation task.
-- Purpose: each seller/buyer subject can have at most one OWNER account.

set names utf8mb4;

delimiter //

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

drop procedure if exists add_index_if_missing//
create procedure add_index_if_missing(in p_table varchar(64), in p_index varchar(64), in p_definition text)
begin
  if not exists (
    select 1 from information_schema.statistics
    where table_schema = database() and table_name = p_table and index_name = p_index
  ) then
    set @ddl = concat('alter table ', p_table, ' add ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
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

delimiter ;

call assert_no_duplicate_owner_account('seller_account', 'seller_id', 'seller_account has duplicate OWNER accounts');
call assert_no_duplicate_owner_account('buyer_account', 'buyer_id', 'buyer_account has duplicate OWNER accounts');

call add_column_if_missing('seller_account', 'owner_unique_seller_id', 'bigint(20) generated always as (case when account_role = ''OWNER'' then seller_id else null end) stored');
call add_column_if_missing('buyer_account', 'owner_unique_buyer_id', 'bigint(20) generated always as (case when account_role = ''OWNER'' then buyer_id else null end) stored');

call add_index_if_missing('seller_account', 'uk_seller_account_owner', 'unique key uk_seller_account_owner (owner_unique_seller_id)');
call add_index_if_missing('buyer_account', 'uk_buyer_account_owner', 'unique key uk_buyer_account_owner (owner_unique_buyer_id)');

drop procedure if exists add_column_if_missing;
drop procedure if exists add_index_if_missing;
drop procedure if exists assert_no_duplicate_owner_account;
