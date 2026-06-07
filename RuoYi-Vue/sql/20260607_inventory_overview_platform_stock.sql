-- Inventory overview platform stock model.
-- Purpose:
-- 1. Store platform sellable stock by SKU + warehouse row.
-- 2. Keep reservation, adjustment ledger, source deduction pending and in-transit tracking auditable.
-- 3. Materialize SPU/SKU inventory overview read models for the admin inventory overview page.
-- 4. Move menu 2420 from placeholder to the real Inventory/Overview page.

set names utf8mb4;
set @confirm_inventory_overview_platform_stock := coalesce(@confirm_inventory_overview_platform_stock, '');

delimiter //

drop procedure if exists assert_inventory_overview_platform_stock_confirmed//
create procedure assert_inventory_overview_platform_stock_confirmed()
begin
  if coalesce(@confirm_inventory_overview_platform_stock, '')
      <> 'APPLY_INVENTORY_OVERVIEW_PLATFORM_STOCK' then
    signal sqlstate '45000' set message_text = 'set @confirm_inventory_overview_platform_stock = APPLY_INVENTORY_OVERVIEW_PLATFORM_STOCK before running this migration';
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

drop procedure if exists assert_inventory_overview_sys_menu_guard//
create procedure assert_inventory_overview_sys_menu_guard()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2080
      and parent_id = 0
      and menu_type = 'M'
      and path = 'inventory'
      and route_name = 'InventoryManagement'
  ) then
    signal sqlstate '45000' set message_text = 'inventory overview requires inventory top menu 2080 from top_menu_seed.sql';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_inventory_overview_sys_menu_guard_ids seed_id
      on seed_id.menu_id = m.menu_id
    left join tmp_inventory_overview_sys_menu_guard seed
      on seed.menu_id = m.menu_id
     and coalesce(m.parent_id, -1) = seed.parent_id
     and coalesce(m.menu_type, '') = seed.menu_type
     and coalesce(m.path, '') = seed.path
     and coalesce(m.component, '') = seed.component
     and coalesce(m.route_name, '') = seed.route_name
     and coalesce(m.perms, '') = seed.perms
    where seed.menu_id is null
  ) then
    signal sqlstate '45000' set message_text = 'inventory overview sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_inventory_overview_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = seed.path
     and coalesce(m.component, '') = seed.component
     and coalesce(m.route_name, '') = seed.route_name
     and coalesce(m.perms, '') = seed.perms
  ) then
    signal sqlstate '45000' set message_text = 'inventory overview sys_menu signature is already used by another menu';
  end if;
end//

delimiter ;

call assert_inventory_overview_platform_stock_confirmed();

call assert_table_exists('product_spu', 'product_spu is required before inventory overview');
call assert_table_exists('product_sku', 'product_sku is required before inventory overview');
call assert_table_exists('product_spu_warehouse', 'product_spu_warehouse is required before inventory overview');
call assert_table_exists('product_sku_source_binding', 'product_sku_source_binding is required before inventory overview');
call assert_table_exists('source_warehouse_stock_detail', 'source_warehouse_stock_detail is required before inventory overview');
call assert_table_exists('warehouse', 'warehouse is required before inventory overview');

call assert_column_exists('source_warehouse_stock_detail', 'master_warehouse_name',
  'source_warehouse_stock_detail.master_warehouse_name is required before inventory overview');
call assert_column_exists('source_warehouse_stock_detail', 'inventory_scope',
  'source_warehouse_stock_detail.inventory_scope is required before inventory overview');
call assert_column_exists('source_warehouse_stock_detail', 'inventory_attribute',
  'source_warehouse_stock_detail.inventory_attribute is required before inventory overview');
call assert_column_exists('source_warehouse_stock_detail', 'available_quantity',
  'source_warehouse_stock_detail.available_quantity is required before inventory overview');
call assert_column_exists('source_warehouse_stock_detail', 'in_transit_quantity',
  'source_warehouse_stock_detail.in_transit_quantity is required before inventory overview');

create temporary table if not exists tmp_inventory_overview_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_inventory_overview_sys_menu_guard_id (menu_id)
) engine=memory;

create temporary table if not exists tmp_inventory_overview_sys_menu_guard_ids (
  menu_id bigint not null,
  primary key (menu_id)
) engine=memory;

truncate table tmp_inventory_overview_sys_menu_guard;
truncate table tmp_inventory_overview_sys_menu_guard_ids;

