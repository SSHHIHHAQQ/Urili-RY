-- Source warehouse stock read model.
-- Scope:
-- 1. Create source warehouse stock group/detail/filter read-model tables.
-- 2. Backfill read models from upstream_system_sku_inventory_snapshot.
-- Source of truth remains upstream_system_sku_inventory_snapshot.

set names utf8mb4;

set @confirm_source_warehouse_stock_read_model := coalesce(@confirm_source_warehouse_stock_read_model, '');

delimiter //

drop procedure if exists assert_source_warehouse_stock_read_model_confirmed//
create procedure assert_source_warehouse_stock_read_model_confirmed()
begin
  if coalesce(@confirm_source_warehouse_stock_read_model, '')
      <> 'APPLY_SOURCE_WAREHOUSE_STOCK_READ_MODEL' then
    signal sqlstate '45000' set message_text = 'set @confirm_source_warehouse_stock_read_model = APPLY_SOURCE_WAREHOUSE_STOCK_READ_MODEL before running this migration';
  end if;
end//

drop procedure if exists assert_table_exists//
create procedure assert_table_exists(in p_table varchar(64), in p_message varchar(255))
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
create procedure assert_column_exists(in p_table varchar(64), in p_column varchar(64), in p_message varchar(255))
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

call assert_source_warehouse_stock_read_model_confirmed();
call assert_table_exists('upstream_system_sku_inventory_snapshot',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot');
call assert_table_exists('upstream_system_connection',
  'source warehouse stock read model requires upstream_system_connection');

call assert_column_exists('upstream_system_sku_inventory_snapshot', 'inventory_snapshot_id',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.inventory_snapshot_id');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'connection_code',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.connection_code');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'upstream_warehouse_code',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.upstream_warehouse_code');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'upstream_warehouse_name',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.upstream_warehouse_name');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'master_sku',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.master_sku');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'master_product_name',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.master_product_name');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'inventory_scope',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.inventory_scope');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'inventory_attribute',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.inventory_attribute');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'batch_no',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.batch_no');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'location_code',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.location_code');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'total_quantity',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.total_quantity');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'available_quantity',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.available_quantity');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'locked_quantity',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.locked_quantity');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'in_transit_quantity',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.in_transit_quantity');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'boxed_quantity',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.boxed_quantity');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'unboxed_quantity',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.unboxed_quantity');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'system_warehouse_code',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.system_warehouse_code');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'system_warehouse_name',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.system_warehouse_name');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'system_sku',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.system_sku');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'system_sku_name',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.system_sku_name');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'customer_name',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.customer_name');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'status',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.status');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'sync_batch_id',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.sync_batch_id');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'source_payload_hash',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.source_payload_hash');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'first_seen_time',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.first_seen_time');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'last_seen_time',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.last_seen_time');
call assert_column_exists('upstream_system_sku_inventory_snapshot', 'update_time',
  'source warehouse stock read model requires upstream_system_sku_inventory_snapshot.update_time');

call assert_column_exists('upstream_system_connection', 'connection_code',
  'source warehouse stock read model requires upstream_system_connection.connection_code');
call assert_column_exists('upstream_system_connection', 'system_kind',
  'source warehouse stock read model requires upstream_system_connection.system_kind');
call assert_column_exists('upstream_system_connection', 'master_warehouse_name',
  'source warehouse stock read model requires upstream_system_connection.master_warehouse_name');

drop procedure if exists assert_source_warehouse_stock_read_model_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists assert_column_exists;

