-- Product configuration change log migration.
-- Scope: product_config_change_log table and historical update_time backfill.
-- This script is prepared for review; do not execute against a remote database without confirmation and backup.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_product_config_change_log := coalesce(@confirm_product_config_change_log, '');
set @product_config_change_log_backfill_expected_count :=
    coalesce(@product_config_change_log_backfill_expected_count, '');
set @product_config_change_log_backfill_expected_signature :=
    coalesce(@product_config_change_log_backfill_expected_signature, '');

delimiter //

drop procedure if exists assert_product_config_change_log_confirmed//
create procedure assert_product_config_change_log_confirmed()
begin
  if coalesce(@confirm_product_config_change_log, '')
      <> 'APPLY_PRODUCT_CONFIG_CHANGE_LOG' then
    signal sqlstate '45000' set message_text = 'set @confirm_product_config_change_log = APPLY_PRODUCT_CONFIG_CHANGE_LOG before running this migration';
  end if;

  if coalesce(@product_config_change_log_backfill_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @product_config_change_log_backfill_expected_count after previewing exact product config update_time backfill rows';
  end if;
  if coalesce(@product_config_change_log_backfill_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @product_config_change_log_backfill_expected_signature after previewing exact product config update_time backfill rows';
  end if;
end//

drop procedure if exists assert_product_config_change_log_column_contract//
create procedure assert_product_config_change_log_column_contract()
begin
  declare v_mismatches int default 0;

  select count(1)
    into v_mismatches
  from (
    select 'log_id' expected_column, 'bigint' expected_type, null expected_length, 'NO' expected_nullable, null expected_default, 'auto_increment' expected_extra
    union all select 'biz_type', 'varchar', 32, 'NO', null, ''
    union all select 'biz_id', 'bigint', null, 'NO', null, ''
    union all select 'biz_code', 'varchar', 128, 'YES', '', ''
    union all select 'biz_name', 'varchar', 256, 'YES', '', ''
    union all select 'action_type', 'varchar', 32, 'NO', null, ''
    union all select 'action_source', 'varchar', 32, 'NO', 'PAGE', ''
    union all select 'operator_name', 'varchar', 64, 'YES', '', ''
    union all select 'change_summary', 'varchar', 1000, 'YES', '', ''
    union all select 'before_json', 'longtext', null, 'YES', null, ''
    union all select 'after_json', 'longtext', null, 'YES', null, ''
    union all select 'diff_json', 'longtext', null, 'YES', null, ''
    union all select 'change_time', 'datetime', null, 'NO', null, ''
    union all select 'remark', 'varchar', 500, 'YES', '', ''
  ) expected
  left join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = 'product_config_change_log'
   and c.column_name = expected.expected_column
  where c.column_name is null
     or lower(c.data_type) <> expected.expected_type
     or (expected.expected_length is not null
         and coalesce(c.character_maximum_length, -1) <> expected.expected_length)
     or c.is_nullable <> expected.expected_nullable
     or coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>')
     or lower(coalesce(c.extra, '')) <> expected.expected_extra;

  if v_mismatches > 0 then
    signal sqlstate '45000' set message_text = 'product_config_change_log column contract mismatch';
  end if;
end//

drop procedure if exists assert_product_config_change_log_index_definition//
create procedure assert_product_config_change_log_index_definition(
  in p_index_name varchar(128),
  in p_expected_columns varchar(512),
  in p_expected_non_unique int
)
begin
  declare v_exists int default 0;
  declare v_actual_columns varchar(512) default '';
  declare v_actual_non_unique int default -1;

  select count(distinct index_name),
         coalesce(group_concat(column_name order by seq_in_index separator ','), ''),
         coalesce(max(non_unique), -1)
    into v_exists, v_actual_columns, v_actual_non_unique
  from information_schema.statistics
  where table_schema = database()
    and table_name = 'product_config_change_log'
    and index_name = p_index_name;

  if v_exists = 0 then
    signal sqlstate '45000' set message_text = 'product_config_change_log index is missing';
  end if;
  if v_actual_columns <> p_expected_columns or v_actual_non_unique <> p_expected_non_unique then
    signal sqlstate '45000' set message_text = 'product_config_change_log index contract mismatch';
  end if;
end//

drop procedure if exists assert_product_config_change_log_index_contract//
create procedure assert_product_config_change_log_index_contract()
begin
  call assert_product_config_change_log_index_definition('PRIMARY', 'log_id', 0);
  call assert_product_config_change_log_index_definition('idx_product_config_change_log_biz',
    'biz_type,biz_id,change_time', 1);
  call assert_product_config_change_log_index_definition('idx_product_config_change_log_time',
    'change_time', 1);
  call assert_product_config_change_log_index_definition('idx_product_config_change_log_operator',
    'operator_name', 1);
end//

drop procedure if exists assert_product_config_change_log_backfill_targets//
create procedure assert_product_config_change_log_backfill_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(target order by target separator '|'), ''), 256)
    into v_count, v_signature
  from (
    select concat_ws(':',
             'product_category',
             category_id,
             coalesce(category_code, ''),
             coalesce(category_name, ''),
             coalesce(create_by, ''),
             coalesce(date_format(create_time, '%Y-%m-%d %H:%i:%s'), ''),
             coalesce(update_by, '')
           ) as target
      from product_category
     where update_time is null
       and create_time is not null
    union all
    select concat_ws(':',
             'product_attribute',
             attribute_id,
             coalesce(attribute_code, ''),
             coalesce(attribute_name, ''),
             coalesce(create_by, ''),
             coalesce(date_format(create_time, '%Y-%m-%d %H:%i:%s'), ''),
             coalesce(update_by, '')
           )
      from product_attribute
     where update_time is null
       and create_time is not null
    union all
    select concat_ws(':',
             'product_attribute_option',
             option_id,
             attribute_id,
             coalesce(option_code, ''),
             coalesce(option_label, ''),
             coalesce(create_by, ''),
             coalesce(date_format(create_time, '%Y-%m-%d %H:%i:%s'), ''),
             coalesce(update_by, '')
           )
      from product_attribute_option
     where update_time is null
       and create_time is not null
    union all
    select concat_ws(':',
             'product_category_attribute',
             category_attribute_id,
             category_id,
             attribute_id,
             coalesce(rule_mode, ''),
             coalesce(create_by, ''),
             coalesce(date_format(create_time, '%Y-%m-%d %H:%i:%s'), ''),
             coalesce(update_by, '')
           )
      from product_category_attribute
     where update_time is null
       and create_time is not null
  ) targets;

  if v_count <> cast(@product_config_change_log_backfill_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'product config update_time backfill exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@product_config_change_log_backfill_expected_signature) then
    signal sqlstate '45000' set message_text = 'product config update_time backfill exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_product_config_change_log_confirmed();

create table if not exists product_config_change_log (
  log_id          bigint(20)    not null auto_increment comment '修改记录ID',
  biz_type        varchar(32)   not null                comment '业务类型：CATEGORY/ATTRIBUTE/ATTRIBUTE_OPTION/CATEGORY_ATTRIBUTE_RULE',
  biz_id          bigint(20)    not null                comment '业务主键',
  biz_code        varchar(128)  default ''              comment '业务编码',
  biz_name        varchar(256)  default ''              comment '业务名称',
  action_type     varchar(32)   not null                comment '操作类型：CREATE/UPDATE/ENABLE/DISABLE/DELETE',
  action_source   varchar(32)   not null default 'PAGE' comment '操作来源：PAGE页面 IMPORT导入',
  operator_name   varchar(64)   default ''              comment '操作人',
  change_summary  varchar(1000) default ''              comment '变更摘要',
  before_json     longtext                              comment '修改前快照JSON',
  after_json      longtext                              comment '修改后快照JSON',
  diff_json       longtext                              comment '字段差异JSON',
  change_time     datetime      not null                comment '变更时间',
  remark          varchar(500)  default ''              comment '备注',
  primary key (log_id),
  key idx_product_config_change_log_biz (biz_type, biz_id, change_time),
  key idx_product_config_change_log_time (change_time),
  key idx_product_config_change_log_operator (operator_name)
) engine=innodb auto_increment=1 comment='商品配置修改记录表';

call assert_product_config_change_log_column_contract();
call assert_product_config_change_log_index_contract();
call assert_product_config_change_log_backfill_targets();

-- Optional historical backfill: creation is also an update for update-time semantics.
start transaction;

update product_category
set update_by = if(update_by is null or update_by = '', create_by, update_by),
    update_time = create_time
where update_time is null
  and create_time is not null;

update product_attribute
set update_by = if(update_by is null or update_by = '', create_by, update_by),
    update_time = create_time
where update_time is null
  and create_time is not null;

update product_attribute_option
set update_by = if(update_by is null or update_by = '', create_by, update_by),
    update_time = create_time
where update_time is null
  and create_time is not null;

update product_category_attribute
set update_by = if(update_by is null or update_by = '', create_by, update_by),
    update_time = create_time
where update_time is null
  and create_time is not null;

commit;

drop procedure if exists assert_product_config_change_log_confirmed;
drop procedure if exists assert_product_config_change_log_column_contract;
drop procedure if exists assert_product_config_change_log_index_definition;
drop procedure if exists assert_product_config_change_log_index_contract;
drop procedure if exists assert_product_config_change_log_backfill_targets;