insert into tmp_inventory_overview_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2420, 2080, 'C', 'overview', 'Inventory/Overview/index', 'InventoryOverview', 'inventory:overview:list'),
    (2420, 2080, 'C', 'overview', 'Common/PlannedPage/index', 'InventoryOverview', 'inventory:overview:list'),
    (242001, 2420, 'F', '#', '', '', 'inventory:overview:query'),
    (242002, 2420, 'F', '#', '', '', 'inventory:overview:adjust'),
    (242003, 2420, 'F', '#', '', '', 'inventory:overview:ledger'),
    (242004, 2420, 'F', '#', '', '', 'inventory:overview:export');

insert ignore into tmp_inventory_overview_sys_menu_guard_ids(menu_id)
select distinct menu_id
from tmp_inventory_overview_sys_menu_guard;

call assert_inventory_overview_sys_menu_guard();

drop procedure if exists assert_inventory_overview_platform_stock_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists assert_column_exists;

create table if not exists inventory_sku_warehouse_stock (
  stock_id bigint(20) not null auto_increment comment '库存行主键',
  stock_key varchar(128) not null comment '稳定库存行key',
  spu_id bigint(20) not null comment '商城SPU ID',
  sku_id bigint(20) not null comment '商城SKU ID',
  seller_id bigint(20) not null comment '卖家ID快照',
  system_sku_code varchar(64) not null comment '系统SKU编码快照',
  warehouse_kind varchar(32) not null comment '仓库类型：official/third_party',
  warehouse_ref_type varchar(32) not null comment '仓库引用类型：OFFICIAL_MASTER/THIRD_PARTY_WAREHOUSE/UNMATCHED_OFFICIAL',
  warehouse_id bigint(20) default null comment '三方仓warehouse_id；官方来源主仓为空',
  warehouse_code varchar(64) not null default '' comment '三方仓编码或官方占位编码',
  warehouse_name varchar(200) not null default '' comment '页面展示仓库名',
  source_scope varchar(32) not null default 'OFFICIAL_MASTER' comment '来源范围',
  source_master_warehouse_name varchar(128) not null default '' comment '来源主仓名',
  source_inventory_scope varchar(32) not null default 'COMPREHENSIVE' comment '来源库存口径',
  source_inventory_attribute varchar(64) not null default '0' comment '来源库存属性，第一版固定正品',
  source_total_qty bigint(20) not null default 0 comment '来源总库存',
  source_available_qty bigint(20) not null default 0 comment '来源可用库存',
  source_in_transit_qty bigint(20) not null default 0 comment '来源在途库存',
  source_snapshot_time datetime default null comment '来源库存最新更新时间',
  platform_total_qty bigint(20) not null default 0 comment '平台总库存',
  platform_reserved_qty bigint(20) not null default 0 comment '平台锁定库存',
  platform_in_transit_qty bigint(20) not null default 0 comment '平台在途库存',
  pending_available_inbound_qty bigint(20) not null default 0 comment '来源在途已减少但来源可用尚未增加的内部观察数量',
  pending_source_deduction_qty bigint(20) not null default 0 comment '官方仓出库已完成但来源库存尚未同步扣减的校准数量',
  platform_available_qty bigint(20) not null default 0 comment '平台可售库存',
  effective_status varchar(32) not null default 'ACTIVE' comment '库存状态',
  version int not null default 0 comment '乐观锁版本',
  calc_time datetime default null comment '最近计算时间',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default '' comment '备注',
  primary key (stock_id),
  unique key uk_inventory_sku_warehouse_stock_key (stock_key),
  key idx_inventory_stock_sku (sku_id, warehouse_kind),
  key idx_inventory_stock_spu (spu_id),
  key idx_inventory_stock_seller (seller_id),
  key idx_inventory_stock_source_wh (source_scope, source_master_warehouse_name),
  key idx_inventory_stock_status (effective_status)
) engine=innodb default charset=utf8mb4 comment='SKU仓库平台库存当前表';