create table if not exists source_warehouse_stock_group (
  source_stock_group_key     varchar(128)  not null                 comment '稳定来源库存组key',
  repository_scope           varchar(32)   not null default 'OFFICIAL_MASTER' comment '来源范围',
  inventory_scope            varchar(32)   not null                 comment '库存口径',
  master_sku                 varchar(128)  not null                 comment '来源SKU',
  master_product_name        varchar(255)  not null default ''      comment '来源商品名',
  inventory_attribute_codes  varchar(200)  not null default ''      comment '库存属性code摘要',
  inventory_attribute_labels varchar(500)  not null default ''      comment '库存属性展示摘要',
  inventory_attribute_count  int           not null default 0       comment '库存属性去重数量',
  source_connection_codes    varchar(1000) not null default ''      comment '来源连接编号摘要',
  master_warehouse_names     varchar(1000) not null default ''      comment '来源主仓名摘要',
  master_warehouse_count     int           not null default 0       comment '来源主仓数量',
  upstream_warehouse_codes   varchar(1000) not null default ''      comment '来源仓库code摘要',
  upstream_warehouse_names   varchar(1000) not null default ''      comment '来源仓库名摘要',
  upstream_warehouse_count   int           not null default 0       comment '来源仓库数量',
  detail_row_count           int           not null default 0       comment '明细行数',
  active_detail_count        int           not null default 0       comment '正常明细数',
  missing_detail_count       int           not null default 0       comment '上游缺失明细数',
  total_quantity             bigint(20)    not null default 0       comment '总库存汇总',
  available_quantity         bigint(20)    not null default 0       comment '可用库存汇总',
  locked_quantity            bigint(20)    not null default 0       comment '锁定库存汇总',
  in_transit_quantity        bigint(20)    not null default 0       comment '在途库存汇总',
  boxed_quantity             bigint(20)                             comment '箱内库存汇总',
  unboxed_quantity           bigint(20)                             comment '散件库存汇总',
  system_warehouse_codes     varchar(1000) not null default ''      comment '系统仓库code摘要',
  system_warehouse_names     varchar(1000) not null default ''      comment '系统仓库名摘要',
  system_skus                varchar(1000) not null default ''      comment '商城SKU摘要',
  system_sku_names           varchar(1000) not null default ''      comment '商城SKU名称摘要',
  customer_names             varchar(1000) not null default ''      comment '客户名称摘要',
  warehouse_pairing_status   varchar(32)   not null default 'UNASSIGNED' comment '仓库配对汇总状态',
  sku_pairing_status         varchar(32)   not null default 'UNASSIGNED' comment 'SKU配对汇总状态',
  status                     varchar(16)   not null default 'ACTIVE' comment '同步汇总状态',
  latest_sync_batch_id       varchar(64)   not null default ''      comment '最近同步批次',
  first_seen_time            datetime      not null                 comment '最早首次发现时间',
  last_seen_time             datetime      not null                 comment '最近发现时间',
  latest_update_time         datetime      not null                 comment '最近更新时间',
  search_text                text          not null                 comment '搜索文本',
  rebuild_time               datetime      not null                 comment '读模型构建时间',
  primary key (source_stock_group_key),
  unique key uk_source_warehouse_stock_group_natural
    (repository_scope, inventory_scope, master_sku, master_product_name),
  key idx_source_warehouse_stock_group_list
    (repository_scope, inventory_scope, latest_update_time, source_stock_group_key),
  key idx_source_warehouse_stock_group_sku
    (repository_scope, inventory_scope, master_sku),
  key idx_source_warehouse_stock_group_status
    (repository_scope, inventory_scope, status),
  key idx_source_warehouse_stock_group_wh_pairing
    (repository_scope, inventory_scope, warehouse_pairing_status),
  key idx_source_warehouse_stock_group_sku_pairing
    (repository_scope, inventory_scope, sku_pairing_status)
) engine=innodb comment='来源仓库库存组读模型';

