-- System channel to upstream main logistics channel pairing scope migration.
-- Purpose:
-- 1. Keep logistics channel pairing at system-channel scope.
-- 2. Stop using system warehouse and upstream warehouse as pairing dimensions.
-- 3. Allow one upstream main logistics channel to be reused by many system channels.

set names utf8mb4;
set @confirm_system_channel_main_channel_pairing_scope :=
    coalesce(@confirm_system_channel_main_channel_pairing_scope, '');

delimiter //

drop procedure if exists assert_system_channel_main_channel_pairing_scope_confirmed//
create procedure assert_system_channel_main_channel_pairing_scope_confirmed()
begin
  if coalesce(@confirm_system_channel_main_channel_pairing_scope, '')
      <> 'APPLY_SYSTEM_CHANNEL_MAIN_CHANNEL_PAIRING_SCOPE' then
    signal sqlstate '45000' set message_text = 'set @confirm_system_channel_main_channel_pairing_scope = APPLY_SYSTEM_CHANNEL_MAIN_CHANNEL_PAIRING_SCOPE before running this migration';
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

drop procedure if exists assert_no_system_channel_pairing_duplicates//
create procedure assert_no_system_channel_pairing_duplicates()
begin
  if exists (
    select 1
    from (
      select system_channel_code, pairing_role, count(1) as duplicate_count
      from upstream_system_logistics_channel_pairing
      group by system_channel_code, pairing_role
      having count(1) > 1
    ) duplicate_rows
  ) then
    signal sqlstate '45000' set message_text = 'duplicate system channel logistics pairing rows exist';
  end if;
end//

drop procedure if exists drop_index_if_exists//
create procedure drop_index_if_exists(in p_table varchar(64), in p_index varchar(64))
begin
  if exists (
    select 1
    from information_schema.statistics
    where table_schema = database()
      and table_name = p_table
      and index_name = p_index
  ) then
    set @ddl = concat('alter table `', p_table, '` drop index `', p_index, '`');
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
  where table_schema = database()
    and table_name = p_table
    and index_name = p_index;

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
  where table_schema = database()
    and table_name = p_table
    and index_name = p_index;

  if v_index_count <> 1
      or v_actual_columns <> p_expected_columns
      or v_actual_non_unique <> p_expected_non_unique then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

delimiter ;

call assert_system_channel_main_channel_pairing_scope_confirmed();
call assert_table_exists('upstream_system_logistics_channel_pairing',
  'upstream logistics channel pairing table is missing');
call assert_no_system_channel_pairing_duplicates();

call drop_index_if_exists('upstream_system_logistics_channel_pairing', 'uk_upstream_channel_pairing_system');
call drop_index_if_exists('upstream_system_logistics_channel_pairing', 'idx_upstream_channel_pairing_upstream');

call recreate_index_if_mismatch('upstream_system_logistics_channel_pairing',
  'uk_upstream_channel_pairing_system_role', 'system_channel_code,pairing_role', 0,
  'unique key `uk_upstream_channel_pairing_system_role` (`system_channel_code`, `pairing_role`)');
call recreate_index_if_mismatch('upstream_system_logistics_channel_pairing',
  'idx_upstream_channel_pairing_upstream_role', 'connection_code,upstream_channel_code,pairing_role', 1,
  'key `idx_upstream_channel_pairing_upstream_role` (`connection_code`, `upstream_channel_code`, `pairing_role`)');

call assert_index_definition('upstream_system_logistics_channel_pairing',
  'uk_upstream_channel_pairing_system_role', 'system_channel_code,pairing_role', 0,
  'system channel logistics pairing unique index mismatch');
call assert_index_definition('upstream_system_logistics_channel_pairing',
  'idx_upstream_channel_pairing_upstream_role', 'connection_code,upstream_channel_code,pairing_role', 1,
  'upstream logistics pairing lookup index mismatch');

drop procedure if exists assert_system_channel_main_channel_pairing_scope_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists assert_no_system_channel_pairing_duplicates;
drop procedure if exists drop_index_if_exists;
drop procedure if exists recreate_index_if_mismatch;
drop procedure if exists assert_index_definition;