create table if not exists inventory_stock_ledger (
  ledger_id bigint(20) not null auto_increment comment '流水主键',
  stock_id bigint(20) not null comment '库存行ID',
  stock_key varchar(128) not null comment '库存行key快照',
  spu_id bigint(20) not null comment 'SPU ID快照',
  sku_id bigint(20) not null comment 'SKU ID快照',
  seller_id bigint(20) not null comment '卖家ID快照',
  warehouse_kind varchar(32) not null comment '仓库类型快照',
  warehouse_ref_type varchar(32) not null comment '仓库引用类型快照',
  warehouse_name varchar(200) not null default '' comment '仓库展示名快照',
  operation_type varchar(64) not null comment '操作类型code',
  operation_source varchar(32) not null comment '操作来源：ADMIN/ORDER/SYSTEM_SYNC',
  biz_type varchar(64) not null default '' comment '业务类型',
  biz_no varchar(128) not null default '' comment '业务单号',
  delta_qty bigint(20) not null default 0 comment '本次影响数量',
  before_platform_total_qty bigint(20) not null default 0 comment '调整前平台总库存',
  after_platform_total_qty bigint(20) not null default 0 comment '调整后平台总库存',
  before_available_qty bigint(20) not null default 0 comment '调整前平台可售',
  after_available_qty bigint(20) not null default 0 comment '调整后平台可售',
  before_reserved_qty bigint(20) not null default 0 comment '调整前平台锁定',
  after_reserved_qty bigint(20) not null default 0 comment '调整后平台锁定',
  before_in_transit_qty bigint(20) not null default 0 comment '调整前平台在途',
  after_in_transit_qty bigint(20) not null default 0 comment '调整后平台在途',
  risk_confirmed char(1) not null default 'N' comment '是否经过风险二次确认',
  risk_message varchar(1000) not null default '' comment '二次确认提示文案快照',
  reason varchar(500) not null default '' comment '调整原因',
  operator_id bigint(20) default null comment '操作人ID',
  operator_name varchar(64) not null default '' comment '操作人名称',
  operate_time datetime not null comment '操作时间',
  create_time datetime default null comment '创建时间',
  remark varchar(500) default '' comment '备注',
  primary key (ledger_id),
  key idx_inventory_ledger_stock (stock_id, operate_time),
  key idx_inventory_ledger_sku (sku_id, operate_time),
  key idx_inventory_ledger_biz (biz_type, biz_no),
  key idx_inventory_ledger_operator (operator_id, operate_time)
) engine=innodb default charset=utf8mb4 comment='库存流水表';

create table if not exists inventory_reservation (
  reservation_id bigint(20) not null auto_increment comment '锁定主键',
  reservation_no varchar(64) not null comment '锁定单号',
  stock_id bigint(20) not null comment '库存行ID',
  stock_key varchar(128) not null comment '库存行key快照',
  spu_id bigint(20) not null comment 'SPU ID快照',
  sku_id bigint(20) not null comment 'SKU ID快照',
  seller_id bigint(20) not null comment '卖家ID快照',
  order_no varchar(128) not null default '' comment '平台订单号',
  order_item_no varchar(128) not null default '' comment '平台订单明细号',
  reserved_qty bigint(20) not null default 0 comment '锁定数量',
  released_qty bigint(20) not null default 0 comment '已释放数量',
  consumed_qty bigint(20) not null default 0 comment '已出库消耗数量',
  status varchar(32) not null default 'RESERVED' comment '锁定状态',
  reserve_time datetime not null comment '锁定时间',
  release_time datetime default null comment '释放时间',
  consume_time datetime default null comment '出库消耗时间',
  expire_time datetime default null comment '锁定过期时间',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default '' comment '备注',
  primary key (reservation_id),
  unique key uk_inventory_reservation_no (reservation_no),
  key idx_inventory_reservation_stock (stock_id, status),
  key idx_inventory_reservation_order (order_no, order_item_no),
  key idx_inventory_reservation_status (status, reserve_time)
) engine=innodb default charset=utf8mb4 comment='平台库存锁定表';

create table if not exists inventory_source_deduction_pending (
  pending_id bigint(20) not null auto_increment comment '来源校准主键',
  stock_id bigint(20) not null comment '库存行ID',
  stock_key varchar(128) not null comment '库存行key快照',
  sku_id bigint(20) not null comment 'SKU ID快照',
  source_master_warehouse_name varchar(128) not null comment '来源主仓名',
  outbound_biz_no varchar(128) not null comment '出库业务单号',
  pending_qty bigint(20) not null default 0 comment '初始待校准数量',
  covered_qty bigint(20) not null default 0 comment '已被来源快照覆盖数量',
  remaining_qty bigint(20) not null default 0 comment '剩余待校准数量',
  baseline_source_available_qty bigint(20) not null default 0 comment '出库时来源可用库存基线',
  baseline_source_total_qty bigint(20) not null default 0 comment '出库时来源总库存基线',
  baseline_source_snapshot_time datetime default null comment '出库时来源快照时间',
  status varchar(32) not null default 'PENDING' comment '校准状态',
  cover_time datetime default null comment '完全覆盖时间',
  create_time datetime default null comment '创建时间',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default '' comment '备注',
  primary key (pending_id),
  key idx_inventory_source_deduction_stock (stock_id, status),
  key idx_inventory_source_deduction_sku_wh (sku_id, source_master_warehouse_name, status),
  key idx_inventory_source_deduction_biz (outbound_biz_no)
) engine=innodb default charset=utf8mb4 comment='官方仓来源同步延迟校准表';