create table if not exists source_warehouse_stock_detail (
  inventory_snapshot_id      bigint(20)   not null                 comment '库存快照ID',
  source_stock_group_key     varchar(128) not null                 comment '来源库存组key',
  repository_scope           varchar(32)  not null default 'OFFICIAL_MASTER' comment '来源范围',
  connection_code            varchar(64)  not null                 comment '来源连接编号',
  system_kind                varchar(64)  not null default ''      comment '来源系统类型',
  master_warehouse_name      varchar(128) not null default ''      comment '来源主仓名',
  upstream_warehouse_code    varchar(100) not null                 comment '来源仓库code',
  upstream_warehouse_name    varchar(200) not null default ''      comment '来源仓库名称',
  master_sku                 varchar(128) not null                 comment '来源SKU',
  master_product_name        varchar(255) not null default ''      comment '来源商品名',
  inventory_scope            varchar(32)  not null                 comment '库存口径',
  inventory_attribute        varchar(64)  not null default ''      comment '库存属性code',
  inventory_attribute_label  varchar(100) not null default ''      comment '库存属性展示名',
  batch_no                   varchar(128) not null default ''      comment '批次号',
  location_code              varchar(128) not null default ''      comment '库位代码',
  total_quantity             bigint(20)   not null default 0       comment '总库存',
  available_quantity         bigint(20)   not null default 0       comment '可用库存',
  locked_quantity            bigint(20)   not null default 0       comment '锁定库存',
  in_transit_quantity        bigint(20)   not null default 0       comment '在途库存',
  boxed_quantity             bigint(20)                            comment '箱内库存',
  unboxed_quantity           bigint(20)                            comment '散件库存',
  system_warehouse_code      varchar(64)  not null default ''      comment '系统仓库code',
  system_warehouse_name      varchar(200) not null default ''      comment '系统仓库名称',
  system_sku                 varchar(128) not null default ''      comment '商城SKU',
  system_sku_name            varchar(255) not null default ''      comment '商城SKU名称',
  customer_name              varchar(200) not null default ''      comment '客户名称',
  warehouse_pairing_status   varchar(32)  not null default 'UNASSIGNED' comment '仓库配对状态',
  sku_pairing_status         varchar(32)  not null default 'UNASSIGNED' comment 'SKU配对状态',
  status                     varchar(16)  not null default 'ACTIVE' comment '同步状态',
  sync_batch_id              varchar(64)  not null default ''      comment '同步批次',
  source_payload_hash        varchar(64)  not null default ''      comment '来源库存行hash',
  first_seen_time            datetime     not null                 comment '首次发现时间',
  last_seen_time             datetime     not null                 comment '最近发现时间',
  update_time                datetime     not null                 comment '快照更新时间',
  rebuild_time               datetime     not null                 comment '读模型构建时间',
  primary key (inventory_snapshot_id),
  key idx_source_warehouse_stock_detail_group
    (source_stock_group_key, update_time),
  key idx_source_warehouse_stock_detail_connection
    (connection_code, status),
  key idx_source_warehouse_stock_detail_master_wh
    (repository_scope, inventory_scope, master_warehouse_name),
  key idx_source_warehouse_stock_detail_upstream_wh
    (repository_scope, inventory_scope, upstream_warehouse_code),
  key idx_source_warehouse_stock_detail_sku
    (repository_scope, inventory_scope, master_sku),
  key idx_source_warehouse_stock_detail_attribute
    (repository_scope, inventory_scope, inventory_attribute),
  key idx_source_warehouse_stock_detail_system_sku
    (repository_scope, inventory_scope, system_sku)
) engine=innodb comment='来源仓库库存明细读模型';

create table if not exists source_warehouse_stock_filter_metric (
  metric_key                 varchar(160) not null                 comment '筛选指标key',
  source_stock_group_key     varchar(128) not null                 comment '来源库存组key',
  repository_scope           varchar(32)  not null default 'OFFICIAL_MASTER' comment '来源范围',
  inventory_scope            varchar(32)  not null                 comment '库存口径',
  filter_type                varchar(64)  not null                 comment '筛选维度类型',
  filter_value               varchar(255) not null default ''      comment '筛选维度值',
  filter_label               varchar(255) not null default ''      comment '筛选维度展示值',
  detail_row_count           int          not null default 0       comment '命中明细行数',
  total_quantity             bigint(20)   not null default 0       comment '命中总库存',
  available_quantity         bigint(20)   not null default 0       comment '命中可用库存',
  locked_quantity            bigint(20)   not null default 0       comment '命中锁定库存',
  in_transit_quantity        bigint(20)   not null default 0       comment '命中在途库存',
  boxed_quantity             bigint(20)                            comment '命中箱内库存',
  unboxed_quantity           bigint(20)                            comment '命中散件库存',
  latest_update_time         datetime     not null                 comment '命中最近更新时间',
  rebuild_time               datetime     not null                 comment '读模型构建时间',
  primary key (metric_key),
  unique key uk_source_warehouse_stock_filter_metric_natural
    (source_stock_group_key, filter_type, filter_value),
  key idx_source_warehouse_stock_filter_lookup
    (repository_scope, inventory_scope, filter_type, filter_value, source_stock_group_key),
  key idx_source_warehouse_stock_filter_group
    (source_stock_group_key, filter_type)
) engine=innodb comment='来源仓库库存筛选指标读模型';

