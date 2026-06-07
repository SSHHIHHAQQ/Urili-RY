-- Product SKU source binding migration.
-- Purpose:
-- 1. Keep the product-side source SKU binding as the primary fact.
-- 2. Enforce one ACTIVE official source SKU group to one mall SKU in the first version.
-- 3. Depend on fulfillment warehouse pairing so official warehouse scope is derived, not manually selected.

set names utf8mb4;
set @confirm_product_sku_source_binding := coalesce(@confirm_product_sku_source_binding, '');

delimiter //

drop procedure if exists assert_product_sku_source_binding_confirmed//
create procedure assert_product_sku_source_binding_confirmed()
begin
  if coalesce(@confirm_product_sku_source_binding, '')
      <> 'APPLY_PRODUCT_SKU_SOURCE_BINDING' then
    signal sqlstate '45000' set message_text = 'set @confirm_product_sku_source_binding = APPLY_PRODUCT_SKU_SOURCE_BINDING before running this migration';
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

delimiter ;

call assert_product_sku_source_binding_confirmed();

call assert_table_exists('product_spu', 'product_spu is required before product source binding');
call assert_table_exists('product_sku', 'product_sku is required before product source binding');
call assert_table_exists('seller', 'seller is required before product source binding');
call assert_table_exists('warehouse', 'warehouse is required before product source binding');
call assert_table_exists('source_product_dimension_group', 'source_product_dimension_group is required before product source binding');
call assert_table_exists('source_product_warehouse_detail', 'source_product_warehouse_detail is required before product source binding');
call assert_table_exists('upstream_system_sku_pairing', 'upstream_system_sku_pairing is required before product source binding');
call assert_table_exists('upstream_system_warehouse_pairing', 'upstream_system_warehouse_pairing is required before product source binding');

call assert_column_exists('upstream_system_warehouse_pairing', 'pairing_role',
  'run 20260607_upstream_pairing_role_binding.sql before product source binding');
call assert_column_exists('warehouse', 'warehouse_kind', 'warehouse.warehouse_kind is required before product source binding');
call assert_column_exists('warehouse', 'settlement_currency', 'warehouse.settlement_currency is required before product source binding');

drop procedure if exists assert_product_sku_source_binding_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists assert_column_exists;

create table if not exists product_sku_source_binding (
  binding_id                   bigint(20)    not null auto_increment comment '绑定主键',
  spu_id                       bigint(20)    not null                comment '商城SPU ID',
  sku_id                       bigint(20)    not null                comment '商城SKU ID',
  seller_id                    bigint(20)    not null                comment '绑定卖家ID快照',
  system_sku_code              varchar(64)   not null                comment '系统SKU编码快照',
  source_scope                 varchar(32)   not null default 'OFFICIAL_MASTER' comment '来源范围',
  source_sku_group_key         varchar(96)   not null                comment '来源SKU组key，占用判断使用',
  source_dimension_group_key   varchar(160)  not null                comment '来源SKU尺寸组key，尺寸重量和仓库范围推导使用',
  master_sku                   varchar(128)  not null                comment '来源SKU快照',
  master_product_name_snapshot varchar(255)  not null                comment '来源商品名快照',
  system_sku_name_snapshot     varchar(255)  default ''              comment '系统SKU名称投影快照',
  seller_name_snapshot         varchar(200)  default ''              comment '卖家名称快照',
  source_payload_hash          varchar(64)   default ''              comment '来源商品快照hash',
  wms_payload_hash             varchar(64)   default ''              comment 'WMS尺寸快照hash',
  measure_length_cm            decimal(18,4)                         comment '最终采用长度cm',
  measure_width_cm             decimal(18,4)                         comment '最终采用宽度cm',
  measure_height_cm            decimal(18,4)                         comment '最终采用高度cm',
  measure_weight_kg            decimal(18,4)                         comment '最终采用重量kg',
  measure_source               varchar(32)   not null default 'PRODUCT' comment '尺寸来源：WMS/PRODUCT',
  currency_code                varchar(16)   not null                comment '官方仓派生币种',
  source_warehouse_names       varchar(1000) default ''              comment '来源仓库名称快照',
  source_warehouse_count       int           not null default 0       comment '来源仓库数量快照',
  binding_status               varchar(32)   not null default 'ACTIVE' comment '绑定状态：ACTIVE/REPLACED/RELEASED',
  lock_status                  varchar(32)   not null default 'UNLOCKED' comment '锁定状态：UNLOCKED/LOCKED',
  locked_time                  datetime                              comment '锁定时间',
  locked_by                    varchar(64)   default ''              comment '锁定操作人',
  release_reason               varchar(500)  default ''              comment '释放原因',
  replace_reason               varchar(500)  default ''              comment '换绑原因',
  active_sku_key               bigint(20)                             comment 'ACTIVE状态唯一SKU键',
  active_source_key            varchar(96)                            comment 'ACTIVE状态唯一来源SKU组键',
  create_by                    varchar(64)   default ''              comment '创建者',
  create_time                  datetime                              comment '创建时间',
  update_by                    varchar(64)   default ''              comment '更新者',
  update_time                  datetime                              comment '更新时间',
  remark                       varchar(500)  default ''              comment '备注',
  primary key (binding_id),
  unique key uk_product_sku_source_active_sku (active_sku_key),
  unique key uk_product_sku_source_active_source (active_source_key),
  key idx_product_sku_source_spu (spu_id),
  key idx_product_sku_source_sku (sku_id),
  key idx_product_sku_source_seller (seller_id),
  key idx_product_sku_source_dimension (source_dimension_group_key),
  key idx_product_sku_source_status (binding_status, lock_status),
  constraint fk_product_sku_source_spu foreign key (spu_id) references product_spu (spu_id),
  constraint fk_product_sku_source_sku foreign key (sku_id) references product_sku (sku_id),
  constraint fk_product_sku_source_seller foreign key (seller_id) references seller (seller_id)
) engine=innodb comment='商城SKU来源绑定主事实表';