create table if not exists inventory_in_transit_tracking (
  tracking_id bigint(20) not null auto_increment comment '在途跟踪主键',
  tracking_no varchar(64) not null comment '在途跟踪单号',
  stock_id bigint(20) not null comment '库存行ID',
  stock_key varchar(128) not null comment '库存行key快照',
  sku_id bigint(20) not null comment 'SKU ID快照',
  source_master_warehouse_name varchar(128) not null comment '来源主仓名',
  configured_qty bigint(20) not null default 0 comment '用户配置的平台在途数量',
  released_qty bigint(20) not null default 0 comment '已释放到平台总库存的数量',
  pending_available_qty bigint(20) not null default 0 comment '来源在途已减少、等待来源可用增加的数量',
  remaining_qty bigint(20) not null default 0 comment '剩余未释放数量',
  baseline_source_in_transit_qty bigint(20) not null default 0 comment '配置时来源在途基线',
  baseline_source_available_qty bigint(20) not null default 0 comment '配置时来源可用基线',
  last_source_in_transit_qty bigint(20) not null default 0 comment '上次观察到的来源在途',
  last_source_available_qty bigint(20) not null default 0 comment '上次观察到的来源可用',
  last_source_snapshot_time datetime default null comment '上次来源快照时间',
  status varchar(32) not null default 'ACTIVE' comment '跟踪状态',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default '' comment '备注',
  primary key (tracking_id),
  unique key uk_inventory_in_transit_tracking_no (tracking_no),
  key idx_inventory_in_transit_stock (stock_id, status),
  key idx_inventory_in_transit_sku_wh (sku_id, source_master_warehouse_name, status)
) engine=innodb default charset=utf8mb4 comment='平台在途库存跟踪表';

create table if not exists inventory_overview_sku_read_model (
  sku_stock_key varchar(128) not null comment 'SKU库存读模型key',
  spu_id bigint(20) not null comment 'SPU ID',
  sku_id bigint(20) not null comment 'SKU ID',
  seller_id bigint(20) not null comment '卖家ID',
  system_sku_code varchar(64) not null comment '系统SKU编码',
  product_name varchar(255) not null default '' comment '商品名',
  sku_name varchar(255) not null default '' comment 'SKU名',
  sku_image_url varchar(1000) not null default '' comment 'SKU图片',
  warehouse_kind_summary varchar(32) not null default '' comment '仓库类型摘要',
  warehouse_count int not null default 0 comment '仓库明细行数量',
  platform_total_qty bigint(20) not null default 0 comment '平台总库存汇总',
  platform_available_qty bigint(20) not null default 0 comment '平台可售库存汇总',
  platform_reserved_qty bigint(20) not null default 0 comment '平台锁定库存汇总',
  platform_in_transit_qty bigint(20) not null default 0 comment '平台在途库存汇总',
  source_total_qty bigint(20) not null default 0 comment '官方仓来源总库存汇总',
  source_available_qty bigint(20) not null default 0 comment '官方仓来源可用库存汇总',
  source_in_transit_qty bigint(20) not null default 0 comment '官方仓来源在途库存汇总',
  inventory_status varchar(32) not null default 'OUT_OF_STOCK' comment '库存状态',
  latest_source_snapshot_time datetime default null comment '最新来源快照时间',
  latest_stock_update_time datetime default null comment '最新平台库存更新时间',
  search_text text not null comment '搜索文本',
  rebuild_time datetime not null comment '读模型构建时间',
  primary key (sku_stock_key),
  unique key uk_inventory_overview_sku_id (sku_id),
  key idx_inventory_overview_sku_spu (spu_id),
  key idx_inventory_overview_sku_seller (seller_id),
  key idx_inventory_overview_sku_status (inventory_status),
  key idx_inventory_overview_sku_list (latest_stock_update_time, sku_id)
) engine=innodb default charset=utf8mb4 comment='库存总览SKU读模型';

create table if not exists inventory_overview_spu_read_model (
  spu_stock_key varchar(128) not null comment 'SPU库存读模型key',
  spu_id bigint(20) not null comment 'SPU ID',
  seller_id bigint(20) not null comment '卖家ID',
  system_spu_code varchar(64) not null comment '系统SPU编码',
  product_name varchar(255) not null default '' comment '商品名',
  main_image_url varchar(1000) not null default '' comment 'SPU主图',
  sku_count int not null default 0 comment 'SKU数量',
  warehouse_kind_summary varchar(32) not null default '' comment '仓库类型摘要',
  warehouse_count int not null default 0 comment '仓库明细行数量',
  platform_total_qty bigint(20) not null default 0 comment '平台总库存汇总',
  platform_available_qty bigint(20) not null default 0 comment '平台可售库存汇总',
  platform_reserved_qty bigint(20) not null default 0 comment '平台锁定库存汇总',
  platform_in_transit_qty bigint(20) not null default 0 comment '平台在途库存汇总',
  source_total_qty bigint(20) not null default 0 comment '官方仓来源总库存汇总',
  source_available_qty bigint(20) not null default 0 comment '官方仓来源可用库存汇总',
  source_in_transit_qty bigint(20) not null default 0 comment '官方仓来源在途库存汇总',
  inventory_status varchar(32) not null default 'OUT_OF_STOCK' comment '库存状态',
  latest_source_snapshot_time datetime default null comment '最新来源快照时间',
  latest_stock_update_time datetime default null comment '最新平台库存更新时间',
  search_text text not null comment '搜索文本',
  rebuild_time datetime not null comment '读模型构建时间',
  primary key (spu_stock_key),
  unique key uk_inventory_overview_spu_id (spu_id),
  key idx_inventory_overview_spu_seller (seller_id),
  key idx_inventory_overview_spu_status (inventory_status),
  key idx_inventory_overview_spu_list (latest_stock_update_time, spu_id)
) engine=innodb default charset=utf8mb4 comment='库存总览SPU读模型';

insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '库存状态', 'inventory_status', '0', 'admin', sysdate(), '库存总览库存状态'
where not exists (select 1 from sys_dict_type where dict_type = 'inventory_status');

insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'inventory_status', '', seed.list_class, 'N', '0', 'admin', sysdate(), '库存总览库存状态'
from (
    select 1 dict_sort, '有货' dict_label, 'IN_STOCK' dict_value, 'success' list_class
    union all select 2, '缺货', 'OUT_OF_STOCK', 'default'
    union all select 3, '无来源库存', 'NO_SOURCE', 'warning'
    union all select 4, '仅来源在途', 'SOURCE_ONLY_IN_TRANSIT', 'processing'
    union all select 5, '停用', 'DISABLED', 'error'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'inventory_status' and d.dict_value = seed.dict_value
);

insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '库存操作类型', 'inventory_operation_type', '0', 'admin', sysdate(), '库存流水操作类型'
where not exists (select 1 from sys_dict_type where dict_type = 'inventory_operation_type');

insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'inventory_operation_type', '', '', 'N', '0', 'admin', sysdate(), '库存流水操作类型'
from (
    select 1 dict_sort, '手工增加库存' dict_label, 'MANUAL_INCREASE' dict_value
    union all select 2, '手工减少库存', 'MANUAL_DECREASE'
    union all select 3, '订单锁定', 'ORDER_RESERVE'
    union all select 4, '订单释放', 'ORDER_RELEASE'
    union all select 5, '出库扣减', 'OUTBOUND_DEDUCT'
    union all select 6, '来源校准扣减', 'SOURCE_DEDUCTION_PENDING'
    union all select 7, '在途配置', 'IN_TRANSIT_CONFIG'
    union all select 8, '在途释放', 'IN_TRANSIT_RELEASE'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'inventory_operation_type' and d.dict_value = seed.dict_value
);

update sys_menu
set menu_name = '库存总览',
    path = 'overview',
    component = 'Inventory/Overview/index',
    route_name = 'InventoryOverview',
    perms = 'inventory:overview:list',
    icon = 'DashboardOutlined',
    remark = '库存总览展示商城SKU平台可售库存、来源库存参考和库存调整入口',
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2420
  and menu_type = 'C';

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select 2420, '库存总览', 2080, 5, 'overview', 'Inventory/Overview/index', '', 'InventoryOverview',
       1, 0, 'C', '0', '0', 'inventory:overview:list', 'DashboardOutlined', 'admin',
       sysdate(), '', null, '库存总览展示商城SKU平台可售库存、来源库存参考和库存调整入口'
where not exists (select 1 from sys_menu where menu_id = 2420);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_id, seed.menu_name, 2420, seed.order_num, '#', '', '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select 242001 menu_id, '库存总览查询' menu_name, 1 order_num, 'inventory:overview:query' perms, '库存总览明细查询按钮' remark
    union all select 242002, '库存调整', 2, 'inventory:overview:adjust', '库存总览平台库存调整按钮'
    union all select 242003, '库存流水', 3, 'inventory:overview:ledger', '库存总览库存流水按钮'
    union all select 242004, '库存导出', 4, 'inventory:overview:export', '库存总览导出按钮'
) seed
where not exists (select 1 from sys_menu m where m.menu_id = seed.menu_id);

