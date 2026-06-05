-- 商城商品销售状态、管控状态、销售价与操作日志迁移脚本
-- 确认来源：docs/plans/2026-06-05-product-distribution-status-price-log-schema-design.md

alter table product_spu
    add column control_status varchar(32) not null default 'NORMAL' comment 'SPU管控状态：NORMAL正常，DISABLED停用' after spu_status,
    add column control_reason varchar(500) null comment '最近一次停用原因' after control_status,
    add column control_by varchar(64) null comment '最近一次停用操作人' after control_reason,
    add column control_time datetime null comment '最近一次停用时间' after control_by,
    add column recover_by varchar(64) null comment '最近一次恢复操作人' after control_time,
    add column recover_time datetime null comment '最近一次恢复时间' after recover_by;

alter table product_sku
    add column control_status varchar(32) not null default 'NORMAL' comment 'SKU管控状态：NORMAL正常，DISABLED停用' after sku_status,
    add column control_reason varchar(500) null comment '最近一次停用原因' after control_status,
    add column control_by varchar(64) null comment '最近一次停用操作人' after control_reason,
    add column control_time datetime null comment '最近一次停用时间' after control_by,
    add column recover_by varchar(64) null comment '最近一次恢复操作人' after control_time,
    add column recover_time datetime null comment '最近一次恢复时间' after recover_by,
    modify column sale_price decimal(18,4) null comment '销售价';

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

create index idx_product_spu_control_status on product_spu(control_status);
create index idx_product_spu_status_control on product_spu(spu_status, control_status);
create index idx_product_sku_control_status on product_sku(control_status);
create index idx_product_sku_status_control on product_sku(sku_status, control_status);

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

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_id, seed.menu_name, 2402, seed.order_num, '#', '', '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select 2485 as menu_id, '商城商品调价' as menu_name, 5 as order_num, 'product:distribution:price' as perms, '商城商品按钮：调整销售价' as remark
    union all select 2486, '商城商品操作日志', 6, 'product:distribution:log', '商城商品按钮：操作日志'
) seed
where not exists (select 1 from sys_menu m where m.menu_id = seed.menu_id)
  and not exists (select 1 from sys_menu p where p.perms = seed.perms);