set group_concat_max_len = 1048576;

drop temporary table if exists tmp_source_warehouse_stock_detail;
drop temporary table if exists tmp_source_warehouse_stock_group;
drop temporary table if exists tmp_source_warehouse_stock_filter_metric;

create temporary table tmp_source_warehouse_stock_detail like source_warehouse_stock_detail;
create temporary table tmp_source_warehouse_stock_group like source_warehouse_stock_group;
create temporary table tmp_source_warehouse_stock_filter_metric like source_warehouse_stock_filter_metric;

insert into tmp_source_warehouse_stock_detail(
  inventory_snapshot_id, source_stock_group_key, repository_scope, connection_code, system_kind,
  master_warehouse_name, upstream_warehouse_code, upstream_warehouse_name, master_sku, master_product_name,
  inventory_scope, inventory_attribute, inventory_attribute_label, batch_no, location_code,
  total_quantity, available_quantity, locked_quantity, in_transit_quantity, boxed_quantity, unboxed_quantity,
  system_warehouse_code, system_warehouse_name, system_sku, system_sku_name, customer_name,
  warehouse_pairing_status, sku_pairing_status, status, sync_batch_id, source_payload_hash,
  first_seen_time, last_seen_time, update_time, rebuild_time
)
select s.inventory_snapshot_id,
       concat('SOURCE_STOCK:', sha2(concat(
         'OFFICIAL_MASTER|', coalesce(s.inventory_scope, ''), '|',
         char_length(trim(coalesce(s.master_sku, ''))), ':', trim(coalesce(s.master_sku, '')), '|',
         char_length(trim(coalesce(s.master_product_name, ''))), ':', trim(coalesce(s.master_product_name, ''))
       ), 256)) as source_stock_group_key,
       'OFFICIAL_MASTER' as repository_scope,
       s.connection_code,
       case when c.system_kind = 'LINGXING_WMS' then 'lingxing-wms' else coalesce(c.system_kind, '') end as system_kind,
       coalesce(c.master_warehouse_name, '') as master_warehouse_name,
       s.upstream_warehouse_code,
       coalesce(s.upstream_warehouse_name, '') as upstream_warehouse_name,
       s.master_sku,
       coalesce(s.master_product_name, '') as master_product_name,
       s.inventory_scope,
       s.inventory_attribute,
       case
         when s.inventory_attribute = '0' then '正品'
         when s.inventory_attribute = '1' then '次品'
         when nullif(s.inventory_attribute, '') is null then ''
         else s.inventory_attribute
       end as inventory_attribute_label,
       s.batch_no,
       s.location_code,
       s.total_quantity,
       s.available_quantity,
       s.locked_quantity,
       s.in_transit_quantity,
       s.boxed_quantity,
       s.unboxed_quantity,
       coalesce(s.system_warehouse_code, '') as system_warehouse_code,
       coalesce(s.system_warehouse_name, '') as system_warehouse_name,
       coalesce(s.system_sku, '') as system_sku,
       coalesce(s.system_sku_name, '') as system_sku_name,
       coalesce(s.customer_name, '') as customer_name,
       case when nullif(s.system_warehouse_code, '') is null then 'UNASSIGNED' else 'PAIRED' end as warehouse_pairing_status,
       case when nullif(s.system_sku, '') is null then 'UNASSIGNED' else 'PAIRED' end as sku_pairing_status,
       s.status,
       s.sync_batch_id,
       coalesce(s.source_payload_hash, '') as source_payload_hash,
       s.first_seen_time,
       s.last_seen_time,
       s.update_time,
       sysdate() as rebuild_time
from upstream_system_sku_inventory_snapshot s
left join upstream_system_connection c on c.connection_code = s.connection_code;