insert into inventory_sku_warehouse_stock(
    stock_key, spu_id, sku_id, seller_id, system_sku_code, warehouse_kind, warehouse_ref_type,
    warehouse_id, warehouse_code, warehouse_name, source_scope, source_master_warehouse_name,
    source_inventory_scope, source_inventory_attribute, source_total_qty, source_available_qty,
    source_in_transit_qty, source_snapshot_time, platform_total_qty, platform_reserved_qty,
    platform_in_transit_qty, pending_available_inbound_qty, pending_source_deduction_qty,
    platform_available_qty, effective_status, calc_time, create_by, create_time, update_by, update_time, remark
)
select concat('INV:', sha2(concat_ws('|', 'SKU', sk.sku_id, 'OFFICIAL_MASTER', src.master_warehouse_name), 256)),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       'official',
       'OFFICIAL_MASTER',
       null,
       '',
       src.master_warehouse_name,
       'OFFICIAL_MASTER',
       src.master_warehouse_name,
       'COMPREHENSIVE',
       '0',
       src.source_total_qty,
       src.source_available_qty,
       src.source_in_transit_qty,
       src.source_snapshot_time,
       0, 0, 0, 0, 0, 0,
       case
         when src.source_available_qty > 0 then 'OUT_OF_STOCK'
         when src.source_in_transit_qty > 0 then 'SOURCE_ONLY_IN_TRANSIT'
         else 'NO_SOURCE'
       end,
       sysdate(), 'admin', sysdate(), 'admin', sysdate(), '来源库存初始化生成'
from product_sku sk
join product_sku_source_binding b
  on b.sku_id = sk.sku_id
 and b.binding_status = 'ACTIVE'
join (
    select d.source_stock_group_key,
           d.master_warehouse_name,
           sum(coalesce(d.total_quantity, 0)) source_total_qty,
           sum(coalesce(d.available_quantity, 0)) source_available_qty,
           sum(coalesce(d.in_transit_quantity, 0)) source_in_transit_qty,
           max(d.update_time) source_snapshot_time
    from source_warehouse_stock_detail d
    where d.repository_scope = 'OFFICIAL_MASTER'
      and d.inventory_scope = 'COMPREHENSIVE'
      and d.inventory_attribute = '0'
      and nullif(d.master_warehouse_name, '') is not null
    group by d.source_stock_group_key, d.master_warehouse_name
) src on src.source_stock_group_key = b.source_sku_group_key
where sk.del_flag = '0'
on duplicate key update
    source_total_qty = values(source_total_qty),
    source_available_qty = values(source_available_qty),
    source_in_transit_qty = values(source_in_transit_qty),
    source_snapshot_time = values(source_snapshot_time),
    warehouse_name = values(warehouse_name),
    source_master_warehouse_name = values(source_master_warehouse_name),
    effective_status = case
      when inventory_sku_warehouse_stock.platform_available_qty > 0 then 'IN_STOCK'
      when values(source_available_qty) <= 0 and values(source_in_transit_qty) > 0 then 'SOURCE_ONLY_IN_TRANSIT'
      when values(source_available_qty) <= 0 and values(source_in_transit_qty) <= 0 then 'NO_SOURCE'
      else 'OUT_OF_STOCK'
    end,
    calc_time = sysdate(),
    update_by = 'admin',
    update_time = sysdate();

insert into inventory_sku_warehouse_stock(
    stock_key, spu_id, sku_id, seller_id, system_sku_code, warehouse_kind, warehouse_ref_type,
    warehouse_id, warehouse_code, warehouse_name, source_scope, source_master_warehouse_name,
    source_inventory_scope, source_inventory_attribute, source_total_qty, source_available_qty,
    source_in_transit_qty, source_snapshot_time, platform_total_qty, platform_reserved_qty,
    platform_in_transit_qty, pending_available_inbound_qty, pending_source_deduction_qty,
    platform_available_qty, effective_status, calc_time, create_by, create_time, update_by, update_time, remark
)
select concat('INV:', sha2(concat_ws('|', 'SKU', sk.sku_id, 'UNMATCHED_OFFICIAL'), 256)),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       'official',
       'UNMATCHED_OFFICIAL',
       null,
       '',
       '官方仓（未匹配来源库存）',
       'OFFICIAL_MASTER',
       '',
       'COMPREHENSIVE',
       '0',
       0, 0, 0, null, 0, 0, 0, 0, 0, 0, 'NO_SOURCE',
       sysdate(), 'admin', sysdate(), 'admin', sysdate(), '官方仓无来源库存占位行'
from product_sku sk
where sk.del_flag = '0'
  and (
    exists (
      select 1 from product_spu_warehouse pw
      where pw.spu_id = sk.spu_id
        and pw.warehouse_kind = 'official'
    )
    or exists (
      select 1 from product_sku_source_binding b
      where b.sku_id = sk.sku_id
        and b.binding_status = 'ACTIVE'
    )
  )
  and not exists (
    select 1
    from product_sku_source_binding b
    join source_warehouse_stock_detail d
      on d.source_stock_group_key = b.source_sku_group_key
     and d.repository_scope = 'OFFICIAL_MASTER'
     and d.inventory_scope = 'COMPREHENSIVE'
     and d.inventory_attribute = '0'
     and nullif(d.master_warehouse_name, '') is not null
    where b.sku_id = sk.sku_id
      and b.binding_status = 'ACTIVE'
  )
