-- Upstream warehouse/channel pairing role migration.
-- Purpose:
-- 1. Allow each official warehouse to bind one fulfillment warehouse and one quote warehouse.
-- 2. Bind logistics channels under a concrete warehouse context.
-- 3. Keep existing rows as fulfillment by default and fail closed before remote DDL.

set names utf8mb4;
set @confirm_upstream_pairing_role_binding := coalesce(@confirm_upstream_pairing_role_binding, '');

delimiter //

drop procedure if exists assert_upstream_pairing_role_binding_confirmed//
create procedure assert_upstream_pairing_role_binding_confirmed()
begin
  if coalesce(@confirm_upstream_pairing_role_binding, '')
      <> 'APPLY_UPSTREAM_PAIRING_ROLE_BINDING' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_pairing_role_binding = APPLY_UPSTREAM_PAIRING_ROLE_BINDING before running this migration';
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

drop procedure if exists assert_column_exists//
create procedure assert_column_exists(in p_table varchar(64), in p_column varchar(64), in p_message varchar(128))
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
    set @ddl = concat('alter table `', p_table, '` add column `', p_column, '` ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
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

call assert_upstream_pairing_role_binding_confirmed();
call assert_table_exists('upstream_system_warehouse_pairing',
  'upstream_system_warehouse_pairing is required before pairing role migration');
call assert_table_exists('upstream_system_logistics_channel_pairing',
  'upstream_system_logistics_channel_pairing is required before pairing role migration');

call assert_column_exists('upstream_system_warehouse_pairing', 'system_warehouse_name',
  'warehouse pairing requires system_warehouse_name before adding pairing_role');
call assert_column_exists('upstream_system_logistics_channel_pairing', 'connection_code',
  'logistics pairing requires connection_code before adding warehouse context');
call assert_column_exists('upstream_system_logistics_channel_pairing', 'system_channel_name',
  'logistics pairing requires system_channel_name before adding pairing_role');

call add_column_if_missing('upstream_system_warehouse_pairing', 'pairing_role',
  'varchar(32) not null default ''FULFILLMENT'' comment ''配对用途：FULFILLMENT履约仓，QUOTE报价仓'' after `system_warehouse_name`');

call add_column_if_missing('upstream_system_logistics_channel_pairing', 'system_warehouse_code',
  'varchar(64) not null default '''' comment ''系统仓库代码'' after `connection_code`');
call add_column_if_missing('upstream_system_logistics_channel_pairing', 'upstream_warehouse_code',
  'varchar(100) not null default '''' comment ''领星仓库代码'' after `system_warehouse_code`');
call add_column_if_missing('upstream_system_logistics_channel_pairing', 'pairing_role',
  'varchar(32) not null default ''FULFILLMENT'' comment ''配对用途：FULFILLMENT履约渠道，QUOTE报价渠道'' after `system_channel_name`');

update upstream_system_warehouse_pairing
set pairing_role = 'FULFILLMENT'
where pairing_role is null or pairing_role = '';

update upstream_system_logistics_channel_pairing
set pairing_role = 'FULFILLMENT'
where pairing_role is null or pairing_role = '';

update upstream_system_logistics_channel_pairing l
inner join upstream_system_warehouse_pairing p
        on p.connection_code = l.connection_code
       and p.pairing_role = l.pairing_role
set l.system_warehouse_code = p.system_warehouse_code,
    l.upstream_warehouse_code = p.upstream_warehouse_code
where l.system_warehouse_code = ''
  and l.upstream_warehouse_code = ''
  and (
      select count(1)
      from upstream_system_warehouse_pairing p2
      where p2.connection_code = l.connection_code
        and p2.pairing_role = l.pairing_role
  ) = 1;

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '上游配对用途', 'upstream_pairing_role', '0', 'admin', sysdate(), '', null, '上游配对用途'
where not exists (select 1 from sys_dict_type where dict_type = 'upstream_pairing_role');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'upstream_pairing_role', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '上游配对用途'
from (
    select 1 as dict_sort, '履约' as dict_label, 'FULFILLMENT' as dict_value, 'primary' as list_class, 'Y' as is_default
    union all select 2, '报价', 'QUOTE', 'warning', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'upstream_pairing_role' and d.dict_value = seed.dict_value);

call drop_index_if_exists('upstream_system_warehouse_pairing', 'uk_upstream_wh_pairing_system');
call drop_index_if_exists('upstream_system_warehouse_pairing', 'uk_upstream_wh_pairing_upstream');
call drop_index_if_exists('upstream_system_logistics_channel_pairing', 'uk_upstream_channel_pairing_system');
call drop_index_if_exists('upstream_system_logistics_channel_pairing', 'idx_upstream_channel_pairing_upstream');

call recreate_index_if_mismatch('upstream_system_warehouse_pairing',
  'uk_upstream_wh_pairing_system_role', 'system_warehouse_code,pairing_role', 0,
  'unique key `uk_upstream_wh_pairing_system_role` (`system_warehouse_code`, `pairing_role`)');
call recreate_index_if_mismatch('upstream_system_warehouse_pairing',
  'uk_upstream_wh_pairing_upstream_role', 'connection_code,upstream_warehouse_code,pairing_role', 0,
  'unique key `uk_upstream_wh_pairing_upstream_role` (`connection_code`, `upstream_warehouse_code`, `pairing_role`)');
call recreate_index_if_mismatch('upstream_system_logistics_channel_pairing',
  'uk_upstream_channel_pairing_system_role', 'system_warehouse_code,system_channel_code,pairing_role', 0,
  'unique key `uk_upstream_channel_pairing_system_role` (`system_warehouse_code`, `system_channel_code`, `pairing_role`)');
call recreate_index_if_mismatch('upstream_system_logistics_channel_pairing',
  'idx_upstream_channel_pairing_upstream_role', 'connection_code,upstream_warehouse_code,upstream_channel_code,pairing_role', 1,
  'key `idx_upstream_channel_pairing_upstream_role` (`connection_code`, `upstream_warehouse_code`, `upstream_channel_code`, `pairing_role`)');

call assert_index_definition('upstream_system_warehouse_pairing',
  'uk_upstream_wh_pairing_system_role', 'system_warehouse_code,pairing_role', 0,
  'warehouse system pairing role unique index mismatch');
call assert_index_definition('upstream_system_warehouse_pairing',
  'uk_upstream_wh_pairing_upstream_role', 'connection_code,upstream_warehouse_code,pairing_role', 0,
  'warehouse upstream pairing role unique index mismatch');
call assert_index_definition('upstream_system_logistics_channel_pairing',
  'uk_upstream_channel_pairing_system_role', 'system_warehouse_code,system_channel_code,pairing_role', 0,
  'logistics system pairing role unique index mismatch');
call assert_index_definition('upstream_system_logistics_channel_pairing',
  'idx_upstream_channel_pairing_upstream_role', 'connection_code,upstream_warehouse_code,upstream_channel_code,pairing_role', 1,
  'logistics upstream pairing role index mismatch');

drop procedure if exists assert_upstream_pairing_role_binding_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists assert_column_exists;
drop procedure if exists add_column_if_missing;
drop procedure if exists drop_index_if_exists;
drop procedure if exists recreate_index_if_mismatch;
drop procedure if exists assert_index_definition;