insert into tmp_source_warehouse_stock_group(
  source_stock_group_key, repository_scope, inventory_scope, master_sku, master_product_name,
  inventory_attribute_codes, inventory_attribute_labels, inventory_attribute_count,
  source_connection_codes, master_warehouse_names, master_warehouse_count,
  upstream_warehouse_codes, upstream_warehouse_names, upstream_warehouse_count,
  detail_row_count, active_detail_count, missing_detail_count,
  total_quantity, available_quantity, locked_quantity, in_transit_quantity, boxed_quantity, unboxed_quantity,
  system_warehouse_codes, system_warehouse_names, system_skus, system_sku_names, customer_names,
  warehouse_pairing_status, sku_pairing_status, status, latest_sync_batch_id,
  first_seen_time, last_seen_time, latest_update_time, search_text, rebuild_time
)
select d.source_stock_group_key,
       max(d.repository_scope),
       max(d.inventory_scope),
       max(d.master_sku),
       max(d.master_product_name),
       coalesce(group_concat(distinct nullif(d.inventory_attribute, '') order by d.inventory_attribute separator ','), ''),
       coalesce(group_concat(distinct nullif(d.inventory_attribute_label, '') order by d.inventory_attribute_label separator ' / '), ''),
       count(distinct nullif(d.inventory_attribute, '')),
       coalesce(group_concat(distinct d.connection_code order by d.connection_code separator ','), ''),
       coalesce(group_concat(distinct nullif(d.master_warehouse_name, '') order by d.master_warehouse_name separator ' / '), ''),
       count(distinct nullif(d.master_warehouse_name, '')),
       coalesce(group_concat(distinct nullif(d.upstream_warehouse_code, '') order by d.upstream_warehouse_code separator ','), ''),
       coalesce(group_concat(distinct nullif(d.upstream_warehouse_name, '') order by d.upstream_warehouse_name separator ' / '), ''),
       count(distinct nullif(d.upstream_warehouse_code, '')),
       count(1),
       sum(case when d.status = 'ACTIVE' then 1 else 0 end),
       sum(case when d.status = 'MISSING' then 1 else 0 end),
       sum(coalesce(d.total_quantity, 0)),
       sum(coalesce(d.available_quantity, 0)),
       sum(coalesce(d.locked_quantity, 0)),
       sum(coalesce(d.in_transit_quantity, 0)),
       case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end,
       case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end,
       coalesce(group_concat(distinct nullif(d.system_warehouse_code, '') order by d.system_warehouse_code separator ','), ''),
       coalesce(group_concat(distinct nullif(d.system_warehouse_name, '') order by d.system_warehouse_name separator ' / '), ''),
       coalesce(group_concat(distinct nullif(d.system_sku, '') order by d.system_sku separator ' / '), ''),
       coalesce(group_concat(distinct nullif(d.system_sku_name, '') order by d.system_sku_name separator ' / '), ''),
       coalesce(group_concat(distinct nullif(d.customer_name, '') order by d.customer_name separator ' / '), ''),
       case
         when sum(case when d.warehouse_pairing_status = 'PAIRED' then 1 else 0 end) = 0 then 'UNASSIGNED'
         when sum(case when d.warehouse_pairing_status = 'PAIRED' then 1 else 0 end) = count(1) then 'PAIRED'
         else 'PARTIAL'
       end,
       case
         when sum(case when d.sku_pairing_status = 'PAIRED' then 1 else 0 end) = 0 then 'UNASSIGNED'
         when sum(case when d.sku_pairing_status = 'PAIRED' then 1 else 0 end) = count(1) then 'PAIRED'
         else 'PARTIAL'
       end,
       case when count(distinct d.status) = 1 then max(d.status) else 'MIXED' end,
       max(d.sync_batch_id),
       min(d.first_seen_time),
       max(d.last_seen_time),
       max(d.update_time),
       concat_ws(' ',
         max(d.master_sku), max(d.master_product_name),
         coalesce(group_concat(distinct nullif(d.master_warehouse_name, '') order by d.master_warehouse_name separator ' '), ''),
         coalesce(group_concat(distinct nullif(d.upstream_warehouse_code, '') order by d.upstream_warehouse_code separator ' '), ''),
         coalesce(group_concat(distinct nullif(d.upstream_warehouse_name, '') order by d.upstream_warehouse_name separator ' '), ''),
         coalesce(group_concat(distinct nullif(d.system_warehouse_code, '') order by d.system_warehouse_code separator ' '), ''),
         coalesce(group_concat(distinct nullif(d.system_warehouse_name, '') order by d.system_warehouse_name separator ' '), ''),
         coalesce(group_concat(distinct nullif(d.system_sku, '') order by d.system_sku separator ' '), ''),
         coalesce(group_concat(distinct nullif(d.system_sku_name, '') order by d.system_sku_name separator ' '), ''),
         coalesce(group_concat(distinct nullif(d.customer_name, '') order by d.customer_name separator ' '), '')
       ),
       sysdate()