on duplicate key update
    spu_id = values(spu_id),
    seller_id = values(seller_id),
    system_sku_code = values(system_sku_code),
    effective_status = 'NO_SOURCE',
    calc_time = sysdate(),
    update_by = 'admin',
    update_time = sysdate();

insert into inventory_sku_warehouse_stock(
    stock_key, spu_id, sku_id, seller_id, system_sku_code, warehouse_kind, warehouse_ref_type,
    warehouse_id, warehouse_code, warehouse_name, source_scope, source_master_warehouse_name,
    source_inventory_scope, source_inventory_attribute, source_total_qty, source_available_qty,
    source_in_transit_qty, source_snapshot_time, platform_total_qty, platform_reserved_qty,
    platform_in_transit_qty, pending_available_inbound_qty, pending_source_deduction_qty,
    platform_available_qty, effective_status, calc_time, create_by, create_time, update_by, update_time, remark
)
select concat('INV:', sha2(concat_ws('|', 'SKU', sk.sku_id, 'THIRD_PARTY_WAREHOUSE', pw.warehouse_id), 256)),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       'third_party',
       'THIRD_PARTY_WAREHOUSE',
       pw.warehouse_id,
       pw.warehouse_code,
       pw.warehouse_name,
       '',
       '',
       '',
       '',
       0, 0, 0, null, 0, 0, 0, 0, 0, 0, 'OUT_OF_STOCK',
       sysdate(), 'admin', sysdate(), 'admin', sysdate(), '三方仓库存初始化生成'
from product_sku sk
join product_spu_warehouse pw
  on pw.spu_id = sk.spu_id
 and pw.warehouse_kind = 'third_party'
where sk.del_flag = '0'
on duplicate key update
    spu_id = values(spu_id),
    seller_id = values(seller_id),
    system_sku_code = values(system_sku_code),
    warehouse_id = values(warehouse_id),
    warehouse_code = values(warehouse_code),
    warehouse_name = values(warehouse_name),
    calc_time = sysdate(),
    update_by = 'admin',
    update_time = sysdate();

insert into inventory_overview_sku_read_model(
    sku_stock_key, spu_id, sku_id, seller_id, system_sku_code, product_name, sku_name, sku_image_url,
    warehouse_kind_summary, warehouse_count, platform_total_qty, platform_available_qty,
    platform_reserved_qty, platform_in_transit_qty, source_total_qty, source_available_qty,
    source_in_transit_qty, inventory_status, latest_source_snapshot_time, latest_stock_update_time,
    search_text, rebuild_time
)
select concat('SKU:', sk.sku_id),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       p.product_name,
       trim(concat_ws(' / ', nullif(sk.color, ''), nullif(sk.size, ''), nullif(sk.model, ''))),
       coalesce(sk.sku_image_url, ''),
       case when count(distinct st.warehouse_kind) = 1 then min(st.warehouse_kind) else 'MIXED' end,
       count(st.stock_id),
       sum(coalesce(st.platform_total_qty, 0)),
       sum(coalesce(st.platform_available_qty, 0)),
       sum(coalesce(st.platform_reserved_qty, 0)),
       sum(coalesce(st.platform_in_transit_qty, 0)),
       sum(case when st.warehouse_kind = 'official' then coalesce(st.source_total_qty, 0) else 0 end),
       sum(case when st.warehouse_kind = 'official' then coalesce(st.source_available_qty, 0) else 0 end),
       sum(case when st.warehouse_kind = 'official' then coalesce(st.source_in_transit_qty, 0) else 0 end),
       case
         when sum(coalesce(st.platform_available_qty, 0)) > 0 then 'IN_STOCK'
         when sum(case when st.effective_status = 'SOURCE_ONLY_IN_TRANSIT' then 1 else 0 end) > 0 then 'SOURCE_ONLY_IN_TRANSIT'
         when sum(case when st.effective_status = 'NO_SOURCE' then 1 else 0 end) > 0 then 'NO_SOURCE'
         else 'OUT_OF_STOCK'
       end,
       max(st.source_snapshot_time),
       max(st.update_time),
       concat_ws(' ', p.system_spu_code, p.product_name, sk.system_sku_code, sk.seller_sku_code, sk.color, sk.size, sk.model),
       sysdate()
from product_sku sk
join product_spu p on p.spu_id = sk.spu_id
join inventory_sku_warehouse_stock st on st.sku_id = sk.sku_id
where sk.del_flag = '0'
group by sk.sku_id, sk.spu_id, sk.seller_id, sk.system_sku_code, p.product_name, sk.sku_image_url,
         sk.color, sk.size, sk.model, p.system_spu_code, sk.seller_sku_code
