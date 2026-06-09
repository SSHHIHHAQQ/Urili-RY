-- 商城商品销售状态、管控状态、销售价与操作日志迁移脚本
-- 确认来源：docs/plans/2026-06-05-product-distribution-status-price-log-schema-design.md

set names utf8mb4;

set @confirm_product_distribution_status_price_log := coalesce(@confirm_product_distribution_status_price_log, '');
set @product_distribution_status_price_log_spu_disabled_expected_count :=
    coalesce(@product_distribution_status_price_log_spu_disabled_expected_count, '');
set @product_distribution_status_price_log_spu_disabled_expected_signature :=
    coalesce(@product_distribution_status_price_log_spu_disabled_expected_signature, '');
set @product_distribution_status_price_log_sku_disabled_expected_count :=
    coalesce(@product_distribution_status_price_log_sku_disabled_expected_count, '');
set @product_distribution_status_price_log_sku_disabled_expected_signature :=
    coalesce(@product_distribution_status_price_log_sku_disabled_expected_signature, '');

delimiter //

drop procedure if exists assert_product_distribution_status_price_log_confirmed//
create procedure assert_product_distribution_status_price_log_confirmed()
begin
  if coalesce(@confirm_product_distribution_status_price_log, '')
      <> 'APPLY_PRODUCT_DISTRIBUTION_STATUS_PRICE_LOG' then
    signal sqlstate '45000' set message_text = 'set @confirm_product_distribution_status_price_log = APPLY_PRODUCT_DISTRIBUTION_STATUS_PRICE_LOG before running this migration';
  end if;

  if coalesce(@product_distribution_status_price_log_spu_disabled_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @product_distribution_status_price_log_spu_disabled_expected_count after previewing exact product_spu DISABLED rows';
  end if;
  if coalesce(@product_distribution_status_price_log_spu_disabled_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @product_distribution_status_price_log_spu_disabled_expected_signature after previewing exact product_spu DISABLED rows';
  end if;
  if coalesce(@product_distribution_status_price_log_sku_disabled_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @product_distribution_status_price_log_sku_disabled_expected_count after previewing exact product_sku DISABLED rows';
  end if;
  if coalesce(@product_distribution_status_price_log_sku_disabled_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @product_distribution_status_price_log_sku_disabled_expected_signature after previewing exact product_sku DISABLED rows';
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

drop procedure if exists modify_product_sku_sale_price_if_needed//
create procedure modify_product_sku_sale_price_if_needed()
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = 'product_sku'
      and column_name = 'sale_price'
  ) then
    signal sqlstate '45000' set message_text = 'product_sku.sale_price column is required before product distribution status price migration';
  end if;

  if exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = 'product_sku'
      and column_name = 'sale_price'
      and (
        data_type <> 'decimal'
        or numeric_precision <> 18
        or numeric_scale <> 4
        or is_nullable <> 'YES'
        or column_comment <> '销售价'
      )
  ) then
    alter table product_sku modify column sale_price decimal(18,4) null comment '销售价';
  end if;
end//

drop procedure if exists create_index_if_missing//
create procedure create_index_if_missing(in p_table varchar(64), in p_index varchar(64), in p_definition text)
begin
  if not exists (
    select 1
    from information_schema.statistics
    where table_schema = database()
      and table_name = p_table
      and index_name = p_index
  ) then
    set @ddl = concat('create index ', p_index, ' on ', p_table, ' ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_product_distribution_status_price_log_expected_targets//
create procedure assert_product_distribution_status_price_log_expected_targets()
begin
  declare v_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             spu_id,
             coalesce(spu_status, ''),
             coalesce(control_status, ''),
             coalesce(control_reason, '')
           )
           order by spu_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from product_spu
  where spu_status = 'DISABLED';

  if v_count <> cast(@product_distribution_status_price_log_spu_disabled_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'product_spu DISABLED exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@product_distribution_status_price_log_spu_disabled_expected_signature) then
    signal sqlstate '45000' set message_text = 'product_spu DISABLED exact target signature mismatch';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             sku_id,
             coalesce(sku_status, ''),
             coalesce(control_status, ''),
             coalesce(control_reason, '')
           )
           order by sku_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from product_sku
  where sku_status = 'DISABLED';

  if v_count <> cast(@product_distribution_status_price_log_sku_disabled_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'product_sku DISABLED exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@product_distribution_status_price_log_sku_disabled_expected_signature) then
    signal sqlstate '45000' set message_text = 'product_sku DISABLED exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_product_distribution_status_price_log_confirmed();
drop procedure if exists assert_product_distribution_status_price_log_confirmed;

call add_column_if_missing('product_spu', 'control_status',
  'varchar(32) not null default ''NORMAL'' comment ''SPU管控状态：NORMAL正常，DISABLED停用'' after spu_status');
call add_column_if_missing('product_spu', 'control_reason',
  'varchar(500) null comment ''最近一次停用原因'' after control_status');
call add_column_if_missing('product_spu', 'control_by',
  'varchar(64) null comment ''最近一次停用操作人'' after control_reason');
call add_column_if_missing('product_spu', 'control_time',
  'datetime null comment ''最近一次停用时间'' after control_by');
call add_column_if_missing('product_spu', 'recover_by',
  'varchar(64) null comment ''最近一次恢复操作人'' after control_time');
call add_column_if_missing('product_spu', 'recover_time',
  'datetime null comment ''最近一次恢复时间'' after recover_by');

call add_column_if_missing('product_sku', 'control_status',
  'varchar(32) not null default ''NORMAL'' comment ''SKU管控状态：NORMAL正常，DISABLED停用'' after sku_status');
call add_column_if_missing('product_sku', 'control_reason',
  'varchar(500) null comment ''最近一次停用原因'' after control_status');
call add_column_if_missing('product_sku', 'control_by',
  'varchar(64) null comment ''最近一次停用操作人'' after control_reason');
call add_column_if_missing('product_sku', 'control_time',
  'datetime null comment ''最近一次停用时间'' after control_by');
call add_column_if_missing('product_sku', 'recover_by',
  'varchar(64) null comment ''最近一次恢复操作人'' after control_time');
call add_column_if_missing('product_sku', 'recover_time',
  'datetime null comment ''最近一次恢复时间'' after recover_by');
call modify_product_sku_sale_price_if_needed();
call assert_product_distribution_status_price_log_expected_targets();

update product_spu
set control_status = 'DISABLED',
    control_reason = coalesce(nullif(control_reason, ''), '由历史销售状态DISABLED迁移'),
    spu_status = 'OFF_SALE'
where spu_status = 'DISABLED';

update product_sku
set control_status = 'DISABLED',
    control_reason = coalesce(nullif(control_reason, ''), '由历史销售状态DISABLED迁移'),
    sku_status = 'OFF_SALE'
where sku_status = 'DISABLED';

call create_index_if_missing('product_spu', 'idx_product_spu_control_status', '(control_status)');
call create_index_if_missing('product_spu', 'idx_product_spu_status_control', '(spu_status, control_status)');
call create_index_if_missing('product_sku', 'idx_product_sku_control_status', '(control_status)');
call create_index_if_missing('product_sku', 'idx_product_sku_status_control', '(sku_status, control_status)');

create table if not exists product_distribution_operation_log (
  log_id bigint not null auto_increment comment '日志ID',
  batch_no varchar(64) not null comment '批量操作批次号',
  operation_type varchar(32) not null comment '操作类型',
  owner_type varchar(16) not null comment '对象类型：SPU/SKU',
  spu_id bigint not null comment 'SPU ID',
  sku_id bigint null comment 'SKU ID',
  system_spu_code varchar(64) null comment '系统SPU编码快照',
  system_sku_code varchar(64) null comment '系统SKU编码快照',
  seller_id bigint null comment '卖家ID快照',
  seller_name varchar(128) null comment '卖家名称快照',
  before_sales_status varchar(32) null comment '操作前销售状态',
  after_sales_status varchar(32) null comment '操作后销售状态',
  before_control_status varchar(32) null comment '操作前管控状态',
  after_control_status varchar(32) null comment '操作后管控状态',
  before_sale_price decimal(18,4) null comment '操作前销售价',
  after_sale_price decimal(18,4) null comment '操作后销售价',
  currency_code varchar(16) null comment '币种快照',
  reason varchar(500) null comment '操作原因',
  change_summary varchar(500) null comment '操作摘要',
  diff_json longtext null comment '字段差异JSON',
  operator_name varchar(64) not null comment '操作人账号',
  operation_time datetime not null default current_timestamp comment '操作时间',
  operation_source varchar(32) not null default 'PAGE' comment '操作来源',
  remark varchar(500) null comment '备注',
  primary key (log_id),
  key idx_product_dist_log_batch (batch_no),
  key idx_product_dist_log_spu (spu_id, operation_time),
  key idx_product_dist_log_sku (sku_id, operation_time),
  key idx_product_dist_log_type (operation_type, operation_time),
  key idx_product_dist_log_operator (operator_name, operation_time)
) comment='商城商品业务操作日志';

update sys_dict_data
set status = '1',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '停用已拆分为商品管控状态，不再作为销售状态'
where dict_type = 'product_sales_status'
  and dict_value = 'DISABLED';

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '商品管控状态', 'product_control_status', '0', 'admin', sysdate(), '', null, '商城商品SPU/SKU独立管控状态'
where not exists (select 1 from sys_dict_type where dict_type = 'product_control_status');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'product_control_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '商品管控状态'
from (
    select 1 as dict_sort, '正常' as dict_label, 'NORMAL' as dict_value, 'success' as list_class, 'Y' as is_default
    union all select 2, '停用', 'DISABLED', 'danger', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'product_control_status' and d.dict_value = seed.dict_value);

drop procedure if exists create_index_if_missing;
drop procedure if exists assert_product_distribution_status_price_log_expected_targets;
drop procedure if exists modify_product_sku_sale_price_if_needed;
drop procedure if exists add_column_if_missing;