from tmp_source_warehouse_stock_detail d
where d.repository_scope = 'OFFICIAL_MASTER'
group by d.source_stock_group_key;

insert into tmp_source_warehouse_stock_filter_metric(
  metric_key, source_stock_group_key, repository_scope, inventory_scope, filter_type, filter_value,
  filter_label, detail_row_count, total_quantity, available_quantity, locked_quantity, in_transit_quantity,
  boxed_quantity, unboxed_quantity, latest_update_time, rebuild_time
)
select concat('SOURCE_STOCK_METRIC:', sha2(concat(m.source_stock_group_key, '|', m.filter_type, '|', m.filter_value), 256)),
       m.source_stock_group_key, m.repository_scope, m.inventory_scope, m.filter_type, m.filter_value, m.filter_label,
       m.detail_row_count, m.total_quantity, m.available_quantity, m.locked_quantity, m.in_transit_quantity,
       m.boxed_quantity, m.unboxed_quantity, m.latest_update_time, sysdate()
from (
  select d.source_stock_group_key, max(d.repository_scope) repository_scope, max(d.inventory_scope) inventory_scope,
         'INVENTORY_ATTRIBUTE' filter_type, d.inventory_attribute filter_value, max(d.inventory_attribute_label) filter_label,
         count(1) detail_row_count, sum(coalesce(d.total_quantity, 0)) total_quantity,
         sum(coalesce(d.available_quantity, 0)) available_quantity, sum(coalesce(d.locked_quantity, 0)) locked_quantity,
         sum(coalesce(d.in_transit_quantity, 0)) in_transit_quantity,
         case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end boxed_quantity,
         case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end unboxed_quantity,
         max(d.update_time) latest_update_time
  from tmp_source_warehouse_stock_detail d
  where d.repository_scope = 'OFFICIAL_MASTER' and nullif(d.inventory_attribute, '') is not null
  group by d.source_stock_group_key, d.inventory_attribute
  union all
  select d.source_stock_group_key, max(d.repository_scope), max(d.inventory_scope), 'MASTER_WAREHOUSE',
         d.master_warehouse_name, d.master_warehouse_name, count(1), sum(coalesce(d.total_quantity, 0)),
         sum(coalesce(d.available_quantity, 0)), sum(coalesce(d.locked_quantity, 0)), sum(coalesce(d.in_transit_quantity, 0)),
         case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end,
         case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end,
         max(d.update_time)
  from tmp_source_warehouse_stock_detail d
  where d.repository_scope = 'OFFICIAL_MASTER' and nullif(d.master_warehouse_name, '') is not null
  group by d.source_stock_group_key, d.master_warehouse_name
  union all
  select d.source_stock_group_key, max(d.repository_scope), max(d.inventory_scope), 'UPSTREAM_WAREHOUSE',
         d.upstream_warehouse_code, max(d.upstream_warehouse_name), count(1), sum(coalesce(d.total_quantity, 0)),
         sum(coalesce(d.available_quantity, 0)), sum(coalesce(d.locked_quantity, 0)), sum(coalesce(d.in_transit_quantity, 0)),
         case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end,
         case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end,
         max(d.update_time)
  from tmp_source_warehouse_stock_detail d
  where d.repository_scope = 'OFFICIAL_MASTER' and nullif(d.upstream_warehouse_code, '') is not null
  group by d.source_stock_group_key, d.upstream_warehouse_code
  union all
  select d.source_stock_group_key, max(d.repository_scope), max(d.inventory_scope), 'SYSTEM_WAREHOUSE',
         d.system_warehouse_code, max(d.system_warehouse_name), count(1), sum(coalesce(d.total_quantity, 0)),
         sum(coalesce(d.available_quantity, 0)), sum(coalesce(d.locked_quantity, 0)), sum(coalesce(d.in_transit_quantity, 0)),
         case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end,
         case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end,
         max(d.update_time)
  from tmp_source_warehouse_stock_detail d
  where d.repository_scope = 'OFFICIAL_MASTER' and nullif(d.system_warehouse_code, '') is not null
  group by d.source_stock_group_key, d.system_warehouse_code
  union all
  select d.source_stock_group_key, max(d.repository_scope), max(d.inventory_scope), 'SYSTEM_SKU',
         d.system_sku, max(d.system_sku_name), count(1), sum(coalesce(d.total_quantity, 0)),
         sum(coalesce(d.available_quantity, 0)), sum(coalesce(d.locked_quantity, 0)), sum(coalesce(d.in_transit_quantity, 0)),
         case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end,
         case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end,
         max(d.update_time)
  from tmp_source_warehouse_stock_detail d
  where d.repository_scope = 'OFFICIAL_MASTER' and nullif(d.system_sku, '') is not null
  group by d.source_stock_group_key, d.system_sku
  union all
  select d.source_stock_group_key, max(d.repository_scope), max(d.inventory_scope), 'CUSTOMER',
         d.customer_name, d.customer_name, count(1), sum(coalesce(d.total_quantity, 0)),
         sum(coalesce(d.available_quantity, 0)), sum(coalesce(d.locked_quantity, 0)), sum(coalesce(d.in_transit_quantity, 0)),
         case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end,
         case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end,
         max(d.update_time)
  from tmp_source_warehouse_stock_detail d
  where d.repository_scope = 'OFFICIAL_MASTER' and nullif(d.customer_name, '') is not null
  group by d.source_stock_group_key, d.customer_name
  union all
  select d.source_stock_group_key, max(d.repository_scope), max(d.inventory_scope), 'STATUS',
         d.status, d.status, count(1), sum(coalesce(d.total_quantity, 0)),
         sum(coalesce(d.available_quantity, 0)), sum(coalesce(d.locked_quantity, 0)), sum(coalesce(d.in_transit_quantity, 0)),
         case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end,
         case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end,
         max(d.update_time)
  from tmp_source_warehouse_stock_detail d
  where d.repository_scope = 'OFFICIAL_MASTER'
  group by d.source_stock_group_key, d.status
  union all
  select d.source_stock_group_key, max(d.repository_scope), max(d.inventory_scope), 'WAREHOUSE_PAIRING_STATUS',
         d.warehouse_pairing_status, d.warehouse_pairing_status, count(1), sum(coalesce(d.total_quantity, 0)),
         sum(coalesce(d.available_quantity, 0)), sum(coalesce(d.locked_quantity, 0)), sum(coalesce(d.in_transit_quantity, 0)),
         case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end,
         case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end,
         max(d.update_time)
  from tmp_source_warehouse_stock_detail d
  where d.repository_scope = 'OFFICIAL_MASTER'
  group by d.source_stock_group_key, d.warehouse_pairing_status
  union all
  select d.source_stock_group_key, max(d.repository_scope), max(d.inventory_scope), 'SKU_PAIRING_STATUS',
         d.sku_pairing_status, d.sku_pairing_status, count(1), sum(coalesce(d.total_quantity, 0)),
         sum(coalesce(d.available_quantity, 0)), sum(coalesce(d.locked_quantity, 0)), sum(coalesce(d.in_transit_quantity, 0)),
         case when sum(case when d.boxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.boxed_quantity, 0)) end,
         case when sum(case when d.unboxed_quantity is not null then 1 else 0 end) = 0 then null else sum(coalesce(d.unboxed_quantity, 0)) end,
         max(d.update_time)
  from tmp_source_warehouse_stock_detail d
  where d.repository_scope = 'OFFICIAL_MASTER'
  group by d.source_stock_group_key, d.sku_pairing_status
) m;

start transaction;

delete from source_warehouse_stock_filter_metric where repository_scope = 'OFFICIAL_MASTER';
delete from source_warehouse_stock_group where repository_scope = 'OFFICIAL_MASTER';
delete from source_warehouse_stock_detail where repository_scope = 'OFFICIAL_MASTER';

insert into source_warehouse_stock_detail
select * from tmp_source_warehouse_stock_detail;

insert into source_warehouse_stock_group
select * from tmp_source_warehouse_stock_group;

insert into source_warehouse_stock_filter_metric
select * from tmp_source_warehouse_stock_filter_metric;

commit;

drop temporary table if exists tmp_source_warehouse_stock_filter_metric;
drop temporary table if exists tmp_source_warehouse_stock_group;
drop temporary table if exists tmp_source_warehouse_stock_detail;