on duplicate key update
    spu_id = values(spu_id),
    seller_id = values(seller_id),
    system_sku_code = values(system_sku_code),
    product_name = values(product_name),
    sku_name = values(sku_name),
    sku_image_url = values(sku_image_url),
    warehouse_kind_summary = values(warehouse_kind_summary),
    warehouse_count = values(warehouse_count),
    platform_total_qty = values(platform_total_qty),
    platform_available_qty = values(platform_available_qty),
    platform_reserved_qty = values(platform_reserved_qty),
    platform_in_transit_qty = values(platform_in_transit_qty),
    source_total_qty = values(source_total_qty),
    source_available_qty = values(source_available_qty),
    source_in_transit_qty = values(source_in_transit_qty),
    inventory_status = values(inventory_status),
    latest_source_snapshot_time = values(latest_source_snapshot_time),
    latest_stock_update_time = values(latest_stock_update_time),
    search_text = values(search_text),
    rebuild_time = sysdate();

insert into inventory_overview_spu_read_model(
    spu_stock_key, spu_id, seller_id, system_spu_code, product_name, main_image_url, sku_count,
    warehouse_kind_summary, warehouse_count, platform_total_qty, platform_available_qty,
    platform_reserved_qty, platform_in_transit_qty, source_total_qty, source_available_qty,
    source_in_transit_qty, inventory_status, latest_source_snapshot_time, latest_stock_update_time,
    search_text, rebuild_time
)
select concat('SPU:', p.spu_id),
       p.spu_id,
       p.seller_id,
       p.system_spu_code,
       p.product_name,
       coalesce(p.main_image_url, ''),
       count(distinct sk.sku_id),
       case when count(distinct srm.warehouse_kind_summary) = 1 then min(srm.warehouse_kind_summary) else 'MIXED' end,
       sum(coalesce(srm.warehouse_count, 0)),
       sum(coalesce(srm.platform_total_qty, 0)),
       sum(coalesce(srm.platform_available_qty, 0)),
       sum(coalesce(srm.platform_reserved_qty, 0)),
       sum(coalesce(srm.platform_in_transit_qty, 0)),
       sum(coalesce(srm.source_total_qty, 0)),
       sum(coalesce(srm.source_available_qty, 0)),
       sum(coalesce(srm.source_in_transit_qty, 0)),
       case
         when sum(coalesce(srm.platform_available_qty, 0)) > 0 then 'IN_STOCK'
         when sum(case when srm.inventory_status = 'SOURCE_ONLY_IN_TRANSIT' then 1 else 0 end) > 0 then 'SOURCE_ONLY_IN_TRANSIT'
         when sum(case when srm.inventory_status = 'NO_SOURCE' then 1 else 0 end) > 0 then 'NO_SOURCE'
         else 'OUT_OF_STOCK'
       end,
       max(srm.latest_source_snapshot_time),
       max(srm.latest_stock_update_time),
       concat_ws(' ', p.system_spu_code, p.seller_spu_code, p.product_name, p.product_name_en, p.seller_name),
       sysdate()
from product_spu p
join product_sku sk on sk.spu_id = p.spu_id and sk.del_flag = '0'
join inventory_overview_sku_read_model srm on srm.sku_id = sk.sku_id
where p.del_flag = '0'
group by p.spu_id, p.seller_id, p.system_spu_code, p.product_name, p.main_image_url,
         p.seller_spu_code, p.product_name_en, p.seller_name
on duplicate key update
    seller_id = values(seller_id),
    system_spu_code = values(system_spu_code),
    product_name = values(product_name),
    main_image_url = values(main_image_url),
    sku_count = values(sku_count),
    warehouse_kind_summary = values(warehouse_kind_summary),
    warehouse_count = values(warehouse_count),
    platform_total_qty = values(platform_total_qty),
    platform_available_qty = values(platform_available_qty),
    platform_reserved_qty = values(platform_reserved_qty),
    platform_in_transit_qty = values(platform_in_transit_qty),
    source_total_qty = values(source_total_qty),
    source_available_qty = values(source_available_qty),
    source_in_transit_qty = values(source_in_transit_qty),
    inventory_status = values(inventory_status),
    latest_source_snapshot_time = values(latest_source_snapshot_time),
    latest_stock_update_time = values(latest_stock_update_time),
    search_text = values(search_text),
    rebuild_time = sysdate();

drop temporary table if exists tmp_inventory_overview_sys_menu_guard;
drop temporary table if exists tmp_inventory_overview_sys_menu_guard_ids;
drop procedure if exists assert_inventory_overview_sys_menu_guard;
